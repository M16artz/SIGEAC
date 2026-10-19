# Tareas: Sistema de Evaluación de Carreras

**Entrada**: artefactos de diseño organizados bajo `docs/`

**Prerrequisitos**: `docs/requirements/spec.md`, `docs/planning/plan.md`, `docs/architecture/research.md`, `docs/architecture/data-model.md`, `docs/technical/contracts/`, `docs/technical/quickstart.md`

**Pruebas**: obligatorias para contratos y reglas críticas según la constitución. En cada historia, escribir primero las pruebas y comprobar que fallan antes de implementar.

**Organización**: US1–US9 corresponden a I0–I8. El MVP completo requiere US1–US7; US8 introduce Strategy en I7 y US9 reserva Adapter para importaciones/integraciones de I8. Ambas extensiones son posteriores y no bloqueantes.

### Equivalencia con el paquete de tareas propuesto

Los IDs con guion recibidos en la propuesta se mapean a tareas canónicas existentes para conservar la numeración ejecutable T001–T133 y sus dependencias.

| ID propuesto | Incremento propuesto | Tarea canónica | Incremento efectivo |
|---|---|---|---|
| T-001 | I2 | T043 | I1; el esquema bloquea las pruebas de configuración de I2. |
| T-002 | I2 | T044 | I1; la jerarquía bloquea las pruebas de configuración de I2. |
| T-003 | I2 | T045 | I1; las fuentes y mecanismos bloquean las pruebas de configuración de I2. |
| T-004 | I2 | T042 | I2 |
| T-005 | I2 | T048 | I2 |
| T-006 | I3 | T054 | I2, conforme al incremento vigente de procesos. |
| T-007 | I4 | T066 | I3, conforme al incremento vigente de evidencias; los requisitos proceden del modelo asociado. |
| T-008 | I7 | T112 | I7 |
| T-009 | I7 | T113 | I7 |
| T-010 | I8/integración | T118–T119 | I8 |

## Fase 1: Preparación compartida

- [ ] T001 Crear el proyecto Maven Java 25/Spring Boot 4.1.x y fijar dependencias aprobadas en `backend/pom.xml`
- [ ] T002 [P] Crear el workspace Angular 22 con versiones bloqueadas, scripts de lint/test/build y accesibilidad en `frontend/package.json`, `frontend/angular.json` y `frontend/package-lock.json`
- [ ] T003 [P] Configurar EditorConfig y reglas de formato/análisis para Java y TypeScript en `.editorconfig`, `backend/pom.xml` y `frontend/eslint.config.js`
- [ ] T004 Crear paquetes `shared`, `audit`, `identityaccess`, `evaluationmodel`, `selfassessment`, `evidence`, `documentreview`, `academicevaluation`, `reporting`, `integration` y `filestorage` bajo `backend/src/main/java/ec/edu/unl/sec/`
- [ ] T005 [P] Crear estructura `core`, `shared` y features I0–I6 bajo `frontend/src/app/` con rutas lazy en `frontend/src/app/app.routes.ts`
- [ ] T006 [P] Configurar perfiles locales/prueba sin Docker y variables sin secretos en `backend/src/main/resources/application.yml`, `backend/src/test/resources/application-test.yml` y `backend/.env.example`
- [ ] T007 [P] Configurar herramientas OpenAPI/AsyncAPI lint y validación contra `docs/technical/contracts/` en `scripts/validate-contracts.ps1` y `scripts/validate-contracts.sh`
- [ ] T008 [P] Crear fixtures sintéticos base y prohibición de datos reales para pruebas en `tests/fixtures/pilot/README.md` y `tests/fixtures/pilot/manifest.json` (RNF-019)
- [ ] T009 Ajustar backend, frontend y documentación para ejecutar comandos reales conservando exclusivamente `workflow_dispatch` y sin despliegue en `.github/workflows/backend-ci.yml`, `.github/workflows/frontend-ci.yml` y `.github/workflows/documentation-ci.yml`; mantener `.github/workflows/android-ci.yml` como marcador manual sin convertir móvil en alcance del MVP
- [ ] T010 [P] Incorporar los artefactos SDD y ADR al sitio Quarto en `_quarto.yml` y `docs/technical/index.qmd` (FR-040)

---

## Fase 2: Fundamentos bloqueantes

**Propósito**: infraestructura común que bloquea todas las historias.

- [ ] T011 Crear migración inicial para extensiones, convenciones UUID/UTC y tabla de historial Flyway en `backend/src/main/resources/db/migration/V001__foundation.sql`
- [ ] T012 [P] Implementar tipos compartidos de identificador, reloj y bloqueo optimista en `backend/src/main/java/ec/edu/unl/sec/shared/domain/` (RNF-011)
- [ ] T013 [P] Implementar RFC 9457 con `code`, `correlationId` y detalles seguros en `backend/src/main/java/ec/edu/unl/sec/shared/api/ApiProblemHandler.java` (FR-039, RNF-005)
- [ ] T014 [P] Implementar filtro de correlación y sanitización de logs en `backend/src/main/java/ec/edu/unl/sec/shared/infrastructure/CorrelationFilter.java` y `backend/src/main/resources/logback-spring.xml` (RNF-013)
- [ ] T015 Crear migración y permisos append-only de auditoría en `backend/src/main/resources/db/migration/V002__audit.sql` (RNF-012: eventos inmutables para usuarios ordinarios)
- [ ] T016 Implementar puerto y servicio transaccional de auditoría en `backend/src/main/java/ec/edu/unl/sec/audit/application/AuditPort.java` y `backend/src/main/java/ec/edu/unl/sec/audit/infrastructure/JpaAuditAdapter.java`
- [ ] T017 [P] Implementar `FileStoragePort` y tipos de archivo sin adaptador concreto en `backend/src/main/java/ec/edu/unl/sec/filestorage/application/FileStoragePort.java` y `backend/src/main/java/ec/edu/unl/sec/filestorage/domain/StoredFile.java`
- [ ] T018 [P] Crear validación automática de dependencias api/application/domain/infrastructure y módulos en `backend/src/test/java/ec/edu/unl/sec/architecture/ModuleArchitectureTest.java` (RNF-017)
- [ ] T019 [P] Configurar pruebas con PostgreSQL local y Testcontainers opcional, sin H2, en `backend/src/test/java/ec/edu/unl/sec/support/PostgresTestSupport.java` y `backend/src/test/resources/application-test.yml`
- [ ] T020 [P] Crear cliente HTTP, manejo de Problem Details y propagación CSRF en `frontend/src/app/core/http/api-client.ts`, `frontend/src/app/core/http/problem.ts` y `frontend/src/app/core/http/csrf.interceptor.ts`
- [ ] T021 [P] Crear layout accesible con salto de contenido, foco visible y regiones etiquetadas en `frontend/src/app/core/layout/` y `frontend/src/styles.scss` (RNF-014)
- [ ] T022 Crear prueba que aplique todas las migraciones desde una base PostgreSQL limpia en `backend/src/test/java/ec/edu/unl/sec/migration/CleanDatabaseMigrationTest.java`
- [ ] T023 [P] Crear validación de contrato OpenAPI y errores comunes en `backend/src/test/java/ec/edu/unl/sec/contract/OpenApiContractTest.java`
- [ ] T024 [P] Crear comprobación de secretos/datos reales en fixtures y artefactos en `scripts/verify-synthetic-data.ps1` y `scripts/verify-synthetic-data.sh` (RNF-019)
- [ ] T025 Actualizar matriz inicial con IDs de pruebas y comandos fundacionales en `docs/testing/traceability.md`

**Punto de control**: migraciones, errores, auditoría, arquitectura, almacenamiento por puerto y harness de pruebas funcionan antes de historias.

---

## Fase 3: US1 — I0 Acceso y ámbitos autorizados (P0)

**Objetivo**: autenticar, autorizar por función/ámbito, impedir conflictos y auditar.

**Prueba independiente**: ejecutar matriz función×carrera×proceso×indicador; 100 % permitido dentro del ámbito y rechazado fuera de él sin filtración.

- [ ] T026 [P] [US1] Escribir pruebas de dominio para conflictos, excepciones temporales y prohibición de decisiones a cuentas técnicas en `backend/src/test/java/ec/edu/unl/sec/identityaccess/domain/FunctionConflictPolicyTest.java` (FR-003, FR-004)
- [ ] T027 [P] [US1] Escribir pruebas de seguridad para sesión, CSRF, expiración de 30 minutos, revocación y cinco intentos en 15 minutos en `backend/src/test/java/ec/edu/unl/sec/identityaccess/api/SessionSecurityTest.java` (RNF-001–RNF-003)
- [ ] T028 [P] [US1] Escribir matriz de autorización negativa por objeto en `backend/src/test/java/ec/edu/unl/sec/identityaccess/application/ScopedAuthorizationTest.java` (FR-002, SC-001)
- [ ] T029 [P] [US1] Escribir recorridos E2E de login y administración de asignaciones en `tests/e2e/i0-access.spec.ts`
- [ ] T030 [US1] Crear esquema de `user_account`, `session`, `process_role`, `scope_assignment`, `function_conflict_policy` y `function_exception` en `backend/src/main/resources/db/migration/V003__identity_access.sql`; aplicar literalmente username 3–100, excepción siempre temporal/justificada/autorizada y asignaciones idénticas no superpuestas
- [ ] T031 [US1] Implementar agregados y validaciones de I0 en `backend/src/main/java/ec/edu/unl/sec/identityaccess/domain/` (FR-001–FR-005)
- [ ] T032 [US1] Implementar repositorios JPA y consultas filtradas por ámbito en `backend/src/main/java/ec/edu/unl/sec/identityaccess/infrastructure/persistence/`
- [ ] T033 [US1] Implementar Argon2id, sesión opaca revocable, limitación de intentos y CSRF en `backend/src/main/java/ec/edu/unl/sec/identityaccess/infrastructure/security/` (RNF-001–RNF-003)
- [ ] T034 [US1] Implementar casos de uso de usuario, asignación, conflicto y excepción en `backend/src/main/java/ec/edu/unl/sec/identityaccess/application/`
- [ ] T035 [US1] Implementar `/sessions`, `/me`, `/users` y asignaciones según OpenAPI en `backend/src/main/java/ec/edu/unl/sec/identityaccess/api/`
- [ ] T036 [P] [US1] Implementar login, expiración y cierre de sesión accesibles en `frontend/src/app/features/access/session/`
- [ ] T037 [P] [US1] Implementar administración de usuarios/asignaciones y explicación de conflictos en `frontend/src/app/features/access/assignments/`
- [ ] T038 [US1] Emitir auditoría para sesión, autenticación fallida/limitada, permisos, asignaciones/excepciones, rechazos por función/ámbito, conflictos y transiciones u operaciones críticas fallidas desde `backend/src/main/java/ec/edu/unl/sec/identityaccess/application/` (FR-006)
- [ ] T039 [US1] Documentar configuración y recorrido I0 en `docs/technical/i0-access.qmd`, `docs/user/i0-access.qmd` y `docs/testing/traceability.md`

---

## Fase 4: US2 — I1 Modelo de evaluación versionado (P0)

**Objetivo**: crear, validar y publicar un catálogo versionado de modelos con jerarquías flexibles e indicadores configurables.

**Prueba independiente**: configurar el modelo general 2024 y estructuras sintéticas representativas de Medicina, Enfermería, Odontología y Derecho; publicar v1, rechazar su modificación, crear v2 relacionada y comprobar que ningún indicador se combina entre modelos.

- [ ] T040 [P] [US2] Escribir pruebas de dominio para jerarquía flexible, unicidad por versión, separación general/específica, aprobación e inmutabilidad en `backend/src/test/java/ec/edu/unl/sec/evaluationmodel/domain/ModelVersionTest.java` (FR-007–FR-010, FR-042; RF-051, RF-052, RF-055, RF-057)
- [ ] T041 [P] [US2] Escribir pruebas contractuales de listado, detalle, versiones y publicación que exijan organismo emisor, denominación, fecha, documento fuente y, por indicador, fuentes, período, naturaleza y mecanismo en `backend/src/test/java/ec/edu/unl/sec/evaluationmodel/api/ModelContractTest.java` (FR-008; RF-051, RF-054)
- [ ] T042 [P] [US2] Registrar fixture del modelo general 2024 y probar publicación/sucesión sin mezcla de indicadores en `tests/fixtures/models/general-2024/` y `tests/e2e/i1-model-versioning.spec.ts` (FR-009, FR-042; T-004)
- [ ] T043 [US2] Definir el esquema relacional de catálogo, versiones, nodos jerárquicos, indicadores, fuentes, períodos, escalas, mecanismos y fórmulas en `backend/src/main/resources/db/migration/V004__evaluation_models.sql`; aplicar literalmente `(model_id, version_number)` único y pertenencia de cada nodo/indicador a una sola versión (FR-007–FR-009, FR-042; T-001)
- [ ] T044 [US2] Implementar `EvaluationModel`, `ModelVersion` y una jerarquía arbitraria ordenada de nodos tipados, sin clases por carrera, en `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/domain/` (FR-007; T-002)
- [ ] T045 [US2] Implementar fuentes de información, período, naturaleza cualitativa/cuantitativa, mecanismo de valoración y validador de publicación append-only en `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/application/PublishModelVersionService.java` (FR-008, FR-009; T-003)
- [ ] T046 [US2] Implementar repositorios y mapeos inmutables en `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/infrastructure/persistence/`
- [ ] T047 [US2] Implementar endpoints de listado/detalle de modelos y versiones, creación y publicación conforme a OpenAPI, auditando publicaciones y rechazos, en `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/api/`
- [ ] T048 [P] [US2] Probar configuraciones sintéticas representativas de Medicina, Enfermería, Odontología y Derecho, verificando estructuras diferentes sin exigir todos sus cálculos académicos, en `tests/fixtures/models/careers/` y `backend/src/test/java/ec/edu/unl/sec/evaluationmodel/application/RepresentativeModelConfigurationTest.java` (FR-007, FR-008, FR-042; T-005)
- [ ] T049 [P] [US2] Implementar editor jerárquico, consulta y comparación de versiones con errores de validación y control ETag en `frontend/src/app/features/models/editor/` y `frontend/src/app/features/models/versions/`
- [ ] T050 [US2] Documentar I1 y actualizar trazabilidad en `docs/technical/i1-models.qmd`, `docs/user/i1-models.qmd` y `docs/testing/traceability.md`

---

## Fase 5: US3 — I2 Proceso y asignaciones (P0)

**Objetivo**: abrir proceso sobre modelo publicado y asignar funciones por indicador.

**Prueba independiente**: activar proceso v1, asignar participantes y comprobar que publicar v2 no cambia su referencia.

- [ ] T051 [P] [US3] Escribir pruebas de estados `DRAFT→ACTIVE→CLOSING→CLOSED` y reapertura autorizada en `backend/src/test/java/ec/edu/unl/sec/selfassessment/domain/ProcessStateTest.java` (FR-014, FR-015)
- [ ] T052 [P] [US3] Escribir pruebas de pertenencia del indicador, asociación histórica e inmutable de la versión del modelo y conflictos de asignación en `backend/src/test/java/ec/edu/unl/sec/selfassessment/application/ProcessAssignmentTest.java` (FR-011–FR-013; RF-053)
- [ ] T053 [P] [US3] Escribir recorrido E2E de creación, asignación y activación en `tests/e2e/i2-process.spec.ts`
- [ ] T054 [US3] Crear esquema de `career`, `self_assessment_process`, `process_assignment` y `reopening_decision` en `backend/src/main/resources/db/migration/V005__self_assessment.sql`; aplicar literalmente una versión publicada por proceso, asociación inmutable desde el inicio e indicador perteneciente a esa versión (FR-011, FR-012; RF-053; T-006)
- [ ] T055 [US3] Implementar agregados de carrera, proceso, asignación y reapertura en `backend/src/main/java/ec/edu/unl/sec/selfassessment/domain/`
- [ ] T056 [US3] Implementar políticas de transición, asignación y autorización por ámbito en `backend/src/main/java/ec/edu/unl/sec/selfassessment/application/`
- [ ] T057 [US3] Implementar persistencia y bloqueo optimista del proceso en `backend/src/main/java/ec/edu/unl/sec/selfassessment/infrastructure/persistence/`
- [ ] T058 [US3] Implementar endpoints de listado/detalle de procesos e indicadores, activación, asignaciones y reapertura, auditando transiciones y rechazos, en `backend/src/main/java/ec/edu/unl/sec/selfassessment/api/`
- [ ] T059 [P] [US3] Implementar asistente accesible para crear/activar proceso en `frontend/src/app/features/processes/setup/`
- [ ] T060 [P] [US3] Implementar matriz de asignaciones por indicador en `frontend/src/app/features/processes/assignments/`
- [ ] T061 [US3] Documentar I2 y actualizar trazabilidad en `docs/technical/i2-processes.qmd`, `docs/user/i2-processes.qmd` y `docs/testing/traceability.md`

---

## Fase 6: US4 — I3 Recopilación y remisión de evidencias (P0)

**Objetivo**: recopilar evidencia, almacenar archivos fuera de PostgreSQL y remitir versiones inmutables.

**Prueba independiente**: cargar archivo sintético, remitir v1, impedir sobrescritura y crear v2 sucesora.

- [ ] T062 [P] [US4] Escribir pruebas de dominio de remisión, sucesión e inmutabilidad en `backend/src/test/java/ec/edu/unl/sec/evidence/domain/EvidenceVersionTest.java` (FR-016, FR-019, FR-020)
- [ ] T063 [P] [US4] Escribir pruebas de archivo para tamaño 1–26 214 400 bytes, SHA-256 de 64 hexadecimales, tipo detectado y fallas de promoción en `backend/src/test/java/ec/edu/unl/sec/filestorage/LocalFileStorageTest.java` (RNF-004, RNF-010)
- [ ] T064 [P] [US4] Escribir pruebas contractuales de listado/detalle, historial, descarga autorizada, multipart y errores 403/413/422 en `backend/src/test/java/ec/edu/unl/sec/evidence/api/EvidenceContractTest.java`
- [ ] T065 [P] [US4] Escribir recorrido E2E de borrador, carga, remisión y corrección en `tests/e2e/i3-evidence.spec.ts`
- [ ] T066 [US4] Crear esquema de evidencia sin estados documentales persistidos, indicador, versión, archivo y referencia en `backend/src/main/resources/db/migration/V006__evidence.sql`; validar los requisitos documentales y fuentes declarados por el indicador de la versión asociada al proceso, además de versión remitida inmutable, predecesora de la misma evidencia y al menos un archivo o referencia antes de remitir (FR-008, FR-016–FR-020; RF-054; T-007)
- [ ] T067 [US4] Implementar agregados y estados de evidencia/versiones en `backend/src/main/java/ec/edu/unl/sec/evidence/domain/`
- [ ] T068 [US4] Implementar adaptador local con claves opacas, staging, SHA-256, promoción y reconciliación en `backend/src/main/java/ec/edu/unl/sec/filestorage/infrastructure/local/`
- [ ] T069 [US4] Implementar casos de uso de borrador, archivo, referencia, remisión y sucesión en `backend/src/main/java/ec/edu/unl/sec/evidence/application/`
- [ ] T070 [US4] Implementar persistencia transaccional y consultas por ámbito en `backend/src/main/java/ec/edu/unl/sec/evidence/infrastructure/persistence/`
- [ ] T071 [US4] Implementar endpoints de listado/detalle de evidencia, historial, carga/descarga autorizada y remisión, auditando remisiones y fallos de carga/descarga, en `backend/src/main/java/ec/edu/unl/sec/evidence/api/`
- [ ] T072 [P] [US4] Implementar formulario accesible de evidencia y metadatos en `frontend/src/app/features/evidence/editor/`
- [ ] T073 [P] [US4] Implementar carga con progreso/errores recuperables e historial de versiones en `frontend/src/app/features/evidence/files/` y `frontend/src/app/features/evidence/history/`
- [ ] T074 [US4] Documentar I3, recuperación de archivos y trazabilidad en `docs/technical/i3-evidence.qmd`, `docs/user/i3-evidence.qmd` y `docs/testing/traceability.md`

---

## Fase 7: US5 — I4 Revisión documental (P0)

**Objetivo**: decidir suficiencia documental sin inferir cumplimiento académico.

**Prueba independiente**: aceptar/observar/rechazar con justificación y rectificar sin sobrescribir ni valorar.

- [ ] T075 [P] [US5] Escribir pruebas de separación que demuestren que aceptar no crea nivel académico en `backend/src/test/java/ec/edu/unl/sec/documentreview/domain/DocumentReviewSeparationTest.java` (FR-021–FR-024, SC-005)
- [ ] T076 [P] [US5] Escribir pruebas de bandeja/listado, historial, revisor por ámbito y conflicto con remitente en `backend/src/test/java/ec/edu/unl/sec/documentreview/application/DocumentReviewAuthorizationTest.java`
- [ ] T077 [P] [US5] Escribir recorrido E2E de bandeja, decisión y rectificación en `tests/e2e/i4-document-review.spec.ts`
- [ ] T078 [US5] Crear esquema append-only `document_review_decision` en `backend/src/main/resources/db/migration/V007__document_review.sql`; aplicar literalmente justificación de 10–4000 caracteres, evidencia remitida y ausencia de escala o nivel académico
- [ ] T079 [US5] Implementar agregado y política de rectificación en `backend/src/main/java/ec/edu/unl/sec/documentreview/domain/`
- [ ] T080 [US5] Implementar caso de uso autorizado y puerto público de elegibilidad documental en `backend/src/main/java/ec/edu/unl/sec/documentreview/application/`
- [ ] T081 [US5] Implementar persistencia y endpoints de bandeja, historial, detalle y decisión conforme OpenAPI, auditando decisiones y rechazos, en `backend/src/main/java/ec/edu/unl/sec/documentreview/infrastructure/persistence/` y `backend/src/main/java/ec/edu/unl/sec/documentreview/api/`
- [ ] T082 [P] [US5] Implementar bandeja, visor seguro y formulario accesible de decisión/historial en `frontend/src/app/features/reviews/`
- [ ] T083 [US5] Documentar I4 y actualizar trazabilidad en `docs/technical/i4-document-review.qmd`, `docs/user/i4-document-review.qmd` y `docs/testing/traceability.md`

---

## Fase 8: US6 — I5 Valoración académica (P0)

**Objetivo**: valorar indicadores solo con evidencia aceptada y preservar revisiones.

**Prueba independiente**: rechazar evidencia no aceptada, finalizar con sustentos aceptados y rectificar append-only.

- [ ] T084 [P] [US6] Escribir pruebas de dominio y consulta para escala, sustentos aceptados, opción perteneciente al modelo, bandeja e historial de revisiones en `backend/src/test/java/ec/edu/unl/sec/academicevaluation/domain/AcademicEvaluationTest.java` (FR-025–FR-028)
- [ ] T085 [P] [US6] Escribir pruebas de autorización evaluador×ámbito y no modificación documental en `backend/src/test/java/ec/edu/unl/sec/academicevaluation/application/AcademicEvaluationAuthorizationTest.java`
- [ ] T086 [P] [US6] Escribir recorrido E2E de selección de sustentos, valoración y rectificación en `tests/e2e/i5-academic-evaluation.spec.ts`
- [ ] T087 [US6] Crear esquema `academic_evaluation` y `evaluation_evidence` en `backend/src/main/resources/db/migration/V008__academic_evaluation.sql`; aplicar literalmente revisión única por proceso+indicador, sustentos con decisión ACCEPTED vigente y final append-only
- [ ] T088 [US6] Implementar agregado y validación de escala/sustentos en `backend/src/main/java/ec/edu/unl/sec/academicevaluation/domain/`
- [ ] T089 [US6] Implementar casos de uso de borrador, finalización y rectificación en `backend/src/main/java/ec/edu/unl/sec/academicevaluation/application/`
- [ ] T090 [US6] Implementar persistencia y endpoints de bandeja, detalle, historial y decisión conforme OpenAPI, auditando finalizaciones y rechazos, en `backend/src/main/java/ec/edu/unl/sec/academicevaluation/infrastructure/persistence/` y `backend/src/main/java/ec/edu/unl/sec/academicevaluation/api/`
- [ ] T091 [P] [US6] Implementar área accesible de valoración con sustentos y escala configurada en `frontend/src/app/features/evaluations/`
- [ ] T092 [US6] Documentar I5 y actualizar trazabilidad en `docs/technical/i5-academic-evaluation.qmd`, `docs/user/i5-academic-evaluation.qmd` y `docs/testing/traceability.md`

---

## Fase 9: US7 — I6 Seguimiento, cierre y reporte (P0, MVP completo)

**Objetivo**: avance reproducible, cierre seguro e informe interno trazable.

**Prueba independiente**: contrastar porcentajes, bloquear cierre incompleto y rastrear informe cerrado hasta fuentes.

- [ ] T093 [P] [US7] Escribir pruebas deterministas de fórmula y tres medidas separadas en `backend/src/test/java/ec/edu/unl/sec/reporting/domain/ProgressCalculatorTest.java` (FR-029, SC-006)
- [ ] T094 [P] [US7] Escribir pruebas de carrera cierre-vs-observación y prerrequisitos con bloqueo en `backend/src/test/java/ec/edu/unl/sec/reporting/application/CloseProcessConcurrencyTest.java` (FR-031, RNF-011)
- [ ] T095 [P] [US7] Escribir prueba de trazabilidad completa y contrato de estado/descarga autorizada del informe desde valoración a modelo/asignación/decisión/evidencia en `backend/src/test/java/ec/edu/unl/sec/reporting/application/ClosedReportTraceabilityTest.java` (FR-030–FR-034, SC-004)
- [ ] T096 [P] [US7] Escribir recorrido E2E de tablero, bloqueo, cierre e informe en `tests/e2e/i6-reporting.spec.ts`
- [ ] T097 [P] [US7] Crear pruebas de accesibilidad por teclado y axe para recorridos I0–I6 en `tests/e2e/accessibility.spec.ts` (RNF-014)
- [ ] T098 [US7] Crear esquema `progress_snapshot`, `closure_snapshot` y `report_export` en `backend/src/main/resources/db/migration/V009__reporting.sql`; aplicar literalmente conteos no negativos, porcentajes derivados, mismos datos+fórmula=mismo hash y cierre inmutable
- [ ] T099 [US7] Implementar cálculo de avance versionado en `backend/src/main/java/ec/edu/unl/sec/reporting/domain/ProgressCalculator.java`
- [ ] T100 [US7] Implementar consultas agregadas navegables sin acceder a repositorios internos en `backend/src/main/java/ec/edu/unl/sec/reporting/application/ProgressQueryService.java`
- [ ] T101 [US7] Implementar cierre transaccional, revalidación y hash del resumen en `backend/src/main/java/ec/edu/unl/sec/reporting/application/CloseProcessService.java`
- [ ] T102 [US7] Implementar generador de informe con declaración de no acreditación y filtro de ámbito en `backend/src/main/java/ec/edu/unl/sec/reporting/application/ReportExportService.java`
- [ ] T103 [US7] Implementar persistencia y endpoints de avance, cierre, listado/estado y descarga autorizada de informes, auditando cierres, exportaciones, descargas y rechazos, en `backend/src/main/java/ec/edu/unl/sec/reporting/infrastructure/persistence/` y `backend/src/main/java/ec/edu/unl/sec/reporting/api/`
- [ ] T104 [P] [US7] Implementar tablero accesible con fórmula/fecha y navegación a sustentos en `frontend/src/app/features/reporting/dashboard/`
- [ ] T105 [P] [US7] Implementar cierre, bloqueos explicados y descarga autorizada en `frontend/src/app/features/reporting/closure/`
- [ ] T106 [P] [US7] Crear dataset RNF-008 y prueba k6 p95≤2 s / carga 25 MiB p95≤15 s en `tests/performance/pilot-data.js` y `tests/performance/mvp-thresholds.js` (RNF-006–RNF-008, SC-008)
- [ ] T107 [P] [US7] Diseñar y ejecutar prueba moderada con al menos 20 participantes sintéticos de los cinco perfiles, una tarea representativa por función y registro de inicio, fin, ayuda y primer intento en `tests/usability/i0-i6-protocol.md` y `tests/acceptance/results/usability.md` (SC-007)
- [ ] T108 [US7] Ejecutar quickstart I0–I6 sin componentes I7/I8 y registrar evidencia en `tests/acceptance/results/mvp-validation.md` (SC-003, SC-010)
- [ ] T109 [US7] Documentar I6, resultados de rendimiento/usabilidad y actualizar trazabilidad en `docs/technical/i6-reporting.qmd`, `docs/user/i6-reporting.qmd` y `docs/testing/traceability.md`

---

## Fase 10: US8 — I7 Estrategias de valoración (P1, fuera del MVP)

**Objetivo**: seleccionar algoritmos de valoración distintos mediante una interfaz común y ejecutar solo implementaciones registradas, versionadas y validadas.

**Prueba independiente**: registrar una estrategia sintética validada, resolverla por clave/versión, rechazar estrategias ausentes o no validadas y ejecutar I0–I6 con el registro de estrategias deshabilitado.

- [ ] T110 [P] [US8] Escribir pruebas de resolución por clave/versión, compatibilidad de mecanismo y rechazo de estrategias ausentes o no validadas en `backend/src/test/java/ec/edu/unl/sec/academicevaluation/strategy/EvaluationStrategyRegistryTest.java` (FR-041; RF-056)
- [ ] T111 [P] [US8] Escribir prueba de arquitectura que impida dependencias desde I0–I6 hacia implementaciones de estrategia I7 y prohíba Adapter dentro de la selección de modelos en `backend/src/test/java/ec/edu/unl/sec/architecture/ExtensionIsolationTest.java`
- [ ] T112 [US8] Diseñar `EvaluationStrategy`, entrada/salida canónica y Factory/Registry por clave y versión en `backend/src/main/java/ec/edu/unl/sec/academicevaluation/strategy/EvaluationStrategy.java` y `backend/src/main/java/ec/edu/unl/sec/academicevaluation/strategy/EvaluationStrategyRegistry.java` (FR-041; T-008)
- [ ] T113 [US8] Implementar las estrategias de valoración validadas y su registro explícito, sin interpretar scripts o expresiones arbitrarias, en `backend/src/main/java/ec/edu/unl/sec/academicevaluation/strategy/validated/` (FR-041; T-009)
- [ ] T114 [P] [US8] Crear fixtures de mecanismos compatibles/incompatibles y recorrido de aceptación del registro en `tests/fixtures/evaluation-strategies/` y `tests/acceptance/i7-evaluation-strategies.ps1`
- [ ] T115 [US8] Documentar I7, registro/validación y actualizar trazabilidad en `docs/technical/i7-evaluation-strategies.qmd` y `docs/testing/traceability.md`

---

## Fase 11: US9 — I8 Importaciones e integraciones (P2, fuera del MVP)

**Objetivo**: traducir formatos o servicios externos mediante adaptadores específicos, validar paquetes completos y conservar eventos complementarios sin transferir autoridad ni disponibilidad.

**Prueba independiente**: importar un paquete sintético mediante su adaptador, rechazar atómicamente formatos inválidos o no soportados y comprobar que una falla de integración o notificación no cambia el agregado fuente.

- [ ] T116 [P] [US9] Escribir pruebas contractuales de validación completa, formato no soportado y ausencia de cambios parciales en `backend/src/test/java/ec/edu/unl/sec/integration/api/ExchangeContractTest.java` (FR-035, FR-036)
- [ ] T117 [P] [US9] Escribir pruebas de esquema AsyncAPI, minimización de payload y falla de publicador/consumidor sin cambio de estado en `backend/src/test/java/ec/edu/unl/sec/integration/events/NotificationEventContractTest.java` y `backend/src/test/java/ec/edu/unl/sec/integration/events/NotificationFailureIsolationTest.java` (FR-037, FR-038, RNF-005, RNF-009)
- [ ] T118 [US9] Crear esquema `exchange_job` y `notification_event` con procedencia, versión, resultado, errores, actor y payload mínimo en `backend/src/main/resources/db/migration/V011__integrations.sql` (FR-035–FR-038; T-010)
- [ ] T119 [US9] Implementar el contrato interno de importación y adaptadores específicos solo para formatos necesarios en `backend/src/main/java/ec/edu/unl/sec/integration/imports/`; cada adaptador debe traducir al modelo canónico antes de aplicar cambios (FR-035, FR-036; T-010)
- [ ] T120 [P] [US9] Implementar puerto/adaptador inerte de eventos y crear fixtures de importación y consumidor desacoplado en `backend/src/main/java/ec/edu/unl/sec/integration/events/`, `tests/fixtures/exchange/v1/` y `tests/acceptance/i8-integration/`
- [ ] T121 [US9] Documentar I8, criterios para crear adaptadores y actualizar trazabilidad en `docs/technical/i8-integrations.qmd` y `docs/testing/traceability.md`

---

## Fase 12: Pulido y preocupaciones transversales

- [ ] T122 [P] Ejecutar y corregir pruebas ArchUnit de todas las fronteras modulares en `backend/src/test/java/ec/edu/unl/sec/architecture/`
- [ ] T123 [P] Ejecutar validación OpenAPI/AsyncAPI y documentar compatibilidad en `tests/acceptance/results/contracts.md`
- [ ] T124 Validar migraciones V001–V011 desde cero y desde cada incremento previo en `backend/src/test/java/ec/edu/unl/sec/migration/IncrementalMigrationTest.java`
- [ ] T125 Documentar respaldo/restauración de PostgreSQL y archivos con verificación SHA-256 en `docs/technical/backup-restore.qmd`
- [ ] T126 [P] Ejecutar matriz de seguridad completa y guardar resumen sanitizado en `tests/acceptance/results/security-matrix.md` (SC-001)
- [ ] T127 [P] Ejecutar pruebas de rendimiento RNF-006–RNF-008 y guardar percentiles en `tests/acceptance/results/performance.md`
- [ ] T128 [P] Ejecutar pruebas de accesibilidad/compatibilidad RNF-014–RNF-015 y guardar resultados en `tests/acceptance/results/accessibility.md`
- [ ] T129 Validar que no existen secretos ni datos reales y guardar resultado en `tests/acceptance/results/synthetic-data.md` (RNF-019)
- [ ] T130 [P] Completar guía técnica y manual de usuario I0–I6 en `docs/technical/index.qmd` y `docs/user/index.qmd`
- [ ] T131 Reconciliar FR-001–FR-042, alias RF-051–RF-057, RNF-001–RNF-019 y SC-001–SC-010 con pruebas/resultados en `docs/testing/traceability.md`
- [ ] T132 Ejecutar todos los escenarios de `docs/technical/quickstart.md` y registrar el acta final en `tests/acceptance/results/quickstart.md`
- [ ] T133 Revisar constitución, CL-001–CL-010 y puertas humanas antes de aceptar el incremento en `tests/acceptance/results/review-gate.md`

---

## Dependencias y orden de ejecución

### Dependencias de fases

- Fase 1 → Fase 2 → US1.
- US2 depende de fundamentos y puede desarrollarse en paralelo con US1, pero US3 necesita US1 (autorización) y US2 (modelo publicado).
- US4 depende de US3; US5 depende de US4; US6 depende de US5; US7 depende de US1–US6.
- US8 y US9 comienzan únicamente después de aceptar US7 y nunca bloquean el MVP.
- Pulido final depende de las historias incluidas en el alcance de entrega.

### Grafo de historias

```text
Fundamentos
├── US1 (I0) ─┐
└── US2 (I1) ─┴→ US3 (I2) → US4 (I3) → US5 (I4) → US6 (I5) → US7 (I6 / MVP)
                                                               ├→ US8 (I7)
                                                               └→ US9 (I8)
```

### Oportunidades paralelas

- T002–T003, T005–T008 y T010 se ejecutan en archivos distintos.
- T012–T014 y T017–T024 pueden dividirse después de T011/T015 según dependencia indicada.
- En cada historia, las pruebas `[P]` pueden escribirse en paralelo antes de la implementación.
- Frontend y documentación `[P]` pueden avanzar cuando el contrato y los casos de uso de la historia estén estables.
- US1 y US2 pueden avanzar en paralelo tras fundamentos; el resto sigue el grafo por dependencia de dominio.

## Estrategia de entrega

### Primer recorrido demostrable

Fases 1–2 + US1 ofrecen acceso seguro y ámbitos, pero no constituyen el MVP del producto académico. La primera demostración de dominio ocurre al completar US1–US4; la separación completa se demuestra en US5–US6.

### MVP constitucional

Completar Fases 1–2 y US1–US7, detenerse y validar quickstart, seguridad, rendimiento, accesibilidad, migraciones, trazabilidad y documentación. No iniciar US8/US9 para declarar completo I0–I6.

### Extensiones

US8 y US9 se programan como entregas independientes después de validar el MVP. US8 requiere validar CL-005 y solo introduce Strategy para algoritmos realmente distintos; US9 requiere validar CL-010 y solo introduce Adapter ante una fuente externa concreta. Sus fallas o ausencia no pueden romper pruebas del MVP.

## Reglas de ejecución

- Una tarea no se marca completa hasta que sus pruebas asociadas pasen y la trazabilidad se actualice.
- Todo cambio de contrato, seguridad, modelo transversal o arquitectura requiere revisar los ADR.
- Los workflows permanecen manuales; no añadir `push`, `pull_request`, `schedule` ni despliegue.
- Ninguna tarea puede introducir datos institucionales reales o convertir valores sintéticos CL en normativa oficial.
