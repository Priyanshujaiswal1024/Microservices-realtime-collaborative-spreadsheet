package com.collab.spreadsheet.common.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all Kafka events in the system
 * 
 * Provides common fields for event identification, deduplication, and ordering
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = CellEditEvent.class, name = "CELL_EDIT"),
    @JsonSubTypes.Type(value = SheetLifecycleEvent.class, name = "SHEET_LIFECYCLE"),
    @JsonSubTypes.Type(value = CommentEvent.class, name = "COMMENT"),
    @JsonSubTypes.Type(value = NotificationEvent.class, name = "NOTIFICATION")
})
public abstract class BaseEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Unique event identifier (for deduplication)
     */
    private String eventId;
    
    /**
     * Timestamp when event was created
     */
    private Instant timestamp;
    
    /**
     * User ID who triggered the event
     */
    private String actorId;
    
    /**
     * Event version for schema evolution
     */
    private String version;

    /**
     * Initialize common fields
     */
    protected void initializeBaseFields(String actorId) {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.actorId = actorId;
        this.version = "1.0";
    }

    /**
     * Get event type for routing/filtering
     */
    public abstract String getEventType();
}
