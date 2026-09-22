package com.collab.spreadsheet.sheet.kafka;

import com.collab.spreadsheet.common.config.KafkaTopics;
import com.collab.spreadsheet.common.events.SheetLifecycleEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaSheetLifecycleProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishEvent(SheetLifecycleEvent event) {
        String key = event.getWorkbookId() != null ? event.getWorkbookId() : event.getEventId();
        log.info("Publishing sheet-lifecycle event: action={}, workbookId={}, key={}", 
                event.getAction(), event.getWorkbookId(), key);
        try {
            kafkaTemplate.send(KafkaTopics.SHEET_LIFECYCLE, key, event);
        } catch (Exception e) {
            log.error("Failed to publish sheet lifecycle event to Kafka: {}", e.getMessage(), e);
        }
    }
}
