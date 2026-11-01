# Feature Specification: preparación inicial de la autoevaluación — versión de trabajo 01

**Created**: 2026-10-31

**Status**: Borrador académico inicial de esta preparación, no documento de una sesión anterior.

**Input**: APE2, Guía Nro. 002: refinar specification.md a partir del proyecto y de los insumos ACD2.

## Contexto y alcance inicial de trabajo

El proyecto organiza la autoevaluación interna de carreras. Para la Unidad 1 se propone seleccionar acceso autorizado, catálogo de carreras y preparación de un proceso con modelo publicado, sin activar el proceso ni recopilar evidencias.

La propuesta de alcance requiere validación docente. No se afirma que exista aceptación grupal ni institucional.

## User Scenarios & Testing

### User Story 1 - Acceder al sistema (Priority: P1)

Como usuario habilitado, quiero acceder a mis funciones para trabajar en mi ámbito autorizado.

### User Story 2 - Gestionar carreras (Priority: P1)

Como administrador funcional, quiero mantener carreras para preparar procesos sobre un catálogo identificable.

### User Story 3 - Preparar proceso (Priority: P2)

Como coordinador autorizado, quiero preparar un proceso con carrera, período y modelo publicado para organizar una autoevaluación sin presumir su autorización institucional.

## Requirements

### Functional Requirements

- FR-001/002: autenticar y autorizar las operaciones protegidas.
- FR-043: administrar carreras y unidades con código único e historial.
- FR-011: crear proceso para carrera, período y modelo publicado.
- FR-039: devolver errores estables.

### Non-functional Requirements

- RNF-002: expirar sesión a los 30 minutos de inactividad.
- RNF-006: p95 interactivo del MVP no mayor que 2 segundos bajo carga piloto.
- RNF-011: impedir pérdida silenciosa de cambios concurrentes.
- RNF-014: recorridos accesibles según la exigencia documentada del proyecto.

## Success Criteria

### Measurable Outcomes

Queda pendiente desglosar verificaciones, criterios Dado–Cuando–Entonces, recursos de contrato y decisiones de clarificación.

## Assumptions

Se utilizan datos sintéticos, identidades y modelo publicado ya disponibles. No se crean autoridades institucionales ni se implementan nuevas funciones durante esta práctica documental.

