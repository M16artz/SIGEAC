# APE3 — Fuentes y localización de evidencia

## Base documental y restricciones

- `documentos-academicos/APE2-semana-2/specification.md`: contexto, recorte y requisitos locales.
- `documentos-academicos/AA2-semana-2/DOSSIER_REQUISITOS_AA2.md`: actores, necesidades, RF/RNF, HU, AC y VC.
- `documentos-academicos/AA2-semana-2/MATRIZ_TRAZABILIDAD.md`: procedencia y límites de validación.
- `documentos-academicos/ACD3-semana-3/ACD3_CLINICA_ARQUITECTURA_API.md`: F-ACD3-01/02, DA-01–08 y trazabilidad.
- `documentos-academicos/ACD3-semana-3/ARQUITECTURA_C4.md`: vistas Mermaid de la base existente.
- `documentos-academicos/ACD3-semana-3/CONTRATO_API_INICIAL.md`: inventario anterior de 18 operaciones y alternativa de sobre.
- `documentos-academicos/ACD3-semana-3/REGISTRO_OBSERVACIONES.md`: hallazgos técnicos y retroalimentación pendiente, no acta de sesión.
- `.specify/memory/constitution.md`: base Spring y límites de diseño, pruebas y decisiones abiertas.
- `docs/requirements/spec.md` y `docs/requirements/process-flows.md`: requisitos y procesos de producto.
- `docs/adr/0001-modular-monolith.md`, `docs/adr/0004-opaque-sessions-scope-authorization.md`, `docs/adr/0018-catalog-contract-migration-compatibility.md`: decisiones existentes.
- `docs/architecture/research.md`: planificación tecnológica; no confundir Java 25 allí con nivel 21 configurado en POM.

## Implementación y contratos inspeccionados

| Ruta exacta | Evidencia utilizada |
|---|---|
| `backend/pom.xml` | Java/Spring, Maven, dependencias y plugins de prueba/arranque. |
| `backend/src/main/resources/application.yml` | Base /api/v1, PostgreSQL y configuración de identidad/archivos. |
| `backend/src/main/resources/application-sqlite-test.yml` | Perfil auxiliar, SQL de inicialización y datos demo. |
| `backend/src/main/java/ec/edu/unl/sec/identityaccess/api/SessionController.java` | POST/DELETE sessions; SessionView, cookie y 204. |
| `backend/src/main/java/ec/edu/unl/sec/identityaccess/infrastructure/SessionCookie.java` | SEC_SESSION, X-CSRF-TOKEN y atributos. |
| `backend/src/main/java/ec/edu/unl/sec/identityaccess/infrastructure/SessionAuthenticationFilter.java` | Autenticación y CSRF real antes de controladores. |
| `backend/src/main/java/ec/edu/unl/sec/selfassessment/api/CareerController.java` | Rutas/DTO de catálogo, ETag, Location y validaciones. |
| `backend/src/main/java/ec/edu/unl/sec/selfassessment/api/CareerCatalogProblemHandler.java` | Problem Details de errores de catálogo. |
| `backend/src/main/java/ec/edu/unl/sec/selfassessment/application/CareerAdministrationService.java` | Capacidad/ámbito, unicidad, unidad activa, revisión, auditoría. |
| `backend/src/main/java/ec/edu/unl/sec/selfassessment/domain/Career.java` | Data, status, revisión, normalización y vigencia. |
| `backend/src/main/java/ec/edu/unl/sec/selfassessment/domain/AcademicUnit.java` | Record y forma anidada de unidad. |
| `backend/src/main/java/ec/edu/unl/sec/selfassessment/application/CatalogPage.java` | items/page/size/totalElements. |
| `backend/src/main/java/ec/edu/unl/sec/selfassessment/infrastructure/JdbcCareerCatalogRepository.java` | Adaptador de persistencia/revisiones del UML. |
| `backend/src/main/java/ec/edu/unl/sec/selfassessment/api/SelfAssessmentController.java` | Preparar y consultar proceso, DTO y ETag. |
| `backend/src/main/java/ec/edu/unl/sec/selfassessment/application/SelfAssessmentService.java` | Carrera activa, modelo publicado, PREPARATION/revisión 0. |
| `backend/src/main/java/ec/edu/unl/sec/selfassessment/domain/SelfAssessmentProcess.java` | Campos y período de proceso. |
| `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/api/EvaluationModelController.java` | Rutas legada/configurada de lectura. |
| `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/api/LegacyModelVersion.java` | from llama a requireLegacy. |
| `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/application/EvaluationModelService.java` | requireLegacy y rechazo MODEL_VERSION_REQUIRES_V2. |
| `backend/src/main/java/ec/edu/unl/sec/selfassessment/infrastructure/SyntheticPilotData.java` | Identificadores y modelo/carrera piloto sintéticos. |
| `backend/src/main/java/ec/edu/unl/sec/selfassessment/infrastructure/SyntheticCareerData.java` | Carrera/unidad demo y vigencia. |
| `backend/src/main/java/ec/edu/unl/sec/shared/api/EntityTagSupport.java` | ETag fuerte numérico y 400/428. |
| `backend/src/main/java/ec/edu/unl/sec/shared/api/ApiProblemHandler.java` | 400 de DTO y formato de error. |
| `docs/technical/contracts/openapi.yaml` | Fuente del subconjunto observado. |
| `specs/005-complete-evaluation-workflows/contracts/openapi.yaml` | Referencias de catálogo; no contrato v2 íntegramente implementado. |
| `frontend/package.json` | Angular y dependencias instaladas de documentación/validación. |

Los restantes puertos y componentes citados están indexados con ruta completa en `documentos-academicos/ACD3-semana-3/FUENTES.md`. La lectura no implica haber ejecutado cada componente ni cubierto cada regla.

## Pruebas ejecutadas y artefactos nuevos

Se ejecutaron únicamente estas suites de Maven:

- `backend/src/test/java/ec/edu/unl/sec/selfassessment/api/CareerControllerTest.java`.
- `backend/src/test/java/ec/edu/unl/sec/selfassessment/application/CareerAdministrationServiceTest.java`.
- `backend/src/test/java/ec/edu/unl/sec/selfassessment/domain/CareerTest.java`.
- `backend/src/test/java/ec/edu/unl/sec/architecture/ModuleArchitectureTest.java`.

Sus XML se escribieron en `backend/target/surefire-reports/TEST-ec.edu.unl.sec.…xml`. El resumen entregado conserva clase, nombres, conteos, tiempo y timestamp. No se atribuyen a esta actividad las otras suites ya presentes en ese directorio.

Evidencia HTTP/muestras y herramientas reproducibles: `verificacion/verificar_http.py`, `generar_contratos.mjs`, `validar_respuestas.mjs`; resultados bajo `evidencias/`. Plantilla: `.agents/skills/informes-latex-unl/assets/FORMATOAPE.tex`; logo real: `assets/reporte_tecnico_overleaf_logo.png` dentro de la misma habilidad.

## Referencias técnicas primarias consultadas

| Fuente | Relación con el trabajo |
|---|---|
| [C4: contexto](https://c4model.com/diagrams/system-context), [contenedores](https://c4model.com/diagrams/container), [componentes](https://c4model.com/diagrams/component) | Diferencia entre niveles y frontera representada; revisadas en ACD3. |
| [Structurizr DSL — lenguaje](https://docs.structurizr.com/dsl/language) y [vista de componentes](https://docs.structurizr.com/dsl/cookbook/component-view/) | Estructura model/views, identificadores jerárquicos e inclusiones. No sustituyen un parseo oficial. |
| [PlantUML — clases](https://plantuml.com/class-diagram) | Relaciones y tipos reales del catálogo. |
| [OpenAPI 3.1.0](https://spec.openapis.org/oas/v3.1.0) | Paths/operations, schemas, responses y seguridad; versión elegida compatible con fuente, sin afirmar que es la más reciente. |
| [RFC 9110](https://www.rfc-editor.org/rfc/rfc9110.html) | §9 métodos, §13 precondiciones, §15 estados; ver análisis de ACD3/AA2. |
| [RFC 5789](https://www.rfc-editor.org/rfc/rfc5789.html), [RFC 6585](https://www.rfc-editor.org/rfc/rfc6585.html), [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457.html) | PATCH, 428/429 y Problem Details; analizadas en ACD3. |
| [DRF — vistas](https://www.django-rest-framework.org/api-guide/views/) | Referencia de tecnología exigida; no convierte los controladores Java en APIView. |
| [Quarto — HTML](https://quarto.org/docs/output-formats/html-basics.html) | Página técnica versionable y salida HTML. |

Las referencias justifican los formatos/técnicas; las fuentes del dominio y competencias institucionales siguen en AA2 con sus límites. No se realizaron entrevistas, validación normativa ni retroalimentación docente nuevas.
