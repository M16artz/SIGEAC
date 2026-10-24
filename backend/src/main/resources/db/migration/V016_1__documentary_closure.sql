-- Forward migration after V016: sealed documentary phase closure, independent of academic workflows.
CREATE UNIQUE INDEX ux_documentary_snapshot_process ON documentary_snapshot(id,process_id);
CREATE TABLE documentary_closure (
    id uuid PRIMARY KEY,
    process_id uuid NOT NULL REFERENCES self_assessment_process(id) ON DELETE RESTRICT,
    career_id uuid NOT NULL REFERENCES career(id) ON DELETE RESTRICT,
    snapshot_id uuid NOT NULL UNIQUE,
    process_revision bigint NOT NULL CHECK (process_revision >= 0),
    policy_version varchar(100) NOT NULL CHECK (policy_version = 'DOCUMENTARY_STRICT_V1'),
    closed_by uuid NOT NULL REFERENCES access_identity(id),
    closed_at timestamptz NOT NULL,
    reason varchar(2000) NOT NULL CHECK (char_length(reason) BETWEEN 10 AND 2000),
    sha256 varchar(64) NOT NULL CHECK (sha256 ~ '^[a-f0-9]{64}$'),
    payload_json text NOT NULL CHECK (jsonb_typeof(payload_json::jsonb) = 'object'),
    UNIQUE (process_id,process_revision),
    FOREIGN KEY (snapshot_id,process_id) REFERENCES documentary_snapshot(id,process_id) ON DELETE RESTRICT
);
CREATE INDEX ix_documentary_closure_history ON documentary_closure(process_id,closed_at DESC,id DESC);
CREATE TRIGGER documentary_closure_immutable BEFORE UPDATE OR DELETE ON documentary_closure
    FOR EACH ROW EXECUTE FUNCTION reject_documentary_snapshot_mutation();
