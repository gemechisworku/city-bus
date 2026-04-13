-- Phase 5: Passenger reservations, check-ins, reminder preferences, messaging

CREATE TABLE passenger_reservations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    schedule_id BIGINT NOT NULL REFERENCES schedules (id) ON DELETE CASCADE,
    stop_id BIGINT NOT NULL REFERENCES stops (id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    reserved_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reservations_user ON passenger_reservations (user_id);
CREATE INDEX idx_reservations_schedule ON passenger_reservations (schedule_id);
CREATE INDEX idx_reservations_status ON passenger_reservations (status);

CREATE TABLE passenger_checkins (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    reservation_id BIGINT REFERENCES passenger_reservations (id) ON DELETE SET NULL,
    stop_id BIGINT NOT NULL REFERENCES stops (id) ON DELETE CASCADE,
    checked_in_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_checkins_user ON passenger_checkins (user_id);
CREATE INDEX idx_checkins_reservation ON passenger_checkins (reservation_id);

CREATE TABLE reminder_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    minutes_before INT NOT NULL DEFAULT 15,
    channel VARCHAR(32) NOT NULL DEFAULT 'IN_APP',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE do_not_disturb_windows (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_dnd_user ON do_not_disturb_windows (user_id);

CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_messages_user ON messages (user_id);
CREATE INDEX idx_messages_read ON messages (user_id, read);

CREATE TABLE message_queue (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL REFERENCES messages (id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL DEFAULT 'QUEUED',
    scheduled_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_message_queue_status ON message_queue (status);

CREATE TABLE message_queue_attempts (
    id BIGSERIAL PRIMARY KEY,
    queue_id BIGINT NOT NULL REFERENCES message_queue (id) ON DELETE CASCADE,
    attempted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    outcome VARCHAR(32) NOT NULL,
    error_message TEXT
);

CREATE INDEX idx_mq_attempts_queue ON message_queue_attempts (queue_id);

CREATE TABLE message_redaction_rules (
    id BIGSERIAL PRIMARY KEY,
    pattern VARCHAR(255) NOT NULL,
    replacement VARCHAR(255) NOT NULL DEFAULT '***',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
