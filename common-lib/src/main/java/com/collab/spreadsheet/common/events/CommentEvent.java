package com.collab.spreadsheet.common.events;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.*;

import java.util.List;

/**
 * Event published for comment operations
 * 
 * Topic: comments
 * Partition Key: workbookId
 * Consumers: notification-service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeName("COMMENT")
public class CommentEvent extends BaseEvent {
    
    private static final long serialVersionUID = 1L;
    
    public enum Action {
        COMMENT_CREATED,
        COMMENT_UPDATED,
        COMMENT_DELETED,
        COMMENT_RESOLVED,
        COMMENT_REOPENED,
        REPLY_ADDED
    }
    
    /**
     * Comment action
     */
    private Action action;
    
    /**
     * Comment ID
     */
    private String commentId;
    
    /**
     * Workbook ID
     */
    private String workbookId;
    
    /**
     * Sheet ID
     */
    private String sheetId;
    
    /**
     * Cell ID
     */
    private String cellId;
    
    /**
     * Comment text
     */
    private String text;
    
    /**
     * Mentioned user IDs (extracted from @mentions)
     */
    private List<String> mentionedUserIds;
    
    /**
     * Parent comment ID (for replies)
     */
    private String parentCommentId;

    @Builder
    public CommentEvent(String actorId, Action action, String commentId, 
                       String workbookId, String sheetId, String cellId,
                       String text, List<String> mentionedUserIds, String parentCommentId) {
        super();
        initializeBaseFields(actorId);
        this.action = action;
        this.commentId = commentId;
        this.workbookId = workbookId;
        this.sheetId = sheetId;
        this.cellId = cellId;
        this.text = text;
        this.mentionedUserIds = mentionedUserIds;
        this.parentCommentId = parentCommentId;
    }

    @Override
    public String getEventType() {
        return "COMMENT";
    }
}
