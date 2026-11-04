-- V015: append-only documentary review decisions with per-coverage outcomes
-- (D5, P09-P10). Decisions are chained per version; outcomes never duplicate
-- evidence state and the global status is always derived, never stored twice.
CREATE INDEX ix_evidence_version_successor
    ON evidence_version(predecessor_id) WHERE predecessor_id IS NOT NULL;
CREATE UNIQUE INDEX ux_evidence_version_successor
    ON evidence_version(predecessor_id) WHERE predecessor_id IS NOT NULL;

CREATE TABLE document_review_decision (
    id uuid PRIMARY KEY,
    version_id uuid NOT NULL REFERENCES evidence_version(id) ON DELETE RESTRICT,
    decision varchar(20) NOT NULL CHECK (decision IN ('ACCEPTED','OBSERVED','REJECTED')),
    justification varchar(4000) NOT NULL CHECK (char_length(justification) BETWEEN 10 AND 4000),
    reviewer_id uuid NOT NULL REFERENCES access_identity(id),
    reviewed_at timestamptz NOT NULL,
    self_review_exception boolean NOT NULL DEFAULT false,
    exception_reason varchar(4000),
    predecessor_id uuid REFERENCES document_review_decision(id),
    successor_version_id uuid REFERENCES evidence_version(id),
    revision bigint NOT NULL,
    UNIQUE (version_id, revision),
    UNIQUE (successor_version_id),
    CHECK (NOT self_review_exception OR (exception_reason IS NOT NULL AND char_length(exception_reason) BETWEEN 10 AND 4000))
);

CREATE INDEX ix_document_review_version
    ON document_review_decision(version_id, revision DESC);

CREATE TABLE document_review_outcome (
    id uuid PRIMARY KEY,
    decision_id uuid NOT NULL REFERENCES document_review_decision(id) ON DELETE RESTRICT,
    required_source_id uuid NOT NULL,
    outcome varchar(20) NOT NULL CHECK (outcome IN ('ACCEPTED','OBSERVED','REJECTED')),
    note varchar(4000),
    UNIQUE (decision_id, required_source_id)
);

CREATE INDEX ix_document_review_outcome_decision
    ON document_review_outcome(decision_id);
