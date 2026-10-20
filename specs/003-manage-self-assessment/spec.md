# Feature Specification: I2 — Proceso de autoevaluación y asignaciones

**Feature Branch**: `main` (no se creó una rama porque no existe un hook `before_specify`)

**Created**: 2026-10-19

**Status**: Draft

**Input**: User description: "Módulo I2 selfassessment — Proceso de autoevaluación y asignaciones del Sistema de Evaluación de Carreras SEC-UNL. Un coordinador abre un proceso para la carrera piloto con período y una versión publicada del modelo, y asigna responsables, revisores y evaluadores por indicador con ámbitos explícitos y vigencias. Incluye estados borrador, activo, en cierre, cerrado y reabierto con transiciones autorizadas, inmutabilidad de la versión del modelo tras la activación y auditoría de reaperturas y cambios críticos de asignación. Un proceso cerrado no admite cambios salvo reapertura excepcional autorizada, motivada y auditada. Excluye evidencias, revisión documental, valoración académica y reportes. Requisitos de referencia: FR-011 a FR-015; RNF-011, RNF-012, RNF-018. Resultado medible: activar un proceso con modelo v1, asignar participantes y comprobar que publicar un modelo v2 no altera la referencia del proceso."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Abrir y activar un proceso (Priority: P1)

Como coordinador autorizado, quiero crear un proceso para la carrera piloto con un período y una versión publicada del modelo para organizar la autoevaluación sobre una referencia conocida y estable.

**Why this priority**: El proceso y su modelo constituyen el contexto necesario para todas las asignaciones y para los incrementos posteriores.

**Independent Test**: Con una carrera sintética y una versión publicada del modelo se crea un proceso en borrador, se activa y se comprueba que conserva carrera, período y versión seleccionada.

**Acceptance Scenarios**:

1. **Given** una carrera sintética y una versión publicada del modelo dentro del ámbito del coordinador, **When** registra un proceso con código, denominación y período válido, **Then** el proceso queda en estado borrador y conserva las referencias seleccionadas.
2. **Given** un proceso en borrador completo con una versión publicada, **When** un coordinador autorizado lo activa, **Then** queda activo y registra quién y cuándo realizó la activación.
3. **Given** una versión del modelo en borrador o inexistente, **When** se intenta crear o activar un proceso con ella, **Then** la operación se rechaza sin dejar un proceso activo parcial.
4. **Given** un proceso con fecha inicial posterior a la fecha final, **When** se intenta guardarlo o activarlo, **Then** la operación se rechaza con una explicación comprensible.

---

### User Story 2 - Conservar la versión del modelo (Priority: P1)

Como coordinador, quiero que la versión del modelo quede inmutable al activar el proceso para que toda actividad posterior conserve el mismo marco de evaluación, aunque se publiquen versiones nuevas.

**Why this priority**: Cambiar el modelo de un proceso en curso impediría reconstruir el contexto de sus asignaciones y resultados futuros.

**Independent Test**: Se activa un proceso con el modelo v1, se publica el modelo v2 y se verifica que la referencia del proceso sigue siendo v1; cualquier intento de cambiarla después de la activación se rechaza.

**Acceptance Scenarios**:

1. **Given** un proceso activo vinculado al modelo v1, **When** se publica el modelo v2 como sucesor, **Then** el proceso continúa vinculado exactamente a v1 y no incorpora automáticamente ningún elemento de v2.
2. **Given** un proceso activo, en cierre, cerrado o reabierto, **When** se intenta sustituir su versión del modelo, **Then** el cambio se rechaza y la referencia previa permanece intacta.
3. **Given** un proceso todavía en borrador, **When** un coordinador autorizado selecciona otra versión publicada antes de activarlo, **Then** el borrador conserva la nueva selección y la activación posterior fija esa referencia.
4. **Given** una consulta del proceso, **When** se muestra su modelo, **Then** se identifica la versión exacta y no solamente la versión más reciente disponible.

---

### User Story 3 - Asignar participantes por indicador (Priority: P1)

Como coordinador autorizado, quiero asignar responsables, revisores y evaluadores a indicadores concretos con vigencias explícitas para que cada participante conozca y ejerza únicamente su responsabilidad dentro del proceso.

**Why this priority**: Las asignaciones convierten el proceso activo en un trabajo distribuido y delimitan el ámbito operativo de cada persona.

**Independent Test**: Con identidades e indicadores sintéticos se crean asignaciones de las tres funciones, se comprueba su vigencia y alcance, y se rechazan indicadores ajenos al modelo, funciones conflictivas o cambios realizados por personas sin autoridad.

**Acceptance Scenarios**:

1. **Given** un proceso no cerrado y una identidad habilitada, **When** el coordinador asigna una función de responsable, revisor o evaluador para un indicador del modelo del proceso con una vigencia válida, **Then** la asignación queda identificada con participante, función, proceso, indicador, inicio, fin y otorgante.
2. **Given** una asignación vigente para un indicador, **When** el participante consulta sus responsabilidades, **Then** ve el proceso, indicador, función y vigencia exactos sin recibir asignaciones ajenas.
3. **Given** un indicador que no pertenece a la versión fijada en el proceso, **When** se intenta asignar un participante sobre él, **Then** la asignación se rechaza.
4. **Given** una combinación de funciones incompatible según la política vigente de I0, **When** se intenta guardar la asignación sin excepción válida, **Then** se rechaza y se informa el conflicto de forma segura.
5. **Given** una asignación futura, vencida o revocada, **When** el participante intenta utilizarla, **Then** no concede autoridad sobre el indicador.
6. **Given** un cambio de asignación que afecta un proceso activo, en cierre o reabierto, **When** una persona autorizada lo confirma con un motivo, **Then** el cambio entra en vigor sin borrar el historial y queda auditado como crítico.

---

### User Story 4 - Controlar el ciclo de estados (Priority: P2)

Como coordinador autorizado, quiero avanzar el proceso por estados definidos y reabrirlo solo mediante una excepción motivada para proteger su consistencia y evitar cambios posteriores al cierre sin control.

**Why this priority**: El ciclo de vida determina cuándo puede prepararse, ejecutarse, cerrarse o corregirse un proceso y protege el estado cerrado.

**Independent Test**: Un proceso sintético recorre borrador, activo, en cierre y cerrado; se prueban transiciones inválidas y cambios prohibidos, después se registra una reapertura autorizada con motivo y se vuelve a activar.

**Acceptance Scenarios**:

1. **Given** un proceso en borrador, **When** un coordinador autorizado lo activa, **Then** cambia a activo; desde activo puede pasar a en cierre y desde en cierre a cerrado mediante transiciones autorizadas.
2. **Given** un proceso en cualquier estado, **When** se solicita una transición que no corresponde a `borrador → activo → en cierre → cerrado` ni a `cerrado → reabierto → activo`, **Then** la transición se rechaza y el estado no cambia.
3. **Given** un proceso cerrado, **When** se intenta modificar sus datos o asignaciones, **Then** la operación se rechaza y no produce cambios parciales.
4. **Given** un proceso cerrado y una persona autorizada por la política vigente, **When** solicita reabrirlo con un motivo no vacío, **Then** el proceso pasa a reabierto y conserva el cierre, el motivo, el responsable y la fecha de reapertura.
5. **Given** un proceso reabierto, **When** se realizan cambios autorizados y el coordinador confirma la reanudación, **Then** vuelve a activo sin cambiar la versión del modelo fijada originalmente.
6. **Given** una persona sin autoridad o una solicitud sin motivo, **When** intenta reabrir un proceso cerrado, **Then** la reapertura se rechaza y queda auditada.

---

### User Story 5 - Reconstruir cambios críticos (Priority: P3)

Como responsable autorizado de control, quiero consultar el historial de transiciones, reaperturas y cambios críticos de asignación para reconstruir quién cambió el proceso, por qué y con qué resultado.

**Why this priority**: La trazabilidad permite demostrar que los cambios posteriores a la activación y las excepciones de reapertura fueron autorizados.

**Independent Test**: Se ejecutan transiciones, un cambio crítico de asignación, una reapertura aprobada y varios intentos rechazados; la consulta autorizada muestra eventos ordenables e inmutables con actor, motivo, objeto y resultado.

**Acceptance Scenarios**:

1. **Given** una activación, transición, reapertura o cambio crítico de asignación, **When** la acción concluye, **Then** queda un evento inmutable con actor, fecha, proceso, acción, resultado, motivo cuando sea obligatorio y correlación.
2. **Given** un intento rechazado de transición, reapertura o cambio crítico, **When** se consulta la auditoría dentro del ámbito autorizado, **Then** puede reconstruirse el rechazo sin revelar información fuera de ámbito.
3. **Given** un usuario ordinario, **When** intenta modificar o eliminar el historial, **Then** la operación se impide.

### Edge Cases

- El período comienza y termina el mismo día: es válido porque las fechas inicial y final son inclusivas; solo se rechaza cuando la fecha inicial es posterior a la final.
- La versión elegida deja de ser la más reciente o se marca como sustituida después de activar el proceso: la referencia fijada permanece válida e inalterada.
- Una nueva versión del modelo reutiliza códigos de indicadores: las asignaciones existentes siguen apuntando a los indicadores de la versión fijada y no se trasladan automáticamente.
- La vigencia de una asignación no se superpone con el período del proceso: la asignación se rechaza porque nunca podría estar activa dentro del proceso.
- Dos asignaciones idénticas se superponen para la misma identidad, función, proceso e indicador: la segunda se rechaza para evitar ambigüedad.
- Una identidad se desactiva o una excepción de conflicto vence durante el proceso: las operaciones posteriores dejan de estar autorizadas, pero la asignación histórica se conserva.
- Dos coordinadores modifican simultáneamente el proceso o la misma asignación: se detecta la revisión obsoleta y ningún cambio sobrescribe silenciosamente al otro.
- Dos solicitudes intentan activar o cambiar de estado el mismo proceso al mismo tiempo: solo una transición parte del estado vigente; la otra se rechaza como conflicto recuperable.
- Un proceso cerrado recibe una solicitud combinada de reapertura y modificación: la reapertura debe confirmarse primero; ningún cambio se aplica si la reapertura falla.
- Una reapertura se autoriza, pero el evento obligatorio de auditoría no puede conservarse: el proceso no se presenta como reabierto.
- La fecha de una asignación se modifica de modo que deja de cubrir el momento actual: deja de conceder autoridad inmediatamente después del cambio confirmado, sin eliminar su historial previo.

## Requirements *(mandatory)*

### Scope Boundaries

**Included**:

- Registro de la carrera piloto sintética necesaria para identificar el ámbito del proceso.
- Creación de un proceso con código, denominación, carrera, período y versión publicada del modelo.
- Activación e inmutabilidad de la referencia a la versión del modelo.
- Asignaciones de responsables, revisores y evaluadores por indicador, con vigencia e historial.
- Estados borrador, activo, en cierre, cerrado y reabierto, con transiciones autorizadas.
- Reapertura excepcional autorizada y motivada.
- Auditoría de transiciones, reaperturas, cambios críticos de asignación y rechazos relacionados.

**Excluded**:

- Registro, carga, remisión o versionado de evidencias.
- Decisiones de revisión documental.
- Valoraciones académicas y aplicación de escalas.
- Cálculo de avance, tableros, informes y exportaciones.
- Ejecución de fórmulas o estrategias de valoración.
- Definición definitiva de autoridades, conflictos, plazos o motivos institucionales de reapertura.
- Cambio o migración automática del proceso a una versión posterior del modelo.

### Functional Requirements

- **FR-011 — Creación del proceso**: Un coordinador autorizado DEBE poder crear un proceso en estado borrador para una carrera, un período válido y una versión publicada del modelo, con código y denominación identificables; el sistema DEBE rechazar versiones no publicadas, períodos inválidos y referencias fuera del ámbito autorizado sin dejar una activación parcial.
- **FR-012 — Modelo fijado al activar**: Al activar el proceso, el sistema DEBE fijar la identidad y el número exactos de la versión publicada del modelo; esa referencia NO DEBE cambiar en los estados activo, en cierre, cerrado o reabierto, ni al publicarse o sustituirse posteriormente otra versión del modelo.
- **FR-013 — Asignaciones por indicador**: Un coordinador autorizado DEBE poder asignar identidades habilitadas como responsables, revisores o evaluadores sobre indicadores pertenecientes a la versión fijada, con vigencia explícita que se superponga al período del proceso; la asignación DEBE respetar el ámbito, la política de conflictos y las excepciones vigentes de I0, conservar el historial y no conceder autoridad fuera de su función, indicador o vigencia.
- **FR-013a — Cambio crítico de asignación**: Crear, modificar, revocar o reemplazar una asignación que afecte un proceso activo, en cierre o reabierto DEBE considerarse un cambio crítico, exigir una persona autorizada y un motivo no vacío, surtir efecto únicamente si se confirma por completo y conservar el estado anterior en el historial.
- **FR-014 — Estados y transiciones**: El sistema DEBE administrar los estados borrador, activo, en cierre, cerrado y reabierto mediante las transiciones autorizadas `borrador → activo → en cierre → cerrado` y `cerrado → reabierto → activo`; cualquier otra transición DEBE rechazarse sin alterar el estado. Un proceso cerrado NO DEBE admitir cambios hasta que una reapertura excepcional haya concluido correctamente.
- **FR-015 — Reapertura y auditoría**: Reabrir un proceso cerrado DEBE requerir una persona autorizada por la política vigente y un motivo no vacío, conservar la información del cierre anterior y registrar responsable, fecha, motivo y resultado; las transiciones, reaperturas, cambios críticos de asignación y rechazos asociados DEBEN generar eventos de auditoría inmutables y consultables por ámbito.

### Applicable Quality Requirements

- **RNF-011 — Concurrencia**: Las modificaciones concurrentes de procesos, asignaciones o estados DEBEN detectar revisiones obsoletas, impedir transiciones duplicadas y evitar la pérdida silenciosa de cambios.
- **RNF-012 — Auditoría**: Los eventos de transiciones, reaperturas y cambios críticos DEBEN ser inmutables para usuarios ordinarios, consultables solo dentro del ámbito autorizado y ordenables inequívocamente por fecha y correlación.
- **RNF-018 — Verificabilidad**: Las reglas de modelo fijado, ámbitos, vigencias, conflictos, estados, reapertura y concurrencia DEBEN contar con pruebas automatizadas reproducibles basadas en datos sintéticos.

### Key Entities *(include if feature involves data)*

- **Carrera**: Unidad académica que delimita el proceso; para la aceptación de I2 se utiliza una carrera piloto sintética identificable.
- **Proceso de autoevaluación**: Instancia con código, denominación, carrera, período, versión del modelo, estado y responsables de sus transiciones.
- **Referencia de versión del modelo**: Identidad exacta de una versión publicada seleccionada durante el borrador y fijada de forma inmutable al activar el proceso.
- **Indicador del proceso**: Referencia a un indicador perteneciente a la versión fijada; no copia ni mezcla indicadores de versiones posteriores.
- **Asignación del proceso**: Vínculo histórico entre proceso, indicador, identidad, función, vigencia, otorgante, motivo cuando sea crítico y estado vigente o revocado.
- **Transición de estado**: Cambio autorizado entre dos estados del proceso con actor, fecha y resultado; no contiene evidencias, decisiones ni cálculos de avance.
- **Reapertura excepcional**: Autorización motivada que permite pasar de cerrado a reabierto sin borrar el cierre previo ni cambiar la versión del modelo.
- **Evento de auditoría**: Registro inmutable de actor, fecha, proceso, acción, objeto, resultado, motivo cuando corresponda y correlación, sujeto al ámbito autorizado de consulta.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En el recorrido de aceptación se activa un proceso con el modelo v1, se asignan responsables, revisores y evaluadores, se publica el modelo v2 y el 100 % de las consultas posteriores del proceso continúa identificando exactamente el modelo v1 y sus indicadores.
- **SC-002**: El 100 % de las transiciones válidas de la matriz de estados se completa con el estado esperado y el 100 % de las transiciones no autorizadas se rechaza sin cambios parciales.
- **SC-003**: El 100 % de las asignaciones aceptadas en la matriz de prueba corresponde a una identidad habilitada, una de las tres funciones permitidas, un indicador de la versión fijada y una vigencia que se superpone al período; el 100 % de las combinaciones inválidas se rechaza.
- **SC-004**: El 100 % de los intentos de modificar procesos cerrados se rechaza hasta completar una reapertura autorizada y motivada; el 100 % de las reaperturas aceptadas conserva el cierre anterior y la misma versión del modelo.
- **SC-005**: El 100 % de las transiciones, reaperturas, cambios críticos de asignación y rechazos incluidos en la aceptación tiene un evento de auditoría correlacionable, ordenable e inmutable para usuarios ordinarios.
- **SC-006**: El 100 % de los conflictos concurrentes simulados sobre proceso, estado o asignación evita sobrescrituras silenciosas y permite revisar o reintentar la operación rechazada.
- **SC-007**: Al menos 19 de 20 coordinadores de prueba completan correctamente, en su primer intento y en menos de 5 minutos, la creación, activación y asignación inicial de un proceso sintético usando únicamente la interfaz y la ayuda disponible.
- **SC-008**: El 100 % de las reglas críticas de modelo fijado, asignaciones, ámbitos, vigencias, conflictos, estados, reapertura y concurrencia tiene al menos una prueba automatizada reproducible con datos sintéticos.

## Assumptions

- I0 proporciona identidades habilitadas, funciones, ámbitos, políticas de conflicto, excepciones y autorización; I2 consume esas decisiones sin duplicarlas ni redefinirlas.
- I1 proporciona versiones publicadas e inmutables de modelos e indicadores identificables; I2 no edita su contenido.
- La carrera piloto y todos los procesos, identidades, períodos y asignaciones utilizados en desarrollo, prueba y demostración son sintéticos.
- El coordinador puede cambiar la versión seleccionada mientras el proceso permanezca en borrador; la primera activación fija la referencia para el resto de la vida del proceso, incluidas sus reaperturas.
- Las fechas inicial y final del período son inclusivas y permiten procesos de un solo día. Una vigencia de asignación es válida cuando su inicio es anterior a su fin y existe al menos un instante de superposición con el período del proceso; fuera de esa intersección no concede autoridad.
- Las asignaciones se realizan por indicador. La coordinación general del proceso proviene del ámbito de I0 y no se infiere de una asignación de responsable, revisor o evaluador.
- La política institucional determina quién puede ordenar transiciones y autorizar reaperturas. Para el piloto se usan configuraciones sintéticas, no autoridades institucionales codificadas.
- I2 establece el orden y la autorización del ciclo de estados. Las condiciones académicas o documentales necesarias para cerrar se incorporan en incrementos posteriores y no pueden ser simuladas como decisiones reales en I2.
- El estado reabierto conserva la evidencia de que existió un cierre excepcionalmente revertido; antes de retomar el trabajo ordinario debe transitar de reabierto a activo.
- Un cambio crítico de asignación requiere motivo aunque no exista conflicto de funciones. Si además necesita una excepción de conflicto, se aplican las condiciones y vigencia de I0.
- La auditoría registra resultados y referencias suficientes para reconstruir el cambio, pero no duplica datos sensibles ni contenido de incrementos posteriores.
