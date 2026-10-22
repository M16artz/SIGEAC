-- D3: policy-backed authorizations, teams and planning.
CREATE TABLE process_team (
    id uuid PRIMARY KEY,
    process_id uuid NOT NULL REFERENCES self_assessment_process(id),
    name varchar(300) NOT NULL,
    process_role varchar(100) NOT NULL,
    valid_from timestamptz NOT NULL,
    valid_until timestamptz NOT NULL,
    created_by uuid NOT NULL,
    created_at timestamptz NOT NULL,
    revision bigint NOT NULL DEFAULT 0,
    CHECK (valid_from < valid_until)
);

CREATE TABLE team_membership (
    id uuid PRIMARY KEY,
    team_id uuid NOT NULL REFERENCES process_team(id),
    user_id uuid NOT NULL,
    role_assignment_id uuid NOT NULL,
    valid_from timestamptz NOT NULL,
    valid_until timestamptz NOT NULL,
    created_by uuid NOT NULL,
    created_at timestamptz NOT NULL,
    CHECK (valid_from < valid_until)
);

ALTER TABLE process_assignment ALTER COLUMN identity_id DROP NOT NULL;
ALTER TABLE process_assignment ADD COLUMN team_id uuid REFERENCES process_team(id);
ALTER TABLE process_assignment ADD CONSTRAINT process_assignment_subject_check
    CHECK ((identity_id IS NOT NULL) <> (team_id IS NOT NULL));

CREATE TABLE process_authorization (
    id uuid PRIMARY KEY,
    process_id uuid NOT NULL REFERENCES self_assessment_process(id),
    decision_type varchar(30) NOT NULL CHECK (decision_type IN ('INITIAL','SUSPENSION','RESUMPTION','REOPENING','TRANSITION')),
    authority_assignment_id uuid NOT NULL,
    legal_basis varchar(4000) NOT NULL,
    document_reference varchar(2000) NOT NULL,
    reason varchar(4000) NOT NULL,
    decided_by uuid NOT NULL,
    decided_at timestamptz NOT NULL
);

ALTER TABLE process_transition ADD COLUMN authority_assignment_id uuid;
ALTER TABLE process_transition ADD COLUMN legal_basis varchar(4000);
ALTER TABLE process_transition ADD COLUMN document_reference varchar(2000);

CREATE TABLE process_activity (
    id uuid PRIMARY KEY,
    process_id uuid NOT NULL REFERENCES self_assessment_process(id),
    indicator_id uuid NOT NULL,
    required_source_id uuid,
    assignment_id uuid NOT NULL REFERENCES process_assignment(id),
    title varchar(300) NOT NULL,
    due_at timestamptz NOT NULL,
    status varchar(30) NOT NULL,
    created_by uuid NOT NULL,
    created_at timestamptz NOT NULL,
    revision bigint NOT NULL DEFAULT 0,
    snapshot_json jsonb NOT NULL
);

CREATE TABLE process_activity_dependency (
    activity_id uuid NOT NULL REFERENCES process_activity(id),
    depends_on_activity_id uuid NOT NULL REFERENCES process_activity(id),
    PRIMARY KEY (activity_id, depends_on_activity_id),
    CHECK (activity_id <> depends_on_activity_id)
);

CREATE TABLE process_activity_revision (
    id uuid PRIMARY KEY,
    activity_id uuid NOT NULL REFERENCES process_activity(id),
    revision bigint NOT NULL,
    snapshot_json jsonb NOT NULL,
    recorded_at timestamptz NOT NULL
);

CREATE INDEX ix_process_team_scope ON process_team(process_id, valid_from, valid_until);
CREATE INDEX ix_team_membership_team ON team_membership(team_id, user_id);
CREATE INDEX ix_process_activity_process ON process_activity(process_id, due_at);
CREATE INDEX ix_process_authorization_process ON process_authorization(process_id, decided_at);
