package com.collab.spreadsheet.common.dto;

/**
 * Workbook-level permission roles (Google Sheets-style)
 */
public enum PermissionRole {
    /**
     * Owner - full control including delete and sharing
     */
    OWNER,
    
    /**
     * Editor - can edit cells and add comments
     */
    EDITOR,
    
    /**
     * Commenter - can only add comments, no cell edits
     */
    COMMENTER,
    
    /**
     * Viewer - read-only access
     */
    VIEWER;

    /**
     * Check if role has edit permission
     */
    public boolean canEdit() {
        return this == OWNER || this == EDITOR;
    }

    /**
     * Check if role can comment
     */
    public boolean canComment() {
        return this == OWNER || this == EDITOR || this == COMMENTER;
    }

    /**
     * Check if role can share/manage permissions
     */
    public boolean canManagePermissions() {
        return this == OWNER;
    }

    /**
     * Check if role can delete workbook
     */
    public boolean canDelete() {
        return this == OWNER;
    }
}
