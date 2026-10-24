---

description: "Dependency-ordered implementation tasks for I3 evidence collection and submission"
---

# Tasks: I3 â€” RecopilaciÃ³n y remisiÃ³n de evidencias

**Input**: Design documents from `/specs/004-submit-evidence/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/openapi.yaml`, `quickstart.md`

**Tests**: Required. The specification, constitution and plan require automated verification of authorization, file integrity/recovery, concurrency, immutability, succession and contracts. Write each test task first and verify that it fails for the intended reason before implementing the corresponding behavior.

**Organization**: Tasks are grouped by user story. US1â€“US4 are P1 and are ordered by the domain journey; US5 is P2. Each story remains independently testable with fixtures/doubles even when the integrated journey builds on earlier stories.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel because it changes different files and has no dependency on an incomplete task.
- **[Story]**: User story mapped from `spec.md`.
- Every task names concrete repository paths.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirm upstream boundaries, add approved dependencies/configuration and prepare deterministic synthetic inputs.

- [X] T001 Verify that I0 exposes authenticated identity plus function/process/indicator scope and I2 exposes active process, fixed model version, indicators and current assignments; record available interfaces and any blocking gaps without recreating I0/I2 rules in `specs/004-submit-evidence/implementation-prerequisites.md`
- [X] T002 Pin Apache Tika 3.x for backend detection and Playwright for `tests/acceptance/*.spec.ts`, document the required k6 version/installation and add executable test scripts in `backend/pom.xml`, `frontend/package.json`, `frontend/playwright.config.ts`, `tests/performance/README.md` and `specs/004-submit-evidence/research.md`
- [X] T003 [P] Bind typed file settings for `root`, exact `maxBytes=26214400`, active validation-policy identifier and configurable reconciliation grace/lease values in `backend/src/main/resources/application.yml` and `backend/src/main/java/ec/edu/unl/sec/filestorage/infrastructure/FileStorageProperties.java`
- [X] T004 [P] Create deterministic generators/manifests for permitted small, exact 26 214 400-byte, 26 214 401-byte and declared/detected-mismatch files using no real institutional data in `scripts/generate-evidence-fixtures.ps1`, `scripts/generate-evidence-fixtures.sh`, `tests/fixtures/pilot/evidence/manifest.json` and `tests/fixtures/pilot/evidence/README.md`
- [X] T005 Create and approve the versioning/security/public-contract decision in `docs/adr/0003-evidence-versioning-contract.md`, then reconcile the approved I3 paths/schemas from `specs/004-submit-evidence/contracts/openapi.yaml` into `docs/technical/contracts/openapi.yaml` and validate both with `scripts/validate-contracts.mjs`, Redocly and `postman.cmd spec lint --issueType syntax --no-report-events`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Establish shared contract, persistence, authorization and storage boundaries used by every story.

**âš ï¸ CRITICAL**: T001 is a hard gate for T014 and every JPA, controller or integrated API/UI task. If I0/I2 contracts or migrations are missing, stop those tasks; only isolated domain/storage tests and implementations that use explicit doubles may continue. Do not create substitute I0/I2 tables or policies inside I3.

### Tests first

- [X] T006 [P] Extend contract tests to require every I3 path, strong draft `ETag`, mandatory `If-Match`, `412`, `428`, distinct declared/detected media types, full version snapshots and RFC 9457 `type`, `title`, `status`, `detail`, `instance`, `code`, `correlationId`, object-shaped `details` in `backend/src/test/java/ec/edu/unl/sec/contract/OpenApiContractTest.java`
- [X] T007 [P] Extend architecture tests so `evidence.domain` and `filestorage.domain` cannot depend on API/infrastructure, I3 cannot depend on I4â€“I8, and cross-module I0/I2/audit access occurs through application ports in `backend/src/test/java/ec/edu/unl/sec/architecture/ModuleArchitectureTest.java`
- [X] T008 [P] Add clean-database tests for V006 requiring the complete evidence schema, no binary columns/large objects, `RESTRICT` historical foreign keys and recovery indexes in `backend/src/test/java/ec/edu/unl/sec/migration/EvidenceMigrationTest.java`; leave submitted-row mutation guards to T046 after the successor migration reserved as V011 in T051

### Shared implementation

- [X] T009 Unify backend/frontend/OpenAPI Problem Details on RFC 9457 standard members plus `code`, safe 8â€“64-character `correlationId` and `details: Record<string,string>` in `backend/src/main/java/ec/edu/unl/sec/shared/api/ApiProblemHandler.java`, `backend/src/main/java/ec/edu/unl/sec/shared/api/DomainProblemException.java`, `frontend/src/app/core/http/problem.ts` and `frontend/src/app/core/http/problem.spec.ts`
- [X] T010 [P] Define I3-owned read/authorization boundaries for current actor, process, fixed model version, indicator membership and assignment vigency in `backend/src/main/java/ec/edu/unl/sec/evidence/application/EvidenceAuthorizationPort.java` and `backend/src/main/java/ec/edu/unl/sec/evidence/application/EvidenceContextPort.java`
- [X] T011 [P] Implement reusable strong ETag parsing/rendering and map missing/stale preconditions to `428 PRECONDITION_REQUIRED` and `412 STALE_DRAFT` in `backend/src/main/java/ec/edu/unl/sec/shared/api/EntityTagSupport.java` and `backend/src/test/java/ec/edu/unl/sec/shared/api/EntityTagSupportTest.java`
- [X] T012 [P] Add a controlled failure-audit boundary that can persist safe rejection events after rollback while successful submission continues to use mandatory transaction participation in `backend/src/main/java/ec/edu/unl/sec/audit/application/FailureAuditPort.java` and `backend/src/main/java/ec/edu/unl/sec/audit/infrastructure/TransactionalFailureAuditAdapter.java`
- [X] T013 Redesign `FileStoragePort` around idempotent prepare/promote/open/verify and opaque keys; make `StoredFile` keep declared and detected type separately, lowercase SHA-256 matching `^[0-9a-f]{64}$`, and `sizeBytes` in `0..26214400` so policyâ€”not a constructorâ€”decides empty-file acceptance in `backend/src/main/java/ec/edu/unl/sec/filestorage/application/FileStoragePort.java` and `backend/src/main/java/ec/edu/unl/sec/filestorage/domain/StoredFile.java`
- [X] T014 After T001 passes, create immutable-once-submitted-ready schema `backend/src/main/resources/db/migration/V006__evidence.sql` with `Evidence`, `EvidenceVersion`, `VersionIndicator`, `FileAsset`, `VersionFile`, `ExternalReference`, `FileValidationPolicy` and `FileRecoveryOperation`: require UUID identities; positive unique `(evidence_id,revision_number)`; unique non-null predecessor from the same evidence; one active draft per evidence; title `1..300`, description `1..8000`, provenance `1..4000`; `covered_from <= covered_to`; file size `0..26214400`; lowercase 64-hex SHA-256; non-unique hash; opaque unique storage/upload keys; absolute URI up to 2048 and description `1..1000`; leased recovery indexes; and no cascade delete of history; reserve trigger enforcement for the successor V011 without later editing V006

**Checkpoint**: Contract, schema and boundaries exist; foundational tests pass and story work can begin.

---

## Phase 3: User Story 1 â€” Preparar un borrador de evidencia (Priority: P1) ðŸŽ¯ MVP

**Goal**: Create, list, read and edit v1 drafts with complete metadata and one or more authorized indicators.

**Independent Test**: With synthetic process/assignment doubles, create a draft for two in-scope indicators, edit it with the current ETag, and reject an out-of-process, out-of-scope or expired assignment without disclosing foreign data.

### Tests for User Story 1 â€” write and fail first

- [X] T015 [P] [US1] Add domain tests for stable `Evidence` identity and `EvidenceVersion` draft fieldsâ€”title `1..300`, description `1..8000`, provenance `1..4000`, inclusive dates with `coveredFrom <= coveredTo`, positive revision, null predecessor only for v1, at least one unique indicator and non-negative lock revisionâ€”in `backend/src/test/java/ec/edu/unl/sec/evidence/domain/EvidenceDraftTest.java`
- [X] T016 [P] [US1] Add application tests for creation/update authorization, current assignment, process-model indicator membership, scope-filtered listing, hidden direct IDs and stale/missing ETags in `backend/src/test/java/ec/edu/unl/sec/evidence/application/EvidenceDraftServiceTest.java`
- [X] T017 [P] [US1] Add MockMvc contract/security tests for `GET/POST /evidences`, `GET /evidences/{evidenceId}`, and `GET/PATCH /evidence-versions/{versionId}` including `Location`, `ETag`, CSRF, `404` non-disclosure, `412` and `428` in `backend/src/test/java/ec/edu/unl/sec/evidence/api/EvidenceDraftControllerTest.java`
- [X] T018 [P] [US1] Add Angular tests for list/new/edit routes, reactive validation, indicator selection, ETag propagation, keyboard focus and safe stale-draft recovery in `frontend/src/app/features/evidence/pages/evidence-editor.page.spec.ts` and `frontend/src/app/features/evidence/pages/evidence-list.page.spec.ts`

### Implementation for User Story 1

- [X] T019 [P] [US1] Implement `Evidence` as stable lineage (`id`, required process/owner/createdAt/correlation, latest revision starting at 0) in `backend/src/main/java/ec/edu/unl/sec/evidence/domain/Evidence.java`
- [X] T020 [P] [US1] Implement mutable-only-while-`DRAFT` `EvidenceVersion` with the exact field/range/date/revision/predecessor/lock constraints tested in T015 in `backend/src/main/java/ec/edu/unl/sec/evidence/domain/EvidenceVersion.java` and `backend/src/main/java/ec/edu/unl/sec/evidence/domain/EvidenceVersionStatus.java`
- [X] T021 [P] [US1] Implement `VersionIndicator` with composite identity, required indicator ID and immutable code/title display snapshots in `backend/src/main/java/ec/edu/unl/sec/evidence/domain/VersionIndicator.java`
- [X] T022 [US1] Map roots, versions and indicator snapshots with pessimistic root lookup and scope-filtered projections in `backend/src/main/java/ec/edu/unl/sec/evidence/infrastructure/JpaEvidenceRepository.java`, `backend/src/main/java/ec/edu/unl/sec/evidence/infrastructure/EvidenceJpaEntity.java` and `backend/src/main/java/ec/edu/unl/sec/evidence/infrastructure/EvidenceVersionJpaEntity.java`
- [X] T023 [US1] Implement create/list/get/update cases that reauthorize every operation through I0/I2 ports, reject indicators outside the fixed process model or current assignment, and apply ETag concurrency in `backend/src/main/java/ec/edu/unl/sec/evidence/application/EvidenceDraftService.java`
- [X] T024 [US1] Implement request/response DTOs and controllers for draft operations without exposing repository/storage details in `backend/src/main/java/ec/edu/unl/sec/evidence/api/EvidenceDraftController.java`, `backend/src/main/java/ec/edu/unl/sec/evidence/api/EvidenceDraftRequest.java` and `backend/src/main/java/ec/edu/unl/sec/evidence/api/EvidenceView.java`
- [X] T025 [US1] Extend the Angular HTTP client for typed headers/ETag and implement evidence models/service in `frontend/src/app/core/http/api-client.ts`, `frontend/src/app/features/evidence/data-access/evidence.models.ts` and `frontend/src/app/features/evidence/data-access/evidence-api.service.ts`
- [X] T026 [US1] Replace the placeholder page with accessible list/new/edit pages and routes in `frontend/src/app/features/evidence/pages/evidence-list.page.ts`, `frontend/src/app/features/evidence/pages/evidence-editor.page.ts`, `frontend/src/app/features/evidence/ui/evidence-metadata-form.component.ts` and `frontend/src/app/features/evidence/evidence.routes.ts`

**Checkpoint**: US1 passes independently with process/auth doubles and delivers a usable authorized draft without files or submission.

---

## Phase 4: User Story 2 â€” Adjuntar archivos o referencias seguras (Priority: P1)

**Goal**: Attach validated/promoted files or described external references to an editable draft and recover every interrupted upload without false availability.

**Independent Test**: Attach a permitted synthetic file at or below 25 MiB and a described URI; verify declared/detected type, size and SHA-256, then reject oversize/type mismatch and simulate every staging/promotion/confirmation interruption while leaving the draft retryable.

### Tests for User Story 2 â€” write and fail first

- [X] T027 [P] [US2] Add bounded-stream tests for 0 bytes governed by policy, 26 214 400 accepted bytes, 26 214 401 rejected bytes, lowercase SHA-256, allowed/forbidden detected types and declared/detected compatibility in `backend/src/test/java/ec/edu/unl/sec/filestorage/domain/FileValidationPolicyTest.java`
- [X] T028 [P] [US2] Add local-adapter tests for same-FileStore staging, unavailable `ATOMIC_MOVE`, opaque/contained paths, symlink defense, idempotent promotion, immutable final object, open/verify and exact bytes/hash in `backend/src/test/java/ec/edu/unl/sec/filestorage/infrastructure/LocalFileStorageAdapterTest.java`
- [X] T029 [P] [US2] Add reconciliation tests for all six matrix statesâ€”staged object, already-promoted object, missing both, missing available, corrupt available and orphan objectâ€”with leases/idempotent retry and no deletion of submitted assets in `backend/src/test/java/ec/edu/unl/sec/filestorage/application/FileReconciliationServiceTest.java`
- [X] T030 [P] [US2] Add application/API tests for multipart upload, draft file removal and external-reference add/update/remove with ETag/CSRF, safe `413/422/503`, no `storageKey`, absolute URI `<=2048` and description `1..1000` in `backend/src/test/java/ec/edu/unl/sec/evidence/api/EvidenceSourceControllerTest.java`
- [X] T031 [P] [US2] Add Angular tests for separate file/reference collections, upload progress, type/size errors, source removal, structural-only URI wording, no outbound reachability claim and ETag refresh in `frontend/src/app/features/evidence/ui/evidence-sources.component.spec.ts`

### Implementation for User Story 2

- [X] T032 [P] [US2] Implement versioned/configurable validation policy with maximum `26214400`, configurable `allowEmpty`, allowed detected types and declared/detected compatibility in `backend/src/main/java/ec/edu/unl/sec/filestorage/domain/FileValidationPolicy.java` and `backend/src/main/java/ec/edu/unl/sec/filestorage/infrastructure/JpaFileValidationPolicyRepository.java`
- [X] T033 [P] [US2] Implement bounded single-pass byte count, SHA-256 and Apache Tika content detection without trusting extension/header/OS probing in `backend/src/main/java/ec/edu/unl/sec/filestorage/infrastructure/ContentInspectionService.java`
- [X] T034 [US2] Implement local staging and promotion beneath configured `staging/` and `objects/` roots using opaque server keys, path containment and required same-FileStore `ATOMIC_MOVE` in `backend/src/main/java/ec/edu/unl/sec/filestorage/infrastructure/LocalFileStorageAdapter.java`
- [X] T035 [P] [US2] Implement `FileAsset` with unique opaque storage/upload identity, distinct types, `sizeBytes 0..26214400`, lowercase 64-hex non-unique hash, policy ID and states `STAGED|AVAILABLE|QUARANTINED|MISSING` in `backend/src/main/java/ec/edu/unl/sec/filestorage/domain/FileAsset.java`
- [X] T036 [P] [US2] Implement `VersionFile` snapshot fields and stable position, plus `ExternalReference` absolute structurally validated URI `<=2048` and required description `1..1000`; do not persist availability or perform outbound dereference in `backend/src/main/java/ec/edu/unl/sec/evidence/domain/VersionFile.java` and `backend/src/main/java/ec/edu/unl/sec/evidence/domain/ExternalReference.java`
- [X] T037 [P] [US2] Implement durable `FileRecoveryOperation` with `PROMOTE|VERIFY|QUARANTINE|CLEANUP`, statuses, non-negative attempts, `notBefore`, nullable lease, safe reason and correlation in `backend/src/main/java/ec/edu/unl/sec/filestorage/domain/FileRecoveryOperation.java`
- [X] T038 [US2] Implement JPA mappings/repositories for file assets, version associations, references, policies and recovery leases in `backend/src/main/java/ec/edu/unl/sec/filestorage/infrastructure/JpaFileAssetRepository.java`, `backend/src/main/java/ec/edu/unl/sec/filestorage/infrastructure/JpaFileRecoveryRepository.java` and `backend/src/main/java/ec/edu/unl/sec/evidence/infrastructure/JpaEvidenceSourceRepository.java`
- [X] T039 [US2] Orchestrate prepare â†’ validate â†’ persist `STAGED` â†’ idempotent promote â†’ verify â†’ mark `AVAILABLE`/attach, recording safe failure reconciliation without a distributed transaction in `backend/src/main/java/ec/edu/unl/sec/evidence/application/EvidenceFileService.java`
- [X] T040 [US2] Implement leased idempotent recovery for every matrix state and audit outcomes without fixed retention decisions in `backend/src/main/java/ec/edu/unl/sec/filestorage/application/FileReconciliationService.java` and `backend/src/main/java/ec/edu/unl/sec/filestorage/infrastructure/FileReconciliationScheduler.java`
- [X] T041 [US2] Implement reference add/update/remove and draft file-association removal with current authorization and ETag, never mutating submitted versions in `backend/src/main/java/ec/edu/unl/sec/evidence/application/EvidenceSourceService.java`
- [X] T042 [US2] Implement multipart file and external-reference endpoints/responses per OpenAPI in `backend/src/main/java/ec/edu/unl/sec/evidence/api/EvidenceSourceController.java`, `backend/src/main/java/ec/edu/unl/sec/evidence/api/ExternalReferenceRequest.java` and `backend/src/main/java/ec/edu/unl/sec/evidence/api/FileMetadataView.java`
- [X] T043 [US2] Implement accessible upload progress, safe errors and separate file/reference editing with current ETag in `frontend/src/app/features/evidence/ui/evidence-sources.component.ts` and `frontend/src/app/features/evidence/data-access/evidence-api.service.ts`

**Checkpoint**: US2 passes independently against a fixture draft and proves validation, promotion, retry and reference semantics without submission.

---

## Phase 5: User Story 3 â€” Remitir una versiÃ³n inmutable y descargarla (Priority: P1)

**Goal**: Submit a complete atomic v1 snapshot, prevent all later mutations and deliver only authorized, integrity-verified bytes.

**Independent Test**: Submit a complete v1 with one synthetic file; prove all version/child mutations fail, authorized download matches stored SHA-256, out-of-scope IDs reveal nothing and audit failure prevents submission.

### Tests for User Story 3 â€” write and fail first

- [X] T044 [P] [US3] Add domain tests requiring complete text/dates, at least one authorized indicator, at least one file/reference, every file `AVAILABLE` and verifiable, single `DRAFTâ†’SUBMITTED` transition, author/time and immutable children in `backend/src/test/java/ec/edu/unl/sec/evidence/domain/EvidenceSubmissionTest.java`
- [ ] T045 [P] [US3] Add transactional/concurrency tests proving two submissions create at most one submitted revision, stale ETag fails, successful audit shares the submission transaction and audit failure rolls everything back in `backend/src/test/java/ec/edu/unl/sec/evidence/application/SubmitEvidenceServiceTest.java`; reopened 2026-10-24 because CAS unit tests do not prove transaction rollback or simultaneous submissions
- [X] T046 [P] [US3] Add PostgreSQL integration tests that direct `UPDATE/DELETE` against a submitted version, indicators, files or references fails while draft mutation remains allowed in `backend/src/test/java/ec/edu/unl/sec/evidence/infrastructure/SubmittedEvidenceImmutabilityTest.java`; executed on disposable PostgreSQL 16.14 and SQLite, see `tests/acceptance/results/2026-10-24-pull-review.md`
- [X] T047 [P] [US3] Add download tests proving authorization precedes metadata disclosure, full pre-response SHA/size verification, safe filename, no `storageKey`, exact bytes for authorized users and indistinguishable `404` for foreign/unknown IDs in `backend/src/test/java/ec/edu/unl/sec/evidence/application/EvidenceDownloadServiceTest.java`
- [X] T048 [P] [US3] Add MockMvc/OpenAPI tests for `POST /evidence-versions/{versionId}/submit` and `GET .../files/{fileId}/content`, including CSRF/ETag, `409/412/422/428`, content headers and no bytes on integrity failure in `backend/src/test/java/ec/edu/unl/sec/evidence/api/EvidenceSubmissionControllerTest.java`
- [ ] T049 [P] [US3] Add Angular tests for completeness feedback, confirmation result, editor-to-read-only transition, safe Blob download and focus/status announcements in `frontend/src/app/features/evidence/pages/evidence-version.page.spec.ts`

### Implementation for User Story 3

- [X] T050 [US3] Implement domain submission validation and the irreversible `DRAFTâ†’SUBMITTED` transition while preserving all metadata/indicator/file/reference snapshots in `backend/src/main/java/ec/edu/unl/sec/evidence/domain/EvidenceVersion.java`
- [X] T051 [US3] Add a new reproducible Flyway migration with defensive PostgreSQL trigger/functions that allow only the submission transition and reject later update/delete of submitted rows/children in `backend/src/main/resources/db/migration/V015__submitted_evidence_guards.sql`; V011 is already historical and remains unchanged, per ADR-0020
- [ ] T052 [US3] Implement root-locked, reauthorized, ETag-checked submission with complete revalidation and mandatory same-transaction `AuditPort.record`, then safe independent failure audit after rollback in `backend/src/main/java/ec/edu/unl/sec/evidence/application/SubmitEvidenceService.java`; reopened 2026-10-24: reauthorization, modality/integrity checks and CAS exist, but root locking and independent failure audit remain pending
- [X] T053 [US3] Implement verified-download spooling/handle lifecycle so size/SHA-256 is confirmed before HTTP response begins and temporary verified copies are cleaned in `backend/src/main/java/ec/edu/unl/sec/evidence/application/EvidenceDownloadService.java`
- [X] T054 [US3] Implement submission and download controllers with safe `Content-Disposition`, detected media type, `Content-Length`, digest, hidden foreign IDs and RFC 9457 mappings in `backend/src/main/java/ec/edu/unl/sec/evidence/api/EvidenceSubmissionController.java` and `backend/src/main/java/ec/edu/unl/sec/evidence/api/EvidenceDownloadController.java`
- [X] T055 [US3] Implement immutable version detail and submit/download actions in `frontend/src/app/features/evidence/pages/evidence-version.page.ts`, `frontend/src/app/features/evidence/ui/evidence-version-detail.component.ts` and `frontend/src/app/features/evidence/data-access/evidence-api.service.ts`
- [ ] T056 [US3] Add the synthetic v1 submit â†’ rejected mutation â†’ authorized/denied download acceptance scenario and evidence capture in `tests/acceptance/evidence-v1.spec.ts` and `tests/acceptance/results/evidence-v1.md`

**Checkpoint**: US3 demonstrates the central measurable result for v1 without any I4 review or academic state.

---

## Phase 6: User Story 4 â€” Corregir mediante una versiÃ³n sucesora (Priority: P1)

**Goal**: Create exactly one editable v2 from immutable v1 and preserve the full ordered lineage after v2 submission.

**Independent Test**: Create v2 from submitted v1, change and submit it, then compare both snapshots and reject a concurrent/duplicate successor.

### Tests for User Story 4 â€” write and fail first

- [ ] T057 [P] [US4] Add domain tests for v1 null predecessor, vN same-evidence predecessor, sequential positive revision, exact source/content copy into a mutable successor and unchanged predecessor in `backend/src/test/java/ec/edu/unl/sec/evidence/domain/EvidenceSuccessorTest.java`
- [ ] T058 [P] [US4] Add application/concurrency tests proving a short root lock plus unique predecessor permits at most one direct successor and returns `SUCCESSOR_ALREADY_EXISTS` without branching in `backend/src/test/java/ec/edu/unl/sec/evidence/application/CreateEvidenceSuccessorServiceTest.java`
- [ ] T059 [P] [US4] Add API tests for `POST /evidence-versions/{versionId}/successor`, version history ordering/pagination, `Location`, new draft ETag, hidden IDs and full immutable snapshots in `backend/src/test/java/ec/edu/unl/sec/evidence/api/EvidenceHistoryControllerTest.java`
- [ ] T060 [P] [US4] Add Angular tests for successor action, copied v2 editor, immutable v1 detail and ordered v1/v2 timeline in `frontend/src/app/features/evidence/pages/evidence-history.page.spec.ts`

### Implementation for User Story 4

- [ ] T061 [US4] Implement successor-copy domain behavior that allocates revision `latest+1`, sets the direct submitted predecessor and copies metadata/indicators/file snapshots/references without sharing mutable children in `backend/src/main/java/ec/edu/unl/sec/evidence/domain/Evidence.java` and `backend/src/main/java/ec/edu/unl/sec/evidence/domain/EvidenceVersion.java`
- [ ] T062 [US4] Implement root-locked, reauthorized successor creation and ordered scope-filtered history queries in `backend/src/main/java/ec/edu/unl/sec/evidence/application/CreateEvidenceSuccessorService.java` and `backend/src/main/java/ec/edu/unl/sec/evidence/application/EvidenceHistoryService.java`
- [ ] T063 [US4] Implement successor and history REST operations with `409 SUCCESSOR_ALREADY_EXISTS`, `Location`, ETag and complete version summaries in `backend/src/main/java/ec/edu/unl/sec/evidence/api/EvidenceHistoryController.java`
- [ ] T064 [US4] Implement history timeline, immutable version navigation and â€œCreate successorâ€ flow in `frontend/src/app/features/evidence/pages/evidence-history.page.ts`, `frontend/src/app/features/evidence/ui/evidence-history-timeline.component.ts` and `frontend/src/app/features/evidence/evidence.routes.ts`
- [ ] T065 [US4] Extend the acceptance journey to submit v2 and assert v1/v2 field, author, timestamp, predecessor, file/reference and SHA history in `tests/acceptance/evidence-v1-v2.spec.ts` and `tests/acceptance/results/evidence-v1-v2.md`
- [ ] T066 [US4] Add simultaneous edit, submission and successor race coverage against PostgreSQL constraints/locks in `backend/src/test/java/ec/edu/unl/sec/evidence/infrastructure/EvidenceConcurrencyIntegrationTest.java`

**Checkpoint**: US4 proves the full required v1 â†’ immutable â†’ v2 lineage with no overwrite.

---

## Phase 7: User Story 5 â€” Auditar remisiones y fallos (Priority: P2)

**Goal**: Reconstruct submissions and relevant failures through immutable, scope-filtered and privacy-safe audit events.

**Independent Test**: Produce accepted/rejected submission, validation, promotion, concurrency and denied-download events; query them by evidence/process/correlation and verify safe ordered content and ordinary-user immutability.

### Tests for User Story 5 â€” write and fail first

- [ ] T067 [P] [US5] Add audit-policy tests requiring actor, UTC time, action, object, outcome, safe reason and correlation while forbidding bytes, storage/staging keys, full external URIs, secrets, stack traces and out-of-scope metadata in `backend/src/test/java/ec/edu/unl/sec/evidence/application/EvidenceAuditPolicyTest.java`
- [ ] T068 [P] [US5] Add repository/API tests for scope-filtered queries by process/evidence/version/action/outcome/correlation/date with stable ordering/pagination and `403` only for visible collection capability failures in `backend/src/test/java/ec/edu/unl/sec/audit/api/AuditEventControllerTest.java`
- [ ] T069 [P] [US5] Add failure-path integration tests for rejected submission, invalid file, promotion failure, stale ETag and denied/integrity-failed download, proving each commits one safe audit event after business rollback in `backend/src/test/java/ec/edu/unl/sec/evidence/application/EvidenceFailureAuditIntegrationTest.java`
- [ ] T070 [P] [US5] Add Angular tests for authorized audit filtering, chronological reconstruction, correlation display and absence of sensitive metadata in `frontend/src/app/features/evidence/pages/evidence-audit.page.spec.ts`

### Implementation for User Story 5

- [ ] T071 [P] [US5] Define centralized I3 audit actions/outcomes/reason codes and metadata allowlist/redaction in `backend/src/main/java/ec/edu/unl/sec/evidence/application/EvidenceAuditPolicy.java`
- [ ] T072 [US5] Apply the audit policy consistently to draft, file, reference, submission, successor, reconciliation and download failure paths in `backend/src/main/java/ec/edu/unl/sec/evidence/application/EvidenceAuditService.java`
- [ ] T073 [US5] Implement scope-aware indexed audit search projections for process/evidence/version/action/outcome/correlation/date filters in `backend/src/main/java/ec/edu/unl/sec/audit/infrastructure/JpaAuditAdapter.java`
- [ ] T074 [US5] Implement authorized audit REST query and DTO sanitization in `backend/src/main/java/ec/edu/unl/sec/audit/api/AuditEventController.java` and `backend/src/main/java/ec/edu/unl/sec/audit/api/AuditEventView.java`
- [ ] T075 [US5] Implement the evidence audit page with accessible filters, empty/error states and correlation-safe details in `frontend/src/app/features/evidence/pages/evidence-audit.page.ts` and `frontend/src/app/features/evidence/evidence.routes.ts`
- [ ] T076 [US5] Add acceptance evidence for the complete remittance/failure sequence and attempted ordinary-user mutation of audit history in `tests/acceptance/evidence-audit.spec.ts` and `tests/acceptance/results/evidence-audit.md`

**Checkpoint**: US5 reconstructs required events without exposing content or introducing review/academic decisions.

---

## Phase 8: Polish & Cross-Cutting Verification

**Purpose**: Reconcile public documentation, prove operational qualities and close all constitutional gates.

- [ ] T077 [P] Update ADR-0002 with final port operations, atomic-move startup requirement, durable reconciliation ledger, S3-equivalent semantics and explicitly non-distributed transaction boundary in `docs/adr/0002-file-storage-port.md`
- [ ] T078 [P] Document the I3 user journey, immutable versions, safe errors, external-reference meaning and absence of document/academic decisions in `docs/user/index.qmd`
- [ ] T079 [P] Document module/package flow, configuration, reconciliation matrix, backup/restore pairing and Postman CLI contract checks in `docs/technical/index.qmd` and `docs/technical/quickstart.md`
- [ ] T080 Reconcile the implemented controller schemas/errors with both contracts and rerun Redocly, `scripts/validate-contracts.mjs` and Postman CLI in `specs/004-submit-evidence/contracts/openapi.yaml` and `docs/technical/contracts/openapi.yaml`
- [ ] T081 [P] Add reproducible k6 upload/load checks and documented `k6 run tests/performance/evidence-upload.js` command for p95 â‰¤15 s up to 25 MiB and no regression of p95 â‰¤2 s interactive operations at pilot concurrency in `tests/performance/evidence-upload.js` and `tests/performance/README.md`
- [ ] T082 [P] Add file-root plus PostgreSQL backup/restore and post-restore reconciliation validation in `scripts/backup-evidence.ps1`, `scripts/restore-evidence.ps1` and `tests/acceptance/results/evidence-restore.md`
- [ ] T083 [P] Extend synthetic-data/secret detection for generated evidence fixtures, audit outputs and file roots in `scripts/verify-synthetic-data.ps1` and `scripts/verify-synthetic-data.sh`
- [ ] T084 Map US1â€“US5, FR-016â€“FR-020, RNF-004/005/010/011/019 and SC-001â€“SC-008 to contract, task, automated test and acceptance evidence in `docs/testing/traceability.md`
- [ ] T085 Run clean migration, backend `verify`, frontend test/lint/build, contract checks, architecture tests and I0â€“I2 regression; record exact commands/results and unresolved failures in `tests/acceptance/results/evidence-regression.md`
- [ ] T086 Execute every scenario in `specs/004-submit-evidence/quickstart.md`, including interruption matrix and v1/v2 history, and record reproducible evidence in `tests/acceptance/results/evidence-quickstart.md`
- [ ] T087 Validate SC-007 with 20 representative synthetic responsible-user participants, recording anonymous participant code, first-attempt completion, start/end time, help requested and result; require at least 19 of 20 to finish create â†’ attach â†’ submit in under five minutes using only UI/help in `tests/acceptance/results/evidence-usability.md`
- [ ] T088 Perform the required final human review of requirements, authorization matrix, RFC 9457 errors, CL-001â€“CL-010 configurability, accessibility, data protection and constitutional compliance, including the SC-007 evidence, in `tests/acceptance/results/evidence-review-gate.md`

---

## Dependencies & Execution Order

### Phase dependencies

- **Phase 1 â€” Setup**: starts immediately. T001 is a hard gate for migration/persistence/API integration; T003 and T004 can run in parallel after repository inspection.
- **Phase 2 â€” Foundational**: depends on Setup; blocks all user-story implementation. Tests T006â€“T008 run before T009â€“T014.
- **Phase 3 â€” US1**: depends on Foundational and establishes the draft used by later integrated journeys.
- **Phase 4 â€” US2**: depends on Foundational; independently testable with a fixture draft, but integrated delivery follows US1.
- **Phase 5 â€” US3**: depends on US1 and at least one US2 source for the required file-based acceptance path.
- **Phase 6 â€” US4**: depends on US3 because a successor requires a submitted predecessor.
- **Phase 7 â€” US5**: depends on Foundational; its isolated query tests use fixture events, while full acceptance consumes US1â€“US4 events.
- **Phase 8 â€” Polish**: depends on every story selected for release.

### User-story dependency graph

```text
Setup â†’ Foundation â†’ US1 Draft â”€â”€â†’ US2 Sources â”€â”€â†’ US3 Submit/Download â”€â”€â†’ US4 Successor/History
                         â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â†’ US5 Audit
Foundation â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â†’ US5 Audit (fixture test)
```

### Within each story

1. Write the story tests and confirm intended failures.
2. Implement constrained domain types/entities.
3. Implement persistence and application orchestration.
4. Implement REST endpoints.
5. Implement Angular interaction.
6. Run the independent checkpoint before advancing.

## Parallel Opportunities

- T003 and T004 can run in parallel; T006â€“T008 can run in parallel.
- US1 tests T015â€“T018 and domain types T019â€“T021 are parallel groups.
- US2 tests T027â€“T031 are parallel; T032, T033, T035â€“T037 use separate files and can proceed in parallel after T013/T014.
- US3 tests T044â€“T049 are parallel before implementation.
- US4 tests T057â€“T060 are parallel; US5 tests T067â€“T070 are parallel.
- US5 query implementation can begin against synthetic audit fixtures after Foundation while US1â€“US4 proceed, but T076 waits for their real events.
- Documentation/performance/backup/data-scan tasks T077â€“T083 can run in parallel after their corresponding implementation surfaces stabilize.

## Parallel Examples

### User Story 1

```text
T015: Domain constraints and state tests
T016: Authorization/concurrency service tests
T017: REST contract/security tests
T018: Angular editor/list tests
```

### User Story 2

```text
T027: Validation-policy boundaries
T028: Local atomic storage behavior
T029: Reconciliation matrix
T030: Source API contract/security
T031: Angular source interaction
```

### User Story 3

```text
T044: Submission invariants
T045: Transaction/concurrency behavior
T046: Database immutability guards
T047: Authorized verified download
T048: REST behavior
T049: Angular immutable view
```

## Implementation Strategy

### MVP first

The smallest demonstrable delivery is **Setup + Foundation + US1**: an authorized evidence draft with metadata, indicators and optimistic concurrency. This is useful and independently testable but does not yet satisfy the feature-level measurable outcome.

The **minimum feature-complete I3 slice** is Setup + Foundation + US1 + US2 + US3 + US4. It proves v1 with a synthetic file, immutable submission and v2 succession. US5 completes the explicitly requested audit visibility and is required before accepting the whole specification.

### Incremental delivery

1. Setup/Foundation â†’ contracts, schema and boundaries stable.
2. US1 â†’ draft creation/editing demo.
3. US2 â†’ validated sources and recovery demo.
4. US3 â†’ immutable v1 and authorized download demo.
5. US4 â†’ v2 and full history demo.
6. US5 â†’ audit reconstruction demo.
7. Polish â†’ performance, restore, documentation, traceability and human gate.

### Before implementation

Run `$speckit-analyze` and resolve every critical inconsistency across `spec.md`, `plan.md` and this task list. Then run `$speckit-implement`; it must process tasks in phase order and mark a task `[X]` only after its files and verification are complete.

## Notes

- `[P]` never authorizes concurrent edits to the same file.
- I3 consumes I0/I2 public contracts; it must not recreate their tables or institutional rules.
- No task may add document-review states, academic ratings, progress calculations or process closing.
- File hashes are integrity evidence, not identities, deduplication keys or authorization grants.
- Keep CL-001â€“CL-010 configurable and synthetic; do not turn pilot values into fixed policy.
- Postman CLI is available as instructed by `AGENTS.md`; use `postman.cmd` on Windows when execution policy blocks the `.ps1` wrapper.
