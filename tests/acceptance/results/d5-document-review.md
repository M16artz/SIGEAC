# D5 - Revision documental y subsanacion (evidencia)

## Alcance
- Fase 5 US5/D5 (tareas T050-T057 + T119): revision documental por cobertura con resultados fundados (ACCEPTED/OBSERVED/REJECTED), nota obligatoria >= 10 caracteres para OBSERVED/REJECTED, justificacion global obligatoria, excepcion de auto-evaluacion con motivo, inbox del revisor, historial append-only por proceso y subsanacion P10 con sucesor enlazado a la decision que la origina.
- Sin declaracion de aceptacion institucional; solo datos sinteticos. PostgreSQL real no habilitado en este paso; validado con SQLite (backend descartable) y suite backend completa con pruebas de persistencia pares sqlite/postgres opt-in (`SEC_RUN_FILE_DATABASE_TESTS`).
- Compatibilidad aditiva con rutas/contratos v1 mantenida (T113); sin cambios de OpenAPI respecto de lo ya acordado para D4.

## Backend implementado
- Dominio: `DocumentReviewDecision` (revision con predecesor obligatorio en rectificaciones, `selfReviewException` exige `exceptionReason` >= 10, `successorVersionId` opcional) y `ReviewCoverageOutcome` (resultado por fuente requerida; nota obligatoria >= 10 para OBSERVED/REJECTED, opcional en ACCEPTED, <= 4000). La aceptacion no se hereda entre versiones.
- Migracion `V015__document_review.sql` (decisiones append-only, `UNIQUE(version_id, revision)`, `UNIQUE(successor_version_id)`, resultados por cobertura `UNIQUE(decision_id, required_source_id)` con `note` nullable) + espejo `db/sqlite/schema.sql`.
- Aplicacion: `DocumentReviewService` (decide con If-Match de la revision de la decision vigente; correccion `correct()` que crea el sucesor DRAFT desde la version remitida y lo enlaza a la decision; 409 `SUCCESSOR_EXISTS`, 412 `STALE_REVIEW`, 428 `PRECONDITION_REQUIRED`, 422 `INVALID_ETAG`/`INVALID_OUTCOME`, 403 `FORBIDDEN`).
- Puertos/adaptadores: `ReviewSubmissionPort` + `JdbcReviewSubmissionAdapter` (inbox de versiones SUBMITTED sin decision previa en la version); `DocumentAuthorizationPort` revisa capacidad `EVIDENCE_REVIEW` y recusa al propio responsable salvo excepcion explicita.

## API (contratos v1 aditivos)
- GET `/processes/{id}/document-review/inbox` (ETag strong del tamano del listado).
- GET `/processes/{id}/document-review-decisions` (historial del proceso).
- GET/POST `/evidence-versions/{id}/document-review` (POST con If-Match; sin decision devuelve `PENDING_REVIEW`; decide responde 201 + ETag `"0"`).
- POST `/document-review-decisions/{id}/correction` (If-Match de la decision; 201 + Location `/api/v1/evidence-versions/{sucesor}`; el sucesor hereda coberturas y referencias).

## Pruebas
- `CompleteDocumentReviewTest` (T050): separacion de resultados por cobertura duplicada, construcciones de 12 argumentos corregidas en este incremento, excepciones de auto-evaluacion y rectificaciones encadenadas.
- `DocumentReviewControllerTest` (T051, 8 pruebas): inbox con ETag, decide 201/ETag `"0"`, falta If-Match -> 428, resultado invalido -> 422 `INVALID_OUTCOME`, version-review pendiente, correccion 201 + Location, `STALE_REVIEW` 412, historial por proceso.
- `DocumentReviewPersistenceTest` (parametrizado sqlite + postgres opt-in): guarda/rectifica con historial intacto, `linkSuccessor` con UNIQUE de sucesor, inbox de pendientes, revisiones y reconstruccion de consultas; aislamiento y orden por codigo con aserciones `containsExactlyInAnyOrder`.
- Suite backend completa en verde con JDK 24: 326 prueba, 0 fallos, 0 errores, 10 omitidas (opt-in PostgreSQL/Testcontainers).

## Recorrido de aceptacion T056 (2026-10-24, 11:37)
`npm.cmd run test:d5` ejecuta `tests/acceptance/document-review-correction.spec.ts` en Edge headless contra Angular en 14205, backend real en 18085 (perfil SQLite descartable) y sin mocks HTTP ni SQL de preparacion; los datos se crean via APIs publicas con datos sinteticos. Resultado: **1 recorrido completo aprobado** (2.8 s).

- El recopilador (RESPONSIBLE) crea evidencia v1, declara cobertura y agrega referencia externa (`409 SOURCE_REQUIRED` si no hay archivo ni referencia; se remite con If-Match de la referencia, 200 snapshot inmutable sin ETag).
- El responsable no puede decidir (403 sin capacidad `EVIDENCE_REVIEW`).
- El revisor ve la version en el inbox (`PENDING_REVIEW`), rechaza un If-Match obsoleto (412 `STALE_REVIEW`) y registra REJECTED con justificacion y nota por cobertura (>= 10 caracteres).
- La inssubsane P10: correccion obsoleta rechazada (412); correccion valida 201 con Location al sucesor; segunda subsanacion rechazada (409 `SUCCESSOR_EXISTS`).
- La v2 hereda coberturas y referencias de la v1 y se remite con If-Match `"0"` (200, snapshot inmutable sin ETag, contrato fijado en `EvidenceSubmissionControllerTest`).
- El revisor acepta la v2 (ACCEPTED sin nota por cobertura, ETag `"0"`); v1 permanece REJECTED con `successorVersionId` apuntando a la v2 y la v2 queda ACCEPTED sin sucesor.
- El historial del proceso conserva ambas decisiones (filtrado por versiones del recorrido; `id` exacto del REJECTED).
- UI: el enlace "Revisión documental" es visible para el rol REVIEWER y la bandeja responde bajo `/reviews`.

## Comandos de ejecucion
```powershell
# Terminal 1 (backend descartable SQLite en 18085)
$env:SEC_SQLITE_PATH = "$env:TEMP\opencode\sec-d5-acceptance.sqlite"
$env:SEC_TEST_FILE_ROOT = "$env:TEMP\opencode\sec-d5-files"
.\mvnw.cmd -q -Djava.version=24 -Denforcer.skip=true spring-boot:run `
  -Dspring-boot.run.profiles=sqlite-test -Dspring-boot.run.useTestClasspath=true `
  "-Dspring-boot.run.jvmArguments=-Dserver.port=18085"

# Terminal 2 (frontend en 14205)
npm.cmd start -- --host 127.0.0.1 --port 14205 --proxy-config proxy.d5.conf.json

# Terminal 3 (recorrido)
npm.cmd run test:d5
```

## Notas
- El recorrido de aceptacion detecto que `document_review_outcome.note` estaba NOT NULL mientras el dominio permite nota nula para ACCEPTED (provocaba 500 `INTERNAL_ERROR`). Se corrigio en `V015__document_review.sql` y en el espejo SQLite durante este incremento; las pruebas de persistencia y la suite completa se reejecutaron en verde.
- Se agrego `playwright.d5.config.ts`, `proxy.d5.conf.json` y `scripts/run-d5-acceptance.mjs` con el script `test:d5` (patron D3).
- `ApiProblemHandler` ahora registra las excepciones no esperadas a nivel ERROR antes de responder 500 (antes los 500 internos no dejaban rastro en el log).
- UI T055: bandeja e historial en `document-review-inbox.page.ts`, rutas `/reviews` (`REVIEW_ROUTES`) con `accessGuard` en `app.routes.ts` y enlace "Revisión documental" condicionado a `hasRole('REVIEWER')` en el layout; `DatePipe` importado; suite frontend 45 pruebas en verde, lint y build sin errores.
- Datos exclusivamente sinteticos; la validacion con PostgreSQL real queda para la campana de aceptacion integral.
