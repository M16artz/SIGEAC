# Matriz de trazabilidad inicial

**Nota de estado**: las tablas de evidencia histórica conservan referencias documentales previas. Esta actualización no volvió a ejecutar esas pruebas ni certifica los resultados de D1–D8.

## Evidencia ejecutada D1 — 2026-10-21

La [entrega técnica D1](../../tests/acceptance/results/d1-careers.md) sí ejecuta
regresión backend, Angular y recorridos API/navegador sobre PostgreSQL y SQLite.
No extiende estos resultados a D2–D8 ni a validación institucional.

| Requisito / proceso | Tareas 005 | Pruebas y resultado |
|---|---|---|
| FR-043 / P02 | T013–T020 | `CareerTest`, `CareerAdministrationServiceTest`, `CareerControllerTest`: código, unidad, vigencias, estado, permisos y revisiones. |
| FR-043 / RNF-003, RNF-005 | T013, T017–T020 | `career-catalog.spec.ts`: CSRF, 404 ajeno/desconocido y totales filtrados, tanto en SQLite como en PostgreSQL. |
| RNF-011, RNF-012 / historial | T014–T018 | `CareerCatalogMigrationTest`: UUID y revisiones anteriores conservados; triggers rechazan mutaciones históricas. |
| RNF-016 / arranque y migración | T005, T015 | `CareerCatalogStartupTest`, `CareerCatalogMigrationTest`, `CleanDatabaseMigrationTest`: desde V007 y desde cero, Flyway antes de JPA. |
| Compatibilidad de interfaces | T113; avance D1 de T002 | `check-contract-compatibility.mjs` y lints de contratos: 12 operaciones D1 compartidas sin colisiones. |
| FR-048, prerrequisito de carrera activa | T017; avance D1 de D3 | Recorrido API rechaza crear proceso sobre carrera desactivada; no declara completo el gobierno de procesos. |

## Avance interno D2 — 2026-10-21

Las [bases D2](../../tests/acceptance/results/d2-model-foundations.md) cubren
jerarquía flexible y valores de fuentes/indicadores. La [continuación de
publicación](../../tests/acceptance/results/d2-model-publication.md) verifica
T006/T022/T023/T025/T026: cinco configuraciones **sintéticas**, publicación por
indicador y bloqueo de automatización no validada. Regresión: 145 aprobados,
0 fallos, 8 omitidos (PostgreSQL). No acredita aceptación D2, configuración
normativa corroborada, API v2 ni migración V009.

| Requisito / proceso | Tareas 005 | Evidencia |
|---|---|---|
| FR-042, FR-044 / P03–P04; US2.1 | T021, T023, T025 | `FlexibleModelHierarchyTest`, `FiveModelConfigurationTest`: tipos, profundidades, indicador directo y cinco configuraciones sin motores por carrera. |
| FR-044–FR-047; US2.2–US2.3; AC-X10 | T022, T025, T026 | `RequiredSourceTest`, `CompleteModelPublicationTest`, `ValuationRuleTest`: pertenencia, cardinalidad, unidades, referencias, umbrales y automatización solo con regla/estrategia validadas. |
| RNF-011 / versiones; US2.4 | T021, T022, T025 | `FlexibleModelPersistenceTest`, `ModelComparisonServiceTest`, `CompleteModelPublicationTest`: JSON/configuración exacta conservados; cambios de método/escala; inmutabilidad/sucesión y conflicto al guardar con 412 sin sobrescritura. |
| RNF-019 / fuentes sintéticas | T006, T023 | `FiveModelConfigurationTest`: manifiesto con vigencia y corroboración pendientes; cuatro modalidades, unidades y umbrales propios. T114/T116 siguen pendientes. |
| Compatibilidad v1 | Avance T002; T025 | `EvaluationModelControllerTest`, `LegacyModelCompatibilityTest`: DTO legado y rechazo de estructuras/configuraciones v2 antes de mutar. |

## Evidencia ejecutada D5 — 2026-10-24

La [entrega técnica D5](../../tests/acceptance/results/d5-document-review.md) ejecuta
la revisión documental y subsanación P09–P10 sobre backend real (SQLite descartable)
y Angular, con el recorrido `tests/acceptance/document-review-correction.spec.ts`
en verde: v1 REJECTED fundada, corrección P10 con sucesor enlazado y 409
`SUCCESSOR_EXISTS` al repetir, v2 ACCEPTED sin herencia previa. Regresión backend:
326 pruebas, 0 fallos, 0 errores, 10 omitidas (opt-in PostgreSQL). No acredita
validación con PostgreSQL real ni aceptación institucional.

| Requisito / proceso | Tareas 005 | Pruebas y resultado |
|---|---|---|
| FR-021–FR-023 / P09 | T050–T053, T119 | `CompleteDocumentReviewTest`, `DocumentReviewControllerTest`, `DocumentReviewPersistenceTest`: máquina de decisiones por cobertura, notas fundadas, inbox append-only y 412/428/409/422 correctos. |
| FR-024 / P10 | T054, T056 | Recorrido de aceptación: corrección obsoleta 412, subsanación 201 con Location, repetición 409 `SUCCESSOR_EXISTS`, sucesor hereda coberturas y se remite; v2 ACCEPTED sin heredar la v1. |
| FR-055 / US5 | T050, T053, T056, T057 | `CoverageReviewProjectionTest` + recorrido: sin valoración académica creada por decisiones documentales. |
| RNF-012, RNF-018 / auditoría | T119 | `DocumentReviewPersistenceTest` pares sqlite/postgres; migración V015 con UNIQUE de sucesor y nota nullable conforme al dominio. |

## Matriz canónica P01–P16

| Entrega | Procesos | Requisitos | Historias | Tareas | Validación |
|---|---|---|---|---|---|
| D1 | P01–P02 | FR-001–FR-006, FR-043 | US1 + acceso existente | T007–T020 | carrera única, desactivación, ámbito |
| D2 | P03–P04 | FR-007–FR-010, FR-041–FR-047 | US2 | T021–T030 | cinco modelos, jerarquía, fuentes, reglas |
| D3 | P05–P06 | FR-011–FR-015, FR-048–FR-050, FR-067 | US3 | T031–T039 | autorización, suspensión, planificación |
| D4 | P07–P08 | FR-016–FR-020, FR-051–FR-052, FR-069 | US4 | T040–T049 | cobertura explícita e inmutabilidad |
| D5 | P09–P10 | FR-021–FR-024, FR-055 | US5 | T050–T057 | decisión, sucesión y separación |
| D6 | P11, P16 básico | FR-029–FR-034, FR-053–FR-054, FR-064, FR-068 | US6 | T058–T067 | 75/50, cero fuentes, MVP independiente |
| D7 | P12–P14 | FR-025–FR-028, FR-056–FR-061 | US7–US8 | T068–T085 | manual, cálculo, hallazgos, informe |
| D8 | P15–P16 | FR-062–FR-066 | US9–US10 | T086–T102 | mejora, cierre, archivo/restauración |
| Transversal | P01–P16 | FR-039–FR-040, FR-070, RNF-001–RNF-019 | Todas | T103–T112 | contratos, migraciones, seguridad y revisión humana |

Esta matriz prevalece sobre la tabla histórica siguiente cuando difieran el MVP,
la relación evidencia–fuente o los identificadores FR-041–FR-070.

| Incremento | Historia | Requisitos principales | Contrato | Tareas | Validación |
|---|---|---|---|---|---|
| I0 | US1 | FR-001–FR-006; RNF-001–RNF-003, RNF-005, RNF-012–RNF-013, RNF-019 | `/sessions`, `/me`, `/users`, `/users/{userId}/assignments`, `/conflict-exceptions`, `/audit-events` | T001–T038 | Quickstart I0; matriz función–ámbito–operación; aceptación Playwright |
| I1 | US1–US5 | FR-007–FR-010, FR-008a; RNF-011–RNF-012, RNF-018–RNF-019 | `/models`, `/models/{modelId}/versions`, `/model-versions/{versionId}`, `publish`, `successors`, `compare`, `/model-audit-events` | `specs/002-version-evaluation-model/tasks.md` T001–T036 | Fixture sintético general-2024; validadores, inmutabilidad, sucesión y comparación |
| I2 | US1–US5 | FR-011–FR-015, FR-013a; RNF-011–RNF-012, RNF-018–RNF-019 | `/processes`, detalle, `activate`, `transitions`, `assignments`, `reopen`, auditoría | `specs/003-manage-self-assessment/tasks.md` T001–T030 | Modelo v1 fijado, matriz de estados/asignaciones y reapertura |
| I3 | US4 | FR-016–FR-020; RNF-004–RNF-005, RNF-007, RNF-010–RNF-012 | GET/POST `/evidences`, detalle, historial, carga/descarga de archivos, `submit` | T061–T073 | Quickstart I3; atomicidad e integridad |
| I4 | US5 | FR-021–FR-024; RNF-012, RNF-018 | `/document-review-inbox`, GET/POST `/evidence-versions/{versionId}/document-reviews` | T074–T082 | Quickstart I4; separación documental |
| I5 | US6 | FR-025–FR-028; RNF-012, RNF-018 | `/academic-evaluation-inbox`, GET/POST historial por indicador | T083–T091 | Quickstart I5; sustentos aceptados |
| I6 | US7 | FR-029–FR-034; RNF-006, RNF-008–RNF-009, RNF-014–RNF-015, RNF-018 | `progress`, `close`, listado/estado/descarga de `reports` | T092–T108 | Quickstart I6; reproducibilidad, rendimiento, usabilidad y accesibilidad |
| I7 histórico | US8 | FR-041; RNF-009, RNF-016 | `EvaluationStrategy` y Factory/Registry internos | T109–T114 | Resolución validada; rechazo de estrategia ausente/no validada; MVP sin registro |
| I8 | US9 | FR-035–FR-038; RNF-005, RNF-009 | `/exchange-packages/validate`, adaptadores de importación y `events-asyncapi.yaml` | T115–T120 | Validación atómica; formato no soportado; falla de consumidor sin impacto |
| Transversal | Todas | FR-039–FR-040; RNF-005–RNF-006, RNF-013–RNF-019; SC-001–SC-010 | Problem Details y seguridad global | T001–T025, T121–T132 | Arquitectura, migración, E2E, k6, usabilidad, accesibilidad y documentación |

Cada prueba crítica debe declarar los FR/RNF/SC y procesos P que valida. Cada resultado de aceptación debe conservar entorno, fecha, versión ejecutada y datos sintéticos utilizados.

## Evidencia I0

| Requisito | Evidencia | Comando |
|---|---|---|
| FR-001, FR-002, RNF-002, RNF-003 | `SessionServiceTest` | `cd backend && .\mvnw.cmd test` |
| FR-003, FR-005 | `ScopeAuthorizationServiceTest`, `AccessScopeRulesTest` | `cd backend && .\mvnw.cmd test` |
| FR-004, FR-005 | `IdentityAdministrationServiceTest` | `cd backend && .\mvnw.cmd test` |
| FR-006, RNF-012 | `IdentityAccessMigrationTest`, `JpaAuditAdapter`, `AccessAuditController` | `SEC_RUN_DATABASE_TESTS=true` sobre base descartable; `.\mvnw.cmd test` |
| RNF-013 | `CorrelationFilter` y Problem Details de acceso | `cd backend && .\mvnw.cmd verify` |
| RNF-019 | `tests/fixtures/pilot/access/manifest.json` y `scripts/verify-synthetic-data.ps1` | `.\scripts\verify-synthetic-data.ps1` |
| SC-001–SC-006 | `tests/acceptance/access-scopes.spec.ts` | `cd frontend && npm run test:acceptance` con backend y frontend activos |
| RNF-014 | Formularios con etiquetas, tablas con `caption`/`scope` y mensajes `role="alert"`/`role="status"` | `cd frontend && npm run build && npm run lint` |

## Evidencia I1

| Requisito | Evidencia | Comando |
|---|---|---|
| FR-007, RNF-011 | `ModelVersionTest`, `EvaluationModelControllerTest` | `cd backend && .\mvnw.cmd test` |
| FR-008, FR-008a | `PublicationValidatorTest`, `FormulaValidatorTest` | `cd backend && .\mvnw.cmd test` |
| FR-009 | `ModelSuccessionServiceTest`, guardas V004 | `cd backend && .\mvnw.cmd test` |
| FR-010, RNF-012 | `ModelComparisonServiceTest`, `EvaluationModelAuditTest` | `cd backend && .\mvnw.cmd test` |
| RNF-018, RNF-019 | `tests/fixtures/models/general-2024/manifest.json` | `scripts/verify-synthetic-data.ps1` |

## Fundamentos ejecutables (T001–T025)

| Regla | Evidencia automatizada | Comando |
|---|---|---|
| RNF-011, RNF-017 | `ModuleArchitectureTest` y tipos `DomainId`, `DomainClock`, `OptimisticLockVersion` | `cd backend && .\mvnw.cmd verify` |
| FR-039, RNF-005 | `OpenApiContractTest`, `ApiProblemHandler` y tipos frontend `ApiProblem` | `cd backend && .\mvnw.cmd test`; `cd frontend && npm test` |
| RNF-012 | V002, trigger append-only y `JpaAuditAdapter` transaccional | `CleanDatabaseMigrationTest` con `SEC_RUN_DATABASE_TESTS=true` sobre base descartable |
| RNF-013 | `CorrelationFilter` y saneamiento de Logback | `cd backend && .\mvnw.cmd verify` |
| RNF-014 | layout con enlace de salto, foco visible y regiones semánticas | `cd frontend && npm run build` |
| RNF-019 | manifiesto sintético y detector de datos/secretos | `scripts/verify-synthetic-data.ps1` o `scripts/verify-synthetic-data.sh` |
| Contratos | OpenAPI 3.1 y AsyncAPI | `scripts/validate-contracts.ps1` o `scripts/validate-contracts.sh` |

La prueba de migración limpia nunca usa la base de desarrollo: permanece omitida salvo que se habilite explícitamente contra una base PostgreSQL descartable mediante `SEC_RUN_DATABASE_TESTS=true` y credenciales `SEC_TEST_DB_*`, o mediante `SEC_USE_TESTCONTAINERS=true`.

El perfil `sqlite-test` aporta esquema y datos sintéticos para la suite rápida
sin PostgreSQL; `SqliteSyntheticDataTest` verifica usuarios, funciones, proceso,
indicadores y asignaciones. Sus resultados no constituyen evidencia de
migración, concurrencia, inmutabilidad por triggers ni aceptación final. Los
comandos y fixtures se documentan en [sqlite.md](./sqlite.md).

## Cobertura explícita FR-041–FR-070

Las tareas de esta sección pertenecen exclusivamente a `specs/005-complete-evaluation-workflows/tasks.md`. Las historias US1–US10 también pertenecen a 005. Las relaciones son cobertura **de diseño**: las pruebas y resultados de ejecución siguen pendientes.

| Requisito | Historias | Tareas planificadas | Escenarios de aceptación | Criterio de éxito |
|---|---|---|---|---|
| FR-041 | US2, US7 | T022, T069, T072 | AC-P04-02, AC-P12-02, AC-X10 | SC-014 |
| FR-042 | US2 | T006, T023, T030, T114 | AC-P03-01, AC-P04-01 | SC-011 |
| FR-043 | US1 | T013–T020 | AC-P02-01, AC-P02-02, AC-P02-03 | SC-001 |
| FR-044 | US2 | T024, T027, T114, T116 | AC-P03-01, AC-P03-02, AC-X04 | SC-011 |
| FR-045 | US2 | T021, T024, T025 | AC-P04-01, AC-P04-02, AC-X05 | SC-011 |
| FR-046 | US2 | T022, T026, T028, T029 | AC-P04-01, AC-P04-03 | SC-011 |
| FR-047 | US2, US7 | T022, T026, T068, T069, T072 | AC-P12-01, AC-P12-02, AC-X10 | SC-014 |
| FR-048 | US3 | T031, T034, T035, T037 | AC-P05-01, AC-P05-02, AC-X04 | SC-018 |
| FR-049 | US3 | T031, T034, T035 | AC-P05-03 | SC-018 |
| FR-050 | US3 | T032, T033, T036–T039, T117 | AC-P06-01, AC-P06-02, AC-P06-03 | SC-001 |
| FR-051 | US4 | T040, T042–T045, T047, T048 | AC-P07-01, AC-P08-01, AC-X06 | SC-012 |
| FR-052 | US4, US6 | T041, T043, T046, T058, T120 | AC-P11-02, AC-X09 | SC-012 |
| FR-053 | US6 | T058, T059, T062, T063, T120 | AC-P11-01, AC-P11-03, AC-X08 | SC-012 |
| FR-054 | US5, US6 | T053, T058, T062, T119, T120 | AC-P09-01, AC-P11-03, AC-X06 | SC-012 |
| FR-055 | US5 | T050, T053, T056, T057 | AC-P09-01, AC-X06 | SC-013 |
| FR-056 | US7 | T068, T073–T076 | AC-P12-01, AC-P12-02 | SC-014 |
| FR-057 | US7 | T069–T072, T074–T076 | AC-P12-01, AC-P12-02, AC-X10 | SC-014 |
| FR-058 | US8 | T077, T080, T082, T121 | AC-P13-01, AC-P13-02, AC-P13-03 | SC-015 |
| FR-059 | US8 | T077, T079, T080, T083 | AC-P13-01 | SC-015 |
| FR-060 | US8 | T078, T079, T081–T084, T122 | AC-P14-01, AC-P14-03 | SC-015 |
| FR-061 | US8 | T078, T081–T085, T122 | AC-P14-01, AC-P14-02, AC-X12 | SC-015 |
| FR-062 | US9 | T086–T089, T091–T093 | AC-P15-01, AC-P15-02 | SC-016 |
| FR-063 | US9 | T086, T090, T092, T123 | AC-P15-03, AC-X13 | SC-016 |
| FR-064 | US6 | T060–T067, T120 | AC-P16-01 documental, AC-X14 | SC-017 |
| FR-065 | US10 | T094, T096, T097, T124 | AC-P16-01 integral, AC-P16-02 | SC-018 |
| FR-066 | US10 | T095, T098–T102, T124 | AC-P16-03, AC-X15 | SC-018 |
| FR-067 | US1–US10 | T007–T010, T035, T081, T117, T124 | AC-X01, AC-X04, AC-P14-02 | SC-001, SC-018 |
| FR-068 | US6–US10 | T065, T078, T081, T111 | AC-P14-01, AC-X12 | SC-013, SC-015 |
| FR-069 | US4 | T118, T040, T045, T047 | AC-P07-01, AC-X16 | SC-012 |
| FR-070 | Todas | T002, T113, T111 | AC-P05-01 (tipo interno); contrato sin operación de acreditación/evaluación externa | SC-017 |

Los escenarios AC-Pxx están en [process-acceptance.md](process-acceptance.md) y AC-Xxx en [domain-rules.md](../requirements/domain-rules.md). La referencia a una tarea no acredita que esté terminada.

## Requisitos no funcionales y resultados pendientes

| Requisito | Tareas / referencia de pruebas | Verificación de aceptación |
|---|---|---|
| RNF-001–RNF-003 | 001 US1; 005 T010, T020, T125 | Hash adaptativo, cierre/desactivación, 30 min de inactividad y cinco fallos en ventana móvil de 15 min. |
| RNF-004 | 004 US2; 005 T115, T118 | Tipo detectado, integridad y límite exacto 26 214 400 bytes, incluyendo interrupciones y referencias. |
| RNF-005 | T010, T107, T109, T125 | Listados, agregados, errores y auditoría sin secretos ni datos de ámbitos ajenos. |
| RNF-006–RNF-008 | T106; 004 T081 | Dataset de 100 usuarios, 30 sesiones simultáneas, 500 indicadores y 10 000 versiones; p95 interactivo <=2 s y cargas <=25 MiB <=15 s en entorno registrado. |
| RNF-009 | T067, T125 | MVP ejecutable con módulos posteriores e integraciones deshabilitados. |
| RNF-010 | 004 US2; T115, T101 | Fallos antes/después de promoción y restauración conjunta sin referencias falsas. |
| RNF-011 | T012, T042, T051, T060, T119, T124 | ETag/estados en conflicto, sin doble decisión vigente ni pérdida silenciosa. |
| RNF-012 | T008, T050, T078, T095, T125 | Auditoría append-only, orden temporal y ámbito de consulta. |
| RNF-013 | T011, T107, T125 | Toda respuesta/fallo relevante conserva correlación segura. |
| RNF-014–RNF-015 | T105 | Teclado, foco, mensajes/tablas, WCAG 2.2 AA y matriz de navegadores del proyecto. |
| RNF-016 | T108, T110 | Migración y recorridos reproducibles con PostgreSQL local, sin Docker obligatorio. |
| RNF-017 | T003, T126 | Pruebas de dependencia y revisión de puertos por módulo. |
| RNF-018 | T010–T012, T013–T102, T125 | Casos críticos de estados, permisos, cálculo y versiones vinculados con FR y AC. |
| RNF-019 | T006, T109, T114 | Fixtures y resultados sintéticos, sin datos reales ni secretos. |

## Criterios de éxito de la reconciliación

La [revisión ejecutada del código existente](implementation-review.md) registra
correcciones y pruebas de regresión del 2026-10-21. No acredita los criterios de
éxito pendientes de la tabla siguiente ni la implementación completa de P01–P16.

| Criterio | Evidencia planificada | Estado |
|---|---|---|
| SC-011 | T023/T030: cinco configuraciones verificadas y sin mezcla | Pendiente de ejecución |
| SC-012 | T058/T120: métricas manuales, cardinalidad y casos cero | Pendiente de ejecución |
| SC-013 | T050/T056: aceptación documental no genera valoración | Pendiente de ejecución |
| SC-014 | T068–T076: fórmula/variables, escala y estrategia verificables | Pendiente de ejecución |
| SC-015 | T078/T084/T085: informe hasta sus sustentos exactos | Pendiente de ejecución |
| SC-016 | T086/T093: mejora hasta hallazgo e informe de origen | Pendiente de ejecución |
| SC-017 | T067: MVP con P12–P15 deshabilitados | Pendiente de ejecución |
| SC-018 | T094/T101/T102: cierres, excepciones y restauración | Pendiente de ejecución |
