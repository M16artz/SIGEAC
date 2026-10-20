# Implementation Plan: I1 — Modelo de evaluación versionado

**Branch**: `002-version-evaluation-model` | **Date**: 2026-10-20 | **Spec**: [spec.md](./spec.md)

## Summary

Implementar modelos y versiones editables solo en borrador, jerarquías completas, escala y fórmula declarativa validable; publicar instantáneas inmutables con aprobación independiente; crear sucesoras, comparar versiones y auditar el ciclo completo. I1 consume únicamente los puertos públicos de autorización y auditoría de I0.

## Technical Context

**Language/Version**: Java 25; TypeScript 6 con Angular 22.2; PostgreSQL 18  
**Primary Dependencies**: Spring Boot 4.1.1, Spring Data JPA/JDBC, Bean Validation, Angular Reactive Forms  
**Storage**: PostgreSQL; JSONB solo para fórmula declarativa y diferencias  
**Testing**: JUnit 5, MockMvc, Testcontainers PostgreSQL opcional, Vitest, Playwright  
**Target Platform**: Windows/Linux con camino sin Docker  
**Project Type**: cliente Angular + backend monolítico modular REST  
**Performance Goals**: p95 ≤2 s para operaciones interactivas del piloto  
**Constraints**: `If-Match`; publicación atómica; contenido publicado inmutable; datos sintéticos; CL-001–CL-010 configurables  
**Scale/Scope**: hasta 500 indicadores en una versión del piloto

## Constitution Check

| Principle | Result | Evidence |
|---|---|---|
| I. Separación académica | PASS | I1 define el modelo; no recopila evidencia, revisa ni valora. |
| II. Trazabilidad | PASS | FR-007–FR-010/FR-008a se conectan con contrato, tareas, pruebas y quickstart. |
| III. Incremento vertical | PASS | Incluye UI, REST, dominio, persistencia, migración y pruebas. |
| IV. Seguridad por ámbito | PASS | Usa puertos públicos de I0; el frontend no decide permisos. |
| V. Inmutabilidad y auditoría | PASS | Publicación congela contenido; corrección crea sucesora y eventos append-only. |
| VI. Arquitectura modular | PASS | `evaluationmodel` no importa infraestructura de I0 ni auditoría. |
| VII. Calidad | PASS | Reglas críticas tendrán pruebas sintéticas. |

No se solicitan excepciones. Escalas y fórmulas piloto son configuración con procedencia, no normativa institucional.

## Project Structure

```text
backend/src/main/java/ec/edu/unl/sec/evaluationmodel/{api,application,domain,infrastructure}/
backend/src/main/resources/db/migration/V004__evaluation_model.sql
frontend/src/app/features/models/{data-access,pages,ui}/
tests/acceptance/model-versioning.spec.ts
```

**Structure Decision**: ampliar el monolito modular. I1 publica consultas de versiones/indicadores mediante puerto para I2–I6 y consume `AuthorizationPort`, `CurrentActorPort` y `AuditPort`.

## Design Outcomes

- [research.md](./research.md): instantáneas, fórmula, comparación y concurrencia.
- [data-model.md](./data-model.md): agregados, estados y transición de publicación.
- [contracts/openapi.yaml](./contracts/openapi.yaml): creación, edición, publicación, sucesión y comparación.
- [quickstart.md](./quickstart.md): recorrido v1 inmutable → v2 sucesora.

## Sequencing

1. Conciliar contrato y migración V004 con pruebas desde base limpia.
2. Implementar agregado y validación de publicación antes de controladores/UI.
3. Incorporar autorización, auditoría, sucesión y comparación.
4. Ejecutar regresión I0+I1, aceptación y revisión constitucional.

## Post-design Constitution Check

El diseño conserva separación modular, autorización en backend, publicación atómica, historial íntegro y verificabilidad. No quedan aclaraciones técnicas abiertas.
