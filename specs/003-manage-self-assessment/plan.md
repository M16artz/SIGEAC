# Implementation Plan: I2 — Gestión del proceso de autoevaluación

**Branch**: `003-manage-self-assessment` | **Date**: 2026-10-20 | **Spec**: [spec.md](./spec.md)

## Summary

Crear procesos para carrera, período y versión publicada; fijar esa versión al activar; administrar responsables/revisores/evaluadores por indicador y vigencia; aplicar una máquina de estados con reapertura motivada; detectar concurrencia y auditar cambios críticos. I2 consume I0 para autorización/conflictos y `ModelCatalogPort` de I1.

## Technical Context

**Language/Version**: Java 25, TypeScript 6/Angular 22.2, PostgreSQL 18  
**Dependencies**: Spring Boot 4.1.1, JDBC, Validation, Angular Reactive Forms  
**Storage**: PostgreSQL con historial append-only de asignaciones/estados  
**Testing**: JUnit 5, MockMvc, PostgreSQL opcional, Vitest, Playwright  
**Platform**: Windows/Linux, sin Docker obligatorio  
**Performance**: p95 ≤2 s  
**Constraints**: modelo publicado; ETags; referencia de modelo inmutable desde activación; cambios críticos motivados; CL-001/002/007 configurables  
**Scale**: piloto con una carrera, 500 indicadores y 30 sesiones concurrentes

## Constitution Check

| Principle | Result | Evidence |
|---|---|---|
| Separación académica | PASS | I2 organiza proceso/asignaciones; no recopila, revisa ni valora. |
| Trazabilidad | PASS | FR-011–FR-015/FR-013a cubiertos por contrato, tareas y pruebas. |
| Incremento vertical | PASS | UI, REST, dominio, persistencia y pruebas. |
| Seguridad por ámbito | PASS | I0 decide operación, carrera, proceso, indicador, conflicto y vigencia. |
| Inmutabilidad/auditoría | PASS | Modelo fijado, historial de cierres/asignaciones y eventos append-only. |
| Arquitectura modular | PASS | Solo puertos públicos I0/I1/audit. |
| Calidad | PASS | Matrices de estado, ámbito, concurrencia y reapertura automatizadas. |

Sin excepciones constitucionales.

## Project Structure

```text
backend/src/main/java/ec/edu/unl/sec/selfassessment/{api,application,domain,infrastructure}/
backend/src/main/resources/db/migration/V005__self_assessment.sql
frontend/src/app/features/processes/{data-access,pages,ui}/
tests/acceptance/self-assessment-process.spec.ts
```

## Design Outcomes

- [research.md](./research.md): modelo fijado, estados y asignaciones históricas.
- [data-model.md](./data-model.md): proceso, transición, cierre y asignación.
- [contracts/openapi.yaml](./contracts/openapi.yaml): CRUD, activación, transición, reapertura y asignaciones.
- [quickstart.md](./quickstart.md): proceso v1 que no cambia al publicarse v2.

## Sequencing

Migración/puertos → borrador/activación → modelo fijado → asignaciones → estados/reapertura → auditoría/UI/verificación.

## Post-design Constitution Check

El diseño mantiene las siete puertas; las políticas institucionales abiertas son configuración y no constantes normativas.
