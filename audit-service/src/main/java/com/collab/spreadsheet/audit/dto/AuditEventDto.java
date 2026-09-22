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
public class AuditEventDto {
    private String id;
    private String eventId;
    private String workbookId;
    private String sheetId;
    private Integer row;
    private Integer col;
    private String eventType;
    private String actorId;
    private String previousValue;
    private String newValue;
    private String hlcTimestamp;
    private Instant createdAt;
}
