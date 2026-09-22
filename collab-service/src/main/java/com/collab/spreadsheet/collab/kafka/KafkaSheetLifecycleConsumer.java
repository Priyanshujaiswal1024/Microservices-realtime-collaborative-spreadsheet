package com.collab.spreadsheet.collab.kafka;

import com.collab.spreadsheet.common.config.KafkaTopics;
import com.collab.spreadsheet.common.events.SheetLifecycleEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaSheetLifecycleConsumer {

    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = KafkaTopics.SHEET_LIFECYCLE, groupId = "collab-service-sheet-lifecycle")
    public void consumeSheetLifecycle(SheetLifecycleEvent event) {
        if (event == null || event.getWorkbookId() == null) {
            log.warn("Received null or invalid SheetLifecycleEvent: {}", event);
            return;
        }

        log.info("Collab-service received sheet lifecycle event: action={}, workbookId={}, sheetId={}, name={}",
                event.getAction(), event.getWorkbookId(), event.getSheetId(), event.getName());

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "SHEET_LIFECYCLE");
        payload.put("action", event.getAction() != null ? event.getAction().name() : null);
        payload.put("workbookId", event.getWorkbookId());
        payload.put("sheetId", event.getSheetId());
        payload.put("name", event.getName());
        payload.put("actorId", event.getActorId());
        payload.put("timestamp", event.getTimestamp() != null ? event.getTimestamp().toString() : null);

        // Broadcast to workbook updates
        messagingTemplate.convertAndSend("/topic/workbook/" + event.getWorkbookId() + "/update", payload);
        messagingTemplate.convertAndSend("/topic/workbook/" + event.getWorkbookId() + "/sheets", payload);
        messagingTemplate.convertAndSend("/topic/workbook/global/update", payload);

        // If sheetId is present, also notify the sheet channel
        if (event.getSheetId() != null) {
            messagingTemplate.convertAndSend("/topic/sheet/" + event.getSheetId() + "/updates", payload);
        }
    }
}
