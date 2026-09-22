package com.collab.spreadsheet.common.websocket;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.*;

/**
 * WebSocket message for cursor position updates
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeName("CURSOR_MOVE")
public class CursorMoveMessage extends WebSocketMessage {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Sheet ID
     */
    private String sheetId;
    
    /**
     * Row number (0-indexed, null if no selection)
     */
    private Integer row;
    
    /**
     * Column number (0-indexed, null if no selection)
     */
    private Integer col;
    
    /**
     * User display name
     */
    private String userName;
    
    /**
     * User color for cursor display (hex color)
     */
    private String color;

    @Override
    public String getType() {
        return "CURSOR_MOVE";
    }
}
