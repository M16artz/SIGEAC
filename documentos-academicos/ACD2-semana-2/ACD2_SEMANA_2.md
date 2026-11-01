# ACD2 — Semana 2: requisitos verificables y fundamentos de HTTP

| Dato | Información |
|---|---|
| Universidad | Universidad Nacional de Loja |
| Facultad | Facultad de la Energía, las Industrias y los Recursos Naturales No Renovables |
| Carrera | Computación |
| Asignatura | Desarrollo de Software Distribuido (SDD) |
| Modalidad | Actividad grupal con evidencia individual |
| Proyecto | SIGEAC; denominado SEC-UNL en la especificación del repositorio |
| Integrantes | [PENDIENTE: nombres del equipo] |
| Docente | [PENDIENTE: nombre del docente] |
| Ciclo/paralelo | [PENDIENTE] |
| Fecha de la sesión | [PENDIENTE: fecha real de la actividad] |
| Base de revisión | Commit `faf868ea`; código y documentación disponibles |
| Estado | Desarrollo documental preparado; socialización, autoría individual y retroalimentación real pendientes de registrar |

**Propósito de la guía:** Formular requisitos funcionales y no funcionales verificables a partir del problema del proyecto y comprender los fundamentos de HTTP como protocolo base para la comunicación entre clientes y servicios web.

**Alcance de esta entrega.** Este documento constituye la actualización académica complementaria de la especificación inicial para ACD2. Selecciona los requisitos principales, precisa sus verificaciones y los relaciona con historias, escenarios e intercambios HTTP. La especificación completa permanece en `docs/requirements/spec.md` y `specs/005-complete-evaluation-workflows/spec.md`. Las mejoras propuestas no se presentan como decisiones institucionales aprobadas ni se aplican silenciosamente a los documentos canónicos.

**Declaración de uso de IA.** Se utilizó ChatGPT/Codex para inspeccionar archivos, contrastar requisitos con código, consultar documentación técnica y redactar esta entrega. El equipo debe revisar el contenido, completar sus datos y asumir la responsabilidad del documento final. No se atribuyen al equipo intervenciones en clase ni retroalimentación no aportada. No se declara supervisión humana ya completada.

## 1. Metodología de análisis de requisitos y HTTP

### 1.1. Fuentes y procedimiento

Se partió del diagnóstico de `AUDITORIA_DOCUMENTACION_SDD.md` y se contrastaron las fuentes del proyecto con controladores y DTO reales. El procedimiento fue: revisar contexto y alcance; seleccionar requisitos principales; hacer explícitas condiciones y resultados; relacionar escenarios y aceptación; interpretar solicitudes/respuestas; registrar correcciones y pendientes de socialización.

Fuentes de producto, relativas a la raíz del repositorio:

| Código | Archivo | Uso en la actividad |
|---|---|---|
| D1 | `docs/requirements/spec.md` | Actores, alcance, FR-001–040, RNF-001–019, criterios SC y CL-001–010. |
| D2 | `docs/requirements/domain-rules.md` | Estados, fórmulas, responsabilidades y CL-011–017. |
| D3 | `docs/requirements/process-flows.md` | Procesos P01–P16 y escenarios alternativos. |
| D4 | `specs/005-complete-evaluation-workflows/spec.md` | FR-041–070 e historias de flujos completos. |
| D5 | `docs/testing/process-acceptance.md` | Criterios AC-P y AC-X. |
| D6 | `docs/testing/traceability.md` | Relaciones entre requisitos, historias, tareas y pruebas. |
| D7 | `docs/technical/contracts/openapi.yaml` | Contrato HTTP; debe contrastarse con los controladores. |
| D8 | `docs/architecture/complete-workflows.md` | Contexto, contenedores y límites del backend modular. |

Fuentes ejecutables específicas:

| Código | Archivo exacto |
|---|---|
| C1 | `backend/src/main/java/ec/edu/unl/sec/selfassessment/api/CareerController.java` |
| C2 | `backend/src/main/java/ec/edu/unl/sec/selfassessment/application/CareerAdministrationService.java` |
| C3 | `backend/src/main/java/ec/edu/unl/sec/selfassessment/domain/Career.java` |
| C4 | `backend/src/main/java/ec/edu/unl/sec/identityaccess/api/SessionController.java` |
| C5 | `backend/src/main/java/ec/edu/unl/sec/identityaccess/infrastructure/SessionAuthenticationFilter.java` |
| C6 | `backend/src/main/java/ec/edu/unl/sec/identityaccess/infrastructure/SessionCookie.java` |
| C7 | `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/api/EvaluationModelController.java` |
| C8 | `backend/src/main/java/ec/edu/unl/sec/evidence/api/EvidenceSourceController.java` |
| C9 | `backend/src/main/java/ec/edu/unl/sec/reporting/domain/DocumentaryProgressCalculator.java` |
| C10 | `backend/src/main/java/ec/edu/unl/sec/shared/api/ApiProblemHandler.java` |
| C11 | `backend/src/main/java/ec/edu/unl/sec/shared/api/EntityTagSupport.java` |
| C12 | `frontend/src/app/core/http/api-client.ts` |
| C13 | `frontend/proxy.conf.json` y `frontend/angular.json` |
| C14 | `backend/src/main/java/ec/edu/unl/sec/identityaccess/infrastructure/SecurityConfiguration.java` |
| C15 | `backend/src/main/java/ec/edu/unl/sec/identityaccess/infrastructure/JpaIdentityAccessRepository.java` |
| C16 | `backend/src/main/java/ec/edu/unl/sec/selfassessment/api/CareerCatalogProblemHandler.java` |

Los ejemplos HTTP se reconstruyen a partir de estas fuentes con valores sintéticos. No son capturas de tráfico ni resultados de pruebas realizadas para esta entrega. La revisión es documental y estática; no se arrancaron servicios ni se modificó el código de negocio.

### 1.2. Problema del proyecto y necesidad

El proyecto aborda la organización y trazabilidad de la autoevaluación interna de carreras: configurar modelos, planificar responsables, aportar información, revisar documentación y conservar las decisiones y resultados. La especificación exige distinguir tres actos: recopilar información, aceptar documentalmente una evidencia y valorar académicamente un indicador.

La necesidad funcional documentada consiste en disponer de información identificable por carrera, proceso, indicador, fuente y versión, con acceso autorizado e historial. Sin esa separación, una carga o aceptación documental podría interpretarse indebidamente como cumplimiento académico; el proyecto establece explícitamente que tal equivalencia no debe producirse.

**Límite de la evidencia:** no se encontraron entrevistas, cifras de tiempos actuales ni un diagnóstico institucional que pruebe cómo se desarrolla hoy el proceso fuera del sistema. La descripción anterior resume el problema abordado por la especificación; no afirma que una institución utilice actualmente hojas de cálculo, pierda documentos o tenga una tasa de errores determinada.

### 1.3. Actores y responsabilidades

| Actor | Responsabilidad pertinente |
|---|---|
| Administrador funcional | Mantener catálogos, modelos y asignaciones dentro de sus capacidades autorizadas. |
| Coordinador de autoevaluación | Preparar y planificar procesos; consultar avance y tramitar decisiones autorizadas. |
| Responsable documental | Crear borradores, aportar contenido y remitir evidencias de su ámbito. |
| Revisor documental | Decidir sobre coberturas de una versión remitida, conservando fundamento. |
| Evaluador académico | Valorar indicadores con reglas y sustentos autorizados; no modificar revisión documental. |
| Consultor autorizado | Consultar resultados e historial dentro del ámbito otorgado. |
| Administrador técnico | Operar configuración y cuentas según sus capacidades, sin adquirir competencia académica. |
| Cuenta técnica | Ejecutar tareas técnicas explícitas; no adoptar decisiones humanas por privilegio técnico. |

Las autoridades, comisiones y funciones de aprobación de P12–P16 están descritas como configurables. Su competencia real sigue pendiente de validación institucional. No se equipara “tener sesión” con “poder aprobar”.

### 1.4. Alcance y exclusiones

El MVP documental comprende P01–P11 y consulta histórica básica de P16: acceso, carreras, modelos, autorización del proceso, planificación, evidencias, revisión, subsanación y seguimiento documental. Valoración académica, informes, mejora y cierre integral corresponden al alcance posterior D7–D8, aunque el repositorio ya contiene implementación y evidencias técnicas de esos módulos.

Se excluyen acreditación oficial, evaluación externa en el alcance inicial, datos reales no autorizados, integraciones institucionales productivas, aplicación móvil y operación sin conexión durante el MVP. La disponibilidad multirregión y el despliegue productivo tampoco son compromisos de esta actividad.

Para ACD2 se analizan escenarios representativos del MVP; la valoración se incluye para demostrar la separación entre aceptación documental y resultado académico.

### 1.5. Clarificaciones de la semana anterior

Se recuperan decisiones existentes; no se inventan respuestas previas.

| Registro | Pregunta recuperada o reformulada | Información documentada | Estado de confirmación |
|---|---|---|---|
| CL-001 / CL-015 | ¿Qué función y autoridad pueden autorizar cada actuación? | Funciones y política sintética; asignación y constancia obligatorias. | Pendiente de validación institucional. |
| CL-002 | ¿Cuándo se permite combinar funciones o autorrevisar? | Política de conflictos y excepciones temporales auditables. | Confirmar autoridad y procedimiento de excepción. |
| CL-005 / CL-013 | ¿Qué escala o regla se aplica y qué puede automatizarse? | Escalas configurables; cálculo solo con reglas validadas. | Confirmar metodología de cada indicador. |
| CL-006 | ¿Cómo se calcula el avance? | Fuentes obligatorias aplicables; métricas separadas. | Regla del piloto documentada; validación institucional pendiente. |
| CL-008 | ¿Qué archivos y conservación se requieren? | Máximo inicial 25 MiB y conservación durante la vida del proceso. | Tipos, retención y eliminación institucional por confirmar. |
| CL-011 / CL-014 | ¿Se utilizan cinco modelos completos y vigentes? | Configuraciones representativas; procedencia y vigencia por corroborar. | No confundir prueba sintética con catálogo normativo completo. |
| CL-016 / CL-017 | ¿Quién aprueba informes y qué permite cerrar? | Políticas versionadas, pendientes y excepciones explícitas. | Sin constancia institucional de aprobación. |

No existe evidencia suficiente para afirmar que estas preguntas fueron respondidas en la sesión anterior. El equipo debe completar persona, fecha, respuesta y respaldo cuando disponga del registro real.

## 2. Resultados de la actividad: especificación académica revisada

### 2.1. Requisitos funcionales principales

Se conservan los IDs del proyecto. Las formulaciones siguientes precisan la intención de sus fuentes para esta actividad; no introducen funcionalidades ajenas ni cambian su aprobación institucional.

| ID y fuente | Formulación verificable | Actor y verificación |
|---|---|---|
| FR-001–002, D1 | Antes de atender una operación protegida, el servidor deberá comprobar sesión vigente, capacidad y ámbito aplicables. Si no se satisfacen, rechazará la operación sin modificar el recurso. | Todos; sesión ausente, válida y fuera de ámbito. |
| FR-005, D1 | Un administrador autorizado podrá habilitar o desactivar una identidad y conservará su autoría histórica; la desactivación impedirá acceso posterior. | Administrador; desactivar y repetir solicitud con sesión previa. |
| FR-043, D4 | El administrador funcional podrá registrar carreras con código normalizado único, nombre, unidad académica y vigencia válida; se rechazará el código duplicado y se conservará el historial de cambios. | Administrador; alta válida, duplicado y cambio histórico. |
| FR-007/045, D1/D4 | Una versión de modelo admitirá nodos tipados con distinta profundidad; rechazará ciclos y pertenencias inválidas antes de publicarse. | Administrador de modelos; indicador directo/anidado y ciclo. |
| FR-008–009, D1 | La publicación validará estructura, fuentes, metodología y revisión independiente; una versión publicada no podrá sobrescribirse y su corrección originará una sucesora. | Autor/revisor; publicación incompleta y sucesión. |
| FR-011/048, D1/D4 | El coordinador podrá preparar un proceso para carrera, período y modelo; la activación exigirá carrera/modelo aplicables y autorización registrada y verificada. | Coordinador/autoridad; sin autorización permanece PREPARATION. |
| FR-013/050, D1/D4 | La planificación asignará participantes o equipos con función, indicador y vigencia explícitos; rechazará asignaciones incompatibles sin excepción válida. | Coordinador; vigencia, conflicto y ámbito. |
| FR-016–018/051, D1/D4 | El responsable podrá preparar evidencia de su ámbito y declarar coberturas exactas; remitir exigirá metadatos completos y contenido compatible con cada fuente cubierta. | Responsable; fuente ajena, contenido incompleto y caso válido. |
| FR-019–020, D1 | Una remisión conservará versión, autor y fecha; no admitirá edición posterior. Una corrección creará una sucesora sin eliminar la predecesora. | Responsable; remitir, intentar edición y corregir. |
| FR-021–022, D1 | El revisor autorizado registrará resultados por cobertura de una versión remitida y se derivará el estado global de esos resultados, conservando actor, fecha y fundamento aplicable. | Revisor; aceptar una cobertura y observar otra. |
| FR-023/055, D1/D4 | Aceptar documentalmente una evidencia no creará una valoración académica ni marcará el indicador como cumplido. | Revisor/evaluador; comparar registros académicos antes/después. |
| FR-053, D4 y D2 | La recopilación será el número de IDs distintos de fuentes obligatorias aplicables con cobertura remitida vigente dividido entre las fuentes obligatorias aplicables, por cien; borradores y fuentes opcionales no contarán. | Coordinador; 3 de 4 = 75 %. |
| FR-054, D4 y D2 | La revisión documental contará fuentes obligatorias aplicables cuya cardinalidad se satisfaga con coberturas aceptadas vigentes; no duplicará una fuente por tener varios archivos. | Coordinador; 2 de 4 = 50 % y cardinalidad mayor que uno. |
| FR-030, D1 | Desde un agregado autorizado, el usuario podrá acceder a indicadores, decisiones y versiones que explican el conteo, conservando las restricciones de ámbito. | Consultor/coordinador; cada destino abre el sustento correspondiente. |
| FR-031/064, D1/D4 | El sistema distinguirá un corte sin cambio de estado del cierre documental sujeto a política; ninguno exigirá ejecutar valoración, informes o mejora posteriores. | Coordinador; corte con pendientes y MVP sin P12–P15. |
| FR-039, D1 | Los errores expondrán una estructura estable con código, descripción, instancia y correlación, sin secretos ni información de ámbitos ajenos. | Todos; entrada inválida y error de negocio. |

Observaciones: FR-030 mantiene implementación parcial en el tablero localizado. La consulta/registro de decisiones existe, pero no demuestra toda la política institucional. Los requisitos compuestos deben desglosarse al diseñar pruebas; aprobar un caso no verifica automáticamente todas sus cláusulas.

### 2.2. Requisitos no funcionales y mecanismo de medición

Los umbrales numéricos provienen del repositorio. Las condiciones experimentales pendientes se identifican, sin inventar resultados.

| Categoría / ID | Exigencia y forma de verificar | Estado y límite |
|---|---|---|
| Seguridad, RNF-001 | Almacenar contraseñas mediante hash adaptativo con sal; inspeccionar almacenamiento y verificar autenticación sin exponer secretos. | BCrypt localizado; no es auditoría integral de seguridad. |
| Seguridad, RNF-002 | Expirar sesión tras 30 minutos de inactividad; cerrar sesión o desactivar cuenta invalidará acceso posterior. Verificar con reloj controlado y solicitudes sucesivas. | Mecanismos y pruebas existentes. |
| Seguridad, RNF-003 | Limitar autenticaciones al alcanzar cinco fallos en 15 minutos. Comparar error de usuario inexistente y contraseña incorrecta. | Confirmar si son consecutivos o acumulados: texto y SQL difieren. |
| Rendimiento, RNF-006 | p95 de operaciones interactivas del MVP ≤ 2 s, excluyendo archivos e informes. Definir inicio/fin visibles, entorno, red y operaciones antes de medir. | Medición histórica de consultas seleccionadas: no acredita todos los recorridos visibles. |
| Rendimiento/capacidad, RNF-007–008 | p95 de cargas hasta 25 MiB ≤ 15 s; al menos 100 usuarios, 30 sesiones, 500 indicadores, 10 000 versiones y 100 GiB sin incumplir RNF-006. | Falta campaña de cargas y volumen 100 GiB acreditado. |
| Disponibilidad, RNF-009 | Un fallo de exportación/notificación futura no impedirá autenticación ni operaciones del MVP. Ejecutar recorridos con ese componente fallando y comparar estado. | No se exige un porcentaje de uptime no definido; ensayo de fallos pendiente. |
| Recuperación, RNF-010 | Una interrupción entre metadatos y archivos dejará operación completa o recuperable, sin archivo ausente presentado como disponible. Interrumpir, reconciliar y comprobar integridad. | Recuperación localizada; RPO/RTO institucional por definir. |
| Usabilidad, SC-003/007 | Recorrido D1–D6 en ≤ 45 min; al menos 19 de 20 participantes completan la tarea al primer intento según protocolo. | Los datos deben ser sintéticos; los participantes humanos representativos. Protocolo/resultados pendientes. |
| Accesibilidad, RNF-014 | Recorridos declarados con teclado, foco visible, etiquetas y contraste WCAG 2.2 AA. Verificar criterios aplicables automáticamente y con revisión humana. | Recorridos Edge no certifican todo el nivel AA. |
| Mantenibilidad, RNF-017–018 | Mantener reglas separadas de controladores, persistencia e interfaz; verificar dependencias y pruebas reproducibles de estados, permisos y cálculo. | ArchUnit y suites disponibles; dependencias SQL requieren revisión adicional. |
| Interoperabilidad, FR-035 y contratos | Validar formatos versionados, entradas y errores al intercambiar información; distinguir contrato operativo y futuro. | No existe RNF independiente específico; integración externa aún futura. |

**Candidato no aprobado de interoperabilidad (ACD-INT-01):** “Los clientes de las operaciones HTTP seleccionadas deberán poder interpretar las representaciones JSON y los errores documentados, conservando nombres, tipos y semántica del contrato versionado”. Verificación propuesta: cliente HTTP independiente y pruebas de esquema contra rutas operativas. Se deriva del diseño REST; el equipo debe validarlo antes de integrarlo al catálogo RNF oficial. No obliga a habilitar CORS ni a implementar importación externa futura.

### 2.3. Calidad y requisitos corregidos

| ID | Redacción o problema de partida | Formulación revisada y justificación |
|---|---|---|
| FR-053 | “Fuentes aplicables” puede incluir opcionales y duplicar coberturas. | Contar IDs distintos de fuentes obligatorias aplicables cubiertas por versión remitida vigente. Hace explícitos conjunto, denominador y vigencia; coincide con D2 y C9. |
| FR-030 | “Permitir navegar” no define origen, destino ni comprobación. | Desde cada agregado autorizado se accederá a sus indicadores, decisiones y versiones exactas, manteniendo ámbito. Permite comprobar cada enlace y detectar una interfaz incompleta. |
| RNF-006 | “Resultado visible” no equivale a duración del endpoint. | Mantener p95 ≤ 2 s y definir medición desde acción del usuario hasta presentación del resultado, con conjunto de operaciones y entorno registrado. Evita usar una medición API como prueba del recorrido completo. |
| RNF-003 | El enunciado dice fallos consecutivos; el SQL cuenta fallos dentro de ventana. | Pendiente de decisión: conservar consecutividad y ajustar comportamiento, o aprobar ventana acumulada de cinco fallos/15 min. No se elige una política silenciosamente. |
| SC-007 | Se menciona “participantes sintéticos”. | Participantes representativos utilizando datos sintéticos. La usabilidad requiere personas; la protección de datos se mantiene. |

Comprobación de los cinco criterios de la guía:

| Requisito revisado | Claro | Específico | Pertinente | Verificable | Consistente con alcance |
|---|---|---|---|---|---|
| FR-053 | Define qué se cuenta. | Distingue obligatorias y vigentes. | Seguimiento documental. | Ejemplo 3/4 y cero fuentes. | P11, MVP. |
| FR-030 | Identifica navegación y sustentos. | Define destinos y ámbito. | Trazabilidad. | Apertura y permisos de cada destino. | MVP; implementación parcial. |
| RNF-006 | Resultado visible definido. | p95 y exclusiones existentes. | Respuesta del sistema. | Requiere protocolo de entorno. | MVP; no amplía entregas. |
| RNF-003 | Aún requiere elegir política. | Ventana/umbral conocidos. | Control de acceso. | Dos secuencias de intentos. | Acceso; no aprobado el cambio. |
| SC-007 | Persona y datos diferenciados. | 19 de 20 participantes. | Usabilidad. | Registro de tareas y ayuda. | Recorridos documentales. |

### 2.4. Casos de uso representativos

Los identificadores P proceden de D3; se detallan cuatro escenarios suficientes para el propósito de ACD2.

#### P02 — Registrar una carrera

- Actor principal: administrador funcional autorizado.
- Objetivo: disponer de una carrera identificable para posteriores procesos.
- Precondiciones: sesión vigente, capacidad de administración y unidad académica activa.
- Flujo principal: seleccionar unidad; introducir código, nombre y vigencia; enviar; normalizar código; validar unicidad/unidad/fechas; guardar historial y auditoría; responder con carrera, Location y ETag.
- Alternativa: código repetido produce 409 y no crea otra carrera.
- Excepción: solicitud malformada o campos inválidos produce 400; unidad no disponible produce 422 según C2.
- Postcondición de éxito: carrera activa con revisión inicial 1, unidad y autoría conservadas.
- Postcondición de rechazo: no se crea la carrera solicitada.
- Requisitos: FR-043, FR-002, FR-006; RNF-011–012.

#### P08 — Remitir una versión de evidencia

- Actor principal: responsable documental.
- Objetivo: poner una versión completa a disposición del revisor.
- Precondiciones: proceso habilitado, asignación vigente, borrador completo, contenido permitido y coberturas exactas.
- Flujo principal: consultar versión/revisión; comprobar metadatos, fuentes y contenido; remitir con revisión vigente; reautorizar y validar integridad; conservar versión inmutable y autor/fecha; incorporarla a revisión.
- Alternativa: metadatos o fuentes incompletos conservan el borrador y bloquean remisión.
- Excepción: revisión obsoleta produce 412; no debe sobrescribirse la versión ajena.
- Postcondición: versión SUBMITTED inmutable o borrador conservado sin remisión parcial.
- Requisitos: FR-018–019, FR-051; RNF-010–012.

#### P09 — Revisar coberturas documentales

- Actor principal: revisor documental autorizado.
- Objetivo: determinar aceptabilidad documental de cada cobertura.
- Precondiciones: versión remitida vigente, capacidad de revisión y separación de funciones aplicable.
- Flujo principal: consultar contenido; revisar cada fuente; registrar ACCEPTED/OBSERVED/REJECTED y fundamento; derivar global; conservar decisión; actualizar proyección documental.
- Alternativa: aceptar una cobertura y observar otra produce global OBSERVED; únicamente la cobertura aceptada puede contribuir conforme a cardinalidad.
- Excepción: autorrevisión sin excepción válida o acceso no autorizado se rechazan; la política de excepción exige validación adicional.
- Postcondición: decisión conservada y sin valoración académica creada automáticamente.
- Requisitos: FR-021–024, FR-054–055.

#### P11 — Consultar seguimiento documental

- Actor principal: coordinador o consultor autorizado.
- Objetivo: identificar pendientes sin confundir documentación con cumplimiento académico.
- Precondiciones: proceso existente y ámbito de consulta vigente.
- Flujo principal: filtrar fuentes autorizadas; excluir opcionales/no aplicables del denominador; contar remisiones y aceptaciones vigentes; calcular porcentajes; mostrar sustento y política.
- Alternativa: cuatro fuentes, tres remitidas y dos aceptadas producen 75 % y 50 %.
- Excepción/caso límite: sin configuración devuelve porcentaje no calculable; todas excluidas autorizadamente tampoco equivalen a 100 %.
- Postcondición: consulta sin cambios de estado y agregados reproducibles.
- Requisitos: FR-029–030, FR-053–054.

### 2.5. Historias de usuario y criterios de aceptación

Son selecciones/refinamientos académicos de historias existentes; no se atribuye su aprobación a la sesión.

#### HU-ACD-01 — Catálogo de carreras (005/US1)

Como administrador funcional, quiero registrar carreras con su unidad y vigencia, para preparar procesos sobre un catálogo identificable y conservar su historial.

- AC-01: **Dado que** la unidad está activa y el código no existe, **cuando** registro campos válidos, **entonces** se crea una carrera con revisión 1 y se devuelve 201 y su Location.
- AC-02: **Dado que** el código normalizado ya existe, **cuando** intento registrar otro igual, **entonces** recibo 409 y no se crea un duplicado.
- AC-03: **Dado que** mi identidad no tiene capacidad de administración, **cuando** intento crear, **entonces** la operación se rechaza y no modifica el catálogo.

#### HU-ACD-02 — Remisión trazable (004/US3 y 005/US4)

Como responsable documental, quiero remitir una versión completa vinculada a fuentes exactas, para que el revisor conozca qué documentación se aporta y bajo qué versión.

- AC-04: **Dado que** el borrador satisface metadatos, contenido y cobertura, **cuando** lo remito con revisión vigente, **entonces** queda SUBMITTED con autor y fecha y no admite edición ordinaria.
- AC-05: **Dado que** falta una cobertura requerida, **cuando** intento remitir, **entonces** no se crea una remisión parcial y se conserva el borrador.
- AC-06: **Dado que** otro cambio avanzó la revisión, **cuando** envío un If-Match obsoleto, **entonces** recibo 412 y debo recargar antes de decidir.

#### HU-ACD-03 — Revisión por cobertura (005/US5, parte del revisor)

Como revisor documental, quiero decidir por cobertura de una versión remitida, para comunicar su aceptabilidad sin atribuir un resultado académico.

- AC-07: **Dado que** hay dos coberturas, **cuando** acepto una y observo otra con fundamento, **entonces** se conservan ambos resultados y el global se deriva como OBSERVED.
- AC-08: **Dado que** no existe valoración académica, **cuando** acepto documentación, **entonces** no se crea una valoración ni se marca cumplimiento académico.

#### HU-ACD-04 — Seguimiento reproducible (005/US6)

Como coordinador, quiero consultar conteos y porcentajes separados por fuentes, para intervenir sobre pendientes documentales y explicar el sustento de cada agregado.

- AC-09: **Dado que** hay cuatro fuentes obligatorias aplicables, tres con remisión vigente y dos con cardinalidad aceptada, **cuando** consulto seguimiento, **entonces** obtengo 75 % de recopilación y 50 % de revisión.
- AC-10: **Dado que** una fuente opcional tiene varios archivos, **cuando** consulto, **entonces** no aumenta el denominador obligatorio ni se cuenta varias veces.
- AC-11: **Dado que** no existen fuentes obligatorias configuradas, **cuando** consulto, **entonces** no se divide entre cero ni se informa 100 %.

### 2.6. Trazabilidad de la selección

| Necesidad | Requisitos | Historia / caso | Implementación y prueba existente |
|---|---|---|---|
| Identificar la carrera y preservar historial | FR-043 | HU-ACD-01 / P02 | C1–C3; `tests/acceptance/career-catalog.spec.ts`. |
| Conservar información remitida exacta | FR-018–020/051 | HU-ACD-02 / P08/P10 | `backend/src/main/java/ec/edu/unl/sec/evidence/application/SubmitEvidenceService.java`; `tests/acceptance/document-review-correction.spec.ts`. |
| Separar dictamen documental de académico | FR-021–023/055 | HU-ACD-03 / P09 | `backend/src/main/java/ec/edu/unl/sec/documentreview/application/DocumentReviewService.java`; `tests/acceptance/complete-workflows.spec.ts`, AC-P09-01. |
| Explicar avance por fuentes | FR-030/053/054 | HU-ACD-04 / P11 | C9; suite consolidada AC-P11-01. FR-030 sigue parcial en interfaz. |

Las pruebas se identifican como existentes, no como ejecutadas nuevamente. Las tres pruebas vacías `evidence-v1.spec.ts`, `evidence-v1-v2.spec.ts` y `evidence-audit.spec.ts` no se utilizan como prueba de cumplimiento.

## 3. Contraste técnico: HTTP, JSON y revisión final

### 3.1. Funcionamiento básico y arquitectura

HTTP es un protocolo de aplicación de solicitud/respuesta. El método expresa la operación, la URL identifica el recurso, las cabeceras aportan metadatos y el contenido transporta una representación cuando corresponde. El servidor responde con estado, cabeceras y contenido opcional. La semántica del protocolo se mantiene aunque la aplicación conserve una sesión mediante cookies. Fuente: [RFC 9110](https://www.rfc-editor.org/rfc/rfc9110.html).

En este proyecto, Angular utiliza rutas relativas `/api/v1` mediante C12; Spring Boot recibe las solicitudes, autoriza y coordina módulos, persistencia PostgreSQL y archivos. El backend es monolítico modular; no se afirma que existan microservicios ni un broker activo.

Una solicitud comprende línea de petición, cabeceras, línea en blanco y cuerpo opcional. En los ejemplos se usa notación HTTP/1.1 legible; no se afirma que una captura real negociara esa versión. Una URL completa distingue esquema, host, puerto, ruta y consulta. Por ejemplo, `http://localhost:8080/api/v1/careers?page=0&size=20` identifica el servicio local, catálogo y parámetros de paginación; el fragmento de URL no se envía al servidor.

### 3.2. Comparación de GET, POST, PUT, PATCH y DELETE

| Método | Semántica general | Ejemplo real del proyecto |
|---|---|---|
| GET | Recuperar una representación; seguro e idempotente. No se solicita una mutación de negocio. | `/api/v1/careers/{id}` devuelve carrera y ETag. |
| POST | Procesar contenido conforme al recurso; puede crear o ejecutar una acción. No es idempotente por definición. | `/api/v1/careers` crea; `/api/v1/sessions` inicia sesión. |
| PUT | Crear/reemplazar el estado representado del recurso objetivo; semántica idempotente. | `/api/v1/model-versions/{id}` reemplaza un borrador con el DTO legado. |
| PATCH | Aplicar modificaciones a un recurso; no es necesariamente idempotente. | `/api/v1/users/{userId}` recibe `{active:false}`. |
| DELETE | Solicitar retirar la asociación del recurso objetivo; semántica idempotente. | Retirar una referencia externa del borrador; logout en `/api/v1/sessions`. |

Fuentes: [MDN: métodos](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Methods) y [RFC 5789: PATCH](https://www.rfc-editor.org/rfc/rfc5789.html).

Idempotencia significa que repetir una operación tiene el mismo efecto pretendido, no que siempre devuelva el mismo código. Un segundo DELETE puede ser rechazado o encontrar el recurso ausente. Un segundo PUT con revisión vieja puede recibir 412 sin contradecir esa semántica. PATCH no significa enviar cualquier subconjunto de campos: manda el contrato concreto. Por ejemplo, el PATCH de carreras exige todos los campos de CareerInput; no se debe omitir `academicUnitId` suponiendo un JSON Merge Patch genérico.

### 3.3. Códigos de estado solicitados

| Estado | Interpretación general | Ejemplo respaldado por el código |
|---|---|---|
| 200 OK | Solicitud atendida correctamente. | GET de carrera o PUT del borrador. |
| 201 Created | Se crearon uno o más recursos. | POST de carrera con Location. |
| 204 No Content | Operación completada sin contenido de respuesta. | DELETE de referencia o cierre de sesión. |
| 400 Bad Request | Solicitud no procesable por error del cliente. | JSON/UUID inválido o campos no válidos. |
| 401 Unauthorized | Faltan credenciales válidas para el recurso. | Sesión ausente, revocada o expirada. |
| 403 Forbidden | El servidor rechaza la operación. | Sesión válida sin capacidad o token CSRF inválido. |
| 404 Not Found | No se encuentra o no se revela el recurso. | Carrera desconocida o no visible. |
| 409 Conflict | Conflicto con el estado actual. | Código de carrera duplicado. |
| 422 Unprocessable Content | Contenido comprendido, instrucciones no procesables. | Unidad académica no disponible, según C2. |
| 500 Internal Server Error | Condición inesperada del servidor. | C10 devuelve INTERNAL_ERROR sin detalle técnico al cliente. |

Fuente de semántica general: [MDN: estados HTTP](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status). La asociación con rutas concretas procede del repositorio.

También son relevantes 412 (revisión obsoleta), 428 (falta If-Match), 413 (archivo demasiado grande) y 429 (limitación de autenticación). Un error de negocio no debe interpretarse automáticamente como 500; el cuerpo explica el código de dominio. Un fallo de CORS del navegador tampoco equivale necesariamente a un 500 del backend.

### 3.4. Cabeceras y autenticación real del proyecto

| Cabecera | Propósito | Aplicación o límite |
|---|---|---|
| Content-Type | Describe el formato del contenido transportado. | `application/json`; para errores `application/problem+json`; los archivos se cargan con multipart. |
| Accept | Indica formatos de respuesta admitidos por el cliente. | Puede indicar JSON y Problem Details; no convierte por sí sola el cuerpo enviado. |
| Authorization | Transporta credenciales según un esquema de autenticación. | `Bearer <token>` es una forma común, pero no es el mecanismo implementado de SIGEAC. |
| Location | Referencia un recurso creado o destino, según la respuesta. | En 201 de carrera: `/api/v1/careers/{id}`. No significa una redirección automática. |
| ETag | Validador opaco de una representación. | C11 utiliza revisión fuerte entre comillas, por ejemplo `"1"`. No es credencial de autorización. |
| If-Match | Condiciona la modificación al validador recibido. | Evita sobrescritura silenciosa; si está obsoleto, 412. |
| Cookie / Set-Cookie | Envío y establecimiento de estado de sesión. | Cookie SEC_SESSION; no enviar su valor real en el informe. |
| X-CSRF-TOKEN | Token propio de protección de mutaciones autenticadas. | C5 valida su hash; una cookie válida no sustituye el token. |

Referencias: [Content-Type](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Content-Type), [Accept](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Accept), [Authorization](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Authorization), [Location](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Location) y [ETag](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/ETag).

El login de C4 devuelve `csrfToken` y establece SEC_SESSION. No devuelve JWT ni habilita `Authorization: Bearer` por el ejemplo de la guía. Se conserva ese concepto en el análisis general y se usa Cookie en los ejemplos del proyecto. En C14 Spring desactiva CSRF integrado, pero C5 implementa una comprobación propia; no se concluye ausencia de protección a partir de una sola línea.

### 3.5. Ejercicio resuelto: POST de carrera y respuesta JSON

Ejemplo sintético compatible con C1–C3. Los UUID, fecha y tokens no representan personas ni solicitudes reales. Se presupone una unidad activa con el ID indicado y un administrador autorizado.

```http
POST /api/v1/careers HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Accept: application/json, application/problem+json
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

| Elemento | Interpretación |
|---|---|
| Método | POST solicita crear una carrera mediante el catálogo. |
| Recurso | `/api/v1/careers` es la colección; la carrera creada tiene URI propia. |
| URL y Host | La ruta y `Host` permiten identificar el servicio y recurso. |
| Cabeceras de petición | Content-Type describe JSON; Accept admite éxito/error; Cookie identifica sesión; CSRF protege la mutación. |
| Cuerpo | Código, nombre, unidad y vigencia; el servidor asigna ID, autor, estado y revisión. |
| Estado | 201 es apropiado porque se creó un recurso, no solo se consultó uno. |
| Location | Identifica la nueva carrera; C1 implementa el GET de esa ruta. |
| ETag | Revisión inicial `"1"` para futuras modificaciones condicionadas. |
| Cuerpo de respuesta | Representación del recurso; `data` contiene los atributos de negocio. |

Si se repite el código, C2 lanza 409 DUPLICATE_CODE; no se promete que repetir POST cree o devuelva siempre el mismo objeto. Si la unidad no está activa, se produce 422 UNIT_UNAVAILABLE. El cuerpo debe respetar la distinción de fechas ISO, cadenas y `null`.

### 3.6. GET y PATCH: consulta y concurrencia

```http
GET /api/v1/careers/22222222-2222-4222-8222-222222222222 HTTP/1.1
Host: localhost:8080
Accept: application/json
Cookie: SEC_SESSION=<sesion-sintetica>
```

La respuesta exitosa es 200 con la representación Career y ETag. No se envía cuerpo para este GET.

```http
PATCH /api/v1/careers/22222222-2222-4222-8222-222222222222 HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Cookie: SEC_SESSION=<sesion-sintetica>
X-CSRF-TOKEN: <csrf-sintetico>
If-Match: "1"

{
  "code": "CARRERA-DEMO",
  "name": "Carrera de demostración actualizada",
  "academicUnitId": "11111111-1111-4111-8111-111111111111",
  "validFrom": "2026-01-01",
  "validUntil": null
}
```

Con revisión 1 vigente, la respuesta es 200, revisión 2 y ETag `"2"`. Si otro cliente ya guardó la revisión 2, el PATCH anterior recibe 412 y no sobrescribe sus cambios. La respuesta de error tiene forma Problem Details; este ejemplo es reconstruido desde C2/C16:

```http
HTTP/1.1 412 Precondition Failed
Content-Type: application/problem+json

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

No confundir 412 con 409: el primero señala que no se cumplió la condición de versión; el segundo puede señalar código duplicado o estado incompatible.

### 3.7. PUT y DELETE implementados

PUT operativo: `/api/v1/model-versions/{id}`. El DTO legado tiene `source`, `criteria`, `scale` y `formula`; el DTO configurado de `/configured` tiene `source`, `nodes` y `configuration`. Debe enviarse la representación admitida completa, sesión, CSRF e If-Match. Éxito: 200 y ETag. Un modelo publicado requiere sucesión; no se presupone que el PUT cree un recurso inexistente porque la semántica general lo permita.

DELETE operativo: retirar una referencia externa de un borrador autorizado:

```http
DELETE /api/v1/evidence-versions/55555555-5555-4555-8555-555555555555/external-references/66666666-6666-4666-8666-666666666666 HTTP/1.1
Host: localhost:8080
Cookie: SEC_SESSION=<sesion-sintetica>
X-CSRF-TOKEN: <csrf-sintetico>
If-Match: "3"
```

```http
HTTP/1.1 204 No Content
ETag: "4"
```

No hay cuerpo JSON tras 204. Se retira la referencia del borrador, no necesariamente el contenido externo al que apunta. C8 llama al servicio que exige versión editable; no es un permiso para borrar versiones remitidas.

### 3.8. JSON: interpretación de datos y errores

JSON representa objetos, arreglos, cadenas, números, booleanos y `null`; los nombres de objeto y cadenas usan comillas dobles. No incorpora comentarios ni comas finales. Fechas e identificadores se representan como cadenas con convenciones del contrato. Fuente: [RFC 8259](https://www.rfc-editor.org/rfc/rfc8259.html).

En el alta de carrera, `revision` es número, `classificationPending` booleano y `validUntil` puede ser `null`; `"null"` sería una cadena distinta. Un JSON sintácticamente válido todavía puede infringir una regla de negocio: por ejemplo, una unidad inexistente produce 422 en C2. Un cuerpo malformado se interpreta como 400 en C10. Los campos de autoría/revisión los controla el servidor y no se añaden al cuerpo CareerInput.

### 3.9. CORS y orígenes distintos

Un origen combina esquema, host y puerto. `http://localhost:4200` y `http://localhost:8080` son orígenes distintos. CORS permite al servidor declarar qué orígenes pueden leer respuestas desde el navegador; no sustituye autenticación ni autorización. Fuente: [Fetch Standard: CORS](https://fetch.spec.whatwg.org/#http-cors-protocol).

En C12 el cliente usa `/api/v1`; C13 configura proxy `/api` hacia `http://localhost:8080`. Si el navegador accede a Angular en 4200 y solicita esa ruta relativa, ve el mismo origen; el proxy realiza el salto al backend. No se localizaron políticas CORS explícitas en las fuentes revisadas y no se afirma que el acceso directo entre orígenes esté habilitado.

Si se despliegan frontend y API en orígenes distintos, deben definirse orígenes permitidos, métodos y cabeceras; con cookies, credenciales y política de cookies compatibles. Con credenciales no se usa `Access-Control-Allow-Origin: *`. Para leer ETag o Location desde JavaScript pueden requerirse `Access-Control-Expose-Headers`. JSON y cabeceras como If-Match/CSRF pueden provocar preflight OPTIONS. Referencia: [MDN: CORS](https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/CORS).

Ejercicio hipotético de preflight, **no implementado ni ejecutado como política del proyecto**:

```http
OPTIONS /api/v1/careers HTTP/1.1
Host: localhost:8080
Origin: http://localhost:4200
Access-Control-Request-Method: POST
Access-Control-Request-Headers: content-type,x-csrf-token
```

Para permitir ese escenario, una configuración futura tendría que responder con origen explícito, método/cabeceras permitidos y credenciales cuando corresponda. Un navegador puede impedir leer la respuesta si faltan permisos CORS, aunque la API funcione en Postman. No se modifica la configuración como parte de ACD2.

### 3.10. Socialización y retroalimentación

No se ha aportado evidencia de la sesión. El análisis técnico siguiente sirve como insumo de discusión, no como testimonio de compañeros o docente.

| Observación técnica preparada | Mejora incorporada a este desarrollo | Validación grupal |
|---|---|---|
| Un archivo no equivale a una fuente aceptada ni a cumplimiento académico. | FR-053/054 distinguen fuentes, remisiones y aceptación; AC-08/09 comprueban separación. | [PENDIENTE: confirmar discusión y acuerdo] |
| “Rápido” no ofrece un criterio de aprobación. | RNF-006 conserva p95 ≤ 2 s y exige protocolo del resultado visible. | [PENDIENTE] |
| El ejemplo Bearer de la guía no coincide con la aplicación. | Ejercicios usan SEC_SESSION y CSRF, sin inventar JWT. | [PENDIENTE] |
| Una cabecera ETag sin If-Match no evita por sí sola sobrescrituras. | Ejercicio de concurrencia interpreta 412 y recarga. | [PENDIENTE] |

Registro para la sesión real:

| Persona que observa | Observación recibida y fecha | Requisito/ejercicio afectado | Cambio realizado | Evidencia/acuerdo |
|---|---|---|---|---|
| [PENDIENTE] | [PENDIENTE: observación real] | [PENDIENTE] | [PENDIENTE] | [PENDIENTE] |

No debe marcarse esta tabla como completada copiando una observación hipotética. La evidencia individual complementaria contiene dos requisitos corregidos y un ejercicio desarrollado, pero también exige el comentario real de retroalimentación de cada estudiante.

### 3.11. Comprobación de la guía y conclusiones

| Criterio de la actividad | Evidencia preparada | Pendiente real |
|---|---|---|
| Problema, actores, alcance y clarificaciones | Secciones 1.2–1.5, fuentes D1–D4. | Diagnóstico institucional y respuestas verificadas. |
| Requisitos funcionales | Sección 2.1 y verificaciones. | Aprobación del equipo y decisiones abiertas. |
| Siete categorías de RNF | Sección 2.2, umbrales y límites. | Protocolos y resultados que faltan. |
| Redacción clara y verificable | Sección 2.3 y justificaciones. | Registrar aceptación de correcciones. |
| Casos de uso e historias | Secciones 2.4–2.6; AC-01–11. | Socialización y ajustes acordados. |
| HTTP y JSON | Secciones 3.1–3.8; solicitudes/respuestas resueltas. | Si se pide ejecución, capturas reales sanitizadas. |
| CORS | Sección 3.9 y proxy existente. | Decidir política solo si el despliegue lo requiere. |
| Evidencia individual | `EVIDENCIA_INDIVIDUAL.md`. | Nombre, aporte real, retroalimentación y revisión personal. |
| Participación y retroalimentación | Registro preparado en 3.10. | No acreditada todavía. |

El aporte del análisis consiste en vincular necesidades documentadas con condiciones y resultados comprobables: conservar versiones, autorizar por ámbito y contar fuentes distintas. Los ejemplos HTTP muestran que el mismo recurso puede consultarse, crearse o modificarse con métodos y precondiciones diferentes. ETag/If-Match permite explicar concurrencia entre clientes; Cookie y CSRF reflejan la autenticación real del sistema. El informe no acredita participación, ejecución ni aprobación institucional inexistentes.

## Referencias

- Proyecto SIGEAC/SEC-UNL, commit `faf868ea`. Archivos D1–D8 y C1–C16 identificados en la sección 1.1; repositorio local.
- [Auditoría documental previa](../../AUDITORIA_DOCUMENTACION_SDD.md).
- Fielding, R.; Nottingham, M.; Reschke, J. (2022). [RFC 9110: HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110.html).
- Dusseault, L.; Snell, J. (2010). [RFC 5789: PATCH Method for HTTP](https://www.rfc-editor.org/rfc/rfc5789.html).
- Bray, T. (2017). [RFC 8259: The JavaScript Object Notation (JSON) Data Interchange Format](https://www.rfc-editor.org/rfc/rfc8259.html).
- WHATWG. [Fetch Standard, CORS protocol](https://fetch.spec.whatwg.org/#http-cors-protocol). Consulta técnica: 31 de octubre de 2026.
- MDN Web Docs. Referencias de [métodos](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Methods), [estados](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status), [cabeceras](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers) y [CORS](https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/CORS), consultadas para esta entrega.

