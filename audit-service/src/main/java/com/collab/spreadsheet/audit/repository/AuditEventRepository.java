package com.collab.spreadsheet.audit.repository;

import com.collab.spreadsheet.audit.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, String> {

    Optional<AuditEvent> findByEventId(String eventId);

    Page<AuditEvent> findByWorkbookIdOrderByCreatedAtDesc(String workbookId, Pageable pageable);

    Page<AuditEvent> findBySheetIdOrderByCreatedAtDesc(String sheetId, Pageable pageable);

    List<AuditEvent> findByWorkbookIdAndCreatedAtLessThanEqualOrderByCreatedAtAsc(String workbookId, Instant targetTimestamp);

    List<AuditEvent> findByWorkbookIdAndCreatedAtBetweenOrderByCreatedAtAsc(String workbookId, Instant from, Instant to);
}
