CREATE TABLE ranking_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(64) NOT NULL UNIQUE,
    route_weight NUMERIC(10, 4) NOT NULL DEFAULT 1.0,
    stop_weight NUMERIC(10, 4) NOT NULL DEFAULT 1.0,
    popularity_weight NUMERIC(10, 4) NOT NULL DEFAULT 0.5,
    max_suggestions INT NOT NULL DEFAULT 8,
    max_results INT NOT NULL DEFAULT 20,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO ranking_config (config_key)
VALUES ('DEFAULT');

CREATE TABLE stop_popularity_metrics (
    stop_id BIGINT PRIMARY KEY REFERENCES stops (id) ON DELETE CASCADE,
    impression_count BIGINT NOT NULL DEFAULT 0,
    selection_count BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE search_events (
    id BIGSERIAL PRIMARY KEY,
    query_text VARCHAR(255) NOT NULL,
    scope VARCHAR(16) NOT NULL,
    result_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_search_events_created ON search_events (created_at);
