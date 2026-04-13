-- Phase 10 enhancements: pinyin search, workflow branching, reminder default fix

ALTER TABLE route_versions ADD COLUMN IF NOT EXISTS search_pinyin VARCHAR(512);
ALTER TABLE route_versions ADD COLUMN IF NOT EXISTS search_initials VARCHAR(128);

ALTER TABLE stop_versions ADD COLUMN IF NOT EXISTS search_pinyin VARCHAR(512);
ALTER TABLE stop_versions ADD COLUMN IF NOT EXISTS search_initials VARCHAR(128);

ALTER TABLE workflow_definitions ADD COLUMN IF NOT EXISTS approval_mode VARCHAR(32) NOT NULL DEFAULT 'ALL';
ALTER TABLE workflow_definitions ADD COLUMN IF NOT EXISTS required_approvals INT NOT NULL DEFAULT 1;

ALTER TABLE reminder_preferences ALTER COLUMN minutes_before SET DEFAULT 10;
