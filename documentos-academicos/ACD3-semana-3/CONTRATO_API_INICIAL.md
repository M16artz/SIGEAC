# ACD3 — Recursos y contrato inicial de API

## 1. Estado y reglas de interpretación

**Base implementada:** `/api/v1`, configurada en `backend/src/main/resources/application.yml`. **Alcance:** 18 operaciones seleccionadas de sesiones, catálogo, modelos y procesos. No es un inventario completo de la API ni un OpenAPI nuevo. La propuesta de sobre común de la sección 4 **no está implementada** en estos endpoints. Rutas, DTO y estados observados se mantienen separados del objetivo académico.

Fuentes principales: `SessionController`, `CareerController`, `EvaluationModelController`, `SelfAssessmentController` y sus servicios/handlers, con rutas exactas en [FUENTES.md](FUENTES.md). El contrato v1 está en `docs/technical/contracts/openapi.yaml`; `specs/005-complete-evaluation-workflows/contracts/openapi.yaml` contiene además diseño objetivo v2 y excepciones v1. No se combinan ambos como si todo estuviera desplegado.

**Seguridad común S:** cookie de sesión `SEC_SESSION`; comprobar actor humano y capacidad/ámbito; para mutaciones autenticadas, `X-CSRF-TOKEN` vigente. POST de sesión está exceptuado del CSRF de este filtro. A una lectura protegida sin sesión le corresponde 401; una capacidad denegada puede producir 403 y un identificador fuera del ámbito puede ocultarse con 404. El resultado exacto depende del servicio y orden de rechazo. No se usa `Authorization: Bearer` en los ejemplos.

**Resultados transversales:** 400 para JSON/UUID/DTO inválidos; 401 falta de autenticación; 403 denegación o CSRF inválido en mutaciones; 500 fallo técnico no controlado. Estas posibilidades no prueban que todos ocurran en toda operación. 429 está asociado al rechazo de autenticación por intentos; no se generaliza a cada recurso.

## 2. Tabla de operaciones observadas

Las rutas de la tabla incluyen la base real. Los cuerpos se detallan en sección 3. `GET` no recibe cuerpo en estas operaciones. `size` no existe en el listado de procesos.

| ID / requisito AA2 | Método y ruta | Entrada y permiso | Respuesta de éxito observada | Rechazos relevantes y causa |
|---|---|---|---|---|
| OP-01 / AA-RF-01 | POST `/api/v1/sessions` | Login; sin sesión previa | 201; SessionView; Set-Cookie | 400 formato; 401 credenciales rechazadas; 429 límite de intentos. |
| OP-02 / AA-RF-02 | DELETE `/api/v1/sessions` | S; sin cuerpo | 204 sin cuerpo; borrar cookie | 401 falta de sesión; 403 CSRF. |
| OP-03 / AA-RF-03 | GET `/api/v1/me` | S; sin parámetros | 200; identidad y funciones/ámbitos vigentes propios | 401 no autenticado. |
| OP-04 / AA-RF-05 | POST `/api/v1/academic-units` | UnitInput; S y CAREER_ADMIN | 201; AcademicUnit; Location y ETag | 400 entrada; 403 capacidad; 409 código duplicado. |
| OP-05 / AA-RF-07 | GET `/api/v1/academic-units` | page, size, q; S y lectura de catálogo aplicable | 200; página de unidades visibles | 400 paginación/búsqueda; 403 sin capacidad de lectura. |
| OP-06 / AA-RF-07 | GET `/api/v1/academic-units/{id}` | UUID; S y visibilidad de unidad | 200; AcademicUnit; ETag | 400 UUID; 404 inexistente/no visible. |
| OP-07 / AA-RF-06 | POST `/api/v1/careers` | CareerInput; S y CAREER_ADMIN | 201; Career; Location y ETag | 400 entrada/fechas; 403 capacidad; 409 código duplicado; 422 unidad no disponible. |
| OP-08 / AA-RF-07 | GET `/api/v1/careers` | page, size, q, status, academicUnitId; S y lectura visible | 200; página de carreras con total filtrado | 400 filtros/formato/página; 403 sin lectura. |
| OP-09 / AA-RF-07/08 | GET `/api/v1/careers/{id}` | UUID; S y acceso aplicable | 200; Career; ETag | 400 UUID; 404 inexistente/no visible. |
| OP-10 / AA-RF-08 | PATCH `/api/v1/careers/{id}` | UUID, If-Match, CareerInput completo; S y CAREER_ADMIN | 200; Career con nueva revisión y ETag | 400 entrada/ETag; 404 ID; 409 duplicado; 412 revisión vieja; 422 unidad; 428 falta If-Match. |
| OP-11 / AA-RF-07/08 | GET `/api/v1/careers/{id}/history` | UUID, page, size; S y visibilidad | 200; página de revisiones | 400 página; 404 carrera no visible/inexistente. |
| OP-12 / AA-RF-09 | POST `/api/v1/careers/{id}/state-decisions` | UUID, If-Match, StateChange; S y CAREER_ADMIN | 201; Career y ETag; sin Location explícita en controlador | 400 DTO/ETag; 404 ID; 409 sin cambio o reactivación no posible; 412 revisión; 422 motivo/unidad según escenario; 428 sin precondición. |
| OP-13 / AA-RF-10 | GET `/api/v1/models` | S y MODEL_READ | 200; lista de modelos | 401 identidad; 403 capacidad según servicio. |
| OP-14 / AA-RF-10 | GET `/api/v1/model-versions/{id}` | UUID; S y lectura de modelo | 200; LegacyModelVersion y ETag cuando la estructura es compatible | 400 UUID; 404 versión inexistente/no visible; 409 MODEL_VERSION_REQUIRES_V2 cuando requiere representación configurada. Consultar no garantiza que esté publicada. |
| OP-15 / AA-RF-12 | GET `/api/v1/processes` | S; filtrar por capacidades/ámbitos; sin paginación declarada | 200; arreglo de procesos visibles | 401 identidad; no prometer total/paginación. |
| OP-16 / AA-RF-12 | GET `/api/v1/processes/{id}` | UUID; S y acceso a proceso/indicadores según servicio | 200; SelfAssessmentProcess y ETag | 400 UUID; 404 inexistente/no visible. |
| OP-17 / AA-RF-04/07 | GET `/api/v1/careers/access` | S y acceso al catálogo | 200; `{ "canManage": true }` o false según permiso | 403 cuenta técnica o sin capacidad de catálogo. No reemplaza autorización de mutaciones. |
| OP-18 / AA-RF-11 | POST `/api/v1/processes` | ProcessCommand; S y PROCESS_MANAGE en carrera | 201; SelfAssessmentProcess; Location, ETag | 400 DTO/período invertido/modelo no publicado; 403 ámbito; 404 carrera ausente; 409 carrera inactiva; 422 período fuera de vigencia. |

AA-RF-13/14 y RNF de privacidad, auditoría, correlación y contrato son transversales. La existencia de un código en el servicio no demuestra su respuesta integrada: validar con pruebas/solicitudes en APE3. No se añade 422 a todo error de validación: los DTO actuales usan 400. No se asume 409 por cualquier error SQL.

## 3. Entradas, restricciones y representaciones actuales

### 3.1 Parámetros y paginación

Los `id` y `academicUnitId` son UUID. Catálogos/historial usan `page` desde 0, `size` por defecto 20, permitido 1–100; `page` debe ser no negativo y no superar `Integer.MAX_VALUE/100`. `q` por defecto vacío, longitud máxima 300, recortado por el servicio. Carreras admite `status` ACTIVE o DISABLED y filtro de unidad. No inventar filtros de fecha o de modelo para `/processes`.

`If-Match` se obtiene de ETag del detalle actual, por ejemplo `"1"`. `EntityTagSupport` admite un único entero no negativo entre comillas fuertes; rechaza forma débil y otras formas con 400, ausencia con 428. La gramática general de HTTP es más amplia que este subconjunto del proyecto; revisar esa restricción como decisión de contrato, no enseñarla como gramática universal. Revisión vieja → 412 sin sobrescritura.

### 3.2 Cuerpos

| Tipo | Campos y restricciones del DTO / dominio | Validación de negocio adicional |
|---|---|---|
| LoginRequest | username no vacío, 3–100; password no vacío, 12–256 | Identidad habilitada, comprobación de credencial, política de intentos; no registrar contraseña. |
| UnitInput | code no vacío, máximo 100; name no vacío, máximo 300 | Código único normalizado; creador/revisión por servidor. |
| CareerInput | code/name con límites 100/300; academicUnitId y validFrom obligatorios; validUntil opcional | Unidad activa, código único; validUntil no anterior a validFrom. Código se recorta, normaliza espacios y convierte a mayúsculas. |
| StateChange | status obligatorio ACTIVE/DISABLED; reason no vacío de 10–4000 | Estado diferente; reactivar exige unidad activa y clasificación resuelta. |
| ProcessCommand | careerId/modelVersionId obligatorios; code/name no vacíos, máximo 100/300; periodStart/periodEnd obligatorios | Inicio no posterior al fin; período contenido en vigencia; carrera utilizable en fecha actual UTC; versión publicada. |

UnitInput, CareerInput y StateChange rechazan campos desconocidos mediante `JsonAnySetter`; no generalizar esta garantía a ProcessCommand sin comprobar configuración/prueba. Los ID, actor, fecha y revisión se asignan en servidor.

### 3.3 Formas actuales relevantes

- Career: `id`, `data` con campos de negocio, `status`, `revision`, `createdBy`, `createdAt`, `classificationPending`; revisión inicial 1. La entrada de creación es plana, no contiene un `data` externo.
- AcademicUnit: campos de catálogo propios y revisión inicial 1; confirmar schema exacto en su record antes de derivar OpenAPI.
- CatalogPage: `items`, `page`, `size`, `totalElements`; los totales deben respetar visibilidad.
- SessionView: `userId`, `expiresAt`, `csrfToken`; cookie de sesión por Set-Cookie, no contraseña/token opaco en JSON.
- Proceso: `id`, `careerId`, `code`, `name`, fechas, `modelVersionId`, `modelVersionNumber`, `state`, `revision`, `createdBy`, `createdAt`, `indicators`, `authorization`; en preparación inicial, revisión 0, indicadores vacíos y autorización ausente.
- Modelo por OP-14: DTO `LegacyModelVersion` solo si `EvaluationModelService.requireLegacy` admite su estructura; en otro caso responde 409 MODEL_VERSION_REQUIRES_V2. Para configuración completa existe GET `/api/v1/model-versions/{id}/configured`. Es una ruta real complementaria, fuera de las 18 seleccionadas; no tratar DTO legado y configurado como intercambiables. Esta precisión se confirmó después en APE3, casos HTTP-16/16B, y no se atribuye a una observación docente.
- Error: Problem Details con `type`, `title`, `status`, `detail`, `instance` y extensiones `code`, `correlationId`, `details` según handler. No contiene automáticamente `success`/`errors` del sobre académico.

## 4. Sobre común solicitado: propuesta académica pendiente

La guía solicita esta estructura; se propone como **contrato objetivo para discutir**, sin atribuirla al código. Mantener el estado HTTP real y los headers semánticos. Una respuesta con error no debe usar 200 por llevar `success: false`.

| Escenario | Uso propuesto del sobre | Estado propuesto / límite |
|---|---|---|
| Alta con representación | success=true; message informativo; data=representación; errors=null; meta con correlación | 201 y Location cuando identifica el recurso creado. |
| Lectura individual | data=representación; errors=null | 200; ETag donde se implemente precondición/revisión. |
| Colección paginada | data=arreglo de items; meta={page,size,totalElements,correlationId} | 200; no duplicar simultáneamente paginación en data y meta. |
| Colección no paginada | data=arreglo; meta con correlación, sin total/página inventados | 200 para procesos mientras no cambie la operación. |
| Validación | success=false; data=null; errors con código/campo/descripción; meta con correlación | 400 para DTO inválido; 422 solo para reglas que lo justifiquen. |
| Acceso denegado | success=false; data=null; mensaje seguro; errors con código | 401/403 según autenticación/capacidad; respetar ocultación 404. |
| Recurso ausente/no visible | success=false; data=null; mensaje indistinguible para ocultación | 404. |
| Conflicto o precondición | data=null; errors identifican código estable y acción recuperable | 409 para duplicado/estado; 412 revisión vieja; 428 If-Match ausente. |
| Sin contenido | Ningún sobre ni JSON, ni objeto vacío | 204, por ejemplo logout; tampoco se añade cuerpo a HEAD/304. |

Los campos `message`, `errors[].field` y `meta.correlationId` son propuesta de forma; no códigos/contratos aprobados adicionales. `Content-Type: application/json` para este sobre; no etiquetarlo `application/problem+json` porque no es un Problem Details de primer nivel.

**Compatibilidad que debe resolver el equipo/docente:**

1. Acordar si la entrega exige migrar el producto o documentar un contrato futuro y su diferencia actual.
2. Si se adopta el sobre, escoger versión/frontera y migrar coordinadamente frontend, OpenAPI, handlers, filtros de autenticación, pruebas y clientes. No envolver únicamente controladores exitosos y dejar errores fuera por accidente.
3. Si se conserva Problem Details, registrar aprobación real de la excepción académica; no afirmar que el sobre ya está cumplido.
4. Mantener cabeceras, permisos y errores semánticos en cualquier alternativa. `/api/v2` ya aparece como diseño objetivo de 005: no reutilizarlo para este cambio sin revisar sus otros compromisos.

## 5. Ejemplos explicados

Valores e identificadores sintéticos; no son respuestas capturadas ni solicitudes ejecutadas. Las representaciones parciales se etiquetan como extractos, sin presentarlas como respuesta completa.

### 5.1 Alta de carrera: formato actual

```http
POST /api/v1/careers HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Accept: application/json
Cookie: SEC_SESSION=<sesion-sintetica>
X-CSRF-TOKEN: <csrf-de-la-sesion>

{
  "code": "DEMO-C01",
  "name": "Carrera de demostracion",
  "academicUnitId": "11111111-1111-4111-8111-111111111111",
  "validFrom": "2026-01-01",
  "validUntil": null
}
```

Si la sesión tiene CAREER_ADMIN, la unidad es activa y el código es nuevo, el controlador construye esta clase de respuesta. Las fechas/identidades siguientes son ilustrativas:

```http
HTTP/1.1 201 Created
Content-Type: application/json
Location: /api/v1/careers/22222222-2222-4222-8222-222222222222
ETag: "1"

{
  "id": "22222222-2222-4222-8222-222222222222",
  "data": {
    "code": "DEMO-C01",
    "name": "Carrera de demostracion",
    "academicUnitId": "11111111-1111-4111-8111-111111111111",
    "validFrom": "2026-01-01",
    "validUntil": null
  },
  "status": "ACTIVE",
  "revision": 1,
  "createdBy": "33333333-3333-4333-8333-333333333333",
  "createdAt": "2026-10-31T12:00:00Z",
  "classificationPending": false
}
```

POST solicita crear un elemento de la colección; 201 comunica creación. Location identifica su URI y ETag permite una edición condicional posterior. El `data` mostrado es un campo real de Career; **no** es el sobre de éxito pedido por la guía. Content-Type describe el JSON enviado y Accept expresa la representación solicitada.

### 5.2 Revisión obsoleta y recuperación

Tras consultar GET `/api/v1/careers/{id}` y recibir ETag `"1"`, otro cliente puede guardar revisión 2. El primer cliente intenta PATCH con `If-Match: "1"`, cookie/CSRF y **todos los campos de CareerInput**, no solamente name. El servicio rechaza con 412 y código `STALE_REVISION`; debe recargar y conciliar cambios antes de reenviar. Un error actual didáctico tiene forma:

```json
{
  "type": "urn:sec-unl:problem:stale-revision",
  "title": "Precondition Failed",
  "status": 412,
  "detail": "El recurso cambió; recargue antes de continuar.",
  "instance": "/api/v1/careers/22222222-2222-4222-8222-222222222222",
  "code": "STALE_REVISION",
  "correlationId": "44444444-4444-4444-8444-444444444444",
  "details": {}
}
```

La forma se deriva del handler de catálogo; verificar serialización/cabeceras con prueba integrada antes de presentarla como captura. `ETag` no sustituye autorización ni es token CSRF.

### 5.3 Sobre académico: colección propuesta

Para una consulta paginada vacía de carreras, el **objetivo propuesto**, no respuesta actual, sería 200 con:

```json
{
  "success": true,
  "message": "Consulta realizada correctamente.",
  "data": [],
  "errors": null,
  "meta": {
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "correlationId": "44444444-4444-4444-8444-444444444444"
  }
}
```

Ejemplo propuesto de código duplicado: HTTP 409, Content-Type application/json:

```json
{
  "success": false,
  "message": "El código ya está registrado.",
  "data": null,
  "errors": [{"code": "DUPLICATE_CODE", "field": "code", "message": "El código ya está registrado."}],
  "meta": {"correlationId": "44444444-4444-4444-8444-444444444444"}
}
```

### 5.4 Cierre de sesión

DELETE `/api/v1/sessions` con cookie y CSRF válidos devuelve **204 sin cuerpo**, con Set-Cookie para retirar la cookie. No se devuelve el sobre ni `{}`. Reutilizar una sesión revocada en una ruta protegida debe rechazarse; AC-AA-03 exige comprobarlo, no se afirma haberlo ejecutado.

## 6. Métodos, estados y cabeceras: justificación limitada al caso

Según [RFC 9110, §9](https://www.rfc-editor.org/rfc/rfc9110.html#section-9), GET consulta, POST procesa contenido según recurso, PUT reemplaza y DELETE elimina la asociación del recurso. Seguridad semántica e idempotencia no equivalen a idéntico código en cada repetición. Aquí DELETE actúa sobre sesión vigente, no borra carreras. PUT no se selecciona porque no existe reemplazo PUT de estos recursos en los controladores revisados.

[RFC 5789, §2](https://www.rfc-editor.org/rfc/rfc5789.html#section-2) define PATCH para modificaciones parciales. La implementación de catálogo conserva ID/estado/historial pero exige el bloque completo de datos editables: documentar esta restricción y discutir si conviene un documento de cambios específico. No denominarlo JSON Patch ni JSON Merge Patch: no implementa esos formatos por llamarse PATCH.

200 representa resultado con contenido; 201 creación; 204 éxito sin contenido; 400 entrada inválida; 401 autenticación necesaria; 403 denegación; 404 ausencia/ocultación; 409 conflicto actual; 412 precondición falsa; 422 contenido no procesable por regla; 500 fallo no esperado. 428 se fundamenta en [RFC 6585 §3](https://www.rfc-editor.org/rfc/rfc6585.html#section-3), no en RFC 9110. Solo asociar cada estado a escenarios de la tabla, sin prometer códigos no implementados.

Content-Type y Accept describen/negocian representación; Location identifica recurso creado; ETag e If-Match coordinan revisión; Cookie/Set-Cookie mantienen sesión; X-CSRF-TOKEN protege mutaciones según filtro. 401 requiere revisar la cabecera de desafío WWW-Authenticate conforme a [RFC 9110 §15.5.2](https://www.rfc-editor.org/rfc/rfc9110.html#section-15.5.2): AA2 ya registró HTTP-AA-01 y el código revisado no la fija explícitamente. No inventar un esquema Bearer como corrección.

## 7. CORS y límites distribuidos

En desarrollo `frontend/proxy.conf.json` reenvía `/api` a `http://localhost:8080`: el navegador puede usar el mismo origen visible del frontend. Si se despliegan frontend y API en orígenes distintos, acordar política CORS, credenciales y exposición de ETag/Location para que el cliente pueda leerlas. El uso de cookies y cabeceras de aplicación exige revisar preflight y configuración de despliegue; no hay evidencia aquí de una política CORS productiva validada. CORS no concede permisos funcionales ni sustituye CSRF. Fundamento: [Fetch, protocolo CORS](https://fetch.spec.whatwg.org/#http-cors-protocol).

El navegador/API y backend/DB son fronteras de comunicación separadas. No reintentar POST automáticamente tras timeout sin resolver resultado/idempotencia: no se ha definido clave de idempotencia en este contrato. Las transacciones entre catálogo, proceso y auditoría son locales al backend/DB; no se atribuye commit distribuido. Para APE3 habrá que probar pérdida de respuesta, errores, correlación y ausencia de efectos parciales.
