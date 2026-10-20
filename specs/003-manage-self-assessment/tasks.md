# Tasks: I2 â€” GestiÃ³n del proceso de autoevaluaciÃ³n

## Phase 1: Setup

- [X] T001 Reconcile/lint I2 contracts in `specs/003-manage-self-assessment/contracts/openapi.yaml` and `docs/technical/contracts/openapi.yaml`
- [X] T002 Create state/fixed-model ADR in `docs/adr/0006-fixed-model-process-state-machine.md`

## Phase 2: Foundational

- [X] T003 Add clean migration/history tests in `backend/src/test/java/ec/edu/unl/sec/migration/SelfAssessmentMigrationTest.java`
- [X] T004 Create process, indicator snapshot, assignment, transition and closure tables in `backend/src/main/resources/db/migration/V005__self_assessment.sql`
- [X] T005 [P] Define repository and downstream process catalog ports in `backend/src/main/java/ec/edu/unl/sec/selfassessment/application/SelfAssessmentRepository.java` and `ProcessCatalogPort.java`
- [X] T006 [P] Define process state, assignment function and aggregate types in `backend/src/main/java/ec/edu/unl/sec/selfassessment/domain/SelfAssessmentProcess.java` and `ProcessAssignment.java`
- [X] T007 Implement JDBC persistence with optimistic revisions in `backend/src/main/java/ec/edu/unl/sec/selfassessment/infrastructure/JdbcSelfAssessmentRepository.java`

## Phase 3: User Story 1 â€” Abrir y activar (P1)

- [X] T008 [P] [US1] Add creation/activation/model-publication tests in `backend/src/test/java/ec/edu/unl/sec/selfassessment/application/SelfAssessmentServiceTest.java`
- [X] T009 [P] [US1] Add REST/ETag tests in `backend/src/test/java/ec/edu/unl/sec/selfassessment/api/SelfAssessmentControllerTest.java`
- [X] T010 [US1] Implement authorized draft creation and activation in `backend/src/main/java/ec/edu/unl/sec/selfassessment/application/SelfAssessmentService.java`
- [X] T011 [US1] Implement process DTOs/endpoints in `backend/src/main/java/ec/edu/unl/sec/selfassessment/api/SelfAssessmentController.java`
- [X] T012 [US1] Implement Angular process client/create page in `frontend/src/app/features/processes/data-access/process-api.service.ts` and `frontend/src/app/features/processes/pages/process-create.page.ts`

## Phase 4: User Story 2 â€” Conservar modelo (P1)

- [X] T013 [P] [US2] Add fixed-version/v2 publication tests in `backend/src/test/java/ec/edu/unl/sec/selfassessment/domain/FixedModelSnapshotTest.java`
- [X] T014 [US2] Implement indicator snapshot and downstream published process view in `backend/src/main/java/ec/edu/unl/sec/selfassessment/infrastructure/ProcessCatalogAdapter.java`
- [X] T015 [US2] Implement fixed-model detail UI in `frontend/src/app/features/processes/pages/process-detail.page.ts`

## Phase 5: User Story 3 â€” Asignar participantes (P1)

- [X] T016 [P] [US3] Add identity/function/indicator/vigency/conflict/critical-change matrix tests in `backend/src/test/java/ec/edu/unl/sec/selfassessment/application/ProcessAssignmentServiceTest.java`
- [X] T017 [US3] Implement assignment validation/history through I0 public `FunctionalAssignmentPolicyPort` in `backend/src/main/java/ec/edu/unl/sec/selfassessment/application/ProcessAssignmentService.java`
- [X] T018 [US3] Implement assignment endpoints/UI in `backend/src/main/java/ec/edu/unl/sec/selfassessment/api/ProcessAssignmentController.java` and `frontend/src/app/features/processes/ui/process-assignment-form.component.ts`

## Phase 6: User Story 4 â€” Estados y reapertura (P2)

- [X] T019 [P] [US4] Add full valid/invalid transition, closed lock and reopen tests in `backend/src/test/java/ec/edu/unl/sec/selfassessment/domain/ProcessStateMachineTest.java`
- [X] T020 [US4] Implement explicit state machine and preserved closure history in `backend/src/main/java/ec/edu/unl/sec/selfassessment/domain/ProcessStateMachine.java`
- [X] T021 [US4] Implement transition/reopen service/endpoints with ETags and reasons in `backend/src/main/java/ec/edu/unl/sec/selfassessment/application/ProcessTransitionService.java` and `backend/src/main/java/ec/edu/unl/sec/selfassessment/api/SelfAssessmentController.java`
- [X] T022 [US4] Implement accessible state controls in `frontend/src/app/features/processes/ui/process-state-controls.component.ts`

## Phase 7: User Story 5 â€” AuditorÃ­a (P3)

- [X] T023 [P] [US5] Add safe event/rejection tests in `backend/src/test/java/ec/edu/unl/sec/selfassessment/application/SelfAssessmentAuditTest.java`
- [X] T024 [US5] Implement transactional audit integration/query endpoint in `backend/src/main/java/ec/edu/unl/sec/selfassessment/application/SelfAssessmentAuditService.java` and `backend/src/main/java/ec/edu/unl/sec/selfassessment/api/ProcessAuditController.java`

## Phase 8: Verification

- [X] T025 [P] Add deterministic process fixture in `tests/fixtures/pilot/process/manifest.json`
- [ ] T026 Add fixed-v1, assignments, states and reopen acceptance in `tests/acceptance/self-assessment-process.spec.ts`
- [X] T027 Update routes/docs/traceability in `frontend/src/app/features/processes/processes.routes.ts`, `docs/user/i2-processes.qmd`, `docs/technical/i2-processes.qmd` and `docs/testing/traceability.md`
- [X] T028 Add I2 cross-module architecture rule in `backend/src/test/java/ec/edu/unl/sec/architecture/ModuleArchitectureTest.java`
- [ ] T029 Run regression/contracts/synthetic scan/quickstart and record `tests/acceptance/results/process-quickstart.md`
- [ ] T030 Validate 20-participant SC-007 and final review in `tests/acceptance/results/process-usability.md` and `tests/acceptance/results/process-review-gate.md`

## Dependencies

Setup â†’ Foundational â†’ US1 â†’ US2 â†’ US3 â†’ US4 â†’ US5 â†’ Verification. I3 consumes `ProcessCatalogPort` only after activation and fixed-model tests pass.
