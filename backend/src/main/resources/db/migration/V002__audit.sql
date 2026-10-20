CREATE TABLE audit_event (
    id uuid PRIMARY KEY,
    occurred_at timestamptz NOT NULL,
    actor_user_id uuid,
    technical_account_id uuid,
    correlation_id uuid NOT NULL,
    action varchar(100) NOT NULL,
    object_type varchar(100) NOT NULL,
    object_id uuid NOT NULL,
    scope_career_id uuid,
    scope_process_id uuid,
    outcome varchar(30) NOT NULL,
    reason_code varchar(100),
    metadata_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT ck_audit_actor CHECK (num_nonnulls(actor_user_id, technical_account_id) >= 1),
    CONSTRAINT ck_audit_outcome CHECK (outcome IN ('SUCCESS', 'REJECTED', 'FAILED'))
);

CREATE INDEX ix_audit_object_date ON audit_event(object_type, object_id, occurred_at DESC);
CREATE INDEX ix_audit_scope_process_date ON audit_event(scope_process_id, occurred_at DESC);

CREATE FUNCTION reject_audit_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'audit_event is append-only';
END;
$$;

CREATE TRIGGER audit_event_append_only
BEFORE UPDATE OR DELETE ON audit_event
FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();

REVOKE UPDATE, DELETE, TRUNCATE ON audit_event FROM PUBLIC;
