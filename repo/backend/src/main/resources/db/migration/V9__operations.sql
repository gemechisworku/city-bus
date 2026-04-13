-- Phase 8: Observability — system alerts and diagnostic reports

CREATE TABLE system_alerts (
    id BIGSERIAL PRIMARY KEY,
    severity VARCHAR(16) NOT NULL DEFAULT 'INFO',
    source VARCHAR(128) NOT NULL,
    title VARCHAR(255) NOT NULL,
    detail TEXT,
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_by BIGINT REFERENCES users (id),
    acknowledged_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_system_alerts_severity ON system_alerts (severity);
CREATE INDEX idx_system_alerts_ack ON system_alerts (acknowledged);
CREATE INDEX idx_system_alerts_created ON system_alerts (created_at);

CREATE TABLE diagnostic_reports (
    id BIGSERIAL PRIMARY KEY,
    report_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'RUNNING',
    summary TEXT,
    detail TEXT,
    triggered_by BIGINT REFERENCES users (id),
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_diag_reports_type ON diagnostic_reports (report_type);
CREATE INDEX idx_diag_reports_created ON diagnostic_reports (started_at);
