# AA3 — Matriz de trazabilidad del ADR

ADR-AA3-001 está **Propuesto**, con alternativa C seleccionada para revisión. P-XX/S-XX remiten a [FUENTES.md](FUENTES.md). Los PF son verificaciones futuras, no pruebas ejecutadas ni requisitos oficiales nuevos.

| Origen / necesidad | Requisito y criterio | Riesgo / criterio comparativo | ADR y componente C4 | Código o contrato identificado | Evidencia y estado |
|---|---|---|---|---|---|
| N-AA-05, comprender rechazo; A-AA-01–04 | AA-RF-14; AC-AA-26 | RK-AA3-02/04/07; CR-02/07 | ADR-AA3-001 C; http y acceso | ApiProblemHandler, CareerCatalogProblemHandler, AccessProblemWriter; OpenAPI observado/objetivo | Errores actuales en APE3 HTTP-01/04/05/07/09/11/14/16. PF-AA3-02 pendiente para C. |
| N-AA-01/04, privacidad | AA-RNF-03; VC-AA-03 | RK-AA3-04; CR-07 | Política pública propuesta en frontera | Cuerpos/handlers y ámbitos existentes; no mapper C implementado | Redacción de evidencia APE3 no demuestra privacidad completa. Prueba con secretos/ámbitos pendiente. |
| N-AA-05, diagnóstico | AA-RNF-07; VC-AA-07 | RK-AA3-02/04; CR-07 | http/acceso y correlación | CorrelationFilter y extensiones de error actuales | Muestras incluyen correlationId; relación completa con logs/éxitos y contrato futuro pendiente. |
| N-AA-05, reglas verificables | AA-RNF-09; VC-AA-09 | RK-AA3-05; CR-03/04 | Mantener aplicación/dominio fuera de sobre | ModuleArchitectureTest: DOMAIN_IS_INDEPENDENT, APPLICATION_IGNORES_API_AND_INFRASTRUCTURE | APE3 registró ejecución de suite de arquitectura; no prueba C inexistente. PF-AA3-05 pendiente. |
| N-AA-04/05, intercambio interpretable | AA-RNF-10; VC-AA-10 | RK-AA3-01/03/07; CR-01/02 | Adaptadores explícitos propuestos | Dos OpenAPI de APE3, contratos distintos | 22 muestras conformes al observado; cero pruebas del sobre objetivo. PF-AA3-01 pendiente. |
| N-AA-01, terminar acceso | AA-RF-02; AC-AA-03 | RK-AA3-03; CR-02 | Frontera de sesión preserva respuesta vacía | SessionController.revokeCurrentSession; DELETE sessions | APE3 HTTP-20/21: 204 cero bytes y acceso posterior rechazado. Regresión futura PF-AA3-03. |
| Consumidor funcional / compatibilidad | ADR-0018; AA-RNF-10 | RK-AA3-01; CR-01/05 | Cambios coordinados de cliente y API | AccessApi.rememberCsrf, SessionView, isApiProblem, catálogo items | Lectura de código, sin nueva ejecución Angular en AA3. PF-AA3-04 pendiente. |
| Exigencia académica de sobre | Guías ACD3/APE3; APE3-002 propuesto | RK-AA3-06/07; CR-05/06 | Condición de aceptación del ADR | openapi-objetivo.yaml declara no implementación | Política de paginación y versión/frontera pendientes. No declarar aprobado por estar especificado. |
| Tecnología y arquitectura existentes | Constitución; ADR-0001/0004/0018; APE3-001 propuesto | RK-AA3-06; CR-03/06 | Backend único; C3 http/acceso | POM Java21/Boot4.1.1, paquetes Spring; DSL APE3 | DRF exigido en APE3 sigue sin resolución. Documentación oficial consultada no decide por el equipo. |

## Verificaciones propuestas

| ID | Qué comprobar | Evidencia que deberá producirse |
|---|---|---|
| PF-AA3-01 | Contrato aceptado, estado, schema, Content-Type y headers de cada operación incluida | Prueba de contrato más capturas redactadas de intercambios, versión y resultado. |
| PF-AA3-02 | Errores MVC y seguridad, campos públicos, privacidad y correlación | Casos negativos de integración; lector autorizado/ajeno; diagnóstico sin secretos. |
| PF-AA3-03 | 204 sin cuerpo y exclusiones semánticas | Aserción de cero bytes y headers, regresión del logout; otros métodos/estados solo si la superficie los implementa. |
| PF-AA3-04 | Cliente conservado y migrado interpretan su contrato | Pruebas de login/CSRF, catálogo/páginas y errores sobre entorno/cliente acordados. |
| PF-AA3-05 | Aplicación/dominio sin dependencias de presentación, reglas de negocio fuera de adaptación | ArchUnit, revisión de dependencias y pruebas de reglas pertinentes; registrar límites de SQL y serialización. |

La documentación establece relaciones comprobables con artefactos actuales; la alternativa seleccionada sigue siendo una propuesta. Ningún hash, diagrama o test de APE3 sustituye la aceptación humana ni la verificación futura.
