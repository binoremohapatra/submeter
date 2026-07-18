CREATE TABLE import_history (
    id UUID PRIMARY KEY,
    dataset VARCHAR(255) NOT NULL,
    mode VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    duration_ms BIGINT,
    customers INT DEFAULT 0,
    subscriptions INT DEFAULT 0,
    invoices INT DEFAULT 0,
    payments INT DEFAULT 0,
    usage_events INT DEFAULT 0,
    batch_count INT DEFAULT 0,
    imported_by VARCHAR(255),
    error_message TEXT,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_import_history_started_at ON import_history(started_at DESC);
