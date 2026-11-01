# ACD2 — Evidencia individual para personalizar

| Dato | Valor |
|---|---|
| Estudiante | [PENDIENTE: nombre completo] |
| Equipo y ciclo/paralelo | [PENDIENTE] |
| Fecha de participación | [PENDIENTE: fecha real] |
| Actividad | ACD2 — Semana 2, Desarrollo de Software Distribuido |
| Proyecto | SIGEAC / SEC-UNL |
| Estado | Desarrollo técnico propuesto; pendiente de revisión y atribución personal |

Este contenido desarrolla los ejercicios, pero no acredita por sí solo participación individual. Cada estudiante debe revisarlo, explicar su razonamiento y registrar su retroalimentación real. No presentar el mismo texto como aporte personal independiente de todos los integrantes sin identificar la colaboración y el uso de IA.

## 1. Dos requisitos revisados y justificación

### 1.1. FR-053 — Cálculo de recopilación documental

**Fuente:** `specs/005-complete-evaluation-workflows/spec.md`, complementada por `docs/requirements/domain-rules.md`, sección Cálculo documental sin ambigüedad.

**Enunciado de partida:** “El avance de recopilación DEBE calcularse como fuentes aplicables cubiertas por evidencia remitida dividido para fuentes requeridas aplicables, multiplicado por cien”.

**Redacción corregida propuesta:** El avance de recopilación deberá calcularse como el número de IDs distintos de fuentes obligatorias aplicables cubiertas por al menos una versión remitida vigente, dividido entre el número de IDs distintos de fuentes obligatorias aplicables, multiplicado por cien. Se excluirán borradores y fuentes opcionales. Si el denominador es cero, se mostrará porcentaje no calculable y el estado correspondiente de configuración o no aplicabilidad; no se mostrará 100 %.

**Justificación:** “Fuentes aplicables” no precisa por sí sola obligatoriedad, duplicación ni vigencia. La corrección conserva la intención y utiliza reglas existentes, haciendo posible diseñar casos de prueba sin depender del número de archivos.

**Criterio:** Dado que existen cuatro fuentes obligatorias aplicables y tres tienen cobertura remitida vigente, cuando consulto recopilación, entonces obtengo 75 %. Adjuntar dos archivos a una misma fuente no debe aumentar ese conteo.

**Evidencia técnica:** `backend/src/main/java/ec/edu/unl/sec/reporting/domain/DocumentaryProgressCalculator.java`; escenario AC-P11-01 de `tests/acceptance/complete-workflows.spec.ts`. No se ejecutó la prueba durante la preparación documental.

### 1.2. FR-030 — Navegación hacia los sustentos

**Fuente:** `docs/requirements/spec.md`.

**Enunciado de partida:** “El tablero DEBE permitir navegar desde conteos agregados hasta indicadores, decisiones y evidencias autorizadas que los sustentan”.

**Redacción corregida propuesta:** Desde cada conteo agregado presentado en el tablero, el usuario autorizado deberá poder acceder a los indicadores, decisiones documentales y versiones de evidencia exactas que forman el conteo. Cada consulta de destino mantendrá el ámbito del usuario y no expondrá registros ajenos. La interfaz deberá conservar el contexto del proceso consultado.

**Justificación:** Se especifican origen, destinos y restricción observable. Así se distingue una navegación funcional de mostrar un UUID sin enlace. La conservación de contexto se propone para revisión del equipo y no se presenta como una decisión previamente aprobada.

**Criterio:** Dado que un conteo incluye una cobertura aceptada de mi ámbito, cuando accedo a su sustento, entonces veo el indicador, decisión y versión exacta relacionados. Dado que carezco de autorización para un recurso, cuando intento abrirlo, entonces no recibo su contenido ni se amplía mi ámbito.

**Evidencia y límite:** `frontend/src/app/features/reporting/pages/documentary-dashboard.page.ts` filtra por fuente, pero muestra UUID de versiones como texto; la navegación completa sigue parcial. Formular el requisito no demuestra que esté implementado.

**Aporte personal a confirmar:** [PENDIENTE: explicar con palabras propias qué parte se revisó, qué decisión se discutió y cuál fue el cambio aceptado por el grupo].

## 2. Ejercicio de análisis HTTP resuelto

Ejemplo reconstruido del contrato de `backend/src/main/java/ec/edu/unl/sec/selfassessment/api/CareerController.java` con datos sintéticos. Se presupone sesión administrativa vigente y unidad activa. No es una captura de ejecución real.

```http
POST /api/v1/careers HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Accept: application/json
Cookie: SEC_SESSION=<sesion-sintetica>
X-CSRF-TOKEN: <csrf-sintetico>

{
  "code": "CARRERA-DEMO",
  "name": "Carrera de demostración",
  "academicUnitId": "11111111-1111-4111-8111-111111111111",
  "validFrom": "2026-01-01",
  "validUntil": null
}
```

```http
HTTP/1.1 201 Created
Content-Type: application/json
Location: /api/v1/careers/22222222-2222-4222-8222-222222222222
ETag: "1"

{
  "id": "22222222-2222-4222-8222-222222222222",
  "data": {
    "code": "CARRERA-DEMO",
    "name": "Carrera de demostración",
    "academicUnitId": "11111111-1111-4111-8111-111111111111",
    "validFrom": "2026-01-01",
    "validUntil": null
  },
  "status": "ACTIVE",
  "revision": 1,
  "createdBy": "33333333-3333-4333-8333-333333333333",
  "createdAt": "2026-01-01T12:00:00Z",
  "classificationPending": false
}
```

| Elemento solicitado | Interpretación |
|---|---|
| Método | POST: se solicita crear una carrera. |
| Recurso | Colección `/api/v1/careers`; el servidor asigna una URI a la carrera nueva. |
| Host | `localhost:8080` identifica el servicio usado en el ejemplo. |
| Content-Type | El cuerpo de la petición es JSON; la respuesta también declara JSON. |
| Accept | El cliente solicita una representación JSON de respuesta. |
| Cookie | Lleva la sesión opaca; el servidor comprueba su vigencia y permisos. |
| X-CSRF-TOKEN | Protege la mutación autenticada contra solicitudes cruzadas no autorizadas. |
| Cuerpo de petición | Código, nombre, ID de unidad y vigencia. No envía autor ni revisión. |
| Estado | 201 Created confirma creación; no es simplemente una consulta 200. |
| Location | Identifica la carrera recién creada y permite construir una consulta posterior. |
| ETag | `"1"` es la revisión inicial; se usa con If-Match en cambios posteriores. |
| Cuerpo de respuesta | Representación Career, incluyendo atributos de negocio en `data` y metadatos del servidor. |
| Significado completo | Se creó una carrera activa, con unidad y vigencia válidas, bajo una identidad autorizada. |

**Comparación de rechazos:** sin sesión corresponde 401; sin capacidad o con CSRF inválido, 403; código duplicado, 409; unidad no disponible, 422 según el servicio. No todos los JSON erróneos producen 422: errores sintácticos o de validación de campos pueden producir 400.

**Pregunta de concurrencia resuelta:** si leo ETag `"1"` y otro cliente modifica antes, mi PATCH con If-Match `"1"` recibe 412; debo recargar. ETag no autentica mi identidad y no autoriza mi acción.

**Pregunta CORS resuelta:** el cliente Angular usa rutas relativas y un proxy de desarrollo. Si llama directamente desde un origen distinto al backend, el navegador requiere una política CORS compatible; Postman no verifica ese comportamiento del navegador. No se presume CORS habilitado.

**Authorization de la guía:** su ejemplo usa Bearer; este sistema utiliza SEC_SESSION y CSRF. Se interpreta Authorization como cabecera general sin afirmar soporte JWT en el proyecto.

Fuentes conceptuales: [RFC 9110](https://www.rfc-editor.org/rfc/rfc9110.html), [MDN: ETag](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/ETag) y [Fetch: CORS](https://fetch.spec.whatwg.org/#http-cors-protocol). Las rutas, DTO y códigos aplicados proceden del repositorio.

## 3. Comentario de retroalimentación real

No se proporcionó una observación recibida en clase. Completar el siguiente registro después de la socialización; no atribuir la revisión de IA al docente o a un compañero.

| Campo | Registro |
|---|---|
| Persona que realizó la observación | [PENDIENTE] |
| Fecha y contexto | [PENDIENTE] |
| Observación recibida | [PENDIENTE: texto fiel o resumen identificado] |
| Requisito o ejercicio afectado | [PENDIENTE] |
| Cambio utilizado para mejorar el trabajo | [PENDIENTE] |
| Justificación de cómo resolvió el problema | [PENDIENTE] |
| Evidencia de participación | [PENDIENTE: acta, revisión, entrega o registro real] |

Estructura de redacción para completar: “Durante la sesión, [persona] observó que [observación real]. A partir de ello revisé [ID o ejercicio] y cambié [antes] por [después], porque [justificación verificable]”.

## 4. Comprobación individual

- Dos requisitos revisados con justificación: desarrollados en 1.1–1.2; revisión y adopción personal pendientes.
- Solicitud/respuesta interpretadas con método, recurso, cabeceras, cuerpo y estado: desarrolladas en 2.
- Retroalimentación real y uso explicado: pendiente en 3.
- Nombre, fecha y contribución individual: pendientes.
- Las solicitudes son sintéticas y las pruebas no se presentan como ejecutadas.

