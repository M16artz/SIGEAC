# Candidatos pendientes de validación

Siete rutas revisadas por fuente y por revisores independientes. Ninguna fue reproducida; no se asigna gravedad. Los pasos siguientes usan únicamente fixtures ficticios en un sandbox conforme, nunca un despliegue compartido.

## NV-01: El alta de asignaciones no comprueba la autoridad sobre el ámbito destino

Fingerprint: `identityaccess:IdentityAdministrationService.assign:destination-scope`.

Un administrador humano limitado a carrera A puede solicitar una asignación en B. La ruta exige capacidad administrativa en algún ámbito, pero no autorización sobre B. El resultado candidato es conceder funciones fuera del ámbito delegado, incluso a sí mismo. createException presenta la misma pérdida del ámbito destino. No se ejecutó una reproducción.

Causa propuesta: assign y createException usan require/hasOperation como autorización suficiente y persisten un AuthorizationScope de la solicitud sin comprobar isAllowed para ese destino. Estructura, claves foráneas y vigencia no comprueban competencia del otorgante.

Ruta de código:

- `backend/src/main/java/ec/edu/unl/sec/identityaccess/api/IdentityAdministrationController.java:86` — POST /users/{userId}/assignments: Recibe función y ámbito del solicitante autenticado.
- `backend/src/main/java/ec/edu/unl/sec/identityaccess/application/IdentityAdministrationService.java:157` — assign: Exige ACCESS_ASSIGN_SCOPE; construye ámbito en 172 y persiste en 237 sin isAllowed del destino.
- `backend/src/main/java/ec/edu/unl/sec/identityaccess/infrastructure/JpaIdentityAccessRepository.java:249` — createAssignment: Inserta la asignación para el ámbito recibido sin comprobar competencia del otorgante.

Evidencia adicional:

- `specs/001-identity-access-scopes/spec.md:43` — Exige rechazar creación, cambio o revocación fuera del ámbito del administrador.
- `backend/src/main/java/ec/edu/unl/sec/identityaccess/application/ScopeAuthorizationService.java:53` — hasOperation no recibe el ámbito destino; isAllowed sí compara contains en línea 35.
- `backend/src/main/java/ec/edu/unl/sec/identityaccess/application/IdentityAdministrationService.java:258` — La revocación sí aplica isAllowed sobre el ámbito de la asignación.
- `backend/src/main/java/ec/edu/unl/sec/identityaccess/application/IdentityAdministrationService.java:288` — createException exige capacidad general, construye ámbito en 301 y persiste en 322 sin decisión por destino.

Bloqueos:

- No se ejecutó una reproducción: no está establecido un sandbox OS con aislamiento de red, entorno vacío con allowlist, fuente/herramientas de solo lectura, escritura exclusiva en scratch y límites explícitos de CPU, memoria, procesos, archivos, disco y tiempo.

Prueba local propuesta: En sandbox completo sin red, crear carreras ficticias A/B, administrador con ACCESS_ASSIGN_SCOPE solo en A y receptor activo sin B. Comprobar isAllowed del otorgante para B==false; invocar assign en B con función válida sin conflictos; observar como máximo una inserción y una decisión funcional del receptor. Esperado: rechazo sin escritura. En un fixture separado, usar gestor con ACCESS_MANAGE_EXCEPTIONS únicamente en A, política vigente y receptor con función aplicable, e intentar createException para B. Detenerse tras observar si se inserta una única excepción fuera de ámbito.

## NV-02: La consulta individual de revisión documental omite la autorización del recurso

Fingerprint: `documentreview:versionReview:missing-resource-authorization`.

Una sesión válida humana o técnica puede consultar la revisión de una versión ajena mediante su UUID conocido. La ruta no comprueba permiso sobre carrera, proceso o evidencia y proyecta resultados y comentarios. El resultado candidato es divulgación fuera del ámbito; no se afirma enumeración ni explotación demostrada.

Causa propuesta: versionReview exige únicamente currentActor y devuelve repository.latest(versionId). La consulta por UUID no añade autorización; omite la comprobación humana y canReview que protegen historial y bandeja. No se encontró control fuente alternativo que subsane esta ruta.

Ruta de código:

- `backend/src/main/java/ec/edu/unl/sec/documentreview/api/DocumentReviewController.java:53` — GET /evidence-versions/{versionId}/document-review: Recibe UUID y entrega reviews.versionReview.
- `backend/src/main/java/ec/edu/unl/sec/documentreview/application/DocumentReviewService.java:117` — versionReview: Exige solo actor autenticado y consulta repositorio.
- `backend/src/main/java/ec/edu/unl/sec/documentreview/infrastructure/JdbcDocumentReviewRepository.java:68` — latest: Selecciona por version_id sin identidad/carrera/proceso.
- `backend/src/main/java/ec/edu/unl/sec/documentreview/api/DocumentReviewController.java:153` — VersionReviewView.from: Proyecta decisión, revisión y resultados con comentarios note.

Evidencia adicional:

- `backend/src/main/java/ec/edu/unl/sec/documentreview/application/DocumentReviewService.java:104` — scoped sí rechaza cuentas técnicas y exige canReview por carrera/proceso.
- `backend/src/main/java/ec/edu/unl/sec/documentreview/api/DocumentReviewController.java:144` — OutcomeView copia requiredSourceId, outcome y note.
- `backend/src/main/java/ec/edu/unl/sec/identityaccess/infrastructure/SecurityConfiguration.java:34` — La cadena exige autenticación general, sin decisión por objeto.
- `backend/src/main/java/ec/edu/unl/sec/documentreview/infrastructure/DocumentReviewBoundaryAdapter.java:27` — currentActor traduce identidad; canReview es operación separada omitida.

Bloqueos:

- No se ejecutó una reproducción: no está establecido un sandbox OS con aislamiento de red, entorno vacío con allowlist, fuente/herramientas de solo lectura, escritura exclusiva en scratch y límites explícitos de CPU, memoria, procesos, archivos, disco y tiempo.

Prueba local propuesta: En sandbox conforme sin red externa, preparar carreras ficticias A/B, decisión de B con note='CONFIDENCIAL_B' y sesión humana autorizada solo en A. Una única petición local GET /api/v1/evidence-versions/{uuid_B}/document-review con UUID conocido comprueba si devuelve ese comentario aunque la evidencia B no sea visible. Esperado: rechazo sin datos. Detenerse tras esa observación mínima.

## NV-03: La consulta de auditoría pierde las restricciones de proceso del auditor

Fingerprint: `identityaccess:AccessAuditController.search:scope-collapse`.

Una identidad humana activa con ACCESS_READ_AUDIT solo en P1 puede alcanzar eventos y conteos de P2 de la misma carrera. La proyección incluye identificadores, fecha, correlación, acción, objeto, resultado, motivo y ámbitos. No devuelve metadata_json ni se afirma exposición de secretos. Ruta verificada por fuente, sin fixture ejecutado.

Causa propuesta: auditCareerIds usa repository.authorizedCareerIds, cuyo SELECT DISTINCT career_id elimina proceso e indicador. AuditQueryRepository recibe solo carreras y no conserva ámbito efectivo en search/count; hasOperation acredita alguna asignación, sin autorizar cada evento.

Ruta de código:

- `backend/src/main/java/ec/edu/unl/sec/identityaccess/api/AccessAuditController.java:26` — GET /audit-events: Consulta autenticada obtiene careerIds para selección y conteo.
- `backend/src/main/java/ec/edu/unl/sec/identityaccess/application/IdentityAdministrationService.java:347` — auditCareerIds: Exige capacidad general y obtiene carreras sin procesos/indicadores.
- `backend/src/main/java/ec/edu/unl/sec/identityaccess/infrastructure/JpaIdentityAccessRepository.java:424` — authorizedCareerIds: Devuelve DISTINCT career_id de asignaciones vigentes.
- `backend/src/main/java/ec/edu/unl/sec/audit/infrastructure/JpaAuditAdapter.java:99` — AuditQueryRepository.filter: Filtra por carrera, sin proceso; controlador solicita includeUnscoped=true.

Evidencia adicional:

- `specs/001-identity-access-scopes/spec.md:92` — Exige no devolver eventos, conteos o indicios ajenos al ámbito.
- `backend/src/main/java/ec/edu/unl/sec/identityaccess/application/ScopeAuthorizationService.java:35` — La decisión ordinaria sí exige contains(requested), distingue P1/P2.
- `backend/src/main/java/ec/edu/unl/sec/identityaccess/api/AccessAuditController.java:42` — search/count reciben solo carreras sin filtrado posterior.
- `backend/src/main/java/ec/edu/unl/sec/audit/infrastructure/JpaAuditAdapter.java:72` — Proyección sin metadata_json ni columnas de actor.

Bloqueos:

- No se ejecutó una reproducción: no está establecido un sandbox OS con aislamiento de red, entorno vacío con allowlist, fuente/herramientas de solo lectura, escritura exclusiva en scratch y límites explícitos de CPU, memoria, procesos, archivos, disco y tiempo.

Prueba local propuesta: En sandbox completo, base descartable y sin red externa, crear carrera con procesos P1/P2, auditor solo P1 y evento sintético por proceso. Comprobar autorización ordinaria deniega P2. Una petición local GET /api/v1/audit-events comprueba si devuelve evento P2 y total=2. Esperado solo P1 y total=1. Detenerse tras la observación mínima.

## NV-04: El alta de asignaciones crea excepciones sin exigir el permiso específico

Fingerprint: `identityaccess:IdentityAdministrationService.assign:inline-conflict-capability`.

Un otorgante con ACCESS_ASSIGN_SCOPE pero sin ACCESS_MANAGE_EXCEPTIONS puede alcanzar una rama que crea una excepción aprobada por él al aportar exceptionReason. El endpoint independiente exige la capacidad específica. El escenario requiere una matriz que separe ambas capacidades; no se verificó el despliegue ni se ejecutó un fixture.

Causa propuesta: La rama de conflicto de assign valida longitud del motivo y llama createConflictException sin exigir ACCESS_MANAGE_EXCEPTIONS. Dominio e INSERT no comprueban competencia del aprobador.

Ruta de código:

- `backend/src/main/java/ec/edu/unl/sec/identityaccess/api/IdentityAdministrationController.java:86` — POST /users/{userId}/assignments: Acepta función, ámbito, vigencia y exceptionReason.
- `backend/src/main/java/ec/edu/unl/sec/identityaccess/application/IdentityAdministrationService.java:157` — assign: Exige ACCESS_ASSIGN_SCOPE; la rama de 195 admite motivo de 10 a 1000 caracteres.
- `backend/src/main/java/ec/edu/unl/sec/identityaccess/application/IdentityAdministrationService.java:204` — assign conflict branch: Crea excepción aprobada por el otorgante sin comprobar la capacidad específica.
- `backend/src/main/java/ec/edu/unl/sec/identityaccess/infrastructure/JpaIdentityAccessRepository.java:406` — createConflictException: Inserta la excepción; assign persiste después la función incompatible.

Evidencia adicional:

- `backend/src/main/java/ec/edu/unl/sec/identityaccess/application/IdentityAdministrationService.java:288` — createException sí exige ACCESS_MANAGE_EXCEPTIONS.
- `backend/src/main/java/ec/edu/unl/sec/identityaccess/application/IdentityAdministrationService.java:362` — require comprueba exclusivamente la operación recibida.
- `backend/src/main/java/ec/edu/unl/sec/identityaccess/infrastructure/JpaIdentityAccessRepository.java:182` — La evaluación efectiva acepta excepción vigente sin volver a verificar la competencia de approvedBy.
- `specs/001-identity-access-scopes/spec.md:134` — FR-004 permite únicamente excepciones expresamente autorizadas.

Bloqueos:

- No se ejecutó una reproducción: no está establecido un sandbox OS con aislamiento de red, entorno vacío con allowlist, fuente/herramientas de solo lectura, escritura exclusiva en scratch y límites explícitos de CPU, memoria, procesos, archivos, disco y tiempo.
- No se observó la matriz efectiva del despliegue; requiere otorgante con capacidad de asignar pero sin gestionar excepciones.

Prueba local propuesta: En sandbox completo sin red, otorgante ASSIGNER humano con solo ACCESS_ASSIGN_SCOPE y autoridad en el mismo ámbito del receptor RESPONSIBLE; configurar conflicto RESPONSIBLE/REVIEWER. Comprobar ausencia de ACCESS_MANAGE_EXCEPTIONS y comparar assign sin motivo con assign de REVIEWER y motivo válido. Observar como máximo una excepción/asignación y una decisión resultante; comparar createException, que debe denegar. Detenerse. No mezclar con el defecto de ámbito.

Observación del propietario: El propietario puede inspeccionar una exportación local de function_operation y asignaciones vigentes para determinar si ambas capacidades se separan; sin solicitudes de prueba al despliegue.

## NV-05: La autorrevisión acepta una excepción declarada por el propio revisor

Fingerprint: `documentreview:decide:self-authorized-self-review-exception`.

Un remitente histórico con permiso actual de revisión puede declarar selfReviewException=true y un motivo para revisar su propia versión. Los controles exigen competencia, proceso ACTIVE, versión vigente y revisión correcta, pero no una autorización de excepción emitida por otra autoridad. Resultado candidato: aceptación documental de evidencia propia sin excepción autorizada. Sin reproducción ejecutada.

Causa propuesta: El booleano HTTP evita SELF_REVIEW_FORBIDDEN. Dominio, persistencia y migración solo exigen motivo; no vinculan la excepción a una decisión autorizadora. Los conflictos de funciones vigentes no sustituyen el control sobre autoría histórica.

Ruta de código:

- `backend/src/main/java/ec/edu/unl/sec/documentreview/api/DocumentReviewController.java:63` — POST /evidence-versions/{versionId}/document-review: Transfiere booleano y motivo del solicitante a ReviewInput.
- `backend/src/main/java/ec/edu/unl/sec/documentreview/application/DocumentReviewService.java:71` — decide: Detecta remitente, pero rechaza solo cuando el booleano enviado es false.
- `backend/src/main/java/ec/edu/unl/sec/documentreview/domain/DocumentReviewDecision.java:31` — DocumentReviewDecision: Exige motivo de longitud válida sin referencia autorizadora.
- `backend/src/main/java/ec/edu/unl/sec/documentreview/infrastructure/JdbcDocumentReviewRepository.java:47` — save: Inserta decisión, booleano y motivo sin consultar autorización de excepción.

Evidencia adicional:

- `specs/005-complete-evaluation-workflows/spec.md:109` — Exige bloquear coincidencia de autor/revisor sin excepción autorizada.
- `backend/src/main/java/ec/edu/unl/sec/documentreview/application/DocumentReviewService.java:57` — Controles previos de revisión, proceso abierto, versión y revisión no autorizan excepción separadamente.
- `backend/src/main/java/ec/edu/unl/sec/identityaccess/infrastructure/JpaIdentityAccessRepository.java:177` — Conflictos consideran asignaciones actualmente vigentes; estudiar antiguo recopilador ahora revisor.
- `backend/src/main/java/ec/edu/unl/sec/documentreview/infrastructure/DocumentReviewBoundaryAdapter.java:32` — canReview no verifica excepción de autorrevisión.

Bloqueos:

- No se ejecutó una reproducción: no está establecido un sandbox OS con aislamiento de red, entorno vacío con allowlist, fuente/herramientas de solo lectura, escritura exclusiva en scratch y límites explícitos de CPU, memoria, procesos, archivos, disco y tiempo.

Prueba local propuesta: En sandbox conforme sin red, usuario ficticio U remite versión completa ACTIVE. Revocar o dejar vencer recopilación y conceder revisión del mismo proceso sin autorización de excepción. Comprobar canReview y que selfReviewException=false rechaza. En fixture limpio pedir una decisión con revisión vigente, booleano true, motivo/justificación válidos y resultados ACCEPTED. Observar solo si persiste ACCEPTED sin autorización y detenerse. Esperado: rechazo sin nueva decisión.

## NV-06: Las mutaciones de cobertura y remisión omiten el control de autor del borrador

Fingerprint: `evidence:draft-mutations:missing-author-check`.

Un responsable B con recopilación y responsabilidad vigente sobre todos los indicadores puede acceder al borrador de A. Algunas rutas permiten cambiar coberturas, registrar estructura o remitirlo con ese permiso de lectura, mientras archivos, referencias y metadatos exigen autoría. La remisión además registra a A como actor aunque la solicite B. Diferencia verificada por fuente, sin reproducción.

Causa propuesta: declare/remove, register estructurado y submit usan drafts.getVersion, cuyo guard comprueba ámbito y responsabilidad sin autor. Omiten requireEditableVersion que compara autor y actor; persistencia tampoco comprueba al autor solicitante.

Ruta de código:

- `backend/src/main/java/ec/edu/unl/sec/evidence/api/EvidenceSourceCoverageController.java:77` — POST /evidence-versions/{versionId}/submit: Recibe UUID/If-Match y llama remisión.
- `backend/src/main/java/ec/edu/unl/sec/evidence/application/SubmitEvidenceService.java:48` — submit: Valida revisión, borrador, proceso y fuentes, sin comparar solicitante y autor.
- `backend/src/main/java/ec/edu/unl/sec/evidence/application/EvidenceDraftService.java:180` — visible: Autoriza responsable vigente con canCollect por indicadores, sin exigir autoría.
- `backend/src/main/java/ec/edu/unl/sec/evidence/application/SubmitEvidenceService.java:64` — submit: Marca remitida la versión; auditoría usa version.authorId en 71.

Evidencia adicional:

- `backend/src/main/java/ec/edu/unl/sec/evidence/application/EvidenceDraftService.java:170` — requireEditableVersion rechaza actor diferente del autor; update también lo hace en 144.
- `backend/src/main/java/ec/edu/unl/sec/evidence/application/EvidenceSourceCoverageService.java:44` — declare usa guard de lectura; remove lo repite en 77 antes de eliminar.
- `backend/src/main/java/ec/edu/unl/sec/evidence/application/StructuredEvidenceService.java:51` — register no compara autoría.
- `backend/src/main/java/ec/edu/unl/sec/evidence/infrastructure/JdbcEvidenceGovernanceRepository.java:150` — markSubmitted usa UUID/estado/revisión, no autor.
- `backend/src/main/java/ec/edu/unl/sec/selfassessment/application/ProcessAssignmentService.java:55` — Duplicado incluye identidad, permite estudiar responsables distintos de un indicador.
- `specs/004-submit-evidence/spec.md:144` — FR-020a exige conservar el actor de la operación en auditoría.

Bloqueos:

- No se ejecutó una reproducción: no está establecido un sandbox OS con aislamiento de red, entorno vacío con allowlist, fuente/herramientas de solo lectura, escritura exclusiva en scratch y límites explícitos de CPU, memoria, procesos, archivos, disco y tiempo.
- Falta observar la combinación de asignaciones admitida y la mutación resultante; no se encontró autorización expresa de edición colaborativa ni compensación de persistencia.

Prueba local propuesta: En sandbox completo sin red, crear ACTIVE con A/B responsables del mismo indicador y EVIDENCE_COLLECT. A crea borrador con cobertura/referencia. Como B, comprobar que requireEditableVersion deniega y getVersion permite. Eliminar una cobertura con UUID/If-Match y observar persistencia. En fixture limpio remitir como B y comprobar solo SUBMITTED y actor de EVIDENCE_SUBMITTED. Esperado: rechazo de mutación ajena; delegación intencional debe conservar actor real. Detenerse en observaciones mínimas.

## NV-07: El registro de avances de mejora omite la suspensión del proceso

Fingerprint: `improvement:ActionProgressService:mutations-ignore-process-suspension`.

Una identidad humana con IMPROVEMENT_PROGRESS vigente puede alcanzar el registro de avances de una acción durante SUSPENDED. Resultado candidato: insertar avance e incrementar revisión sin reanudación institucional. Permiso de ámbito y revisión optimista existen, pero no sustituyen la suspensión. Sin reproducción ejecutada.

Causa propuesta: lockScope devuelve cualquier estado tras comprobar autorización. recordProgress rechaza VERIFIED y especializa CLOSED, pero no SUSPENDED antes de appendProgress/advanceHead. decide comparte omisión; persistencia no añade control.

Ruta de código:

- `backend/src/main/java/ec/edu/unl/sec/improvement/api/ImprovementController.java:125` — POST /improvement-actions/{actionId}/progress: Recibe acción, If-Match y datos de avance.
- `backend/src/main/java/ec/edu/unl/sec/improvement/application/ActionProgressService.java:68` — recordProgress: Obtiene proceso, comprueba revisión/acción, solo especializa CLOSED.
- `backend/src/main/java/ec/edu/unl/sec/improvement/application/ActionProgressService.java:178` — lockScope: Bloquea proceso y autoriza ámbito sin rechazar SUSPENDED.
- `backend/src/main/java/ec/edu/unl/sec/improvement/application/ActionProgressService.java:88` — recordProgress: Inserta avance y aumenta revisión de acción.

Evidencia adicional:

- `specs/005-complete-evaluation-workflows/spec.md:84` — Suspensión debe bloquear actuaciones ordinarias hasta reanudación autorizada.
- `backend/src/main/java/ec/edu/unl/sec/improvement/application/ImprovementPlanService.java:31` — Creación/revisión paralelas sí incluyen SUSPENDED en fases bloqueadas.
- `backend/src/main/java/ec/edu/unl/sec/selfassessment/application/DocumentaryProcessService.java:24` — lock devuelve estado persistido sin exigir fase de escritura.
- `backend/src/main/java/ec/edu/unl/sec/improvement/infrastructure/JdbcImprovementRepository.java:122` — appendProgress inserta sin comprobar fase; replaceHead en 102 usa identificador/revisión.
- `backend/src/main/java/ec/edu/unl/sec/selfassessment/domain/ProcessStateMachine.java:10` — ACTIVE puede suspenderse sin eliminar acciones existentes.

Bloqueos:

- No se ejecutó una reproducción: no está establecido un sandbox OS con aislamiento de red, entorno vacío con allowlist, fuente/herramientas de solo lectura, escritura exclusiva en scratch y límites explícitos de CPU, memoria, procesos, archivos, disco y tiempo.

Prueba local propuesta: En sandbox completo sin red, crear ACTIVE, acción APPROVED/IN_PROGRESS y actor con IMPROVEMENT_PROGRESS vigente. Suspender mediante transición institucional, conservar acción/asignación. Una petición local POST /api/v1/improvement-actions/{actionId}/progress con sesión/CSRF ficticios, If-Match vigente, descripción válida, valor/fecha/evidencia sintética comprueba si inserta avance y aumenta revisión mientras SUSPENDED. Esperado rechazo sin escritura. Detenerse tras observación mínima.

