# Matriz de trazabilidad inicial

| Incremento | Historia | Requisitos principales | Contrato | Tareas | Validación |
|---|---|---|---|---|---|
| I0 | US1 | FR-001–FR-006; RNF-001–RNF-003, RNF-005, RNF-012–RNF-013, RNF-019 | `/sessions`, `/me`, `/users`, `/users/{userId}/assignments` | T026–T039 | Quickstart I0; matriz función×ámbito×operación |
| I1 | US2 | FR-007–FR-010, FR-042; RF-051, RF-052, RF-054, RF-055, RF-057; RNF-011–RNF-012, RNF-018 | GET/POST `/models`, GET/POST `/models/{modelId}/versions`, GET `/model-versions/{versionId}`, `publish` | T040–T050 | Modelo general 2024; configuraciones representativas; inmutabilidad y no mezcla |
| I2 | US3 | FR-011–FR-015; RF-053; RNF-011–RNF-012, RNF-018 | `/processes`, detalle, indicadores, `activate`, `assignments`, `reopen` | T051–T061 | Quickstart I2; asociación histórica, estados y conflictos |
| I3 | US4 | FR-016–FR-020; RNF-004–RNF-005, RNF-007, RNF-010–RNF-012 | GET/POST `/evidences`, detalle, historial, carga/descarga de archivos, `submit` | T062–T074 | Quickstart I3; atomicidad e integridad |
| I4 | US5 | FR-021–FR-024; RNF-012, RNF-018 | `/document-review-inbox`, GET/POST `/evidence-versions/{versionId}/document-reviews` | T075–T083 | Quickstart I4; separación documental |
| I5 | US6 | FR-025–FR-028; RNF-012, RNF-018 | `/academic-evaluation-inbox`, GET/POST historial por indicador | T084–T092 | Quickstart I5; sustentos aceptados |
| I6 | US7 | FR-029–FR-034; RNF-006, RNF-008–RNF-009, RNF-014–RNF-015, RNF-018 | `progress`, `close`, listado/estado/descarga de `reports` | T093–T109 | Quickstart I6; reproducibilidad, rendimiento, usabilidad y accesibilidad |
| I7 | US8 | FR-041; RF-056; RNF-009, RNF-016 | `EvaluationStrategy` y Factory/Registry internos | T110–T115 | Resolución validada; rechazo de estrategia ausente/no validada; MVP sin registro |
| I8 | US9 | FR-035–FR-038; RNF-005, RNF-009 | `/exchange-packages/validate`, adaptadores de importación y `events-asyncapi.yaml` | T116–T121 | Validación atómica; formato no soportado; falla de consumidor sin impacto |
| Transversal | Todas | FR-039–FR-040; RNF-005–RNF-006, RNF-013–RNF-019; SC-001–SC-010 | Problem Details y seguridad global | T001–T025, T122–T133 | Arquitectura, migración, E2E, k6, usabilidad, accesibilidad y documentación |

Cada prueba crítica debe declarar los FR/RNF/SC que valida y cada resultado de aceptación debe conservar entorno, fecha y versión ejecutada.
