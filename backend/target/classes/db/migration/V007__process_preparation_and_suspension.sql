-- Retain existing histories; do not invent authorizations for legacy active processes.
ALTER TABLE self_assessment_process DROP CONSTRAINT self_assessment_process_state_check;
ALTER TABLE self_assessment_process ADD CONSTRAINT self_assessment_process_state_check
    CHECK (state IN ('DRAFT','PREPARATION','ACTIVE','SUSPENDED','DOCUMENTARY_CLOSED','CLOSING','CLOSED','REOPENED'));
UPDATE self_assessment_process
SET state = 'PREPARATION', snapshot_json = jsonb_set(snapshot_json, '{state}', '"PREPARATION"'::jsonb)
WHERE state = 'DRAFT';
