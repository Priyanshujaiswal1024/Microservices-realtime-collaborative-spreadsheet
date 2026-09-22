package com.collab.spreadsheet.sheet.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "protected_ranges", indexes = {
    @Index(name = "idx_protected_range_sheet", columnList = "sheet_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProtectedRange {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sheet_id", nullable = false)
    private Sheet sheet;

    @Column(length = 100)
    private String name;

    @Column(name = "start_row", nullable = false)
    private Integer startRow;

    @Column(name = "start_col", nullable = false)
    private Integer startCol;

    @Column(name = "end_row", nullable = false)
    private Integer endRow;

    @Column(name = "end_col", nullable = false)
    private Integer endCol;

    @Column(name = "allowed_user_ids", columnDefinition = "TEXT")
    private String allowedUserIds; // JSON array of user UUID strings, e.g. '["u1", "u2"]'

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
