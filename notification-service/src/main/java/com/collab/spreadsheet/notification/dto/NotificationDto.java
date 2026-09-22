package com.collab.spreadsheet.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDto {
    private String id;
    private String userId;
    private String actorId;
    private String type;
    private String title;
    private String message;
    private String link;
    private String workbookId;
    private String sheetId;
    private Boolean read;
    private Instant readAt;
    private Instant createdAt;
}
