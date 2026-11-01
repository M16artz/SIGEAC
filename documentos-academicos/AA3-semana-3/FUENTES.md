# AA3 — Registro de fuentes y evidencia

Rutas relativas a la raíz. Las fuentes P describen el proyecto; S fundamentan mecanismos técnicos y forma del ADR. Las ventajas/costes comparativos y la selección C son inferencias del análisis, no recomendaciones explícitas de un fabricante ni resultados experimentales. No se hicieron entrevistas ni nuevas pruebas de negocio.

## Fuentes del proyecto

| ID | Archivos exactos / localizadores | Uso y límite |
|---|---|---|
| P-01 | `documentos-academicos/AA2-semana-2/DOSSIER_REQUISITOS_AA2.md`, tablas RF/RNF y AC/VC; `documentos-academicos/AA2-semana-2/MATRIZ_TRAZABILIDAD.md` | N-AA-05, AA-RF-14, AA-RNF-03/07/09/10; actores y criterios. No aprobación institucional. |
| P-02 | `documentos-academicos/ACD3-semana-3/ACD3_CLINICA_ARQUITECTURA_API.md`, §§2/4/6; `CONTRATO_API_INICIAL.md`, §4, y `REVISION_GUIA.md`, bajo la misma carpeta | Procesos, alternativas de respuesta y compatibilidad. La propuesta preliminar considera paginación en meta; no es acuerdo aceptado. |
| P-03 | `documentos-academicos/APE3-semana-3/adr/APE3-001-alineacion-tecnologica.md`; `documentos-academicos/APE3-semana-3/adr/APE3-002-contrato-universal.md` | Diferencias de tecnología y sobre todavía propuestas; AA3 refina una subdecisión, no reemplaza su estado. |
| P-04 | `documentos-academicos/APE3-semana-3/arquitectura/workspace.dsl`, componentes `sigeac.api.http` y `sigeac.api.acceso`; `documentos-academicos/ACD3-semana-3/ARQUITECTURA_C4.md` | Ubicación del cambio propuesto en C3; no nuevo contenedor. |
| P-05 | `documentos-academicos/APE3-semana-3/contratos/openapi-observado.yaml`; `documentos-academicos/APE3-semana-3/contratos/openapi-objetivo.yaml` | Contrato actual de muestra y objetivo no implementado. El objetivo mantiene CatalogPage dentro de data; confirmar variante de paginación antes de adoptar. |
| P-06 | `backend/src/main/java/ec/edu/unl/sec/selfassessment/api/CareerController.java`, retornos ResponseEntity; `backend/src/main/java/ec/edu/unl/sec/identityaccess/api/SessionController.java`, create/revokeCurrentSession; `backend/src/main/java/ec/edu/unl/sec/shared/api/ApiProblemHandler.java`, problem/handlers; `backend/src/main/java/ec/edu/unl/sec/selfassessment/api/CareerCatalogProblemHandler.java`; `backend/src/main/java/ec/edu/unl/sec/identityaccess/infrastructure/AccessProblemWriter.java`, write; `backend/src/main/java/ec/edu/unl/sec/identityaccess/infrastructure/SessionAuthenticationFilter.java`, doFilterInternal; `backend/src/main/java/ec/edu/unl/sec/shared/infrastructure/CorrelationFilter.java` | Distintos puntos de salida actuales. El writer escribe al servlet y no retorna desde un método MVC. No se implementó ni probó advice/mapeador C nuevo. |
| P-07 | `frontend/src/app/features/access/data-access/access-api.service.ts`, login/rememberCsrf; `frontend/src/app/features/access/data-access/access.models.ts`, SessionView; `frontend/src/app/core/http/problem.ts`, isApiProblem; `frontend/src/app/features/careers/pages/career-catalog.page.ts`, result.items/history | Acoplamientos comprobables del cliente a las representaciones existentes. Lectura de código, sin ejecución Angular en AA3. |
| P-08 | `backend/src/test/java/ec/edu/unl/sec/architecture/ModuleArchitectureTest.java`, DOMAIN_IS_INDEPENDENT y APPLICATION_IGNORES_API_AND_INFRASTRUCTURE; `documentos-academicos/APE3-semana-3/evidencias/pruebas-maven.json`; `documentos-academicos/APE3-semana-3/evidencias/http-resultados.json`; `documentos-academicos/APE3-semana-3/evidencias/esquemas-resultados.json` | Pruebas y resultados previos: 23 seleccionados/22 HTTP, no cobertura de la alternativa C ni del sobre. Se revisó evidencia, no se reejecutó. |
| P-09 | `.specify/memory/constitution.md`, principios II/VI/VII y restricciones; `docs/adr/0001-modular-monolith.md`; `docs/adr/0004-opaque-sessions-scope-authorization.md`; `docs/adr/0018-catalog-contract-migration-compatibility.md`; `backend/pom.xml`, parent/properties | Arquitectura y compatibilidad existentes. POM declara Java21/Boot4.1.1; no se equipara con versión de la documentación Spring consultada. |

## Fuentes técnicas primarias consultadas

| ID | Fuente y localizador | Afirmación respaldada / límite |
|---|---|---|
| S-01 | [Spring Framework: Return Values](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/return-types.html), tabla de valores de retorno, filas ResponseEntity/ProblemDetail | Spring distingue cuerpo, cabeceras y tipos de respuesta. No prescribe una arquitectura de sobre ni demuestra compatibilidad del proyecto. |
| S-02 | [Spring Framework: Error Responses](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html), Main abstractions / Render | ProblemDetail y handlers son mecanismos de errores; deben conservar relación entre representación, status y media type. No envolver como JSON arbitrario manteniendo etiqueta Problem Details sin revisar semántica. |
| S-03 | [ResponseBodyAdvice Javadoc](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/mvc/method/annotation/ResponseBodyAdvice.html), descripción, supports y beforeBodyWrite | Punto de extensión tras método MVC y antes de escribir con conversor. La conclusión de que no basta para el writer actual se deduce también de P-06, no de una prueba experimental. |
| S-04 | [RFC 9110](https://www.rfc-editor.org/rfc/rfc9110.html), §§6.4.1, 15.3.5 y 15.5.2 | Límites de contenido HTTP y revisión de autenticación. La transformación JSON no permite añadir cuerpo a 204 ni resuelve por sí sola el desafío 401. |
| S-05 | [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457.html), §§3 y 5 | Forma de errores Problem Details y cuidado al revelar información interna. No demuestra que los errores del proyecto ya sean seguros en todos los casos. |
| S-06 | [Django REST Framework: Exceptions](https://www.django-rest-framework.org/api-guide/exceptions/), Custom exception handling | Handler personalizable para excepciones de vistas; debe distinguirse de respuestas directas. Es contraste de framework, no selección o instalación de DRF. |
| S-07 | [Michael Nygard: Documenting Architecture Decisions](https://www.cognitect.com/blog/2011/11/15/documenting-architecture-decisions), 15 noviembre 2011, decisión/contexto/consecuencias/estado | Justifica conservar motivación y reemplazos sin borrar historia. La guía AA3 amplía la estructura con riesgos y mitigaciones. |
| S-08 | [C4 model: Component diagram](https://c4model.com/diagrams/component), Scope / Primary elements | C3 describe componentes dentro de un contenedor. La propuesta no obliga a añadir servicio desplegable. |

Las páginas Spring consultadas se identificaron como **Spring Framework 7.0.10**, incluidos manual/Javadoc current. Son documentación de mecanismos, no certificación del conjunto resuelto por Boot 4.1.1. Antes de programar, comprobar versiones efectivas y documentación correspondiente. Las URLs current pueden evolucionar; el manifiesto registra fecha UTC de elaboración y esta nota preserva la versión observada. No se descargaron snapshots completos ni se afirma una versión de DRF instalada.

## Procedencia de las conclusiones

- **Documentado:** requisitos, contratos propuestos y decisiones previas P-01–05/P-09.
- **Implementado:** controladores/handlers/writer/consumidores P-06/07.
- **Ejecutado anteriormente:** resultados P-08 en APE3, con límites Spring/SQLite.
- **Inferido/propuesto en AA3:** ventajas y costes de A/B/C, selección C, riesgos y verificaciones PF.
- **Pendiente:** aceptación, cambios de código, prueba de C, validación docente y revisión personal.

El AA3 no modifica estas fuentes ni cambia el estado de los ADR del producto. El manifest de la carpeta registra archivos y checks documentales, sin acreditar quién revisó personalmente la decisión.
