# Fuentes compartidas — Semanas 4 y 5

## Evidencia del repositorio

| ID | Rutas desde la raíz / símbolo relevante | Relación y límite |
|---|---|---|
| F-01 | `docs/requirements/spec.md`, RNF-001–005/011–014/017–019; `documentos-academicos/AA2-semana-2/DOSSIER_REQUISITOS_AA2.md`; `documentos-academicos/APE3-semana-3/TRAZABILIDAD.md` | Requisitos, AC y verificaciones. No inventar aprobaciones institucionales. |
| F-02 | `backend/src/main/java/ec/edu/unl/sec/identityaccess/application/SessionService.java`, login/authenticateForRequest/logout; `backend/src/main/java/ec/edu/unl/sec/identityaccess/infrastructure/SessionCookie.java`; `backend/src/main/java/ec/edu/unl/sec/identityaccess/infrastructure/SessionAuthenticationFilter.java` | Sesión aleatoria, hashes, expiración, cookie y CSRF propio. No deducir ausencia de CSRF por deshabilitar el mecanismo estándar. |
| F-03 | `backend/src/main/java/ec/edu/unl/sec/identityaccess/application/ScopeAuthorizationService.java`; `backend/src/main/java/ec/edu/unl/sec/identityaccess/infrastructure/SecurityConfiguration.java`; `backend/src/main/java/ec/edu/unl/sec/identityaccess/infrastructure/JpaIdentityAccessRepository.java`, countFailures | Capacidad/ámbito y política de autenticación; countFailures no recorta tras éxito. |
| F-04 | `backend/src/main/java/ec/edu/unl/sec/selfassessment/api/CareerController.java`; `backend/src/main/java/ec/edu/unl/sec/selfassessment/application/CareerAdministrationService.java`; `backend/src/main/java/ec/edu/unl/sec/selfassessment/domain/Career.java`; `backend/src/main/java/ec/edu/unl/sec/selfassessment/infrastructure/JdbcCareerCatalogRepository.java` | Validación sintáctica/de negocio, normalización, unicidad, visibilidad, revisiones y auditoría. |
| F-05 | `backend/src/main/java/ec/edu/unl/sec/selfassessment/api/SelfAssessmentController.java`; `backend/src/main/java/ec/edu/unl/sec/selfassessment/application/SelfAssessmentService.java`, list/create; `backend/src/main/java/ec/edu/unl/sec/selfassessment/infrastructure/JdbcSelfAssessmentRepository.java`, list | Preparación y consulta; listado materializa sin LIMIT antes de filtrar. DoS no demostrado. |
| F-06 | `backend/src/main/resources/application.yml`; `backend/src/main/resources/application-prod.yml`; `backend/.env.example`; `.gitignore`; `.github/workflows/backend-ci.yml` | Perfil, parámetros, variables y CI manual con PostgreSQL/contraseña sintética. Ejemplos no equivalen a credenciales reales ni entorno desplegado. |
| F-07 | `backend/src/main/java/ec/edu/unl/sec/shared/api/ApiProblemHandler.java`; `backend/src/main/java/ec/edu/unl/sec/identityaccess/infrastructure/AccessProblemWriter.java`; `backend/src/main/java/ec/edu/unl/sec/shared/api/EntityTagSupport.java` | Errores seguros por intención, correlación y precondiciones. Revisar cuerpo real; no afirmar ausencia total de secretos por el nombre de handler. |
| F-08 | `backend/src/test/java/ec/edu/unl/sec/identityaccess/application/SessionServiceTest.java`; `backend/src/test/java/ec/edu/unl/sec/identityaccess/application/ScopeAuthorizationServiceTest.java`; `backend/src/test/java/ec/edu/unl/sec/selfassessment/application/CareerAdministrationServiceTest.java`; `backend/src/test/java/ec/edu/unl/sec/selfassessment/api/CareerControllerTest.java`; `backend/src/test/java/ec/edu/unl/sec/selfassessment/domain/CareerTest.java`; `backend/src/test/java/ec/edu/unl/sec/selfassessment/application/SelfAssessmentAuditServiceTest.java` | Tests existentes inspeccionados; no ejecutados nuevamente en Semanas 4–5. |
| F-09 | `documentos-academicos/APE3-semana-3/evidencias/http-resultados.json`; `pruebas-maven.json`; `http-intento-inicial.json`; `esquemas-resultados.json`, bajo la misma carpeta | Evidencia previa, 22 casos HTTP finales y 23 tests seleccionados. Muestras no son nuevas pruebas APE4, ni cubren toda seguridad. |
| F-10 | `documentos-academicos/APE3-semana-3/arquitectura/workspace.dsl`; `documentos-academicos/APE3-semana-3/contratos/openapi-observado.yaml`; `openapi-objetivo.yaml` en la misma carpeta contratos; `documentos-academicos/AA3-semana-3/ADR-AA3-001-adaptacion-respuestas-api.md` | C4, contrato real/objetivo y propuesta; sobre universal y DRF pendientes. |
| F-11 | `securityOutputs/run-1/run-metadata.json`; `securityOutputs/run-1/findings.json`; `securityOutputs/run-1/coverage-ledger.json` | Antecedente incompleto de otro commit; siete needs_validation, ninguno confirmado. No heredar conclusión ni severidad sin revalidación independiente. |
| F-12 | `backend/pom.xml`; `.specify/memory/constitution.md`; `docs/adr/0001-modular-monolith.md`; `docs/adr/0018-catalog-contract-migration-compatibility.md`; `docs/technical/quickstart.md` | Stack real Java21/Boot4.1.1 configurados, límites SDD/compatibilidad y operación existente. JDK local25 no redefine nivel de compilación. |

Los archivos auxiliares nuevos de APE4/APE5 son propuestas de tests/configuración. Una comprobación de sintaxis de Python/YAML o un manifiesto no prueba autenticación, aislamiento, build, contenedorización ni despliegue.

## Referencias oficiales consultadas

| ID | Referencia | Uso acotado |
|---|---|---|
| O-01 | [OWASP API1:2023 BOLA](https://api-security.owasp.org/editions/2023/en/0xa1-broken-object-level-authorization/) | Revisar autorización por objeto, no solo sesión o UUID. |
| O-02 | [OWASP API4:2023 Resource Consumption](https://api-security.owasp.org/editions/2023/en/0xa4-unrestricted-resource-consumption/) | Analizar límites de listado y coste de peticiones; no demuestra DoS del proyecto. |
| O-03 | [OWASP Authentication](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html), Login Throttling / Responses | Política por cuenta y respuesta uniforme; un registro de IP no equivale a admisión global limitada. |
| O-04 | [OWASP Session Management](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html), Cookies / Expiration | Revisar Secure/HttpOnly/SameSite y expiración; el entorno efectivo importa. |
| O-05 | [OWASP Authorization](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html), Least Privileges / Every Request | Denegar por defecto y verificar permiso de cada operación/recurso. |
| O-06 | [OWASP Secrets Management](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html) | Gestión externa, alcance y ciclo de vida; no publicar los valores hallados. |
| O-07 | [OWASP Error Handling](https://cheatsheetseries.owasp.org/cheatsheets/Error_Handling_Cheat_Sheet.html) | Diagnóstico con información pública acotada; separar registro técnico del cuerpo de respuesta. |
| O-08 | [OWASP Input Validation](https://cheatsheetseries.owasp.org/cheatsheets/Input_Validation_Cheat_Sheet.html) | Validación sintáctica y semántica; no sustituye permisos. |
| O-09 | [Microsoft: amenazas STRIDE](https://learn.microsoft.com/en-us/azure/security/develop/threat-modeling-tool-threats) | Estructura de amenazas pertinentes, sin forzar categorías vacías. |
| D-01 | [Docker Compose: services](https://docs.docker.com/reference/compose-file/services/) | Configuración de servicio, límites, usuario/volúmenes; debe validarse con motor/versiones. |
| D-02 | [Docker Compose: networks](https://docs.docker.com/reference/compose-file/networks/), internal | Red interna propuesta; no certifica sandbox o host. |
| D-03 | [Imagen oficial PostgreSQL](https://raw.githubusercontent.com/docker-library/docs/master/postgres/README.md), PGDATA | Montaje PostgreSQL18 bajo /var/lib/postgresql; verificar imagen fijada y migración. |
| D-04 | [Spring Boot: Container Images](https://docs.spring.io/spring-boot/reference/packaging/container-images/index.html) | Dockerfile/Buildpacks como opciones; página consultada identifica Boot4.1.2, no actualiza el POM4.1.1. |
| D-05 | [pytest: skip y xfail](https://docs.pytest.org/en/stable/how-to/skipping.html) | Una omisión es limitación, no test satisfactorio. |
| H-01 | [RFC9110](https://www.rfc-editor.org/rfc/rfc9110.html), §§9/15.5.2; [RFC9457](https://www.rfc-editor.org/rfc/rfc9457.html) | Semántica HTTP, desafío401 y Problem Details; sobre académico distinto. Revisadas en actividades previas. |

Estas fuentes guían el análisis; no se copian listas OWASP como hallazgos. Las inferencias locales y las valoraciones P/I se identifican en las matrices. No se investigaron CVE nuevas ni se hizo una SCA transitiva completa; no se certifican dependencias por no encontrar patrones de secretos.

## Ejecución y límites

La habilidad de seguridad exige aislamiento OS con red/entorno/escrituras/recursos restringidos. Docker y Podman no estaban disponibles; WSL visible no acredita por sí solo una distribución aislada configurada. El proceso de auditoría queda incompleto por esa capacidad y no ejecuta código del target. Se conservaron metadatos de escaneo confiable de texto y antecedentes; no se realizaron ataques ni pruebas en servicios ajenos.

Las actividades académicas contienen propuestas y análisis completos hasta donde hay evidencia, pero las partes de implementación, reejecución y despliegue siguen pendientes. No se produjeron aprobaciones, correcciones funcionales, commits o feedback grupal ficticios.
