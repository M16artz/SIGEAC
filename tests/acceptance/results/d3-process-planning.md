# D3 - Autorizacion y planificacion de procesos (evidencia)

## Alcance
- Fase 5 US3/D3: autorizacion, suspension/reanudacion/reapertura con procedencia, equipos/actividades/plazos con excepciones autorizadas y revisiones.
- Sin declaracion de aceptacion institucional; solo datos sinteticos. Persistencia y migracion D3 verificadas en SQLite y PostgreSQL 18.4 descartable; recorrido integral Edge con backend PostgreSQL aprobado.
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

## Continuacion verificada (2026-10-22)

Se corrigio una brecha de T038: los controles enviaban solo el motivo al suspender,
reanudar o reabrir, aunque el backend exige una decision autorizada. Ahora solicitan
asignacion propia, fundamento y referencia documental y envian esos campos junto
con el motivo y el `If-Match` de la revision actual. Los controles bloquean decisiones
incompletas y envios simultaneos; un rechazo conserva los datos para corregirlos.

- `process-state-controls.component.spec.ts`: siete casos para campos obligatorios,
  suspension, reanudacion desde suspension/reapertura, reapertura, rechazo y doble envio.
- `process-api.service.spec.ts`: verifica procedencia y ETag en los cuerpos HTTP.
- `SelfAssessmentControllerTest`: verifica el traslado de la procedencia al servicio
  y el rechazo de decisiones parciales sin mutacion.
- Frontend: `npm.cmd test`, 24 pruebas aprobadas; `npm.cmd run lint` y
  `npm.cmd run build`, sin errores.
- Backend: `mvnw.cmd test` con JDK 25, 211 casos registrados, 203 aprobados y
  ocho omitidos, sin fallos ni errores. Los omitidos requieren PostgreSQL/Testcontainers.

Esta continuacion no cierra D3: siguen pendientes la reconciliacion completa del
contrato, la aceptacion integral y la validacion con PostgreSQL real.

## Integridad de equipos y actividades (2026-10-22)

- Una membresia exige una identidad individual activa, no tecnica, y una asignacion
  de rol propia que cubra el ambito y toda su vigencia. Roles ajenos, revocados,
  futuros, vencidos o en conflicto se rechazan antes de cambiar la revision.
- Las asignaciones a equipos reconocen a sus integrantes vigentes. Cada consulta
  reevalua su rol; la pertenencia al equipo no concede permisos de operacion.
  El listado de responsabilidades usa la misma comprobacion que el catalogo
  consumido por evidencias y conserva el filtro independiente de autorizacion.
- Se verifica la politica individual al asignar una funcion al equipo y al agregar
  un integrante a un equipo ya asignado, evitando eludir conflictos por esa via.
- Crear o actualizar una actividad exige una asignacion del mismo indicador y,
  si se indica fuente, que pertenezca a ese indicador en la version fijada del modelo.
  Los rechazos conservan actividad y revision anteriores.
- La pantalla solicita el rol del integrante, filtra asignaciones por indicador,
  permite indicar una fuente, la conserva al editar y valida las fechas antes de
  convertirlas. El proceso suspendido no muestra controles de edicion de actividades.

Verificacion ejecutada con JDK 25: `mvnw.cmd test`, 224 casos registrados,
216 aprobados y ocho omitidos por requerir PostgreSQL; sin fallos ni errores.
`npm.cmd test`: 30 pruebas aprobadas. `npm.cmd run lint` y `npm.cmd run build`:
sin errores. Se agregaron `TeamMembershipPolicyTest`, `TeamAssignmentAccessTest`
y `process-planning.page.spec.ts`, y se extendio `TeamPlanningIntegrityTest`.

Esto completa las pruebas de integridad T117 y de planificacion T032 en el alcance
de software sintetico. Siguen pendientes el recorrido integral en navegador,
PostgreSQL real y la aceptacion completa D3.

## Consulta de actividades y contrato HTTP (2026-10-22)

- `/processes/{id}/activities` y `/activities/{id}` permiten lectura a participantes
  con asignacion vigente y permiso de la funcion correspondiente. Los miembros de
  equipo pasan por la misma comprobacion de rol que el catalogo de responsabilidades.
  Coordinacion y administracion conservan la consulta completa. Sin ambito, recurso
  ajeno e inexistente devuelven `404/PROCESS_NOT_FOUND`; cuentas tecnicas no acceden.
- La ruta web `/processes/:processId/activities`, enlazada desde el detalle, muestra
  actividades e indicadores permitidos, vencimientos en hora local, estado vacio,
  carga, errores y aviso de suspension. No presenta controles de modificacion.
- `ProcessGovernanceControllerTest` ejecuta controladores y servicios reales con
  repositorios en memoria: equipos/membresias, creacion y actualizacion de actividades,
  ETags de proceso, rechazos obsoletos, permisos de persona/equipo y aislamiento.
  Las decisiones rechazadas no modifican actividad ni revision. No sustituye pruebas
  de persistencia ni de CSRF/autenticacion en servidor HTTP completo.
- La validacion de entrada rechaza dependencias nulas y alias `identityId`/`userId`
  contradictorios con 400 antes de mutar.
- `docs/technical/contracts/openapi.yaml` incorpora equipos, membresias, actividades,
  sus esquemas y respuestas, y la procedencia ya exigida por transiciones/reapertura.
  `ProcessAssignmentCommand` queda separado de `AssignmentCommand` de acceso.
  La decision v1 aditiva se registra en `docs/technical/migration-p01-p16.qmd`.

Validacion ejecutada: backend con JDK 25, 230 casos registrados, 222 aprobados y ocho
omitidos por PostgreSQL; cero fallos/errores. Frontend: 35 pruebas aprobadas, lint y
compilacion sin errores. Los tres contratos OpenAPI y AsyncAPI validan; las 12
operaciones compartidas no presentan incompatibilidades. Se conserva una advertencia
de componente sin uso para `ReasonCommand`, retenido y deprecado por compatibilidad.

T031/T034 cuentan con las pruebas existentes de autorizacion, legado y matriz de
estados aprobadas. T037/T038 completan su alcance de endpoints/contrato e interfaz
para software sintetico. D3 no se declara cerrado: faltan PostgreSQL real, comprobar
la persistencia completa de gobierno y el recorrido integral en navegador.

## Persistencia transaccional y PostgreSQL real (2026-10-22)

Se reprodujeron dos fallas antes de corregirlas: una dependencia invalida dejaba
actividad, historial y dependencias parcialmente modificados al guardar directamente
en el repositorio; las decisiones se insertaban con un UUID distinto al del dominio.

`JdbcSelfAssessmentRepository.saveActivity` y `recordTransition` ahora son
transaccionales y conservan el identificador de la decision. Las escrituras con
revision obsoleta lanzan `ProcessProblemException/PRECONDITION`, que la API traduce
a 412, conservando datos e historial en vez de producir un error interno.

`ProcessGovernancePersistenceTest` verifica seis casos en ambos motores:

- Equipos, integrantes, asignaciones y actividades leidos desde otra conexion.
- Revision anterior y dependencias preservadas al actualizar una actividad.
- Rollback completo ante una dependencia que viola integridad referencial.
- Autorizacion inicial, activacion, suspension y reanudacion persistidas, sin
  fabricar autorizaciones de procesos legados. La politica se simula en estos
  casos para aislar la persistencia; sus permisos se verifican en las pruebas
  de aplicacion/API existentes.
- Identificador estable de decision y rollback ante su insercion duplicada.
- Revisiones obsoletas y fallo de auditoria sin escrituras parciales.

`ProcessGovernanceMigrationTest` aplica V001-V009 en un esquema aislado, carga
procesos/asignaciones/transiciones legados y aplica solamente V010. Comprueba
snapshots intactos, sujetos individuales conservados, ausencia de autorizaciones
retroactivas, asignaciones a equipos y rechazo SQL de sujetos vacios o dobles.
Las pruebas de persistencia tambien migran desde cero hasta V013. No se modifico
ningun SQL historico, checksum, registro Flyway ni se uso `repair`/`out-of-order`.

Entorno ejecutado: JDK 25 y PostgreSQL 18.4 en un cluster nuevo y descartable,
`127.0.0.1:55443/postgres`, usuario sintetico `sec_d3`, directorio
`backend/target/pg-d3-persistence`. Cada prueba usa y elimina un esquema UUID
propio; no limpia esquemas compartidos ni toca bases institucionales.

Suite completa con la bandera D3 habilitada: **243 casos registrados, 235 aprobados,
0 fallos, 0 errores y 8 omitidos** de las pruebas historicas de otros bloques.
Los 13 casos nuevos (12 de persistencia y uno de migracion) se ejecutaron sin omisiones.
No se modifico ni se reejecuto el frontend en esta continuacion.

Reproduccion desde `backend/`, con PostgreSQL descartable disponible:

```powershell
$env:SEC_RUN_GOVERNANCE_DATABASE_TESTS='true'
$env:SEC_TEST_DB_URL='jdbc:postgresql://127.0.0.1:55443/postgres'
$env:SEC_TEST_DB_USER='sec_d3'
.\mvnw.cmd test "-Dtest=ProcessGovernancePersistenceTest,ProcessGovernanceMigrationTest"
```

T033/T035/T036 quedan cerradas para software sintetico. T039 y la aceptacion
integral D3 siguen pendientes del recorrido de navegador con backend real.

## T039 - Aceptacion tecnica en navegador completada (2026-10-22)

`npm.cmd run test:d3` ejecuta `tests/acceptance/process-planning.spec.ts` en Edge
headless contra Angular en 14203, backend real en 18083 y PostgreSQL 18.4 en 55443.
Resultado final: **1 recorrido completo aprobado**, sin mocks HTTP ni cambios SQL
para preparar los datos del recorrido. Referencias, modelo publicado, aplicabilidad,
roles y usuarios se preparan mediante las APIs publicas con datos sinteticos.

El recorrido verifica:

- Crear proceso en preparacion y rechazar activacion sin autorizacion.
- Registrar autoridad, fundamento, constancia y motivo; activar con modelo aplicable.
- Crear equipo, agregar integrante con rol propio y asignar responsabilidad al equipo.
- Crear y corregir actividad, conservando su fuente, revision y datos tras recargar.
- Rechazar CSRF ausente, If-Match ausente/obsoleto y ciclos de actividades.
- Suspender con decision y rechazar nuevas actividades durante la suspension.
- Consultar como responsable las actividades de su equipo y ocultar las de otro
  responsable, manteniendo 404 indistinguible y prohibicion de planificar.
- Reanudar con constancia, conservar modelo/autorizacion y recuperar el plan.

La integracion detecto un prerrequisito que las pruebas en memoria no cubrian:
el vinculo normativo apuntaba a `model_node`, mientras los modelos se guardan en
snapshots. V014 valida el indicador contra la configuracion de la version exacta.
Conserva los vinculos historicos y sus otras FK, soporta IDs estables en sucesores
y rechaza indicadores ajenos. El mismo guard se prueba en SQLite nuevo.
El perfil SQLite auxiliar no migra archivos antiguos; esta aceptacion usa PostgreSQL.

Regresion final con JDK 25 y bandera PostgreSQL D3 habilitada: **245 casos backend,
237 aprobados, cero fallos/errores, ocho omitidos de otros bloques**. Las 14 pruebas
de persistencia (siete por motor) y la migracion D3 se ejecutaron sin omisiones.
Frontend: **35 pruebas aprobadas** y lint sin errores. La compilacion de desarrollo
se ejecuto al servir la interfaz para el recorrido. La primera ejecucion de navegador
se interrumpio por una recarga del servidor de desarrollo; la repeticion final paso.

Reproduccion con un backend descartable migrado a V014, `sec.demo-data=true`, puerto
18083 y costo bcrypt permitido (10 o superior):

```powershell
# Terminal frontend; backend ya iniciado en 18083
npm.cmd start -- --host 127.0.0.1 --port 14203 --proxy-config proxy.d3.conf.json
# Otra terminal frontend, con el servidor listo
npm.cmd run test:d3
```

Base del recorrido ejecutado: `sec_d3_browser_1792712458781`, nueva y descartable.
T039 y D3 quedan cerrados para software sintetico. La aceptacion institucional y
los bloques restantes conservan sus propios criterios y pendientes.
