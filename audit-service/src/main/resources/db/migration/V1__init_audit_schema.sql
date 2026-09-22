-- V1__init_audit_schema.sql
-- Audit events and snapshot checkpoints schema

CREATE TABLE IF NOT EXISTS audit_events (
    id VARCHAR(36) PRIMARY KEY,
    event_id VARCHAR(100) UNIQUE,
    workbook_id VARCHAR(36) NOT NULL,
    sheet_id VARCHAR(36),
    cell_row INT,
    cell_col INT,
    event_type VARCHAR(50) NOT NULL,
    actor_id VARCHAR(36) NOT NULL,
    payload TEXT,
    previous_value TEXT,
    new_value TEXT,
    hlc_timestamp VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS snapshots (
    id VARCHAR(36) PRIMARY KEY,
    workbook_id VARCHAR(36) NOT NULL,
    label VARCHAR(255) NOT NULL,
    description TEXT,
    created_by VARCHAR(36) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    snapshot_data TEXT NOT NULL
);

-- Indexes for lightning fast timeline queries
CREATE INDEX IF NOT EXISTS idx_audit_workbook_created ON audit_events (workbook_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_sheet_created ON audit_events (sheet_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_actor ON audit_events (actor_id);
CREATE INDEX IF NOT EXISTS idx_snapshots_workbook ON snapshots (workbook_id, created_at DESC);
