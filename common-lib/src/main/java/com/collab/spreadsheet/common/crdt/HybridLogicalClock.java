package com.collab.spreadsheet.common.crdt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.time.Instant;

/**
 * Hybrid Logical Clock (HLC) implementation for CRDT timestamp ordering.
 * 
 * HLC combines physical time with logical counters to provide causally-ordered
 * timestamps that work across distributed systems without perfect clock synchronization.
 * 
 * Algorithm:
 * - When creating a new timestamp: use max(local_physical_time, last_hlc.physical) + logical_counter
 * - When receiving a remote timestamp: merge it with local time to maintain causality
 * 
 * Comparison: HLC1 > HLC2 if:
 *   1. physical1 > physical2, OR
 *   2. physical1 == physical2 AND logical1 > logical2, OR
 *   3. physical1 == physical2 AND logical1 == logical2 AND clientId1 > clientId2
 */
@Getter
@ToString
@EqualsAndHashCode
public class HybridLogicalClock implements Comparable<HybridLogicalClock>, Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Physical time component (milliseconds since epoch)
     */
    private final long physicalTime;
    
    /**
     * Logical counter for ordering events at the same physical time
     */
    private final long logicalCounter;
    
    /**
     * Client/node identifier for deterministic tie-breaking
     */
    private final String clientId;

    @JsonCreator
    public HybridLogicalClock(
            @JsonProperty("physicalTime") long physicalTime,
            @JsonProperty("logicalCounter") long logicalCounter,
            @JsonProperty("clientId") String clientId) {
        
        if (physicalTime < 0) {
            throw new IllegalArgumentException("Physical time cannot be negative");
        }
        if (logicalCounter < 0) {
            throw new IllegalArgumentException("Logical counter cannot be negative");
        }
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("Client ID cannot be null or blank");
        }
        
        this.physicalTime = physicalTime;
        this.logicalCounter = logicalCounter;
        this.clientId = clientId;
    }

    /**
     * Create a new HLC timestamp for a local event
     * 
     * @param clientId This client's unique identifier
     * @param lastHlc The last HLC timestamp used (may be null for first event)
     * @return New HLC timestamp
     */
    public static HybridLogicalClock now(String clientId, HybridLogicalClock lastHlc) {
        long currentPhysicalTime = System.currentTimeMillis();
        
        if (lastHlc == null) {
            return new HybridLogicalClock(currentPhysicalTime, 0, clientId);
        }
        
        // If physical time has advanced, reset logical counter
        if (currentPhysicalTime > lastHlc.physicalTime) {
            return new HybridLogicalClock(currentPhysicalTime, 0, clientId);
        }
        
        // Physical time hasn't advanced - increment logical counter
        return new HybridLogicalClock(
            lastHlc.physicalTime,
            lastHlc.logicalCounter + 1,
            clientId
        );
    }

    /**
     * Merge incoming remote HLC with local state to maintain causality
     * 
     * @param clientId This client's unique identifier
     * @param lastHlc This client's last HLC timestamp
     * @param remoteHlc Remote HLC timestamp received
     * @return New HLC timestamp that preserves causality
     */
    public static HybridLogicalClock merge(String clientId, HybridLogicalClock lastHlc, HybridLogicalClock remoteHlc) {
        long currentPhysicalTime = System.currentTimeMillis();
        
        // Find the maximum physical time across all sources
        long maxPhysicalTime = Math.max(
            currentPhysicalTime,
            Math.max(
                lastHlc != null ? lastHlc.physicalTime : 0,
                remoteHlc.physicalTime
            )
        );
        
        long newLogicalCounter = 0;
        
        // If we're using the remote's physical time, increment its logical counter
        if (maxPhysicalTime == remoteHlc.physicalTime) {
            newLogicalCounter = remoteHlc.logicalCounter + 1;
        }
        
        // If we're using our last HLC's physical time, increment its logical counter
        if (lastHlc != null && maxPhysicalTime == lastHlc.physicalTime) {
            newLogicalCounter = Math.max(newLogicalCounter, lastHlc.logicalCounter + 1);
        }
        
        return new HybridLogicalClock(maxPhysicalTime, newLogicalCounter, clientId);
    }

    /**
     * Compare two HLC timestamps for ordering
     * 
     * @param other Other HLC timestamp
     * @return Negative if this < other, 0 if equal, positive if this > other
     */
    @Override
    public int compareTo(HybridLogicalClock other) {
        if (other == null) {
            return 1;
        }
        
        // Compare physical time first
        int physicalCompare = Long.compare(this.physicalTime, other.physicalTime);
        if (physicalCompare != 0) {
            return physicalCompare;
        }
        
        // Physical times equal - compare logical counters
        int logicalCompare = Long.compare(this.logicalCounter, other.logicalCounter);
        if (logicalCompare != 0) {
            return logicalCompare;
        }
        
        // Both physical and logical equal - tie-break with client ID
        return this.clientId.compareTo(other.clientId);
    }

    /**
     * Check if this HLC happened before another (strict causality)
     */
    public boolean happenedBefore(HybridLogicalClock other) {
        return this.compareTo(other) < 0;
    }

    /**
     * Check if this HLC happened after another
     */
    public boolean happenedAfter(HybridLogicalClock other) {
        return this.compareTo(other) > 0;
    }

    /**
     * Get the physical time as an Instant
     */
    public Instant toInstant() {
        return Instant.ofEpochMilli(physicalTime);
    }

    /**
     * Create HLC from string representation (for debugging/testing)
     * Format: "physicalTime:logicalCounter:clientId"
     */
    public static HybridLogicalClock fromString(String str) {
        String[] parts = str.split(":", 3);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid HLC string format: " + str);
        }
        return new HybridLogicalClock(
            Long.parseLong(parts[0]),
            Long.parseLong(parts[1]),
            parts[2]
        );
    }

    /**
     * Convert to compact string representation
     */
    public String toCompactString() {
        return physicalTime + ":" + logicalCounter + ":" + clientId;
    }
}
