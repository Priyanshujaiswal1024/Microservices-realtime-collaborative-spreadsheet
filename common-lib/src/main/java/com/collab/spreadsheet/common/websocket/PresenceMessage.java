package com.collab.spreadsheet.common.websocket;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.*;

/**
 * WebSocket message for user presence (join/leave)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeName("PRESENCE")
public class PresenceMessage extends WebSocketMessage {
    
    private static final long serialVersionUID = 1L;
    
    public enum Action {
        JOIN,
        LEAVE
    }
    
    /**
     * Sheet ID
     */
    private String sheetId;
    
    /**
     * Presence action
     */
    private Action action;
    
    /**
     * User display name
     */
    private String userName;
    
    /**
     * User color for presence indicator
     */
    private String color;

    @Override
    public String getType() {
        return "PRESENCE";
    }
}
