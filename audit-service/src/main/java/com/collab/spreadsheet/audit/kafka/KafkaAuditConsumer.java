package com.collab.spreadsheet.audit.kafka;

import com.collab.spreadsheet.audit.entity.AuditEvent;
import com.collab.spreadsheet.audit.repository.AuditEventRepository;
import com.collab.spreadsheet.common.config.KafkaTopics;
import com.collab.spreadsheet.common.events.CellEditEvent;
import com.collab.spreadsheet.common.events.SheetLifecycleEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaAuditConsumer {

    private final AuditEventRepository auditEventRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String DEDUPE_PREFIX = "audit:dedupe:";
    private static final Duration DEDUPE_TTL = Duration.ofHours(24);

    @KafkaListener(topics = KafkaTopics.CELL_EDITS, groupId = "audit-service-cell-edits")
    @Transactional
    public void consumeCellEdit(CellEditEvent event) {
        if (event == null || event.getEventId() == null) {
            return;
        }

        String dedupeKey = DEDUPE_PREFIX + event.getEventId();
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(dedupeKey, "1", DEDUPE_TTL);
        if (Boolean.FALSE.equals(isNew)) {
            log.debug("Duplicate CellEditEvent skipped: {}", event.getEventId());
            return;
        }

        try {
            String hlcTs = (event.getCellState() != null && event.getCellState().getTimestamp() != null)
                    ? event.getCellState().getTimestamp().toCompactString()
                    : null;
            String newVal = event.getCellState() != null ? event.getCellState().getValue() : null;

            AuditEvent audit = AuditEvent.builder()
                    .eventId(event.getEventId())
                    .workbookId(event.getWorkbookId() != null ? event.getWorkbookId() : "unknown")
                    .sheetId(event.getSheetId())
                    .row(event.getRow())
                    .col(event.getCol())
                    .eventType("CELL_EDIT")
                    .actorId(event.getActorId())
                    .payload(objectMapper.writeValueAsString(event.getCellState()))
                    .previousValue(event.getPreviousValue())
                    .newValue(newVal)
                    .hlcTimestamp(hlcTs)
                    .createdAt(event.getTimestamp())
                    .build();

            auditEventRepository.save(audit);
            log.debug("Persisted audit event for cell edit: eventId={}", event.getEventId());

        } catch (Exception e) {
            log.error("Failed to persist audit event for cell edit: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.SHEET_LIFECYCLE, groupId = "audit-service-sheet-lifecycle")
    @Transactional
    public void consumeSheetLifecycle(SheetLifecycleEvent event) {
        if (event == null || event.getEventId() == null) {
            return;
        }

        String dedupeKey = DEDUPE_PREFIX + event.getEventId();
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(dedupeKey, "1", DEDUPE_TTL);
        if (Boolean.FALSE.equals(isNew)) {
            log.debug("Duplicate SheetLifecycleEvent skipped: {}", event.getEventId());
            return;
        }

        try {
            AuditEvent audit = AuditEvent.builder()
                    .eventId(event.getEventId())
                    .workbookId(event.getWorkbookId())
                    .sheetId(event.getSheetId())
                    .eventType("SHEET_LIFECYCLE_" + event.getAction())
                    .actorId(event.getActorId())
                    .payload(event.getMetadata() != null ? objectMapper.writeValueAsString(event.getMetadata()) : null)
                    .createdAt(event.getTimestamp())
                    .build();

            auditEventRepository.save(audit);
            log.info("Persisted audit event for sheet lifecycle: action={}, workbookId={}", event.getAction(), event.getWorkbookId());

        } catch (Exception e) {
            log.error("Failed to persist audit event for sheet lifecycle: {}", e.getMessage(), e);
        }
    }
}
