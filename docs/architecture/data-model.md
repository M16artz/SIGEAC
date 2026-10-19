# Modelo de datos

## Convenciones

- Identificadores UUID generados por la aplicación; fechas en UTC (`timestamptz`).
- Agregados editables incluyen `version bigint` para bloqueo optimista.
- Publicaciones, remisiones y decisiones finales son append-only; una corrección apunta a su predecesora.
- Toda tabla mutable incluye `created_at`, `created_by`, `updated_at`, `updated_by`; los registros históricos no se eliminan físicamente.
- Códigos institucionales y nombres del piloto son datos configurables, no enumeraciones compiladas.

## Identidad y acceso (I0)

### UserAccount

Campos: `id`, `username` (único, normalizado), `display_name`, `password_hash`, `status` (`ACTIVE|DISABLED|LOCKED`), `failed_attempts`, `locked_until`, `last_login_at`, `version`.

Reglas: username obligatorio de 3–100 caracteres; desactivar no elimina autoría; el hash cumple RNF-001.

### Session

Campos: `id`, `user_id`, `secret_hash`, `created_at`, `last_seen_at`, `expires_at`, `revoked_at`, `client_fingerprint` opcional.

Reglas: solo se conserva hash del secreto; 30 minutos de inactividad; revocación en logout y desactivación.

### ProcessRole

Campos: `id`, `code` (único), `name`, `capabilities`, `technical` boolean, `active`, `version`.

Reglas: una función técnica no puede incluir capacidades `DOCUMENT_REVIEW_DECIDE` ni `ACADEMIC_EVALUATION_DECIDE`.

### ScopeAssignment

Campos: `id`, `user_id`, `role_id`, `career_id` opcional, `process_id` opcional, `indicator_snapshot_id` opcional, `valid_from`, `valid_until`, `status`, `version`.

Reglas: el ámbito más específico debe pertenecer al más general; vigencia no vacía; asignaciones superpuestas idénticas son inválidas.

### FunctionConflictPolicy / FunctionException

Política: `id`, `policy_version`, `left_capability`, `right_capability`, `same_object_only`, `active_from`, `published_at`.

Excepción: `id`, `policy_id`, `user_id`, ámbitos, `reason`, `authorized_by`, `valid_from`, `valid_until`, `status`.

Reglas: excepción siempre temporal, justificada y autorizada; no se concede a cuentas técnicas para decisiones académicas.

## Modelos de evaluación (I1)

### EvaluationModel / ModelVersion

Modelo: `id`, `code` (único), `name`, `issuer_organization`, `scope` (`GENERAL|SPECIFIC`), `status`.

Versión: `id`, `model_id`, `version_number`, `version_date`, `status` (`DRAFT|PUBLISHED|SUPERSEDED`), `predecessor_id`, `created_by`, `published_by`, `published_at`, `source_document`, `version`.

Reglas: `(model_id, version_number)` único; solo borrador editable; publicación exige organismo emisor, denominación, fecha, documento fuente y aprobador distinto del autor para el piloto; una versión publicada no cambia. Los modelos generales y específicos son independientes aunque compartan códigos.

### EvaluationNodeSnapshot / IndicatorDefinition

Nodo: `id`, `model_version_id`, `parent_id` opcional, `node_type` (`CRITERION|SUBCRITERION|STANDARD|INDICATOR`), `code`, `title`, `description`, `ordinal`, `mandatory`, `weight` opcional.

Indicador: `node_id`, `value_nature` (`QUALITATIVE|QUANTITATIVE`), `evaluation_period_definition`, `valuation_mechanism_key` opcional, `rating_scale_id` opcional.

Relaciones: una versión posee un bosque ordenado de nodos; `parent_id` pertenece a la misma versión y no puede formar ciclos. Solo un nodo `INDICATOR` posee `IndicatorDefinition`. Códigos únicos por tipo dentro de la versión. Todos son inmutables al publicar.

Convención referencial: los campos existentes denominados `indicator_snapshot_id` apuntan a un `EvaluationNodeSnapshot` cuyo `node_type=INDICATOR`; se conserva el nombre para expresar el rol de la referencia, no una tabla jerárquica separada.

### IndicatorInformationSource

Campos: `id`, `indicator_node_id`, `source_type`, `name`, `description`, `required`, `ordinal`.

Reglas: cada indicador publicado declara al menos una fuente; las fuentes pertenecen a un solo indicador y versión.

### RatingScale / RatingOption / ProgressFormula

Escala: `id`, `model_version_id`, `name`, `version_label`.

Opción: `id`, `scale_id`, `code`, `label`, `ordinal`, `numeric_value` opcional.

Fórmula: `id`, `model_version_id`, `formula_version`, `definition`, `strategy_key` opcional, `strategy_version` opcional, `closure_policy_version`, `closure_policy_definition`, `published_at`.

Reglas: al menos una opción; códigos y ordinales únicos; la fórmula del piloto calcula separadamente recopilación, revisión y valoración. Si declara estrategia automática, clave y versión son obligatorias y deben existir como registro validado en I7 antes de ejecutar. La política de cierre versionada exige indicadores obligatorios con valoración final válida, ausencia de rectificaciones pendientes y archivos referenciados en estado `AVAILABLE`.

## Proceso y asignaciones (I2)

### Career

Campos: `id`, `code` (único), `name`, `status`, `synthetic` boolean, `version`.

Regla MVP: exactamente una carrera `synthetic=true` en los fixtures de aceptación.

### SelfAssessmentProcess

Campos: `id`, `career_id`, `model_version_id`, `code`, `name`, `period_start`, `period_end`, `status` (`DRAFT|ACTIVE|CLOSING|CLOSED|REOPENED`), `opened_at`, `closed_at`, `version`.

Reglas: `model_version_id` debe estar publicado y no cambia tras activación; transición válida: `DRAFT→ACTIVE→CLOSING→CLOSED`, `CLOSED→REOPENED→ACTIVE` solo con autorización.

### ProcessAssignment

Campos: `id`, `process_id`, `indicator_snapshot_id` opcional, `user_id`, `role_id`, `valid_from`, `valid_until`, `status`, `version`.

Reglas: indicador pertenece al modelo del proceso; valida conflicto de funciones; cambios críticos generan auditoría.

### ReopeningDecision

Campos: `id`, `process_id`, `from_status`, `to_status`, `reason`, `authorized_by`, `decided_at`.

Regla: append-only y obligatoria para salir de `CLOSED`.

## Evidencia (I3)

### Evidence

Campos: `id`, `process_id`, `title`, `description`, `source`, `covered_from`, `covered_to`, `owner_id`, `archived_at` opcional, `version`.

Relación: N–M con `EvaluationNodeSnapshot` de tipo `INDICATOR` mediante `EvidenceIndicator`.

Reglas: la evidencia no persiste estados `ACCEPTED`, `OBSERVED` ni `REJECTED`. El estado documental mostrado es una proyección derivada de la última `DocumentReviewDecision` vigente para cada `EvidenceVersion`; nunca es una propiedad académica ni una fuente de verdad duplicada.

### EvidenceVersion

Campos: `id`, `evidence_id`, `revision_number`, `predecessor_id`, `status` (`DRAFT|SUBMITTED`), `title_snapshot`, `description_snapshot`, `source_snapshot`, `covered_from`, `covered_to`, `submitted_by`, `submitted_at`, `version`.

Reglas: `(evidence_id, revision_number)` único; una versión remitida es inmutable; predecesora pertenece a la misma evidencia.

### FileObject

Campos: `id`, `evidence_version_id`, `storage_key` (único y opaco), `original_name`, `declared_media_type`, `detected_media_type`, `size_bytes`, `sha256`, `status` (`STAGED|AVAILABLE|QUARANTINED|MISSING`), `created_at`.

Reglas: `size_bytes` entre 1 y 26 214 400; SHA-256 de 64 caracteres hexadecimales; no se persiste contenido binario.

### ExternalReference

Campos: `id`, `evidence_version_id`, `uri`, `label`, `accessed_at`, `integrity_note`.

Reglas: URI válida; una versión debe tener al menos un archivo o referencia antes de remitirse.

## Revisión documental (I4)

### DocumentReviewDecision

Campos: `id`, `evidence_version_id`, `revision_number`, `predecessor_id`, `outcome` (`ACCEPTED|OBSERVED|REJECTED`), `justification`, `reviewer_id`, `decided_at`, `status` (`FINAL|SUPERSEDED`).

Reglas: evidencia remitida; revisor autorizado y sin conflicto; justificación de 10–4000 caracteres; append-only; no contiene escala ni nivel académico.

## Valoración académica (I5)

### AcademicEvaluation

Campos: `id`, `process_id`, `indicator_snapshot_id`, `revision_number`, `predecessor_id`, `rating_option_id`, `justification`, `evaluator_id`, `decided_at`, `status` (`DRAFT|FINAL|SUPERSEDED`), `version` para borrador.

Relación: N–M con versiones de evidencia mediante `EvaluationEvidence`.

Reglas: `(process_id, indicator_snapshot_id, revision_number)` único; toda evidencia vinculada posee una decisión documental vigente `ACCEPTED`; opción pertenece a la escala del modelo del proceso; final append-only.

## Seguimiento y cierre (I6)

### ProgressSnapshot

Campos: `id`, `process_id`, `calculated_at`, `formula_id`, `collection_completed`, `collection_total`, `review_completed`, `review_total`, `evaluation_completed`, `evaluation_total`, `result_hash`.

Reglas: conteos no negativos; porcentajes derivados, no almacenados; mismos datos y fórmula producen mismo hash.

### ClosureSnapshot

Campos: `id`, `process_id` (único por cierre vigente), `model_version_id`, `formula_id`, `closed_by`, `closed_at`, `snapshot_json`, `sha256`, `predecessor_id` opcional.

Reglas: se crea solo al cerrar tras revalidar condiciones; inmutable; una reapertura conserva el cierre anterior.

### ReportExport

Campos: `id`, `closure_snapshot_id`, `requested_by`, `format`, `status`, `storage_key` opcional, `requested_at`, `completed_at`, `error_code` opcional.

Reglas: la falla no altera el proceso; la descarga revalida ámbito.

## Auditoría y extensiones

### AuditEvent

Campos: `id`, `occurred_at`, `actor_user_id` opcional, `technical_account_id` opcional, `correlation_id`, `action`, `object_type`, `object_id`, `scope_career_id` opcional, `scope_process_id` opcional, `outcome`, `reason_code` opcional, `metadata_json` sanitizado.

Reglas: append-only; actor humano o técnico obligatorio; no guarda secretos ni contenido de archivo.

### EvaluationStrategyRegistration (I7)

Campos: `id`, `strategy_key`, `strategy_version`, `compatible_mechanism`, `status` (`DRAFT|VALIDATED|RETIRED`), `implementation_reference`, `validated_by`, `validated_at`.

Reglas: `(strategy_key, strategy_version)` único; solo `VALIDATED` puede resolverse; retirar una estrategia impide nuevas ejecuciones pero conserva trazabilidad histórica; no contiene scripts o expresiones ejecutables aportadas por usuarios.

### ExchangeJob (I8)

Campos: `id`, `contract_version`, `direction`, `source`, `requested_by`, `status`, `validation_result`, `created_at`, `completed_at`.

Regla: validar paquete completo antes de aplicar; su ausencia no afecta I0–I6.

### NotificationEvent (I8)

Campos: `id`, `event_version`, `event_type`, `subject_type`, `subject_id`, `recipient_id`, `minimal_payload`, `occurred_at`, `delivery_status`.

Regla: no contiene evidencia ni justificaciones completas; una falla no cambia el agregado fuente.

## Integridad referencial y borrado

- Relaciones históricas usan `RESTRICT`; nunca cascada desde usuario, modelo, proceso, evidencia o decisión.
- Catálogos operativos se desactivan; no se borran si tienen referencias.
- Temporales de archivos sin metadatos confirmados pueden purgarse mediante trabajo de reconciliación auditado.
- Índices mínimos: claves de ámbito, estados por proceso, historial por predecesor, auditoría por objeto/fecha y SHA-256 de archivos.
