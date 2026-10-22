# D3 - Autorizacion y planificacion de procesos (evidencia)

## Alcance
- Fase 5 US3/D3: autorizacion, suspension/reanudacion/reapertura con procedencia, equipos/actividades/plazos con excepciones autorizadas y revisiones.
- Sin declaracion de aceptacion institucional; solo datos sinteticos. PostgreSQL real no habilitado; validado por migracion SQLite y pruebas.
- Se mantiene compatibilidad aditiva con rutas/contratos v1 (T113).

## Backend implementado
- Dominio: `ProcessTeam`, `TeamMembership`, `ProcessActivity` (PLANNED), `ProcessAssignment` extendido con `teamId`/`identityId` y regla exactamente uno.
- Repositorio: `SelfAssessmentRepository` ampliado (teams/members/activities/authorizations/transiciones). `JdbcSelfAssessmentRepository` persistente; `InMemorySelfAssessmentRepository` actualizado. Historial `process_activity_revision`.
- Aplicacion: `ProcessPlanningService` (equipos, integrantes, asignaciones a persona/equipo, actividades, dependencias aciclicas, plazo dentro de periodo o excepcion autorizada). `ProcessTransitionService` exige procedencia para suspension/reanudacion/reapertura, persiste decision. `SelfAssessmentService` guarda autorizacion inicial.
- API: `ProcessGovernanceController` (/processes/{id}/teams, /teams/{id}/memberships, /processes/{id}/activities, /activities/{id}) con ETag por revision de proceso. `ProcessAssignmentController` acepta `identityId`/`userId`/`teamId`. `SelfAssessmentController` valida procedencia completa.
- Migracion: `V010__process_governance.sql` (process_team, team_membership, process_authorization, process_activity, dependencias, process_transition.decision_*, process_assignment.team_id/identity_id, CHECK). `schema.sql` SQLite actualizado.

## Pruebas
- `AuthorizedProcessLifecycleTest`: autorizacion + activacion; suspension/resume con politica.
- `ProcessTransitionServiceTest`: procedencia obligatoria y sobrecarga retrocompatible.
- `ProcessPlanningServiceTest`: equipos/actividades/plazos/excepciones.
- `TeamPlanningIntegrityTest`: integrante individual, exactamente persona o equipo, ciclo de dependencias, excepcion de plazo.
- Backend: suite completa ejecutada con JDK 24 (mvnw) tras correccion de constructor `ProcessTransitionService`; contexto Spring OK tras ajuste. Estado final: ver resultado de `mvn test` en `backend/target/surefire-reports`.

## Frontend
- Ruta `/processes/:processId/planning` con `ProcessPlanningPage` (autorizacion, equipos/integrantes, asignaciones persona/equipo, actividades con dependencias y excepcion de plazo). Enlace desde detalle de proceso. `ProcessApi` extendido. Build/lint/test OK.

## Notas
- No se añaden operaciones `PROCESS_PLAN`/`PROCESS_READ` a OpenAPI para mantener compatibilidad (T113).
- Evidencia sintética, sin declaracion institucional.
