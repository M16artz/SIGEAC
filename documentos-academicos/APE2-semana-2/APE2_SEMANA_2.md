# Reporte Técnico de Actividades Práctico-Experimentales Nro. 002

Elaboración y refinamiento de specification.md con historias de usuario, requisitos, criterios de aceptación y contratos preliminares.

## 1. Datos de Identificación del Estudiante y la Práctica

| Dato | Información |
|---|---|
| Nombre del estudiante(s) | [PENDIENTE: integrantes reales del equipo] |
| Asignatura | Desarrollo Basado en Plataformas |
| Ciclo | 5 A |
| Unidad | 1 |
| Resultado de aprendizaje | Discute cómo los estándares Web influyen en el desarrollo de software, bajo los principios de solidaridad, transparencia, responsabilidad y honestidad. |
| Práctica Nro. | 002 |
| Título de la Práctica | Elaboración y refinamiento de specification.md con historias de usuario, requisitos, criterios de aceptación y contratos preliminares. |
| Nombre del Docente | Edison Leonardo Coronel Romero |
| Fecha | Semana 2; [PENDIENTE: fecha real de la sesión] |
| Horario | Por definir según horario académico |
| Lugar | Aula / laboratorio asignado |
| Tiempo planificado en el Sílabo | 3 horas |
| Proyecto | SIGEAC / SEC-UNL |
| Estado del reporte | Desarrollo documental preparado; validación docente, participación y publicación pendientes de evidencia |

Se utilizan los datos de la guía actual. El documento ACD2 previo se conserva como insumo, aunque su cabecera corresponde a otra denominación de asignatura. No se corrigen documentos anteriores suponiendo que ambas guías pertenecen a la misma materia.

## 2. Objetivo(s) de la Práctica

- Construir y refinar la especificación funcional del proyecto mediante Spec-Driven Development (SDD).
- Transformar las necesidades de los actores en requisitos funcionales y no funcionales claros y verificables.
- Definir historias de usuario y criterios de aceptación que permitan comprobar el comportamiento esperado.
- Establecer contratos preliminares que sirvan de base para el diseño posterior de la API.
- Validar la especificación con el docente e incorporar la retroalimentación de manera trazable.

Los cuatro primeros objetivos tienen desarrollo documental en los archivos de esta carpeta. El quinto requiere revisión real de Edison Leonardo Coronel Romero; no se acredita mediante una redacción supuesta.

## 3. Materiales, Reactivos, Equipos y Herramientas

### 3.1. Insumos solicitados y disponibles

| Insumo de la guía | Evidencia localizada | Tratamiento |
|---|---|---|
| Banco de proyectos y análisis ACD1 | No se aportó/localizó una entrega académica nominal ACD1 en esta preparación. | Recuperar contexto de `docs/requirements/spec.md`; no inventar el banco ni una actividad realizada. |
| Actores y fundamentos SDD de AA1 | No se aportó/localizó AA1 como entrega independiente. | Utilizar actores de especificaciones y constitución; incorporar AA1 real cuando exista. |
| Requisitos y observaciones de ACD2 | `documentos-academicos/ACD2-semana-2/ACD2_SEMANA_2.md`. | Usar refinamientos técnicos; retroalimentación de aula sigue pendiente. |
| Repositorio configurado APE1 | Git local y origin `git@github.com:M16artz/SIGEAC.git`. | Se verifica configuración actual, no la ejecución de APE1 por integrantes. |
| Documentación del proyecto | `docs/requirements/`, `specs/`, `docs/testing/`. | Fuentes de requisitos, estados y trazabilidad. |
| Diagnóstico previo | `AUDITORIA_DOCUMENTACION_SDD.md`. | Identificar inconsistencias sin tratar el diagnóstico como aprobación. |

### 3.2. Herramientas y función real

Se utilizaron archivos del repositorio, Git de solo lectura para identificar rama/commit/remote, Markdown, inspección de fuentes Java y consulta de documentación técnica. La estructura de historias, requisitos, entidades y criterios se tomó de `.specify/templates/spec-template.md`. No se ejecutó el CLI de Spec Kit ni se crearon ramas, tareas de implementación o migraciones por una actividad documental.

Quarto/Pandoc se utiliza para convertir el contenido a LaTeX, respetando la plantilla APE original. Se conservará su recurso de logo disponible. No hay motor TeX local disponible en PATH; la compilación y revisión visual del PDF quedan pendientes de Overleaf.

No se realizaron instalaciones, llamadas a APIs del proyecto, pruebas de carga ni despliegues. Tampoco se atribuye uso efectivo de VS Code o herramientas colaborativas a estudiantes sin evidencia.

## 4. Procedimiento / Metodología Ejecutada

### 4.1. Revisar los insumos existentes

Se revisaron contexto, actores, alcance, exclusiones y decisiones CL de `docs/requirements/spec.md`, reglas de dominio, catálogo P01–P16, especificación 005 y ACD2. El código permite contrastar catálogo, sesión y preparación de proceso. Los resultados de pruebas anteriores no se presentan como ejecución nueva.

Hallazgos que orientaron el refinamiento:

- El problema funcional está descrito, pero falta evidencia empírica del proceso institucional actual.
- La denominación SIGEAC/SEC-UNL debe explicitarse para identificar el mismo repositorio.
- Las competencias institucionales y vigencia normativa siguen pendientes.
- Los documentos históricos pueden llamar borrador al estado que el código actual crea como PREPARATION.
- Crear proceso y activarlo son operaciones distintas.
- RNF-003 dice fallos consecutivos, mientras el repositorio cuenta fallos en ventana; no se escoge una política sin decisión.
- El OpenAPI común contiene diferencias con controladores; contratos preliminares se contrastaron con código.

### 4.2. Refinar problema y alcance

Se propuso un incremento U1 de acceso autorizado, catálogo de carreras y preparación de proceso con modelo publicado. El objetivo es identificar carrera, período, versión y actor antes de decisiones institucionales posteriores.

Se excluyeron del recorte autorización/activación, planificación avanzada, evidencias, revisión, valoración, informes, mejora y archivo. La exclusión corresponde a la selección de esta práctica; varios módulos existen en el proyecto. La propuesta es viable como trabajo de especificación en 3 horas, no como promesa de implementar todos los comportamientos en ese tiempo.

La validación del recorte se registra como CL-APE-01 pendiente. No se introducen capacidades nuevas ajenas al código o a la documentación.

### 4.3. Formular requisitos funcionales

Se redactaron diez requisitos locales APE-FR-01–10 en [specification.md](specification.md), vinculados a FR canónicos. Se separan alta de carrera, consulta, modificación histórica, selección de modelo y preparación de proceso. Se describen actor, condición y resultado observable sin imponer una clase, tabla o framework como requisito de negocio.

Ejemplo de refinamiento: “gestionar carreras” se desglosa en registrar datos válidos y código único; consultar catálogo autorizado; modificar/desactivar conservando identidad e historial. Cada operación tiene escenarios de éxito y rechazo propios.

### 4.4. Formular requisitos no funcionales

Se seleccionaron diez verificaciones APE-NFR-01–10 relativas a seguridad, rendimiento, disponibilidad, concurrencia, auditoría, accesibilidad, mantenibilidad, interoperabilidad, usabilidad y datos sintéticos. Se mantienen los umbrales existentes de sesión y p95; el entorno experimental y la aplicación de usabilidad al recorte requieren confirmación.

Se distingue una condición verificable de una certificación: disponer de ETag no prueba toda la concurrencia; disponer de etiquetas no acredita WCAG AA; usar JSON no demuestra por sí solo interoperabilidad. No se inventa un porcentaje de disponibilidad ni una medición de rendimiento.

### 4.5. Elaborar historias de usuario

Se elaboraron tres recorridos priorizados:

| Historia | Necesidad y beneficio | Prueba independiente planteada |
|---|---|---|
| US1, P1: acceder y consultar funciones | N-01; terminar acceso y operar dentro de funciones vigentes. | Identidad sintética preexistente: login, identidad y logout. |
| US2, P1: catálogo identificable e histórico | N-02; disponer de carrera inequívoca sin borrar historial. | Administrador/unidad existentes: alta, consulta, cambio y desactivación. |
| US3, P2: preparar proceso | N-03; identificar evaluación sin activación institucional implícita. | Carrera/modelo publicados existentes: alta y consulta de PREPARATION. |

Independencia significa poder validar el recorrido con prerrequisitos disponibles, no ausencia total de dependencias del dominio.

### 4.6. Definir criterios de aceptación

Se formularon 18 escenarios Dado–Cuando–Entonces: cuatro de acceso, ocho de catálogo y seis de preparación. Cubren éxito, campos/fechas inválidas, duplicados, ámbito, cierre/expiración de sesión y concurrencia.

Ejemplo: Dado que la carrera/unidad están activas, el período es válido y el modelo está publicado, cuando el coordinador con capacidad de gestión crea un proceso, entonces queda PREPARATION con las referencias exactas y sin activarse. Los escenarios identificados se conectan a requisitos y posibles pruebas; no se declara que ya fueron aprobados.

### 4.7. Ejecutar clarificación

Se identificaron y registraron las ambigüedades. Se adoptaron dos decisiones editoriales verificables: nombre `specification.md` dentro de la carpeta académica, y uso de los datos de esta guía. Se dejaron seis preguntas CL-APE pendientes sobre recorte, competencias, intentos fallidos, problema empírico, protocolos y retroalimentación.

**No se completó una clarificación con el docente**, porque no se proporcionaron respuestas ni una sesión verificable. Este paso queda preparado para realizarlo honestamente, con campos de persona, fecha, respuesta y respaldo.

### 4.8. Identificar contratos preliminares

Se redactaron nueve fichas CP-01–09 en [CONTRATOS_PRELIMINARES.md](CONTRATOS_PRELIMINARES.md). Relacionan necesidad/requisito, recurso, operación, entrada, precondición, resultado y errores. Las rutas se recuperaron de controladores reales, pero no se exige formalizar OpenAPI definitivo en esta práctica.

Se conservaron diferencias importantes del código: entrada de carrera plana y respuesta con `data`; revisión inicial de carrera 1 y de proceso 0; errores de campos 400 frente a algunas reglas 422; catálogo paginado y listado de procesos sin paginación declarada. La sesión utiliza cookie y CSRF, no JWT supuesto.

### 4.9. Actualizar specification.md dentro de la estructura de Spec Kit

Se conservaron las secciones `User Scenarios & Testing`, `Requirements`, `Key Entities`, `Success Criteria` y `Assumptions` de la plantilla local. Se añadieron contexto, actores, alcance, restricciones, clarificaciones, referencias a contratos y matriz de trazabilidad.

La ubicación de entrega es `documentos-academicos/APE2-semana-2/specification.md`. Se conserva la nomenclatura `spec.md` de los paquetes canónicos para no romper sus enlaces. Esta es una selección académica enlazada con el producto; no se afirma que haya reemplazado ni aprobado la especificación global.

Se guardó una versión de trabajo inicial real de esta preparación y una versión refinada, junto a un diff y hashes. No se presenta el borrador inicial como trabajo de la semana anterior ni como un commit inexistente.

### 4.10. Validar con el docente

Se preparó el guion: problema abordado, actores, incluido/excluido, requisitos del recorte, criterios de comprobación y preguntas abiertas. La validación y cambios acordados están pendientes. El registro al final de `specification.md` deberá completarse con la observación real y los IDs afectados.

Propuesta de distribución de las 3 horas, **no registro de tiempos ejecutados**:

| Bloque | Minutos previstos |
|---|---:|
| Insumos, problema y alcance | 25 |
| Requisitos funcionales/no funcionales | 35 |
| Historias y aceptación | 35 |
| Clarificaciones y contratos | 35 |
| Revisión docente, ajustes y trazabilidad | 30 |
| Evidencia individual e historial | 20 |
| Total | 180 |

## 5. Resultados

### 5.1. Productos preparados

| Producto solicitado | Resultado local | Límite |
|---|---|---|
| specification.md actualizado | Documento refinado con contexto, diez FR locales, diez NFR, tres historias, 18 AC y criterios de éxito. | Pendiente de validar recorte/políticas; no aprobado por docente. |
| Contratos preliminares | Nueve fichas con requisitos, recursos y comportamientos. | No son OpenAPI final ni tráfico capturado. |
| Clarificación | Dos decisiones editoriales y seis preguntas explícitas. | Sin respuestas docentes inventadas. |
| Historial de cambios | Borrador v01, especificación refinada, diff y hashes. | Historial local de esta preparación; no sustituye un commit publicado. |
| Nota individual | Desarrollo orientativo y campos de aporte/enlace. | Identidad, revisión personal y commit/PR pendientes. |
| Enlace al repositorio | Origin deriva en `https://github.com/M16artz/SIGEAC`. | No se verificó acceso externo ni publicación de archivos nuevos. |

### 5.2. Correspondencia de calidad

| Criterio | Evidencia |
|---|---|
| Claridad | Roles, acciones y resultados diferenciados; creación no equivale a activación. |
| Especificidad | Código normalizado, vigencia, modelo publicado, estado inicial y revisión de cambios. |
| Pertinencia | Cada requisito se relaciona con N-01–04 y con el recorte U1 propuesto. |
| Verificabilidad | 18 AC y comprobaciones RNF con métodos y límites indicados. |
| Consistencia | IDs locales enlazados a FR/RNF canónicos; exclusiones expresas y políticas pendientes. |
| Transparencia | Fuentes identificadas; sin resultados, asistencia o retroalimentación inventados. |

### 5.3. Influencia de estándares Web y valores

HTTP permite distinguir recurso, método, representación y resultado; los contratos hacen visibles esas decisiones entre cliente y servicio. OpenAPI puede formalizar posteriormente la descripción de la API, pero un documento válido no demuestra implementación o permisos correctos. Spec Kit organiza el trabajo guiado por especificación; el uso de su estructura no certifica por sí solo el resultado.

La solidaridad se concreta en compartir un vocabulario y revisar escenarios del equipo; la transparencia, en declarar fuentes y pendientes; la responsabilidad, en exigir verificación y proteger datos; la honestidad, en no presentar borradores, fixtures o correcciones asistidas como validación institucional o contribución personal demostrada.

## 6. Preguntas de Control

### 6.1. ¿Qué diferencia existe entre una necesidad, un requisito y una decisión de implementación?

La necesidad expresa el problema o beneficio del actor: identificar qué carrera y modelo corresponden a una evaluación. El requisito especifica un comportamiento exigible: crear un proceso con carrera, período y versión publicada en preparación. La decisión de implementación determina cómo resolverlo: un controlador Spring que delega a un servicio y persiste en PostgreSQL. La necesidad justifica; el requisito permite comprobar; la decisión técnica puede cambiar conservando el comportamiento.

### 6.2. ¿Qué característica permite considerar que un requisito es verificable?

Debe permitir decidir con evidencia si se cumple: condiciones de entrada, acción, resultado observable y criterio de aprobación/rechazo. “Catálogo rápido” no basta; p95 de resultado visible ≤ 2 s en un entorno y carga definidos sí permite una medición. Si el entorno falta, el requisito todavía necesita precisión experimental. “Crear proceso en PREPARATION y no activarlo” puede comprobarse consultando el estado guardado y ausencia de activación.

### 6.3. ¿Por qué una historia de usuario debe expresar el beneficio esperado?

El beneficio explica para qué existe la capacidad y permite priorizarla según una necesidad. En US3 no se solicita llenar un formulario por sí mismo: se busca identificar la autoevaluación antes de su autorización. Esa intención permite rechazar una activación implícita aunque la interfaz parezca más cómoda y ayuda a evitar funcionalidades ajenas al incremento.

### 6.4. ¿Qué relación existe entre una historia y sus criterios de aceptación?

La historia expresa actor, capacidad y beneficio; sus criterios delimitan qué resultados hacen aceptable esa capacidad. Una historia puede necesitar varios escenarios: alta válida, código repetido, unidad inexistente y acceso sin permiso. Los criterios no describen cada línea de código, pero son una base para diseñar pruebas. Aprobar un único caso feliz no cubre todos los criterios de la historia.

### 6.5. ¿Qué debe hacerse cuando existe una ambigüedad que el equipo aún no puede resolver?

Registrar una pregunta identificada, alternativas, impacto, responsable de decidir y estado pendiente; consultar al docente o persona competente. Mientras tanto, limitar el alcance o usar un supuesto explícito reversible sin presentarlo como aprobación. Aquí CL-APE-03 conserva la diferencia entre fallos consecutivos y acumulados; no se cambia una política de seguridad por conveniencia de redacción.

### 6.6. ¿Por qué los contratos definidos en esta práctica se consideran preliminares?

Porque identifican recursos, operaciones y datos para acordar comportamiento, pero todavía requieren validación de alcance, esquemas, errores, permisos y compatibilidad con clientes. Que las fichas se apoyen en código existente reduce especulación, pero no les concede aprobación docente ni las convierte en un OpenAPI final. Podrán refinarse en el diseño posterior conservando su vínculo con requisitos y evitando romper contratos operativos.

### 6.7. ¿Cómo se relacionará posteriormente un requisito con una prueba automatizada?

Se enlaza necesidad → requisito → historia → criterio → prueba → ejecución. Por ejemplo, APE-FR-03 se relaciona con AC-U2-02: crear dos carreras con código normalizado equivalente debe rechazar la segunda y dejar una sola. La prueba automatizada comprobará estado HTTP o excepción de dominio y persistencia, utilizando datos sintéticos. Se registrarán commit, entorno, fecha y resultado. La existencia de `CareerAdministrationServiceTest` es un punto de partida, no una nueva ejecución ni prueba de todo el requisito.

## 7. Conclusiones

Se preparó una especificación académica acotada al acceso, catálogo y preparación de procesos, con necesidades, actores y límites explícitos. La separación de alta y activación permite conservar la intención institucional sin inventar competencias. Los requisitos se relacionan con historias, escenarios y operaciones preliminares, lo que facilita su revisión y posterior automatización.

La práctica deja pendientes verificables: aceptación docente del alcance, evidencia institucional del problema, resolución de políticas abiertas, protocolos RNF y contribuciones individuales. La validación docente y publicación no se sustituyen por un documento elaborado ni por un enlace al commit base.

## 8. Recomendaciones

- Presentar `specification.md` al docente y completar CL-APE con respuestas y respaldo real.
- Incorporar ACD1, AA1 y APE1 si el equipo dispone de esos archivos; no reconstruirlos como hechos pasados.
- Validar los contratos contra controladores y corregir OpenAPI en una tarea posterior autorizada.
- Registrar el aporte de cada integrante con revisión personal, cambio verificable y enlace real de commit/PR cuando se publique.
- Versionar los archivos de esta actividad de forma selectiva; no incluir otros archivos locales ni secretos por usar una operación Git indiscriminada.
- Compilar en Overleaf y revisar tablas/enlaces antes de entregar el PDF si el docente lo solicita.

## 9. Bibliografía / Referencias

- Guía de Actividades Práctico-Experimentales Nro. 002, Desarrollo Basado en Plataformas, proporcionada por el usuario; datos y preguntas reproducidos en este reporte.
- Proyecto SIGEAC/SEC-UNL, commit `faf868ea5a8a85285290c06d3baf8fa49889edac`: `docs/requirements/spec.md`, `docs/requirements/domain-rules.md`, `docs/requirements/process-flows.md`, `specs/005-complete-evaluation-workflows/spec.md` y `.specify/templates/spec-template.md`.
- [ACD2, insumo académico](../ACD2-semana-2/ACD2_SEMANA_2.md) y [auditoría documental](../../AUDITORIA_DOCUMENTACION_SDD.md).
- [GitHub Spec Kit, repositorio oficial](https://github.com/github/spec-kit). Se utiliza como referencia del enfoque y se inspecciona la plantilla local; no se declara ejecución del CLI.
- [RFC 9110: HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110.html), Fielding, Nottingham y Reschke, 2022.
- [OpenAPI Specification](https://spec.openapis.org/oas/latest.html), OpenAPI Initiative; referencia de descripción de interfaces HTTP, no actualización de la versión del proyecto.

## 10. Anexos

| Anexo | Archivo |
|---|---|
| A. Especificación refinada | [specification.md](specification.md) |
| B. Contratos preliminares | [CONTRATOS_PRELIMINARES.md](CONTRATOS_PRELIMINARES.md) |
| C. Historial local y evidencia de diferencias | [HISTORIAL_CAMBIOS.md](HISTORIAL_CAMBIOS.md) |
| D. Nota individual para personalizar | [NOTA_INDIVIDUAL.md](NOTA_INDIVIDUAL.md) |

La versión LaTeX incorpora la especificación, los contratos y la nota individual como anexos de contenido. El historial/diff y manifiesto permanecen como evidencia digital enlazada, sin capturas ficticias.

## 11. Declaración de uso de Inteligencia Artificial

Se utilizó ChatGPT/Codex para inspeccionar documentación y código del repositorio, seleccionar un incremento académico, redactar y refinar requisitos, historias, criterios, contratos y respuestas de control, y generar los archivos Markdown/LaTeX y su evidencia local. También se consultaron fuentes técnicas públicas oficiales.

No se ejecutaron pruebas de negocio ni solicitudes HTTP, no se crearon commits/PR ni se publicó la entrega. No se atribuye al docente una revisión no realizada ni a estudiantes una contribución no identificada. Los integrantes deben revisar el contenido, completar sus datos, registrar su aporte y asumir responsabilidad por la versión presentada. La revisión humana final continúa pendiente.

