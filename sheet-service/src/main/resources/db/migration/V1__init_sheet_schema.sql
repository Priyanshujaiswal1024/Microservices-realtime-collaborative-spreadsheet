-- Initialize Sheet Service Database Schema
CREATE TABLE IF NOT EXISTS workbooks (
    id VARCHAR(36) PRIMARY KEY,
    owner_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_workbook_owner ON workbooks(owner_id);

CREATE TABLE IF NOT EXISTS sheets (
    id VARCHAR(36) PRIMARY KEY,
    workbook_id VARCHAR(36) NOT NULL REFERENCES workbooks(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    position INT NOT NULL DEFAULT 0,
    row_count INT NOT NULL DEFAULT 100,
    col_count INT NOT NULL DEFAULT 26,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sheet_workbook ON sheets(workbook_id);

CREATE TABLE IF NOT EXISTS cells (
    id VARCHAR(36) PRIMARY KEY,
    sheet_id VARCHAR(36) NOT NULL REFERENCES sheets(id) ON DELETE CASCADE,
    cell_row INT NOT NULL,
    cell_col INT NOT NULL,
    cell_value TEXT,
    data_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    format TEXT,
    last_modified_ts VARCHAR(100),
    last_modified_by VARCHAR(36),
    version BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_sheet_cell UNIQUE(sheet_id, cell_row, cell_col)
);

CREATE INDEX IF NOT EXISTS idx_cell_sheet_coords ON cells(sheet_id, cell_row, cell_col);

CREATE TABLE IF NOT EXISTS permissions (
    id VARCHAR(36) PRIMARY KEY,
    workbook_id VARCHAR(36) NOT NULL REFERENCES workbooks(id) ON DELETE CASCADE,
    user_id VARCHAR(36) NOT NULL,
    user_email VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'VIEWER',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_workbook_user_permission UNIQUE(workbook_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_permission_user ON permissions(user_id);
CREATE INDEX IF NOT EXISTS idx_permission_workbook ON permissions(workbook_id);

CREATE TABLE IF NOT EXISTS protected_ranges (
    id VARCHAR(36) PRIMARY KEY,
    sheet_id VARCHAR(36) NOT NULL REFERENCES sheets(id) ON DELETE CASCADE,
    name VARCHAR(100),
    start_row INT NOT NULL,
    start_col INT NOT NULL,
    end_row INT NOT NULL,
    end_col INT NOT NULL,
    allowed_user_ids TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_protected_range_sheet ON protected_ranges(sheet_id);
