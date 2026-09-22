package com.collab.spreadsheet.common.crdt;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Hybrid Logical Clock implementation
 */
class HybridLogicalClockTest {

    @Test
    void testCreateNow() {
        HybridLogicalClock hlc = HybridLogicalClock.now("client1", null);
        
        assertNotNull(hlc);
        assertEquals("client1", hlc.getClientId());
        assertEquals(0, hlc.getLogicalCounter());
        assertTrue(hlc.getPhysicalTime() > 0);
    }

    @Test
    void testCreateNowWithPreviousHlc() throws InterruptedException {
        HybridLogicalClock hlc1 = HybridLogicalClock.now("client1", null);
        
        // Wait a bit to ensure time advances
        Thread.sleep(10);
        
        HybridLogicalClock hlc2 = HybridLogicalClock.now("client1", hlc1);
        
        assertTrue(hlc2.getPhysicalTime() >= hlc1.getPhysicalTime());
        if (hlc2.getPhysicalTime() == hlc1.getPhysicalTime()) {
            // If time didn't advance, logical counter should increment
            assertEquals(hlc1.getLogicalCounter() + 1, hlc2.getLogicalCounter());
        } else {
            // If time advanced, logical counter should reset
            assertEquals(0, hlc2.getLogicalCounter());
        }
    }

    @Test
    void testComparison() {
        long currentTime = System.currentTimeMillis();
        
        HybridLogicalClock hlc1 = new HybridLogicalClock(currentTime, 0, "client1");
        HybridLogicalClock hlc2 = new HybridLogicalClock(currentTime, 1, "client1");
        HybridLogicalClock hlc3 = new HybridLogicalClock(currentTime + 1, 0, "client1");
        
        // hlc1 < hlc2 (same physical time, lower logical counter)
        assertTrue(hlc1.compareTo(hlc2) < 0);
        assertTrue(hlc1.happenedBefore(hlc2));
        
        // hlc2 < hlc3 (lower physical time)
        assertTrue(hlc2.compareTo(hlc3) < 0);
        assertTrue(hlc2.happenedBefore(hlc3));
        
        // hlc1 < hlc3
        assertTrue(hlc1.compareTo(hlc3) < 0);
    }

    @Test
    void testComparisonWithClientIdTieBreaker() {
        long currentTime = System.currentTimeMillis();
        
        HybridLogicalClock hlc1 = new HybridLogicalClock(currentTime, 0, "clientA");
        HybridLogicalClock hlc2 = new HybridLogicalClock(currentTime, 0, "clientB");
        
        // Same physical time and logical counter - tie-break with client ID
        assertTrue(hlc1.compareTo(hlc2) < 0);
        assertEquals("clientA", hlc1.getClientId());
        assertEquals("clientB", hlc2.getClientId());
    }

    @Test
    void testMerge() {
        long currentTime = System.currentTimeMillis();
        
        HybridLogicalClock local = new HybridLogicalClock(currentTime, 5, "client1");
        HybridLogicalClock remote = new HybridLogicalClock(currentTime + 100, 3, "client2");
        
        HybridLogicalClock merged = HybridLogicalClock.merge("client1", local, remote);
        
        // Should use the maximum physical time
        assertEquals(currentTime + 100, merged.getPhysicalTime());
        
        // Should increment the logical counter from remote
        assertEquals(4, merged.getLogicalCounter());
        
        // Should use our client ID
        assertEquals("client1", merged.getClientId());
    }

    @Test
    void testMergeWithNullLocal() {
        long currentTime = System.currentTimeMillis();
        HybridLogicalClock remote = new HybridLogicalClock(currentTime, 5, "client2");
        
        HybridLogicalClock merged = HybridLogicalClock.merge("client1", null, remote);
        
        assertNotNull(merged);
        assertTrue(merged.getPhysicalTime() >= remote.getPhysicalTime());
        assertEquals("client1", merged.getClientId());
    }

    @Test
    void testFromStringAndToCompactString() {
        HybridLogicalClock hlc = new HybridLogicalClock(1234567890L, 42, "client-abc");
        
        String compactStr = hlc.toCompactString();
        assertEquals("1234567890:42:client-abc", compactStr);
        
        HybridLogicalClock parsed = HybridLogicalClock.fromString(compactStr);
        assertEquals(hlc, parsed);
    }

    @Test
    void testValidation() {
        assertThrows(IllegalArgumentException.class, () -> 
            new HybridLogicalClock(-1, 0, "client1")
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new HybridLogicalClock(100, -1, "client1")
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new HybridLogicalClock(100, 0, null)
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new HybridLogicalClock(100, 0, "")
        );
    }

    @Test
    void testToInstant() {
        long timestamp = System.currentTimeMillis();
        HybridLogicalClock hlc = new HybridLogicalClock(timestamp, 0, "client1");
        
        assertEquals(timestamp, hlc.toInstant().toEpochMilli());
    }

    @Test
    void testHappenedAfter() {
        long currentTime = System.currentTimeMillis();
        HybridLogicalClock hlc1 = new HybridLogicalClock(currentTime, 0, "client1");
        HybridLogicalClock hlc2 = new HybridLogicalClock(currentTime + 1, 0, "client1");
        
        assertTrue(hlc2.happenedAfter(hlc1));
        assertFalse(hlc1.happenedAfter(hlc2));
    }
}
