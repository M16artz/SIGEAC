# Research: I0 — Acceso y ámbitos autorizados

## Credenciales y sesiones

**Decision**: BCrypt con coste configurable y sal aleatoria embebida; token opaco de 256 bits en cookie `HttpOnly`, `Secure` configurable y `SameSite=Lax`; PostgreSQL guarda solo SHA-256 del token.  
**Rationale**: Spring Security lo soporta directamente y permite revocación inmediata y expiración por inactividad.  
**Alternatives considered**: cifrado reversible, hashes rápidos y JWT autocontenido se rechazan por seguridad o revocación tardía.

## Limitación de autenticación

**Decision**: registrar fallos por clave HMAC del identificador normalizado y origen, contar cinco fallos consecutivos dentro de 15 minutos y responder igual para identidades existentes e inexistentes.  
**Rationale**: reduce enumeración y conserva auditoría sin guardar contraseñas.  
**Alternatives considered**: contador en memoria no sobrevive reinicios; bloqueo permanente contradice la ventana móvil.

## Autorización

**Decision**: `AuthorizationPort` recibe operación y ámbito opcional; consulta asignaciones, función/operaciones, conflicto/excepción y vigencia en cada llamada. Colecciones se filtran y recursos directos fuera de ámbito se ocultan como inexistentes.  
**Rationale**: los permisos no quedan congelados en la sesión y los consumidores no acceden a tablas internas.

## Conflictos, auditoría y concurrencia

**Decision**: políticas versionadas/configurables; excepciones finitas con autoridad, motivo y ámbito. Cambios aceptados y auditoría comparten transacción; rechazos usan evento sanitizado independiente. Asignaciones usan revisión optimista y revocación histórica.  
**Rationale**: CL-001–CL-010 no se fijan en código, ninguna operación sensible se presenta completa sin evento y no hay pérdida silenciosa.
