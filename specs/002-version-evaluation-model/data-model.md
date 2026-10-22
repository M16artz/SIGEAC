# Data Model: I1 — Modelo de evaluación versionado

## Entities

- **EvaluationModel**: UUID, nombre `1..200`, descripción `<=2000`, createdBy, createdAt.
- **ModelVersion**: UUID, modelId, número positivo único, estado `DRAFT|PUBLISHED|SUPERSEDED`, fuente `1..2000`, authorId, approverId opcional, predecessorId opcional, revisión, fechas.
- **ModelNode**: UUID, versionId, parentId opcional, tipo versionado, código normalizado único por tipo/versión, nombre `1..300`, descripción `<=4000`, posición positiva.
- **IndicatorDefinition**: pertenece a un `ModelNode` evaluable y declara estándar, naturaleza, período, fuentes requeridas, escala y regla aplicable.
- **RequiredSource**: identidad, código, tipo, nombre, descripción, cardinalidad y regla de período dentro de un indicador.
- **ValuationRule / Variable / Threshold**: metodología manual o cuantitativa por indicador, con estado de validación.
- **RatingScale** y **ScaleOption**: escala única por versión; opciones con código, etiqueta `1..200`, posición y valor declarado opcional.
- **DocumentaryProgressFormula**: versión de las fórmulas de recopilación y revisión por fuentes aplicables, con tratamiento explícito de denominador cero.
- **ModelAuditEvent**: se registra mediante el agregado append-only de `audit`.

## Relationships and constraints

- Cada nodo pertenece a una única versión; padre e hijo comparten `version_id`.
- Número de versión, códigos normalizados por tipo y posiciones entre hermanos son únicos.
- Un borrador puede estar incompleto. Publicar exige procedencia, autor, aprobador distinto, jerarquía sin ciclos, indicadores alcanzables y fuentes/metodologías completas según su naturaleza.
- `predecessor_id` referencia una versión publicada/sustituida del mismo modelo y no forma ciclos.
- El contenido publicado no admite `UPDATE`/`DELETE`; solo `PUBLISHED → SUPERSEDED` al publicar sucesora.
- Toda mutación compara la revisión esperada y la incrementa una vez.

## State transitions

```text
DRAFT --publish(valid, independent approver)--> PUBLISHED
PUBLISHED --publish(successor)---------------> SUPERSEDED
PUBLISHED|SUPERSEDED --create successor------> new DRAFT
```

No hay retorno a borrador, eliminación histórica ni comparación entre modelos distintos.
