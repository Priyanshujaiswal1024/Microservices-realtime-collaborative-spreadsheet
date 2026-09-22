package com.collab.spreadsheet.common.config;

/**
 * Constants for Kafka topic names
 */
public final class KafkaTopics {
    
    private KafkaTopics() {
        // Utility class
    }

    /**
     * Topic for real-time cell edit events
     * Partition key: sheetId
     */
    public static final String CELL_EDITS = "cell-edits";

    /**
     * Topic for workbook/sheet lifecycle events
     * Partition key: workbookId
     */
    public static final String SHEET_LIFECYCLE = "sheet-lifecycle";

    /**
     * Topic for comment events
     * Partition key: workbookId
     */
    public static final String COMMENTS = "comments";

    /**
     * Topic for notification events
     * Partition key: userId
     */
    public static final String NOTIFICATIONS = "notifications";

    /**
     * Topic for user lifecycle and OTP events
     * Partition key: email
     */
    public static final String USER_EVENTS = "user-events";
}
