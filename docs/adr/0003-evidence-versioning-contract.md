# ADR-0003: Versionado, seguridad y contrato público de evidencias

**Estado**: Aceptado  
**Fecha**: 2026-10-20

## Contexto

I3 necesita editar borradores sin perder cambios concurrentes, remitir snapshots
inmutables, corregir por sucesión y conservar archivos fuera de PostgreSQL. El
contrato también debe ocultar la existencia de recursos fuera del ámbito y
mantener la auditoría obligatoria sin fingir una transacción distribuida con el
almacenamiento.

## Decisión

- `Evidence` es la raíz estable y `EvidenceVersion` contiene el snapshot. Solo
  `DRAFT` es mutable; `SUBMITTED` y sus hijos son append-only.
- Cada sucesora apunta a una única predecesora del mismo `Evidence`; una
  restricción única impide ramas directas duplicadas.
- Toda mutación de borrador exige un ETag fuerte y `If-Match`. Una revisión
  ausente produce 428 y una obsoleta 412.
- Cada operación vuelve a autorizar actor, proceso e indicadores mediante
  puertos públicos de I0/I2. Un identificador desconocido o fuera de ámbito
  devuelve la misma forma 404 y nunca expone claves de almacenamiento.
- La remisión y su auditoría exitosa comparten transacción PostgreSQL. Los
  rechazos posteriores a rollback usan una transacción independiente con
  metadatos sanitizados.
- El contrato REST canónico de I3 es
  `specs/004-submit-evidence/contracts/openapi.yaml`; el contrato global enlaza
  sus Path Items para evitar divergencia de operaciones y esquemas.
- Las referencias externas son declaraciones estructuralmente válidas. I3 no
  las desreferencia ni afirma disponibilidad o integridad.

## Consecuencias

- Una corrección crea v2 y nunca sobrescribe v1.
- Los triggers defensivos se agregan en una migración nueva; una migración
  Flyway aplicada nunca se modifica.
- PostgreSQL y el almacenamiento se coordinan con estados durables,
  idempotencia y reconciliación, no con una transacción distribuida.
- La autorización requiere capacidades ejecutables de I0/I2 antes de crear
  migraciones, JPA o controladores integrados de I3.

## Alternativas descartadas

- Editar una versión remitida: destruye trazabilidad.
- Reintentar automáticamente un ETag obsoleto: puede perder cambios.
- Revelar 403 para IDs directos fuera de ámbito: permite enumeración.
- Guardar binarios en PostgreSQL: contradice la constitución.
- Comprobar URLs externas: introduce integración y riesgo SSRF fuera de I3.
