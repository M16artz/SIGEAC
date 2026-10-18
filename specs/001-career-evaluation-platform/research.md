# Investigación y decisiones técnicas

**Fecha**: 2026-10-18

## Versiones base

### Decisión: Java 25 LTS y Spring Boot 4.1.x

**Razonamiento**: Java 25 es una versión LTS y Spring Boot 4.1.1 declara compatibilidad hasta Java 26. La combinación ofrece soporte vigente sin abandonar la restricción Java LTS.

**Alternativas consideradas**: Java 21 con Spring Boot 3.5.x, válida pero con una ventana tecnológica más corta para un proyecto nuevo; Java 27, descartada por no ser LTS.

**Fuentes**: Oracle Java SE Support Roadmap; documentación oficial de requisitos de Spring Boot.

### Decisión: Angular 22

**Razonamiento**: es la línea activa a la fecha de planificación y su matriz oficial define Node.js y TypeScript compatibles. Se fijarán versiones exactas en lockfile y se actualizarán solo mediante tareas explícitas.

**Alternativas consideradas**: Angular 21 LTS, estable pero con menor ventana restante; Angular 20, próximo al final de soporte.

**Fuente**: documentación oficial de versiones y calendario de Angular.

### Decisión: PostgreSQL 18

**Razonamiento**: es la versión estable actual, con soporte hasta 2030, y está soportada por Flyway. Se utilizará el último parche 18.x disponible en cada entorno.

**Alternativas consideradas**: PostgreSQL 17, estable pero con un año menos de soporte; versiones preliminares 19, descartadas.

**Fuentes**: política de versionado y documentación oficial de PostgreSQL; matriz oficial de Flyway.

## Arquitectura y límites

### Decisión: monolito modular por capacidades

**Razonamiento**: el MVP tiene un único equipo, una base transaccional y recorridos fuertemente consistentes. Los módulos conservan límites de dominio sin el costo operativo de servicios distribuidos.

**Alternativas consideradas**: microservicios, descartados por complejidad y porque contradirían la constitución; arquitectura por capas globales, descartada por facilitar acoplamiento transversal.

### Decisión: API REST síncrona y eventos internos post-commit

**Razonamiento**: REST es el contrato constitucional. Los casos de uso críticos requieren respuesta transaccional; los eventos internos sirven para auditoría y futuros puertos I7–I8 sin convertir un bus externo en dependencia.

**Alternativas consideradas**: GraphQL, no aprobado como contrato común; broker externo, innecesario para el piloto.

## Identidad y autorización

### Decisión: sesión opaca revocable con cookie segura

**Razonamiento**: permite cumplir expiración por inactividad, cierre de sesión y desactivación inmediata sin introducir un proveedor institucional aún no confirmado. El backend mantiene autoridad completa y usa CSRF en mutaciones.

**Alternativas consideradas**: JWT autocontenido, descartado por revocación compleja; SSO institucional, reservado como adaptador futuro por CL-010.

### Decisión: política RBAC con ámbito y reglas de conflicto

**Razonamiento**: la función otorga capacidad y el ámbito restringe objetos por carrera/proceso/indicador. Una política versionada identifica conflictos y excepciones temporales auditadas.

**Alternativas consideradas**: roles globales simples, insuficientes; ABAC totalmente genérico, innecesariamente complejo para el piloto.

## Persistencia, archivos y auditoría

### Decisión: esquema relacional normalizado con revisiones append-only

**Razonamiento**: claves y restricciones expresan identidad, unicidad y transiciones. Los registros publicados o finalizados se añaden y nunca se sobrescriben; los borradores usan bloqueo optimista.

**Alternativas consideradas**: event sourcing completo, descartado por costo; documentos JSON como fuente principal, descartados por menor integridad referencial.

### Decisión: puerto de almacenamiento con adaptador local inicial

**Razonamiento**: cumple archivos fuera de PostgreSQL y desarrollo sin Docker. El adaptador usa rutas opacas, temporales, promoción atómica cuando el sistema lo permite y huella SHA-256. Un adaptador S3 puede añadirse sin cambiar dominio.

**Alternativas consideradas**: bytea/large objects en PostgreSQL, prohibido; S3 obligatorio, descartado por dependencia operativa del MVP.

### Decisión: auditoría de aplicación inmutable

**Razonamiento**: un evento se guarda en la misma transacción que el cambio crítico, con actor efectivo, cuenta técnica si aplica, objeto, acción, resultado y correlación. La base revoca `UPDATE/DELETE` sobre la tabla al usuario de aplicación.

**Alternativas consideradas**: solo logs de servidor, insuficientes para trazabilidad; triggers para toda semántica, descartados porque perderían contexto del caso de uso.

## Consistencia y errores

### Decisión: bloqueo optimista y precondiciones HTTP

**Razonamiento**: `version`/ETag permite detectar edición concurrente y responder 409/412 sin pérdida silenciosa. El cierre obtiene bloqueo pesimista breve para revalidar condiciones.

**Alternativas consideradas**: último escritor gana, contrario a RNF-011; bloqueo pesimista general, perjudica concurrencia.

### Decisión: RFC 9457 Problem Details con códigos de dominio

**Razonamiento**: ofrece forma estable, correlación y detalles seguros. Los códigos documentados permiten pruebas y mensajes localizables sin filtrar datos.

**Alternativas consideradas**: estructuras ad hoc por endpoint, descartadas por inconsistencia.

## Verificación

### Decisión: pirámide de pruebas con PostgreSQL real

**Razonamiento**: JUnit prueba reglas; integración valida migraciones y consultas sobre PostgreSQL; contratos validan OpenAPI; Playwright cubre recorridos; k6 mide umbrales. Existe un camino local sin Docker y Testcontainers es acelerador opcional.

**Alternativas consideradas**: H2 como sustituto de aceptación, descartado por diferencias SQL; pruebas solo E2E, lentas y poco diagnósticas.

### Decisión: CI exclusivamente manual

**Razonamiento**: mantiene `workflow_dispatch` en cada workflow, conforme a la constitución. Los mismos comandos se ejecutan localmente antes de invocar CI.

**Alternativas consideradas**: triggers por push o pull request y despliegue continuo, prohibidos mientras la regla esté vigente.

## Decisiones no resueltas técnicamente

No quedan `NEEDS CLARIFICATION` técnicas. CL-001–CL-010 son validaciones institucionales documentadas en la especificación; el diseño evita convertir sus valores piloto en normativa fija.
