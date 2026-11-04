CREATE TABLE evidence (
    id uuid PRIMARY KEY,
    process_id uuid NOT NULL REFERENCES self_assessment_process(id) ON DELETE RESTRICT,
    owner_id uuid NOT NULL,
    created_at timestamptz NOT NULL,
    correlation_id uuid NOT NULL,
    latest_revision integer NOT NULL DEFAULT 0 CHECK (latest_revision >= 0)
);

CREATE TABLE evidence_version (
    id uuid PRIMARY KEY,
    evidence_id uuid NOT NULL REFERENCES evidence(id) ON DELETE RESTRICT,
    revision_number integer NOT NULL CHECK (revision_number > 0),
    predecessor_id uuid,
    status varchar(20) NOT NULL CHECK (status IN ('DRAFT','SUBMITTED')),
    title varchar(300) NOT NULL CHECK (length(title) BETWEEN 1 AND 300),
    description varchar(8000) NOT NULL CHECK (length(description) BETWEEN 1 AND 8000),
    provenance varchar(4000) NOT NULL CHECK (length(provenance) BETWEEN 1 AND 4000),
    covered_from date NOT NULL,
    covered_to date NOT NULL,
    author_id uuid NOT NULL,
    submitted_at timestamptz,
    lock_revision bigint NOT NULL DEFAULT 0 CHECK (lock_revision >= 0),
    created_at timestamptz NOT NULL,
    UNIQUE(evidence_id, revision_number), UNIQUE(id,evidence_id), UNIQUE(predecessor_id),
    CHECK (covered_from <= covered_to),
    CHECK ((revision_number=1 AND predecessor_id IS NULL) OR (revision_number>1 AND predecessor_id IS NOT NULL)),
    FOREIGN KEY(predecessor_id,evidence_id) REFERENCES evidence_version(id,evidence_id) ON DELETE RESTRICT
);
CREATE UNIQUE INDEX ux_evidence_one_draft ON evidence_version(evidence_id) WHERE status='DRAFT';

CREATE TABLE evidence_version_indicator (
    version_id uuid NOT NULL REFERENCES evidence_version(id) ON DELETE RESTRICT,
    indicator_id uuid NOT NULL,
    indicator_code varchar(100) NOT NULL,
    indicator_title varchar(300) NOT NULL,
    position integer NOT NULL CHECK(position>0),
    PRIMARY KEY(version_id,indicator_id), UNIQUE(version_id,position)
);

CREATE TABLE file_validation_policy (
    id uuid PRIMARY KEY, policy_code varchar(100) NOT NULL UNIQUE, max_bytes bigint NOT NULL CHECK(max_bytes BETWEEN 0 AND 26214400),
    allow_empty boolean NOT NULL, allowed_detected_types jsonb NOT NULL, compatibility_json jsonb NOT NULL,
    active boolean NOT NULL, created_at timestamptz NOT NULL
);

CREATE TABLE file_asset (
    id uuid PRIMARY KEY, upload_id uuid NOT NULL UNIQUE, storage_key varchar(500) UNIQUE, staging_key varchar(500) UNIQUE,
    original_name varchar(500) NOT NULL, declared_media_type varchar(255) NOT NULL, detected_media_type varchar(255) NOT NULL,
    size_bytes bigint NOT NULL CHECK(size_bytes BETWEEN 0 AND 26214400), sha256 char(64) NOT NULL CHECK(sha256 ~ '^[0-9a-f]{64}$'),
    validation_policy_id uuid NOT NULL REFERENCES file_validation_policy(id) ON DELETE RESTRICT,
    status varchar(20) NOT NULL CHECK(status IN('STAGED','AVAILABLE','QUARANTINED','MISSING')), created_at timestamptz NOT NULL
);
CREATE INDEX ix_file_asset_sha256 ON file_asset(sha256);

CREATE TABLE evidence_version_file (
    version_id uuid NOT NULL REFERENCES evidence_version(id) ON DELETE RESTRICT,
    file_id uuid NOT NULL REFERENCES file_asset(id) ON DELETE RESTRICT,
    original_name varchar(500) NOT NULL, declared_media_type varchar(255) NOT NULL, detected_media_type varchar(255) NOT NULL,
    size_bytes bigint NOT NULL CHECK(size_bytes BETWEEN 0 AND 26214400), sha256 char(64) NOT NULL CHECK(sha256 ~ '^[0-9a-f]{64}$'),
    position integer NOT NULL CHECK(position>0), PRIMARY KEY(version_id,file_id), UNIQUE(version_id,position)
);

CREATE TABLE evidence_external_reference (
    id uuid PRIMARY KEY, version_id uuid NOT NULL REFERENCES evidence_version(id) ON DELETE RESTRICT,
    uri varchar(2048) NOT NULL CHECK(length(uri) BETWEEN 1 AND 2048),
    description varchar(1000) NOT NULL CHECK(length(description) BETWEEN 1 AND 1000),
    position integer NOT NULL CHECK(position>0), UNIQUE(version_id,position)
);

CREATE TABLE file_recovery_operation (
    id uuid PRIMARY KEY, file_id uuid REFERENCES file_asset(id) ON DELETE RESTRICT,
    operation varchar(20) NOT NULL CHECK(operation IN('PROMOTE','VERIFY','QUARANTINE','CLEANUP')),
    status varchar(20) NOT NULL CHECK(status IN('PENDING','LEASED','SUCCEEDED','FAILED')),
    attempts integer NOT NULL DEFAULT 0 CHECK(attempts>=0), not_before timestamptz NOT NULL,
    lease_owner varchar(200), lease_until timestamptz, safe_reason varchar(500), correlation_id uuid NOT NULL,
    created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL
);
CREATE INDEX ix_file_recovery_due ON file_recovery_operation(status,not_before) WHERE status IN('PENDING','FAILED');
CREATE INDEX ix_file_recovery_lease ON file_recovery_operation(lease_until) WHERE status='LEASED';
