# Tasks: Flujos completos de evaluación de carreras

**Input**: Design documents from `/specs/005-complete-evaluation-workflows/`

**Prerequisites**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/openapi.yaml`

**Tests**: obligatorias para contratos, permisos, cálculos, estados, versionado y trazabilidad.

**Alcance de actualización documental**: los checks de este archivo significan trabajo implementado y validado, no documentación redactada. Las pruebas/operaciones de D1–D8 siguen pendientes mientras no exista evidencia ejecutada. Los añadidos T113–T126 se insertan en la fase correspondiente sin renumerar IDs previos.

**Reglas obligatorias**: [domain-rules.md](../../docs/requirements/domain-rules.md) y [process-acceptance.md](../../docs/testing/process-acceptance.md). Los criterios AC-X01–AC-X16 y AC-P01-01–AC-P16-03 completan las tareas de prueba.

**Revisión del código existente (2026-10-21)**: [implementation-review.md](../../docs/testing/implementation-review.md) registra correcciones de autorización inicial, estados, permisos, ETags y borradores. Son avances parciales; no completan por sí solos las tareas ni las entregas de este plan.

**Siguiente ejecución**: D1 ya está verificado. D2 completó su alcance de software con T006/T021–T030 y T114/T116; la [evidencia de modelos completos](../../tests/acceptance/results/d2-five-models.md) conserva los límites de JDK 24, PostgreSQL y verificación institucional. No se declara aceptación institucional ni API v2 completa. Conservar los 126 IDs y extender las verificaciones transversales por entrega; los módulos académicos no bloquean el MVP.

## Phase 1: Setup and reconciliation

**D1 técnico cerrado**: T005/T113/T115 y T013–T020 cuentan con evidencia en `tests/acceptance/results/d1-careers.md`. T002/T009/T012 avanzaron solo en D1 y siguen abiertos para las demás entregas. Los módulos D2–D8, 004 y la aceptación institucional no se dan por terminados.

- [X] T001 Reconcile FR-001–FR-070, SC-001–SC-018 and P01–P16 in `docs/testing/traceability.md`
- [X] T113 Review endpoint/schema collisions between `specs/005-complete-evaluation-workflows/contracts/openapi.yaml` and `docs/technical/contracts/openapi.yaml`; record compatibility/versioning decisions in `docs/technical/migration-p01-p16.qmd` before T002, without replacing implemented schemas silently
- [X] T002 Merge the completion contract surface incrementally per delivery into `docs/technical/contracts/openapi.yaml` after T113 and update affected client schemas without silent breaking changes
- [X] T003 [P] Add `governance`, `improvement` and `archive` module boundaries to `backend/src/test/java/ec/edu/unl/sec/architecture/ModuleArchitectureTest.java`
- [X] T004 [P] Add frontend routes/placeholders for careers, reports, improvement and archive in `frontend/src/app/app.routes.ts`
- [X] T005 Define the forward-only migration sequence in `docs/technical/migration-p01-p16.qmd`; V008 implements careers, V009/V010 reserve models/governance and V011 evidence immutability before V012–V020; verify V001–V008 on the disposable PostgreSQL history without out-of-order or repair
- [X] T006 Add synthetic normative manifests for the five supplied models in `tests/fixtures/models/normative-manifest.json`
- [X] T114 [P] Produce a source-by-source verification manifest with exact pages, edition and unresolved applicability for the five models and UNL instruction in `tests/fixtures/models/normative-verification.json`; link unresolved CL-011–CL-017 to institutional owners without claiming approval

## Phase 2: Foundational governance

- [X] T007 [P] Add policy-backed institutional authority types in `backend/src/main/java/ec/edu/unl/sec/identityaccess/domain/InstitutionalAuthority.java`
- [X] T008 [P] Add append-only decision metadata value object in `backend/src/main/java/ec/edu/unl/sec/shared/domain/DecisionProvenance.java`
- [X] T009 Extend existing capabilities for careers, authorizations, reports, improvement and archive in `backend/src/main/java/ec/edu/unl/sec/identityaccess/domain/OperationCode.java` per delivery; preserve `PROCESS_AUTHORIZE` and do not create a duplicate capability catalog
- [X] T010 Extend scoped authorization tests for all P01–P16 operations in `backend/src/test/java/ec/edu/unl/sec/identityaccess/application/CompleteWorkflowAuthorizationTest.java`
- [X] T011 Extend RFC 9457 error codes for applicability, unvalidated rules and closure blockers in `backend/src/main/java/ec/edu/unl/sec/shared/api/ApiProblemHandler.java`
- [X] T012 Add migration tests from each accepted schema and from clean PostgreSQL in `backend/src/test/java/ec/edu/unl/sec/migration/CompleteWorkflowMigrationTest.java`
- [X] T115 Establish the prerequisite gate for unfinished I0–I3 paths, list T027–T088 from `specs/004-submit-evidence/tasks.md` that remain necessary, and record evidence in `tests/acceptance/results/p01-p16-prerequisites.md`; D4 cannot pass until upload, reference, submit, download, successor and recovery contracts are executable

## Phase 3: US1 — Careers and scopes (D1, P01–P02)

**Independent Test**: create, update and deactivate a referenced career while preserving history and scoped access.

- [X] T013 [P] [US1] Add contract and authorization tests for career CRUD/deactivation in `backend/src/test/java/ec/edu/unl/sec/selfassessment/api/CareerControllerTest.java`
- [X] T014 [P] [US1] Add unique-code, unit-membership and no-delete domain tests in `backend/src/test/java/ec/edu/unl/sec/selfassessment/domain/CareerTest.java`
- [X] T015 [US1] Create forward migration for `academic_unit` and career lifecycle fields in `backend/src/main/resources/db/migration/V008__career_catalog.sql`
- [X] T016 [US1] Implement `AcademicUnit` in `backend/src/main/java/ec/edu/unl/sec/selfassessment/domain/AcademicUnit.java` and `Career` in `backend/src/main/java/ec/edu/unl/sec/selfassessment/domain/Career.java`; enforce "Código normalizado único; una carrera referenciada solo se desactiva."
- [X] T017 [US1] Implement scoped career administration in `backend/src/main/java/ec/edu/unl/sec/selfassessment/application/CareerAdministrationService.java`
- [X] T018 [US1] Implement career API endpoints in `backend/src/main/java/ec/edu/unl/sec/selfassessment/api/CareerController.java`
- [X] T019 [US1] Implement accessible career catalog and deactivation flow in `frontend/src/app/features/careers/pages/career-catalog.page.ts`
- [X] T020 [US1] Record D1 acceptance evidence in `tests/acceptance/results/d1-careers.md`

## Phase 4: US2 — Configurable models (D2, P03–P04)

**Independent Test**: publish the five configurations with variable hierarchies, required sources and per-indicator methods using one engine.

**Avance interno D2 (2026-10-22)**: [bases de jerarquía](../../tests/acceptance/results/d2-model-foundations.md), [publicación por indicador](../../tests/acceptance/results/d2-model-publication.md) y [modelos completos](../../tests/acceptance/results/d2-five-models.md). T006/T021–T030 y T114/T116 tienen pruebas aprobadas de aplicación, snapshots, migración SQLite y endpoints aditivos v1. D2 queda demostrada para software sintético; no se declara aceptación institucional, aplicación PostgreSQL de V009 ni API v2 completa.

- [X] T021 [P] [US2] Add recursive hierarchy and direct-indicator tests in `backend/src/test/java/ec/edu/unl/sec/evaluationmodel/domain/FlexibleModelHierarchyTest.java`
- [X] T022 [P] [US2] Add source/rule publication tests including variables, units, thresholds and unvalidated strategies in `backend/src/test/java/ec/edu/unl/sec/evaluationmodel/application/CompleteModelPublicationTest.java`
- [X] T023 [P] [US2] Add five-model fixture acceptance tests in `backend/src/test/java/ec/edu/unl/sec/evaluationmodel/application/FiveModelConfigurationTest.java`
- [X] T024 [US2] Create migration for normative applicability, recursive node types, required sources and valuation rules in `backend/src/main/resources/db/migration/V009__complete_evaluation_models.sql`
- [X] T025 [US2] Replace rigid publication validation in `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/domain/PublicationValidator.java` and update existing callers in `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/application/EvaluationModelService.java` and `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/infrastructure/ModelCatalogAdapter.java` without duplicating publication services
- [X] T026 [US2] Implement indicator definitions in `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/domain/IndicatorDefinition.java` and required sources in `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/domain/RequiredSource.java`, enforcing "Código único por indicador; `cardinality_min >= 1`; máximo nulo o >= mínimo."; preserve per-indicator scales/rules and "Solo una regla `VALIDATED` puede calcular automáticamente."
- [X] T027 [US2] Implement normative applicability service without inferring vigency in `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/application/ModelApplicabilityService.java`
- [X] T028 [US2] Extend model API schemas/controllers in `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/api/EvaluationModelController.java`
- [X] T029 [US2] Extend the existing model editor for hierarchy, sources and rules in `frontend/src/app/features/models/pages/model-editor.page.ts` and its DTOs in `frontend/src/app/features/models/data-access/model.models.ts`
- [X] T030 [US2] Validate and document the five fixture manifests in `tests/acceptance/results/d2-five-models.md`
- [X] T116 [US2] Implement `NormativeReference` and career applicability with verification status, source/disposition links per indicator and forward-only persistence in `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/domain/NormativeReference.java` and `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/infrastructure/JpaNormativeReferenceRepository.java`; prove unresolved institutional applicability blocks activation in `backend/src/test/java/ec/edu/unl/sec/evaluationmodel/application/NormativeApplicabilityTest.java` before accepting T030

## Phase 5: US3 — Authorization and planning (D3, P05–P06)

**D3 tecnico verificado (2026-10-22)**: se conecto la procedencia autorizadora de suspension,
reanudacion y reapertura entre controles y cliente HTTP (avance T038), con pruebas de
componente, cliente y controlador. Ver `tests/acceptance/results/d3-process-planning.md`.
T032 y T117 quedan verificados para software sintetico: membresias con rol propio,
acceso efectivo por equipos, conflictos individuales e integridad indicador/asignacion/fuente.
T031/T034 quedan comprobados por las pruebas existentes de ciclo de vida y estados;
T037/T038 incorporan contrato v1 reconciliado, pruebas HTTP y consulta de actividades
para participantes. T033/T035/T036 quedan verificados en SQLite y PostgreSQL 18.4
descartable: migracion V009 -> V010, esquema limpio hasta V013, persistencia, historial
y rollback. T039 completa la aceptacion tecnica con Edge, backend y PostgreSQL
reales. La correccion de vinculos normativos ocupa V014; D5-D8 se reservan ahora
V015-V020, sin reusar V013 (snapshots estructurados) ni editar migraciones historicas.

**Independent Test**: keep a process in preparation until authorized, then assign teams/activities, suspend and resume it.

- [X] T031 [P] [US3] Add state/authorization/suspension tests in `backend/src/test/java/ec/edu/unl/sec/selfassessment/domain/AuthorizedProcessLifecycleTest.java`
- [X] T032 [P] [US3] Add assignment, due-date and conflict tests in `backend/src/test/java/ec/edu/unl/sec/selfassessment/application/ProcessPlanningServiceTest.java`
- [X] T033 [US3] Create migration for authorizations, suspension decisions, teams and activities in `backend/src/main/resources/db/migration/V010__process_governance.sql`
- [X] T034 [US3] Extend process states and transition policy in `backend/src/main/java/ec/edu/unl/sec/selfassessment/domain/SelfAssessmentProcess.java`
- [X] T035 [US3] Complete policy-backed authorization and suspension by extending `backend/src/main/java/ec/edu/unl/sec/selfassessment/application/SelfAssessmentService.java`, `backend/src/main/java/ec/edu/unl/sec/selfassessment/application/ProcessTransitionService.java` and the existing `backend/src/main/java/ec/edu/unl/sec/identityaccess/application/InstitutionalAuthorityPort.java`; reconcile initial authorization snapshots without inventing legacy decisions
- [X] T036 [US3] Implement teams, activities and deadlines in `backend/src/main/java/ec/edu/unl/sec/selfassessment/application/ProcessPlanningService.java`
- [X] T037 [US3] Extend authorization endpoints in `backend/src/main/java/ec/edu/unl/sec/selfassessment/api/SelfAssessmentController.java` and add planning endpoints in `backend/src/main/java/ec/edu/unl/sec/selfassessment/api/ProcessGovernanceController.java` according to T113 compatibility decisions; never register duplicate route mappings
- [X] T038 [US3] Implement process authorization and planning UI in `frontend/src/app/features/processes/pages/process-planning.page.ts`
- [X] T039 [US3] Record D3 acceptance evidence in `tests/acceptance/results/d3-process-planning.md`
- [X] T117 [US3] Validate individual team membership, exactly one user/team per assignment, dependency-cycle rejection and deadline exceptions in `backend/src/test/java/ec/edu/unl/sec/selfassessment/application/TeamPlanningIntegrityTest.java`; implement the corresponding rules and API revisions in `backend/src/main/java/ec/edu/unl/sec/selfassessment/application/ProcessPlanningService.java` before T039

## Phase 6: US4 — Required-source coverage (D4, P07–P08)

**Independent Test**: submit evidence for three of four applicable sources and preserve explicit coverage per immutable version.

- [x] T040 [P] [US4] Add source-coverage domain and authorization tests in `backend/src/test/java/ec/edu/unl/sec/evidence/domain/SourceCoverageTest.java`
- [x] T041 [P] [US4] Add applicability-decision tests in `backend/src/test/java/ec/edu/unl/sec/evidence/application/ApplicabilityDecisionServiceTest.java`
- [x] T042 [P] [US4] Add API tests rejecting foreign/incompatible required sources in `backend/src/test/java/ec/edu/unl/sec/evidence/api/EvidenceSourceCoverageControllerTest.java`
- [x] T043 [US4] Create migration for `source_coverage` and `applicability_decision` in `backend/src/main/resources/db/migration/V012__source_coverage.sql`
- [x] T044 [US4] Implement `SourceCoverage` and invariants in `backend/src/main/java/ec/edu/unl/sec/evidence/domain/SourceCoverage.java`
- [x] T045 [US4] Require explicit coverage during submission in `backend/src/main/java/ec/edu/unl/sec/evidence/application/SubmitEvidenceService.java`
- [x] T046 [US4] Implement applicability decisions in `backend/src/main/java/ec/edu/unl/sec/evidence/application/ApplicabilityDecisionService.java`
- [x] T047 [US4] Add source selection and reuse controls in `frontend/src/app/features/evidence/ui/evidence-source-coverage.component.ts`
- [x] T048 [US4] Extend immutable version detail with coverage snapshots in `frontend/src/app/features/evidence/pages/evidence-version.page.ts`
- [x] T049 [US4] Record D4 acceptance evidence in `tests/acceptance/results/d4-source-coverage.md`
- [x] T118 [US4] Implement typed structured-data/institutional-reference snapshots, period/unit validation, API and editor support in `backend/src/main/java/ec/edu/unl/sec/evidence/application/StructuredEvidenceService.java`, `backend/src/main/java/ec/edu/unl/sec/evidence/api/StructuredEvidenceController.java` and `frontend/src/app/features/evidence/ui/structured-evidence.component.ts`; verify AC-X16 in `backend/src/test/java/ec/edu/unl/sec/evidence/application/StructuredEvidenceServiceTest.java` before T049

## Phase 7: US5 — Review and correction (D5, P09–P10)

**Independent Test**: reject v1, submit v2, accept v2 and prove no academic evaluation was created.

- [x] T050 [P] [US5] Add separation, self-review and rectification tests in `backend/src/test/java/ec/edu/unl/sec/documentreview/domain/CompleteDocumentReviewTest.java`
- [x] T051 [P] [US5] Add review-inbox and coverage-projection API tests in `backend/src/test/java/ec/edu/unl/sec/documentreview/api/DocumentReviewControllerTest.java`
- [x] T052 [US5] Create append-only review migration in `backend/src/main/resources/db/migration/V015__document_review.sql`, adapting rather than duplicating any historical schema already applied
- [x] T053 [US5] Implement review decision and rectification services in `backend/src/main/java/ec/edu/unl/sec/documentreview/application/DocumentReviewService.java`
- [x] T054 [US5] Link correction successors to the triggering review in `backend/src/main/java/ec/edu/unl/sec/evidence/application/CreateEvidenceSuccessorService.java`
- [x] T055 [US5] Implement reviewer inbox and decision history UI in `frontend/src/app/features/reviews/pages/document-review-inbox.page.ts`
- [x] T056 [US5] Add v1 rejected → v2 accepted acceptance journey in `tests/acceptance/document-review-correction.spec.ts`
- [x] T057 [US5] Record D5 acceptance evidence in `tests/acceptance/results/d5-document-review.md`
- [x] T119 [US5] Persist review outcomes per coverage and derive global status without duplicating evidence state in `backend/src/main/java/ec/edu/unl/sec/documentreview/domain/CoverageReviewOutcome.java` and `backend/src/main/java/ec/edu/unl/sec/documentreview/infrastructure/JpaDocumentReviewRepository.java`; verify AC-X06–AC-X07 and no inherited acceptance in `backend/src/test/java/ec/edu/unl/sec/documentreview/application/CoverageReviewProjectionTest.java` before T057

## Phase 8: US6 — Documentary tracking (D6, P11 and basic P16) 🎯 MVP

**Independent Test**: calculate 75% collection and 50% review for four applicable sources and close a documentary snapshot with P12–P15 disabled.

**Avance parcial D6 (2026-10-24)**: T058/T062 verificadas con JDK 25; fórmulas,
cardinalidad, opcionales, exclusiones y corrección de coberturas retiradas por
sucesoras. La suite backend registra 343 pruebas, 0 fallos, 0 errores y 10
omitidas. Ver [bases del cálculo](../../tests/acceptance/results/d6-progress-foundations.md).
T120 solo avanzó en semántica de candidatas; la continuación de consulta e
interfaz se registra a continuación. Persistencia, cierre y aceptación del MVP
permanecen pendientes.

**Consulta y tablero D6 (2026-10-24)**: T059/T063/T065 verificadas con permiso
`PROGRESS_READ` por indicador, API v1 aditiva, puertos de lectura, adaptadores
SQLite y tablero Angular. Suite backend: 365 pruebas, 0 fallos/errores y 10
omitidas; frontend: 51 aprobadas; lint, build y contratos aprobados.
Ver [evidencia de consulta](../../tests/acceptance/results/d6-progress-query.md).
La persistencia de cortes, cierre, validación PostgreSQL de los adaptadores y
aceptación completa del MVP siguen pendientes.

**Cortes históricos D6 (2026-10-24)**: T061 verificada en SQLite y PostgreSQL
18.4 temporal desde limpio y desde V015. Hay guardado de corte actual con
SHA-256, auditoría y reserva serializable de revisión sin cambiar ACTIVE,
API/histórico autorizado y pantalla `/reporting/history`. Avance parcial de
T064/T066/T120; no se marca cierre documental ni aceptación del MVP.
Ver [evidencia de cortes](../../tests/acceptance/results/d6-documentary-snapshots.md).
Suite backend final: 382 pruebas, 0 fallos/errores y 11 omitidas; frontend: 57
aprobadas. Persistencia/concurrencia opt-in: 5 aprobadas con SQLite/PostgreSQL;
lint, build y contratos aprobados. No se declara el cierre de fase documental.

**Checkpoint técnico D6 completado (2026-10-24)**: T060/T064/T066/T067/T120
verificadas con cierre estricto, integridad de archivos, rollback y bloqueo común
de escritores. Suite backend: 401 pruebas, 0 fallos/errores, 12 omitidas;
frontend: 62 aprobadas, lint/build aprobados. Persistencia opt-in: 6 casos;
carreras PostgreSQL: 2 casos. El navegador real completó 75 %/50 %, corte,
bloqueo de pendientes y cierre sin P12–P15 (1 caso aprobado). Los estados
parciales anteriores son históricos. Ver [evidencia y límites](../../tests/acceptance/results/d6-documentary-mvp.md).
La validación institucional sigue pendiente; D7–D8 conservan su backlog.

- [X] T058 [P] [US6] Add exact-formula and zero-denominator tests in `backend/src/test/java/ec/edu/unl/sec/reporting/domain/DocumentaryProgressCalculatorTest.java`
- [X] T059 [P] [US6] Add scope-filtered drill-down tests in `backend/src/test/java/ec/edu/unl/sec/reporting/application/DocumentaryProgressQueryTest.java`
- [X] T060 [P] [US6] Add documentary-closure independence tests in `backend/src/test/java/ec/edu/unl/sec/reporting/application/DocumentaryClosureServiceTest.java`
- [X] T061 [US6] Create progress and documentary snapshot migration in `backend/src/main/resources/db/migration/V016__documentary_reporting.sql`
- [X] T062 [US6] Implement source-based calculator in `backend/src/main/java/ec/edu/unl/sec/reporting/domain/DocumentaryProgressCalculator.java`
- [X] T063 [US6] Implement scoped drill-down query in `backend/src/main/java/ec/edu/unl/sec/reporting/application/DocumentaryProgressQueryService.java`
- [X] T064 [US6] Implement documentary closure snapshot in `backend/src/main/java/ec/edu/unl/sec/reporting/application/DocumentaryClosureService.java`
- [X] T065 [US6] Implement collection/review dashboard with academic status separate in `frontend/src/app/features/reporting/pages/documentary-dashboard.page.ts`
- [X] T066 [US6] Implement documentary closure and historical view in `frontend/src/app/features/reporting/pages/documentary-closure.page.ts`
- [X] T067 [US6] Run MVP with P12–P15 disabled and record SC-017 in `tests/acceptance/results/d6-documentary-mvp.md`
- [X] T120 [US6] Verify distinct sources, cardinality, optional/excluded sources, successor cutoffs, rounding and snapshot-vs-closure transitions in `backend/src/test/java/ec/edu/unl/sec/reporting/application/DocumentarySnapshotSemanticsTest.java`; apply `domain-rules.md` to `backend/src/main/java/ec/edu/unl/sec/reporting/application/DocumentaryClosureService.java` before T067

## Phase 9: US7 — Academic evaluation (D7, P12)

**Independent Test**: complete one qualitative and one validated quantitative evaluation; reject missing variables and unvalidated rules.

**Reconciliación D7 y primer incremento manual (2026-10-24)**: se encontraron
marcas de finalización con clases locales de prueba, cuerpos vacíos y respuestas
fijas. Se reabren las tareas académicas y de informes sin implementación real;
las migraciones V017–V020 se conservan como historia y no se reescriben.
T073 ahora tiene valoración manual persistente, autorización por indicador,
regla/escala publicadas, sustentos aceptados, revisiones propias inmutables y
auditoría transaccional. T074/T075 avanzan solo en su parte manual con API v1
aditiva y pantalla `/evaluations`; siguen abiertas por el flujo cuantitativo.
V021 agrega las valoraciones sin alterar tablas históricas. Cálculo y
conciliación provisionales devuelven 501 en lugar de éxitos simulados.
Ver [evidencia manual y pendientes](../../tests/acceptance/results/d7-academic-evaluation.md).
Este incremento no completa D7 ni habilita resultados consolidados finales.

**Continuación cuantitativa (2026-10-24)**: T068/T069/T071/T072/T074/T075
verificadas con estrategia `ratio-percent/1`, captura por variable de cifras,
unidades, período y sustentos, verificación explícita y cálculo inmutable con
regla/escala/umbral completos. V022 sucede V021 sin reescribir V017. El alcance
admite únicamente el contrato exacto documentado; las estrategias y reglas no
soportadas se rechazan. La API v1 es aditiva y las rutas provisionales siguen
devolviendo 501. T070 avanza en variables/cálculos mediante V022, pero conserva
pendientes las discrepancias; T121 y T076 siguen abiertos por conciliación.
El navegador completó juicios cualitativos y cálculos 75 %/50 %, preservando
predecesores. Ver [evidencia cuantitativa](../../tests/acceptance/results/d7-quantitative-evaluation.md).

**Conciliación y cierre técnico de P12 (2026-10-25)**: T070/T121/T076 completadas
mediante V023 y el flujo autorizado de discrepancias. Se conservan ambos
fundamentos y una resolución append-only con procedimiento, participantes,
asignación competente y juicio seleccionado. La consulta marca sustentos
invalidados, revisiones sustituidas y variables posteriores sin alterar los
juicios originales. Una resolución obsoleta deja de consolidar. Se verificaron
483 pruebas backend (15 omitidas), 82 frontend y tres recorridos reales de
navegador; PostgreSQL limpio y upgrades V020/V021/V022 aprobados. Se completa
P12 en su alcance técnico soportado; P13–P14 e institucionalización permanecen
pendientes. Ver [evidencia de conciliación](../../tests/acceptance/results/d7-academic-reconciliation.md).

- [X] T068 [P] [US7] Add qualitative/quantitative rule tests in `backend/src/test/java/ec/edu/unl/sec/academicevaluation/domain/IndicatorValuationRuleTest.java`
- [X] T069 [P] [US7] Add variable verification and strategy registry tests in `backend/src/test/java/ec/edu/unl/sec/academicevaluation/application/AcademicCalculationServiceTest.java`
- [X] T070 [US7] Create forward migrations for variable values, calculation runs and disagreements in `backend/src/main/resources/db/migration/V022__verified_quantitative_calculations.sql` and `backend/src/main/resources/db/migration/V023__academic_reconciliation.sql`; retain historical V017
- [X] T071 [US7] Implement variable capture and verification in `backend/src/main/java/ec/edu/unl/sec/academicevaluation/application/VariableValueService.java`
- [X] T072 [US7] Implement validated calculation execution in `backend/src/main/java/ec/edu/unl/sec/academicevaluation/application/AcademicCalculationService.java`
- [X] T073 [US7] Extend manual evaluation with method/limitations in `backend/src/main/java/ec/edu/unl/sec/academicevaluation/application/AcademicEvaluationService.java`
- [X] T074 [US7] Add academic evaluation endpoints in `backend/src/main/java/ec/edu/unl/sec/academicevaluation/api/AcademicEvaluationController.java`
- [X] T075 [US7] Implement qualitative and quantitative evaluation UI in `frontend/src/app/features/evaluations/pages/indicator-evaluation.page.ts`
- [X] T076 [US7] Record D7/P12 acceptance evidence in `tests/acceptance/results/d7-academic-evaluation.md`
- [X] T121 [US7] Implement discrepancy capture/resolution and invalidated-support flags with preserved prior valuations in `backend/src/main/java/ec/edu/unl/sec/academicevaluation/application/AcademicReconciliationService.java`, `backend/src/main/java/ec/edu/unl/sec/academicevaluation/api/AcademicReconciliationController.java` and `frontend/src/app/features/evaluations/pages/academic-disagreement.page.ts`; test AC-X11 in `backend/src/test/java/ec/edu/unl/sec/academicevaluation/application/AcademicReconciliationServiceTest.java` before T076

## Phase 10: US8 — Findings and reports (D7, P13–P14)

**Independent Test**: create findings, preliminary report, observations, final report and approval decision with full traceability.

**Estado reconciliado (2026-10-25)**: los esqueletos vacíos de hallazgos,
informes y aceptación fueron reemplazados por implementación real
(T077–T085/T122) y verificados con la suite backend (517 pruebas), la suite
frontend (95 pruebas), contratos y el recorrido AC-X12 de navegador sin
mocks. Véase `tests/acceptance/results/d7-reports.md`. La aceptación
institucional y la campaña de rendimiento siguen pendientes; no se declara
D7 completo.

- [X] T077 [P] [US8] Add finding linkage and no-arbitrary-aggregation tests in `backend/src/test/java/ec/edu/unl/sec/governance/domain/FindingTest.java`
- [X] T078 [P] [US8] Add report version/approval tests including no-accreditation language in `backend/src/test/java/ec/edu/unl/sec/governance/application/ReportWorkflowServiceTest.java`
- [X] T079 [US8] Create migration for findings, report versions, observations and approval decisions in `backend/src/main/resources/db/migration/V018__findings_reports.sql`
- [X] T080 [US8] Implement finding consolidation in `backend/src/main/java/ec/edu/unl/sec/governance/application/FindingService.java`
- [X] T081 [US8] Implement report workflow and append-only decisions in `backend/src/main/java/ec/edu/unl/sec/governance/application/ReportWorkflowService.java`
- [X] T082 [US8] Implement findings/report endpoints in `backend/src/main/java/ec/edu/unl/sec/governance/api/ReportController.java`
- [X] T083 [US8] Implement findings and report workflow UI in `frontend/src/app/features/reports/pages/report-workflow.page.ts`
- [X] T084 [US8] Add preliminary → observations → final → approved journey in `tests/acceptance/report-workflow.spec.ts`
- [X] T085 [US8] Record P13–P14 acceptance evidence in `tests/acceptance/results/d7-reports.md`
- [X] T122 [US8] Complete socialization, observation replies, corrected-version linkage and version-specific authority decisions in `backend/src/main/java/ec/edu/unl/sec/governance/application/ReportWorkflowService.java` and `frontend/src/app/features/reports/pages/report-workflow.page.ts`; prove emitted content remains immutable in `backend/src/test/java/ec/edu/unl/sec/governance/application/IssuedReportImmutabilityTest.java` before T085

## Phase 11: US9 — Improvement plans (D8, P15)

**Independent Test**: turn a weakness into an approved action, track progress, expire it and preserve revisions.

- [X] T086 [P] [US9] Add action lifecycle, versioning and deadline tests in `backend/src/test/java/ec/edu/unl/sec/improvement/domain/ImprovementActionTest.java`
- [X] T087 [P] [US9] Add finding-link and authorization API tests in `backend/src/test/java/ec/edu/unl/sec/improvement/api/ImprovementControllerTest.java`
- [X] T088 [US9] Create migration for plans, actions, progress and verification in `backend/src/main/resources/db/migration/V025__improvement_workflow.sql`
- [X] T089 [US9] Implement plan/action workflow in `backend/src/main/java/ec/edu/unl/sec/improvement/application/ImprovementPlanService.java`
- [X] T090 [US9] Implement progress and verification in `backend/src/main/java/ec/edu/unl/sec/improvement/application/ActionProgressService.java`
- [X] T091 [US9] Implement improvement endpoints in `backend/src/main/java/ec/edu/unl/sec/improvement/api/ImprovementController.java`
- [X] T092 [US9] Implement improvement dashboard UI in `frontend/src/app/features/improvement/pages/improvement-plan.page.ts`
- [X] T093 [US9] Record P15 acceptance evidence in `tests/acceptance/results/d8-improvement.md`
- [X] T123 [US9] Enforce separate approval/execution/verification capabilities and append-only progress after evaluation closure when policy permits in `backend/src/main/java/ec/edu/unl/sec/improvement/application/ActionProgressService.java`; test overdue derivation and immutable closure in `backend/src/test/java/ec/edu/unl/sec/improvement/application/PostClosureImprovementTest.java` before T093

## Phase 12: US10 — Integral closure and archive (D8, P16)

**Independent Test**: block incomplete closure, authorize an exception, archive/restore the snapshot and create a separate later process.

- [X] T094 [P] [US10] Add closure-policy and exception tests in `backend/src/test/java/ec/edu/unl/sec/archive/domain/IntegralClosurePolicyTest.java`
- [X] T095 [P] [US10] Add archive authorization, integrity and historical-query tests in `backend/src/test/java/ec/edu/unl/sec/archive/application/ArchiveServiceTest.java`
- [X] T096 [US10] Create migration for closure policies, snapshots and archive records in `backend/src/main/resources/db/migration/V026__integral_closure_archive.sql` (supersedes the V020 scaffold)
- [X] T097 [US10] Implement integral closure service in `backend/src/main/java/ec/edu/unl/sec/archive/application/IntegralClosureService.java`
- [X] T098 [US10] Implement archive and historical query service in `backend/src/main/java/ec/edu/unl/sec/archive/application/ArchiveService.java`
- [X] T099 [US10] Implement closure/archive endpoints in `backend/src/main/java/ec/edu/unl/sec/archive/api/ArchiveController.java`
- [X] T100 [US10] Implement closure blockers and historical UI in `frontend/src/app/features/archive/pages/process-archive.page.ts`
- [X] T101 [US10] Add backup/restore integrity journey in `tests/acceptance/archive-restore.spec.ts`
- [X] T102 [US10] Record P16 acceptance evidence in `tests/acceptance/results/d8-archive.md`
- [X] T124 [US10] Implement policy-specific closure preview/blockers and controlled reopening from documentary or integral closure in `backend/src/main/java/ec/edu/unl/sec/archive/application/IntegralClosureService.java` and `backend/src/main/java/ec/edu/unl/sec/archive/api/ArchiveController.java`; test retained exceptions and integrity failures in `backend/src/test/java/ec/edu/unl/sec/archive/application/ClosureReopeningPolicyTest.java` before T102

## Phase 13: Cross-cutting verification and documentation

- [ ] T103 [P] Update technical manuals for D1–D8 in `docs/technical/index.qmd`
- [ ] T104 [P] Update role-based user manuals for P01–P16 in `docs/user/index.qmd`
- [ ] T105 [P] Add accessibility journeys for new screens in `tests/e2e/accessibility-complete-workflows.spec.ts`
- [ ] T106 [P] Add performance datasets and paginated query checks in `tests/performance/complete-workflows.js`
- [ ] T107 Validate the merged OpenAPI contract with project scripts and Postman CLI, recording results in `tests/acceptance/results/contracts-p01-p16.md`
- [ ] T108 Run clean and incremental migration verification through V020 in `tests/acceptance/results/migrations-p01-p16.md`
- [ ] T109 Verify no secrets or institutional data in fixtures/results using `scripts/verify-synthetic-data.ps1`
- [ ] T110 Execute `specs/005-complete-evaluation-workflows/quickstart.md` and record results in `tests/acceptance/results/p01-p16-quickstart.md`
- [ ] T111 Complete human governance review for normative provenance, competencies and no-accreditation wording in `tests/acceptance/results/p01-p16-review-gate.md`
- [ ] T112 Finalize requirement-to-test-to-result mapping in `docs/testing/traceability.md`
- [ ] T125 [P] Automate the 48 AC-Pxx and 16 AC-X scenarios relevant to each accepted delivery with synthetic fixtures in `tests/acceptance/complete-workflows.spec.ts`; preserve PASS/FAIL/SKIPPED evidence in `tests/acceptance/results/process-acceptance.md` before T110
- [ ] T126 Verify the current C4/module diagrams and contract permissions against accepted code, and record unresolved differences in `docs/architecture/complete-workflows.md`; documentation of a planned module does not satisfy architecture tests or implementation acceptance

## Dependencies and delivery order

```text
Foundation → (D1 + D2) → D3 → D4 → D5 → D6 (MVP accepted) → D7 → D8
```

- D1 and D2 may proceed in parallel after Phase 2.
- D3 requires active careers and published models.
- D4 requires D3; D5 requires D4; D6 requires D2–D5.
- Stop and accept the documentary MVP after T067.
- D7 begins after the D6 acceptance checkpoint, requires accepted documentary inputs and is not required by D6.
- D8 requires findings/reports from D7 for the full path; archive infrastructure tests can begin earlier.

## Parallel opportunities

- Contract, domain and UI tests marked `[P]` can be written in parallel within each story.
- D1 and D2 can be implemented concurrently.
- After the P12 model stabilizes, P13 reporting UI and P15 domain scaffolding can proceed in different modules.
- Documentation, accessibility and performance work can proceed after each contract stabilizes.

T113 precedes T002 (both affect the common contract). T114 and T115 precede implementation of dependent stories. T116–T124 must finish before accepting their respective deliveries even though their stable IDs were appended later. T125 precedes final validation T110. Parallel markers do not authorize editing the same contract/file concurrently.

## Implementation strategy

1. Preserve completed work and introduce only forward migrations.
2. Implement and validate one delivery at a time.
3. Treat T067 as the MVP acceptance gate.
4. Do not begin automated academic calculations until their rule and strategy are validated.
5. Complete D7 and D8 without changing documentary metrics or historical snapshots.

