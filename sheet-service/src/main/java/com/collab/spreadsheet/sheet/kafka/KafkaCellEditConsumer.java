package com.collab.spreadsheet.sheet.kafka;

import com.collab.spreadsheet.common.config.KafkaTopics;
import com.collab.spreadsheet.common.crdt.CellState;
import com.collab.spreadsheet.common.events.CellEditEvent;
import com.collab.spreadsheet.sheet.entity.Cell;
import com.collab.spreadsheet.sheet.entity.Sheet;
import com.collab.spreadsheet.sheet.repository.CellRepository;
import com.collab.spreadsheet.sheet.repository.SheetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaCellEditConsumer {

    private final CellRepository cellRepository;
    private final SheetRepository sheetRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String DEDUPE_PREFIX = "sheet:dedupe:cell-edit:";
    private static final Duration DEDUPE_TTL = Duration.ofHours(24);

    @KafkaListener(topics = KafkaTopics.CELL_EDITS, groupId = "sheet-service-cell-persistence")
    @Transactional
    public void consumeCellEdit(CellEditEvent event) {
        if (event == null || event.getSheetId() == null || event.getCellState() == null) {
            log.warn("Received invalid CellEditEvent: {}", event);
            return;
        }

        // Idempotency check with Redis TTL
        if (event.getEventId() != null) {
            String dedupeKey = DEDUPE_PREFIX + event.getEventId();
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(dedupeKey, "1", DEDUPE_TTL);
            if (Boolean.FALSE.equals(isNew)) {
                log.debug("Duplicate CellEditEvent skipped in sheet-service: {}", event.getEventId());
                return;
            }
        }

        log.debug("Async persisting cell edit from Kafka: sheetId={}, row={}, col={}", 
                event.getSheetId(), event.getRow(), event.getCol());

        Optional<Sheet> sheetOpt = sheetRepository.findById(event.getSheetId());
        if (sheetOpt.isEmpty()) {
            log.warn("Sheet not found for cell-edit event: {}", event.getSheetId());
            return;
        }

        CellState state = event.getCellState();
        String hlcTs = state.getTimestamp() != null ? state.getTimestamp().toCompactString() : null;

        Optional<Cell> existingOpt = cellRepository.findBySheetIdAndRowAndCol(
                event.getSheetId(), event.getRow(), event.getCol());

        if (existingOpt.isPresent()) {
            Cell cell = existingOpt.get();
            cell.setValue(state.getValue());
            cell.setDataType(state.getDataType() != null ? state.getDataType() : "TEXT");
            if (state.getFormat() != null) {
                cell.setFormat(state.getFormat());
            }
            cell.setLastModifiedTs(hlcTs);
            cell.setLastModifiedBy(state.getUserId() != null ? state.getUserId() : event.getActorId());
            cell.setVersion(cell.getVersion() + 1);
            cellRepository.save(cell);
        } else {
            Cell cell = Cell.builder()
                    .sheet(sheetOpt.get())
                    .row(event.getRow())
                    .col(event.getCol())
                    .value(state.getValue())
                    .dataType(state.getDataType() != null ? state.getDataType() : "TEXT")
                    .format(state.getFormat())
                    .lastModifiedTs(hlcTs)
                    .lastModifiedBy(state.getUserId() != null ? state.getUserId() : event.getActorId())
                    .version(1L)
                    .build();
            cellRepository.save(cell);
        }
    }
}
