# Implementation Plan: I3 — Recopilación y remisión de evidencias

**Branch**: `004-submit-evidence` | **Date**: 2026-10-20 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/004-submit-evidence/spec.md`

## Summary

Implementar el recorrido vertical I3 para que un responsable autorizado cree una evidencia y su borrador v1, edite metadatos e indicadores, agregue archivos o referencias externas, remita una versión inmutable, descargue archivos dentro de su ámbito y cree una v2 sucesora sin alterar el historial. El backend amplía los módulos `evidence` y `filestorage` dentro del monolito modular; PostgreSQL conserva identidad, snapshots, integridad, estados operacionales y auditoría, mientras que los bytes permanecen tras `FileStoragePort` con un adaptador local reemplazable.

La carga usa staging, cálculo SHA-256 y detección de tipo en streaming, política configurable y promoción atómica por objeto. PostgreSQL y el almacenamiento no simulan una transacción distribuida: estados durables, operaciones idempotentes y reconciliación resuelven cada ventana de interrupción sin exponer referencias falsas. La remisión solo consume archivos `AVAILABLE`, crea el snapshot y su auditoría en una transacción relacional y aplica ETag/`If-Match` para evitar pérdida silenciosa.

## Technical Context

**Language/Version**: Java 25 LTS; TypeScript 6.0 con Angular 22.2; SQL PostgreSQL 18

**Primary Dependencies**: Spring Boot 4.1.1, Spring MVC, Spring Security, Spring Data JPA/Hibernate, Flyway, Bean Validation, springdoc/OpenAPI 3.1, Apache Tika 3.x para detección reproducible de contenido, Angular Reactive Forms, RxJS 7.8 y Playwright para aceptación web

**Storage**: PostgreSQL 18 para metadatos, snapshots, estados, integridad y auditoría; sistema de archivos local fuera de PostgreSQL detrás de `FileStoragePort`, compatible semánticamente con un adaptador S3 futuro pero no requerido por I3

**Testing**: JUnit 5, Spring Test/MockMvc, Spring Security Test, AssertJ, ArchUnit, Testcontainers PostgreSQL; Vitest y Angular Testing Library; Playwright para recorridos E2E; k6 para carga reproducible; archivos y participantes exclusivamente sintéticos

**Target Platform**: servidor Linux o Windows con Java y PostgreSQL; navegadores Chrome, Edge y Firefox soportados por el proyecto; desarrollo y pruebas sin dependencia obligatoria de Docker

**Project Type**: aplicación web cliente-servidor con backend monolítico modular, API REST y cliente Angular

**Performance Goals**: p95 menor o igual a 2 s para operaciones interactivas sin transferencia; p95 menor o igual a 15 s para completar o rechazar cargas de hasta 25 MiB; descarga verificada sin entregar bytes corruptos

**Constraints**: límite de dominio exacto de 26 214 400 bytes por archivo; binarios fuera de PostgreSQL; promoción atómica dentro del backend de almacenamiento y consistencia recuperable con la base; versiones remitidas append-only; autorización de backend por función, proceso e indicador; datos exclusivamente sintéticos; sin revisión documental, valoración, avance ni cierre

**Scale/Scope**: piloto con 100 usuarios, 30 sesiones concurrentes, 500 indicadores, hasta 10 000 versiones de evidencia y 100 GiB de archivos; una carrera piloto, un modelo publicado y un proceso activo sintéticos para aceptación

**Prerequisites**: I0 debe exponer identidad/autorización por ámbito y sus migraciones; I2 debe exponer proceso, versión de modelo fijada, indicadores, asignaciones vigentes y sus migraciones. Esta es una puerta obligatoria antes de crear `V006`, persistencia JPA, controladores o recorridos integrados de I3. Solo las pruebas aisladas de dominio/almacenamiento pueden adelantarse con dobles explícitos; I3 no recrea tablas ni reglas de I0/I2.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

### Gate before research

| Principle | Result | Plan evidence |
|---|---|---|
| I. Separación estricta del dominio académico | PASS | `evidence` solo recopila y remite; no persiste decisiones documentales ni niveles académicos. |
| II. Desarrollo guiado por especificaciones y trazabilidad | PASS | FR-016–FR-020, RNF y SC se conservan en spec, OpenAPI, quickstart y futuras tareas/pruebas. |
| III. Incrementos verticales e independientes | PASS | I3 incluye UI, REST, dominio, persistencia, almacenamiento, migración y pruebas; no depende de I4–I8. |
| IV. Seguridad por función y ámbito | PASS | Cada comando, consulta y descarga reautoriza en backend la función, proceso, indicadores y vigencia; IDs ajenos no revelan existencia. |
| V. Inmutabilidad, versionado y auditoría | PASS | Borradores usan concurrencia optimista; `SUBMITTED` y sus hijos son append-only; la corrección crea sucesora y la remisión exige auditoría transaccional. |
| VI. Arquitectura modular comprobable | PASS | `evidence` depende de puertos públicos de I0/I2, `filestorage` y `audit`; controladores, aplicación, dominio e infraestructura permanecen separados. |
| VII. Calidad verificable y documentación viva | PASS | Se diseñan pruebas de contrato, dominio, migración, autorización, interrupciones, UI y aceptación; ADR-0002 cubre almacenamiento y ADR-0003 documentará versionado, seguridad, concurrencia y contrato público antes de implementarlos. |

No se solicitan excepciones. Las políticas de tipos, compatibilidad MIME, conservación y consulta de auditoría permanecen configurables y sintéticas; CL-001–CL-010 no se convierten en normativa fija.

### Gate after Phase 1 design

| Principle | Result | Design evidence |
|---|---|---|
| I | PASS | El modelo y contrato no contienen estados de revisión ni valoración; una referencia externa tampoco implica aceptación. |
| II | PASS | `contracts/openapi.yaml`, `data-model.md` y `quickstart.md` enlazan requisitos, invariantes y escenarios medibles. |
| III | PASS | El contrato entrega borrador → fuentes → remisión → descarga → sucesión → historial con dobles para puertos previos y sin componentes posteriores. |
| IV | PASS | El contrato oculta recursos fuera de ámbito, no expone `storageKey` y exige autorización en toda lectura, mutación y descarga. |
| V | PASS | Restricciones, triggers defensivos, unicidad, ETag y snapshots protegen historial y concurrencia; auditoría exitosa comparte la transacción de remisión. |
| VI | PASS | Las rutas fuente previstas respetan módulos y capas; la reconciliación durable vive en infraestructura/aplicación y no contamina el dominio académico. |
| VII | PASS | La guía incluye límites 25 MiB, fallos en cada ventana, SHA-256, conflictos, accesibilidad, regresión y datos sintéticos. |

## Project Structure

### Documentation (this feature)

```text
specs/004-submit-evidence/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── openapi.yaml
├── checklists/
│   └── requirements.md
└── tasks.md                 # generado posteriormente por /speckit.tasks
```

### Source Code (repository root)

```text
backend/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/ec/edu/unl/sec/
    │   │   ├── evidence/
    │   │   │   ├── api/
    │   │   │   ├── application/
    │   │   │   ├── domain/
    │   │   │   └── infrastructure/
    │   │   ├── filestorage/
    │   │   │   ├── application/
    │   │   │   ├── domain/
    │   │   │   └── infrastructure/
    │   │   ├── audit/
    │   │   ├── identityaccess/
    │   │   ├── selfassessment/
    │   │   └── shared/
    │   └── resources/
    │       ├── application.yml
    │       ├── db/migration/V006__evidence.sql
    │       └── db/migration/V011__evidence_immutability.sql
    └── test/
        ├── java/ec/edu/unl/sec/evidence/
        ├── java/ec/edu/unl/sec/filestorage/
        ├── java/ec/edu/unl/sec/contract/
        └── resources/

frontend/
└── src/app/
    ├── core/http/
    ├── shared/
    └── features/evidence/
        ├── data-access/
        ├── pages/
        ├── ui/
        └── evidence.routes.ts

tests/
├── acceptance/
├── fixtures/pilot/
└── performance/

docs/
├── adr/0002-file-storage-port.md
├── adr/0003-evidence-versioning-contract.md
├── technical/contracts/openapi.yaml
├── testing/traceability.md
└── user/index.qmd
```

**Structure Decision**: Se mantiene la aplicación web existente y el monolito modular. `evidence` posee el agregado y casos de uso; `filestorage` posee la mecánica de objetos; `audit`, I0 e I2 solo se consumen mediante puertos. No se crea un servicio, repositorio documental ni proyecto adicional.

## Phase 0 — Research outcome

Las decisiones están consolidadas en [research.md](./research.md). No quedan marcadores `NEEDS CLARIFICATION`. Las decisiones principales son:

1. raíz `Evidence` estable y contenido en `EvidenceVersion`, mutable solo en `DRAFT` e inmutable en `SUBMITTED`;
2. almacenamiento local inicial mediante puerto, promoción atómica por objeto y reconciliación durable entre almacenamiento y PostgreSQL;
3. Apache Tika para tipo detectado y política configurable separada de CL-008;
4. ETag/`If-Match`, bloqueo optimista al editar y bloqueo breve al remitir/crear sucesora;
5. auditoría transaccional para remisión exitosa y transacción independiente para fallos sanitizados;
6. REST síncrono multipart para 25 MiB y descargas verificadas antes de comenzar la respuesta.

## Phase 1 — Design outcome

- [data-model.md](./data-model.md) define agregado, snapshots, estados, restricciones, índices e interrupciones recuperables.
- [contracts/openapi.yaml](./contracts/openapi.yaml) define las operaciones REST específicas de I3, precondiciones, errores seguros y esquemas completos.
- [quickstart.md](./quickstart.md) prueba el recorrido v1 → inmutabilidad → v2, límites de archivo, autorización, concurrencia, recuperación y auditoría.
- ADR-0002 continúa vigente para almacenamiento y recuperación. ADR-0003 debe aprobar, antes de controladores y persistencia, la raíz/versiones, la cadena lineal, ETag/`If-Match`, ocultamiento de IDs, auditoría transaccional y forma del contrato REST público.

## Implementation sequencing

1. Aprobar ADR-0003 y reconciliar el contrato I3 y Problem Details antes de migraciones, controladores o cliente web.
2. Extender `FileStoragePort`, descriptor, política y detector; probar staging/promoción/reconciliación con archivos sintéticos.
3. Crear migración y modelo de dominio con restricciones, snapshots, ETag e inmutabilidad defensiva.
4. Implementar casos de uso y autorización mediante puertos I0/I2, con auditoría y compensación.
5. Implementar API REST y pruebas contractuales/negativas.
6. Implementar cliente Angular y recorridos accesibles.
7. Ejecutar aceptación, regresión, migración desde base limpia, prueba de restauración archivos↔metadatos, rendimiento y trazabilidad.

Las tareas que dependan de I0/I2 deben verificar esos prerrequisitos antes del recorrido integrado; no deben recrear identidades, procesos o indicadores dentro de I3.
