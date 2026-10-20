# Research: I2

## Modelo fijado

**Decision**: el borrador referencia una versión publicada consultada por `ModelCatalogPort`; al activar se copian `modelVersionId`, número e IDs de indicadores a una instantánea del proceso.  
**Rationale**: publicar/sustituir I1 después no cambia el proceso.  
**Alternatives**: resolver siempre “última versión” se descarta por pérdida de reproducibilidad.

## Estados y concurrencia

**Decision**: máquina explícita `DRAFT→ACTIVE→CLOSING→CLOSED→REOPENED→ACTIVE`, revisión optimista e historial de transición. Reabrir requiere motivo y operación configurada en I0.  
**Rationale**: rechaza transiciones implícitas/duplicadas y conserva cierres anteriores.

## Asignaciones

**Decision**: historial propio de participación del proceso, con función `RESPONSIBLE|REVIEWER|EVALUATOR`, indicador de la instantánea, vigencia solapada y revisión/revocación. Antes de escribir, I2 solicita a I0 autorización y validación de conflicto; nunca crea privilegios técnicos.  
**Rationale**: separa organización del proceso de las asignaciones generales I0 y conserva cambios críticos.

## Auditoría

**Decision**: creación, activación, transición, reapertura, asignación/revocación y rechazos usan `AuditPort` en transacciones; motivo obligatorio para cambios críticos cuando el proceso no está en borrador.
