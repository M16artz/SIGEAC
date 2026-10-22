# Data Model: Flujos completos de evaluación

## Conventions

- UUID, fechas UTC, bloqueo optimista en borradores y registros finales append-only.
- Toda decisión conserva actor, función, autoridad aplicada, fundamento, fecha y correlación.
- Toda relación histórica usa restricción; no hay borrado en cascada de registros de dominio.
- Las longitudes, tipos y cuerpos del [contrato objetivo](contracts/openapi.yaml) son restricciones de interfaz; las reglas cruzadas se definen en [domain-rules.md](../../docs/requirements/domain-rules.md). Ningún estado de este documento afirma implementación disponible.

## Organization and careers

### AcademicUnit

`id`, `code`, `name`, `status`, `version`.

### Career

`id`, `academic_unit_id`, `code`, `name`, `status`, `synthetic`, `valid_from`, `valid_until`, `version`.

Código normalizado único; una carrera referenciada solo se desactiva.

## Configurable models

### EvaluationModel / ModelVersion

El modelo identifica código, denominación y alcance. La versión registra emisor, edición, fecha, documento fuente, estado técnico, estado de vigencia, predecesora, autor, revisor y aprobador.

### NormativeReference / ModelCareerApplicability

Referencia: `id`, `title`, `issuer`, `edition`, `document_reference`, `document_sha256` opcional, `disposition`, `verification_status`, `verified_by`, `verified_at`.

Uniones explícitas de referencia con versión e indicador. La fecha/edición desconocida se registra como pendiente, sin inventar valores. La verificación requiere usuario, fecha y disposición corroborada.

Aplicabilidad: `id`, `model_version_id`, `career_id`, `status` (`PENDING_VALIDATION|APPLICABLE|NOT_APPLICABLE|EXPIRED`), `valid_from`, `valid_until`, `decision_id`, `predecessor_id`. Unicidad y vigencias sin contradicciones por carrera/versión.

### ModelNode

`id`, `model_version_id`, `parent_id`, `node_type`, `code`, `title`, `description`, `ordinal`, `mandatory`, `weight`.

Forma un bosque ordenado sin ciclos. `node_type` es catálogo versionado y admite `CRITERION`, `SUBCRITERION`, `STANDARD`, `INDICATOR` y categorías futuras. Un indicador puede depender de cualquier padre permitido por la versión.

### IndicatorDefinition

`node_id`, `value_nature`, `standard_text`, `evaluation_period_definition`, `rating_scale_id`, `valuation_rule_id`.

### RequiredSource

`id`, `indicator_node_id`, `code`, `source_type`, `name`, `description`, `required`, `cardinality_min`, `cardinality_max`, `period_rule`, `ordinal`.

Código único por indicador; `cardinality_min >= 1`; máximo nulo o >= mínimo. La cardinalidad cuenta evidencias distintas aceptadas, no archivos duplicados dentro de una misma evidencia. `source_type` admite archivo, enlace, dato estructurado o referencia institucional. Una fuente opcional no entra al denominador.

### ValuationRule / ValuationVariable / Threshold

Regla: `id`, `indicator_node_id`, `rule_type`, `strategy_key`, `strategy_version`, `formula_definition`, `missing_data_policy`, `status`.

Variable: `id`, `rule_id`, `code`, `name`, `unit`, `data_type`, `required`, `validation_definition`.

Umbral: `id`, `rule_id`, `rating_option_id`, `lower_bound`, `upper_bound`, inclusividad y orden.

Solo una regla `VALIDATED` puede calcular automáticamente.

## Process governance and planning

### SelfAssessmentProcess

Añade estados `PREPARATION`, `ACTIVE`, `SUSPENDED`, `DOCUMENTARY_CLOSED`, `CLOSING`, `CLOSED`, `REOPENED`.

Las transiciones permitidas son exactamente las de [reglas de proceso](../../docs/requirements/domain-rules.md#proceso). Un corte de seguimiento no cambia estado; después del cierre documental P12–P15 pueden continuar. Mapear `DRAFT` histórico a `PREPARATION` no crea una autorización retroactiva.

### ProcessAuthorization

`id`, `process_id`, `decision_type`, `authority_function`, `authority_user_id`, `legal_basis`, `document_reference`, `decision`, `reason`, `decided_at`, `predecessor_id`.

### ProcessActivity / TeamMembership

Actividad: `id`, `process_id`, `indicator_node_id`, `activity_type`, `title`, `due_at`, `status`, `assigned_team_id`, `version`.

Membresía: equipo, usuario, función, vigencia y otorgante. Reasignar no borra la membresía anterior.

`Team`: `id`, `process_id`, `name`, `process_role`, `valid_from`, `valid_until`, `version`. Cada asignación tiene exactamente una persona o un equipo. Las actividades no pueden tener ciclos de dependencias ni fecha anterior al inicio sin excepción autorizada. Las vigencias usan intervalos inequívocos y respetan período y recepción configurados.

## Evidence and documentary review

### EvidenceVersion

Conserva el snapshot ya definido y se amplía con relaciones `SourceCoverage`.

### SourceCoverage

`id`, `evidence_version_id`, `required_source_id`, `coverage_note`, `declared_by`, `declared_at`.

Única por versión y fuente. La fuente pertenece a un indicador asociado a la misma versión y al modelo fijado por el proceso.

### StructuredSourceValue / InstitutionalReference

Relacionan la versión y fuente con valor tipado, unidad opcional, período y referencia. El valor requerido no puede ser vacío; nunca se supone que un enlace implica contenido incorporado. Se incluyen en el snapshot remitido y mantienen la misma inmutabilidad que los archivos.

### ApplicabilityDecision

`id`, `process_id`, `subject_type`, `subject_id`, `decision`, `rule_reference`, `justification`, `authorized_by`, `decided_at`, `predecessor_id`.

### DocumentReviewDecision

Permanece separada de `AcademicEvaluation`. La cobertura documental de una fuente se deriva de una versión que la cubra y posea una decisión vigente `ACCEPTED`.

`CoverageReviewOutcome`: `decision_id`, `source_coverage_id`, `outcome`, `justification`; única por decisión/cobertura. Toda decisión final cubre exactamente las coberturas de esa versión. La aceptación por fuente procede de estas filas; el resultado global se deriva. Una rectificación crea otra decisión con predecesora; nunca dos decisiones vigentes para la misma versión.

## Progress

### ProgressSnapshot

`required_source_total`, `registered_source_total`, `accepted_source_total`, `academic_total`, `academic_completed`, `formula_version`, `calculated_at`, `result_hash`.

`collection_percent = registered_source_total / required_source_total * 100`.

`document_review_percent = accepted_source_total / required_source_total * 100`.

Cuando el total es cero, el resultado es `NOT_CONFIGURED` o `NOT_APPLICABLE`; nunca 100 por defecto.

Los conjuntos usan IDs distintos de fuentes obligatorias y candidatas remitidas vigentes por evidencia. La aceptación previa deja de sumar para esa evidencia cuando se remite una sucesora, no al crear su borrador. La política de cardinalidad, redondeo y temporalidad completa está en [reglas de cálculo](../../docs/requirements/domain-rules.md#cálculo-documental-sin-ambigüedad).

### DocumentaryClosureSnapshot

Instantánea opcional del MVP con versión de modelo, fuentes aplicables, coberturas, decisiones y métricas. No afirma cumplimiento académico.

Se distinguen `DOCUMENTARY_SNAPSHOT` (sin transición) y `DOCUMENTARY_CLOSURE` (fin de recepción/revisión ordinaria conforme a política). Ambos conservan pendientes y decisiones; solo el segundo actualiza el estado de proceso.

## Academic evaluation

### VariableValue

`id`, `process_id`, `indicator_node_id`, `variable_id`, `value`, `unit`, `period`, `verified_by`, `verified_at`, `source_reference`.

### AcademicEvaluation

Añade `method`, `rule_id`, `calculation_run_id`, `limitation`, `status` y discrepancias. Una valoración manual requiere escala y justificación; una cuantitativa referencia entradas inmutables.

### CalculationRun

`id`, `rule_id`, `strategy_key`, `strategy_version`, `input_snapshot`, `result`, `rating_option_id`, `executed_at`, `validated_by`.

### AcademicDisagreement / ReconciliationDecision

Discrepancia relaciona proceso, indicador, evaluaciones y motivos. La resolución conserva procedimiento, participantes, evaluación resultante, autoridad, fecha y fundamento. No elimina los juicios anteriores. Sin resolución aplicable no hay resultado consolidado final.

## Findings and reports

### Finding

`id`, `process_id`, `type` (`STRENGTH|WEAKNESS|OBSERVATION`), `title`, `description`, `status`, `created_by`, `created_at`, `version`.

Relaciones con indicadores, evaluaciones y evidencias mediante tablas explícitas.

### Report / ReportVersion

Informe identifica tipo y proceso. Versión conserva número, estado (`DRAFT|PRELIMINARY|UNDER_OBSERVATION|FINAL|APPROVED|RETURNED`), contenido o archivo, autor, fecha y predecesora.

Separar borrador editable de versión emitida inmutable. El estado de tramitación es proyección de eventos anexos; pasar de `FINAL` a `APPROVED` no modifica contenido ni aprobación de versiones previas.

### ReportObservation / ApprovalDecision

Observación conserva emisor, texto, fecha, respuesta y versión. Decisión conserva autoridad, fundamento, resultado, fecha y versión exacta; nunca representa acreditación.

## Improvement

### ImprovementPlan / ImprovementAction / ActionProgress

Plan pertenece a proceso e informe. Acción vincula hallazgos y registra responsable, fechas, meta, indicador de cumplimiento, medio de verificación y estado. Cada avance y modificación conserva autor, fecha, valor, evidencia y predecesor cuando aplique.

Estados de acción: `PROPOSED|APPROVED|IN_PROGRESS|PENDING_VERIFICATION|VERIFIED`; vencimiento derivado. Verificación y decisión se añaden, no editan una revisión previa. El cierre integral puede permitir que acciones aprobadas continúen, conforme a política, sin alterar la instantánea de evaluación.

## Integral closure and archive

### ClosurePolicy / ClosureSnapshot

La política versionada declara prerrequisitos y excepciones. La instantánea integral referencia modelo, resultados, informes, mejoras, decisiones, actor, fecha y hash.

### ArchiveRecord

`id`, `process_id`, `closure_snapshot_id`, `retention_policy`, `archived_by`, `archived_at`, `integrity_hash`, `status`.

No permite modificación ordinaria. Una rectificación crea reapertura o registro sucesor.

