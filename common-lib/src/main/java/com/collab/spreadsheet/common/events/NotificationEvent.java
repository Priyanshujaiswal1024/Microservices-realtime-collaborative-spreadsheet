package com.collab.spreadsheet.common.events;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.*;

/**
 * Event published for user notifications
 * 
 * Topic: notifications
 * Partition Key: userId
 * Consumers: (none - internal to notification-service)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeName("NOTIFICATION")
public class NotificationEvent extends BaseEvent {
    
    private static final long serialVersionUID = 1L;
    
    public enum Type {
        COMMENT_MENTION,
        COMMENT_REPLY,
        SHEET_SHARED,
        PERMISSION_GRANTED,
        PERMISSION_REVOKED
    }
    
    /**
     * Notification type
     */
    private Type type;
    
    /**
     * Target user ID (who should receive the notification)
     */
    private String userId;
    
    /**
     * Notification title
     */
    private String title;
    
    /**
     * Notification message
     */
    private String message;
    
    /**
     * Link/URL to the relevant resource
     */
    private String link;
    
    /**
     * Workbook ID (for context)
     */
    private String workbookId;
    
    /**
     * Sheet ID (for context)
     */
    private String sheetId;

    @Builder
    public NotificationEvent(String actorId, Type type, String userId, 
                            String title, String message, String link,
                            String workbookId, String sheetId) {
        super();
        initializeBaseFields(actorId);
        this.type = type;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.link = link;
        this.workbookId = workbookId;
        this.sheetId = sheetId;
    }

    @Override
    public String getEventType() {
        return "NOTIFICATION";
    }
}
