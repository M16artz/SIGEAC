# Data Model: I3 — Recopilación y remisión de evidencias

## Design principles

- `Evidence` identifies one lineage; `EvidenceVersion` owns all content.
- `DRAFT` is mutable under optimistic concurrency. `SUBMITTED` and its children are append-only.
- File bytes remain outside PostgreSQL; associations never grant cross-scope access.
- Historical foreign keys use `RESTRICT`, not cascading deletion.
- Validation and retention policies are synthetic configuration, not CL-001–CL-010 decisions.

## Relationships

```text
SelfAssessmentProcess 1 ── * Evidence 1 ── * EvidenceVersion
                                      │       ├── * VersionIndicator * ── 1 IndicatorSnapshot
                                      │       ├── * VersionFile * ── 1 FileAsset
                                      │       └── * ExternalReference
                                      └── predecessor (same Evidence, linear chain)

FileAsset 1 ── * FileRecoveryOperation
Evidence/EvidenceVersion 1 ── * AuditEvent (logical, append-only)
```

## Evidence

| Field | Type | Rules |
|---|---|---|
| `id` | UUID | Primary key, server generated |
| `process_id` | UUID | Required process reference through I2 |
| `owner_id` | UUID | Required historical author |
| `created_at` | instant | Required UTC |
| `latest_revision_number` | integer | Starts at 0; incremented under root lock |
| `created_correlation_id` | string | Required safe correlation |

Indexes: `(process_id, owner_id)`, `(process_id, created_at desc)`. The root does not duplicate version content and cannot be deleted while versions exist.

## EvidenceVersion

| Field | Type | Rules |
|---|---|---|
| `id` | UUID | Primary key |
| `evidence_id` | UUID | Required FK `RESTRICT` |
| `revision_number` | integer | Positive |
| `predecessor_id` | UUID nullable | Null only for v1; otherwise same evidence |
| `status` | enum | `DRAFT` or `SUBMITTED` |
| `title` | string | Trimmed, required before submission |
| `description` | text | Trimmed, required before submission |
| `provenance` | text | Trimmed, required before submission |
| `covered_from`, `covered_to` | date | Required before submission; ordered inclusively |
| `created_by`, `created_at` | UUID, instant | Required |
| `submitted_by`, `submitted_at` | UUID, instant nullable | Both null in draft; both set in submitted |
| `lock_version` | bigint | Optimistic concurrency/ETag while draft |

Constraints: unique `(evidence_id, revision_number)`; unique non-null `predecessor_id`; composite FK `(predecessor_id,evidence_id)` to the same evidence; partial unique index for one `DRAFT` per evidence; index `(evidence_id,revision_number desc)`.

```text
create evidence → v1 DRAFT → submit → v1 SUBMITTED
                                      └ successor → v2 DRAFT → submit → v2 SUBMITTED
```

The only transition on a row later considered submitted is `DRAFT → SUBMITTED`. Database triggers reject subsequent `UPDATE`/`DELETE` on submitted versions and children.

## VersionIndicator

| Field | Type | Rules |
|---|---|---|
| `evidence_version_id` | UUID | Composite primary/FK |
| `indicator_id` | UUID | Composite primary; belongs to process model version |
| `indicator_code_snapshot` | string | Immutable display snapshot |
| `indicator_title_snapshot` | string | Immutable display snapshot |

At least one row is required to submit. Draft mutations revalidate current responsibility; submitted snapshots remain after assignments expire.

## FileAsset

| Field | Type | Rules |
|---|---|---|
| `id` | UUID | Primary key |
| `storage_key` | string | Unique, opaque, never public |
| `staging_key` | string nullable | Opaque recovery key |
| `upload_operation_id` | UUID | Unique idempotency identity |
| `original_name` | string | Sanitized name, not a path |
| `declared_media_type` | string | Required |
| `detected_media_type` | string | Required after detection |
| `size_bytes` | bigint | `0..26_214_400`; policy decides empty-file acceptance |
| `sha256` | char(64) | Lowercase hexadecimal, non-unique |
| `validation_policy_id` | UUID | Exact policy/version used |
| `status` | enum | `STAGED`, `AVAILABLE`, `QUARANTINED`, `MISSING` |
| `created_at`, `promoted_at`, `last_verified_at` | instant | Last two nullable |

Indexes: unique `storage_key`, unique `upload_operation_id`, non-unique `sha256`, `(status,created_at)`. Identity metadata freezes at `AVAILABLE`; reconciliation may change only operational state.

## VersionFile

| Field | Type | Rules |
|---|---|---|
| `evidence_version_id`, `file_asset_id` | UUID | Composite primary/FKs |
| `original_name_snapshot` | string | Immutable |
| `declared_media_type_snapshot` | string | Immutable |
| `detected_media_type_snapshot` | string | Immutable |
| `size_bytes_snapshot` | bigint | Immutable |
| `sha256_snapshot` | char(64) | Immutable |
| `position` | integer | Stable ordering |

A draft may add/remove associations. Submission requires every asset `AVAILABLE` and matching its snapshot. Submitted associations never change.

## ExternalReference

| Field | Type | Rules |
|---|---|---|
| `id` | UUID | Primary key |
| `evidence_version_id` | UUID | Required FK |
| `uri` | string | Absolute permitted URI, bounded length |
| `description` | string | Required, trimmed |
| `position` | integer | Stable ordering |

The URI is validated structurally but never dereferenced by I3. A reference remains a declaration; it never becomes a local file, availability claim or integrity claim.

## FileValidationPolicy

| Field | Type | Rules |
|---|---|---|
| `id` | UUID | Primary key |
| `version` | integer | Unique positive version |
| `max_bytes` | bigint | Pilot value 26 214 400 |
| `allow_empty` | boolean | Configurable |
| `active_from`, `active_to` | instant | Non-overlapping range |
| `source_note` | string | Synthetic/provisional provenance |

Children define allowed detected types and compatible declared/detected pairs. Each asset stores the policy used so later changes do not rewrite history.

## FileRecoveryOperation

| Field | Type | Rules |
|---|---|---|
| `id` | UUID | Primary/idempotency key |
| `file_asset_id` | UUID nullable | May be absent for discovered orphan |
| `operation` | enum | `PROMOTE`, `VERIFY`, `QUARANTINE`, `CLEANUP` |
| `status` | enum | `PENDING`, `RUNNING`, `SUCCEEDED`, `FAILED_RETRYABLE`, `FAILED_FINAL` |
| `attempt_count` | integer | Non-negative |
| `not_before`, `lease_until` | instant | Lease nullable |
| `reason_code` | string nullable | Safe code |
| `correlation_id` | string | Required |
| `created_at`, `updated_at` | instant | Required |

The worker is idempotent, leased and audited. Delays come from configuration; no fixed retention policy is encoded.

## Submission invariants

In one short transaction after authorization and root locking:

1. version is `DRAFT` and `If-Match` equals `lock_version`;
2. text and ordered dates are complete;
3. one or more authorized indicators belong to the process model version;
4. one or more sources exist;
5. every file is `AVAILABLE`, policy-compliant and integrity-verifiable;
6. revision and predecessor form the linear chain;
7. version and child snapshots become immutable;
8. the submission audit event succeeds before commit.

Failure leaves the draft editable and creates no submitted version.

## Recovery invariants

- `AVAILABLE` is never returned without a confirmed final object.
- Promoted objects without confirmed association remain identifiable and reconcilable.
- Missing/corrupt objects block submission/download and become `MISSING`/`QUARANTINED` without rewriting a submitted snapshot.
- No automatic recovery deletes an object referenced by a submitted version.
- Backup/restore validation reconciles PostgreSQL metadata with the external file root.
