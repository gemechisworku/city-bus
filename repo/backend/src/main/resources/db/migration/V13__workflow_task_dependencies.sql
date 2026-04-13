-- Multiple predecessors per task (join / conditional gating). Migrates single predecessor_task_id.

CREATE TABLE workflow_task_dependencies (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES workflow_tasks (id) ON DELETE CASCADE,
    depends_on_task_id BIGINT NOT NULL REFERENCES workflow_tasks (id) ON DELETE CASCADE,
    CONSTRAINT workflow_task_dep_check CHECK (task_id <> depends_on_task_id),
    CONSTRAINT workflow_task_dep_unique UNIQUE (task_id, depends_on_task_id)
);

CREATE INDEX idx_workflow_task_deps_task ON workflow_task_dependencies (task_id);
CREATE INDEX idx_workflow_task_deps_depends ON workflow_task_dependencies (depends_on_task_id);

INSERT INTO workflow_task_dependencies (task_id, depends_on_task_id)
SELECT id, predecessor_task_id
FROM workflow_tasks
WHERE predecessor_task_id IS NOT NULL;

ALTER TABLE workflow_tasks DROP COLUMN predecessor_task_id;
