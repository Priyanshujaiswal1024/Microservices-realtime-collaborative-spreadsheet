package com.collab.spreadsheet.common.dto;

/**
 * Cell data types
 */
public enum CellDataType {
    /**
     * Plain text
     */
    TEXT,
    
    /**
     * Numeric value
     */
    NUMBER,
    
    /**
     * Boolean (TRUE/FALSE)
     */
    BOOLEAN,
    
    /**
     * Date value
     */
    DATE,
    
    /**
     * Date and time
     */
    DATETIME,
    
    /**
     * Formula (stored as text, not evaluated in this version)
     */
    FORMULA,
    
    /**
     * Hyperlink
     */
    LINK;

    /**
     * Infer data type from string value
     */
    public static CellDataType inferFromValue(String value) {
        if (value == null || value.isEmpty()) {
            return TEXT;
        }
        
        // Formula
        if (value.startsWith("=")) {
            return FORMULA;
        }
        
        // Boolean
        if ("TRUE".equalsIgnoreCase(value) || "FALSE".equalsIgnoreCase(value)) {
            return BOOLEAN;
        }
        
        // Number
        try {
            Double.parseDouble(value);
            return NUMBER;
        } catch (NumberFormatException e) {
            // Not a number
        }
        
        // Default to text
        return TEXT;
    }
}
