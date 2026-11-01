# ADR-AA3-001 — Adaptación explícita de respuestas en la frontera API

**Estado: Propuesto.** Selección argumentada para revisión individual/equipo; no aceptado, implementado ni reemplazo de ADR del producto.

**Ámbito:** presentación del backend SIGEAC para los flujos priorizados de catálogo y preparación/consulta. **Autor y fecha de aprobación:** pendientes de evidencia real. El timestamp de generación de archivos no acredita autoría o decisión humana.

## Contexto

AA-RF-14 exige rechazos estables y seguros, AC-AA-26 identifica código/descripción/correlación; AA-RNF-03/07/09/10 respaldan privacidad, diagnóstico, separación y coherencia contractual. Se relacionan con N-AA-05 y los actores autorizados A-AA-01–04. El requisito de sobre `success/message/data/errors/meta` procede de la guía académica, no de una decisión de producto ya aprobada. Debe resolverse esa aceptación antes de cambiar la API.

El producto actual es un monolito modular Spring; los controladores retornan representaciones directas, handlers retornan ProblemDetail y AccessProblemWriter escribe JSON desde la infraestructura de identidad. Los clientes consumen `session.csrfToken`, `CatalogPage.items` y campos de Problem Details. Una envoltura cambia esas expectativas. ADR-0018 impide reemplazar silenciosamente DTO v1; el objetivo v2 existente de 005 tiene otros compromisos, no está libre para adjudicarle este cambio automáticamente.

APE3-002 identifica la incompatibilidad pero deja abierta la organización interna que produciría el sobre. Debe analizarse antes de repartir construcción de JSON entre negocio y transporte o activar una envoltura transversal. Las 22 respuestas comprobadas en APE3 sirven de línea base del formato actual, no demuestran una alternativa aún no implementada.

**Restricciones:** preservar permisos/ámbitos, CSRF, revisión, transacciones y auditoría; no alterar cuerpo prohibido en 204; conservar semántica de estado/media type y headers; no sustituir autoridad institucional por rol técnico; no asumir migración a DRF ni excepción docente concedida. Si se confirma DRF, hay que revalidar los mecanismos concretos con su versión.

## Alternativas consideradas

| Opción | Descripción | Ventaja | Limitación/riesgo |
|---|---|---|---|
| A | Construir sobre por controlador y adaptar cada handler/writer. | Salida visible, pocas piezas nuevas; razonable con pocas operaciones. | Coordinación distribuida y posible divergencia de campos/reglas. |
| B | Envoltura transversal MVC, por ejemplo ResponseBodyAdvice, más integración de seguridad/errores. | Centraliza transformaciones interceptables y evita invocaciones repetidas. | Exige opt-in/exclusiones, revisar conversores y caminos fuera de MVC; puede ocultar el contrato real de una operación. |
| C | Adaptadores/mapeos explícitos de presentación y política común para escritores relevantes. | Mantiene visible la salida y concentra reglas de representación sin introducirlas en negocio. | Añade mapeos/tipos y coordinación entre API y seguridad; puede sobredimensionarse. |

A y B son viables si incorporan todos sus controles. C no se justifica porque las otras sean imposibles, sino por la necesidad local de hacer explícita la compatibilidad en varios caminos de salida. La comparación detallada y CR-01–07 están en el informe AA3.

## Decisión

**Seleccionar C como propuesta de organización**, condicionada a que se confirme la adopción del sobre. Ubicar la traducción de resultados a contrato HTTP en la frontera de presentación, con mapeos explícitos por representación y política compartida de salida pública. No construir el sobre en servicios de aplicación, objetos de dominio o repositorios.

Los handlers y el escritor de seguridad deben aplicar la política acordada mediante sus propios puntos de integración; no dar por hecho que un advice MVC procesa toda respuesta servlet. Conservar 204 sin cuerpo y excluir otros intercambios que no admitan ese JSON. No envolver automáticamente binarios, streams, documentación o endpoints fuera de la superficie aprobada.

Aplicar una migración coordinada de contrato y cliente, cuya versión/frontera debe acordarse. No se elige una nueva URL, se altera v1, se habilita v2 ni se aprueba el sobre mediante este ADR. Para paginación, resolver si se mantiene CatalogPage dentro de data o se trasladan campos a meta: ACD3 y APE3 registraron variantes, todavía no hay acuerdo único. No combinar ambas duplicando totales.

## Justificación

CR-01/02 dan prioridad a compatibilidad y salidas correctas: el cliente y el writer actuales muestran varias fronteras que un cambio debe cubrir. CR-03/04 favorecen separar formato, localizar mapeos y relacionarlos con pruebas. C ofrece esa visibilidad sin trasladar reglas a controladores o aplicación. CR-05 limita el diseño: solo tipos/mapeos necesarios para operaciones aceptadas, sin motor genérico de serialización. CR-06 distingue política de hooks del framework; CR-07 exige seleccionar explícitamente campos públicos y correlación.

La documentación oficial de Spring respalda ResponseEntity/ProblemDetail y los puntos de extensión; no prescribe C como superior. **La selección es una inferencia de diseño** basada en requisitos/código y en la incertidumbre de migración. No se obtuvieron métricas comparativas de rendimiento, defectos, horas o aprendizaje. No se afirma mejora automática de seguridad o escalabilidad.

## Responsabilidades y dependencias

API: seleccionar método/estado/headers y mapear entrada/salida. Aplicación: ejecutar caso, reglas de autorización, transacción y puertos. Dominio: invariantes/estados. Infraestructura: persistir o integrar, respetando contratos; el writer de identidad es una frontera HTTP que debe coordinar la representación sin mezclarse con SQL.

C4: sin cambios a C1/C2. Refinar C3 en `sigeac.api.http` y su relación con la salida de `sigeac.api.acceso`, del workspace APE3. Catálogo, procesos, dominio y repositorios no incorporan el sobre como obligación interna. Los nombres concretos de clases nuevas se definirán después; no se documentan como existentes.

## Consecuencias

**Positivas esperadas:** mapeo público localizable y trazable; separación entre negocio y representación; pruebas de contrato con puntos de entrada identificables; una política revisable de metadatos/errores. Son beneficios previstos, no resultados medidos.

**Negativas/costes:** trabajo inicial de tipos/mapeos, modificación coordinada de consumidor y esquemas, revisión de múltiples escritores y mantenimiento de compatibilidad durante transición. C no evita que un mapeador mal programado omita campos o filtre secretos. No se justifica migrar toda la API ni añadir otro servicio por este ADR.

## Riesgos y mitigaciones

| ID | Riesgo concreto | Mitigación y comprobación futura |
|---|---|---|
| RK-AA3-01 | Cliente v1 deja de leer csrfToken/items/errores. | Acordar estrategia de contrato; conservar pruebas del formato mantenido y ejecutar cliente migrado. PF-AA3-04. |
| RK-AA3-02 | Error del filtro queda fuera de política. | Inventariar writers; probar rechazo sin sesión y CSRF desde integración completa. PF-AA3-02. |
| RK-AA3-03 | Se añade cuerpo a 204 o cambian headers. | Exclusiones y aserciones de bytes/estado/Location/ETag/Set-Cookie por operación. PF-AA3-01/03. |
| RK-AA3-04 | Mensajes/detalles exponen secretos o información ajena. | Lista de campos públicos, saneamiento de diagnóstico y marcadores sintéticos; probar permisos/cuerpos/registros pertinentes. PF-AA3-02. |
| RK-AA3-05 | Se crean mapeadores genéricos innecesarios o reglas entran en presentación. | Limitar superficie; revisar dependencias y reglas ArchUnit. PF-AA3-05. |
| RK-AA3-06 | Se aplica un hook/versionado de framework no confirmado. | Confirmar Spring/DRF y versión resuelta antes de implementar; repetir evaluación de integración. |
| RK-AA3-07 | OpenAPI, paginación, media type y cuerpo divergen. | Acordar un schema aceptado, validar muestras y mantener pares request/response y casos negativos. PF-AA3-01/02. |

HTTP-AA-01, 401 sin desafío explícito, sigue abierto: rediseñar el JSON no lo resuelve. La aceptación del ADR debe contemplar este límite de AA-RNF-10 sin inventar un esquema Bearer.

## Relación con implementación y pruebas

Línea base: SessionController, CareerController, ApiProblemHandler, CareerCatalogProblemHandler y AccessProblemWriter; AccessApi.rememberCsrf e isApiProblem del cliente; contratos/evidencias APE3. Rutas completas en [FUENTES.md](FUENTES.md).

Ver [TRAZABILIDAD_ADR.md](TRAZABILIDAD_ADR.md): RF/RNF→ADR→componente C4→implementación existente o cambio propuesto→prueba existente o PF futura. No se marca como verificada una relación con código nuevo inexistente. Las reglas `DOMAIN_IS_INDEPENDENT` y `APPLICATION_IGNORES_API_AND_INFRASTRUCTURE` existen en ModuleArchitectureTest; no verifican por sí solas el contenido JSON, la privacidad o los caminos de seguridad.

## Condiciones de aceptación y revisión

Confirmar sobre y tecnología, elegir política de paginación/compatibilidad, identificar responsable y fecha, actualizar contrato/tareas, implementar superficie acotada y pasar PF-AA3-01–05 con evidencia. La aprobación institucional de actores no se obtiene mediante este ADR. Registrar quién acepta y el vínculo verificable antes de cambiar Estado a Aceptado.

Revisar/reemplazar si se conserva formato actual, cambia el framework, aparece un cliente adicional, se incorporan streams/otros media types o aumenta el coste de mapeo. Conservar historial del ADR si cambia la decisión.

## Referencias

- P-01–09: requisitos, C4, decisiones, implementación, consumidor y evidencia del proyecto, en [FUENTES.md](FUENTES.md).
- [S-01 Spring: Return Values](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/return-types.html).
- [S-02 Spring: Error Responses](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html).
- [S-03 ResponseBodyAdvice](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/mvc/method/annotation/ResponseBodyAdvice.html).
- [S-04 RFC 9110](https://www.rfc-editor.org/rfc/rfc9110.html), §§6.4.1 y 15.3.5.
- [S-05 RFC 9457](https://www.rfc-editor.org/rfc/rfc9457.html), §§3 y 5.
- [S-06 DRF: Exceptions](https://www.django-rest-framework.org/api-guide/exceptions/), manejo personalizado y alcance.
- [S-07 Nygard: Documenting Architecture Decisions](https://www.cognitect.com/blog/2011/11/15/documenting-architecture-decisions).
- [S-08 C4: Component diagram](https://c4model.com/diagrams/component).

La referencia Spring consultada se identificó como Framework 7.0.10; no certifica por sí sola las dependencias exactas resueltas por el parent Boot 4.1.1 del proyecto. Verificar versión/mecanismo antes de implementar. No se modificó código ni se ejecutaron nuevas pruebas en este AA.
