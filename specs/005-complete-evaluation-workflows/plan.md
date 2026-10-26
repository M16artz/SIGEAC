# Implementation Plan: Flujos completos de evaluación de carreras

**Branch**: `005-complete-evaluation-workflows` | **Date**: 2026-10-21 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/005-complete-evaluation-workflows/spec.md`

**Estado al 2026-10-25**: 125 de 126 tareas técnicas completadas; T111 sigue
pendiente de revisión humana. La campaña consolidada registra 64 PASS y el
quickstart técnico incluye restauración conjunta verificada. Véanse
[resultados actuales](../../tests/acceptance/results/p01-p16-completion-validation.md).
`Branch` identifica el paquete lógico; no afirma un cambio de rama Git. Los
apartados de avance y los PASS de diseño siguientes conservan su alcance histórico.

**Avance técnico D1**: carreras/unidades y compatibilidad entregadas con datos
sintéticos; resultados en [d1-careers.md](../../tests/acceptance/results/d1-careers.md).
D2–D8 y la revisión institucional permanecen pendientes.

**Avance interno D2**: [publicación por indicador](../../tests/acceptance/results/d2-model-publication.md)
verifica T006/T021–T023/T025–T026, cinco configuraciones sintéticas, metodología,
fuentes y protección de reglas automáticas. No completa V009, aplicabilidad,
API/editor v2 ni aceptación D2; continúan T024/T027–T030/T114/T116.

## Summary

Completar P01–P16 mediante un único sistema configurable. La implementación conserva los módulos existentes y añade los conceptos faltantes: catálogo de carreras, procedencia y aplicabilidad normativa, fuentes requeridas, autorización y planificación, métricas por fuente, reglas académicas por indicador, hallazgos, informes institucionales, mejora y archivo. El MVP documental termina con P11 y consulta histórica básica; la valoración académica y los procesos institucionales posteriores se entregan sin bloquear ese MVP.

## Technical Context

**Language/Version**: Java 25 LTS; TypeScript 6.0 con Angular 22; SQL PostgreSQL 18

**Primary Dependencies**: Spring Boot 4.1.x, Spring Security 7.1.x, Spring Data JPA/Hibernate, Flyway, OpenAPI 3.1, Angular Material/CDK, RxJS, Bean Validation

**Storage**: PostgreSQL para datos y metadatos; archivos fuera de PostgreSQL mediante `FileStoragePort`

**Testing**: JUnit 5, AssertJ, ArchUnit, Testcontainers/PostgreSQL local, MockMvc, Vitest, Angular Testing Library, Playwright y k6

**Target Platform**: aplicación web en servidor Linux o Windows, con ruta local reproducible sin Docker

**Project Type**: cliente web y backend monolítico modular con API REST

**Performance Goals**: conservar RNF-006–RNF-008 y paginar catálogos, bandejas, auditoría e históricos

**Constraints**: datos sintéticos; sin acreditación; autorizaciones institucionales registradas pero no inferidas; reglas automáticas solo validadas; versiones publicadas/finales append-only

**Scale/Scope**: cinco configuraciones de modelo, una carrera piloto de aceptación y capacidad para múltiples carreras y procesos históricos

## Constitution Check

### Gate before research

| Principle | Result | Plan evidence |
|---|---|---|
| I. Separación del dominio | PASS | `CoberturaFuente`, `RevisionDocumental` y `Valoracion` son agregados distintos. |
| II. SDD y trazabilidad | PASS | FR-041–FR-070, P01–P16, contratos, tareas y escenarios tienen identificadores estables. |
| III. Incrementos verticales | PASS | El plan usa D1–D8; D1–D6 forman el MVP documental y D7–D8 completan el ciclo. |
| IV. Seguridad por función y ámbito | PASS | Cada comando/consulta declara capacidad y ámbito; la autoridad institucional se registra separadamente. |
| V. Versionado y auditoría | PASS | Modelos, evidencias, revisiones, valoraciones, informes, acciones y cierres preservan predecesores. |
| VI. Arquitectura modular | PASS | Se amplían módulos existentes y se añaden `governance`, `improvement` y puertos públicos. |
| VII. Calidad y documentación | PASS | Cada entrega incluye pruebas, contrato, trazabilidad, manual y quickstart. |

No se requieren excepciones constitucionales.

### Gate after design

**PASS**. El modelo de datos permite jerarquías variables y vincula cada cobertura con `FuenteRequerida`; los cierres documental e integral tienen políticas diferentes; el contrato no expone una operación de acreditación; P12–P15 no son dependencia del MVP documental.

## Architecture changes

| Module | Responsibility added |
|---|---|
| `identityaccess` | capacidades para administrar carreras, autoridades y ámbitos sin mezclar privilegios técnicos |
| `evaluationmodel` | nodos flexibles, fuentes requeridas, aplicabilidad/vigencia y reglas por indicador |
| `selfassessment` | autorización institucional, suspensión, actividades, equipos y fechas |
| `evidence` | `SourceCoverage` entre versión de evidencia y fuente requerida |
| `documentreview` | cobertura aceptada derivada de decisiones vigentes y subsanación enlazada |
| `reporting` | fórmulas exactas de recopilación/revisión y corte documental independiente |
| `academicevaluation` | variables, cálculo validado, valoración manual y discrepancias |
| `governance` | hallazgos, versiones de informe, observaciones y decisiones de aprobación |
| `improvement` | planes, acciones, avances y verificaciones |
| `archive` | políticas de cierre integral, instantáneas y consultas históricas |

Los módulos se comunican mediante puertos; ningún módulo consulta repositorios internos ajenos. `reporting` consume proyecciones de fuentes, evidencias y decisiones. `governance` referencia resultados identificados, pero no los reescribe. [ADR-0017](../../docs/adr/0017-source-coverage-and-process-governance.md) y [reglas de dominio](../../docs/requirements/domain-rules.md) definen revisión por cobertura, versiones vigentes, estados y políticas de cierre.

## Delivery increments

| Delivery | Processes | Outcome | MVP |
|---|---|---|---|
| D1 — Identidad y carreras | P01–P02 | usuarios, carreras, unidades y permisos | Sí |
| D2 — Modelos configurables | P03–P04 | cinco configuraciones versionadas y fuentes requeridas | Sí |
| D3 — Procesos y planificación | P05–P06 | autorización, activación, suspensión, equipos y tareas | Sí |
| D4 — Evidencias | P07–P08 | evidencia vinculada a fuente y remisión inmutable | Sí |
| D5 — Revisión y subsanación | P09–P10 | decisiones documentales y sucesión | Sí |
| D6 — Seguimiento documental | P11 + P16 básico | métricas exactas, tablero y corte histórico documental | Sí |
| D7 — Evaluación e informes | P12–P14 | valoración, hallazgos e informe institucional | No |
| D8 — Mejora y archivo | P15–P16 | acciones, cierre integral y consulta histórica | No |

## Implementation sequence

La [hoja de ruta desde la implementación revisada](../../docs/planning/next-steps.md) concreta módulos, actores, funcionalidades y puertas de cierre. D1 incorpora V008 para carreras; V009/V010 se reservan para modelos/gobierno y V011 para la inmutabilidad pendiente de 004, antes de V012. [ADR-0018](../../docs/adr/0018-catalog-contract-migration-compatibility.md) conserva v1 y separa los cambios incompatibles planificados en v2; no se declara D2–D8 implementadas.

1. Reconciliar requisitos, IDs, contratos y migraciones ya aplicadas; no editar migraciones históricas.
2. Implementar D1 y D2 en paralelo después de las bases de seguridad/auditoría.
3. Implementar D3 sobre versiones publicadas, aplicabilidad por carrera y políticas institucionales configurables. La demostración sintética se rotula expresamente; no se presenta como autorización institucional.
4. Extender I3 mediante migración sucesora para `source_coverage`; conservar versiones ya remitidas.
5. Implementar D5 y calcular D6 desde proyecciones, sin duplicar estados documentales.
6. Validar y aceptar el MVP documental antes de iniciar reglas académicas automáticas.
7. Implementar D7 primero con valoración manual; habilitar estrategias cuantitativas solo cuando estén validadas.
8. Implementar D8 y ejecutar cierre integral, restauración y consulta histórica.

## Migration strategy

- Reservar V008–V020 para esta reconciliación y crear migraciones nuevas/ascendentes; nunca modificar migraciones ya aplicadas ni reutilizar números de los backlogs históricos.
- La reserva V008–V020 es propuesta: antes de crear archivos, T005/T113 inspeccionan el historial realmente aplicado y el estado de los backlogs I0–I3. Registrar cualquier ajuste de numeración, nunca reutilizar una versión ya aplicada. Los saltos de numeración no exigen crear migraciones vacías.
- Incorporar `academic_unit`, ampliar `career`, añadir procedencia normativa y aplicabilidad.
- Migrar la jerarquía rígida existente a nodos recursivos manteniendo UUID y referencias.
- Crear `required_source` y `source_coverage`; los datos previos quedan `PENDING_CLASSIFICATION` hasta reconciliación explícita.
- Preservar IDs de versiones remitidas y referencias de asignación. La clasificación histórica se guarda como decisión anexa, sin editar el snapshot remitido; los registros sin fuente verificada no cuentan como cobertura. No convertir administradores previos en autoridades ni fabricar autorizaciones retroactivas.
- Añadir autorización, actividades, suspensión, variables, hallazgos, informes, mejora y archivo en migraciones independientes por entrega.
- Probar actualización desde cada incremento aceptado y desde base limpia.

## Testing strategy

- Contratos y autorización negativa para cada operación.
- Fixtures de los cinco modelos; General 2024 verifica 5 criterios, 8 subcriterios y 31 indicadores cuando la fuente normativa sea validada.
- Matrices de jerarquías variables, fuentes no aplicables y divisores cero.
- Cálculo manual contra tablero para recopilación y revisión.
- Pruebas de separación: aceptación documental nunca crea valoración.
- Estrategias cuantitativas con variables, unidades, fórmula, umbral y versión.
- Recorridos de informe preliminar/final, mejora y cierre integral.
- Restauración conjunta de PostgreSQL y archivos con verificación de integridad.
- Ejecutar los 48 escenarios [AC-P01–AC-P16](../../docs/testing/process-acceptance.md) y los 16 criterios AC-X de dominio según la entrega. Los casos aún no implementados se registran como pendientes, no como PASS.

## Prerrequisitos de implementación y contratos

El paquete 005 completa trabajo existente, no sustituye upload, remisión, descarga o sucesión pendientes de 004. T115 identifica y termina esos prerrequisitos antes de aceptar D4. D1 valida nuevamente login, expiración, desactivación y autorización negativa de 001.

El [OpenAPI objetivo](contracts/openapi.yaml) contiene 77 operaciones nuevas/revisadas, con entrada, salida, consultas exactas para recuperar ETag, paginación, CSRF y errores. Es un diseño; el [contrato común](../../docs/technical/contracts/openapi.yaml) conserva las operaciones previas. T113/T002 revisan colisiones y compatibilidad antes de componerlos; los contratos no se mezclan sin transición de clientes.

Los 126 ítems de [tasks.md](tasks.md) incluyen T113–T126 para cerrar procedencia por indicador, modalidades de fuente, revisión parcial, conciliación, informes y cierre. Las tareas permanecen pendientes hasta implementar y guardar evidencia.

## Project Structure

### Documentation

```text
specs/005-complete-evaluation-workflows/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/openapi.yaml
├── checklists/requirements.md
└── tasks.md

docs/
├── requirements/spec.md
├── requirements/process-flows.md
├── planning/plan.md
├── planning/tasks.md
├── architecture/data-model.md
└── testing/traceability.md
```

### Source Code

```text
backend/src/main/java/ec/edu/unl/sec/
├── identityaccess/
├── evaluationmodel/
├── selfassessment/
├── evidence/
├── documentreview/
├── reporting/
├── academicevaluation/
├── governance/
├── improvement/
├── archive/
└── audit/

frontend/src/app/features/
├── access/
├── careers/
├── models/
├── processes/
├── evidence/
├── reviews/
├── reporting/
├── evaluations/
├── reports/
├── improvement/
└── archive/
```

**Structure Decision**: ampliar el monolito modular existente. No se crean aplicaciones por carrera ni servicios distribuidos.

## Risks and controls

| Risk | Control |
|---|---|
| Configuraciones normativas no verificadas | estado `PENDING_VALIDATION`; no habilitar para nuevos procesos |
| Evidencia histórica sin fuente | cola de clasificación explícita; no inferir cobertura |
| Promedio académico arbitrario | agregación deshabilitada salvo regla validada de la versión |
| Competencia institucional incorrecta | políticas configurables y decisión firmada/auditada |
| Dependencia del MVP en evaluación | prueba de aceptación SC-017 con módulos P12–P15 deshabilitados |
| Migración destructiva | migraciones sucesoras y pruebas desde cada versión aceptada |


Ajuste de reservas (2026-10-22): V013 contiene snapshots estructurados y V014
valida referencias normativas contra el snapshot versionado. D5-D8 usaran
V015-V020 en orden; no se reutilizan versiones ya aplicadas.
