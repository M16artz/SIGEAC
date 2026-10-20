# Feature Specification: I0 — Acceso y ámbitos autorizados

**Feature Branch**: `main` (no se creó una rama porque no existe un hook `before_specify`)

**Created**: 2026-10-19

**Status**: Draft

**Input**: User description: "Módulo I0 identityaccess — Acceso y ámbitos autorizados del Sistema de Evaluación de Carreras SEC-UNL. Un administrador funcional habilita usuarios con función de proceso y ámbito explícito (carrera, proceso e indicador) con vigencia, para que cada persona opere solo sobre lo que le corresponde. Incluye inicio y cierre de sesión, expiración por 30 minutos de inactividad, limitación tras cinco intentos fallidos en 15 minutos, separación estricta entre actores institucionales, funciones de proceso y privilegios técnicos, detección de conflictos de funciones con excepciones temporales justificadas y auditadas, y auditoría de accesos, cambios de permisos, asignaciones, rechazos por función o ámbito y conflictos. Excluye modelo de evaluación, procesos, evidencias, revisión documental y valoración académica. Requisitos de referencia: FR-001 a FR-006; RNF-001 a RNF-003, RNF-005, RNF-012, RNF-013, RNF-019. Resultado medible: 100% de operaciones dentro del ámbito permitidas y 100% de intentos fuera de ámbito rechazados sin revelar datos, con auditoría registrada. Usar datos sintéticos y no codificar decisiones institucionales CL-001 a CL-010 como reglas fijas."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Acceder mediante una sesión segura (Priority: P1)

Como usuario habilitado, quiero iniciar y cerrar sesión de forma segura para realizar únicamente las operaciones protegidas que me corresponden y terminar mi acceso cuando deje de usar el sistema.

**Why this priority**: La autenticación y la vigencia de la sesión son la puerta de entrada para cualquier otra capacidad protegida del sistema.

**Independent Test**: Con una identidad sintética activa se inicia sesión, se consulta la identidad vigente, se cierra sesión y se comprueba que la sesión cerrada o inactiva durante 30 minutos ya no permite nuevas operaciones.

**Acceptance Scenarios**:

1. **Given** una identidad sintética activa y credenciales válidas, **When** la persona inicia sesión, **Then** obtiene una sesión activa y puede consultar su identidad, funciones y ámbitos vigentes.
2. **Given** una sesión activa, **When** la persona cierra sesión, **Then** la sesión queda invalidada y cualquier uso posterior exige autenticarse nuevamente.
3. **Given** una sesión sin actividad durante 30 minutos, **When** se intenta una nueva operación protegida, **Then** la operación se rechaza sin exponer información protegida y se exige iniciar una nueva sesión.
4. **Given** cinco intentos fallidos consecutivos para una misma identidad aparente dentro de una ventana móvil de 15 minutos, **When** se intenta autenticar nuevamente mientras el umbral sigue vigente, **Then** el acceso queda limitado temporalmente y la respuesta no confirma si la identidad existe.

---

### User Story 2 - Habilitar usuarios y asignar responsabilidades (Priority: P1)

Como administrador funcional autorizado, quiero habilitar o desactivar identidades y otorgar funciones de proceso con ámbitos y vigencias explícitos para que cada persona pueda operar solamente sobre las responsabilidades asignadas.

**Why this priority**: Sin asignaciones explícitas no puede demostrarse quién está autorizado ni durante qué período.

**Independent Test**: Con datos sintéticos se habilita una identidad, se le asigna una función para una carrera, un proceso y un conjunto de indicadores durante una vigencia definida, se consulta la asignación y luego se desactiva la identidad conservando su historial.

**Acceptance Scenarios**:

1. **Given** un administrador funcional autorizado y una identidad sintética, **When** registra una asignación completa con función, ámbito y vigencia, **Then** la asignación queda disponible durante su vigencia y se conserva quién la otorgó.
2. **Given** una asignación futura o vencida, **When** la persona intenta usarla, **Then** la operación se rechaza como no autorizada sin revelar datos del objeto solicitado.
3. **Given** una identidad con historial de actividad, **When** el administrador funcional la desactiva, **Then** sus accesos posteriores quedan invalidados y su identidad histórica no se elimina ni reasigna a otra persona.
4. **Given** un administrador funcional sin autoridad sobre el ámbito solicitado, **When** intenta crear, cambiar o revocar una asignación, **Then** la operación se rechaza y queda auditada.

---

### User Story 3 - Operar únicamente dentro del ámbito (Priority: P1)

Como persona con una función vigente, quiero que el sistema permita las operaciones incluidas en mi ámbito y rechace de forma segura cualquier otra, para proteger la información y las competencias de cada participante.

**Why this priority**: La autorización por función y ámbito es el resultado central y medible del incremento I0.

**Independent Test**: Una matriz sintética de identidades, funciones, carreras, procesos, indicadores y operaciones ejecuta todos los casos permitidos y denegados; cada resultado se compara con la asignación vigente y con su evento de auditoría.

**Acceptance Scenarios**:

1. **Given** una función vigente que permite una operación sobre una carrera, proceso e indicador determinados, **When** la persona solicita esa operación sobre el objeto incluido, **Then** la operación se permite.
2. **Given** una función vigente para un indicador, **When** la persona solicita la misma operación sobre otro indicador, proceso o carrera, **Then** la operación se rechaza sin confirmar la existencia ni devolver atributos del objeto ajeno.
3. **Given** que una asignación se revoca o vence durante una sesión activa, **When** se solicita una nueva operación que dependía de ella, **Then** la autorización se reevalúa, se rechaza la operación y no se aplican cambios parciales.
4. **Given** una consulta que devuelve una colección o un resumen, **When** la persona la ejecuta, **Then** el resultado contiene exclusivamente objetos y valores agregados comprendidos en sus ámbitos vigentes.

---

### User Story 4 - Gestionar conflictos y excepciones temporales (Priority: P2)

Como administrador funcional autorizado, quiero conocer los conflictos antes de confirmar una asignación y, cuando la política vigente lo permita, registrar una excepción temporal justificada para resolver una necesidad operativa sin ocultar el riesgo.

**Why this priority**: La separación de funciones protege la independencia de las decisiones, mientras que una excepción controlada permite atender situaciones justificadas sin eliminar la trazabilidad.

**Independent Test**: Se configura una política sintética de conflictos, se intenta una combinación incompatible, se verifica su rechazo y luego se registra una excepción autorizada con motivo, alcance y vigencia; al vencer, la combinación vuelve a quedar impedida.

**Acceptance Scenarios**:

1. **Given** una política sintética vigente que declara incompatibles dos funciones, **When** se intenta guardar la segunda asignación sin excepción válida, **Then** la asignación se rechaza y se identifica el conflicto sin exponer información ajena al administrador.
2. **Given** un conflicto detectado y una autoridad habilitada por la política vigente, **When** registra una excepción con responsable, justificación, alcance, inicio y fin, **Then** la excepción solo habilita la combinación durante ese período y ámbito.
3. **Given** una excepción vencida o revocada, **When** se intenta una operación que dependía de ella, **Then** la operación se rechaza aunque la sesión haya comenzado mientras la excepción estaba activa.
4. **Given** una cuenta con privilegios técnicos, **When** intenta adoptar una decisión reservada a una función de proceso, **Then** la operación se rechaza salvo que una asignación funcional independiente, vigente y no conflictiva la autorice expresamente.

---

### User Story 5 - Consultar la trazabilidad de acceso (Priority: P3)

Como responsable autorizado de control, quiero consultar los eventos de acceso y autorización dentro de mi ámbito para reconstruir qué ocurrió sin acceder a secretos ni datos fuera de mi competencia.

**Why this priority**: La auditoría permite demostrar el cumplimiento, investigar rechazos y revisar excepciones sin modificar el historial.

**Independent Test**: Se ejecuta una secuencia sintética de inicio y cierre de sesión, cambio de permisos, asignación, conflicto y rechazo; una persona autorizada consulta los eventos dentro de su ámbito y no puede alterarlos ni ver eventos ajenos.

**Acceptance Scenarios**:

1. **Given** eventos auditables dentro del ámbito de consulta, **When** el responsable autorizado solicita el historial, **Then** puede ordenarlo inequívocamente y reconocer actor, fecha, acción, objeto, resultado y correlación.
2. **Given** eventos fuera de su ámbito, **When** el responsable intenta consultarlos directa o indirectamente, **Then** no recibe los eventos, conteos ni indicios que permitan inferir su existencia.
3. **Given** un usuario ordinario, **When** intenta modificar o eliminar un evento de auditoría, **Then** la operación se impide.

### Edge Cases

- Dos asignaciones se superponen parcialmente en vigencia o ámbito: los permisos efectivos son la unión de autorizaciones vigentes, siempre que ninguna combinación infrinja la política de conflictos aplicable.
- Una asignación, una excepción o una identidad se desactiva durante una sesión activa: la siguiente operación protegida reevalúa la autorización y no conserva permisos obsoletos.
- El quinto intento fallido ocurre exactamente en el límite de 15 minutos: la ventana móvil incluye los intentos cuya antigüedad no excede 15 minutos al evaluar el nuevo intento.
- Una autenticación correcta fuera de una ventana ya vencida reinicia la secuencia de fallos consecutivos; los fallos históricos permanecen auditados.
- Un identificador de usuario inexistente y uno existente reciben respuestas externas equivalentes ante fallos o limitación para evitar enumeración de identidades.
- Una solicitud referencia una carrera, proceso o indicador inexistente o fuera de ámbito: la respuesta protegida no distingue entre inexistencia y falta de autorización.
- Una excepción comienza en el futuro, vence o se revoca: no concede permisos fuera de su vigencia efectiva y nunca modifica la política de conflictos base.
- Una misma persona posee una calidad institucional, una función de proceso y un privilegio técnico: cada dimensión se evalúa por separado y ninguna implica automáticamente otra.
- Falla el registro de un evento obligatorio de auditoría: la operación sensible asociada no se presenta como completada y el fallo conserva un identificador de correlación seguro.
- Solicitudes concurrentes intentan modificar la misma asignación: no se pierde silenciosamente un cambio y el resultado efectivo queda inequívocamente auditado.

## Requirements *(mandatory)*

### Scope Boundaries

**Included**:

- Identidades humanas y cuentas técnicas; habilitación y desactivación sin pérdida del historial.
- Inicio, consulta y cierre de sesión; expiración por inactividad y limitación de intentos fallidos.
- Funciones de proceso, ámbitos por carrera, proceso e indicador, vigencias y autorización de operaciones.
- Políticas configurables de conflicto y excepciones temporales justificadas.
- Auditoría de accesos, cambios de permisos, asignaciones, excepciones, conflictos y rechazos por función o ámbito.

**Excluded**:

- Definición o versionado de modelos de evaluación.
- Creación, activación, cierre o reapertura de procesos de autoevaluación.
- Registro, carga, remisión o versionado de evidencias.
- Revisión documental y valoración académica.
- Integración productiva con directorios institucionales, inicio de sesión único, notificaciones u otros servicios externos.
- Definición definitiva de catálogos, competencias, incompatibilidades, autoridades o plazos sujetos a CL-001–CL-010.

### Functional Requirements

- **FR-001 — Autenticación y sesión**: El sistema DEBE autenticar a toda persona o cuenta técnica antes de una operación protegida, permitir inicio y cierre de sesión, mostrar a la identidad autenticada sus funciones y ámbitos vigentes, invalidar el acceso al cerrar sesión o desactivar la identidad y expirar la sesión tras 30 minutos continuos de inactividad.
- **FR-002 — Autorización por función y ámbito**: El sistema DEBE autorizar cada operación protegida mediante una función vigente y, cuando corresponda, un ámbito explícito de carrera, proceso e indicador; la autorización DEBE reevaluarse en cada operación, abarcar lecturas, búsquedas, agregados y modificaciones, y rechazar accesos ajenos sin revelar existencia, atributos ni conteos de datos no autorizados.
- **FR-003 — Separación de dimensiones de acceso**: El sistema DEBE representar y evaluar por separado la identidad o calidad del actor institucional, las funciones temporales que desempeña en el proceso y los privilegios técnicos de operación; ninguna dimensión DEBE conceder automáticamente competencias de otra y una cuenta técnica NO DEBE adoptar decisiones reservadas a funciones de proceso por el solo hecho de poseer privilegios técnicos.
- **FR-004 — Conflictos y excepciones**: El sistema DEBE evaluar toda alta o modificación de asignación contra una política de conflictos configurable y vigente, rechazar combinaciones incompatibles sin excepción válida y permitir únicamente excepciones expresamente autorizadas que registren responsable, justificación, ámbito, inicio y fin; una excepción vencida, revocada o fuera de ámbito NO DEBE conceder permisos ni alterar la política base.
- **FR-005 — Ciclo de vida y asignaciones**: Un administrador funcional autorizado DEBE poder habilitar y desactivar identidades, y crear, consultar, modificar o revocar asignaciones con función, ámbito y vigencia explícitos, sin borrar la identidad histórica ni reutilizarla ambiguamente; cada cambio DEBE surtir efecto para las operaciones posteriores y respetar el ámbito administrativo de quien lo realiza.
- **FR-006 — Auditoría**: El sistema DEBE registrar de forma inmutable y consultable por ámbito el actor, fecha inequívoca, acción, objeto, resultado e identificador de correlación de inicios y cierres de sesión, autenticaciones fallidas o limitadas, cambios de permisos, asignaciones, excepciones, rechazos por función o ámbito y conflictos; los eventos NO DEBEN contener contraseñas, secretos ni datos fuera del ámbito autorizado.
- **FR-006a — Cobertura del resultado sensible**: Una operación de acceso o administración sujeta a auditoría obligatoria NO DEBE presentarse como completada si no puede conservarse su evento de auditoría correspondiente.

### Applicable Quality Requirements

- **RNF-001 — Protección de credenciales**: Las contraseñas DEBEN almacenarse mediante un mecanismo adaptativo con sal única; nunca en texto claro ni mediante cifrado reversible, y nunca deben aparecer en respuestas o auditorías.
- **RNF-002 — Vigencia de sesión**: Las sesiones DEBEN expirar tras 30 minutos de inactividad; cerrar sesión o desactivar una identidad DEBE impedir todo uso posterior de las sesiones afectadas.
- **RNF-003 — Limitación de intentos**: Cinco intentos fallidos consecutivos dentro de una ventana móvil de 15 minutos DEBEN limitar temporalmente nuevos intentos asociados, sin confirmar si la identidad existe; al dejar de cumplirse el umbral puede realizarse un nuevo intento, sin eliminar el historial auditable.
- **RNF-005 — Privacidad**: Mensajes, listados, agregados, registros y exportaciones del módulo NO DEBEN exponer contraseñas, secretos ni información fuera del ámbito autorizado.
- **RNF-012 — Auditoría**: Los eventos auditables DEBEN ser inmutables para usuarios ordinarios, consultables exclusivamente dentro del ámbito autorizado y ordenables inequívocamente por fecha y correlación.
- **RNF-013 — Observabilidad segura**: Cada solicitud DEBE tener un identificador de correlación; los fallos críticos DEBEN conservar contexto suficiente para su investigación sin divulgar información sensible.
- **RNF-019 — Datos de prueba**: Desarrollo, prueba, demostración y validación del módulo DEBEN usar identidades, carreras, procesos, indicadores, políticas y eventos sintéticos libres de secretos y datos institucionales reales.

### Key Entities *(include if feature involves data)*

- **Identidad de acceso**: Representa a una persona o cuenta técnica mediante un identificador estable, estado activo o inactivo y referencias históricas; no equivale por sí sola a una función de proceso.
- **Credencial**: Medio confidencial asociado a una identidad para comprobar su autenticidad, con estado y vigencia propios sin conservar el secreto de forma recuperable.
- **Sesión**: Acceso autenticado con inicio, última actividad, expiración e invalidación; referencia una identidad pero no congela sus permisos.
- **Actor institucional**: Calidad o relación institucional de una persona, separada de las funciones temporales del proceso y de los privilegios técnicos.
- **Función de proceso**: Conjunto configurable de operaciones de negocio que puede asignarse temporalmente; su denominación y competencias definitivas dependen de validación institucional.
- **Privilegio técnico**: Capacidad operativa o de infraestructura que no concede por sí sola autoridad funcional, documental ni académica.
- **Ámbito autorizado**: Delimita una asignación por carrera, proceso e indicador, admitiendo únicamente las dimensiones aplicables y sin otorgar acceso implícito a objetos ajenos.
- **Asignación funcional**: Vincula identidad, función, ámbito, vigencia, otorgante y estado; puede revocarse sin eliminar su historial.
- **Política de conflictos**: Configuración versionada o fechada que identifica combinaciones incompatibles y las condiciones para evaluar una asignación, sin asumir que sus valores piloto sean normativa institucional.
- **Excepción de conflicto**: Autorización temporal y acotada con responsable, justificación, ámbito y vigencia; complementa una asignación sin modificar la política base.
- **Evento de auditoría**: Registro inmutable de actor, fecha, acción, objeto, resultado y correlación, sujeto a autorización de consulta por ámbito.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En una matriz de aceptación que cubra cada combinación representativa de función, operación, carrera, proceso, indicador y vigencia, el 100 % de las operaciones autorizadas se completa y el 100 % de los intentos fuera de ámbito se rechaza sin revelar datos del objeto.
- **SC-002**: El 100 % de los inicios y cierres de sesión, autenticaciones fallidas o limitadas, cambios de permisos, asignaciones, excepciones, conflictos y rechazos por función o ámbito incluidos en las pruebas de aceptación tiene un evento de auditoría correlacionable y sin información sensible.
- **SC-003**: El 100 % de las sesiones probadas queda inutilizable después de 30 minutos continuos de inactividad, después de su cierre explícito o después de la desactivación de la identidad correspondiente.
- **SC-004**: El 100 % de las secuencias sintéticas con cinco intentos fallidos consecutivos dentro de 15 minutos activa la limitación, y las respuestas no permiten distinguir entre identidades existentes e inexistentes.
- **SC-005**: El 100 % de las asignaciones conflictivas sin excepción vigente se rechaza; el 100 % de las excepciones aceptadas contiene responsable, justificación, ámbito, inicio y fin, y deja de conceder permisos al vencer o revocarse.
- **SC-006**: Ninguna cuenta con solo privilegios técnicos logra ejecutar, en la suite de aceptación, una operación reservada a una función de proceso.
- **SC-007**: Al menos 19 de 20 administradores funcionales de prueba completan correctamente, en su primer intento y en menos de 3 minutos, la habilitación y asignación de una identidad sintética con función, ámbito y vigencia, usando únicamente la interfaz y la ayuda disponible.
- **SC-008**: El 100 % de los datos utilizados en pruebas, demostraciones y resultados de aceptación del módulo supera la verificación de contenido sintético y ausencia de secretos o datos institucionales reales.

## Assumptions

- El módulo recibe identificadores sintéticos de carrera, proceso e indicador para delimitar ámbitos, pero no administra el ciclo de vida ni el contenido de esos objetos.
- Las asignaciones y autorizaciones se evalúan usando una referencia temporal inequívoca acordada para el sistema; los límites de vigencia son inclusivos al inicio y exclusivos al final.
- La ventana de cinco intentos fallidos es móvil. La limitación se mantiene mientras los cinco fallos consecutivos permanezcan dentro de los 15 minutos; cualquier duración o desbloqueo adicional deberá configurarse y validarse institucionalmente antes de producción.
- Una autenticación correcta reinicia el conteo de fallos consecutivos para intentos futuros, sin borrar los eventos históricos.
- Pueden existir sesiones simultáneas para una identidad mientras estén vigentes; el cierre invalida la sesión seleccionada y la desactivación de la identidad invalida todas. Una política institucional distinta deberá configurarse, no codificarse como regla fija.
- Los catálogos piloto de actores, funciones, competencias, conflictos, autoridades y excepciones son configuraciones sintéticas. CL-001–CL-010 permanecen abiertos y sus valores no se interpretan como decisiones oficiales.
- La consulta de auditoría requiere una autorización explícita y se filtra por ámbito; los perfiles definitivos de consulta y los plazos de conservación dependen de CL-009.
- No se requiere identidad institucional, directorio externo ni inicio de sesión único para validar I0; una integración futura no cambia los resultados funcionales definidos aquí.
- La aceptación del incremento depende de una matriz de permisos reproducible y de fixtures completamente sintéticos.
