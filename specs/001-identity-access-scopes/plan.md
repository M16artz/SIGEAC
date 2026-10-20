# Implementation Plan: I0 — Acceso y ámbitos autorizados

**Branch**: `001-identity-access-scopes` | **Date**: 2026-10-20 | **Spec**: [spec.md](./spec.md)

## Summary

Implementar autenticación con sesiones opacas, identidades históricas, funciones y asignaciones con ámbitos/vigencias, autorización reevaluada en backend, conflictos configurables, excepciones temporales y auditoría. I0 expone puertos públicos consumibles por I1–I6 sin incorporar reglas de esos módulos.

## Technical Context

**Language/Version**: Java 25; TypeScript 6 con Angular 22.2; PostgreSQL 18  
**Primary Dependencies**: Spring Boot 4.1.1, Spring Security, Spring Data JPA, Flyway, Bean Validation, BCrypt, Angular Reactive Forms  
**Storage**: PostgreSQL; cookies contienen solo tokens opacos y la base conserva hashes SHA-256 de sesión  
**Testing**: JUnit 5, MockMvc, Spring Security Test, Testcontainers PostgreSQL, Vitest, Playwright  
**Target Platform**: Windows/Linux sin Docker obligatorio  
**Project Type**: cliente Angular + backend monolítico modular REST  
**Performance Goals**: p95 ≤2 s sin transferencia; reevaluación de ámbito por operación  
**Constraints**: 30 min de inactividad; cinco fallos en ventana móvil de 15 min; no enumeración; datos sintéticos; CL-001–CL-010 configurables  
**Scale/Scope**: piloto de 100 usuarios y 30 sesiones concurrentes

## Constitution Check

| Principle | Result | Evidence |
|---|---|---|
| I. Separación académica | PASS | Identidad, función y privilegio técnico son conceptos separados; I0 no decide revisión ni valoración. |
| II. Trazabilidad | PASS | FR-001–FR-006a y SC-001–SC-008 se vinculan a contrato, tareas y pruebas. |
| III. Incremento vertical | PASS | Incluye UI, REST, dominio, persistencia, migración y pruebas independientes. |
| IV. Seguridad por ámbito | PASS | Backend reevalúa función, carrera, proceso, indicador y vigencia en cada operación. |
| V. Auditoría | PASS | Cambios sensibles y rechazos producen eventos append-only; la identidad histórica no se elimina. |
| VI. Arquitectura modular | PASS | `identityaccess` publica puertos de aplicación; controladores y repositorios no contienen política académica. |
| VII. Calidad | PASS | Matrices automatizadas cubren sesiones, ámbitos, conflictos, privacidad y concurrencia. |

No se solicitan excepciones. Los catálogos piloto son configuración sintética.

## Project Structure

```text
backend/src/main/java/ec/edu/unl/sec/identityaccess/{api,application,domain,infrastructure}/
backend/src/main/resources/db/migration/V003__identity_access.sql
frontend/src/app/features/access/{data-access,pages,ui}/
tests/acceptance/access-*.spec.ts
```

**Structure Decision**: ampliar el monolito modular existente; `identityaccess` posee autenticación y autorización y usa `audit` mediante puerto.

## Design Outcomes

- [research.md](./research.md): sesiones, credenciales, limitación, autorización y auditoría.
- [data-model.md](./data-model.md): entidades, vigencias, índices y estados.
- [contracts/openapi.yaml](./contracts/openapi.yaml): sesión, identidad, usuarios y asignaciones.
- [quickstart.md](./quickstart.md): recorrido sintético y matrices negativas.

## Sequencing

1. Aprobar contrato/ADR de seguridad y crear V003 desde base limpia.
2. Implementar puertos públicos, sesión y autorización antes de administración.
3. Implementar asignaciones, conflictos, excepciones y auditoría.
4. Integrar UI y ejecutar matrices/aceptación con datos sintéticos.
