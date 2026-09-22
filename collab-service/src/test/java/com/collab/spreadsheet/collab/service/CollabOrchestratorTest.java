package com.collab.spreadsheet.collab.service;

import com.collab.spreadsheet.collab.crdt.CrdtMergeEngine;
import com.collab.spreadsheet.collab.kafka.KafkaCellEditProducer;
import com.collab.spreadsheet.collab.redis.RedisCellStateRepository;
import com.collab.spreadsheet.collab.redis.RedisPubSubBroadcaster;
import com.collab.spreadsheet.collab.redis.RedisStreamReplayBuffer;
import com.collab.spreadsheet.common.crdt.CellState;
import com.collab.spreadsheet.common.crdt.HybridLogicalClock;
import com.collab.spreadsheet.common.websocket.CellUpdateMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollabOrchestratorTest {

    @Mock
    private CrdtMergeEngine crdtMergeEngine;

    @Mock
    private RedisCellStateRepository redisCellStateRepository;

    @Mock
    private RedisPubSubBroadcaster redisPubSubBroadcaster;

    @Mock
    private RedisStreamReplayBuffer redisStreamReplayBuffer;

    @Mock
    private KafkaCellEditProducer kafkaCellEditProducer;

    @Mock
    private com.collab.spreadsheet.collab.security.ProtectedRangeRedisValidator protectedRangeValidator;

    @InjectMocks
    private CollabOrchestrator collabOrchestrator;

    private CellUpdateMessage sampleMessage;
    private CellState sampleIncomingState;

    @BeforeEach
    void setUp() {
        HybridLogicalClock hlc = new HybridLogicalClock(2000L, 0L, "clientA");
        sampleIncomingState = new CellState("New Value", hlc, "clientA", "user-1", "TEXT", null);

        sampleMessage = new CellUpdateMessage();
        sampleMessage.setSheetId("sheet-1");
        sampleMessage.setRow(0);
        sampleMessage.setCol(0);
        sampleMessage.setCellState(sampleIncomingState);

        lenient().when(protectedRangeValidator.hasEditPermission(anyString(), anyString())).thenReturn(true);
        lenient().when(protectedRangeValidator.isCellProtectedForUser(anyString(), anyInt(), anyInt(), anyString())).thenReturn(false);
    }

    @Test
    @DisplayName("Should process winning cell edit by persisting to Redis, broadcasting via Pub/Sub, and publishing to Kafka")
    void testProcessWinningCellEdit() {
        when(redisCellStateRepository.getCell("sheet-1", 0, 0)).thenReturn(null);
        when(crdtMergeEngine.merge(null, sampleIncomingState)).thenReturn(sampleIncomingState);

        CellState result = collabOrchestrator.processCellEdit("sheet-1", sampleMessage, "user-1");

        assertThat(result).isNotNull();
        assertThat(result.getValue()).isEqualTo("New Value");

        verify(redisCellStateRepository).saveCell("sheet-1", 0, 0, sampleIncomingState);
        verify(redisStreamReplayBuffer).recordOp(eq("sheet-1"), any(CellUpdateMessage.class));
        verify(redisPubSubBroadcaster).broadcast(eq("sheet-1"), any(CellUpdateMessage.class));
        verify(kafkaCellEditProducer).publishCellEdit(any());
    }

    @Test
    @DisplayName("Should discard older cell edit and not broadcast or persist")
    void testProcessLosingCellEdit() {
        HybridLogicalClock currentHlc = new HybridLogicalClock(3000L, 0L, "clientB");
        CellState currentState = new CellState("Current Winning Value", currentHlc, "clientB", "user-2", "TEXT", null);

        when(redisCellStateRepository.getCell("sheet-1", 0, 0)).thenReturn(currentState);
        when(crdtMergeEngine.merge(currentState, sampleIncomingState)).thenReturn(currentState);

        CellState result = collabOrchestrator.processCellEdit("sheet-1", sampleMessage, "user-1");

        assertThat(result).isEqualTo(currentState);

        verify(redisCellStateRepository, never()).saveCell(any(), anyInt(), anyInt(), any());
        verify(redisPubSubBroadcaster, never()).broadcast(any(), any());
        verify(kafkaCellEditProducer, never()).publishCellEdit(any());
    }
}
