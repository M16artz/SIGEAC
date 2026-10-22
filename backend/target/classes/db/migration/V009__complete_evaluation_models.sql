CREATE TABLE normative_reference (
    id uuid PRIMARY KEY,
    title varchar(500) NOT NULL,
    issuer varchar(300),
    edition varchar(200),
    document_reference varchar(2000) NOT NULL,
    document_sha256 char(64),
    pages varchar(200),
    disposition varchar(2000),
    verification_status varchar(30) NOT NULL CHECK (verification_status IN ('PENDING_VALIDATION','VERIFIED')),
    verified_by uuid REFERENCES access_identity(id),
    verified_at timestamptz,
    CHECK (verification_status <> 'PENDING_VALIDATION' OR (disposition IS NULL AND verified_by IS NULL AND verified_at IS NULL)),
    CHECK (verification_status <> 'VERIFIED' OR (issuer IS NOT NULL AND edition IS NOT NULL AND disposition IS NOT NULL
        AND document_sha256 IS NOT NULL AND verified_by IS NOT NULL AND verified_at IS NOT NULL))
);

CREATE TABLE model_node (
    id uuid PRIMARY KEY,
    version_id uuid NOT NULL REFERENCES model_version(id),
    parent_id uuid REFERENCES model_node(id),
    code varchar(150) NOT NULL,
    name varchar(300) NOT NULL,
    description varchar(4000) NOT NULL DEFAULT '',
    position integer NOT NULL CHECK (position > 0),
    node_type varchar(30) NOT NULL CHECK (node_type IN ('CRITERION','SUBCRITERION','INDICATOR')),
    UNIQUE(version_id, code),
    UNIQUE(id, version_id),
    UNIQUE(parent_id, position)
);

CREATE TABLE indicator_normative_reference (
    model_version_id uuid NOT NULL REFERENCES model_version(id),
    indicator_node_id uuid NOT NULL,
    normative_reference_id uuid NOT NULL REFERENCES normative_reference(id),
    source_pages varchar(200),
    disposition varchar(2000),
    PRIMARY KEY (model_version_id, indicator_node_id, normative_reference_id),
    FOREIGN KEY (indicator_node_id, model_version_id) REFERENCES model_node(id, version_id)
);

CREATE TABLE required_source (
    id uuid PRIMARY KEY,
    version_id uuid NOT NULL REFERENCES model_version(id),
    indicator_node_id uuid NOT NULL,
    code varchar(150) NOT NULL,
    source_type varchar(40) NOT NULL CHECK (source_type IN ('FILE','EXTERNAL_LINK','STRUCTURED_DATA','INSTITUTIONAL_REFERENCE')),
    name varchar(300) NOT NULL,
    description varchar(4000) NOT NULL DEFAULT '',
    required boolean NOT NULL,
    cardinality_min integer NOT NULL CHECK (cardinality_min >= 1),
    cardinality_max integer CHECK (cardinality_max IS NULL OR cardinality_max >= cardinality_min),
    period_rule varchar(4000) NOT NULL,
    ordinal integer NOT NULL CHECK (ordinal > 0),
    UNIQUE(indicator_node_id, code),
    UNIQUE(indicator_node_id, ordinal),
    FOREIGN KEY (indicator_node_id, version_id) REFERENCES model_node(id, version_id)
);

CREATE TABLE valuation_rule (
    id uuid PRIMARY KEY,
    version_id uuid NOT NULL REFERENCES model_version(id),
    indicator_node_id uuid NOT NULL,
    rule_type varchar(30) NOT NULL CHECK (rule_type IN ('MANUAL_QUALITATIVE','QUANTITATIVE')),
    method_definition varchar(20000) NOT NULL,
    formula_definition varchar(20000),
    strategy_key varchar(200),
    strategy_version varchar(100),
    scale_id uuid,
    missing_data_policy varchar(4000) NOT NULL,
    status varchar(30) NOT NULL CHECK (status IN ('DRAFT','VALIDATED','RETIRED')),
    UNIQUE(id, version_id),
    FOREIGN KEY (indicator_node_id, version_id) REFERENCES model_node(id, version_id)
);

CREATE TABLE valuation_variable (
    version_id uuid NOT NULL REFERENCES model_version(id),
    rule_id uuid NOT NULL,
    code varchar(150) NOT NULL,
    name varchar(300) NOT NULL,
    unit varchar(100) NOT NULL,
    data_type varchar(20) NOT NULL CHECK (data_type IN ('DECIMAL','INTEGER')),
    required boolean NOT NULL,
    validation_definition varchar(4000) NOT NULL,
    ordinal integer NOT NULL CHECK (ordinal > 0),
    PRIMARY KEY (rule_id, code),
    UNIQUE(rule_id, ordinal),
    FOREIGN KEY (rule_id, version_id) REFERENCES valuation_rule(id, version_id)
);

CREATE TABLE valuation_threshold (
    version_id uuid NOT NULL REFERENCES model_version(id),
    rule_id uuid NOT NULL,
    rating_option_id uuid NOT NULL,
    lower_bound numeric,
    upper_bound numeric,
    lower_inclusive boolean NOT NULL,
    upper_inclusive boolean NOT NULL,
    PRIMARY KEY (rule_id, rating_option_id),
    CHECK (lower_bound IS NULL OR upper_bound IS NULL OR lower_bound < upper_bound
        OR (lower_bound = upper_bound AND lower_inclusive AND upper_inclusive)),
    FOREIGN KEY (rule_id, version_id) REFERENCES valuation_rule(id, version_id)
);

CREATE TABLE model_career_applicability (
    id uuid PRIMARY KEY,
    model_version_id uuid NOT NULL REFERENCES model_version(id),
    career_id uuid NOT NULL REFERENCES career(id),
    status varchar(30) NOT NULL CHECK (status IN ('PENDING_VALIDATION','APPLICABLE','NOT_APPLICABLE','EXPIRED')),
    valid_from date NOT NULL,
    valid_until date,
    authority_function varchar(300) NOT NULL,
    legal_basis varchar(2000) NOT NULL,
    document_reference varchar(2000) NOT NULL,
    reason varchar(4000) NOT NULL,
    predecessor_id uuid REFERENCES model_career_applicability(id),
    revision integer NOT NULL CHECK (revision > 0),
    decided_by uuid NOT NULL REFERENCES access_identity(id),
    decided_at timestamptz NOT NULL,
    UNIQUE(model_version_id, career_id, revision),
    CHECK (valid_until IS NULL OR valid_until >= valid_from)
);

CREATE INDEX ix_model_node_version_parent ON model_node(version_id, parent_id, position);
CREATE INDEX ix_indicator_normative_reference_version ON indicator_normative_reference(model_version_id);
CREATE INDEX ix_required_source_version_indicator ON required_source(version_id, indicator_node_id);
CREATE INDEX ix_valuation_rule_version_indicator ON valuation_rule(version_id, indicator_node_id);
CREATE INDEX ix_model_career_applicability_version_career ON model_career_applicability(model_version_id, career_id, revision DESC);

CREATE OR REPLACE FUNCTION reject_normative_reference_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'normative references are immutable; register a new reference';
END;
$$;
CREATE TRIGGER normative_reference_immutable BEFORE UPDATE OR DELETE ON normative_reference
    FOR EACH ROW EXECUTE FUNCTION reject_normative_reference_mutation();

CREATE OR REPLACE FUNCTION reject_indicator_reference_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'indicator normative links are forward-only';
END;
$$;
CREATE TRIGGER indicator_normative_reference_forward_only BEFORE UPDATE OR DELETE ON indicator_normative_reference
    FOR EACH ROW EXECUTE FUNCTION reject_indicator_reference_mutation();

CREATE OR REPLACE FUNCTION reject_applicability_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'career applicability decisions are append-only';
END;
$$;
CREATE TRIGGER model_career_applicability_append_only BEFORE UPDATE OR DELETE ON model_career_applicability
    FOR EACH ROW EXECUTE FUNCTION reject_applicability_mutation();

CREATE TRIGGER node_published_guard BEFORE INSERT OR UPDATE OR DELETE ON model_node FOR EACH ROW EXECUTE FUNCTION reject_published_model_content_mutation();
CREATE TRIGGER required_source_published_guard BEFORE INSERT OR UPDATE OR DELETE ON required_source FOR EACH ROW EXECUTE FUNCTION reject_published_model_content_mutation();
CREATE TRIGGER valuation_rule_published_guard BEFORE INSERT OR UPDATE OR DELETE ON valuation_rule FOR EACH ROW EXECUTE FUNCTION reject_published_model_content_mutation();
CREATE TRIGGER valuation_variable_published_guard BEFORE INSERT OR UPDATE OR DELETE ON valuation_variable FOR EACH ROW EXECUTE FUNCTION reject_published_model_content_mutation();
CREATE TRIGGER valuation_threshold_published_guard BEFORE INSERT OR UPDATE OR DELETE ON valuation_threshold FOR EACH ROW EXECUTE FUNCTION reject_published_model_content_mutation();

INSERT INTO function_operation(function_id,operation_code)
    SELECT id,'MODEL_APPLICABILITY_AUTHORIZE' FROM process_function
    WHERE code IN ('FUNCTIONAL_ADMIN','COORDINATOR','PROCESS_AUTHORITY') ON CONFLICT DO NOTHING;
