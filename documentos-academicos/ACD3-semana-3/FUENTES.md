# ACD3 — Fuentes y evidencia

Las rutas siguientes son relativas a la raíz del repositorio. Se inspeccionó código/documentación, sin ejecutar el backend ni pruebas. El commit base se registra en `evidencias/manifest.json`; los documentos académicos no tienen commit nuevo creado por esta actividad.

## Insumos de Semana 2

| Archivo | Uso / límite |
|---|---|
| `documentos-academicos/APE2-semana-2/specification.md` | Propuesta de incremento y contratos preliminares; no aprobación docente. |
| `documentos-academicos/APE2-semana-2/CONTRATOS_PRELIMINARES.md` | Línea base de sesiones, catálogo y procesos. |
| `documentos-academicos/AA2-semana-2/DOSSIER_REQUISITOS_AA2.md` | Actores, necesidades, HU-AA-01–06, AA-RF-01–14 y AA-RNF-01–10, AC/VC. |
| `documentos-academicos/AA2-semana-2/MATRIZ_TRAZABILIDAD.md` | Procedencia documental y pendientes de validación. |
| `docs/requirements/spec.md` | Problema, alcance, FR/RNF y clarificaciones de producto. |
| `docs/requirements/process-flows.md` | Procesos y límites de responsabilidades. |

## Arquitectura y configuración

| Archivo / sección | Uso |
|---|---|
| `docs/adr/0001-modular-monolith.md` — Decisión/alternativas | Monolito por capacidades, límites entre módulos. |
| `docs/adr/0002-file-storage-port.md` | Almacenamiento fuera de DB; solo contexto del producto. |
| `docs/adr/0004-opaque-sessions-scope-authorization.md` | Sesiones, hash de token y ámbitos. |
| `docs/adr/0018-catalog-contract-migration-compatibility.md` | Compatibilidad v1/v2, catálogo y revisiones, alcance de capacidades. |
| `docs/architecture/research.md` — Versiones base / arquitectura / errores | Justificaciones existentes; Java 25 difiere del Java 21 configurado. No reiterar versiones como recomendación vigente externa. |
| `backend/pom.xml` — parent/properties/dependencies | Java 21, Spring Boot 4.1.1, dependencias de persistencia, Flyway y ArchUnit. No prueba entorno instalado. |
| `frontend/package.json` — dependencies | Angular 22.2.3 declarado. |
| `frontend/angular.json` — serve/proxyConfig | Uso de proxy de desarrollo. |
| `frontend/proxy.conf.json` | `/api` hacia localhost:8080; no política CORS productiva demostrada. |
| `backend/src/main/resources/application.yml` | Base API, PostgreSQL, sesiones y almacenamiento. |
| `backend/src/main/resources/db/migration/V008__career_catalog.sql` | Esquema/revisiones del catálogo; integridad no ejecutada aquí. |
| `docs/technical/contracts/openapi.yaml` | Contrato común v1 con referencias externas. |
| `specs/005-complete-evaluation-workflows/contracts/openapi.yaml` | Objetivo y excepciones por ruta; no demuestra backend v2 disponible. |

## Código de los flujos

Para evitar abreviaturas ambiguas, se registran rutas completas de cada fuente relevante.

| Ruta | Símbolo y evidencia |
|---|---|
| `backend/src/main/java/ec/edu/unl/sec/identityaccess/api/SessionController.java` | create/revokeCurrentSession/me y DTO; OP-01–03. |
| `backend/src/main/java/ec/edu/unl/sec/identityaccess/application/SessionService.java` | Creación, autenticación y revocación. |
| `backend/src/main/java/ec/edu/unl/sec/identityaccess/application/ScopeAuthorizationService.java` | Capacidades y ámbitos. |
| `backend/src/main/java/ec/edu/unl/sec/identityaccess/application/AuthorizationPort.java` | Frontera de autorización de casos de uso. |
| `backend/src/main/java/ec/edu/unl/sec/identityaccess/infrastructure/SessionAuthenticationFilter.java` | Autenticación por cookie y CSRF propio en mutaciones. |
| `backend/src/main/java/ec/edu/unl/sec/identityaccess/infrastructure/SessionCookie.java` | Nombres y atributos de cookie/CSRF. |
| `backend/src/main/java/ec/edu/unl/sec/identityaccess/infrastructure/AccessProblemWriter.java` | Errores anteriores al controlador; revisar cabecera de desafío 401. |
| `backend/src/main/java/ec/edu/unl/sec/selfassessment/api/CareerController.java` | Rutas, DTO completos, Location y ETag; OP-04–12/17. |
| `backend/src/main/java/ec/edu/unl/sec/selfassessment/api/CareerCatalogProblemHandler.java` | Conversión de CareerCatalogException a ProblemDetail. |
| `backend/src/main/java/ec/edu/unl/sec/selfassessment/application/CareerAdministrationService.java` | Autorización, normalización de consulta, unicidad, unidad, historial y estados. |
| `backend/src/main/java/ec/edu/unl/sec/selfassessment/domain/Career.java` | Datos, vigencia, estado y revisión. |
| `backend/src/main/java/ec/edu/unl/sec/selfassessment/domain/AcademicUnit.java` | Representación de unidad. |
| `backend/src/main/java/ec/edu/unl/sec/selfassessment/application/CareerCatalogRepository.java` | Puerto de catálogo. |
| `backend/src/main/java/ec/edu/unl/sec/selfassessment/infrastructure/JdbcCareerCatalogRepository.java` | Persistencia y visibilidad paginada. |
| `backend/src/main/java/ec/edu/unl/sec/selfassessment/api/SelfAssessmentController.java` | List/get/create y ProcessCommand; OP-15/16/18. |
| `backend/src/main/java/ec/edu/unl/sec/selfassessment/api/ProcessExceptionHandler.java` | Estado HTTP según Kind; INVALID produce 400. |
| `backend/src/main/java/ec/edu/unl/sec/selfassessment/application/SelfAssessmentService.java` | create/get/list, versión publicada, PREPARATION, revisión 0, permisos. |
| `backend/src/main/java/ec/edu/unl/sec/selfassessment/domain/SelfAssessmentProcess.java` | Representación y validación de período/estado. |
| `backend/src/main/java/ec/edu/unl/sec/selfassessment/application/SelfAssessmentRepository.java` | Puerto de procesos. |
| `backend/src/main/java/ec/edu/unl/sec/selfassessment/infrastructure/JdbcSelfAssessmentRepository.java` | Adaptador de persistencia de procesos. |
| `backend/src/main/java/ec/edu/unl/sec/selfassessment/application/SelfAssessmentAuditService.java` | Auditoría del caso de proceso. |
| `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/api/EvaluationModelController.java` | DTO legado/configurado; OP-13/14. |
| `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/application/EvaluationModelService.java` | Lectura de modelos y comprobación de capacidad. |
| `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/application/ModelCatalogPort.java` | Consulta publicada desde procesos. |
| `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/infrastructure/ModelCatalogAdapter.java` | Adaptador interno, no API externa. |
| `backend/src/main/java/ec/edu/unl/sec/audit/application/AuditPort.java` | Frontera de registro de auditoría. |
| `backend/src/main/java/ec/edu/unl/sec/shared/api/EntityTagSupport.java` | ETag fuerte numérico y errores 400/428. |
| `backend/src/main/java/ec/edu/unl/sec/shared/api/ApiProblemHandler.java` | DTO inválido 400, permisos 403, errores inesperados 500 y formato de error. |
| `backend/src/main/java/ec/edu/unl/sec/shared/infrastructure/CorrelationFilter.java` | Correlación de solicitud para errores/auditoría. |

Pruebas a consultar: `backend/src/test/java/ec/edu/unl/sec/architecture/ModuleArchitectureTest.java`, `backend/src/test/java/ec/edu/unl/sec/selfassessment/api/CareerControllerTest.java`, `backend/src/test/java/ec/edu/unl/sec/selfassessment/api/SelfAssessmentControllerTest.java`, `backend/src/test/java/ec/edu/unl/sec/selfassessment/application/CareerAdministrationServiceTest.java`, `backend/src/test/java/ec/edu/unl/sec/selfassessment/application/SelfAssessmentServiceTest.java`, `backend/src/test/java/ec/edu/unl/sec/selfassessment/application/SelfAssessmentAuditServiceTest.java`, `tests/acceptance/career-catalog.spec.ts` y `docs/testing/traceability.md`. Son evidencia de mecanismos existentes; no se ejecutaron en esta elaboración.

## Fuentes técnicas primarias consultadas

- [C4: contexto](https://c4model.com/diagrams/system-context): frontera de sistema/personas.
- [C4: contenedores](https://c4model.com/diagrams/container): aplicaciones/almacenes y relaciones tecnológicas; no equivale a Docker.
- [C4: componentes](https://c4model.com/diagrams/component): descomposición interna de un contenedor.
- [RFC 9110](https://www.rfc-editor.org/rfc/rfc9110.html): §9 métodos, §8.8.3 ETag, §10.2.2 Location, §13.1.1 If-Match, §15 estados, en particular §15.5.2 para desafío 401.
- [RFC 5789](https://www.rfc-editor.org/rfc/rfc5789.html): §2, semántica de PATCH; no identifica JSON Patch por sí misma.
- [RFC 6585](https://www.rfc-editor.org/rfc/rfc6585.html): §3 precondición requerida y §4 límite de solicitudes.
- [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457.html): §3, Problem Details y extensiones; sustento del formato actual, distinto del sobre de la guía.
- [WHATWG Fetch](https://fetch.spec.whatwg.org/#http-cors-protocol): protocolo CORS, credenciales y exposición de cabeceras.

Estas fuentes justifican interpretación técnica, no necesidades institucionales ni aprobación del contrato propuesto. El alcance se justifica en los documentos de requisitos, no por autoridad de una RFC.
