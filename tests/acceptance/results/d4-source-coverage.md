# D4 - Evidencias, coberturas y remision (evidencia)

## Alcance
- Fase 5 US4/D4 (T040-T049 + T118): dominio de cobertura de fuentes, remision de versiones DRAFT con inmutabilidad, decisiones de aplicabilidad con procedencia autorizadora, snapshot de fuentes del modelo fijo y snapshots tipados de datos estructurados/referencias institucionales.
- Sin declaracion de aceptacion institucional; solo datos sinteticos. PostgreSQL real no habilitado; validado con SQLite y pruebas unitarias/controller.
- Compatibilidad aditiva con rutas/contratos v1 mantenida (T113); OpenAPI sin cambios.

## Backend implementado
- Dominio: `SourceCoverage` (union de version con fuente requerida, nota 1..4000) y `ApplicabilityDecision` (sujeto INDICATOR/REQUIRED_SOURCE, APPLICABLE/NOT_APPLICABLE, regla, procedencia legal_basis/document_reference, revision con predecesor). `EvidenceVersion.submit(...)` traslada DRAFT a SUBMITTED con lockRevision+1.
- Migraciones: `V011__evidence_immutability.sql` (triggers PostgreSQL que bloquean UPDATE/DELETE de `evidence_version` SUBMITTED y DELETE de `evidence` con versiones remitidas), `V012__source_coverage.sql` (source_coverage UNIQUE(version, required_source), applicability_decision con revision y predecesor). Espejo `db/sqlite/schema.sql`; `CleanDatabaseMigrationTest` exige 13 migraciones.
- Aplicacion: `EvidenceSourceCoverageService` (declara/quita coberturas solo en DRAFT con If-Match; valida compatibilidad contra el catalogo de `RequiredSource` del modelo fijo y membership de indicadores de la version; duplicados rechazados), `ApplicabilityDecisionService` (decisiones con procedencia completa, revision obsoleta rechazada, predecesor encadenado), `SubmitEvidenceService` (exige >=1 cobertura, proceso ACTIVE, ETag; falla `COVERAGE_REQUIRED`/`STALE_DRAFT`/`PROCESS_NOT_ACTIVE`).
- Puertos/adaptadores: `RequiredSourceCatalogPort` + adapter sobre el modelo fijo (`EvaluationModelRepository`); `EvidenceAuthorizationPort.canDecideApplicability` (capability reutilizada `PROCESS_COORDINATE`). `EvidenceProblemException.Kind.FORBIDDEN` -> 403.
- API: `EvidenceSourceCoverageController`:
  - `GET /evidence-versions/{versionId}/required-sources`, `GET/POST/DELETE /evidence-versions/{versionId}/source-coverages` (If-Match lockRevision), `POST /evidence-versions/{versionId}/submit` (respuesta version con ETag nueva).
  - `GET/POST /processes/{processId}/applicability-decisions` (If-Match revision de decision, `ExclusionInput` con `provenance DecisionInput` obligatoria; `AUTHORITY_PROVENANCE_INCOMPLETE`).
  - Rutas v1 aditivas: DELETE de cobertura, POST submit y GET listado de snapshots.

## T118 - Snapshots tipados (FR-069 / AC-X16)
- Dominio: `StructuredEvidence` con modalidad STRUCTURED_DATA/INSTITUTIONAL_REFERENCE, `TypedValue` (NUMBER/STRING/BOOLEAN), unidad opcional (<=100), periodo obligatorio inicio<=fin y referencia opcional (<=2000). Los archivos NUNCA se exigen para estas modalidades y el snapshot comparte la inmutabilidad de la version.
- Migracion `V013__structured_snapshot.sql` (`evidence_structured_value`, UNIQUE(version, required_source), CHECK de periodo) + espejo SQLite.
- Aplicacion: `StructuredEvidenceService` exige cobertura previa (`STRUCTURED_SOURCE_NOT_COVERED`), compatibilidad con catalogo e indicadores de la version (`SOURCE_NOT_COMPATIBLE`, p.ej. fuente FILE no acepta snapshot), duplicado rechazado (`STRUCTURED_SNAPSHOT_DUPLICATE`), periodo/valor/unidad/referencia invalidos (422), solo borradores (`STRUCTURED_SNAPSHOT_IMMUTABLE`), ETag `STALE_DRAFT`.
- API: `StructuredEvidenceController` POST `/evidence-versions/{versionId}/structured-sources` de contrato con ETag por lockRevision; GET listado como ruta v1 aditiva; vista serializa `value` tipado + `valueType`.

## Pruebas
- `SourceCoverageTest`, `ApplicabilityDecisionServiceTest` (procedencia, revision obsoleta, sujeto desconocido, sustitucion con predecesor), `SubmitEvidenceServiceTest` (cobertura requerida, inmutabilidad, proceso inactivo), `EvidenceSourceCoverageControllerTest` (catalogo incompatible, 412, 409, remision correcta).
- `StructuredEvidenceServiceTest` (AC-X16: conserva valor/unidad/periodo para datos estructurados; conserva referencia para referencias institucionales sin exigir PDF; rechaza falta de cobertura, modalidad incompatible, fuente ajena, periodo/valor/metadatos invalidos, duplicado, version remitida, ETag obsoleto) y `StructuredEvidenceControllerTest` (201 + ETag, 428 sin If-Match, 409 duplicado, 422 incompatible, GET listado).
- Suite backend completa en verde con JDK 24 (`mvnw -Djava.version=24 test`); 13 migraciones en `CleanDatabaseMigrationTest`.

## Frontend
- `EvidenceSourceCoverageComponent` (T047): tabla de fuentes requeridas (codigo/indicador/tipo/estado/nota), botones Declarar/Quitar, formulario de nota 1..4000, bloqueo para versiones remitidas.
- `EvidenceVersionPage` (T048) en `/evidence/versions/:versionId`: detalle inmutable de version, coberturas, snapshots y accion Remitir; manejo `STALE_DRAFT` con recarga, `COVERAGE_REQUIRED` y `STRUCTURED_SOURCE_NOT_COVERED`. Enlace "Cobertura" desde el listado de evidencias. `EvidenceApi`/modelos ampliados (`requiredSources`, `coverages`, `declareCoverage`, `removeCoverage`, `submit`, `structuredSnapshots`, `registerStructuredSource`, `applicabilityHistory`, `decideApplicability`). `ApiClient.delete` ahora acepta cabeceras.
- `StructuredEvidenceComponent` (T118): tabla de snapshots y formulario con tipo de valor (numero/texto/booleano), unidad, periodo y referencia; solo fuentes elegibles (modalidad tipada) con cobertura declarada y sin snapshot previo.
- Lint/build OK; test OK (16 tests).

## Notas
- UI de decisiones de aplicabilidad (T046) disponible via API; pantalla dedicada queda para D5 junto a revisiones de cobertura (`CoverageReviewOutcome`/`ReviewInput`).
- V011 triggers solo PostgreSQL; SQLite se limita a espejo de tablas.
- Evidencia sintetica, sin declaracion institucional; gate T115 parcial: remision y snapshots tipados existen a nivel software; modalidades de archivo/snippets de 004 (T116) siguen pendientes.
