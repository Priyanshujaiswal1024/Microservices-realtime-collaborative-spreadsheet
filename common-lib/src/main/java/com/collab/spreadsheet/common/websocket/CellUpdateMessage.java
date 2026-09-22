package com.collab.spreadsheet.common.websocket;

import com.collab.spreadsheet.common.crdt.CellState;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.*;

/**
 * WebSocket message for cell updates in real-time collaboration
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeName("CELL_UPDATE")
public class CellUpdateMessage extends WebSocketMessage {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Sheet ID
     */
    private String sheetId;
    
    /**
     * Row number (0-indexed)
     */
    private Integer row;
    
    /**
     * Column number (0-indexed)
     */
    private Integer col;
    
    /**
     * CRDT cell state with HLC timestamp
     */
    private CellState cellState;

    @Override
    public String getType() {
        return "CELL_UPDATE";
    }
}
