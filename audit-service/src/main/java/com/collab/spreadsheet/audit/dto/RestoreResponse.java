package com.collab.spreadsheet.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestoreResponse {
    private String workbookId;
    private String restoredFromSnapshotId;
    private Instant targetTimestamp;
    private int replayedEventsCount;
    private String snapshotData;
    private String message;
}
