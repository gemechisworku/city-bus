-- Phase 6: Dispatcher workflow platform

CREATE TABLE workflow_definitions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL UNIQUE,
    description TEXT,
    initial_status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO workflow_definitions (name, description)
VALUES
    ('ROUTE_CHANGE', 'Approve route modifications before publishing'),
    ('SCHEDULE_CHANGE', 'Approve schedule modifications before publishing'),
    ('PASSENGER_COMPLAINT', 'Handle and resolve passenger complaints')
ON CONFLICT (name) DO NOTHING;

CREATE TABLE workflow_instances (
    id BIGSERIAL PRIMARY KEY,
    definition_id BIGINT NOT NULL REFERENCES workflow_definitions (id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    created_by BIGINT NOT NULL REFERENCES users (id),
    assigned_to BIGINT REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_workflow_instances_status ON workflow_instances (status);
CREATE INDEX idx_workflow_instances_assigned ON workflow_instances (assigned_to);
CREATE INDEX idx_workflow_instances_created_by ON workflow_instances (created_by);

CREATE TABLE workflow_tasks (
    id BIGSERIAL PRIMARY KEY,
    instance_id BIGINT NOT NULL REFERENCES workflow_instances (id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    assigned_to BIGINT REFERENCES users (id),
    decided_by BIGINT REFERENCES users (id),
    decision_note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_workflow_tasks_instance ON workflow_tasks (instance_id);
CREATE INDEX idx_workflow_tasks_assigned ON workflow_tasks (assigned_to);
CREATE INDEX idx_workflow_tasks_status ON workflow_tasks (status);

CREATE TABLE workflow_escalations (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES workflow_tasks (id) ON DELETE CASCADE,
    escalated_to BIGINT NOT NULL REFERENCES users (id),
    reason TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_workflow_escalations_task ON workflow_escalations (task_id);
