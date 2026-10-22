# D2 — Modelos completos, aplicabilidad y recorrido web

Fecha: 2026-10-22. Continuación de [las bases D2](d2-model-foundations.md) y
[la publicación por indicador](d2-model-publication.md). Esta evidencia cubre el
software con datos sintéticos. No acredita vigencia normativa ni constituye
aceptación institucional de D2.

## Alcance cerrado

| Tarea | Resultado verificable |
|---|---|
| T114 | `tests/fixtures/models/normative-verification.json` declara los cinco modelos, el instructivo UNL, páginas sin corroborar, huellas ausentes y responsables de CL-011–CL-017. Todo queda `PENDING_VALIDATION`; no se afirma aprobación. |
| T116/T027 | `NormativeReference` y `ModelCareerApplicability` son entidades inmutables y de solo adición. La aplicabilidad exige decisión explícita, procedencia, autoridad, revisión optimista y referencias verificadas antes de activar. |
| T024 | `V009__complete_evaluation_models.sql` integra referencias, nodos tipados, fuentes, reglas, variables, umbrales y aplicabilidad. El esquema SQLite de pruebas refleja las mismas tablas y el contador de migraciones pasa a 9. |
| T028 | Se extendió `EvaluationModelController` con endpoints aditivos v1 para configuración, referencias y decisiones de aplicabilidad. No se alteró el contrato v1 existente ni se declaró disponible la API v2 completa. |
| T029 | El editor crea versiones configuradas, conserva jerarquía/fuentes/reglas/referencias, publica por el flujo configurado, muestra referencias pendientes y registra decisiones de aplicabilidad por carrera. |
| T030 | Este documento registra la validación de los manifiestos sintéticos y los límites de la entrega. |

## Reglas comprobadas

- Una referencia `VERIFIED` exige emisor, edición, disposición, SHA-256 de 64
  caracteres hexadecimales, verificador autenticado y fecha. Una referencia
  `PENDING_VALIDATION` no admite disposición, verificador ni fecha.
- Las referencias registradas no se modifican. Una referencia verificada solo
  puede ser registrada o usada por la identidad autenticada que figura como
  verificador (`NORMATIVE_VERIFIER_MISMATCH` en caso contrario).
- Publicar una versión no exige referencias, pero activarla o marcarla
  `APPLICABLE` sí. La comprobación informa `MODEL_APPLICABILITY_PENDING` antes de
  evaluar vigencia y exige que la versión esté `PUBLISHED`.
- Una decisión de aplicabilidad requiere carrera existente, configuración del
  modelo, `If-Match` con la revisión vigente y la operación
  `MODEL_APPLICABILITY_AUTHORIZE`. El historial es append-only y una revisión
  obsoleta no sobrescribe la decisión confirmada.
- La vigencia no se infiere desde reglamentos ni fechas de publicación. Debe
  provenir de una decisión institucional explícita.
- Los endpoints nuevos son aditivos bajo `/api/v1`:
  `POST /models/{modelId}/versions/configured`,
  `PUT|GET /model-versions/{id}/configured`,
  `POST /model-versions/{id}/publish-configured`,
  `POST /normative-references`,
  `GET /model-versions/{id}/normative-references` y
  `POST|GET /model-versions/{id}/applicability-decisions`.

## Evidencia automatizada

Se ejecutó la regresión completa del backend con 54 clases:

```powershell
$env:JAVA_HOME='C:\Users\StarMedia\.jdks\openjdk-24.0.1'
.\mvnw.cmd -q "-Denforcer.skip=true" "-Djava.version=24" test
```

Resultado en `backend/target/surefire-reports`: **169 casos, 161 aprobados,
0 fallos, 0 errores y 8 omitidos**. Los omitidos son pruebas PostgreSQL
condicionadas a `SEC_RUN_DATABASE_TESTS`.

Casos relevantes: `NormativeVerificationManifestTest`,
`NormativeApplicabilityTest`, `NormativeReferencePersistenceTest`,
`EvaluationModelControllerTest` (incluye configuración, referencias y
aplicabilidad), `FiveModelConfigurationTest`, `CompleteModelPublicationTest`,
`FlexibleModelPersistenceTest` y `CleanDatabaseMigrationTest`.

En `frontend/` pasan `npm run build`, `npm run lint` y `npm test` (16 casos).

## Límites de la verificación

- El proyecto declara Java 25, pero este entorno solo dispone de OpenJDK 24. La
  regresión se ejecutó con `JAVA_HOME` 24, `-Djava.version=24` y el enforcer
  omitido. No es equivalente a la ejecución oficial con Java 25.
- V009 se validó por inspección y por paridad con el esquema SQLite. Las ocho
  pruebas PostgreSQL no se habilitaron, por lo que la migración no se aplicó
  sobre una base PostgreSQL real.
- Los datos y manifiestos son sintéticos. Las páginas, ediciones y huellas no
  están corroboradas y CL-011–CL-017 siguen sin aprobación institucional.
- No se ejecutó un recorrido E2E de navegador; el editor se verificó con build,
  lint y pruebas unitarias.
- La API v2 completa para transporte tipado sigue siendo contrato futuro. Esta
  entrega solo añade operaciones compatibles al controlador v1, conforme a
  ADR-0018.
