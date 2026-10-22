-- V012: explicit source coverage during draft submission and process-level
-- applicability decisions for indicators / required sources (P07-P08).
CREATE TABLE source_coverage (
    id uuid PRIMARY KEY,
    version_id uuid NOT NULL REFERENCES evidence_version(id) ON DELETE RESTRICT,
    required_source_id uuid NOT NULL,
    coverage_note varchar(4000) NOT NULL,
    declared_by uuid NOT NULL REFERENCES access_identity(id),
    declared_at timestamptz NOT NULL,
    UNIQUE (version_id, required_source_id)
);

CREATE INDEX ix_source_coverage_version ON source_coverage(version_id);

CREATE TABLE applicability_decision (
    id uuid PRIMARY KEY,
    process_id uuid NOT NULL REFERENCES self_assessment_process(id) ON DELETE RESTRICT,
    subject_type varchar(30) NOT NULL CHECK (subject_type IN ('INDICATOR','REQUIRED_SOURCE')),
    subject_id uuid NOT NULL,
    decision varchar(20) NOT NULL CHECK (decision IN ('APPLICABLE','NOT_APPLICABLE')),
    rule_reference varchar(2000) NOT NULL,
    justification varchar(4000) NOT NULL,
    legal_basis varchar(4000) NOT NULL,
    document_reference varchar(2000) NOT NULL,
    authorized_by uuid NOT NULL REFERENCES access_identity(id),
    decided_at timestamptz NOT NULL,
    predecessor_id uuid REFERENCES applicability_decision(id),
    revision bigint NOT NULL,
    UNIQUE (process_id, subject_type, subject_id, revision),
    CHECK (decided_at IS NOT NULL)
);

CREATE INDEX ix_applicability_decision_subject
    ON applicability_decision(process_id, subject_type, subject_id, revision DESC);
