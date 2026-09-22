package com.collab.spreadsheet.common.util;

import java.util.UUID;

/**
 * Utility for generating unique IDs
 */
public final class IdGenerator {
    
    private IdGenerator() {
        // Utility class
    }

    /**
     * Generate a UUID-based ID
     */
    public static String generateId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate a short ID (first 8 characters of UUID)
     * Suitable for display purposes, not for uniqueness guarantees
     */
    public static String generateShortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Generate a prefixed ID
     */
    public static String generateId(String prefix) {
        return prefix + "-" + generateId();
    }

    /**
     * Generate workbook ID
     */
    public static String generateWorkbookId() {
        return "wb-" + generateShortId();
    }

    /**
     * Generate sheet ID
     */
    public static String generateSheetId() {
        return "sh-" + generateShortId();
    }

    /**
     * Generate cell ID
     */
    public static String generateCellId() {
        return "cell-" + generateShortId();
    }

    /**
     * Generate comment ID
     */
    public static String generateCommentId() {
        return "cmt-" + generateShortId();
    }

    /**
     * Generate client ID (for CRDT)
     */
    public static String generateClientId() {
        return "client-" + generateShortId();
    }
}
