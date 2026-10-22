# Research: Flujos completos de evaluación

## Decision: process IDs are canonical

**Decision**: usar P01–P16 como lenguaje funcional y D1–D8 como entregas. Los nombres I0–I8 anteriores se consideran referencias históricas.

**Rationale**: evita cambiar identificadores ya utilizados en migraciones y artefactos, pero permite un MVP documental coherente.

**Alternatives considered**: renumerar todos los incrementos existentes; mantener I0–I6 como MVP aunque dependa de valoración académica.

## Decision: evidence covers required sources

**Decision**: introducir una relación versionada `SourceCoverage` entre `EvidenceVersion` y `RequiredSource`.

**Rationale**: el indicador por sí solo no permite conocer qué fuente fue recopilada o aceptada.

**Alternatives considered**: inferir por nombre de archivo; considerar cualquier evidencia del indicador como cobertura de todas sus fuentes.

## Decision: separate documentary and integral closure

**Decision**: un corte documental puede congelarse al completar reglas P01–P11; el cierre integral utiliza resultados, informes y mejora según política posterior.

**Rationale**: permite aceptar el MVP sin inventar valoraciones académicas.

**Alternatives considered**: exigir valoración final para todo cierre; no conservar cortes documentales.

## Decision: per-indicator valuation rule

**Decision**: `ValuationRule` pertenece al indicador y versión del modelo. Puede ser manual cualitativa o cuantitativa validada con variables, fórmula y umbrales.

**Rationale**: no existe una fórmula universal para todos los indicadores o modelos.

**Alternatives considered**: fórmula global por modelo; motor de expresiones arbitrarias.

## Decision: institutional authority as policy-backed evidence

**Decision**: autorizaciones, aprobaciones, excepciones y no aplicabilidades registran función, autoridad, fundamento, documento, actor y fecha.

**Rationale**: el software prueba que una decisión fue registrada, pero no inventa quién posee la competencia.

**Alternatives considered**: roles fijos codificados; aprobación automática por estado.

## Decision: exact five configurations, not five systems

**Decision**: cargar cinco configuraciones independientes en el catálogo común. La vigencia se registra separadamente de la publicación técnica.

**Rationale**: publicar una configuración verificable no demuestra por sí mismo que siga vigente normativamente.

**Alternatives considered**: módulos por carrera; asumir vigencia por fecha del archivo.

