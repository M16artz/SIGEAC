# Data Model: I0 — Acceso y ámbitos autorizados

## Entities

- **AccessIdentity**: UUID, login normalizado único, displayName, tipo `HUMAN|TECHNICAL`, activo, revisión, timestamps. Nunca se elimina ni reutiliza.
- **Credential**: UUID, identityId único, `bcryptHash`, activo y timestamps. Ningún secreto recuperable.
- **AccessSession**: UUID, identityId, tokenHash único de 64 hex minúsculas, createdAt, lastActivityAt, expiresAt, revokedAt.
- **AuthenticationAttempt**: UUID, subjectKey/sourceKey HMAC, occurredAt, success, correlationId.
- **InstitutionalActor**: UUID, identityId, actorType configurable y vigencia.
- **ProcessFunction** y **FunctionOperation**: catálogo configurable de funciones y operaciones.
- **TechnicalPrivilege**: identidad, código y vigencia; nunca concede función de proceso.
- **FunctionalAssignment**: identityId, functionId, careerId requerido, processId/indicatorId opcionales, `validFrom < validTo`, grantedBy, revokedAt y revisión.
- **ConflictPolicy**: par normalizado de funciones, ámbito, vigencia y estado.
- **ConflictException**: policyId, identityId, ámbito, justification `10..2000`, validFrom/validTo, approvedBy y revokedAt.
- **AuditEvent**: tabla append-only V002, consumida mediante `AuditPort`.

## Invariants

- Sesión activa: identidad activa, no revocada y menos de 30 minutos desde última actividad.
- Las asignaciones efectivas son la unión de las vigentes/no revocadas salvo conflicto sin excepción vigente.
- Proceso implica carrera; indicador implica proceso. Nulos no amplían el ámbito.
- Desactivar identidad revoca sesiones en la misma transacción.
- Cambios concurrentes comparan revisión y nunca sobrescriben silenciosamente.

## Migration

V003 crea tablas, checks e índices sin catálogos institucionales definitivos.
