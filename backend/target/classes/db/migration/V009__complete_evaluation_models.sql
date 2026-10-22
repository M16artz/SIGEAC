-- D2 — Modelos configurables: jerarquía tipada de profundidad variable, definiciones de indicador,
-- fuentes requeridas, reglas de valoración, procedencia normativa y aplicabilidad por carrera.
-- Migración solo hacia adelante: no modifica V001–V008.

CREATE TABLE model_node (
    id uuid NOT NULL,
    version_id uuid NOT NULL REFERENCES model_version(id) ON DELETE RESTRICT,
    parent_id uuid,
    node_type varchar(30) NOT NULL CHECK (node_type IN ('CRITERION','SUBCRITERION','STANDARD','INDICATOR')),
    code varchar(100) NOT NULL CHECK (char_length(btrim(code)) > 0),
    title varchar(300) NOT NULL CHECK (char_length(btrim(title)) > 0),
    description varchar(4000) NOT NULL DEFAULT '',
    ordinal integer NOT NULL CHECK (ordinal > 0),
    mandatory boolean NOT NULL DEFAULT true,
    weight numeric,
    PRIMARY KEY (id, version_id),
    UNIQUE (version_id, code),
    FOREIGN KEY (parent_id, version_id) REFERENCES model_node(id, version_id) ON DELETE RESTRICT
);
CREATE INDEX ix_model_node_version ON model_node(version_id, parent_id, ordinal);

CREATE TABLE model_indicator_definition (
    node_id uuid NOT NULL,
    version_id uuid NOT NULL REFERENCES model_version(id) ON DELETE RESTRICT,
    nature varchar(20) NOT NULL CHECK (nature IN ('QUALITATIVE','QUANTITATIVE')),
    standard_text varchar(20000) NOT NULL,
    fundamental_elements_json jsonb NOT NULL DEFAULT '[]'::jsonb,
    evaluation_period_definition varchar(4000) NOT NULL,
    rating_scale_json jsonb,
    PRIMARY KEY (node_id, version_id),
    FOREIGN KEY (node_id, version_id) REFERENCES model_node(id, version_id) ON DELETE RESTRICT
);

CREATE TABLE model_required_source (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    version_id uuid NOT NULL REFERENCES model_version(id) ON DELETE RESTRICT,
    code varchar(100) NOT NULL,
    name varchar(300) NOT NULL,
    description varchar(4000) NOT NULL DEFAULT '',
    source_type varchar(30) NOT NULL CHECK (source_type IN ('FILE','EXTERNAL_LINK','STRUCTURED_DATA','INSTITUTIONAL_REFERENCE')),
    required boolean NOT NULL DEFAULT true,
    cardinality_min integer NOT NULL CHECK (cardinality_min >= 1),
    cardinality_max integer CHECK (cardinality_max IS NULL OR cardinality_max >= cardinality_min),
    period_rule varchar(4000) NOT NULL,
    ordinal integer NOT NULL CHECK (ordinal > 0),
    PRIMARY KEY (id, version_id),
    UNIQUE (node_id, code),
    UNIQUE (node_id, ordinal),
    FOREIGN KEY (node_id, version_id) REFERENCES model_node(id, version_id) ON DELETE RESTRICT
);

CREATE TABLE model_valuation_rule (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    version_id uuid NOT NULL REFERENCES model_version(id) ON DELETE RESTRICT,
    rule_type varchar(30) NOT NULL CHECK (rule_type IN ('MANUAL_QUALITATIVE','QUANTITATIVE')),
    method_definition varchar(20000) NOT NULL,
    formula_definition varchar(20000),
    strategy_key varchar(200),
    strategy_version varchar(100),
    missing_data_policy varchar(4000) NOT NULL,
    status varchar(20) NOT NULL CHECK (status IN ('DRAFT','VALIDATED','DEPRECATED')),
    PRIMARY KEY (id, version_id),
    FOREIGN KEY (node_id, version_id) REFERENCES model_node(id, version_id) ON DELETE RESTRICT
);
CREATE TABLE model_valuation_variable (
    id uuid NOT NULL,
    rule_id uuid NOT NULL,
    version_id uuid NOT NULL REFERENCES model_version(id) ON DELETE RESTRICT,
    code varchar(100) NOT NULL,
    name varchar(300) NOT NULL,
    unit varchar(100) NOT NULL,
    data_type varchar(100) NOT NULL,
    required boolean NOT NULL DEFAULT true,
    validation_definition varchar(4000) NOT NULL,
    PRIMARY KEY (id, version_id),
    UNIQUE (rule_id, code),
    FOREIGN KEY (rule_id, version_id) REFERENCES model_valuation_rule(id, version_id) ON DELETE RESTRICT
);
CREATE TABLE model_valuation_threshold (
    id uuid NOT NULL,
    rule_id uuid NOT NULL,
    version_id uuid NOT NULL REFERENCES model_version(id) ON DELETE RESTRICT,
    rating_option_id uuid NOT NULL,
    lower_bound numeric,
    upper_bound numeric,
    lower_inclusive boolean NOT NULL DEFAULT true,
    upper_inclusive boolean NOT NULL DEFAULT true,
    PRIMARY KEY (id, version_id),
    CHECK (lower_bound IS NOT NULL OR upper_bound IS NOT NULL),
    FOREIGN KEY (rule_id, version_id) REFERENCES model_valuation_rule(id, version_id) ON DELETE RESTRICT
);

CREATE TABLE normative_reference (
    id uuid PRIMARY KEY,
    title varchar(500) NOT NULL,
    issuer varchar(300) NOT NULL,
    edition varchar(200) NOT NULL,
    document_reference varchar(2000) NOT NULL,
    document_sha256 varchar(64),
    disposition varchar(2000) NOT NULL,
    verification_status varchar(30) NOT NULL CHECK (verification_status IN ('PENDING_VALIDATION','VERIFIED')),
    verified_by uuid,
    verified_at timestamptz,
    predecessor_id uuid REFERENCES normative_reference(id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL,
    CHECK (verification_status <> 'VERIFIED' OR (verified_by IS NOT NULL AND verified_at IS NOT NULL))
);
CREATE INDEX ix_normative_reference_status ON normative_reference(verification_status, title);

CREATE TABLE model_version_reference (
    id uuid PRIMARY KEY,
    version_id uuid NOT NULL REFERENCES model_version(id) ON DELETE RESTRICT,
    reference_id uuid NOT NULL REFERENCES normative_reference(id) ON DELETE RESTRICT,
    disposition varchar(2000) NOT NULL DEFAULT '',
    created_at timestamptz NOT NULL,
    UNIQUE (version_id, reference_id)
);

CREATE TABLE indicator_reference (
    id uuid PRIMARY KEY,
    node_id uuid NOT NULL,
    version_id uuid NOT NULL REFERENCES model_version(id) ON DELETE RESTRICT,
    reference_id uuid NOT NULL REFERENCES normative_reference(id) ON DELETE RESTRICT,
    disposition varchar(2000) NOT NULL DEFAULT '',
    created_at timestamptz NOT NULL,
    UNIQUE (node_id, reference_id),
    FOREIGN KEY (node_id, version_id) REFERENCES model_node(id, version_id) ON DELETE RESTRICT
);

CREATE TABLE model_career_applicability (
    id uuid PRIMARY KEY,
    model_version_id uuid NOT NULL REFERENCES model_version(id) ON DELETE RESTRICT,
    career_id uuid NOT NULL REFERENCES career(id) ON DELETE RESTRICT,
    status varchar(30) NOT NULL CHECK (status IN ('PENDING_VALIDATION','APPLICABLE','NOT_APPLICABLE','EXPIRED')),
    valid_from date NOT NULL,
    valid_until date,
    decision_id uuid,
    predecessor_id uuid REFERENCES model_career_applicability(id) ON DELETE RESTRICT,
    decided_by uuid NOT NULL,
    decided_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL,
    CHECK (valid_until IS NULL OR valid_from <= valid_until)
);
CREATE INDEX ix_model_career_applicability ON model_career_applicability(career_id, model_version_id, status);

-- El contenido publicado de un modelo es inmutable: se reutiliza la guardia de V004.
-- Las filas de contenido se identifican por (id, version_id): una versión sucesora conserva
-- la identidad de sus nodos y su contenido se congela al publicar.
CREATE TRIGGER model_node_published_guard BEFORE INSERT OR UPDATE OR DELETE ON model_node FOR EACH ROW EXECUTE FUNCTION reject_published_model_content_mutation();
CREATE TRIGGER indicator_definition_published_guard BEFORE INSERT OR UPDATE OR DELETE ON model_indicator_definition FOR EACH ROW EXECUTE FUNCTION reject_published_model_content_mutation();
CREATE TRIGGER required_source_published_guard BEFORE INSERT OR UPDATE OR DELETE ON model_required_source FOR EACH ROW EXECUTE FUNCTION reject_published_model_content_mutation();
CREATE TRIGGER valuation_rule_published_guard BEFORE INSERT OR UPDATE OR DELETE ON model_valuation_rule FOR EACH ROW EXECUTE FUNCTION reject_published_model_content_mutation();
CREATE TRIGGER valuation_variable_published_guard BEFORE INSERT OR UPDATE OR DELETE ON model_valuation_variable FOR EACH ROW EXECUTE FUNCTION reject_published_model_content_mutation();
CREATE TRIGGER valuation_threshold_published_guard BEFORE INSERT OR UPDATE OR DELETE ON model_valuation_threshold FOR EACH ROW EXECUTE FUNCTION reject_published_model_content_mutation();
CREATE TRIGGER version_reference_published_guard BEFORE INSERT OR UPDATE OR DELETE ON model_version_reference FOR EACH ROW EXECUTE FUNCTION reject_published_model_content_mutation();
CREATE TRIGGER indicator_reference_published_guard BEFORE INSERT OR UPDATE OR DELETE ON indicator_reference FOR EACH ROW EXECUTE FUNCTION reject_published_model_content_mutation();

-- Las referencias normativas y las decisiones de aplicabilidad solo se agregan.
CREATE FUNCTION reject_forward_only_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'forward-only record cannot be modified' USING ERRCODE='23514';
END;
$$;
CREATE TRIGGER normative_reference_forward_only BEFORE UPDATE OR DELETE ON normative_reference FOR EACH ROW EXECUTE FUNCTION reject_forward_only_mutation();
CREATE TRIGGER model_career_applicability_forward_only BEFORE UPDATE OR DELETE ON model_career_applicability FOR EACH ROW EXECUTE FUNCTION reject_forward_only_mutation();
