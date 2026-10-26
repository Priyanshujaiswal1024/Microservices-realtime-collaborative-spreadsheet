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

    @jakarta.annotation.PostConstruct
    public void init() {
        this.kafkaTemplate.setProducerListener(null);
    }

    public void publishEvent(SheetLifecycleEvent event) {
        String key = event.getWorkbookId() != null ? event.getWorkbookId() : event.getEventId();
        log.info("Publishing sheet-lifecycle event: action={}, workbookId={}, key={}", 
                event.getAction(), event.getWorkbookId(), key);
        try {
            kafkaTemplate.send(KafkaTopics.SHEET_LIFECYCLE, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Could not publish SheetLifecycleEvent to Kafka: {}", ex.getMessage());
                        } else {
                            log.info("SheetLifecycleEvent published successfully: action={}, workbookId={}", 
                                    event.getAction(), event.getWorkbookId());
                        }
                    });
        } catch (Exception e) {
            log.warn("Failed to initiate Kafka publish for SheetLifecycleEvent: {}", e.getMessage());
        }
    }
}
