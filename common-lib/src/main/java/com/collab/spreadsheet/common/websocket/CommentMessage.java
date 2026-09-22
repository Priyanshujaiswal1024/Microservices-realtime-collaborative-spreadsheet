package com.collab.spreadsheet.common.websocket;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.*;

import java.util.List;

/**
 * WebSocket message for real-time comment updates
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeName("COMMENT")
public class CommentMessage extends WebSocketMessage {
    
    private static final long serialVersionUID = 1L;
    
    public enum Action {
        CREATED,
        UPDATED,
        DELETED,
        RESOLVED,
        REOPENED
    }
    
    /**
     * Comment ID
     */
    private String commentId;
    
    /**
     * Sheet ID
     */
    private String sheetId;
    
    /**
     * Cell ID
     */
    private String cellId;
    
    /**
     * Comment action
     */
    private Action action;
    
    /**
     * Comment text
     */
    private String text;
    
    /**
     * Author name
     */
    private String authorName;
    
    /**
     * Mentioned user IDs
     */
    private List<String> mentionedUserIds;

    @Override
    public String getType() {
        return "COMMENT";
    }
}
