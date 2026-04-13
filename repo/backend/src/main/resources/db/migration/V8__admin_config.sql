-- Phase 7: Administrator configuration console

CREATE TABLE cleaning_rule_sets (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL UNIQUE,
    description TEXT,
    field_target VARCHAR(128) NOT NULL,
    rule_type VARCHAR(32) NOT NULL DEFAULT 'REGEX',
    pattern VARCHAR(512) NOT NULL,
    replacement VARCHAR(512) NOT NULL DEFAULT '',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cleaning_rules_enabled ON cleaning_rule_sets (enabled);

CREATE TABLE cleaning_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    rule_id BIGINT NOT NULL REFERENCES cleaning_rule_sets (id) ON DELETE CASCADE,
    original_value TEXT NOT NULL,
    cleaned_value TEXT NOT NULL,
    applied_by BIGINT REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cleaning_audit_rule ON cleaning_audit_logs (rule_id);
CREATE INDEX idx_cleaning_audit_created ON cleaning_audit_logs (created_at);

CREATE TABLE field_standard_dictionaries (
    id BIGSERIAL PRIMARY KEY,
    field_name VARCHAR(128) NOT NULL,
    canonical_value VARCHAR(255) NOT NULL,
    aliases TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (field_name, canonical_value)
);

CREATE INDEX idx_dictionaries_field ON field_standard_dictionaries (field_name);
