package com.collab.spreadsheet.collab.kafka;

import com.collab.spreadsheet.common.config.KafkaTopics;
import com.collab.spreadsheet.common.events.CellEditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaCellEditProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @jakarta.annotation.PostConstruct
    public void init() {
        // Disable default noisy LoggingProducerListener since we handle exceptions in whenComplete
        this.kafkaTemplate.setProducerListener(null);
    }

    public void publishCellEdit(CellEditEvent event) {
        String key = event.getSheetId(); // Partition key ensures ordering per sheet
        log.debug("Publishing cell edit to Kafka topic {}: sheetId={}, row={}, col={}",
                KafkaTopics.CELL_EDITS, event.getSheetId(), event.getRow(), event.getCol());
        try {
            kafkaTemplate.send(KafkaTopics.CELL_EDITS, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Could not publish CellEditEvent to Kafka (Kafka might be offline): {}", ex.getMessage());
                        } else {
                            log.debug("CellEditEvent published successfully to Kafka topic {}", KafkaTopics.CELL_EDITS);
                        }
                    });
        } catch (Exception e) {
            String cause = e.getCause() != null ? " (" + e.getCause().getMessage() + ")" : "";
            log.warn("Failed to initiate Kafka publish for CellEditEvent: {}{}", e.getMessage(), cause);
        }
    }
}
