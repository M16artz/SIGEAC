# ACD 3 — Clínica de arquitectura y contrato de API

Semana 3 · Actividad grupal con evidencia individual · Proyecto SIGEAC / SEC-UNL.

**Estado:** preparación documental para la clínica, contrastada con el repositorio. No acredita exposición, asistencia, aprobación docente ni observaciones recibidas. Integrantes, asignatura, docente y fecha de sesión: **pendientes de confirmar**. No se modifica código, OpenAPI de producto ni requisitos de Semana 2.

## 1. Revisión de requisitos y flujo principal

El problema es organizar la autoevaluación de carreras con referencias identificables, acceso por ámbito y trazabilidad, evitando cambios sin historia y preparación con carreras o modelos inadecuados. Fuente: `docs/requirements/spec.md`, `docs/requirements/process-flows.md` y el [dossier AA2](../AA2-semana-2/DOSSIER_REQUISITOS_AA2.md).

Se conserva como **propuesta de incremento de Unidad 1**, pendiente de ratificación, el acceso, catálogo académico y preparación/consulta de procesos. La clínica selecciona dos flujos: F-ACD3-01, registrar o corregir una carrera; F-ACD3-02, preparar y consultar un proceso. Inicio/cierre de sesión son soporte transversal. Quedan fuera del incremento la autorización institucional, activación, carga de evidencias, valoración, informes, mejora y archivo; que existan implementados en el producto no los incorpora automáticamente al ejercicio.

| Flujo / necesidad | Actor documentado en AA2 | Requisito e historia | Criterios AA2 | Operaciones de negocio |
|---|---|---|---|---|
| F-ACD3-01 / N-AA-02 y N-AA-05 | A-AA-02, administrador funcional con capacidad de catálogo; lector A-AA-04 | AA-RF-05–09; HU-AA-02–05 | AC-AA-07–17 y 27 | Registrar unidad; registrar carrera; consultar catálogo; actualizar datos con revisión; registrar decisión de estado; consultar historial. |
| F-ACD3-02 / N-AA-03 y N-AA-04 | A-AA-03, gestor autorizado; lector con permiso aplicable | AA-RF-10–12; HU-AA-06 | AC-AA-18–24 | Consultar modelo y versión; validar carrera/período; preparar proceso; consultar estado y referencias. |
| Acceso, integridad y explicación | A-AA-01–04 según operación | AA-RF-01–04 y 13–14; AA-RNF-01–03, 05–07 y 10 | AC-AA-01–06 y 25–26; VC-AA-01–03, 05–07 y 10 | Autenticar; autorizar; registrar auditoría; detectar revisión obsoleta; comunicar error seguro. |

Los criterios completos permanecen en AA2; esta tabla no los redefine. Los roles son funciones de la especificación, no personas entrevistadas. Administrar catálogo no concede competencia institucional para autorizar procesos. La viabilidad del recorte se apoya en que estos comportamientos ya tienen servicios/controladores; no constituye estimación de esfuerzo ni aprobación académica.

## 2. Análisis textual previo al modelado

### F-ACD3-01 — Mantener una carrera identificable

**Participantes y responsabilidades:** administrador funcional, proporcionar datos y motivo; frontend, capturar y mostrar resultados; backend, comprobar identidad, capacidad, reglas y revisión; repositorio, conservar datos/historia; auditoría, registrar cambio crítico. Frontend, repositorio y auditoría son participantes técnicos del proceso, no actores humanos adicionales.

**Inicio:** solicitar el registro o corrección de una carrera. **Precondiciones:** disponer de sesión vigente y `CAREER_ADMIN`; para alta/edición disponer de unidad activa; para edición obtener detalle y ETag actuales.

1. Capturar código, nombre, unidad y vigencia.
2. Validar campos y formato de solicitud.
3. Comprobar identidad y capacidad de administración.
4. Consultar unidad y verificar su estado activo.
5. Verificar unicidad del código normalizado.
6. Verificar revisión vigente cuando se solicite modificación.
7. Registrar carrera o nueva revisión dentro de la transacción.
8. Conservar historial y registrar auditoría del cambio.
9. Devolver representación, ETag y, al crear, Location.
10. Mostrar resultado y permitir consultar historial.

**Decisiones:** ¿es alta o modificación?, ¿la entrada es válida?, ¿la unidad está activa?, ¿el código ya existe?, ¿coincide la revisión? Las actividades de validación de esta descripción no fijan un orden de precedencia universal de errores: el DTO, filtro y servicio pueden rechazar antes de etapas posteriores.

**Alternativa de estado:** consultar revisión; capturar estado y motivo; validar transición; registrar decisión; conservar carrera e historia. Desactivar no elimina el recurso. Una unidad con carreras activas no puede desactivarse. La aplicación devuelve la carrera/unidad actualizada al registrar la decisión, no una representación separada de decisión.

**Excepciones:** 400 por campos/formato; 401 sin sesión; 403 sin capacidad o CSRF inválido; 404 al consultar identificador inexistente/no visible; 409 por código duplicado o estado sin cambio; 422 por unidad no disponible; 428 sin If-Match; 412 por revisión obsoleta. Un fallo técnico puede resultar en 500; la atomicidad requiere prueba, no se demuestra por la anotación transaccional sola.

**Datos:** UUID, código, nombre, unidad, fechas, estado, revisión, creador y fecha del servidor, motivo y correlación. **Resultado:** carrera identificable o rechazo sin declarar cambio completado; historial y auditoría esperados según AC-AA-14/25. Fuente ejecutable: `CareerController`, `CareerAdministrationService`, `Career`, `JdbcCareerCatalogRepository` y migración V008, en las rutas del registro de fuentes.

### F-ACD3-02 — Preparar y consultar un proceso

**Participantes:** gestor autorizado, seleccionar carrera/período/modelo; frontend, presentar selecciones; backend, autorizar y validar; catálogo de carreras, determinar aptitud; catálogo de modelos, resolver versión publicada; repositorio y auditoría, conservar proceso y evento.

**Inicio:** solicitar la preparación de un proceso para una carrera. **Precondiciones:** sesión humana vigente, `PROCESS_MANAGE` en el ámbito de carrera, carrera clasificada y activa en la fecha actual UTC, unidad activa, período contenido en la vigencia y versión publicada identificada explícitamente.

1. Consultar carreras visibles y seleccionar carrera.
2. Consultar modelos/versiones visibles y seleccionar versión exacta.
3. Capturar código, nombre y fechas del proceso.
4. Comprobar identidad, capacidad y ámbito de carrera.
5. Validar actividad de carrera/unidad y pertenencia del período a su vigencia.
6. Resolver versión publicada sin reemplazarla por otra implícitamente.
7. Crear proceso en `PREPARATION`, con revisión inicial 0.
8. Guardar proceso y registrar auditoría en la transacción.
9. Devolver 201, Location, ETag y representación.
10. Consultar proceso por identificador y mostrar estado/referencias permitidos.

**Decisiones:** ¿está permitido gestionar esa carrera?, ¿es utilizable?, ¿pertenece el período a su vigencia?, ¿está publicada la versión?, ¿es visible el proceso consultado? **Excepciones:** entrada ilegible/inválida o versión no publicada → 400; sin identidad → 401; gestión fuera de ámbito → 403; carrera inexistente o proceso no visible → 404; carrera inactiva → 409; período fuera de vigencia → 422. No se promete 409 por código de proceso duplicado: debe comprobarse la regla y su traducción antes de incorporarla al contrato.

**Datos:** `careerId`, `code`, `name`, `periodStart`, `periodEnd`, `modelVersionId`; servidor: ID, número de versión, estado, revisión, actor y fecha. **Resultado:** preparación persistida, sin autorización ni activación automática. `ModelApplicabilityService` se consulta en activación, fuera del recorte: crear en preparación no demuestra aplicabilidad normativa institucional. Fuente: `SelfAssessmentService.create/get/list` y `SelfAssessmentController`.

## 3. Arquitectura C4 preliminar

El [documento C4](ARQUITECTURA_C4.md) incluye C1, C2, C3, responsabilidades, tecnologías y fronteras. Los diagramas son vistas académicas derivadas del código actual, no decisiones nuevas aprobadas. Se distingue el producto completo del recorte de esta clínica.

En C1 se representan personas y el sistema; no se inventa una integración activa con CACES, SSO, correo o sistemas académicos. Los documentos de CACES/UNL son fuentes del dominio, no servicios invocados por estos recorridos. C2 separa aplicación Angular, backend Spring Boot y PostgreSQL. El almacenamiento local pertenece al producto completo y queda fuera de los flujos seleccionados. C3 descompone el backend en identidad, API, aplicación, dominio, repositorios y puertos de catálogo/auditoría.

El backend es un **monolito modular**, según ADR-0001. La comunicación de navegador a API y de backend a PostgreSQL cruza procesos; las llamadas entre módulos del backend son internas al mismo despliegue. No se atribuyen microservicios, broker o transacciones distribuidas al incremento.

## 4. Decisiones y alternativas para discusión

| ID | Decisión y evidencia existente | Alternativa, ventaja y desventaja | Justificación / estado |
|---|---|---|---|
| DA-01 | Monolito modular por capacidades; ADR-0001 | Microservicios: despliegue independiente, mayor operación y consistencia distribuida. Capas globales: arranque simple, más acoplamiento entre capacidades. | Conservar límites existentes y transacciones locales para el recorte. Decisión de producto aceptada; no nueva aprobación de clínica. |
| DA-02 | `api/application/domain/infrastructure`; paquetes reales | Reglas en controlador: menos clases, acopla HTTP y negocio; CRUD genérico: uniforme, insuficiente para estados/ámbito. | API traduce HTTP; aplicación coordina; dominio impone invariantes; infraestructura persiste. ArchUnit existe; no se ejecutó aquí. |
| DA-03 | Java **21** en `backend/pom.xml`; Spring Boot **4.1.1**; Angular **22.2.3** en `frontend/package.json` | Cambiar versión de Java: puede alinear planificación, exige verificar herramientas/CI y compatibilidad. Mantener configuración: reduce cambio, exige corregir investigación. | `docs/architecture/research.md` menciona Java 25. Documentar divergencia y decidir en equipo; no certificar compatibilidad mediante lectura del POM. |
| DA-04 | PostgreSQL como persistencia principal; Flyway, JDBC y partes JPA | SQLite auxiliar: preparación sencilla, no sustituye concurrencia/SQL de aceptación. Documentos como almacén principal: flexibilidad, menor ajuste a relaciones/revisiones existentes. | Referencias y revisiones requieren integridad; no introducir event sourcing ni cambiar migraciones aplicadas. ADR-0018. |
| DA-05 | Sesión opaca revocable y autorización por ámbito; ADR-0004 | JWT autocontenido: verificación local, revocación más compleja. SSO: centraliza identidad, proveedor/contrato no confirmados. | Conservar cookie `SEC_SESSION` y CSRF en mutaciones; no inventar Bearer. Roles técnicos no implican autoridad funcional. |
| DA-06 | Puertos `ModelCatalogPort`, `AuthorizationPort`, `AuditPort`; adaptadores existentes | Acceder a tablas de otro módulo: menor intermediación, rompe límites. Servicio externo obligatorio: independencia, añade disponibilidad/operación. | La consulta de modelo es interna, sin llamada HTTP externa en el flujo. El MVP no requiere integraciones externas. |
| DA-07 | ETag/If-Match y revisiones; `EntityTagSupport` | Último escritor gana: sencillo, pierde cambios; bloqueo largo: reduce carreras de actualización, más contención. | Conservar detección de edición obsoleta. Revisiones iniciales distintas son contrato vigente, no uniformarlas silenciosamente. |
| DA-08 | DTO directo y Problem Details actuales frente al sobre pedido | Migración coordinada: satisface forma académica, rompe clientes si se aplica sin versionar. Mantener Problem Details: compatibilidad, exige acordar excepción a guía. | Presentar sobre objetivo académico y pedir decisión de compatibilidad. No afirmar que ya está implementado. Ver contrato. |

Estas alternativas son análisis preparado para discusión, no alternativas debatidas efectivamente en clase. No se crean ADR aceptados ni se cambia el estado de los existentes.

## 5. Recursos, operaciones y contrato

El [contrato inicial](CONTRATO_API_INICIAL.md) identifica 18 operaciones observadas, sus requisitos, entradas, validaciones, autorizaciones, resultados y la propuesta de respuesta común. Las rutas se toman de controladores y la base `/api/v1` de `application.yml`; un archivo OpenAPI por sí solo no demuestra implementación.

Recursos principales: sesión vigente, identidad propia, unidad académica, carrera, revisión histórica, decisión de estado, modelo, versión de modelo y proceso. Se utiliza `POST /careers/{id}/state-decisions` como recurso de decisión, ya existente. No se añade `/createCareer`. No se incorpora `/activate` al incremento; su existencia en el producto se declara como exclusión, sin esconderla.

## 6. Trazabilidad inicial de arquitectura y API

| Problema / necesidad | RF / historia / criterio AA2 | Proceso | Componente principal | Recurso / operaciones del contrato | Verificación posterior |
|---|---|---|---|---|---|
| Acceso autorizado N-AA-01 | AA-RF-01–04; HU-AA-01; AC-AA-01–06 | Acceso transversal | SessionController, SessionService, ScopeAuthorizationService | OP-01–03 | Sesión válida/inválida, revocación, ámbito y CSRF. |
| Catálogo coherente N-AA-02 | AA-RF-05–07; HU-AA-02/03; AC-AA-07–13/27 | F-ACD3-01 | CareerController, CareerAdministrationService, Career | OP-04–09, 17 | Alta, normalización, duplicados, filtros y visibilidad. |
| Corregir sin perder historia N-AA-05 | AA-RF-08/09; HU-AA-04/05; AC-AA-14–17; AA-RNF-05 | F-ACD3-01 | Servicio de catálogo, repositorio y auditoría | OP-10–12 | Dos clientes/revisiones, no sobrescritura, motivo y estado. |
| Preparar con referencias exactas N-AA-03 | AA-RF-10/11; HU-AA-06; AC-AA-18–22 | F-ACD3-02 | SelfAssessmentService, ModelCatalogPort | OP-13/14/18 | Modelo publicado, carrera vigente, PREPARATION y revisión 0. |
| Consultar únicamente lo visible N-AA-04 | AA-RF-12; HU-AA-06; AC-AA-23/24 | F-ACD3-02 | SelfAssessmentService y autorización | OP-15/16 | Listado filtrado e identificador ajeno oculto. |
| Explicar y auditar N-AA-05 | AA-RF-13/14; AC-AA-25/26; AA-RNF-06/07/10 | Ambos | Auditoría, handlers y CorrelationFilter | Mutaciones y errores transversales | Atomicidad, correlación y contrato cliente; pendiente de ejecución. |

Pruebas existentes que se pueden abrir: `backend/src/test/java/ec/edu/unl/sec/architecture/ModuleArchitectureTest.java`; `selfassessment/api/CareerControllerTest.java`, `SelfAssessmentControllerTest.java`; `selfassessment/application/CareerAdministrationServiceTest.java`, `SelfAssessmentServiceTest.java`, `SelfAssessmentAuditServiceTest.java`, bajo el mismo prefijo de pruebas; `tests/acceptance/career-catalog.spec.ts`; `docs/testing/traceability.md`. Esta es vinculación temática por revisión, no certificación de cobertura individual de todos los AC nuevos ni ejecución reciente.

## 7. Guion de la clínica y observaciones

Presentar, en este orden: problema/recorte; F-ACD3-01/02 y criterios; C1→C2→C3; decisiones DA-01–08; recursos/operaciones; ejemplo 201 y conflicto de revisión; incompatibilidad del sobre; preguntas pendientes. Llevar abiertos AA2, análisis textual, C4 y contrato. Cada integrante debe explicar una decisión y su cadena requisito→proceso→componente→API.

El [registro de observaciones](REGISTRO_OBSERVACIONES.md) separa hallazgos de esta preparación de retroalimentación auténtica de la sesión. Para cada observación real se registrarán fuente/fecha, decisión, cambio o justificación, archivos y evidencia versionada.

## 8. Cumplimiento y entrega para APE3

| Actividad de la guía | Evidencia preparada | Estado / siguiente paso |
|---|---|---|
| 1. Revisar requisitos y flujo | Sección 1 y trazabilidad de sección 6 | Documentado; ratificar recorte/actores. |
| 2. Análisis textual | Sección 2 | Dos procesos, tareas en infinitivo, decisiones/excepciones/datos/resultados. |
| 3. C4 | ARQUITECTURA_C4.md y diagramas Mermaid | Tres niveles; revisar con equipo/docente. |
| 4. Decisiones | Sección 4 | Ocho decisiones contrastadas y alternativas por discutir. |
| 5–6. Recursos y operaciones | CONTRATO_API_INICIAL.md | 18 operaciones reales; no nuevos endpoints ficticios. |
| 7. Respuesta común | Contrato, secciones 4–5 | Sobre académico propuesto; incompatible con DTO/errores vigentes. |
| 8–9. Estados, entradas y validaciones | Contrato, secciones 2–3 | Revisión estática; confirmar mediante pruebas de contrato. |
| 10. Clínica y ajustes | REGISTRO_OBSERVACIONES.md | Registro preparado; sesión y retroalimentación pendientes. |

Para APE3: confirmar alcance y política de contrato; resolver preguntas; actualizar especificación/ADR solo tras acuerdo real; derivar OpenAPI del contrato aceptado; revisar schemas y seguridad; comprobar compatibilidad y respuestas sin cuerpo; implementar únicamente lo aprobado; ejecutar pruebas con datos sintéticos y registrar resultado/commit. No considerar este ACD autorización para reemplazar DTO v1, habilitar v2 o ampliar funciones de Unidad 1.

## 9. Fuentes y límites

El [registro de fuentes](FUENTES.md) identifica rutas exactas, decisiones existentes y documentación primaria consultada. La preparación original ACD3 no ejecutó endpoints, pruebas de negocio ni compilación PDF. Los ejemplos son didácticos derivados del código, no capturas de ejecución. Se utilizó ChatGPT/Codex para inspección, análisis y elaboración documental; la revisión y atribución personal siguen pendientes.

La [revisión de la guía](REVISION_GUIA.md) localiza los diez puntos y distingue evidencia posterior de APE3. Esa evidencia permitió precisar OP-14: el DTO legado rechaza con 409 un modelo que requiere representación configurada; la ruta /configured sí lo devuelve. Esta actualización documental no equivale a una clínica celebrada ni a retroalimentación docente, y no ejecuta nuevamente las pruebas de APE3.
