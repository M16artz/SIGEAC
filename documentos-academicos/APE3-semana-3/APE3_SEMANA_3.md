# APE 3 — Arquitectura, OpenAPI y endpoints base

## Datos de Identificación del Estudiante y la Práctica

| Campo | Información |
|---|---|
| Estudiante(s) / equipo | Pendiente de identificar integrantes reales y su contribución. |
| Asignatura | Desarrollo Basado en Plataformas. |
| Ciclo / unidad | 5 A / 1. |
| Resultado de aprendizaje | Discute cómo los estándares Web influyen en el desarrollo de software, bajo los principios de solidaridad, transparencia, responsabilidad y honestidad. |
| Práctica | 003. |
| Título | Arquitectura, OpenAPI y endpoints base. |
| Docente | Edison Leonardo Coronel Romero. |
| Fecha / horario | Semana 3; fecha real pendiente / por definir según horario académico. |
| Lugar | Aula / laboratorio asignado. |
| Tiempo planificado en el Sílabo | 3 horas; no equivale a duración medida del trabajo preparado. |
| Proyecto | SIGEAC / SEC-UNL. |
| Estado | Artefactos académicos elaborados y verificación parcial ejecutada. La guía exige DRF; el sistema comprobado usa Spring Boot. No se declara la práctica completamente cumplida. |

Revisión posterior contra la guía completa: [REVISION_CUMPLIMIENTO.md](REVISION_CUMPLIMIENTO.md). Distingue evidencia previa, comprobaciones documentales repetidas y requisitos de la práctica que aún no están satisfechos.

## Objetivo(s) de la Práctica

- Diseñar y documentar la arquitectura inicial del backend a partir de los requisitos y procesos priorizados.
- Representar la solución mediante C4 (C1, C2 y C3) y UML/PlantUML para los elementos de código relevantes.
- Refinar el contrato de API y documentarlo mediante OpenAPI / Swagger.
- Implementar endpoints base coherentes con los requisitos, criterios de aceptación y semántica HTTP.
- Mantener trazabilidad entre especificación, arquitectura, contrato e implementación.

Los endpoints demostrados ya existían: se contrastaron y ejecutaron, sin atribuirles implementación nueva en APE3. El paso de implementar con Django REST Framework y el contrato universal de respuestas están pendientes de decisión e implementación, o de excepción docente explícita.

## Materiales, Reactivos, Equipos y Herramientas

### Insumos reales

Se revisaron `documentos-academicos/APE2-semana-2/specification.md`, el dossier y matriz de AA2, y los procesos, decisiones y contrato de ACD3. El [registro de ACD3](../ACD3-semana-3/REGISTRO_OBSERVACIONES.md) contiene hallazgos de preparación, no retroalimentación de una sesión efectivamente registrada. No se inventaron acuerdos docentes para resolverlos.

Se inspeccionaron los controladores/servicios de catálogo, procesos, identidad y modelos; la constitución; ADR existentes; configuración y pruebas. [FUENTES.md](FUENTES.md) enumera las rutas y referencias primarias.

| Herramienta solicitada | Estado observado / uso en esta preparación |
|---|---|
| Git/GitHub y GitFlow | Repositorio Git, rama main; no se creó rama, commit, PR ni publicación. Se propone flujo en NOTA_INDIVIDUAL.md. |
| GitHub Spec Kit | Estructura `.specify/`, constitución y plantillas existentes; no se ejecutó una cadena de comandos Spec Kit ni se simula su resultado. |
| Python/Django/DRF | Python 3.14 disponible; Django/DRF no encontrados en el intérprete inspeccionado. Python se usó como cliente HTTP, no como backend. |
| Backend real | Java 25 instalado; pom configura Java 21 y Spring Boot 4.1.1; backend arrancado desde clases actuales. |
| PostgreSQL | Persistencia principal del producto; no se ejecutó una aceptación PostgreSQL en esta actividad. |
| SQLite | Perfil auxiliar existente `sqlite-test`, base aislada en backend/target para esta verificación. No sustituye PostgreSQL. |
| Structurizr DSL | workspace.dsl elaborado con vistas C1/C2/C3; parser/render oficial no disponible en PATH y no ejecutado. |
| PlantUML | Fuentes de clases y secuencia elaboradas; renderizador no disponible y no ejecutado. |
| OpenAPI/Swagger | Dos contratos académicos, Redocly instalado y validación de muestras con Ajv; disponibilidad HTTP de Swagger comprobada. |
| Cliente HTTP | Python urllib con cookie jar y peticiones reales; Postman CLI disponible, pero no utilizado en esta campaña. |
| Quarto | Fuente técnica QMD y salida HTML del paquete; ver estado de render en evidencias. |
| Editor / LaTeX | Archivos versionables editados; FORMATOAPE.tex adaptado. Sin motor TeX local, sin PDF compilado. |

## Procedimiento / Metodología Ejecutada

### Inicio: alcance, requisitos y decisiones previas

Se conserva el recorte propuesto en Semana 2: acceso, catálogo académico y preparación/consulta de procesos. F-ACD3-01 mantiene carrera y su historial; F-ACD3-02 prepara un proceso con carrera válida y versión publicada. Se excluyen autorización institucional, activación, evidencias, revisión, evaluación, informes y archivo, aunque existan en el producto completo.

Actores: usuario habilitado A-AA-01; administrador funcional A-AA-02; gestor autorizado A-AA-03; lector autorizado A-AA-04. Las capacidades del código no acreditan por sí solas competencias institucionales. Las necesidades N-AA-01–05 y criterios completos siguen en el dossier AA2.

### Paso 1: consolidar el análisis textual

**F-ACD3-01, mantener catálogo.** Participantes: administrador, frontend, API, aplicación, repositorio y auditoría. Inicio: solicitar alta o corrección de carrera. Actividades: capturar datos; validar DTO; comprobar identidad/capacidad; consultar unidad activa; verificar código único; comprobar revisión en cambios; registrar carrera/revisión; conservar historial; registrar auditoría; devolver resultado. Decisiones: alta o cambio, entrada válida, unidad activa, código disponible, revisión vigente. Datos: código, nombre, unidad, vigencia, estado, ID, actor, fecha, revisión y correlación. Excepciones: 400/401/403/404/409/412/422/428 según causa. Resultado: recurso conservado o rechazo controlado; desactivar conserva historia y no borra.

**F-ACD3-02, preparar proceso.** Participantes: gestor, cliente, aplicación de procesos, catálogo de carreras/modelos, persistencia y auditoría. Inicio: solicitar preparación para una carrera. Actividades: seleccionar carrera visible; seleccionar versión exacta; capturar código y período; comprobar permiso/ámbito; verificar actividad y vigencia; resolver versión publicada; crear PREPARATION; guardar proceso y auditoría; devolver referencia; consultar proceso. Decisiones: permiso, carrera utilizable, período contenido, publicación y visibilidad. Datos: careerId, code, name, periodStart, periodEnd y modelVersionId; servidor asigna ID, revisión 0, actor y fecha. Resultado: preparación, sin activación ni autorización automática.

La verificación usó una carrera nueva para catálogo y la carrera piloto sintética ya habilitada para la preparación, pues `CAREER_ADMIN` no concede `PROCESS_MANAGE` automáticamente sobre una carrera recién creada. No se cambió la política de autorización para facilitar la demostración. El análisis extendido permanece en [ACD3](../ACD3-semana-3/ACD3_CLINICA_ARQUITECTURA_API.md), sección 2.

### Paso 2: arquitectura C4 y UML

- C1: administrador, gestor y lector se relacionan con SIGEAC; no se dibujan APIs institucionales no confirmadas.
- C2: frontend Angular, backend monolítico modular Spring Boot y PostgreSQL. SQLite pertenece exclusivamente al entorno auxiliar probado. El almacén de evidencias del producto completo queda fuera del recorte.
- C3: identidad/acceso, API de catálogo/procesos, aplicación de catálogo, aplicación de procesos, dominio, puerto de modelos, persistencia y auditoría. Las llamadas entre módulos son internas al backend, no HTTP entre microservicios.

Artefacto: [workspace.dsl](arquitectura/workspace.dsl). UML: [clases de catálogo](arquitectura/catalogo-clases.puml) y [secuencia de alta](arquitectura/alta-carrera-secuencia.puml), basados en nombres reales. Los diagramas no se presentan como renderizados o validados por sus motores oficiales. La [vista C4 previa](../ACD3-semana-3/ARQUITECTURA_C4.md) conserva una visualización Mermaid en Markdown.

### Paso 3: decisiones arquitectónicas

Se conservan ADR-0001 (monolito modular), ADR-0004 (sesiones/ámbito) y ADR-0018 (compatibilidad). Se elaboran propuestas académicas [APE3-001](adr/APE3-001-alineacion-tecnologica.md), tecnología exigida frente a existente, y [APE3-002](adr/APE3-002-contrato-universal.md), contrato común y migración. Ninguna se marca aceptada ni modifica los ADR del producto.

La pregunta sobre Spring/DRF se planteó al usuario durante esta preparación. Sin elección expresa se conserva el producto y se prepara la entrega sobre su implementación real; no se infiere aprobación docente ni se construye otro backend ocultamente.

### Pasos 4–5: contrato y OpenAPI

Se construyen dos archivos OpenAPI 3.1.0 independientes del contrato de producto:

1. [openapi-observado.yaml](contratos/openapi-observado.yaml): 14 operaciones seleccionadas; DTO directos y errores Problem Details, parámetros, cookie/CSRF, esquemas y resultados relevantes. Se deriva del contrato común y se refina con código y evidencia. No promete cobertura de todos los endpoints existentes.
2. [openapi-objetivo.yaml](contratos/openapi-objetivo.yaml): **propuesta no implementada** del sobre `success/message/data/errors/meta`, conserva códigos y respuestas sin cuerpo. Su servidor `/api-objetivo-no-implementada` es marcador explícito, no una ruta implementada ni una API v2. La paginación se mantiene dentro de data como CatalogPage y meta contiene correlación; esta variante debe confirmarse, difiere de trasladar paginación a meta como alternativa planteada en ACD3.

La selección agrega a las operaciones de catálogo/sesión/proceso la lectura configurada de modelo. Las rutas siguientes existen en controladores. Los nombres de parámetros OpenAPI pueden ser careerId/processId/versionId aunque el controlador use id; representan la misma posición.

| Recurso | Métodos y rutas seleccionadas, bajo /api/v1 | Resultado principal |
|---|---|---|
| Sesión actual | POST /sessions; DELETE /sessions | 201 con SessionView y cookie; 204 sin cuerpo. |
| Unidad | POST /academic-units | 201 con unidad, Location y ETag. |
| Carreras | GET /careers; POST /careers | 200 página visible; 201 alta, Location y ETag. |
| Carrera | GET /careers/{careerId}; PATCH /careers/{careerId} | 200 con recurso y ETag; PATCH exige CareerInput completo e If-Match. |
| Historia | GET /careers/{careerId}/history | 200 página de revisiones. |
| Decisión de estado | POST /careers/{careerId}/state-decisions | 201 con carrera actualizada; no Location explícita. |
| Versión de modelo | GET /model-versions/{versionId}; GET /model-versions/{versionId}/configured | DTO legado cuando compatible; 409 si requiere configuración; ruta configurada devuelve estructura completa. |
| Procesos | GET /processes; POST /processes | 200 arreglo visible sin paginación; 201 preparación con Location y ETag. |
| Proceso | GET /processes/{processId} | 200 con estado, referencias y ETag. |

Los esquemas precisan campos requeridos, UUID, fechas, límites de longitud y precondiciones. Reglas no expresables solo por schema: capacidad/ámbito, unidad activa, código normalizado único, vigencia y publicación. El contrato académico añade 400 de DTO de catálogo, ausente en la tabla heredada de respuestas compartidas, y conserva 409 del modelo legado. No modifica silenciosamente los contratos originales.

Semántica aplicada: GET consulta; POST crea recurso o decisión; PATCH modifica datos editables del catálogo sin reemplazar ID/estado; DELETE revoca sesión. No se inventa un PUT de carreras. 409 representa duplicado/estado incompatible; 412 revisión obsoleta; 428 falta If-Match; 422 regla de negocio concreta. No devolver 200 con success=false. El caso 204 carece de schema/cuerpo. El subconjunto If-Match numérico del proyecto no se enseña como toda la gramática de HTTP.

### Paso 6: endpoints base y diferencia respecto de la guía

Los endpoints ya estaban implementados en `SessionController`, `CareerController`, `EvaluationModelController` y `SelfAssessmentController`. Se revisó separación de DTO/HTTP, aplicación, dominio y repositorio; 13 pruebas ArchUnit comprueban límites. El servicio de procesos usa directamente `CareerAdministrationService` dentro del mismo módulo; no se inventa un puerto/microservicio inexistente.

**No se implementaron nuevos endpoints DRF ni se migró el producto.** Python/DRF es una exigencia explícita pendiente; el sobre universal permanece solo especificado. La documentación y los resultados Spring no bastan para marcar esos pasos como cumplidos. Confirmar alternativa con docente/equipo y luego implementar lo acordado.

### Paso 7: verificación real

Se ejecutaron 23 pruebas existentes seleccionadas con Maven. El intento inicial dentro del sandbox falló al resolver dependencias; se repitió con el acceso autorizado y terminó BUILD SUCCESS. Se inició el backend de clases actuales con `sqlite-test`, limitado a 127.0.0.1:18093 y una base sintética aislada en backend/target. No se usó el JAR antiguo como si representara los cambios actuales.

Se ejecutó `verificacion/verificar_http.py`: primer intento, 15 casos satisfactorios y rechazo inesperado 409 en la representación legada; se conservó la evidencia. Tras contrastar `LegacyModelVersion.from`/`requireLegacy`, se corrigió el escenario: esperar el rechazo y consultar `/configured`. El segundo recorrido completó **22 casos**, con estado y aserciones de cuerpo/cabecera. No se cambió el negocio para forzar el resultado.

Se validaron 22 respuestas capturadas con Ajv contra el contrato observado; Redocly validó ambos OpenAPI. Swagger UI y `/v3/api-docs` respondieron 200, pero no se ejecutaron operaciones desde la interfaz Swagger. Los tokens y credenciales de sesión se redactaron en la evidencia. Los comandos, límites y resultados están en [VERIFICACION.md](VERIFICACION.md).

### Paso 8 y cierre: trazabilidad, socialización y versionado

La [matriz APE3](TRAZABILIDAD.md) relaciona necesidades, requisitos, AC, componentes, operaciones y casos reales, diferenciando cobertura parcial y pendiente. Se integra una [página Quarto](tecnico/APE3.qmd), fuentes de diagramas, contratos y reportes al paquete académico.

Presentación docente, observaciones, aceptación del cambio tecnológico/sobre y contribuciones individuales siguen pendientes de registro. La carpeta contiene archivos versionables; **no significa que estén comprometidos en Git**. No se creó commit/PR ni se aplicó GitFlow sobre main. [NOTA_INDIVIDUAL.md](NOTA_INDIVIDUAL.md) distingue trabajo preparado y aporte verificable que cada persona debe identificar.

## Resultados

| Producto esperado | Resultado comprobado | Límite / pendiente |
|---|---|---|
| C4 C1/C2/C3 | Structurizr DSL y correspondencia con ACD3. | Parser/render oficial y validación de arquitectura con docente. |
| UML/PlantUML | Clases y secuencia vinculadas a código existente. | Render de diagramas. |
| ADR | Dos propuestas con contexto, alternativas y consecuencias. | Decisión y aceptación reales. |
| OpenAPI actualizado | Dos documentos académicos de 14 operaciones, lints correctos; 22 muestras conformes al observado. | Propuesta universal no ejecutable; no sustituye contratos de producto. |
| Endpoints ejecutables | Spring existente arrancado; recorrido HTTP de catálogo y preparación completado. | No implementados con DRF en esta actividad. |
| Pruebas | Maven: 23, sin fallos/errores/omitidas; HTTP: 22; muestras de respuesta: 22. | Sin aceptación PostgreSQL, concurrencia real multicliente, navegador UI, accesibilidad ni carga. |
| Trazabilidad | Matriz con referencias a AC y casos exactos. | No certificar requisitos completos por pruebas parciales. |
| Quarto / informe | Fuente técnica y reporte institucional preparados. | LaTeX sin compilar; ver evidencia de render HTML. |
| Versionado / individuos | Archivos, hashes y nota de contribución. | No commit, PR, GitFlow aplicado ni participación docente acreditada. |

El flujo HTTP cruza cliente, API, servicios y persistencia SQLite; constituye integración por API, no demostración completa desde Angular ni validación de migraciones/restricciones PostgreSQL. No se puede acreditar aún el cierre integral pedido por la guía.

## Preguntas de Control

### 1. ¿Qué diferencia existe entre los niveles C1, C2 y C3 del modelo C4?

C1 ubica personas, sistema y relaciones externas; C2 muestra aplicaciones/almacenes, responsabilidades y comunicaciones; C3 descompone un contenedor. En SIGEAC: personas↔sistema; Angular↔Spring↔PostgreSQL; dentro de Spring, API, servicios, dominio, repositorios, identidad y auditoría. Contenedor C4 no equivale necesariamente a Docker.

### 2. ¿Por qué un diagrama de clases no debe utilizarse como sustituto del modelo de arquitectura?

Muestra tipos, atributos y dependencias del código, pero no basta para identificar usuarios, fronteras, despliegues y comunicación. Career y su repositorio explican el catálogo; no explican por sí solos la relación del navegador con API/DB. Por eso UML complementa C4.

### 3. ¿Qué relación debe existir entre un requisito, un recurso y un endpoint?

El requisito expresa una necesidad comprobable, el recurso representa el concepto pertinente y el endpoint materializa una operación sobre él. AA-RF-06 exige registrar carrera; recurso Career; POST /api/v1/careers; AC-AA-08–12 precisan éxito y rechazos. No derivar necesidades institucionales solo porque exista una ruta.

### 4. ¿Cómo se selecciona el método HTTP apropiado para una operación?

Por su semántica y efecto deseado, no por el nombre de un método de programación. El catálogo se consulta con GET y se crea con POST; PATCH modifica su bloque editable con precondición; DELETE revoca sesión. PUT implicaría reemplazo y no se inventa aquí. El cliente debe considerar seguridad semántica e idempotencia al reintentar, sin asumir que POST es idempotente.

### 5. ¿Qué información mínima debe contener una operación documentada en OpenAPI?

Ruta y método, propósito/operationId, parámetros con ubicación/obligatoriedad/schema, requestBody cuando corresponda, respuestas por estado con media type/schema y cabeceras pertinentes, seguridad y referencias válidas. En esta práctica también se explicitan condiciones de negocio y AC; OpenAPI no sustituye toda la especificación.

### 6. ¿Por qué una decisión arquitectónica relevante debería documentarse mediante ADR?

Para conservar problema/contexto, restricciones, alternativas, decisión, estado y consecuencias. Evita presentar una preferencia como acuerdo ya aprobado. APE3-001 registra la diferencia Spring/DRF; APE3-002, el impacto de adoptar el sobre universal en clientes v1. Ambos siguen propuestos.

### 7. ¿Cómo se comprueba que un endpoint conserva trazabilidad con la especificación?

Seguir necesidad→RF/HU→AC→proceso→componente→OpenAPI→código→prueba/evidencia exacta; comprobar resultado y límites. Para AA-RF-08: AC-AA-14/15, F-ACD3-01, CareerAdministrationService, PATCH career, HTTP-09–12. Detectar revisión vieja no demuestra por sí solo una carrera real de dos transacciones ni toda la auditoría: esos límites quedan en la matriz.

## Conclusiones

La arquitectura y el contrato seleccionados tienen correspondencia con requisitos de Semana 2 y código real. Los 23 tests y 22 intercambios proporcionan evidencia técnica nueva y limitada del backend Spring; el rechazo del DTO legado permitió refinar el contrato con una diferencia observada.

La práctica no está íntegramente acreditada: falta resolver la tecnología DRF, implementar el sobre aceptado, comprobar PostgreSQL, renderizar/validar diagramas y presentar el flujo al docente con evidencia individual/versionada real. Documentar estas diferencias preserva la honestidad de la entrega.

## Recomendaciones

1. Resolver APE3-001/002 antes de ampliar código o cambiar DTO públicos.
2. Confirmar alcance, actores y retroalimentación; no presentar fixtures como normativa validada.
3. Repetir campaña y migraciones sobre PostgreSQL, probar permisos de lector/ámbito, reglas de vigencia y fallos de auditoría; añadir navegador si se exige extremo a extremo web.
4. Validar y renderizar DSL/PlantUML; revisar LaTeX antes de entregar.
5. Aplicar ramas/revisión/commits acordados y completar aporte por integrante con enlaces reales.

## Bibliografía / Referencias

Ver [FUENTES.md](FUENTES.md): requisitos/código/ADR del repositorio y documentación oficial C4, Structurizr, PlantUML, OpenAPI 3.1.0, RFC 9110/5789/6585/9457, Django REST Framework y Quarto. Las fuentes técnicas justifican semántica y formatos; no sustituyen decisiones institucionales o docentes.

## Anexos

- `arquitectura/workspace.dsl`: tres niveles C4.
- `arquitectura/catalogo-clases.puml` y `alta-carrera-secuencia.puml`: UML.
- `contratos/openapi-observado.yaml` y `openapi-objetivo.yaml`.
- `evidencias/http-intento-inicial.json`, `http-resultados.json`, `esquemas-resultados.json`, `pruebas-maven.json` y `swagger-disponibilidad.json`.
- `VERIFICACION.md`, `TRAZABILIDAD.md`, `NOTA_INDIVIDUAL.md`, `OBSERVACIONES.md`, `tecnico/APE3.qmd` y manifiesto de integridad.

## Declaración de uso de Inteligencia Artificial

Se utilizó ChatGPT/Codex para inspeccionar fuentes, elaborar arquitectura, contratos, scripts y documentos, y ejecutar verificaciones. Los resultados declarados provienen de las herramientas; no se inventaron sesiones de clase, aprobación docente, commits ni contribuciones humanas. Cada integrante debe revisar el paquete e identificar su aporte real antes de presentarlo. Esta asistencia no demuestra participación individual por sí sola.
