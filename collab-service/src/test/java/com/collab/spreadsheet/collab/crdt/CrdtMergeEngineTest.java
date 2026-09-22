package com.collab.spreadsheet.collab.crdt;

import com.collab.spreadsheet.common.crdt.CellState;
import com.collab.spreadsheet.common.crdt.HybridLogicalClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CrdtMergeEngineTest {

    private CrdtMergeEngine mergeEngine;

    @BeforeEach
    void setUp() {
        mergeEngine = new CrdtMergeEngine(new LwwCrdtMergeStrategy());
    }

    @Test
    @DisplayName("Should accept incoming update when current cell is null (first write)")
    void testFirstWrite() {
        HybridLogicalClock hlc = new HybridLogicalClock(1000L, 0L, "clientA");
        CellState incoming = new CellState("Hello World", hlc, "clientA", "user-1", "TEXT", null);

        CellState winner = mergeEngine.merge(null, incoming);

        assertThat(winner).isNotNull();
        assertThat(winner.getValue()).isEqualTo("Hello World");
        assertThat(winner.getClientId()).isEqualTo("clientA");
    }

    @Test
    @DisplayName("Should merge using LWW when incoming physical time is strictly greater")
    void testIncomingPhysicalTimeWins() {
        HybridLogicalClock hlcCurrent = new HybridLogicalClock(1000L, 0L, "clientA");
        CellState current = new CellState("Old Value", hlcCurrent, "clientA", "user-1", "TEXT", null);

        HybridLogicalClock hlcIncoming = new HybridLogicalClock(1005L, 0L, "clientB");
        CellState incoming = new CellState("Newer Value", hlcIncoming, "clientB", "user-2", "TEXT", null);

        CellState winner = mergeEngine.merge(current, incoming);

        assertThat(winner.getValue()).isEqualTo("Newer Value");
        assertThat(winner.getClientId()).isEqualTo("clientB");
    }

    @Test
    @DisplayName("Should reject older incoming update when incoming physical time is less")
    void testOlderIncomingRejected() {
        HybridLogicalClock hlcCurrent = new HybridLogicalClock(1005L, 0L, "clientB");
        CellState current = new CellState("Newer Value", hlcCurrent, "clientB", "user-2", "TEXT", null);

        HybridLogicalClock hlcIncoming = new HybridLogicalClock(1000L, 0L, "clientA");
        CellState incoming = new CellState("Old Stale Value", hlcIncoming, "clientA", "user-1", "TEXT", null);

        CellState winner = mergeEngine.merge(current, incoming);

        assertThat(winner.getValue()).isEqualTo("Newer Value");
        assertThat(winner.getClientId()).isEqualTo("clientB");
    }

    @Test
    @DisplayName("Should use logical counter for ordering when physical times are identical")
    void testLogicalCounterTieBreaker() {
        long physicalTime = 1500L;
        HybridLogicalClock hlcCurrent = new HybridLogicalClock(physicalTime, 1L, "clientA");
        CellState current = new CellState("Counter 1", hlcCurrent, "clientA", "user-1", "TEXT", null);

        HybridLogicalClock hlcIncoming = new HybridLogicalClock(physicalTime, 2L, "clientB");
        CellState incoming = new CellState("Counter 2", hlcIncoming, "clientB", "user-2", "TEXT", null);

        CellState winner = mergeEngine.merge(current, incoming);

        assertThat(winner.getValue()).isEqualTo("Counter 2");
    }

    @Test
    @DisplayName("Should deterministically tie-break using clientId when physical and logical counters are identical")
    void testClientIdTieBreaker() {
        long physicalTime = 1500L;
        long logicalCounter = 0L;

        // "clientZ" > "clientA" lexicographically
        HybridLogicalClock hlcA = new HybridLogicalClock(physicalTime, logicalCounter, "clientA");
        CellState stateA = new CellState("From Client A", hlcA, "clientA", "user-1", "TEXT", null);

        HybridLogicalClock hlcZ = new HybridLogicalClock(physicalTime, logicalCounter, "clientZ");
        CellState stateZ = new CellState("From Client Z", hlcZ, "clientZ", "user-2", "TEXT", null);

        CellState winner1 = mergeEngine.merge(stateA, stateZ);
        assertThat(winner1.getValue()).isEqualTo("From Client Z");

        // Reverse order must produce identical result (commutativity)
        CellState winner2 = mergeEngine.merge(stateZ, stateA);
        assertThat(winner2.getValue()).isEqualTo("From Client Z");
    }
}
