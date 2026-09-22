package com.collab.spreadsheet.common.events;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.*;

/**
 * Event published for workbook and sheet lifecycle operations
 * 
 * Topic: sheet-lifecycle
 * Partition Key: workbookId
 * Consumers: audit-service, notification-service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeName("SHEET_LIFECYCLE")
public class SheetLifecycleEvent extends BaseEvent {
    
    private static final long serialVersionUID = 1L;
    
    public enum Action {
        WORKBOOK_CREATED,
        WORKBOOK_UPDATED,
        WORKBOOK_DELETED,
        SHEET_CREATED,
        SHEET_UPDATED,
        SHEET_DELETED,
        PERMISSION_GRANTED,
        PERMISSION_REVOKED,
        SHARED_WITH_USER
    }
    
    /**
     * Lifecycle action
     */
    private Action action;
    
    /**
     * Workbook ID
     */
    private String workbookId;
    
    /**
     * Sheet ID (null for workbook-level actions)
     */
    private String sheetId;
    
    /**
     * Workbook/Sheet name
     */
    private String name;
    
    /**
     * Target user ID (for permission/sharing actions)
     */
    private String targetUserId;
    
    /**
     * Additional metadata (JSON string)
     */
    private String metadata;

    @Builder
    public SheetLifecycleEvent(String actorId, Action action, String workbookId, 
                               String sheetId, String name, String targetUserId, String metadata) {
        super();
        initializeBaseFields(actorId);
        this.action = action;
        this.workbookId = workbookId;
        this.sheetId = sheetId;
        this.name = name;
        this.targetUserId = targetUserId;
        this.metadata = metadata;
    }

    @Override
    public String getEventType() {
        return "SHEET_LIFECYCLE";
    }
}
