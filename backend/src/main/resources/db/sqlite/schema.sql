PRAGMA foreign_keys=ON;

CREATE TABLE IF NOT EXISTS documentary_snapshot (
    id TEXT PRIMARY KEY,
    process_id TEXT NOT NULL REFERENCES self_assessment_process(id),
    career_id TEXT NOT NULL REFERENCES career(id), model_version_id TEXT NOT NULL REFERENCES model_version(id),
    process_revision INTEGER NOT NULL CHECK(process_revision >= 0),
    created_by TEXT NOT NULL REFERENCES access_identity(id), created_at TIMESTAMP NOT NULL,
    reason TEXT NOT NULL CHECK(length(reason) BETWEEN 10 AND 4000),
    formula_version TEXT NOT NULL CHECK(length(formula_version) BETWEEN 1 AND 100),
    progress_state TEXT NOT NULL CHECK(progress_state IN ('CONFIGURED','NOT_CONFIGURED','NOT_APPLICABLE')),
    required_source_total INTEGER NOT NULL CHECK(required_source_total >= 0),
    registered_source_total INTEGER NOT NULL CHECK(registered_source_total BETWEEN 0 AND required_source_total),
    accepted_source_total INTEGER NOT NULL CHECK(accepted_source_total BETWEEN 0 AND registered_source_total),
    sha256 TEXT NOT NULL CHECK(length(sha256)=64 AND sha256 NOT GLOB '*[^a-f0-9]*'),
    payload_json TEXT NOT NULL CHECK(json_valid(payload_json) AND json_type(payload_json)='object'),
    UNIQUE(process_id,process_revision),
    CHECK((progress_state='CONFIGURED')=(required_source_total>0))
);
CREATE INDEX IF NOT EXISTS ix_documentary_snapshot_history ON documentary_snapshot(process_id,created_at DESC,id DESC);
CREATE UNIQUE INDEX IF NOT EXISTS ux_documentary_snapshot_process ON documentary_snapshot(id,process_id);
CREATE TABLE IF NOT EXISTS documentary_closure (
    id TEXT PRIMARY KEY,process_id TEXT NOT NULL REFERENCES self_assessment_process(id),
    career_id TEXT NOT NULL REFERENCES career(id),snapshot_id TEXT NOT NULL UNIQUE,
    process_revision INTEGER NOT NULL CHECK(process_revision >= 0),
    policy_version TEXT NOT NULL CHECK(policy_version='DOCUMENTARY_STRICT_V1'),
    closed_by TEXT NOT NULL REFERENCES access_identity(id),closed_at TIMESTAMP NOT NULL,
    reason TEXT NOT NULL CHECK(length(reason) BETWEEN 10 AND 2000),
    sha256 TEXT NOT NULL CHECK(length(sha256)=64 AND sha256 NOT GLOB '*[^a-f0-9]*'),
    payload_json TEXT NOT NULL CHECK(json_valid(payload_json) AND json_type(payload_json)='object'),
    UNIQUE(process_id,process_revision),
    FOREIGN KEY(snapshot_id,process_id) REFERENCES documentary_snapshot(id,process_id)
);
CREATE INDEX IF NOT EXISTS ix_documentary_closure_history ON documentary_closure(process_id,closed_at DESC,id DESC);

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
CREATE TABLE IF NOT EXISTS normative_reference (
    id TEXT PRIMARY KEY, title TEXT NOT NULL, issuer TEXT, edition TEXT, document_reference TEXT NOT NULL,
    document_sha256 TEXT, pages TEXT, disposition TEXT, verification_status TEXT NOT NULL,
    verified_by TEXT REFERENCES access_identity(id), verified_at TEXT,
    CHECK (verification_status <> 'PENDING_VALIDATION' OR (disposition IS NULL AND verified_by IS NULL AND verified_at IS NULL)),
    CHECK (verification_status <> 'VERIFIED' OR (issuer IS NOT NULL AND edition IS NOT NULL AND disposition IS NOT NULL
        AND document_sha256 IS NOT NULL AND verified_by IS NOT NULL AND verified_at IS NOT NULL))
);
CREATE TABLE IF NOT EXISTS model_node (
    id TEXT PRIMARY KEY, version_id TEXT NOT NULL REFERENCES model_version(id), parent_id TEXT REFERENCES model_node(id),
    code TEXT NOT NULL, name TEXT NOT NULL, description TEXT NOT NULL DEFAULT '', position INTEGER NOT NULL,
    node_type TEXT NOT NULL, UNIQUE(version_id, code), UNIQUE(id, version_id), UNIQUE(parent_id, position)
);
CREATE TABLE IF NOT EXISTS indicator_normative_reference (
    model_version_id TEXT NOT NULL REFERENCES model_version(id), indicator_node_id TEXT NOT NULL,
    normative_reference_id TEXT NOT NULL REFERENCES normative_reference(id), source_pages TEXT, disposition TEXT,
    PRIMARY KEY (model_version_id, indicator_node_id, normative_reference_id)
);
CREATE TABLE IF NOT EXISTS required_source (
    id TEXT PRIMARY KEY, version_id TEXT NOT NULL REFERENCES model_version(id), indicator_node_id TEXT NOT NULL,
    code TEXT NOT NULL, source_type TEXT NOT NULL, name TEXT NOT NULL, description TEXT NOT NULL DEFAULT '',
    required INTEGER NOT NULL, cardinality_min INTEGER NOT NULL, cardinality_max INTEGER,
    period_rule TEXT NOT NULL, ordinal INTEGER NOT NULL, UNIQUE(indicator_node_id, code),
    UNIQUE(indicator_node_id, ordinal),
    FOREIGN KEY (indicator_node_id, version_id) REFERENCES model_node(id, version_id)
);
CREATE TABLE IF NOT EXISTS valuation_rule (
    id TEXT PRIMARY KEY, version_id TEXT NOT NULL REFERENCES model_version(id), indicator_node_id TEXT NOT NULL,
    rule_type TEXT NOT NULL, method_definition TEXT NOT NULL, formula_definition TEXT, strategy_key TEXT,
    strategy_version TEXT, scale_id TEXT, missing_data_policy TEXT NOT NULL, status TEXT NOT NULL,
    UNIQUE(id, version_id),
    FOREIGN KEY (indicator_node_id, version_id) REFERENCES model_node(id, version_id)
);
CREATE TABLE IF NOT EXISTS valuation_variable (
    version_id TEXT NOT NULL REFERENCES model_version(id), rule_id TEXT NOT NULL, code TEXT NOT NULL,
    name TEXT NOT NULL, unit TEXT NOT NULL, data_type TEXT NOT NULL, required INTEGER NOT NULL,
    validation_definition TEXT NOT NULL, ordinal INTEGER NOT NULL, PRIMARY KEY (rule_id, code),
    UNIQUE(rule_id, ordinal), FOREIGN KEY (rule_id, version_id) REFERENCES valuation_rule(id, version_id)
);
CREATE TABLE IF NOT EXISTS valuation_threshold (
    version_id TEXT NOT NULL REFERENCES model_version(id), rule_id TEXT NOT NULL, rating_option_id TEXT NOT NULL,
    lower_bound NUMERIC, upper_bound NUMERIC, lower_inclusive INTEGER NOT NULL, upper_inclusive INTEGER NOT NULL,
    PRIMARY KEY (rule_id, rating_option_id),
    FOREIGN KEY (rule_id, version_id) REFERENCES valuation_rule(id, version_id)
);
CREATE TABLE IF NOT EXISTS model_career_applicability (
    id TEXT PRIMARY KEY, model_version_id TEXT NOT NULL REFERENCES model_version(id),
    career_id TEXT NOT NULL REFERENCES career(id), status TEXT NOT NULL, valid_from TEXT NOT NULL, valid_until TEXT,
    authority_function TEXT NOT NULL, legal_basis TEXT NOT NULL, document_reference TEXT NOT NULL,
    reason TEXT NOT NULL, predecessor_id TEXT REFERENCES model_career_applicability(id),
    revision INTEGER NOT NULL, decided_by TEXT NOT NULL REFERENCES access_identity(id), decided_at TEXT NOT NULL,
    UNIQUE(model_version_id, career_id, revision),
    CHECK (valid_until IS NULL OR valid_until >= valid_from)
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
CREATE TABLE IF NOT EXISTS process_team (
    id TEXT PRIMARY KEY, process_id TEXT NOT NULL REFERENCES self_assessment_process(id), name TEXT NOT NULL,
    process_role TEXT NOT NULL, valid_from TEXT NOT NULL, valid_until TEXT NOT NULL, created_by TEXT NOT NULL,
    created_at TEXT NOT NULL, revision INTEGER NOT NULL DEFAULT 0, CHECK (valid_from < valid_until)
);
CREATE TABLE IF NOT EXISTS team_membership (
    id TEXT PRIMARY KEY, team_id TEXT NOT NULL REFERENCES process_team(id), user_id TEXT NOT NULL,
    role_assignment_id TEXT NOT NULL, valid_from TEXT NOT NULL, valid_until TEXT NOT NULL,
    created_by TEXT NOT NULL, created_at TEXT NOT NULL, CHECK (valid_from < valid_until)
);
CREATE TABLE IF NOT EXISTS process_assignment (
    id TEXT PRIMARY KEY, process_id TEXT NOT NULL REFERENCES self_assessment_process(id), indicator_id TEXT NOT NULL,
    identity_id TEXT, team_id TEXT REFERENCES process_team(id), function_code TEXT NOT NULL,
    valid_from TEXT NOT NULL, valid_to TEXT NOT NULL, created_by TEXT NOT NULL, reason TEXT NOT NULL DEFAULT '',
    revoked_at TEXT, revision INTEGER NOT NULL DEFAULT 0,
    CHECK ((identity_id IS NOT NULL) <> (team_id IS NOT NULL))
);
CREATE TABLE IF NOT EXISTS process_authorization (
    id TEXT PRIMARY KEY, process_id TEXT NOT NULL REFERENCES self_assessment_process(id),
    decision_type TEXT NOT NULL, authority_assignment_id TEXT NOT NULL, legal_basis TEXT NOT NULL,
    document_reference TEXT NOT NULL, reason TEXT NOT NULL, decided_by TEXT NOT NULL, decided_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS process_transition (
    id TEXT PRIMARY KEY, process_id TEXT NOT NULL REFERENCES self_assessment_process(id), from_state TEXT NOT NULL,
    to_state TEXT NOT NULL, actor_id TEXT NOT NULL, reason TEXT NOT NULL DEFAULT '', occurred_at TEXT NOT NULL,
    correlation_id TEXT NOT NULL, resulting_revision INTEGER NOT NULL, authority_assignment_id TEXT,
    legal_basis TEXT, document_reference TEXT
);
CREATE TABLE IF NOT EXISTS process_closure (
    id TEXT PRIMARY KEY, process_id TEXT NOT NULL REFERENCES self_assessment_process(id), closed_at TEXT NOT NULL,
    closed_by TEXT NOT NULL, reopened_at TEXT, reopened_by TEXT, reopen_reason TEXT
);
CREATE TABLE IF NOT EXISTS process_activity (
    id TEXT PRIMARY KEY, process_id TEXT NOT NULL REFERENCES self_assessment_process(id), indicator_id TEXT NOT NULL,
    required_source_id TEXT, assignment_id TEXT NOT NULL REFERENCES process_assignment(id), title TEXT NOT NULL,
    due_at TEXT NOT NULL, status TEXT NOT NULL, created_by TEXT NOT NULL, created_at TEXT NOT NULL,
    revision INTEGER NOT NULL DEFAULT 0, snapshot_json TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS process_activity_dependency (
    activity_id TEXT NOT NULL REFERENCES process_activity(id),
    depends_on_activity_id TEXT NOT NULL REFERENCES process_activity(id),
    PRIMARY KEY (activity_id, depends_on_activity_id), CHECK (activity_id <> depends_on_activity_id)
);
CREATE TABLE IF NOT EXISTS process_activity_revision (
    id TEXT PRIMARY KEY, activity_id TEXT NOT NULL REFERENCES process_activity(id), revision INTEGER NOT NULL,
    snapshot_json TEXT NOT NULL, recorded_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS evidence (
    id BLOB PRIMARY KEY, process_id BLOB NOT NULL, owner_id BLOB NOT NULL, created_at TIMESTAMP NOT NULL,
    correlation_id BLOB NOT NULL, latest_revision INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS evidence_version (
    id BLOB PRIMARY KEY, evidence_id BLOB NOT NULL REFERENCES evidence(id), revision_number INTEGER NOT NULL,
    predecessor_id BLOB, status TEXT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL,
    provenance TEXT NOT NULL, covered_from DATE NOT NULL, covered_to DATE NOT NULL, author_id BLOB NOT NULL,
    submitted_at TIMESTAMP, lock_revision INTEGER NOT NULL DEFAULT 0, created_at TIMESTAMP NOT NULL,
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

CREATE TABLE IF NOT EXISTS source_coverage (
    id BLOB PRIMARY KEY, version_id BLOB NOT NULL REFERENCES evidence_version(id),
    required_source_id BLOB NOT NULL, coverage_note TEXT NOT NULL, declared_by BLOB NOT NULL,
    declared_at TEXT NOT NULL, UNIQUE(version_id, required_source_id)
);
CREATE INDEX IF NOT EXISTS ix_source_coverage_version ON source_coverage(version_id);

CREATE TABLE IF NOT EXISTS applicability_decision (
    id BLOB PRIMARY KEY, process_id BLOB NOT NULL REFERENCES self_assessment_process(id),
    subject_type TEXT NOT NULL CHECK (subject_type IN ('INDICATOR','REQUIRED_SOURCE')),
    subject_id BLOB NOT NULL, decision TEXT NOT NULL CHECK (decision IN ('APPLICABLE','NOT_APPLICABLE')),
    rule_reference TEXT NOT NULL, justification TEXT NOT NULL, legal_basis TEXT NOT NULL,
    document_reference TEXT NOT NULL, authorized_by BLOB NOT NULL,
    decided_at TEXT NOT NULL, predecessor_id BLOB REFERENCES applicability_decision(id),
    revision INTEGER NOT NULL, UNIQUE(process_id, subject_type, subject_id, revision)
);
CREATE INDEX IF NOT EXISTS ix_applicability_decision_subject
    ON applicability_decision(process_id, subject_type, subject_id, revision);

CREATE TABLE IF NOT EXISTS evidence_structured_value (
    id BLOB PRIMARY KEY, version_id BLOB NOT NULL REFERENCES evidence_version(id),
    required_source_id BLOB NOT NULL,
    kind TEXT NOT NULL CHECK (kind IN ('STRUCTURED_DATA','INSTITUTIONAL_REFERENCE')),
    value_type TEXT NOT NULL CHECK (value_type IN ('NUMBER','STRING','BOOLEAN')),
    value_text TEXT NOT NULL, unit TEXT, period_start TEXT NOT NULL, period_end TEXT NOT NULL,
    reference TEXT, created_by BLOB NOT NULL, created_at TEXT NOT NULL,
    UNIQUE(version_id, required_source_id), CHECK (period_start <= period_end)
);
CREATE INDEX IF NOT EXISTS ix_evidence_structured_value_version
    ON evidence_structured_value(version_id);

CREATE TABLE IF NOT EXISTS document_review_decision (
    id BLOB PRIMARY KEY, version_id BLOB NOT NULL REFERENCES evidence_version(id),
    decision TEXT NOT NULL CHECK (decision IN ('ACCEPTED','OBSERVED','REJECTED')),
    justification TEXT NOT NULL, reviewer_id BLOB NOT NULL, reviewed_at TEXT NOT NULL,
    self_review_exception INTEGER NOT NULL DEFAULT 0, exception_reason TEXT,
    predecessor_id BLOB REFERENCES document_review_decision(id), successor_version_id BLOB,
    revision INTEGER NOT NULL, UNIQUE(version_id, revision), UNIQUE(successor_version_id)
);
CREATE INDEX IF NOT EXISTS ix_document_review_version
    ON document_review_decision(version_id, revision);
CREATE TABLE IF NOT EXISTS document_review_outcome (
    id BLOB PRIMARY KEY, decision_id BLOB NOT NULL REFERENCES document_review_decision(id),
    required_source_id BLOB NOT NULL,
    outcome TEXT NOT NULL CHECK (outcome IN ('ACCEPTED','OBSERVED','REJECTED')),
    note TEXT, UNIQUE(decision_id, required_source_id)
);
CREATE INDEX IF NOT EXISTS ix_document_review_outcome_decision
    ON document_review_outcome(decision_id);

CREATE TABLE IF NOT EXISTS manual_academic_evaluation (
    id BLOB PRIMARY KEY, process_id BLOB NOT NULL REFERENCES self_assessment_process(id), indicator_id BLOB NOT NULL,
    evaluator_id BLOB NOT NULL REFERENCES access_identity(id), revision INTEGER NOT NULL CHECK(revision>=1),
    predecessor_id BLOB UNIQUE, evaluated_at TIMESTAMP NOT NULL, payload_json TEXT NOT NULL CHECK(json_valid(payload_json)),
    UNIQUE(process_id,indicator_id,evaluator_id,revision), UNIQUE(id,process_id,indicator_id,evaluator_id),
    FOREIGN KEY(predecessor_id,process_id,indicator_id,evaluator_id)
        REFERENCES manual_academic_evaluation(id,process_id,indicator_id,evaluator_id),
    CHECK((revision=1 AND predecessor_id IS NULL) OR (revision>1 AND predecessor_id IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS ix_manual_evaluation_history ON manual_academic_evaluation(process_id,indicator_id,evaluated_at DESC,id DESC);

CREATE TABLE IF NOT EXISTS quantitative_input_set (
    id BLOB PRIMARY KEY, process_id BLOB NOT NULL REFERENCES self_assessment_process(id), indicator_id BLOB NOT NULL,
    captured_by BLOB NOT NULL REFERENCES access_identity(id), revision INTEGER NOT NULL CHECK(revision>=1),
    predecessor_id BLOB UNIQUE, captured_at TIMESTAMP NOT NULL, payload_json TEXT NOT NULL CHECK(json_valid(payload_json)),
    UNIQUE(process_id,indicator_id,captured_by,revision), UNIQUE(id,process_id,indicator_id,captured_by), UNIQUE(id,process_id,indicator_id),
    FOREIGN KEY(predecessor_id,process_id,indicator_id,captured_by) REFERENCES quantitative_input_set(id,process_id,indicator_id,captured_by),
    CHECK((revision=1 AND predecessor_id IS NULL) OR (revision>1 AND predecessor_id IS NOT NULL))
);
CREATE TABLE IF NOT EXISTS quantitative_verification (
    input_set_id BLOB PRIMARY KEY REFERENCES quantitative_input_set(id),verified_by BLOB NOT NULL REFERENCES access_identity(id),
    verified_at TIMESTAMP NOT NULL,payload_json TEXT NOT NULL CHECK(json_valid(payload_json))
);
CREATE TABLE IF NOT EXISTS quantitative_calculation (
    id BLOB PRIMARY KEY,input_set_id BLOB NOT NULL UNIQUE REFERENCES quantitative_verification(input_set_id),
    process_id BLOB NOT NULL,indicator_id BLOB NOT NULL,executed_by BLOB NOT NULL REFERENCES access_identity(id),
    executed_at TIMESTAMP NOT NULL,payload_json TEXT NOT NULL CHECK(json_valid(payload_json)),
    FOREIGN KEY(input_set_id,process_id,indicator_id) REFERENCES quantitative_input_set(id,process_id,indicator_id)
);
CREATE INDEX IF NOT EXISTS ix_quantitative_history ON quantitative_calculation(process_id,indicator_id,executed_at DESC,id DESC);

CREATE TABLE IF NOT EXISTS academic_reconciliation_policy (
    id BLOB PRIMARY KEY,process_id BLOB NOT NULL REFERENCES self_assessment_process(id),indicator_id BLOB NOT NULL,
    revision INTEGER NOT NULL CHECK(revision>=1),predecessor_id BLOB UNIQUE,configured_by BLOB NOT NULL REFERENCES access_identity(id),
    authority_assignment_id BLOB NOT NULL REFERENCES functional_assignment(id),configured_at TIMESTAMP NOT NULL,payload_json TEXT NOT NULL CHECK(json_valid(payload_json)),
    UNIQUE(process_id,indicator_id,revision),UNIQUE(id,process_id,indicator_id),
    FOREIGN KEY(predecessor_id,process_id,indicator_id) REFERENCES academic_reconciliation_policy(id,process_id,indicator_id),
    CHECK((revision=1 AND predecessor_id IS NULL) OR(revision>1 AND predecessor_id IS NOT NULL))
);
CREATE TABLE IF NOT EXISTS academic_disagreement_record (
    id BLOB PRIMARY KEY,process_id BLOB NOT NULL REFERENCES self_assessment_process(id),indicator_id BLOB NOT NULL,
    revision INTEGER NOT NULL CHECK(revision>=1),predecessor_id BLOB UNIQUE,opened_by BLOB NOT NULL REFERENCES access_identity(id),
    opened_at TIMESTAMP NOT NULL,payload_json TEXT NOT NULL CHECK(json_valid(payload_json)),
    UNIQUE(process_id,indicator_id,revision),UNIQUE(id,process_id,indicator_id),
    FOREIGN KEY(predecessor_id,process_id,indicator_id) REFERENCES academic_disagreement_record(id,process_id,indicator_id),
    CHECK((revision=1 AND predecessor_id IS NULL) OR(revision>1 AND predecessor_id IS NOT NULL))
);
CREATE TABLE IF NOT EXISTS academic_reconciliation_resolution (
    id BLOB PRIMARY KEY,process_id BLOB NOT NULL,indicator_id BLOB NOT NULL,disagreement_id BLOB NOT NULL UNIQUE,policy_id BLOB NOT NULL,
    resolved_by BLOB NOT NULL REFERENCES access_identity(id),authority_assignment_id BLOB NOT NULL REFERENCES functional_assignment(id),
    resolved_at TIMESTAMP NOT NULL,payload_json TEXT NOT NULL CHECK(json_valid(payload_json)),
    FOREIGN KEY(disagreement_id,process_id,indicator_id) REFERENCES academic_disagreement_record(id,process_id,indicator_id),
    FOREIGN KEY(policy_id,process_id,indicator_id) REFERENCES academic_reconciliation_policy(id,process_id,indicator_id)
);
CREATE INDEX IF NOT EXISTS ix_academic_current_run ON quantitative_calculation(process_id,indicator_id,executed_by,executed_at DESC,id DESC);

CREATE TABLE IF NOT EXISTS governance_finding (
    id BLOB PRIMARY KEY, process_id BLOB NOT NULL REFERENCES self_assessment_process(id),
    registered_by BLOB NOT NULL REFERENCES access_identity(id), registered_at TIMESTAMP NOT NULL,
    payload_json TEXT NOT NULL CHECK(json_valid(payload_json))
);
CREATE INDEX IF NOT EXISTS ix_governance_finding ON governance_finding(process_id, registered_at DESC, id DESC);
CREATE TABLE IF NOT EXISTS governance_finding_consolidation (
    id BLOB PRIMARY KEY, process_id BLOB NOT NULL REFERENCES self_assessment_process(id),
    revision INTEGER NOT NULL CHECK(revision>=1), predecessor_id BLOB UNIQUE,
    consolidated_by BLOB NOT NULL REFERENCES access_identity(id), consolidated_at TIMESTAMP NOT NULL,
    payload_json TEXT NOT NULL CHECK(json_valid(payload_json)),
    UNIQUE(process_id,revision), UNIQUE(id,process_id),
    FOREIGN KEY(predecessor_id,process_id) REFERENCES governance_finding_consolidation(id,process_id),
    CHECK((revision=1 AND predecessor_id IS NULL) OR (revision>1 AND predecessor_id IS NOT NULL))
);
CREATE TABLE IF NOT EXISTS governance_consolidation_finding (
    consolidation_id BLOB NOT NULL REFERENCES governance_finding_consolidation(id) ON DELETE CASCADE,
    finding_id BLOB NOT NULL REFERENCES governance_finding(id) ON DELETE RESTRICT,
    PRIMARY KEY(consolidation_id,finding_id)
);
CREATE TABLE IF NOT EXISTS governance_report_version (
    id BLOB PRIMARY KEY, process_id BLOB NOT NULL REFERENCES self_assessment_process(id),
    version_number INTEGER NOT NULL CHECK(version_number>=1), predecessor_version_id BLOB UNIQUE,
    emitted_by BLOB NOT NULL REFERENCES access_identity(id), emitted_at TIMESTAMP NOT NULL,
    content_sha256 TEXT NOT NULL, payload_json TEXT NOT NULL CHECK(json_valid(payload_json)),
    UNIQUE(process_id,version_number), UNIQUE(id,process_id),
    FOREIGN KEY(predecessor_version_id,process_id) REFERENCES governance_report_version(id,process_id)
);
CREATE INDEX IF NOT EXISTS ix_governance_report_versions ON governance_report_version(process_id, version_number ASC);
CREATE TABLE IF NOT EXISTS governance_report_processing (
    version_id BLOB PRIMARY KEY REFERENCES governance_report_version(id), process_id BLOB NOT NULL,
    state TEXT NOT NULL CHECK(state IN ('PRELIMINARY','UNDER_OBSERVATION','FINAL','APPROVED','RETURNED')),
    revision INTEGER NOT NULL CHECK(revision>=0), updated_at TIMESTAMP NOT NULL
);
CREATE TABLE IF NOT EXISTS governance_report_observation (
    id BLOB PRIMARY KEY, version_id BLOB NOT NULL REFERENCES governance_report_version(id),
    observed_by BLOB NOT NULL REFERENCES access_identity(id), observed_at TIMESTAMP NOT NULL,
    payload_json TEXT NOT NULL CHECK(json_valid(payload_json))
);
CREATE TABLE IF NOT EXISTS governance_report_reply (
    id BLOB PRIMARY KEY, observation_id BLOB NOT NULL UNIQUE REFERENCES governance_report_observation(id),
    replied_by BLOB NOT NULL REFERENCES access_identity(id), replied_at TIMESTAMP NOT NULL,
    payload_json TEXT NOT NULL CHECK(json_valid(payload_json))
);
CREATE TABLE IF NOT EXISTS governance_report_socialization (
    id BLOB PRIMARY KEY, version_id BLOB NOT NULL REFERENCES governance_report_version(id),
    socialized_by BLOB NOT NULL REFERENCES access_identity(id), recorded_at TIMESTAMP NOT NULL,
    payload_json TEXT NOT NULL CHECK(json_valid(payload_json))
);
CREATE TABLE IF NOT EXISTS governance_report_decision (
    id BLOB PRIMARY KEY, version_id BLOB NOT NULL REFERENCES governance_report_version(id),
    decided_by BLOB NOT NULL REFERENCES access_identity(id),
    authority_assignment_id BLOB NOT NULL REFERENCES functional_assignment(id), decided_at TIMESTAMP NOT NULL,
    payload_json TEXT NOT NULL CHECK(json_valid(payload_json)), UNIQUE(version_id)
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

CREATE TABLE IF NOT EXISTS improvement_plan (
    id BLOB PRIMARY KEY, process_id BLOB NOT NULL REFERENCES self_assessment_process(id),
    report_version_id BLOB NOT NULL REFERENCES governance_report_version(id),
    created_by BLOB NOT NULL REFERENCES access_identity(id), created_at TIMESTAMP NOT NULL,
    payload_json TEXT NOT NULL CHECK(json_valid(payload_json))
);
CREATE INDEX IF NOT EXISTS ix_improvement_plan ON improvement_plan(process_id, created_at ASC, id ASC);
CREATE TABLE IF NOT EXISTS improvement_action (
    id BLOB PRIMARY KEY, process_id BLOB NOT NULL REFERENCES self_assessment_process(id),
    plan_id BLOB NOT NULL REFERENCES improvement_plan(id),
    state TEXT NOT NULL CHECK(state IN ('PROPOSED','APPROVED','IN_PROGRESS','PENDING_VERIFICATION','VERIFIED')),
    revision INTEGER NOT NULL CHECK(revision>=1), revised_by BLOB NOT NULL REFERENCES access_identity(id),
    revised_at TIMESTAMP NOT NULL, payload_json TEXT NOT NULL CHECK(json_valid(payload_json))
);
CREATE INDEX IF NOT EXISTS ix_improvement_action ON improvement_action(process_id, revised_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS ix_improvement_action_plan ON improvement_action(plan_id);
CREATE TABLE IF NOT EXISTS improvement_action_revision (
    action_id BLOB NOT NULL REFERENCES improvement_action(id), revision INTEGER NOT NULL CHECK(revision>=1),
    predecessor_revision INTEGER,
    state TEXT NOT NULL CHECK(state IN ('PROPOSED','APPROVED','IN_PROGRESS','PENDING_VERIFICATION','VERIFIED')),
    changed_by BLOB NOT NULL REFERENCES access_identity(id), changed_at TIMESTAMP NOT NULL,
    payload_json TEXT NOT NULL CHECK(json_valid(payload_json)),
    PRIMARY KEY(action_id,revision),
    FOREIGN KEY(action_id,predecessor_revision) REFERENCES improvement_action_revision(action_id,revision),
    CHECK((revision=1 AND predecessor_revision IS NULL) OR (revision>1 AND predecessor_revision IS NOT NULL))
);
CREATE TABLE IF NOT EXISTS improvement_progress (
    id BLOB PRIMARY KEY, action_id BLOB NOT NULL REFERENCES improvement_action(id),
    recorded_by BLOB NOT NULL REFERENCES access_identity(id), recorded_at TIMESTAMP NOT NULL,
    payload_json TEXT NOT NULL CHECK(json_valid(payload_json))
);
CREATE INDEX IF NOT EXISTS ix_improvement_progress ON improvement_progress(action_id, recorded_at ASC, id ASC);
CREATE TABLE IF NOT EXISTS improvement_decision (
    id BLOB PRIMARY KEY, action_id BLOB NOT NULL REFERENCES improvement_action(id),
    action_code TEXT NOT NULL CHECK(action_code IN
        ('APPROVE','RETURN','START','SUBMIT_VERIFICATION','VERIFY','REQUEST_CORRECTION')),
    decided_by BLOB NOT NULL REFERENCES access_identity(id), decided_at TIMESTAMP NOT NULL,
    payload_json TEXT NOT NULL CHECK(json_valid(payload_json))
);
CREATE INDEX IF NOT EXISTS ix_improvement_decision ON improvement_decision(action_id, decided_at ASC, id ASC);
CREATE TABLE IF NOT EXISTS closure_policy (
    code TEXT PRIMARY KEY,
    strict INTEGER NOT NULL,
    active INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL
);
INSERT OR IGNORE INTO closure_policy(code,strict,active,created_at) VALUES('INTEGRAL_STRICT_V1',1,1,strftime('%Y-%m-%dT%H:%M:%fZ','now'));
CREATE TABLE IF NOT EXISTS integral_closure (
    id TEXT PRIMARY KEY,
    process_id TEXT NOT NULL REFERENCES self_assessment_process(id),
    documentary_closure_id TEXT NOT NULL REFERENCES documentary_closure(id),
    policy_version TEXT NOT NULL REFERENCES closure_policy(code),
    payload_json TEXT NOT NULL CHECK(json_valid(payload_json) AND json_type(payload_json)='object'),
    sha256 TEXT NOT NULL CHECK(length(sha256)=64 AND sha256 NOT GLOB '*[^a-f0-9]*'),
    pending_ids TEXT NOT NULL DEFAULT '[]' CHECK(json_valid(pending_ids)),
    exception_decision_ids TEXT NOT NULL DEFAULT '[]' CHECK(json_valid(exception_decision_ids)),
    closed_by TEXT NOT NULL REFERENCES access_identity(id),
    closed_at TIMESTAMP NOT NULL,
    revision INTEGER NOT NULL DEFAULT 1 CHECK(revision>=1)
);
CREATE INDEX IF NOT EXISTS ix_integral_closure_history ON integral_closure(process_id, closed_at DESC, id DESC);
CREATE UNIQUE INDEX IF NOT EXISTS ux_integral_closure_documentary ON integral_closure(process_id, documentary_closure_id);
CREATE TABLE IF NOT EXISTS archive_record (
    id TEXT PRIMARY KEY,
    process_id TEXT NOT NULL REFERENCES self_assessment_process(id),
    closure_snapshot_id TEXT NOT NULL REFERENCES integral_closure(id),
    retention_policy_version TEXT NOT NULL,
    archived_by TEXT NOT NULL REFERENCES access_identity(id),
    archived_at TIMESTAMP NOT NULL,
    integrity_hash TEXT NOT NULL CHECK(length(integrity_hash)=64 AND integrity_hash NOT GLOB '*[^a-f0-9]*'),
    integrity_status TEXT NOT NULL CHECK(integrity_status IN ('VERIFIED','FAILED')),
    UNIQUE(closure_snapshot_id)
);
CREATE INDEX IF NOT EXISTS ix_archive_record_history ON archive_record(process_id, archived_at DESC, id DESC);
INSERT OR IGNORE INTO academic_unit VALUES('00000000-0000-0000-0000-000000000008','LEGACY-UNCLASSIFIED','Unidad pendiente de clasificación','DISABLED',1,'00000000-0000-0000-0000-000000000000',strftime('%Y-%m-%dT%H:%M:%fZ','now'));
INSERT OR IGNORE INTO career(id,academic_unit_id,code,name,status,valid_from,classification_pending,revision,created_by,created_at)
SELECT id,'00000000-0000-0000-0000-000000000008','LEGACY-'||upper(id),'Carrera pendiente de clasificación','DISABLED','1900-01-01',1,1,'00000000-0000-0000-0000-000000000000',strftime('%Y-%m-%dT%H:%M:%fZ','now')
FROM (SELECT career_id AS id FROM self_assessment_process UNION SELECT career_id FROM functional_assignment UNION SELECT career_id FROM conflict_exception);
INSERT OR IGNORE INTO academic_unit_history SELECT id,revision,json_object('id',id,'data',json_object('code',code,'name',name),'status',status,'revision',revision,'createdBy',created_by,'createdAt',created_at),created_by,created_at,'LEGACY_REFERENCE_RECONCILIATION' FROM academic_unit;
INSERT OR IGNORE INTO career_history SELECT id,revision,json_object('id',id,'data',json_object('code',code,'name',name,'academicUnitId',academic_unit_id,'validFrom',valid_from,'validUntil',valid_until),'status',status,'revision',revision,'createdBy',created_by,'createdAt',created_at,'classificationPending',json('true')),created_by,created_at,'LEGACY_REFERENCE_RECONCILIATION' FROM career WHERE classification_pending=1;

