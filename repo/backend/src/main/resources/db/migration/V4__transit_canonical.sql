CREATE TABLE routes (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE route_versions (
    id BIGSERIAL PRIMARY KEY,
    route_id BIGINT NOT NULL REFERENCES routes (id) ON DELETE CASCADE,
    version_number INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    effective_from DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (route_id, version_number)
);

CREATE INDEX idx_route_versions_route ON route_versions (route_id);

CREATE TABLE stops (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE stop_versions (
    id BIGSERIAL PRIMARY KEY,
    stop_id BIGINT NOT NULL REFERENCES stops (id) ON DELETE CASCADE,
    version_number INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    effective_from DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (stop_id, version_number)
);

CREATE INDEX idx_stop_versions_stop ON stop_versions (stop_id);

CREATE TABLE route_stops (
    id BIGSERIAL PRIMARY KEY,
    route_version_id BIGINT NOT NULL REFERENCES route_versions (id) ON DELETE CASCADE,
    stop_version_id BIGINT NOT NULL REFERENCES stop_versions (id) ON DELETE CASCADE,
    stop_sequence INT NOT NULL,
    UNIQUE (route_version_id, stop_sequence)
);

CREATE INDEX idx_route_stops_route_version ON route_stops (route_version_id);

CREATE TABLE schedules (
    id BIGSERIAL PRIMARY KEY,
    route_version_id BIGINT NOT NULL REFERENCES route_versions (id) ON DELETE CASCADE,
    trip_code VARCHAR(64),
    departure_time TIME NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_schedules_route_version ON schedules (route_version_id);

CREATE TABLE source_import_jobs (
    id BIGSERIAL PRIMARY KEY,
    source_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    artifact_name VARCHAR(255),
    row_count INT,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_source_import_jobs_created ON source_import_jobs (created_at);

CREATE TABLE field_mappings (
    id BIGSERIAL PRIMARY KEY,
    template_name VARCHAR(128) NOT NULL,
    source_field VARCHAR(128) NOT NULL,
    target_field VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (template_name, source_field)
);

INSERT INTO field_mappings (template_name, source_field, target_field)
VALUES
    ('DEFAULT_V1', 'routes[].routeCode', 'routes.code'),
    ('DEFAULT_V1', 'routes[].name', 'route_versions.name'),
    ('DEFAULT_V1', 'routes[].stops[].stopCode', 'stops.code'),
    ('DEFAULT_V1', 'routes[].stops[].name', 'stop_versions.name')
ON CONFLICT (template_name, source_field) DO NOTHING;
