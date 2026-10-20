# Data Model: I1 — Modelo de evaluación versionado

## Entities

- **EvaluationModel**: UUID, nombre `1..200`, descripción `<=2000`, createdBy, createdAt.
- **ModelVersion**: UUID, modelId, número positivo único, estado `DRAFT|PUBLISHED|SUPERSEDED`, fuente `1..2000`, authorId, approverId opcional, predecessorId opcional, revisión, fechas.
- **Criterion**: UUID, versionId, código normalizado único por tipo/versión, nombre `1..300`, descripción `<=4000`, posición positiva.
- **Standard**: igual, subordinado a criterio de la misma versión.
- **Indicator**: igual, subordinado a estándar de la misma versión; requisitos documentales declarativos opcionales.
- **RatingScale** y **ScaleOption**: escala única por versión; opciones con código, etiqueta `1..200`, posición y valor declarado opcional.
- **ProgressFormula**: única por versión; código, versión de esquema y JSON con resultados, entradas, tratamiento de cero/no aplicables y casos sintéticos.
- **ModelAuditEvent**: se registra mediante el agregado append-only de `audit`.

## Relationships and constraints

- Cada nodo pertenece a una única versión; padre e hijo comparten `version_id`.
- Número de versión, códigos normalizados por tipo y posiciones entre hermanos son únicos.
- Un borrador puede estar incompleto. Publicar exige fuente, autor, aprobador distinto, jerarquía completa, escala con opción y fórmula válida.
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
