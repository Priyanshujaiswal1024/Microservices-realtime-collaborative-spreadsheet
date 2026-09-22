package com.collab.spreadsheet.audit.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "audit_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "event_id", unique = true)
    private String eventId;

    @Column(name = "workbook_id", nullable = false)
    private String workbookId;

    @Column(name = "sheet_id")
    private String sheetId;

    @Column(name = "cell_row")
    private Integer row;

    @Column(name = "cell_col")
    private Integer col;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "actor_id", nullable = false)
    private String actorId;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "previous_value", columnDefinition = "TEXT")
    private String previousValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "hlc_timestamp")
    private String hlcTimestamp;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
