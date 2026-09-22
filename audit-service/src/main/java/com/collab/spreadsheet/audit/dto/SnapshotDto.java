package com.collab.spreadsheet.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SnapshotDto {
    private String id;
    private String workbookId;
    private String label;
    private String description;
    private String createdBy;
    private Instant createdAt;
    private String snapshotData;
}
