-- In-app notification message templates (distinct from import field_mappings templates)

CREATE TABLE notification_templates (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    subject VARCHAR(255) NOT NULL,
    body_template TEXT NOT NULL,
    channel VARCHAR(32) NOT NULL DEFAULT 'IN_APP',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notification_templates_enabled ON notification_templates (enabled);

INSERT INTO notification_templates (code, subject, body_template, channel, enabled)
VALUES (
    'RESERVATION_CONFIRM',
    'Reservation confirmed',
    'Your reservation for trip {{tripCode}} at stop {{stopCode}} is confirmed.',
    'IN_APP',
    TRUE
) ON CONFLICT (code) DO NOTHING;
