package com.collab.spreadsheet.collab.crdt;

import com.collab.spreadsheet.common.crdt.CellState;

/**
 * Strategy pattern interface for CRDT cell state merging algorithms.
 * Allows swappable merge behaviors (LWW, Multi-value register, Sequence CRDT for text, etc.)
 */
public interface CrdtMergeStrategy {

    /**
     * Merge current cell state with incoming cell state.
     *
     * @param current  Current state stored on server (may be null for new cells)
     * @param incoming Incoming update from client
     * @return Merged winning CellState
     */
    CellState merge(CellState current, CellState incoming);

    /**
     * Strategy identifier
     */
    String getStrategyName();
}
