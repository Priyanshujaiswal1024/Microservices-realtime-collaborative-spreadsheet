package com.collab.spreadsheet.sheet.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "cells", indexes = {
    @Index(name = "idx_cell_sheet_coords", columnList = "sheet_id, cell_row, cell_col", unique = true)
}, uniqueConstraints = {
    @UniqueConstraint(name = "uq_sheet_cell", columnNames = {"sheet_id", "cell_row", "cell_col"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cell {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sheet_id", nullable = false)
    private Sheet sheet;

    @Column(name = "cell_row", nullable = false)
    private Integer row;

    @Column(name = "cell_col", nullable = false)
    private Integer col;

    @Column(name = "cell_value", columnDefinition = "TEXT")
    private String value;

    @Column(name = "data_type", nullable = false, length = 20)
    @Builder.Default
    private String dataType = "TEXT";

    @Column(name = "format", columnDefinition = "TEXT")
    private String format; // JSON string with formatting attributes (bold, italic, fontColor, bgColor, numberFormat, align)

    @Column(name = "last_modified_ts", length = 100)
    private String lastModifiedTs; // HLC timestamp string

    @Column(name = "last_modified_by", length = 36)
    private String lastModifiedBy;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 1L;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
}
