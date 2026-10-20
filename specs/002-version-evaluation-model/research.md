# Research: I1 — Modelo de evaluación versionado

## Agregado e instantánea publicada

**Decision**: una versión es el agregado; todos sus componentes referencian `model_version_id`. Solo `DRAFT` admite escrituras. La publicación valida y cambia estado en una transacción.  
**Rationale**: evita mezclar versiones y hace comprobable la inmutabilidad.  
**Alternatives considered**: nodos maestros compartidos permiten cambios retroactivos y se descartan.

## Fórmula declarativa

**Decision**: JSON versionado con identificador, resultados, entradas, política de cero/no aplicables y casos sintéticos. Un validador con allowlist comprueba estructura, referencias y determinismo; I1 no evalúa procesos reales.  
**Rationale**: conserva la fórmula sin inventar ponderaciones ni ejecutar código.  
**Alternatives considered**: scripts se rechazan por seguridad; columnas rígidas codificarían CL-006.

## Concurrencia e inmutabilidad

**Decision**: revisión optimista y `If-Match` en mutaciones. Los repositorios no actualizan/eliminan versiones publicadas y la base añade defensa equivalente.  
**Rationale**: detecta revisiones obsoletas y protege ante errores de aplicación.

## Sucesión y comparación

**Decision**: una sucesora copia el contenido a un borrador con número monotónico y `predecessor_id`; publicarla marca la anterior `SUPERSEDED` sin cambiar contenido. Comparar por `(tipo,código normalizado)` produce `ADDED|REMOVED|MODIFIED|UNCHANGED`.  
**Rationale**: resultado reproducible sin equiparar modelos diferentes.

## Autorización y auditoría

**Decision**: `MODEL_READ`, `MODEL_WRITE`, `MODEL_PUBLISH`, `MODEL_AUDIT_READ` se resuelven mediante I0. Acciones y rechazos críticos usan `AuditPort`; una consulta sensible sin auditoría no entrega resultado.  
**Rationale**: cumple ámbito sin acoplar I1 a infraestructura ajena.
