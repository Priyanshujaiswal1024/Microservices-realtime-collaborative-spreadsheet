package com.collab.spreadsheet.collab;

import com.collab.spreadsheet.collab.crdt.CrdtMergeEngine;
import com.collab.spreadsheet.collab.redis.RedisCellStateRepository;
import com.collab.spreadsheet.collab.redis.RedisStreamReplayBuffer;
import com.collab.spreadsheet.collab.service.CollabOrchestrator;
import com.collab.spreadsheet.common.crdt.CellState;
import com.collab.spreadsheet.common.crdt.HybridLogicalClock;
import com.collab.spreadsheet.common.websocket.CellUpdateMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class CollabServiceIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (redis.isRunning()) {
            registry.add("spring.data.redis.host", redis::getHost);
            registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        }
        if (kafka.isRunning()) {
            registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        }
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.config.enabled", () -> "false");
    }

    @Autowired(required = false)
    private CollabOrchestrator collabOrchestrator;

    @Autowired(required = false)
    private RedisCellStateRepository redisCellStateRepository;

    @Autowired(required = false)
    private RedisStreamReplayBuffer redisStreamReplayBuffer;

    @Test
    @DisplayName("Verify CRDT state persistence and stream replay buffer integration")
    void testCrdtPipelineEndToEnd() {
        if (collabOrchestrator == null || redisCellStateRepository == null) {
            return; // Skip when running in environments without Docker daemon
        }

        String sheetId = "test-sheet-101";
        HybridLogicalClock hlc1 = new HybridLogicalClock(1000L, 0L, "clientA");
        CellState state1 = new CellState("First Value", hlc1, "clientA", "user-1", "TEXT", null);

        CellUpdateMessage msg1 = new CellUpdateMessage();
        msg1.setSheetId(sheetId);
        msg1.setRow(2);
        msg1.setCol(3);
        msg1.setCellState(state1);

        CellState winning1 = collabOrchestrator.processCellEdit(sheetId, msg1, "user-1");
        assertThat(winning1).isNotNull();
        assertThat(winning1.getValue()).isEqualTo("First Value");

        // Verify stored in Redis Hash
        CellState stored = redisCellStateRepository.getCell(sheetId, 2, 3);
        assertThat(stored).isNotNull();
        assertThat(stored.getValue()).isEqualTo("First Value");

        // Test stream replay ops
        List<CellUpdateMessage> replayedOps = redisStreamReplayBuffer.getReplayOps(sheetId, "-", 10);
        assertThat(replayedOps).isNotEmpty();
    }
}
