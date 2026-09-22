package com.collab.spreadsheet.common.crdt;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CellState CRDT implementation
 */
class CellStateTest {

    @Test
    void testMerge_IncomingNewer() {
        HybridLogicalClock oldTimestamp = new HybridLogicalClock(1000, 0, "client1");
        HybridLogicalClock newTimestamp = new HybridLogicalClock(2000, 0, "client2");
        
        CellState current = new CellState("old value", oldTimestamp, "client1", "user1", "TEXT", null);
        CellState incoming = new CellState("new value", newTimestamp, "client2", "user2", "TEXT", null);
        
        CellState merged = CellState.merge(current, incoming);
        
        assertEquals("new value", merged.getValue());
        assertEquals(newTimestamp, merged.getTimestamp());
        assertEquals("client2", merged.getClientId());
    }

    @Test
    void testMerge_CurrentNewer() {
        HybridLogicalClock oldTimestamp = new HybridLogicalClock(1000, 0, "client1");
        HybridLogicalClock newTimestamp = new HybridLogicalClock(2000, 0, "client2");
        
        CellState current = new CellState("current value", newTimestamp, "client2", "user2", "TEXT", null);
        CellState incoming = new CellState("incoming value", oldTimestamp, "client1", "user1", "TEXT", null);
        
        CellState merged = CellState.merge(current, incoming);
        
        // Current should win (newer timestamp)
        assertEquals("current value", merged.getValue());
        assertEquals(newTimestamp, merged.getTimestamp());
        assertEquals("client2", merged.getClientId());
    }

    @Test
    void testMerge_NullCurrent() {
        HybridLogicalClock timestamp = new HybridLogicalClock(1000, 0, "client1");
        CellState incoming = new CellState("value", timestamp, "client1", "user1", "TEXT", null);
        
        CellState merged = CellState.merge(null, incoming);
        
        assertEquals(incoming, merged);
    }

    @Test
    void testMerge_NullIncoming() {
        HybridLogicalClock timestamp = new HybridLogicalClock(1000, 0, "client1");
        CellState current = new CellState("value", timestamp, "client1", "user1", "TEXT", null);
        
        CellState merged = CellState.merge(current, null);
        
        assertEquals(current, merged);
    }

    @Test
    void testIsNewerThan() {
        HybridLogicalClock oldTimestamp = new HybridLogicalClock(1000, 0, "client1");
        HybridLogicalClock newTimestamp = new HybridLogicalClock(2000, 0, "client2");
        
        CellState oldState = new CellState("old", oldTimestamp, "client1", "user1", "TEXT", null);
        CellState newState = new CellState("new", newTimestamp, "client2", "user2", "TEXT", null);
        
        assertTrue(newState.isNewerThan(oldState));
        assertFalse(oldState.isNewerThan(newState));
    }

    @Test
    void testEmpty() {
        CellState empty = CellState.empty("client1", "user1");
        
        assertTrue(empty.isEmpty());
        assertEquals("TEXT", empty.getDataType());
        assertNotNull(empty.getTimestamp());
    }

    @Test
    void testWithValue() {
        HybridLogicalClock timestamp1 = new HybridLogicalClock(1000, 0, "client1");
        CellState state = new CellState("value1", timestamp1, "client1", "user1", "TEXT", null);
        
        HybridLogicalClock timestamp2 = new HybridLogicalClock(2000, 0, "client1");
        CellState updated = state.withValue("value2", timestamp2);
        
        assertEquals("value2", updated.getValue());
        assertEquals(timestamp2, updated.getTimestamp());
        assertEquals("client1", updated.getClientId());
    }

    @Test
    void testWithFormat() {
        HybridLogicalClock timestamp1 = new HybridLogicalClock(1000, 0, "client1");
        CellState state = new CellState("value", timestamp1, "client1", "user1", "TEXT", null);
        
        HybridLogicalClock timestamp2 = new HybridLogicalClock(2000, 0, "client1");
        CellState formatted = state.withFormat("{\"bold\":true}", timestamp2);
        
        assertEquals("{\"bold\":true}", formatted.getFormat());
        assertEquals(timestamp2, formatted.getTimestamp());
    }

    @Test
    void testValidation() {
        assertThrows(IllegalArgumentException.class, () -> 
            new CellState("value", null, "client1", "user1", "TEXT", null)
        );
        
        assertThrows(IllegalArgumentException.class, () -> {
            HybridLogicalClock timestamp = new HybridLogicalClock(1000, 0, "client1");
            new CellState("value", timestamp, null, "user1", "TEXT", null);
        });
    }
}
