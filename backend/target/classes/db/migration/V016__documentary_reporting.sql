-- Documentary cuts preserve exact model/source/version/review references without closing the process.
CREATE TABLE documentary_snapshot (
    id uuid PRIMARY KEY,
    process_id uuid NOT NULL REFERENCES self_assessment_process(id) ON DELETE RESTRICT,
    career_id uuid NOT NULL REFERENCES career(id) ON DELETE RESTRICT,
    model_version_id uuid NOT NULL REFERENCES model_version(id) ON DELETE RESTRICT,
    process_revision bigint NOT NULL CHECK (process_revision >= 0),
    created_by uuid NOT NULL REFERENCES access_identity(id),
    created_at timestamptz NOT NULL,
    reason varchar(4000) NOT NULL CHECK (char_length(reason) BETWEEN 10 AND 4000),
    formula_version varchar(100) NOT NULL CHECK (char_length(formula_version) >= 1),
    progress_state varchar(30) NOT NULL CHECK (progress_state IN ('CONFIGURED','NOT_CONFIGURED','NOT_APPLICABLE')),
    required_source_total integer NOT NULL CHECK (required_source_total >= 0),
    registered_source_total integer NOT NULL CHECK (registered_source_total BETWEEN 0 AND required_source_total),
    accepted_source_total integer NOT NULL CHECK (accepted_source_total BETWEEN 0 AND registered_source_total),
    sha256 varchar(64) NOT NULL CHECK (sha256 ~ '^[a-f0-9]{64}$'),
    payload_json text NOT NULL CHECK (jsonb_typeof(payload_json::jsonb) = 'object'),
    UNIQUE (process_id, process_revision),
    CHECK ((progress_state = 'CONFIGURED') = (required_source_total > 0))
);
CREATE INDEX ix_documentary_snapshot_history ON documentary_snapshot(process_id,created_at DESC,id DESC);

CREATE FUNCTION reject_documentary_snapshot_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'DOCUMENTARY_SNAPSHOT_IMMUTABLE' USING ERRCODE = '23514';
END;
$$;
CREATE TRIGGER documentary_snapshot_immutable BEFORE UPDATE OR DELETE ON documentary_snapshot
    FOR EACH ROW EXECUTE FUNCTION reject_documentary_snapshot_mutation();
