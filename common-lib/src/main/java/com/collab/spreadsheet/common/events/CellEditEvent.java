package com.collab.spreadsheet.common.events;

import com.collab.spreadsheet.common.crdt.CellState;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.*;

/**
 * Event published when a cell is edited in real-time collaboration
 * 
 * Topic: cell-edits
 * Partition Key: sheetId (ensures ordering per sheet)
 * Consumers: audit-service, sheet-service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeName("CELL_EDIT")
public class CellEditEvent extends BaseEvent {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Workbook ID
     */
    private String workbookId;
    
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
    
    /**
     * Previous value (for audit trail)
     */
    private String previousValue;

    @Builder
    public CellEditEvent(String actorId, String workbookId, String sheetId, 
                         Integer row, Integer col, CellState cellState, String previousValue) {
        super();
        initializeBaseFields(actorId);
        this.workbookId = workbookId;
        this.sheetId = sheetId;
        this.row = row;
        this.col = col;
        this.cellState = cellState;
        this.previousValue = previousValue;
    }

    @Override
    public String getEventType() {
        return "CELL_EDIT";
    }

    /**
     * Get cell coordinate in A1 notation (for logging/debugging)
     */
    public String getCellCoordinate() {
        return columnIndexToLetter(col) + (row + 1);
    }

    private static String columnIndexToLetter(int col) {
        StringBuilder result = new StringBuilder();
        while (col >= 0) {
            result.insert(0, (char) ('A' + (col % 26)));
            col = (col / 26) - 1;
        }
        return result.toString();
    }
}
