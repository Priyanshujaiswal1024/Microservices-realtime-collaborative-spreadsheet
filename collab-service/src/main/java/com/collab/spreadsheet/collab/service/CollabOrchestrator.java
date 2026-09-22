package com.collab.spreadsheet.collab.service;

import com.collab.spreadsheet.collab.crdt.CrdtMergeEngine;
import com.collab.spreadsheet.collab.kafka.KafkaCellEditProducer;
import com.collab.spreadsheet.collab.redis.RedisCellStateRepository;
import com.collab.spreadsheet.collab.redis.RedisPubSubBroadcaster;
import com.collab.spreadsheet.collab.redis.RedisStreamReplayBuffer;
import com.collab.spreadsheet.collab.security.ProtectedRangeRedisValidator;
import com.collab.spreadsheet.common.crdt.CellState;
import com.collab.spreadsheet.common.events.CellEditEvent;
import com.collab.spreadsheet.common.websocket.CellBatchUpdateMessage;
import com.collab.spreadsheet.common.websocket.CellUpdateMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollabOrchestrator {

    private final CrdtMergeEngine crdtMergeEngine;
    private final RedisCellStateRepository redisCellStateRepository;
    private final RedisPubSubBroadcaster redisPubSubBroadcaster;
    private final RedisStreamReplayBuffer redisStreamReplayBuffer;
    private final KafkaCellEditProducer kafkaCellEditProducer;
    private final ProtectedRangeRedisValidator protectedRangeValidator;

    public CellState processCellEdit(String sheetId, CellUpdateMessage message, String userId) {
        int row = message.getRow();
        int col = message.getCol();
        CellState incoming = message.getCellState();

        if (incoming == null) {
            log.warn("Received empty CellState in edit message for sheet {}, ({},{})", sheetId, row, col);
            return null;
        }

        // Handle sheet clear broadcast
        if (row == -1 && col == -1 && ("CLEAR_SHEET".equals(incoming.getValue()) || "ACTION".equalsIgnoreCase(incoming.getDataType()))) {
            redisCellStateRepository.clearSheet(sheetId);
            message.setSheetId(sheetId);
            message.setUserId(userId);
            message.setTimestamp(Instant.now());
            redisPubSubBroadcaster.broadcast(sheetId, message);
            log.info("Cleared all cells in Redis cache for sheet {}", sheetId);
            return incoming;
        }

        // Defense in depth: Verify user permission & check if cell is protected
        if (!protectedRangeValidator.hasEditPermission(sheetId, userId)) {
            log.warn("User {} lacks edit permissions for sheet {}", userId, sheetId);
            return null;
        }

        if (protectedRangeValidator.isCellProtectedForUser(sheetId, row, col, userId)) {
            log.warn("Rejected CRDT edit on protected cell ({},{}) for sheet {} by user {}", row, col, sheetId, userId);
            return null;
        }

        message.setSheetId(sheetId);
        message.setUserId(userId);
        message.setTimestamp(Instant.now());

        // 1. Fetch current cell state from Redis Hash
        CellState current = redisCellStateRepository.getCell(sheetId, row, col);

        // 2. Perform CRDT merge
        CellState winningState = crdtMergeEngine.merge(current, incoming);

        // 3. If incoming won, update state & broadcast
        if (winningState == incoming || (current == null && incoming != null)) {
            // Save to Redis Hash cache
            redisCellStateRepository.saveCell(sheetId, row, col, winningState);

            // Record in Redis Streams replay buffer
            redisStreamReplayBuffer.recordOp(sheetId, message);

            // Broadcast across pods via Redis Pub/Sub
            redisPubSubBroadcaster.broadcast(sheetId, message);

            // Publish to Kafka for asynchronous Postgres persistence & audit logging
            CellEditEvent kafkaEvent = CellEditEvent.builder()
                    .actorId(userId)
                    .sheetId(sheetId)
                    .row(row)
                    .col(col)
                    .cellState(winningState)
                    .previousValue(current != null ? current.getValue() : null)
                    .build();
            kafkaCellEditProducer.publishCellEdit(kafkaEvent);

            log.debug("Applied CRDT cell edit for sheet {}, cell ({},{}): value='{}'", 
                    sheetId, row, col, winningState.getValue());
        } else {
            log.debug("Discarded older CRDT edit for sheet {}, cell ({},{})", sheetId, row, col);
        }

        return winningState;
    }

    public void processCellBatchEdit(String sheetId, CellBatchUpdateMessage message, String userId) {
        if (message == null || message.getUpdates() == null || message.getUpdates().isEmpty()) {
            return;
        }

        if (!protectedRangeValidator.hasEditPermission(sheetId, userId)) {
            log.warn("User {} lacks edit permissions for sheet {}", userId, sheetId);
            return;
        }

        List<ProtectedRangeRedisValidator.RangeRule> rules = protectedRangeValidator.getRules(sheetId);
        List<CellBatchUpdateMessage.CellItem> updates = message.getUpdates();
        List<String> fields = new ArrayList<>(updates.size());
        List<CellBatchUpdateMessage.CellItem> permittedUpdates = new ArrayList<>(updates.size());

        for (CellBatchUpdateMessage.CellItem item : updates) {
            if (item.getRow() == null || item.getCol() == null || item.getCellState() == null) continue;
            if (protectedRangeValidator.isCellProtected(rules, item.getRow(), item.getCol(), userId)) {
                log.warn("Rejected batch edit on protected cell ({},{}) for sheet {} by user {}",
                        item.getRow(), item.getCol(), sheetId, userId);
                continue;
            }
            fields.add(item.getRow() + ":" + item.getCol());
            permittedUpdates.add(item);
        }

        if (permittedUpdates.isEmpty()) {
            return;
        }

        // 1. Bulk fetch current cell states from Redis in a single round-trip
        Map<String, CellState> currentStates = redisCellStateRepository.getCells(sheetId, fields);

        // 2. Perform CRDT merge for each cell
        Map<String, CellState> toSave = new HashMap<>();
        List<CellBatchUpdateMessage.CellItem> winningBatch = new ArrayList<>();
        List<CellEditEvent> kafkaEvents = new ArrayList<>();

        Instant now = Instant.now();

        for (CellBatchUpdateMessage.CellItem item : permittedUpdates) {
            int row = item.getRow();
            int col = item.getCol();
            String key = row + ":" + col;
            CellState incoming = item.getCellState();
            CellState current = currentStates.get(key);

            CellState winning = crdtMergeEngine.merge(current, incoming);
            if (winning == incoming || (current == null && incoming != null)) {
                toSave.put(key, winning);
                winningBatch.add(new CellBatchUpdateMessage.CellItem(row, col, winning));
                kafkaEvents.add(CellEditEvent.builder()
                        .actorId(userId)
                        .sheetId(sheetId)
                        .row(row)
                        .col(col)
                        .cellState(winning)
                        .previousValue(current != null ? current.getValue() : null)
                        .build());
            }
        }

        if (!winningBatch.isEmpty()) {
            // 3. Atomic bulk save to Redis Hash
            redisCellStateRepository.saveCells(sheetId, toSave);

            // 4. Construct single batched broadcast message
            CellBatchUpdateMessage broadcastMsg = new CellBatchUpdateMessage();
            broadcastMsg.setSheetId(sheetId);
            broadcastMsg.setUserId(userId);
            broadcastMsg.setTimestamp(now);
            broadcastMsg.setUpdates(winningBatch);

            // 5. Record atomic batch op in Redis Stream
            redisStreamReplayBuffer.recordOp(sheetId, broadcastMsg);

            // 6. Broadcast ONE single atomic message across pods via Redis Pub/Sub
            redisPubSubBroadcaster.broadcast(sheetId, broadcastMsg);

            // 7. Asynchronously publish to Kafka
            for (CellEditEvent event : kafkaEvents) {
                kafkaCellEditProducer.publishCellEdit(event);
            }

            log.info("Applied and broadcasted atomic batch edit of {} cells on sheet {} from user {}",
                    winningBatch.size(), sheetId, userId);
        }
    }
}
