# Tasks: I0 — Acceso y ámbitos autorizados

**Input**: `/specs/001-identity-access-scopes/` design artifacts. Tests are mandatory and written first.

## Phase 1: Setup

- [X] T001 Reconcile and lint the I0 contract in `specs/001-identity-access-scopes/contracts/openapi.yaml` and `docs/technical/contracts/openapi.yaml`
- [X] T002 Create the security decision record in `docs/adr/0004-opaque-sessions-scope-authorization.md`
- [X] T003 [P] Add typed session/rate-limit/cookie configuration in `backend/src/main/resources/application.yml` and `backend/src/main/java/ec/edu/unl/sec/identityaccess/application/IdentityAccessProperties.java`

## Phase 2: Foundational

- [X] T004 Add clean-migration and append-only/security tests in `backend/src/test/java/ec/edu/unl/sec/migration/IdentityAccessMigrationTest.java`
- [X] T005 Create identity, credential, session, attempt, function, operation, assignment, conflict-policy, exception and privilege tables with checks/indexes in `backend/src/main/resources/db/migration/V003__identity_access.sql`
- [X] T006 [P] Define current-actor and authorization contracts in `backend/src/main/java/ec/edu/unl/sec/identityaccess/application/CurrentActorPort.java` and `backend/src/main/java/ec/edu/unl/sec/identityaccess/application/AuthorizationPort.java`
- [X] T007 [P] Define scope, operation and authorization result value objects in `backend/src/main/java/ec/edu/unl/sec/identityaccess/domain/AuthorizationScope.java`, `OperationCode.java` and `AuthorizationDecision.java`
- [X] T008 Configure cookie authentication, CSRF and public-session routes in `backend/src/main/java/ec/edu/unl/sec/identityaccess/infrastructure/SecurityConfiguration.java`

## Phase 3: User Story 1 — Sesión segura (P1)

- [X] T009 [P] [US1] Add clock-controlled BCrypt, five-failure/15-minute, 30-minute inactivity and revoke tests in `backend/src/test/java/ec/edu/unl/sec/identityaccess/application/SessionServiceTest.java`
- [X] T010 [P] [US1] Add `/sessions` and `/me` security/contract tests without enumeration in `backend/src/test/java/ec/edu/unl/sec/identityaccess/api/SessionControllerTest.java`
- [X] T011 [US1] Implement identity, credential and opaque-session domain records in `backend/src/main/java/ec/edu/unl/sec/identityaccess/domain/AccessIdentity.java`, `Credential.java` and `AccessSession.java`
- [X] T012 [US1] Implement JPA session/identity persistence and SHA-256 token lookup in `backend/src/main/java/ec/edu/unl/sec/identityaccess/infrastructure/JpaIdentityAccessRepository.java`
- [X] T013 [US1] Implement BCrypt authentication, throttling, session touch/revoke and identity deactivation semantics in `backend/src/main/java/ec/edu/unl/sec/identityaccess/application/SessionService.java`
- [X] T014 [US1] Implement session/current-identity DTOs and endpoints in `backend/src/main/java/ec/edu/unl/sec/identityaccess/api/SessionController.java`
- [X] T015 [US1] Implement Angular login/current-user service and accessible login page in `frontend/src/app/features/access/data-access/access-api.service.ts` and `frontend/src/app/features/access/pages/login.page.ts`

## Phase 4: User Story 2 — Usuarios y asignaciones (P1)

- [X] T016 [P] [US2] Add lifecycle, vigency, hidden-ID and stale-revision tests in `backend/src/test/java/ec/edu/unl/sec/identityaccess/application/IdentityAdministrationServiceTest.java`
- [X] T017 [US2] Implement functional assignment and configurable process-function domain models in `backend/src/main/java/ec/edu/unl/sec/identityaccess/domain/FunctionalAssignment.java` and `ProcessFunction.java`
- [X] T018 [US2] Implement authorized create/disable identity and assignment administration in `backend/src/main/java/ec/edu/unl/sec/identityaccess/application/IdentityAdministrationService.java`
- [X] T019 [US2] Implement scope-filtered user/assignment REST endpoints with ETags in `backend/src/main/java/ec/edu/unl/sec/identityaccess/api/IdentityAdministrationController.java`
- [X] T020 [US2] Implement accessible identity and assignment pages in `frontend/src/app/features/access/pages/identity-admin.page.ts` and `frontend/src/app/features/access/ui/assignment-form.component.ts`

## Phase 5: User Story 3 — Autorización por ámbito (P1)

- [X] T021 [P] [US3] Add the full synthetic operation/career/process/indicator/vigency authorization matrix in `backend/src/test/java/ec/edu/unl/sec/identityaccess/application/ScopeAuthorizationServiceTest.java`
- [X] T022 [US3] Implement per-operation assignment reevaluation, collection predicates and hidden direct-ID decisions in `backend/src/main/java/ec/edu/unl/sec/identityaccess/application/ScopeAuthorizationService.java`
- [X] T023 [US3] Publish adapters implementing `CurrentActorPort` and `AuthorizationPort` for downstream modules in `backend/src/main/java/ec/edu/unl/sec/identityaccess/infrastructure/IdentityAccessPortAdapter.java`
- [X] T024 [US3] Add authorization architecture tests preventing consumers from importing I0 infrastructure in `backend/src/test/java/ec/edu/unl/sec/architecture/ModuleArchitectureTest.java`

## Phase 6: User Story 4 — Conflictos y excepciones (P2)

- [X] T025 [P] [US4] Add policy, overlap, authority, expiry and technical-privilege separation tests in `backend/src/test/java/ec/edu/unl/sec/identityaccess/application/IdentityAdministrationServiceTest.java`
- [X] T026 [US4] Implement configurable conflict policies and finite exceptions in `backend/src/main/java/ec/edu/unl/sec/identityaccess/domain/ConflictPolicy.java` and `ConflictException.java`
- [X] T027 [US4] Integrate conflict evaluation into assignment writes and authorization reads in `backend/src/main/java/ec/edu/unl/sec/identityaccess/application/IdentityAdministrationService.java`
- [X] T028 [US4] Implement exception administration endpoints/UI in `backend/src/main/java/ec/edu/unl/sec/identityaccess/api/IdentityAdministrationController.java` and `frontend/src/app/features/access/pages/conflict-exceptions.page.ts`

## Phase 7: User Story 5 — Auditoría (P3)

- [X] T029 [P] [US5] Add safe event and scope-filtered immutable query tests in `backend/src/test/java/ec/edu/unl/sec/identityaccess/application/IdentityAccessAuditTest.java`
- [X] T030 [US5] Implement centralized action/reason allowlists and transactional audit calls in `backend/src/main/java/ec/edu/unl/sec/identityaccess/application/IdentityAccessAuditService.java`
- [X] T031 [US5] Extend audit query adapter/controller for authorized access events in `backend/src/main/java/ec/edu/unl/sec/audit/infrastructure/JpaAuditAdapter.java` and `backend/src/main/java/ec/edu/unl/sec/identityaccess/api/AccessAuditController.java`
- [X] T032 [US5] Implement access audit UI in `frontend/src/app/features/access/pages/access-audit.page.ts`

## Phase 8: Verification

- [X] T033 [P] Add deterministic I0 synthetic fixtures and secret scan coverage in `tests/fixtures/pilot/access/manifest.json` and `scripts/verify-synthetic-data.ps1`
- [X] T034 Add session, scope, conflict and audit acceptance journey in `tests/acceptance/access-scopes.spec.ts`
- [X] T035 Update access routes, user/technical documentation and traceability in `frontend/src/app/features/access/access.routes.ts`, `docs/user/i0-access.qmd`, `docs/technical/i0-access.qmd` and `docs/testing/traceability.md`
- [ ] T036 Run clean migration, backend/frontend regression, contract linters and quickstart; record results in `tests/acceptance/results/access-quickstart.md`
- [ ] T037 Validate SC-007 with 20 synthetic administrator participants and record first-attempt completion under three minutes in `tests/acceptance/results/access-usability.md`
- [ ] T038 Perform final constitutional review of permissions, errors, traceability and synthetic data in `tests/acceptance/results/access-review-gate.md`

## Dependencies

Setup → Foundational → US1 → US2 → US3 → US4 → US5 → Verification. Tests precede implementation in each story. I1 and I2 may consume only T006/T023 public ports after I0 verification.
