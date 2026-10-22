PRAGMA foreign_keys=ON;

CREATE TABLE IF NOT EXISTS audit_event (
    id TEXT PRIMARY KEY, occurred_at TEXT NOT NULL, actor_user_id TEXT, technical_account_id TEXT,
    correlation_id TEXT NOT NULL, action TEXT NOT NULL, object_type TEXT NOT NULL, object_id TEXT NOT NULL,
    scope_career_id TEXT, scope_process_id TEXT, outcome TEXT NOT NULL, reason_code TEXT, metadata_json TEXT NOT NULL DEFAULT '{}'
);

CREATE TABLE IF NOT EXISTS access_identity (
    id TEXT PRIMARY KEY, login_normalized TEXT NOT NULL UNIQUE, display_name TEXT NOT NULL,
    identity_type TEXT NOT NULL, active INTEGER NOT NULL DEFAULT 1, revision INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL, deactivated_at TEXT
);
CREATE TABLE IF NOT EXISTS access_credential (
    id TEXT PRIMARY KEY, identity_id TEXT NOT NULL UNIQUE REFERENCES access_identity(id), bcrypt_hash TEXT NOT NULL,
    active INTEGER NOT NULL DEFAULT 1, created_at TEXT NOT NULL, rotated_at TEXT
);
CREATE TABLE IF NOT EXISTS access_session (
    id TEXT PRIMARY KEY, identity_id TEXT NOT NULL REFERENCES access_identity(id), token_hash TEXT NOT NULL UNIQUE,
    csrf_token_hash TEXT NOT NULL, created_at TEXT NOT NULL, last_activity_at TEXT NOT NULL,
    expires_at TEXT NOT NULL, revoked_at TEXT
);
CREATE TABLE IF NOT EXISTS authentication_attempt (
    id TEXT PRIMARY KEY, subject_key TEXT NOT NULL, source_key TEXT NOT NULL, occurred_at TEXT NOT NULL,
    success INTEGER NOT NULL, correlation_id TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS institutional_actor (
    id TEXT PRIMARY KEY, identity_id TEXT NOT NULL REFERENCES access_identity(id), actor_type TEXT NOT NULL,
    valid_from TEXT NOT NULL, valid_to TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS process_function (
    id TEXT PRIMARY KEY, code TEXT NOT NULL UNIQUE, name TEXT NOT NULL, active INTEGER NOT NULL DEFAULT 1
);
CREATE TABLE IF NOT EXISTS function_operation (
    function_id TEXT NOT NULL REFERENCES process_function(id), operation_code TEXT NOT NULL,
    PRIMARY KEY(function_id, operation_code)
);
CREATE TABLE IF NOT EXISTS technical_privilege (
    id TEXT PRIMARY KEY, identity_id TEXT NOT NULL REFERENCES access_identity(id), privilege_code TEXT NOT NULL,
    valid_from TEXT NOT NULL, valid_to TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS functional_assignment (
    id TEXT PRIMARY KEY, identity_id TEXT NOT NULL REFERENCES access_identity(id),
    function_id TEXT NOT NULL REFERENCES process_function(id), career_id TEXT NOT NULL, process_id TEXT,
    indicator_id TEXT, valid_from TEXT NOT NULL, valid_to TEXT NOT NULL,
    granted_by TEXT NOT NULL REFERENCES access_identity(id), granted_at TEXT NOT NULL, revoked_at TEXT,
    revision INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS conflict_policy (
    id TEXT PRIMARY KEY, left_function_id TEXT NOT NULL REFERENCES process_function(id),
    right_function_id TEXT NOT NULL REFERENCES process_function(id), valid_from TEXT NOT NULL,
    valid_to TEXT NOT NULL, active INTEGER NOT NULL DEFAULT 1,
    UNIQUE(left_function_id, right_function_id, valid_from)
);
CREATE TABLE IF NOT EXISTS conflict_exception (
    id TEXT PRIMARY KEY, policy_id TEXT NOT NULL REFERENCES conflict_policy(id),
    identity_id TEXT NOT NULL REFERENCES access_identity(id), career_id TEXT NOT NULL, process_id TEXT,
    indicator_id TEXT, justification TEXT NOT NULL, valid_from TEXT NOT NULL, valid_to TEXT NOT NULL,
    approved_by TEXT NOT NULL REFERENCES access_identity(id), revoked_at TEXT
);

CREATE TABLE IF NOT EXISTS evaluation_model (
    id TEXT PRIMARY KEY, name TEXT NOT NULL, description TEXT NOT NULL DEFAULT '', created_by TEXT NOT NULL, created_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS model_version (
    id TEXT PRIMARY KEY, model_id TEXT NOT NULL REFERENCES evaluation_model(id), version_number INTEGER NOT NULL,
    status TEXT NOT NULL, source TEXT NOT NULL DEFAULT '', author_id TEXT NOT NULL, approver_id TEXT,
    predecessor_id TEXT REFERENCES model_version(id), revision INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL, published_at TEXT, snapshot_json TEXT NOT NULL,
    UNIQUE(model_id, version_number)
);
CREATE TABLE IF NOT EXISTS model_criterion (
    id TEXT PRIMARY KEY, version_id TEXT NOT NULL REFERENCES model_version(id), code TEXT NOT NULL,
    name TEXT NOT NULL, description TEXT NOT NULL DEFAULT '', position INTEGER NOT NULL,
    UNIQUE(id, version_id), UNIQUE(version_id, code), UNIQUE(version_id, position)
);
CREATE TABLE IF NOT EXISTS model_standard (
    id TEXT PRIMARY KEY, version_id TEXT NOT NULL REFERENCES model_version(id), criterion_id TEXT NOT NULL,
    code TEXT NOT NULL, name TEXT NOT NULL, description TEXT NOT NULL DEFAULT '', position INTEGER NOT NULL,
    UNIQUE(id, version_id), UNIQUE(version_id, code), UNIQUE(criterion_id, position)
);
CREATE TABLE IF NOT EXISTS model_indicator (
    id TEXT PRIMARY KEY, version_id TEXT NOT NULL REFERENCES model_version(id), standard_id TEXT NOT NULL,
    code TEXT NOT NULL, name TEXT NOT NULL, description TEXT NOT NULL DEFAULT '', position INTEGER NOT NULL,
    requirements_json TEXT NOT NULL DEFAULT '{}', UNIQUE(version_id, code), UNIQUE(standard_id, position)
);
CREATE TABLE IF NOT EXISTS model_rating_scale (
    id TEXT PRIMARY KEY, version_id TEXT NOT NULL UNIQUE REFERENCES model_version(id), code TEXT NOT NULL, name TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS model_scale_option (
    id TEXT PRIMARY KEY, scale_id TEXT NOT NULL REFERENCES model_rating_scale(id), code TEXT NOT NULL,
    label TEXT NOT NULL, position INTEGER NOT NULL, declared_value NUMERIC,
    UNIQUE(scale_id, code), UNIQUE(scale_id, position)
);
CREATE TABLE IF NOT EXISTS model_progress_formula (
    id TEXT PRIMARY KEY, version_id TEXT NOT NULL UNIQUE REFERENCES model_version(id), code TEXT NOT NULL,
    schema_version TEXT NOT NULL, definition_json TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS self_assessment_process (
    id TEXT PRIMARY KEY, career_id TEXT NOT NULL, code TEXT NOT NULL, name TEXT NOT NULL,
    period_start TEXT NOT NULL, period_end TEXT NOT NULL, model_version_id TEXT NOT NULL REFERENCES model_version(id),
    model_version_number INTEGER NOT NULL, state TEXT NOT NULL, revision INTEGER NOT NULL DEFAULT 0,
    created_by TEXT NOT NULL, created_at TEXT NOT NULL, snapshot_json TEXT NOT NULL,
    UNIQUE(career_id, code)
);
CREATE TABLE IF NOT EXISTS process_indicator (
    process_id TEXT NOT NULL REFERENCES self_assessment_process(id), indicator_id TEXT NOT NULL,
    code TEXT NOT NULL, name TEXT NOT NULL, PRIMARY KEY(process_id, indicator_id)
);
CREATE TABLE IF NOT EXISTS process_assignment (
    id TEXT PRIMARY KEY, process_id TEXT NOT NULL REFERENCES self_assessment_process(id), indicator_id TEXT NOT NULL,
    identity_id TEXT NOT NULL, function_code TEXT NOT NULL, valid_from TEXT NOT NULL, valid_to TEXT NOT NULL,
    created_by TEXT NOT NULL, reason TEXT NOT NULL DEFAULT '', revoked_at TEXT, revision INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS process_transition (
    id TEXT PRIMARY KEY, process_id TEXT NOT NULL REFERENCES self_assessment_process(id), from_state TEXT NOT NULL,
    to_state TEXT NOT NULL, actor_id TEXT NOT NULL, reason TEXT NOT NULL DEFAULT '', occurred_at TEXT NOT NULL,
    correlation_id TEXT NOT NULL, resulting_revision INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS process_closure (
    id TEXT PRIMARY KEY, process_id TEXT NOT NULL REFERENCES self_assessment_process(id), closed_at TEXT NOT NULL,
    closed_by TEXT NOT NULL, reopened_at TEXT, reopened_by TEXT, reopen_reason TEXT
);

CREATE TABLE IF NOT EXISTS evidence (
    id BLOB PRIMARY KEY, process_id BLOB NOT NULL, owner_id BLOB NOT NULL, created_at TEXT NOT NULL,
    correlation_id BLOB NOT NULL, latest_revision INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS evidence_version (
    id BLOB PRIMARY KEY, evidence_id BLOB NOT NULL REFERENCES evidence(id), revision_number INTEGER NOT NULL,
    predecessor_id BLOB, status TEXT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL,
    provenance TEXT NOT NULL, covered_from TEXT NOT NULL, covered_to TEXT NOT NULL, author_id BLOB NOT NULL,
    submitted_at TEXT, lock_revision INTEGER NOT NULL DEFAULT 0, created_at TEXT NOT NULL,
    UNIQUE(evidence_id, revision_number), UNIQUE(predecessor_id)
);
CREATE TABLE IF NOT EXISTS evidence_version_indicator (
    version_id BLOB NOT NULL REFERENCES evidence_version(id), indicator_id BLOB NOT NULL,
    indicator_code TEXT NOT NULL, indicator_title TEXT NOT NULL, position INTEGER NOT NULL,
    PRIMARY KEY(version_id, indicator_id), UNIQUE(version_id, position)
);
CREATE TABLE IF NOT EXISTS file_validation_policy (
    id TEXT PRIMARY KEY, policy_code TEXT NOT NULL UNIQUE, max_bytes INTEGER NOT NULL, allow_empty INTEGER NOT NULL,
    allowed_detected_types TEXT NOT NULL, compatibility_json TEXT NOT NULL, active INTEGER NOT NULL, created_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS file_asset (
    id TEXT PRIMARY KEY, upload_id TEXT NOT NULL UNIQUE, storage_key TEXT UNIQUE, staging_key TEXT UNIQUE,
    original_name TEXT NOT NULL, declared_media_type TEXT NOT NULL, detected_media_type TEXT NOT NULL,
    size_bytes INTEGER NOT NULL, sha256 TEXT NOT NULL, validation_policy_id TEXT NOT NULL REFERENCES file_validation_policy(id),
    status TEXT NOT NULL, created_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS evidence_version_file (
    version_id BLOB NOT NULL REFERENCES evidence_version(id), file_id TEXT NOT NULL REFERENCES file_asset(id),
    original_name TEXT NOT NULL, declared_media_type TEXT NOT NULL, detected_media_type TEXT NOT NULL,
    size_bytes INTEGER NOT NULL, sha256 TEXT NOT NULL, position INTEGER NOT NULL,
    PRIMARY KEY(version_id, file_id), UNIQUE(version_id, position)
);
CREATE TABLE IF NOT EXISTS evidence_external_reference (
    id TEXT PRIMARY KEY, version_id BLOB NOT NULL REFERENCES evidence_version(id), uri TEXT NOT NULL,
    description TEXT NOT NULL, position INTEGER NOT NULL, UNIQUE(version_id, position)
);
CREATE TABLE IF NOT EXISTS file_recovery_operation (
    id TEXT PRIMARY KEY, file_id TEXT REFERENCES file_asset(id), operation TEXT NOT NULL, status TEXT NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0, not_before TEXT NOT NULL, lease_owner TEXT, lease_until TEXT,
    safe_reason TEXT, correlation_id TEXT NOT NULL, created_at TEXT NOT NULL, updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS academic_unit (
    id TEXT PRIMARY KEY,code TEXT NOT NULL UNIQUE,name TEXT NOT NULL,status TEXT NOT NULL CHECK(status IN ('ACTIVE','DISABLED')),
    revision INTEGER NOT NULL CHECK(revision>=1),created_by TEXT NOT NULL,created_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS career (
    id TEXT PRIMARY KEY,academic_unit_id TEXT NOT NULL REFERENCES academic_unit(id) ON DELETE RESTRICT,
    code TEXT NOT NULL UNIQUE,name TEXT NOT NULL,status TEXT NOT NULL CHECK(status IN ('ACTIVE','DISABLED')),
    valid_from TEXT NOT NULL,valid_until TEXT,classification_pending INTEGER NOT NULL DEFAULT 0,
    revision INTEGER NOT NULL CHECK(revision>=1),created_by TEXT NOT NULL,created_at TEXT NOT NULL,
    CHECK(valid_until IS NULL OR valid_from<=valid_until)
);
CREATE TABLE IF NOT EXISTS academic_unit_history (
    unit_id TEXT NOT NULL REFERENCES academic_unit(id),revision INTEGER NOT NULL,snapshot_json TEXT NOT NULL,
    changed_by TEXT NOT NULL,changed_at TEXT NOT NULL,reason TEXT NOT NULL,PRIMARY KEY(unit_id,revision)
);
CREATE TABLE IF NOT EXISTS career_history (
    career_id TEXT NOT NULL REFERENCES career(id),revision INTEGER NOT NULL,snapshot_json TEXT NOT NULL,
    changed_by TEXT NOT NULL,changed_at TEXT NOT NULL,reason TEXT NOT NULL,PRIMARY KEY(career_id,revision)
);
CREATE INDEX IF NOT EXISTS ix_career_unit_status ON career(academic_unit_id,status,code,id);
INSERT OR IGNORE INTO academic_unit VALUES('00000000-0000-0000-0000-000000000008','LEGACY-UNCLASSIFIED','Unidad pendiente de clasificación','DISABLED',1,'00000000-0000-0000-0000-000000000000',strftime('%Y-%m-%dT%H:%M:%fZ','now'));
INSERT OR IGNORE INTO career(id,academic_unit_id,code,name,status,valid_from,classification_pending,revision,created_by,created_at)
SELECT id,'00000000-0000-0000-0000-000000000008','LEGACY-'||upper(id),'Carrera pendiente de clasificación','DISABLED','1900-01-01',1,1,'00000000-0000-0000-0000-000000000000',strftime('%Y-%m-%dT%H:%M:%fZ','now')
FROM (SELECT career_id AS id FROM self_assessment_process UNION SELECT career_id FROM functional_assignment UNION SELECT career_id FROM conflict_exception);
INSERT OR IGNORE INTO academic_unit_history SELECT id,revision,json_object('id',id,'data',json_object('code',code,'name',name),'status',status,'revision',revision,'createdBy',created_by,'createdAt',created_at),created_by,created_at,'LEGACY_REFERENCE_RECONCILIATION' FROM academic_unit;
INSERT OR IGNORE INTO career_history SELECT id,revision,json_object('id',id,'data',json_object('code',code,'name',name,'academicUnitId',academic_unit_id,'validFrom',valid_from,'validUntil',valid_until),'status',status,'revision',revision,'createdBy',created_by,'createdAt',created_at,'classificationPending',json('true')),created_by,created_at,'LEGACY_REFERENCE_RECONCILIATION' FROM career WHERE classification_pending=1;

CREATE TABLE IF NOT EXISTS model_node (
    id TEXT NOT NULL, version_id TEXT NOT NULL REFERENCES model_version(id), parent_id TEXT,
    node_type TEXT NOT NULL CHECK (node_type IN ('CRITERION','SUBCRITERION','STANDARD','INDICATOR')),
    code TEXT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL DEFAULT '',
    ordinal INTEGER NOT NULL CHECK (ordinal>0), mandatory INTEGER NOT NULL DEFAULT 1, weight NUMERIC,
    PRIMARY KEY (id, version_id), UNIQUE (version_id, code),
    FOREIGN KEY (parent_id, version_id) REFERENCES model_node(id, version_id)
);
CREATE INDEX IF NOT EXISTS ix_model_node_version ON model_node(version_id, parent_id, ordinal);
CREATE TABLE IF NOT EXISTS model_indicator_definition (
    node_id TEXT NOT NULL, version_id TEXT NOT NULL REFERENCES model_version(id),
    nature TEXT NOT NULL CHECK (nature IN ('QUALITATIVE','QUANTITATIVE')), standard_text TEXT NOT NULL,
    fundamental_elements_json TEXT NOT NULL DEFAULT '[]', evaluation_period_definition TEXT NOT NULL,
    rating_scale_json TEXT,
    PRIMARY KEY (node_id, version_id),
    FOREIGN KEY (node_id, version_id) REFERENCES model_node(id, version_id)
);
CREATE TABLE IF NOT EXISTS model_required_source (
    id TEXT NOT NULL, node_id TEXT NOT NULL, version_id TEXT NOT NULL REFERENCES model_version(id),
    code TEXT NOT NULL, name TEXT NOT NULL, description TEXT NOT NULL DEFAULT '',
    source_type TEXT NOT NULL CHECK (source_type IN ('FILE','EXTERNAL_LINK','STRUCTURED_DATA','INSTITUTIONAL_REFERENCE')),
    required INTEGER NOT NULL DEFAULT 1, cardinality_min INTEGER NOT NULL CHECK (cardinality_min>=1),
    cardinality_max INTEGER CHECK (cardinality_max IS NULL OR cardinality_max>=cardinality_min),
    period_rule TEXT NOT NULL, ordinal INTEGER NOT NULL CHECK (ordinal>0),
    PRIMARY KEY (id, version_id), UNIQUE (node_id, code), UNIQUE (node_id, ordinal),
    FOREIGN KEY (node_id, version_id) REFERENCES model_node(id, version_id)
);
CREATE TABLE IF NOT EXISTS model_valuation_rule (
    id TEXT NOT NULL, node_id TEXT NOT NULL, version_id TEXT NOT NULL REFERENCES model_version(id),
    rule_type TEXT NOT NULL CHECK (rule_type IN ('MANUAL_QUALITATIVE','QUANTITATIVE')), method_definition TEXT NOT NULL,
    formula_definition TEXT, strategy_key TEXT, strategy_version TEXT, missing_data_policy TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('DRAFT','VALIDATED','DEPRECATED')),
    PRIMARY KEY (id, version_id),
    FOREIGN KEY (node_id, version_id) REFERENCES model_node(id, version_id)
);
CREATE TABLE IF NOT EXISTS model_valuation_variable (
    id TEXT NOT NULL, rule_id TEXT NOT NULL, version_id TEXT NOT NULL REFERENCES model_version(id),
    code TEXT NOT NULL, name TEXT NOT NULL, unit TEXT NOT NULL, data_type TEXT NOT NULL,
    required INTEGER NOT NULL DEFAULT 1, validation_definition TEXT NOT NULL,
    PRIMARY KEY (id, version_id), UNIQUE (rule_id, code),
    FOREIGN KEY (rule_id, version_id) REFERENCES model_valuation_rule(id, version_id)
);
CREATE TABLE IF NOT EXISTS model_valuation_threshold (
    id TEXT NOT NULL, rule_id TEXT NOT NULL, version_id TEXT NOT NULL REFERENCES model_version(id),
    rating_option_id TEXT NOT NULL, lower_bound NUMERIC, upper_bound NUMERIC,
    lower_inclusive INTEGER NOT NULL DEFAULT 1, upper_inclusive INTEGER NOT NULL DEFAULT 1,
    PRIMARY KEY (id, version_id),
    CHECK (lower_bound IS NOT NULL OR upper_bound IS NOT NULL),
    FOREIGN KEY (rule_id, version_id) REFERENCES model_valuation_rule(id, version_id)
);
CREATE TABLE IF NOT EXISTS normative_reference (
    id TEXT PRIMARY KEY, title TEXT NOT NULL, issuer TEXT NOT NULL, edition TEXT NOT NULL,
    document_reference TEXT NOT NULL, document_sha256 TEXT, disposition TEXT NOT NULL,
    verification_status TEXT NOT NULL CHECK (verification_status IN ('PENDING_VALIDATION','VERIFIED')),
    verified_by TEXT, verified_at TEXT, predecessor_id TEXT REFERENCES normative_reference(id), created_at TEXT NOT NULL,
    CHECK (verification_status <> 'VERIFIED' OR (verified_by IS NOT NULL AND verified_at IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS ix_normative_reference_status ON normative_reference(verification_status, title);
CREATE TABLE IF NOT EXISTS model_version_reference (
    id TEXT PRIMARY KEY, version_id TEXT NOT NULL REFERENCES model_version(id),
    reference_id TEXT NOT NULL REFERENCES normative_reference(id),
    disposition TEXT NOT NULL DEFAULT '', created_at TEXT NOT NULL, UNIQUE (version_id, reference_id)
);
CREATE TABLE IF NOT EXISTS indicator_reference (
    id TEXT PRIMARY KEY, node_id TEXT NOT NULL, version_id TEXT NOT NULL REFERENCES model_version(id),
    reference_id TEXT NOT NULL REFERENCES normative_reference(id), disposition TEXT NOT NULL DEFAULT '',
    created_at TEXT NOT NULL, UNIQUE (node_id, reference_id),
    FOREIGN KEY (node_id, version_id) REFERENCES model_node(id, version_id)
);
CREATE TABLE IF NOT EXISTS model_career_applicability (
    id TEXT PRIMARY KEY, model_version_id TEXT NOT NULL REFERENCES model_version(id), career_id TEXT NOT NULL REFERENCES career(id),
    status TEXT NOT NULL CHECK (status IN ('PENDING_VALIDATION','APPLICABLE','NOT_APPLICABLE','EXPIRED')),
    valid_from TEXT NOT NULL, valid_until TEXT, decision_id TEXT, predecessor_id TEXT REFERENCES model_career_applicability(id),
    decided_by TEXT NOT NULL, decided_at TEXT NOT NULL, created_at TEXT NOT NULL,
    CHECK (valid_until IS NULL OR valid_from<=valid_until)
);
CREATE INDEX IF NOT EXISTS ix_model_career_applicability ON model_career_applicability(career_id, model_version_id, status);
