package com.collab.spreadsheet.common.websocket;

import com.collab.spreadsheet.common.crdt.CellState;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.*;

import java.io.Serializable;
import java.util.List;

/**
 * WebSocket message for batched cell updates (range fill, bulk format, paste, etc.)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeName("CELL_BATCH_UPDATE")
public class CellBatchUpdateMessage extends WebSocketMessage {

    private static final long serialVersionUID = 1L;

    /**
     * Sheet ID
     */
    private String sheetId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CellItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private Integer row;
        private Integer col;
        private CellState cellState;
    }

    /**
     * List of cell updates in this atomic batch
     */
    private List<CellItem> updates;

    @Override
    public String getType() {
        return "CELL_BATCH_UPDATE";
    }
}
