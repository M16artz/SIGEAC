-- V013: typed snapshots for structured-data and institutional-reference
-- source modalities (FR-069 / AC-X16). Snapshots share evidence-version
-- immutability; one snapshot per version and required source.
CREATE TABLE evidence_structured_value (
    id uuid PRIMARY KEY,
    version_id uuid NOT NULL REFERENCES evidence_version(id) ON DELETE RESTRICT,
    required_source_id uuid NOT NULL,
    kind varchar(30) NOT NULL CHECK (kind IN ('STRUCTURED_DATA','INSTITUTIONAL_REFERENCE')),
    value_type varchar(8) NOT NULL CHECK (value_type IN ('NUMBER','STRING','BOOLEAN')),
    value_text varchar(4000) NOT NULL,
    unit varchar(100),
    period_start date NOT NULL,
    period_end date NOT NULL,
    reference varchar(2000),
    created_by uuid NOT NULL REFERENCES access_identity(id),
    created_at timestamptz NOT NULL,
    UNIQUE (version_id, required_source_id),
    CHECK (period_start <= period_end)
);

CREATE INDEX ix_evidence_structured_value_version
    ON evidence_structured_value(version_id);
