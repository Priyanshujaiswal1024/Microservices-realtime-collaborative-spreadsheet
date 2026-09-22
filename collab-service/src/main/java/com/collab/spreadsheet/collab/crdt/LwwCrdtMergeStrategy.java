package com.collab.spreadsheet.collab.crdt;

import com.collab.spreadsheet.common.crdt.CellState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Last-Write-Wins (LWW) Element-Register CRDT merge strategy.
 *
 * Uses Hybrid Logical Clock (HLC) comparison:
 * 1. Physical time comparison
 * 2. Logical counter comparison (if physical times equal)
 * 3. ClientId tie-breaker (deterministic across all distributed nodes)
 */
@Component
@Slf4j
public class LwwCrdtMergeStrategy implements CrdtMergeStrategy {

    @Override
    public CellState merge(CellState current, CellState incoming) {
        if (current == null) {
            return incoming;
        }
        if (incoming == null) {
            return current;
        }

        // Apply incoming only if incoming HLC > current HLC
        if (incoming.getTimestamp().compareTo(current.getTimestamp()) > 0) {
            log.debug("LWW Win: incoming HLC {} > current HLC {}", 
                    incoming.getTimestamp().toCompactString(), current.getTimestamp().toCompactString());
            return incoming;
        }

        log.debug("LWW Discard: current HLC {} >= incoming HLC {}", 
                current.getTimestamp().toCompactString(), incoming.getTimestamp().toCompactString());
        return current;
    }

    @Override
    public String getStrategyName() {
        return "LWW-HLC";
    }
}
