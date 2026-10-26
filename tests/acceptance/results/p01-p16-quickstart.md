# T110 — Quickstart P01–P16 ejecutado

Fecha local: 2026-10-25 (America/Bogota). Resultado: **PASS técnico sintético**.
Entorno: Windows, JDK 25, PostgreSQL 18.4 descartable, Spring Boot en puertos
18087/18088 y Playwright 1.64.1. No se usaron datos institucionales.

| Paso de quickstart.md | Ejecución y resultado |
|---|---|
| A1–A4: catálogo, cinco modelos, autorización y planificación | AC-P02–P06: PASS; carreras históricas, vigencias, jerarquías, aplicabilidad, equipos, conflictos, dependencias y auditoría |
| A5–A8: cuatro fuentes, tres remitidas, dos aceptadas y corte | AC-P11-01: PASS; recopilación 75 %, revisión 50 %, sin valoración creada por decisiones documentales |
| A9–A10: sucesoras, cobertura múltiple y denominador cero | AC-P10-03, AC-P11-02/03, AC-X07/08/09: PASS; borrador conserva aceptación previa, remisión exige revisión nueva; NOT_CONFIGURED y NOT_APPLICABLE separados |
| B1–B3: valoración manual y cálculo con variables verificadas | AC-P12-01/02 y AC-X10: PASS; 75 y 50 conservados con entradas/regla/escala; faltantes, unidad ajena, escala ajena y regla no validada rechazados |
| B: discrepancias y pérdida de sustento | AC-P12-03 y AC-X11: PASS; resolución competente y juicios anteriores conservados; sustentos invalidados exigen revisión |
| C1–C3: hallazgos, preliminar, socialización, devolución y aprobación | AC-P13, AC-P14 y AC-X12: PASS; vínculos exactos, respuestas, sucesoras y decisión sobre versión específica |
| D1–D2: mejora, avances, revisión y vencimiento | AC-P15 y AC-X13: PASS; propuesta/aprobación/ejecución/verificación separadas, sin verificar una acción sin avances; plazo revisado con auditoría |
| D3: cierre con pendientes | AC-P16-02 y AC-X14: PASS; bloqueo de fuente pendiente y acción vencida, con causa y estado conservado |
| D4: cierre, archivo, respaldo, restauración y consulta | AC-P16-01 y [restauración](p01-p16-restore.md): PASS; 14 tablas y 30 archivos comparados con hashes; segunda instancia consulta seis respuestas idénticas y descarga autenticada idéntica |
| D5: reapertura y nueva evaluación de la misma carrera | AC-P16-03 y AC-X15: PASS; modelo fijado, cierre sellado y aprobaciones anteriores conservados |

La [campaña consolidada](process-acceptance.md) registra **64 PASS, 0 FAIL,
0 SKIPPED**. El recorrido documental A no invoca P12–P15; su independencia
también tiene evidencia en [D6](d6-documentary-mvp.md).

Comandos reproducibles: `npm run test:complete-workflows` desde `frontend/`,
con los dos perfiles de prueba indicados en `tests/performance/README.md`;
`scripts/verify-complete-workflows-restore.ps1` con puerto/usuario/base/ruta
de archivos explícitos; `node scripts/verify-restored-workflows.mjs <processId>
<versionId> <fileId>` con las dos instancias locales levantadas. Los entornos
restaurados son copias nuevas; no se borra ni se reescribe la base de origen.

La revisión humana exigida por el quickstart se registra separadamente en
[T111](p01-p16-review-gate.md), todavía **PENDIENTE**. La ejecución técnica
no confirma vigencia normativa, competencias institucionales ni acreditación.
El ensayo de restauración no mide RPO/RTO ni políticas de retención reales.
