# Matriz de trazabilidad inicial

| Incremento | Historia | Requisitos principales | Contrato | Tareas | Validación |
|---|---|---|---|---|---|
| I0 | US1 | FR-001–FR-006; RNF-001–RNF-003, RNF-005, RNF-012–RNF-013, RNF-019 | `/sessions`, `/me`, `/users`, `/users/{userId}/assignments`, `/conflict-exceptions`, `/audit-events` | T001–T038 | Quickstart I0; matriz función–ámbito–operación; aceptación Playwright |
| I1 | US1–US5 | FR-007–FR-010, FR-008a; RNF-011–RNF-012, RNF-018–RNF-019 | `/models`, `/models/{modelId}/versions`, `/model-versions/{versionId}`, `publish`, `successors`, `compare`, `/model-audit-events` | `specs/002-version-evaluation-model/tasks.md` T001–T036 | Fixture sintético general-2024; validadores, inmutabilidad, sucesión y comparación |
| I2 | US1–US5 | FR-011–FR-015, FR-013a; RNF-011–RNF-012, RNF-018–RNF-019 | `/processes`, detalle, `activate`, `transitions`, `assignments`, `reopen`, auditoría | `specs/003-manage-self-assessment/tasks.md` T001–T030 | Modelo v1 fijado, matriz de estados/asignaciones y reapertura |
| I3 | US4 | FR-016–FR-020; RNF-004–RNF-005, RNF-007, RNF-010–RNF-012 | GET/POST `/evidences`, detalle, historial, carga/descarga de archivos, `submit` | T061–T073 | Quickstart I3; atomicidad e integridad |
| I4 | US5 | FR-021–FR-024; RNF-012, RNF-018 | `/document-review-inbox`, GET/POST `/evidence-versions/{versionId}/document-reviews` | T074–T082 | Quickstart I4; separación documental |
| I5 | US6 | FR-025–FR-028; RNF-012, RNF-018 | `/academic-evaluation-inbox`, GET/POST historial por indicador | T083–T091 | Quickstart I5; sustentos aceptados |
| I6 | US7 | FR-029–FR-034; RNF-006, RNF-008–RNF-009, RNF-014–RNF-015, RNF-018 | `progress`, `close`, listado/estado/descarga de `reports` | T092–T108 | Quickstart I6; reproducibilidad, rendimiento, usabilidad y accesibilidad |
| I7 | US8 | FR-041; RF-056; RNF-009, RNF-016 | `EvaluationStrategy` y Factory/Registry internos | T109–T114 | Resolución validada; rechazo de estrategia ausente/no validada; MVP sin registro |
| I8 | US9 | FR-035–FR-038; RNF-005, RNF-009 | `/exchange-packages/validate`, adaptadores de importación y `events-asyncapi.yaml` | T115–T120 | Validación atómica; formato no soportado; falla de consumidor sin impacto |
| Transversal | Todas | FR-039–FR-040; RNF-005–RNF-006, RNF-013–RNF-019; SC-001–SC-010 | Problem Details y seguridad global | T001–T025, T121–T132 | Arquitectura, migración, E2E, k6, usabilidad, accesibilidad y documentación |

Cada prueba crítica debe declarar los FR/RNF/SC que valida y cada resultado de aceptación debe conservar entorno, fecha y versión ejecutada.

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
