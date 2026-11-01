# Feature Specification: catálogo autorizado y preparación de procesos de autoevaluación

**Feature Branch**: `main`, rama observada; esta práctica documental no crea una rama de implementación.

**Created**: 2026-10-31, fecha de preparación del documento; no equivale a fecha de asistencia a clase.

**Status**: Draft académico refinado — pendiente de validación docente y del equipo.

**Input**: Guía APE2 Nro. 002, Desarrollo Basado en Plataformas, Unidad 1. Insumos disponibles: especificaciones del repositorio, auditoría documental y ACD2. Los documentos nominales ACD1, AA1 y APE1 no fueron aportados/localizados como entregas académicas independientes en esta preparación.

**Repositorio**: [M16artz/SIGEAC](https://github.com/M16artz/SIGEAC), dirección derivada del remote origin; publicación de esta entrega y accesibilidad externa no verificadas.

**Línea base inspeccionada**: `faf868ea5a8a85285290c06d3baf8fa49889edac`.

## Contexto, problema y necesidades

SIGEAC, denominado SEC-UNL en los documentos funcionales, apoya la autoevaluación interna de carreras. El problema abordado por la especificación es identificar qué carrera se evalúa, qué versión del modelo se utiliza, quién puede actuar y cómo se conservan datos y decisiones sin confundir recopilación, revisión documental y valoración académica.

Para este incremento, la necesidad se concentra en preparar información de partida fiable y autorizada antes de iniciar actuaciones institucionales. No se afirma que se hayan medido pérdidas documentales, tiempos de gestión o errores en la institución: faltan entrevistas y diagnóstico empírico verificables.

| Necesidad identificada | Actor | Fuente existente |
|---|---|---|
| N-01: acceder únicamente a operaciones y datos autorizados. | Usuario habilitado y consultor autorizado. | FR-001–006 de `docs/requirements/spec.md`. |
| N-02: identificar carreras, unidades y vigencias sin códigos ambiguos ni pérdida histórica. | Administrador funcional. | FR-043 de `specs/005-complete-evaluation-workflows/spec.md`; P02. |
| N-03: preparar una evaluación con carrera, período y versión publicada exacta. | Coordinador con función autorizada para gestionar procesos. | FR-011–012; P05; `docs/requirements/domain-rules.md`. |
| N-04: comprender rechazos y resolver cambios concurrentes sin sobrescritura silenciosa. | Administrador/coordinador. | FR-039; RNF-011 y RNF-013. |

## Actores

| Actor | Responsabilidad del incremento | Límite |
|---|---|---|
| Usuario habilitado | Iniciar/cerrar sesión y consultar su identidad y funciones. | Tener sesión no concede administración. |
| Administrador funcional autorizado | Crear y mantener unidades/carreras y consultar historial. | Requiere capacidad de catálogo; no se infiere autoridad académica. |
| Coordinador autorizado | Consultar modelos disponibles y preparar un proceso. | Para crear requiere la capacidad de gestionar procesos en la carrera; el nombre del cargo no basta. |
| Consultor autorizado | Leer catálogo/procesos según su asignación vigente. | No modifica recursos por permiso de lectura. |
| Administrador técnico / cuenta técnica | Operación técnica según las políticas existentes. | No adopta decisiones institucionales; no es actor de alta ordinaria del catálogo en el servicio actual. |

## Objetivo del incremento y alcance para la Unidad 1

Permitir especificar y comprobar un recorrido reducido: autenticar una identidad autorizada, disponer de una carrera identificable, seleccionar una versión publicada y crear un proceso en preparación, con errores y cambios de catálogo trazables.

**Incluye**: sesión y consulta de identidad; listado/consulta de unidades; alta de unidad cuando el actor esté autorizado; alta, consulta, modificación y desactivación de carrera con historial; consulta de versión publicada como prerrequisito; creación y consulta de proceso en `PREPARATION`; controles de ámbito, concurrencia, auditoría y errores aplicables.

**Excluye de este incremento académico**: asignación de nuevas autoridades; publicación o edición del modelo; autorización y activación institucional del proceso; equipos, actividades, evidencias, revisión documental, cálculos, informes, mejora y archivo. Son exclusiones de esta selección, no afirmaciones de ausencia en el repositorio.

**Exclusiones del producto inicial que se conservan**: acreditación oficial, evaluación externa, integración institucional productiva, aplicación móvil, operación sin conexión y datos institucionales reales no autorizados.

**Viabilidad**: la práctica dura 3 horas y produce especificación, clarificaciones y contratos preliminares; no se plantea construir ni certificar todo el sistema en ese tiempo. El recorte propuesto debe ser confirmado por el docente (CL-APE-01).

## Restricciones y separación de decisiones

- Mantener la separación entre revisión documental, valoración y acreditación, aun cuando no se ejecuten aquí.
- Usar identidades y datos sintéticos; conservar los identificadores canónicos FR/RNF y referenciar sus refinamientos.
- No cambiar políticas institucionales ni presentar datos demo como normativa corroborada.
- Las tecnologías existentes son contexto de contraste, no parte de la redacción de necesidades: Angular, Spring Boot, PostgreSQL y el backend modular se describen fuera de los requisitos funcionales.
- Conservar los archivos históricos; las correcciones académicas quedan en este documento y en el registro de cambios, sin sustituir silenciosamente las especificaciones del producto.

## User Scenarios & Testing

Esta sección conserva la estructura de `.specify/templates/spec-template.md`: historias priorizadas, motivo, prueba independiente y escenarios de aceptación. Se utiliza Dado–Cuando–Entonces en español. Las pruebas se describen para validación; no se declara su ejecución en esta práctica.

### User Story 1 - Acceder y consultar funciones vigentes (Priority: P1)

Como usuario habilitado, quiero iniciar y cerrar mi sesión y consultar mis funciones vigentes, para operar solo mientras mi acceso esté autorizado y poder terminarlo cuando finalice mi trabajo.

**Why this priority**: protege el acceso a los demás recorridos y proporciona valor independiente al comprobar identidad y ámbitos.

**Independent Test**: utilizar una identidad sintética ya habilitada; iniciar sesión, consultar identidad, cerrar sesión e intentar repetir la consulta, sin necesitar crear una carrera o proceso.

**Acceptance Scenarios**:

1. **AC-U1-01**: **Dado que** la identidad está activa y sus credenciales son válidas, **cuando** inicia sesión y consulta su identidad, **entonces** obtiene sesión y funciones vigentes sin datos de otras identidades.
2. **AC-U1-02**: **Dado que** la sesión está activa, **cuando** se cierra y se intenta reutilizar, **entonces** la operación protegida es rechazada.
3. **AC-U1-03**: **Dado que** las credenciales son incorrectas o la identidad no existe, **cuando** intenta autenticarse, **entonces** no se concede sesión y el rechazo no confirma la existencia de la cuenta.
4. **AC-U1-04**: **Dado que** han transcurrido 30 minutos sin actividad o la identidad fue desactivada, **cuando** solicita una operación protegida, **entonces** debe autenticarse nuevamente y no recibe el recurso protegido.

### User Story 2 - Mantener catálogo identificable e histórico (Priority: P1)

Como administrador funcional autorizado, quiero registrar y mantener carreras con unidad y vigencia, para preparar evaluaciones sobre un catálogo identificable sin borrar sus relaciones históricas.

**Why this priority**: la carrera es la unidad evaluada y su identidad condiciona el proceso posterior.

**Independent Test**: con administrador y unidad sintéticos disponibles, crear una carrera, consultar, modificar con revisión vigente, desactivar y comprobar historial sin crear un proceso. La alta de unidad es un prerrequisito o alternativa administrativa, no obliga a implementar todas las historias juntas.

**Acceptance Scenarios**:

1. **AC-U2-01**: **Dado que** la unidad existe y está activa y los datos son válidos, **cuando** registra una carrera con código no utilizado tras normalización, **entonces** se crea con identidad, unidad, vigencia y revisión inicial.
2. **AC-U2-02**: **Dado que** el código normalizado ya existe, **cuando** intenta registrar otro equivalente, **entonces** se rechaza y no aparece un duplicado.
3. **AC-U2-03**: **Dado que** falta un campo obligatorio, la unidad no está disponible o la vigencia termina antes de iniciar, **cuando** envía el alta, **entonces** se informa el error correspondiente y no se crea la carrera.
4. **AC-U2-04**: **Dado que** consulta la revisión vigente, **cuando** actualiza campos válidos, **entonces** se conserva una nueva revisión y la anterior permanece consultable.
5. **AC-U2-05**: **Dado que** dos clientes parten de la misma revisión, **cuando** ambos intentan modificar, **entonces** una modificación basada en revisión obsoleta no sobrescribe la otra; se exige recargar.
6. **AC-U2-06**: **Dado que** una carrera tiene historial, **cuando** se desactiva con motivo, **entonces** conserva su identidad y referencias históricas; no se realiza eliminación física.
7. **AC-U2-07**: **Dado que** una identidad carece de capacidad de administración, **cuando** intenta crear/modificar/desactivar, **entonces** el catálogo no se modifica.
8. **AC-U2-08**: **Dado que** una identidad lectora solo tiene carrera A autorizada, **cuando** consulta listado, total o detalle de B, **entonces** no recibe información de B ni totales ajenos.

### User Story 3 - Preparar un proceso con modelo publicado (Priority: P2)

Como coordinador autorizado para gestionar procesos en una carrera, quiero registrar carrera, período y versión publicada del modelo, para preparar una autoevaluación identificable sin presumir que ya fue autorizada institucionalmente.

**Why this priority**: utiliza el catálogo y modelo disponibles; es posterior a las bases de acceso, pero puede validarse con fixtures sin ejecutar altas nuevas ni activación.

**Independent Test**: utilizar una carrera activa y un modelo publicado sintéticos preexistentes; crear el proceso y consultarlo como el mismo actor, sin activar ni remitir evidencias.

**Acceptance Scenarios**:

1. **AC-U3-01**: **Dado que** el actor está autorizado, la carrera/unidad están activas, el período pertenece a su vigencia y el modelo está publicado, **cuando** prepara el proceso, **entonces** se conserva con estado `PREPARATION`, carrera, período y versión exacta del modelo.
2. **AC-U3-02**: **Dado que** la carrera está desactivada o la unidad no está disponible, **cuando** intenta preparar un proceso, **entonces** se rechaza sin crear uno parcialmente válido.
3. **AC-U3-03**: **Dado que** el período termina antes de comenzar o excede la vigencia de la carrera, **cuando** intenta crear, **entonces** se rechaza sin guardar un proceso.
4. **AC-U3-04**: **Dado que** la versión del modelo no está publicada o no está disponible, **cuando** intenta crear, **entonces** se rechaza y no se sustituye automáticamente por otra versión.
5. **AC-U3-05**: **Dado que** existe un proceso en preparación de su ámbito, **cuando** lo consulta, **entonces** observa las referencias guardadas y ningún estado de activación o acreditación inferido.
6. **AC-U3-06**: **Dado que** no tiene capacidad/ámbito de gestión o consulta, **cuando** intenta crear o leer un proceso ajeno, **entonces** no produce cambios ni recibe información protegida.

### Edge Cases

- Diferencias de mayúsculas y espacios de un código no deben permitir duplicados conforme a la normalización definida.
- `validUntil = null` representa vigencia sin término explícito; no equivale a cadena vacía ni a la cadena `"null"`.
- Una carrera activa en estado puede no estar vigente en la fecha de consulta o requerir clasificación; no basta comprobar solo la palabra ACTIVE.
- Crear proceso fija la referencia publicada; activar exige políticas adicionales excluidas aquí. No se mezcla publicación técnica con vigencia normativa institucional.
- La revisión inicial de carrera es 1 y la del proceso actual es 0; no imponer una revisión inicial común a todos los recursos.
- Paginación fuera de rango es inválida; un listado vacío autorizado no demuestra ausencia de registros en otros ámbitos.
- Si falla una escritura crítica o su auditoría obligatoria, no debe presentarse como cambio completado sin evidencia consistente; el fallo requiere verificación transaccional específica.
- La regla de cinco fallos “consecutivos” frente a acumulados en ventana de 15 min sigue pendiente; no se incluye una solución inventada.

## Requirements

### Functional Requirements

Los IDs `APE-FR-*` identifican la selección de esta práctica y no reemplazan los FR canónicos. Se mantienen separados comportamiento observable y detalle de implementación.

| ID local / origen | Requisito refinado | Verificación |
|---|---|---|
| APE-FR-01 / FR-001,005 | El sistema deberá permitir iniciar/cerrar sesión a identidades habilitadas y consultar sus funciones vigentes; al cerrar o desactivar la identidad impedirá acceso posterior con esa sesión. | AC-U1-01–04. |
| APE-FR-02 / FR-002,003 | Cada operación protegida deberá comprobar capacidad y ámbito vigente; el acceso lector no concederá modificación ni el privilegio técnico concederá competencia institucional. | AC-U2-07/08, AC-U3-06. |
| APE-FR-03 / FR-043 | El administrador autorizado podrá registrar unidades y carreras con código normalizado único y campos obligatorios; una carrera requerirá unidad activa y vigencia válida. | AC-U2-01–03. |
| APE-FR-04 / FR-043 | La consulta de catálogo deberá permitir identificar carreras, unidades y vigencia y limitar listados, conteos y detalle al ámbito efectivo del actor. | AC-U2-08; escenario lector y administrador separados. |
| APE-FR-05 / FR-043 | Modificar o desactivar una carrera conservará identidad, revisiones previas y relaciones históricas; desactivar requerirá motivo y no eliminará físicamente la carrera. | AC-U2-04/06. |
| APE-FR-06 / FR-010,012 | El actor autorizado podrá consultar e identificar una versión publicada del modelo para utilizar su referencia exacta al preparar el proceso. | AC-U3-01/04; fixture con versión publicada. |
| APE-FR-07 / FR-011 | El actor con gestión vigente podrá crear un proceso para carrera activa, período permitido y versión publicada; el resultado quedará en preparación, sin activarse por el alta. | AC-U3-01–04. |
| APE-FR-08 / FR-002,011,012 | El actor autorizado podrá consultar el proceso preparado y recuperar carrera, período, estado y versión del modelo; una consulta ajena no revelará el recurso. | AC-U3-05/06. |
| APE-FR-09 / FR-006 | Los cambios críticos de catálogo y creación de procesos deberán conservar actor, fecha, objeto, acción y resultado mediante la auditoría definida para la operación. | Consulta de historial y registros; fallo de auditoría por ensayar. |
| APE-FR-10 / FR-039 | Ante entrada inválida, falta de acceso o conflicto, el sistema rechazará la operación con código y descripción estables y correlación, sin exponer secretos ni datos ajenos. | Casos negativos de US1–US3 y contratos CP-01–09. |

### Non-functional Requirements

| ID local / origen | Atributo y requisito verificable | Método de comprobación / límite |
|---|---|---|
| APE-NFR-01 / RNF-001–002,005 | Credenciales con hash adaptativo y sal; sesiones expiran tras 30 min de inactividad; respuestas y registros no expondrán secretos. | Inspección de almacenamiento y pruebas con reloj; no equivale a auditoría completa. |
| APE-NFR-02 / RNF-006 | p95 de resultado visible de las operaciones interactivas seleccionadas ≤ 2 s bajo carga piloto, excluyendo archivos e informes. | Fijar entorno y operaciones; la campaña histórica de consultas no demuestra este subconjunto completo. |
| APE-NFR-03 / RNF-009 | Fallos de canales complementarios ajenos al incremento no deberán impedir acceso, catálogo ni preparación. | Repetir recorridos con canal indisponible; no se inventa porcentaje de disponibilidad. |
| APE-NFR-04 / RNF-011 | Las modificaciones deberán detectar revisión obsoleta y rechazarla sin pérdida silenciosa de cambios. | Dos clientes parten de la misma revisión; uno debe recargar. |
| APE-NFR-05 / RNF-012–013 | Eventos de auditoría serán inmutables para usuarios ordinarios y las solicitudes tendrán correlación para investigar fallos sin datos sensibles. | Intento de modificación y consulta autorizada; comprobar correlación. |
| APE-NFR-06 / RNF-014 | Los recorridos seleccionados deberán completarse con teclado, foco visible y controles etiquetados, y verificarse contra criterios aplicables WCAG 2.2 AA. | Revisión automática/manual; no se declara conformidad completa por disponer de etiquetas. |
| APE-NFR-07 / RNF-017–018 | Las reglas de estados y autorización permanecerán separadas de presentación/persistencia y tendrán pruebas reproducibles con datos sintéticos. | Revisión de dependencias y pruebas; reconocer límites de acoplamiento SQL. |
| APE-NFR-08 / contratos y FR-039 | La especificación de los intercambios del incremento identificará recurso, operación, datos, resultados y errores para que un cliente interprete su representación. | Comparar CP-01–09 con controladores; refinamiento académico de interoperabilidad, pendiente de ratificar como RNF independiente. |
| APE-NFR-09 / SC-007 | Las tareas de acceso, alta y preparación deberán tener protocolo de usabilidad con participantes representativos y datos sintéticos; el criterio global del proyecto es 19 de 20 participantes al primer intento. | La aplicación de ese umbral al recorte U1 requiere CL-APE-05; no se declara medición. |
| APE-NFR-10 / RNF-019 | Las demostraciones y pruebas del incremento utilizarán identidades y datos sintéticos sin requerir información institucional real. | Fixtures identificados y revisión de ejemplos; no publicar tokens ni contraseñas reales. |

### Key Entities

- **Identidad**: persona/cuenta, estado habilitado y referencia de autenticación; la desactivación conserva autoría.
- **Asignación**: función/capacidad, ámbito y vigencia; preexistente en este recorte.
- **Unidad académica**: identificador, código, nombre y estado.
- **Carrera**: identificador, código, nombre, unidad, vigencia, estado y revisiones históricas.
- **Versión de modelo**: identidad y número de una versión publicada; no se edita en este incremento.
- **Proceso**: identidad, código, nombre, carrera, período, versión publicada y estado de preparación.
- **Evento de auditoría**: actor, acción, objeto, fecha, resultado y correlación.

## Success Criteria

### Measurable Outcomes

- **APE-SC-01**: todos los escenarios funcionales AC-U1-01–04, AC-U2-01–08 y AC-U3-01–06 deberán tener resultado registrado antes de aceptar el incremento; esta entrega solo los especifica.
- **APE-SC-02**: en los casos definidos de código normalizado duplicado se creará cero carreras adicionales.
- **APE-SC-03**: las modificaciones exitosas conservarán una revisión anterior recuperable; la modificación obsoleta causará cero sobrescrituras silenciosas.
- **APE-SC-04**: todos los procesos creados en el recorrido de alta quedarán en preparación y conservarán carrera, período y versión seleccionada; cero activaciones implícitas.
- **APE-SC-05**: todos los requisitos locales tendrán origen canónico, historia/escenario y operación preliminar o método de verificación identificados.
- **APE-SC-06**: cada decisión de clarificación resuelta tendrá persona, fecha, respuesta y respaldo; las no resueltas permanecerán pendientes, sin respuestas supuestas.
- **APE-SC-07**: el objetivo p95 y los criterios de accesibilidad/usabilidad tendrán protocolo definido y resultado acotado; no se aceptará “cumplido” sin medición aplicable.

## Assumptions

- Se utilizan usuarios y asignaciones sintéticos ya habilitados; no se exige crear una nueva autoridad.
- Existe una versión publicada de demostración; no se presume vigencia normativa institucional.
- El cliente dispone de conexión y entorno documentado; no se exige modo sin conexión.
- El recorte U1 es propuesta de esta preparación, pendiente del docente; no es una decisión de una clase cuya evidencia no se aportó.
- Los datos de ACD2 se usan como insumo técnico; las entregas ACD1/AA1/APE1 y su retroalimentación real deben incorporarse si el equipo las tiene.
- Los contratos se recuperan del código operativo como punto de partida, pero no se convierten en OpenAPI definitivo ni certifican todos sus permisos.

## Clarifications

No se ha efectuado una reunión con el docente en esta preparación. El registro distingue decisión editorial adoptada de aprobación externa.

| ID | Pregunta / decisión | Estado y tratamiento | Validación requerida |
|---|---|---|---|
| DEC-APE-01 | ¿Cómo se nombrará la entrega? | Adoptada documentalmente: `specification.md` en esta carpeta académica; se conservan `spec.md` canónicos. | Verificar que el docente acepta el enlace como producto de práctica. |
| DEC-APE-02 | ¿Qué asignatura y datos se usan? | Se usan los de la guía actual: Desarrollo Basado en Plataformas, 5 A, Unidad 1 y docente Edison Leonardo Coronel Romero. | No trasladar la asignatura SDD de ACD2 como nombre de esta guía. |
| CL-APE-01 | ¿Es suficiente acceso, catálogo y preparación sin activación para Unidad 1? | Pendiente; se propone por viabilidad y dependencia de autoridades no validadas. | Docente; respuesta, fecha y respaldo por registrar. |
| CL-APE-02 | ¿Qué actores/asignaciones están institucionalmente autorizados? | Pendiente CL-001/002/015; usar política sintética y no activar institucionalmente. | Autoridad competente y docente según alcance. |
| CL-APE-03 | ¿Se consideran cinco fallos consecutivos o acumulados en 15 min? | Pendiente: RNF-003 y SQL difieren; no cambiar política por documentación. | Equipo/docente y responsable de seguridad. |
| CL-APE-04 | ¿Qué evidencia institucional sustenta el problema? | Pendiente: no se aportaron entrevistas o actas. | Equipo; incorporar fuente real sin inventar cifras. |
| CL-APE-05 | ¿Qué carga, entorno y tareas se medirán para RNF y usabilidad? | Pendiente; conservar umbrales existentes y delimitar protocolos. | Equipo/docente. |
| CL-APE-06 | ¿Qué correcciones indicó el docente al revisar esta especificación? | Pendiente; no existe una observación de sesión aportada. | Registrar texto, persona, fecha, IDs y cambio aceptado. |

## Contratos preliminares

Las fichas [CP-01–CP-09](CONTRATOS_PRELIMINARES.md) integran este documento por referencia. Identifican requisitos, recurso, operación, entrada, resultado y errores. Su estado es preliminar académico, contrastado con código; no se anuncia una API nueva ni se modifican contratos productivos.

| Contrato | Operación / recurso | Requisitos |
|---|---|---|
| CP-01 | Iniciar/cerrar sesión y consultar identidad. | APE-FR-01/02/10; APE-NFR-01. |
| CP-02 | Consultar/registrar unidad académica. | APE-FR-03/04/09/10. |
| CP-03 | Registrar carrera. | APE-FR-03/09/10. |
| CP-04 | Consultar catálogo, detalle e historial. | APE-FR-04/05/10. |
| CP-05 | Modificar carrera con precondición. | APE-FR-05/09/10; APE-NFR-04. |
| CP-06 | Desactivar carrera con motivo. | APE-FR-05/09/10. |
| CP-07 | Consultar modelo/versiones existentes. | APE-FR-06. |
| CP-08 | Preparar proceso. | APE-FR-07/09/10. |
| CP-09 | Consultar proceso preparado. | APE-FR-08/10. |

## Trazabilidad inicial y verificación posterior

Las rutas son relativas a la raíz del repositorio; los nombres de pruebas se citan como existentes, no como resultados nuevos.

| Necesidad | Requisitos / historia | Escenarios | Código y prueba candidata |
|---|---|---|---|
| N-01 | APE-FR-01/02; US1 | AC-U1-01–04, AC-U2-07/08, AC-U3-06 | `backend/src/main/java/ec/edu/unl/sec/identityaccess/application/SessionService.java`; `backend/src/test/java/ec/edu/unl/sec/identityaccess/application/SessionServiceTest.java`; `ScopeAuthorizationServiceTest.java` del mismo directorio. |
| N-02 | APE-FR-03–05; US2 | AC-U2-01–08 | `backend/src/main/java/ec/edu/unl/sec/selfassessment/application/CareerAdministrationService.java`; `backend/src/test/java/ec/edu/unl/sec/selfassessment/application/CareerAdministrationServiceTest.java`; `tests/acceptance/career-catalog.spec.ts`. |
| N-03 | APE-FR-06–08; US3 | AC-U3-01–06 | `backend/src/main/java/ec/edu/unl/sec/selfassessment/application/SelfAssessmentService.java`; `backend/src/test/java/ec/edu/unl/sec/selfassessment/api/SelfAssessmentControllerTest.java`; `tests/acceptance/complete-workflows.spec.ts`, subconjunto P05. |
| N-04 | APE-FR-09/10; APE-NFR-04/05 | AC-U2-04/05, negativos US1–US3 | `backend/src/main/java/ec/edu/unl/sec/shared/api/EntityTagSupport.java`; `backend/src/test/java/ec/edu/unl/sec/selfassessment/application/SelfAssessmentAuditServiceTest.java`. |

La relación posterior será necesidad → requisito → historia → AC → prueba identificada → ejecución con versión/entorno/resultado. Los RNF requieren su verificación adicional; un endpoint presente o una prueba con nombre parecido no acredita cumplimiento.

## Revisión de calidad y cambios

| Problema detectado | Refinamiento realizado | Motivo verificable |
|---|---|---|
| “Gestionar carreras” engloba alta, consulta y cambios. | APE-FR-03/04/05 y AC-U2 separados. | Cada comportamiento tiene entrada, resultado y rechazo identificables. |
| “Crear proceso” puede interpretarse como activarlo. | APE-FR-07 exige PREPARATION; activación fuera de recorte. | Cero activaciones implícitas en APE-SC-04. |
| “Acceso seguro” puede significar solo login. | Sesión, permiso, ámbito y estados diferenciados. | Casos de sesión válida sin permiso y consulta ajena. |
| “Rendimiento adecuado” no define aprobación. | APE-NFR-02 mantiene p95 ≤ 2 s y exige protocolo. | Medición reproducible; entorno aún pendiente. |
| “Datos de demostración” puede confundirse con decisiones oficiales. | Supuestos y CL-APE-02/04 explícitos. | No se atribuye vigencia ni competencia por fixture. |

El documento es claro y pertinente al recorte propuesto; la verificabilidad se expresa con AC e indicadores. La consistencia final de alcance y políticas se mantiene pendiente de CL-APE. Las versiones reales de trabajo y su diferencia se conservan en [HISTORIAL_CAMBIOS.md](HISTORIAL_CAMBIOS.md).

## Validación con el docente

Guion de presentación: el sistema prepara autoevaluación interna para usuarios autorizados; este incremento identifica carrera y versión de modelo y guarda el proceso en preparación. Incluye catálogo, sesión, controles y errores; excluye activación, evidencias y decisiones posteriores. Se comprobará con los 18 AC y las verificaciones RNF, usando datos sintéticos. Siguen pendientes diagnóstico empírico, políticas y aceptación del recorte.

| Revisor | Fecha real | Observación | IDs afectados | Cambio acordado | Evidencia |
|---|---|---|---|---|---|
| [PENDIENTE: revisión de Edison Leonardo Coronel Romero] | [PENDIENTE] | [PENDIENTE: observación real] | [PENDIENTE] | [PENDIENTE] | [PENDIENTE] |

No existe aprobación docente registrada. El enlace GitHub de este archivo solo será evidencia remota después de que el equipo lo versione y publique.

