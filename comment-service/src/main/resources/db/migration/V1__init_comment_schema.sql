-- V1__init_comment_schema.sql
-- Cell comment threads and replies schema

CREATE TABLE IF NOT EXISTS comment_threads (
    id VARCHAR(36) PRIMARY KEY,
    workbook_id VARCHAR(36) NOT NULL,
    sheet_id VARCHAR(36) NOT NULL,
    cell_row INT NOT NULL,
    cell_col INT NOT NULL,
    author_id VARCHAR(36) NOT NULL,
    initial_content TEXT NOT NULL,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_by VARCHAR(36),
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS comment_replies (
    id VARCHAR(36) PRIMARY KEY,
    thread_id VARCHAR(36) NOT NULL REFERENCES comment_threads(id) ON DELETE CASCADE,
    author_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_comment_workbook ON comment_threads (workbook_id);
CREATE INDEX IF NOT EXISTS idx_comment_sheet_cell ON comment_threads (sheet_id, cell_row, cell_col);
CREATE INDEX IF NOT EXISTS idx_comment_replies_thread ON comment_replies (thread_id, created_at ASC);
