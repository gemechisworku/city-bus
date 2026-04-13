-- Optional sequential branching: a task may depend on another task in the same instance being APPROVED first.

ALTER TABLE workflow_tasks
    ADD COLUMN predecessor_task_id BIGINT NULL REFERENCES workflow_tasks (id) ON DELETE SET NULL;

CREATE INDEX idx_workflow_tasks_predecessor ON workflow_tasks (predecessor_task_id);
