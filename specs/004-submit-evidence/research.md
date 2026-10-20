# Research: I3 — Recopilación y remisión de evidencias

**Date**: 2026-10-20  
**Status**: Complete — no unresolved clarifications

## 1. Aggregate and immutable snapshots

**Decision**: `Evidence` is the stable lineage root (`id`, process, owner and creation metadata). `EvidenceVersion` owns all editable/remitted content. A version is mutable only while `DRAFT`; submission performs the single transition to `SUBMITTED`, fills submission metadata and makes the row and all child collections immutable. A correction creates the next `DRAFT` from the exact submitted predecessor.

**Rationale**: This preserves the existing project vocabulary and contract while removing the prior duplication of title/source between `Evidence` and `EvidenceVersion`. Each submitted version is a complete snapshot, and the acceptance test can compare v1 before and after v2.

**Alternatives considered**: Separate `EvidenceDraft` and `EvidenceVersion` tables add mapping/copying tables without improving the observable guarantee when database guards protect submitted rows. Metadata on `Evidence` creates two mutable sources of truth. Event sourcing is disproportionate; snapshots plus append-only audit provide the required history.

## 2. Version chain and concurrency

**Decision**: Draft edits use an optimistic lock exposed as a strong ETag; every mutation requires `If-Match`. Submission and successor creation acquire a short database lock on the evidence root, reauthorize and revalidate, allocate the next revision and rely on unique constraints as a final backstop. A submitted predecessor can have at most one direct successor in I3.

**Rationale**: Optimistic editing avoids long locks, while the brief serialization point prevents duplicate revision numbers and parallel v2 branches. `412 STALE_DRAFT` distinguishes an obsolete representation from `409` domain-state conflicts; missing preconditions return `428`.

**Alternatives considered**: Last writer wins violates RNF-011. Pessimistic locking for the entire edit reduces usability. `MAX(revision)+1` without a root lock races. A client revision in the body duplicates HTTP preconditions.

## 3. File-storage boundary

**Decision**: Keep `FileStoragePort` and the local MVP adapter approved by ADR-0002. Evolve the port to return both declared and detected media types, size and lowercase SHA-256; provide idempotent stage/promote/open/verify operations and leave durable reconciliation records in PostgreSQL. Storage keys are server-generated and opaque.

**Rationale**: The domain remains independent from paths and future S3 mechanics. PostgreSQL keeps metadata and recovery intent but never file bytes. A durable ledger is necessary because an in-memory reconciliation marker alone cannot survive a crash.

**Alternatives considered**: PostgreSQL binary storage is prohibited; direct filesystem access couples the domain; mandatory S3 violates the local development path.

## 4. Atomic promotion and recoverable consistency

**Decision**: Stage and final objects reside on the same local `FileStore`. The local adapter promotes with an atomic move to an immutable opaque destination and fails configuration if atomic move is unavailable. PostgreSQL and object storage use a recoverable saga:

1. stream to staging while counting at most 25 MiB + 1, hashing and detecting type;
2. validate against the active policy;
3. persist a durable `STAGED` file/operation record;
4. promote idempotently;
5. verify the final object and mark `AVAILABLE` in a short transaction;
6. record/retry reconciliation for any uncertain result.

Submission only references `AVAILABLE` sources, so its snapshot and successful audit event share one PostgreSQL transaction.

**Rationale**: Atomic rename prevents partial reads. Durable states cover crashes around either system without holding a database transaction during transfer.

**Alternatives considered**: Direct final writes expose partial content; OS temp may cross filesystems; copy/delete is not atomic; a long database transaction still cannot enlist filesystem/S3 atomically.

## 5. Reconciliation matrix

**Decision**: An idempotent, leased and auditable reconciler handles a configurable grace period: retry `STAGED` objects, verify an already-promoted final object, mark absent objects `MISSING`, quarantine hash mismatches, clean expired unreferenced staging, and quarantine/clean final orphans according to policy. It never deletes submitted history automatically.

**Rationale**: This handles both orphan directions and produces evidence for SC-003.

**Alternatives considered**: Age-only cleanup and request-triggered retries can lose content or leave post-crash objects unrecovered.

## 6. Type detection and validation policy

**Decision**: Use Apache Tika 3.x content detection in a bounded streaming pipeline; do not treat extension, browser `Content-Type` or OS probing as authority. Store declared and detected values separately and evaluate a versioned/configurable allowlist and compatibility mapping. The exact 26 214 400-byte domain limit remains mandatory.

**Rationale**: Tika provides reproducible cross-platform signature detection. A policy object keeps pilot MIME choices and CL-008 out of fixed enums/checks.

**Alternatives considered**: Filename/header data is attacker-controlled; `Files.probeContentType` varies by OS; full content extraction is unnecessary.

## 7. Zero-byte files

**Decision**: Allow `sizeBytes >= 0` in the descriptor and database; the active policy decides whether an empty file/type combination is acceptable. Change the current `StoredFile` invariant that rejects zero bytes.

**Rationale**: This resolves the specification edge case without hard-coding a CL-008 outcome.

**Alternatives considered**: A universal one-byte minimum contradicts the configurable rule in the feature spec.

## 8. SHA-256 and verified download

**Decision**: Compute SHA-256 incrementally over exact staged bytes and store 64 lowercase hexadecimal characters. Do not deduplicate or authorize by hash. Before sending a download response, authorize without revealing metadata, verify the entire object against size/hash and only then stream it with safe `Content-Disposition`.

**Rationale**: Hash is integrity, not identity. Pre-response verification prevents partial disclosure of corrupt bytes.

**Alternatives considered**: ETag/extension trust, post-send verification and SHA uniqueness do not satisfy integrity or authorization.

## 9. Authorization and privacy

**Decision**: Every use case rechecks authenticated capacity, process, every indicator and current assignment through I0/I2 ports. Collections are scope-filtered. Direct access to an out-of-scope identifier returns the same not-found response as an unknown identifier. Storage keys, full external URIs, bytes, secrets and technical failures never appear in errors or audit metadata.

**Rationale**: Backend authorization is constitutional and non-disclosure satisfies RNF-005.

**Alternatives considered**: UI-only permissions and bare repository lookups leak or bypass scope.

## 10. Audit transaction boundaries

**Decision**: A successful submission and its event use `AuditPort` in the same mandatory transaction; audit failure rolls back `SUBMITTED`. Rejections and failures after rollback use a controlled independent transaction with safe reason codes and correlation.

**Rationale**: A submitted version without its required event is forbidden, while failures still need durable evidence.

**Alternatives considered**: Logs alone lack domain traceability; triggers lack actor context; an external bus is unnecessary.

## 11. REST and Problem Details

**Decision**: Keep synchronous REST and multipart upload for the 25 MiB pilot. Add explicit draft editing, external-reference CRUD, draft file removal, submission and successor endpoints. Unify RFC 9457 on standard members plus `code`, `correlationId` and object-shaped `details`; expose ETag on drafts and require `If-Match` for all mutations.

**Rationale**: The existing OpenAPI omits required state-changing operations and snapshots. Explicit successor semantics prevent arbitrary version creation.

**Alternatives considered**: Resumable/pre-signed uploads are unnecessary at this scale; generic version creation is ambiguous; GraphQL is outside the approved contract.

## 12. Angular interaction

**Decision**: Implement list, editor, immutable detail and history routes with reactive forms. Files and external references are separate. The client retains ETags, never blindly retries `STALE_DRAFT`, shows upload progress and safe correlation errors, disables editing after submission, and creates corrections only through the successor action.

**Rationale**: The UI mirrors domain boundaries while the backend remains the permission authority and preserves keyboard/focus accessibility.

**Alternatives considered**: An unrestricted edit screen obscures immutability; automatic retry can overwrite concurrent work.

## 13. External-reference isolation

**Decision**: I3 validates only the structure and bounded length of an external URI and never dereferences it, performs reachability checks or sends outbound requests. The stored description makes the declaration understandable; availability is neither inferred nor persisted.

**Rationale**: The specification treats the URI as a declared source and excludes institutional/external integrations. Avoiding dereference also removes SSRF, timeout and availability coupling from I3.

**Alternatives considered**: Active reachability checks were rejected because they add an external integration, cannot establish documentary validity and require a separate network-security policy.

## 14. Acceptance and performance runners

**Decision**: Use Playwright 1.64.1 for `tests/acceptance/*.spec.ts` browser journeys and k6 1.1.0 for `tests/performance/*.js` load measurements, with pinned/documented versions and commands. Vitest remains for Angular unit/component tests.

**Rationale**: Explicit runners make acceptance and p95 tasks reproducible instead of leaving file extensions without execution semantics.

**Alternatives considered**: Manual-only E2E evidence and ad-hoc timing scripts were rejected because they cannot reliably reproduce regressions or thresholds.
