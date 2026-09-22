package com.collab.spreadsheet.collab.crdt;

import com.collab.spreadsheet.common.crdt.CellState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Merge engine that delegates to the configured CRDT merge strategy
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CrdtMergeEngine {

    private final LwwCrdtMergeStrategy defaultStrategy;

    public CellState merge(CellState current, CellState incoming) {
        return defaultStrategy.merge(current, incoming);
    }

    public boolean isNewer(CellState current, CellState incoming) {
        if (current == null) return true;
        if (incoming == null) return false;
        return incoming.getTimestamp().compareTo(current.getTimestamp()) > 0;
    }
}
