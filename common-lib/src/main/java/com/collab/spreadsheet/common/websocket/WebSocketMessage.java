package com.collab.spreadsheet.common.websocket;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Base class for all WebSocket messages
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = CellUpdateMessage.class, name = "CELL_UPDATE"),
    @JsonSubTypes.Type(value = CellBatchUpdateMessage.class, name = "CELL_BATCH_UPDATE"),
    @JsonSubTypes.Type(value = CursorMoveMessage.class, name = "CURSOR_MOVE"),
    @JsonSubTypes.Type(value = PresenceMessage.class, name = "PRESENCE"),
    @JsonSubTypes.Type(value = CommentMessage.class, name = "COMMENT")
})
public abstract class WebSocketMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Message timestamp
     */
    private Instant timestamp;
    
    /**
     * User ID who sent the message
     */
    private String userId;
    
    /**
     * Get message type
     */
    public abstract String getType();
}
