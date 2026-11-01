# APE2 — Contratos preliminares del incremento Unidad 1

**Estado:** fichas académicas preliminares, recuperadas del código existente. No son un OpenAPI definitivo, una API nueva ni evidencia de ejecución. Complementan [specification.md](specification.md).

**Base:** commit `faf868ea`; prefijo HTTP observado `/api/v1`. Los códigos se obtuvieron de controladores, servicios y manejadores de errores, no únicamente del OpenAPI, que presenta divergencias documentadas en la auditoría.

## 1. Convenciones y fuentes

- Solicitudes/respuestas de negocio en JSON; errores estructurados como Problem Details con `code`, `detail`, `correlationId` y campos HTTP relacionados.
- Autenticación existente: cookie `SEC_SESSION`. Las mutaciones autenticadas requieren `X-CSRF-TOKEN`; no se supone JWT/Bearer.
- Consultas de recurso revisable entregan ETag cuando el controlador lo implementa. Cambios existentes de carrera requieren `If-Match` fuerte, por ejemplo `"1"`.
- Los nombres y DTO descritos reflejan el código actual. Los futuros cambios de esquema, versionado o política deben confirmarse; no se corrigen en esta práctica.
- Los campos ID, autor, fecha y revisión los asigna el servidor; no se aceptan como decisiones de autoridad enviadas por el cliente.
- Un ejemplo de resultado parcial en una ficha no reemplaza el DTO completo ni su esquema.

Rutas de fuente, relativas a la raíz:

| Referencia | Archivo |
|---|---|
| API-S | `backend/src/main/java/ec/edu/unl/sec/identityaccess/api/SessionController.java` |
| API-C | `backend/src/main/java/ec/edu/unl/sec/selfassessment/api/CareerController.java` |
| API-M | `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/api/EvaluationModelController.java` |
| API-P | `backend/src/main/java/ec/edu/unl/sec/selfassessment/api/SelfAssessmentController.java` |
| SVC-C | `backend/src/main/java/ec/edu/unl/sec/selfassessment/application/CareerAdministrationService.java` |
| SVC-P | `backend/src/main/java/ec/edu/unl/sec/selfassessment/application/SelfAssessmentService.java` |
| ERR-C | `backend/src/main/java/ec/edu/unl/sec/selfassessment/api/CareerCatalogProblemHandler.java` |
| ERR-P | `backend/src/main/java/ec/edu/unl/sec/selfassessment/api/ProcessExceptionHandler.java` |
| ERR-G | `backend/src/main/java/ec/edu/unl/sec/shared/api/ApiProblemHandler.java` |
| REV | `backend/src/main/java/ec/edu/unl/sec/shared/api/EntityTagSupport.java` |

## 2. CP-01 — Sesión e identidad

- Necesidad/requisitos: N-01; APE-FR-01/02/10, APE-NFR-01.
- Operaciones: POST `/sessions`, GET `/me`, DELETE `/sessions`.
- Entrada de login: `username` (3–100 caracteres), `password` (12–256), conforme al DTO. No publicar valores reales.
- Resultado: POST devuelve 201, cookie y `{userId, expiresAt, csrfToken}`; GET 200 con identidad/funciones; DELETE 204 y cookie expirada, sin cuerpo.
- Permisos: login público; consulta/cierre requieren sesión vigente. Logout autenticado exige CSRF.
- Errores relevantes: 400 entrada inválida; 401 sesión/credenciales inválidas; 403 CSRF; 429 limitación de intentos. Los errores no confirman existencia de la cuenta.
- Escenarios: AC-U1-01–04. Fuente: API-S y componentes de sesión.
- Pendiente: resolver diferencia de consecutividad del umbral antes de declarar cumplimiento de RNF-003.

## 3. CP-02 — Unidad académica disponible

- Necesidad/requisitos: N-02; APE-FR-03/04/09/10.
- Operaciones: GET/POST `/academic-units`; GET `/academic-units/{id}`.
- Entrada GET de colección: `page` (predeterminado 0), `size` (20) y `q` (texto). No se inventa filtro `status` para este listado.
- Entrada POST: `{code, name}`. Requiere sesión, CSRF y capacidad de administración de catálogo.
- Resultado: GET 200 con página o unidad; POST 201 con unidad, Location y ETag. Revisión inicial de unidad 1.
- Errores: 400 datos/paginación inválidos; 401 sin sesión; 403 sin capacidad; 404 detalle no disponible/visible; 409 código duplicado.
- Fuentes: API-C, SVC-C y ERR-C.
- Nota: el alta de unidad puede evitarse en una prueba independiente mediante fixture activo. No se exige implementar una entidad nueva.

## 4. CP-03 — Registrar carrera

- Necesidad/requisitos: N-02; APE-FR-03/09/10.
- Operación: POST `/careers`.
- Entrada: `code` obligatorio hasta 100 caracteres; `name` obligatorio hasta 300; `academicUnitId` UUID obligatorio; `validFrom` fecha obligatoria; `validUntil` fecha o `null`.
- Precondición: administrador autorizado, unidad activa, código normalizado único y vigencia coherente.
- Resultado: 201 con Career; Location `/api/v1/careers/{id}`; ETag `"1"`; estado ACTIVE y revisión 1. Atributos de negocio se serializan dentro de `data`; el cuerpo de entrada es plano.
- Errores: 400 campos/JSON inválidos; 401 sin sesión; 403 capacidad/CSRF; 409 DUPLICATE_CODE; 422 UNIT_UNAVAILABLE cuando la unidad no está disponible.
- Escenarios: AC-U2-01–03/07. Fuentes: API-C, SVC-C y `selfassessment/domain/Career.java`.

Ejemplo sintético de entrada, condicionado a que la unidad exista y esté activa:

```json
{
  "code": "U1-DEMO",
  "name": "Carrera sintética Unidad 1",
  "academicUnitId": "11111111-1111-4111-8111-111111111111",
  "validFrom": "2026-01-01",
  "validUntil": null
}
```

## 5. CP-04 — Consultar catálogo, detalle e historial

- Necesidad/requisitos: N-02/N-04; APE-FR-04/05/10.
- Operaciones: GET `/careers`; GET `/careers/{id}`; GET `/careers/{id}/history`.
- Consulta de catálogo: `page`, `size`, `q`, `status`, `academicUnitId`. Estados del catálogo: ACTIVE/DISABLED.
- Consulta de historial: `page`, `size`. El detalle usa ID de ruta, no cuerpo de solicitud.
- Resultado: 200; listado/historial paginados según CatalogPage, detalle Career con ETag. Los totales deben corresponder al ámbito filtrado.
- Permiso: capacidad de lectura vigente o administración según la política actual; no confundir un administrador global autorizado con un lector acotado a carrera.
- Errores: 400 paginación/filtros inválidos; 401 sesión; 403 falta de capacidad de colección; 404 recurso ajeno/desconocido según servicio.
- Escenarios: AC-U2-04/08. Fuentes: API-C y SVC-C.
- Pendiente: revisar política completa de alcance administrativo institucional; el contrato preliminar no prueba todos sus controles.

## 6. CP-05 — Modificar carrera sin sobrescribir cambios

- Necesidad/requisitos: N-02/N-04; APE-FR-05/09/10 y APE-NFR-04.
- Operación: PATCH `/careers/{id}`.
- Entrada: ID de ruta; cabecera If-Match; mismo CareerInput completo de CP-03. Aunque sea PATCH, el controlador no acepta cualquier subconjunto de campos.
- Precondición: administrador, sesión/CSRF y revisión vigente; unidad activa, código no duplicado y fechas válidas.
- Resultado: 200 con Career, revisión incrementada y nuevo ETag; historial anterior conservado.
- Errores: 400 datos/ETag malformados o débiles; 403 sin capacidad/CSRF; 404 no encontrado; 409 código duplicado; 412 STALE_REVISION; 422 unidad no disponible; 428 falta If-Match.
- Escenarios: AC-U2-04/05/07. Fuentes: API-C, SVC-C y REV.
- Verificación: consultar revisión, modificar desde dos clientes y comprobar que la revisión obsoleta no sobrescribe el cambio exitoso. No se afirma ejecución.

## 7. CP-06 — Desactivar carrera con motivo

- Necesidad/requisitos: N-02; APE-FR-05/09/10.
- Operación: POST `/careers/{id}/state-decisions`.
- Entrada: If-Match y `{status: "DISABLED", reason: "motivo de 10 a 4000 caracteres"}`.
- Resultado: 201 con carrera, revisión incrementada y ETag; no se elimina su ID ni historial.
- Errores: 400 validación de DTO; 403 sin capacidad/CSRF; 404 recurso ausente; 409 STATE_UNCHANGED si no hay cambio; 412 revisión obsoleta; 422 razón inválida cuando la detecta la regla; 428 sin precondición.
- Escenario: AC-U2-06. Fuentes: API-C y SVC-C.
- Límite: desactivar carrera no implica borrar ni cerrar automáticamente procesos anteriores. La reactivación existe en el servicio pero no amplía el recorte propuesto sin revisión del equipo.

## 8. CP-07 — Seleccionar una versión publicada existente

- Necesidad/requisitos: N-03; APE-FR-06.
- Operaciones: GET `/models`; GET `/models/{modelId}/versions`; GET `/model-versions/{id}`. Existen además variantes `/configured`, pero no se exige migrarlas en esta práctica.
- Entrada: identidad autorizada, modelo o versión existente. Sin cuerpo en estas consultas.
- Resultado: 200 con catálogo/lista/versión; el detalle entrega ETag. Identificar ID, número y estado de publicación.
- Validación: el proceso solo puede prepararse con versión publicada disponible; no seleccionar automáticamente “la última” por nombre.
- Fuentes: API-M, SVC-P y el puerto de catálogo publicado.
- Límite: consultar o publicar técnicamente no demuestra aplicabilidad normativa para activación institucional. Publicación/activación quedan fuera del recorte U1.

## 9. CP-08 — Preparar proceso

- Necesidad/requisitos: N-03; APE-FR-07/09/10.
- Operación: POST `/processes`.
- Entrada: `careerId`, `code` (hasta 100), `name` (hasta 300), `periodStart`, `periodEnd`, `modelVersionId`, todos obligatorios. No se recibe un campo `state` para que el cliente fuerce activación.
- Precondición: capacidad PROCESS_MANAGE en carrera, identidad humana habilitada, carrera/unidad activas y período dentro de vigencia, versión publicada disponible.
- Resultado: 201 con SelfAssessmentProcess; Location `/api/v1/processes/{id}`; estado PREPARATION; revisión inicial 0 y ETag `"0"`. La referencia conserva `modelVersionId` y `modelVersionNumber`.
- Errores relevantes: 400 DTO o MODEL_VERSION_NOT_PUBLISHED; 401 sin sesión; 403 PROCESS_SCOPE_DENIED; 404 carrera inexistente/no visible según regla; 409 CAREER_INACTIVE; 422 UNIT_UNAVAILABLE o CAREER_PERIOD_MISMATCH. Diferenciar error de tipo/sintaxis de regla de período.
- Escenarios: AC-U3-01–04/06. Fuentes: API-P, SVC-P, SVC-C y ERR-P/ERR-C.
- Límite: aquí no se verifican todas las reglas de aplicabilidad normativa de activación; no declarar que el alta concede autorización institucional.

Ejemplo sintético de entrada, condicionado a fixtures existentes:

```json
{
  "careerId": "22222222-2222-4222-8222-222222222222",
  "code": "PROCESO-U1-DEMO",
  "name": "Preparación sintética Unidad 1",
  "periodStart": "2026-01-01",
  "periodEnd": "2026-12-31",
  "modelVersionId": "33333333-3333-4333-8333-333333333333"
}
```

## 10. CP-09 — Consultar proceso preparado

- Necesidad/requisitos: N-03; APE-FR-08/10.
- Operaciones: GET `/processes/{id}` y GET `/processes`.
- Entrada: sesión y ID; el listado actual no declara paginación en su controlador. No inventar `page/size` como contrato implementado de procesos.
- Resultado: 200 con proceso o lista filtrada; detalle con ETag. Permisos y proyección visible pueden diferir según funciones/indicadores. En preparación se consulta con un actor de gestión autorizado, no se presume acceso de un responsable sin indicadores asignados.
- Errores: 401 sin sesión; 404 PROCESS_NOT_FOUND para recurso ausente/no visible según SVC-P.
- Escenarios: AC-U3-05/06. Fuentes: API-P y SVC-P.

## 11. Qué debe decidirse antes de convertir estas fichas en contrato definitivo

| Punto | Evidencia disponible | Decisión pendiente |
|---|---|---|
| Alcance de recursos de U1 | CP-01–09 sobre rutas existentes. | Aceptación de CL-APE-01. |
| Nombres, esquemas y versión de API | Código v1 y OpenAPI común; discrepancias conocidas. | Reconciliar sin romper clientes; no presentar v2 como disponible. |
| Permisos administrativos | Política implementada y reglas institucionales pendientes. | Confirmar competencia y alcance real. |
| Errores y paginación | 400/422 dependen de regla; catálogo paginado, procesos no. | Documentar por operación; evitar una regla genérica falsa. |
| Concurrencia | ETag/If-Match actuales. | Mantener revisiones de cada agregado; no asumir todas empiezan en 1. |
| Validación de interfaces | Fuentes y pruebas existentes. | Ejecutar posteriormente pruebas de esquema/HTTP en entorno sintético. |

Los estándares HTTP y OpenAPI ayudan a describir operaciones y representaciones, pero su existencia no obliga a que esta práctica produzca ya un OpenAPI final. Referencias: [RFC 9110](https://www.rfc-editor.org/rfc/rfc9110.html) y [OpenAPI](https://spec.openapis.org/oas/latest.html). No se cambia la versión OpenAPI del repositorio por consultar el estándar actual.

