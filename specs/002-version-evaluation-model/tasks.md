# Tasks: I1 â€” Modelo de evaluaciÃ³n versionado

**Input**: `/specs/002-version-evaluation-model/` design artifacts. Tests are mandatory and precede implementation.

## Phase 1: Setup

- [X] T001 Reconcile and lint I1 paths/schemas in `specs/002-version-evaluation-model/contracts/openapi.yaml` and `docs/technical/contracts/openapi.yaml`
- [X] T002 Create the versioning and declarative-formula decision record in `docs/adr/0005-model-version-snapshots.md`
- [X] T003 [P] Add configurable formula schema/normalization settings in `backend/src/main/resources/application.yml` and `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/application/EvaluationModelProperties.java`

## Phase 2: Foundational

- [X] T004 Add clean-migration, published-content immutability and same-version-parent tests in `backend/src/test/java/ec/edu/unl/sec/migration/EvaluationModelMigrationTest.java`
- [X] T005 Create model, version, criterion, standard, indicator, scale, option and formula tables with constraints/indexes in `backend/src/main/resources/db/migration/V004__evaluation_model.sql`
- [X] T006 [P] Define I1 repository and downstream read contracts in `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/application/EvaluationModelRepository.java` and `ModelCatalogPort.java`
- [X] T007 [P] Define status, node, scale, formula and version value objects in `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/domain/ModelVersionStatus.java`, `ModelNode.java`, `RatingScale.java`, `ProgressFormula.java` and `ModelVersion.java`
- [X] T008 Implement JDBC persistence without cross-version joins in `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/infrastructure/JdbcEvaluationModelRepository.java`

## Phase 3: User Story 1 â€” Registrar borrador (P1)

**Independent test**: create and replace one incomplete synthetic draft, preserving ordered hierarchy and rejecting a stale ETag.

- [X] T009 [P] [US1] Add aggregate ordering, normalization and draft mutation tests in `backend/src/test/java/ec/edu/unl/sec/evaluationmodel/domain/ModelVersionTest.java`
- [X] T010 [P] [US1] Add create/get/replace contract and stale `If-Match` tests in `backend/src/test/java/ec/edu/unl/sec/evaluationmodel/api/EvaluationModelControllerTest.java`
- [X] T011 [US1] Implement draft aggregate with name `1..200`, source `1..2000`, node name `1..300`, description `<=4000` and positive positions in `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/domain/EvaluationModel.java` and `ModelVersion.java`
- [X] T012 [US1] Implement authorized create/list/get/replace and optimistic revision service in `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/application/EvaluationModelService.java`
- [X] T013 [US1] Implement model/version DTOs and REST endpoints with ETags in `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/api/EvaluationModelController.java`
- [X] T014 [US1] Implement typed Angular client and accessible draft editor in `frontend/src/app/features/models/data-access/model-api.service.ts` and `frontend/src/app/features/models/pages/model-editor.page.ts`

## Phase 4: User Story 2 â€” Validar y publicar (P1)

**Independent test**: omit each publication prerequisite, then publish valid v1 with an approver different from author.

- [X] T015 [P] [US2] Add exhaustive publication validation and atomicity tests for FR-008 in `backend/src/test/java/ec/edu/unl/sec/evaluationmodel/domain/PublicationValidatorTest.java`
- [X] T016 [P] [US2] Add formula structure, reference, zero/non-applicable and deterministic-case tests for FR-008a in `backend/src/test/java/ec/edu/unl/sec/evaluationmodel/domain/FormulaValidatorTest.java`
- [X] T017 [US2] Implement aggregated publication issue model and hierarchy/code/scale validator in `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/domain/PublicationValidator.java`
- [X] T018 [US2] Implement allowlisted declarative formula validation without script execution in `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/domain/FormulaValidator.java`
- [X] T019 [US2] Implement authorized atomic publication with independent approver and revision check in `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/application/EvaluationModelService.java`
- [X] T020 [US2] Implement publish endpoint and accessible validation summary UI in `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/api/EvaluationModelController.java` and `frontend/src/app/features/models/ui/publication-panel.component.ts`

## Phase 5: User Story 3 â€” VersiÃ³n sucesora (P1)

**Independent test**: reject every direct mutation to published v1, create/edit/publish v2, and prove v1 remains byte-for-byte equivalent.

- [X] T021 [P] [US3] Add published immutability and successor provenance tests in `backend/src/test/java/ec/edu/unl/sec/evaluationmodel/application/ModelSuccessionServiceTest.java`
- [X] T022 [US3] Implement deep-copy successor creation and monotonic version numbering in `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/application/ModelSuccessionService.java`
- [X] T023 [US3] Implement successor endpoint and version-history UI in `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/api/EvaluationModelController.java` and `frontend/src/app/features/models/pages/model-history.page.ts`

## Phase 6: User Story 4 â€” Consultar y comparar (P2)

**Independent test**: compare two versions of one model with known additions/removals/modifications/unchanged items and hide out-of-scope/different-model content.

- [X] T024 [P] [US4] Add complete diff classification, non-mutation and different-model tests in `backend/src/test/java/ec/edu/unl/sec/evaluationmodel/application/ModelComparisonServiceTest.java`
- [X] T025 [US4] Implement normalized typed comparison in `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/application/ModelComparisonService.java`
- [X] T026 [US4] Implement comparison endpoint and accessible diff UI in `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/api/EvaluationModelController.java` and `frontend/src/app/features/models/pages/model-compare.page.ts`

## Phase 7: User Story 5 â€” AuditorÃ­a (P3)

**Independent test**: correlate accepted/rejected creation, publication, succession, read and comparison events; ordinary actors cannot mutate them.

- [X] T027 [P] [US5] Add safe audit event, failure and ordering tests in `backend/src/test/java/ec/edu/unl/sec/evaluationmodel/application/EvaluationModelAuditTest.java`
- [X] T028 [US5] Integrate transactional/safe rejection audit calls in `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/application/EvaluationModelAuditService.java` and `EvaluationModelService.java`
- [X] T029 [US5] Implement scope-filtered model audit endpoint/UI in `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/api/ModelAuditController.java` and `frontend/src/app/features/models/pages/model-audit.page.ts`

## Phase 8: Verification

- [X] T030 [P] Add deterministic general-2024-inspired synthetic fixture with explicit non-institutional disclaimer in `tests/fixtures/models/general-2024/manifest.json`
- [ ] T031 Add v1 publication, immutability, v2 succession and comparison acceptance journey in `tests/acceptance/model-versioning.spec.ts`
- [X] T032 Update routes, user/technical docs and requirement traceability in `frontend/src/app/features/models/models.routes.ts`, `docs/user/i1-models.qmd`, `docs/technical/i1-models.qmd` and `docs/testing/traceability.md`
- [X] T033 Add architecture checks preventing I1 from importing I0/audit infrastructure in `backend/src/test/java/ec/edu/unl/sec/architecture/ModuleArchitectureTest.java`
- [ ] T034 Run backend/frontend regression, contract linters, synthetic scan and quickstart; record results in `tests/acceptance/results/model-quickstart.md`
- [ ] T035 Validate SC-006 with 20 synthetic administrator participants in `tests/acceptance/results/model-usability.md`
- [ ] T036 Perform final constitutional review in `tests/acceptance/results/model-review-gate.md`

## Dependencies

Setup â†’ Foundational â†’ US1 â†’ US2 â†’ US3. US4 depends on US1; US5 integrates across completed stories. Verification follows selected stories. I2 consumes only `ModelCatalogPort` after publication behavior is stable.

## Parallel opportunities

T003, T006 and T007 target separate files. Test tasks marked `[P]` can be written together before their implementation group. T030 can proceed after the formula schema is fixed.

## Implementation strategy

MVP: phases 1â€“4 produce a publishable immutable v1. Phase 5 completes the required measurable v1â†’v2 outcome; phases 6â€“8 add comparison, audit and verification.
