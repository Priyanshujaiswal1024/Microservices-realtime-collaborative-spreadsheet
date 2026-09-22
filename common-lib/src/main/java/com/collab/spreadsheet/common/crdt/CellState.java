package com.collab.spreadsheet.common.crdt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 * CRDT state for a single cell using LWW-Element-Register
 * 
 * This represents the state of a cell in the collaborative spreadsheet.
 * The HLC timestamp determines which update wins in case of conflicts.
 */
@Getter
@ToString
@EqualsAndHashCode
public class CellState implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Cell value (can be null for empty cells)
     */
    private final String value;
    
    /**
     * Hybrid Logical Clock timestamp for ordering
     */
    private final HybridLogicalClock timestamp;
    
    /**
     * Client ID that created this state
     */
    private final String clientId;
    
    /**
     * User ID who made the change
     */
    private final String userId;
    
    /**
     * Cell data type (TEXT, NUMBER, BOOLEAN, FORMULA, etc.)
     */
    private final String dataType;
    
    /**
     * Cell format metadata (JSON string: bold, italic, color, numberFormat, etc.)
     */
    private final String format;

    @JsonCreator
    public CellState(
            @JsonProperty("value") String value,
            @JsonProperty("timestamp") HybridLogicalClock timestamp,
            @JsonProperty("clientId") String clientId,
            @JsonProperty("userId") String userId,
            @JsonProperty("dataType") String dataType,
            @JsonProperty("format") Object format) {
        
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("Client ID cannot be null or blank");
        }
        
        this.value = value;
        this.timestamp = timestamp;
        this.clientId = clientId;
        this.userId = userId;
        this.dataType = dataType != null ? dataType : "TEXT";
        if (format == null) {
            this.format = null;
        } else if (format instanceof String) {
            this.format = (String) format;
        } else if (format instanceof com.fasterxml.jackson.databind.JsonNode) {
            com.fasterxml.jackson.databind.JsonNode node = (com.fasterxml.jackson.databind.JsonNode) format;
            this.format = node.isTextual() ? node.asText() : node.toString();
        } else {
            this.format = format.toString();
        }
    }

    /**
     * Create a new CellState with updated value
     */
    public CellState withValue(String newValue, HybridLogicalClock newTimestamp) {
        return new CellState(newValue, newTimestamp, this.clientId, this.userId, this.dataType, this.format);
    }

    /**
     * Create a new CellState with updated format
     */
    public CellState withFormat(String newFormat, HybridLogicalClock newTimestamp) {
        return new CellState(this.value, newTimestamp, this.clientId, this.userId, this.dataType, newFormat);
    }

    /**
     * Check if this state is newer than another based on HLC comparison
     */
    public boolean isNewerThan(CellState other) {
        if (other == null) {
            return true;
        }
        return this.timestamp.happenedAfter(other.timestamp);
    }

    /**
     * Merge two CellStates using LWW strategy
     * Returns the state with the newer timestamp
     */
    public static CellState merge(CellState current, CellState incoming) {
        if (current == null) {
            return incoming;
        }
        if (incoming == null) {
            return current;
        }
        
        // LWW: Keep the one with higher timestamp
        return incoming.isNewerThan(current) ? incoming : current;
    }

    /**
     * Create an empty cell state
     */
    public static CellState empty(String clientId, String userId) {
        HybridLogicalClock timestamp = HybridLogicalClock.now(clientId, null);
        return new CellState(null, timestamp, clientId, userId, "TEXT", null);
    }

    /**
     * Check if cell is empty
     */
    public boolean isEmpty() {
        return value == null || value.isEmpty();
    }
}
