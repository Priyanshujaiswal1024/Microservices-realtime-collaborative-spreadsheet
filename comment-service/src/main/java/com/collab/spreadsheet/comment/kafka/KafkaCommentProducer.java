package com.collab.spreadsheet.comment.kafka;

import com.collab.spreadsheet.common.config.KafkaTopics;
import com.collab.spreadsheet.common.events.CommentEvent;
import com.collab.spreadsheet.common.events.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaCommentProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishCommentEvent(CommentEvent event) {
        String key = event.getWorkbookId();
        log.debug("Publishing CommentEvent to Kafka: action={}, workbookId={}, commentId={}",
                event.getAction(), event.getWorkbookId(), event.getCommentId());
        try {
            kafkaTemplate.send(KafkaTopics.COMMENTS, key, event);
        } catch (Exception e) {
            log.error("Failed to publish CommentEvent: {}", e.getMessage(), e);
        }
    }

    public void publishNotification(NotificationEvent event) {
        String key = event.getUserId();
        log.debug("Publishing NotificationEvent to Kafka: type={}, targetUser={}",
                event.getType(), event.getUserId());
        try {
            kafkaTemplate.send(KafkaTopics.NOTIFICATIONS, key, event);
        } catch (Exception e) {
            log.error("Failed to publish NotificationEvent: {}", e.getMessage(), e);
        }
    }
}
