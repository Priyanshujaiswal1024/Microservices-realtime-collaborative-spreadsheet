package com.collab.spreadsheet.common.dto;

/**
 * Workbook visibility settings
 */
public enum WorkbookVisibility {
    /**
     * Only owner and explicitly shared users can access
     */
    PRIVATE,
    
    /**
     * Anyone with the link can view (if they have at least VIEWER role)
     */
    LINK_SHARED,
    
    /**
     * Publicly accessible to all authenticated users
     */
    PUBLIC
}
