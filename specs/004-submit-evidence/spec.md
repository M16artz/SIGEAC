# Feature Specification: I3 — Recopilación y remisión de evidencias

**Feature Branch**: `main` (no se creó una rama porque no existe un hook `before_specify`)

**Created**: 2026-10-20

**Status**: Draft

**Input**: User description: "Módulo I3 evidence — Recopilación y remisión de evidencias del Sistema de Evaluación de Carreras SEC-UNL. Un responsable de evidencia crea borradores con título, descripción, procedencia, período cubierto y uno o más indicadores dentro de su ámbito; adjunta archivos o referencias externas; y remite versiones inmutables. Incluye validación de archivos (tipos permitidos, máximo 25 MiB, tipo detectado y SHA-256), almacenamiento fuera de PostgreSQL mediante un puerto reemplazable, carga con staging y promoción atómica, corrección mediante versión sucesora sin sobrescritura, historial completo, descarga autorizada y auditoría de remisiones y fallos. Excluye revisión documental, valoración académica, cálculo de avance y cierre de proceso. Requisitos de referencia: FR-016 a FR-020; RNF-004, RNF-005, RNF-010, RNF-011, RNF-019. Resultado medible: remitir v1 con archivo sintético, impedir su modificación y crear v2 sucesora conservando todo el historial. Usar datos sintéticos y no codificar decisiones institucionales CL-001 a CL-010 como reglas fijas."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Preparar un borrador de evidencia (Priority: P1)

Como responsable de evidencia, quiero crear y completar un borrador vinculado a indicadores de mi ámbito para documentar la procedencia y el período de la información antes de remitirla.

**Why this priority**: El borrador reúne el contexto mínimo que da significado a los archivos o referencias y delimita qué indicadores sustenta la evidencia.

**Independent Test**: Con un proceso, indicadores y asignaciones sintéticos se crea un borrador, se modifican sus datos y se comprueba que solo admite indicadores del proceso y del ámbito vigente del responsable.

**Acceptance Scenarios**:

1. **Given** un responsable con asignación vigente sobre dos indicadores de un proceso, **When** crea un borrador con título, descripción, procedencia, período cubierto y ambos indicadores, **Then** el borrador conserva esos datos, identifica a su creador y permanece editable.
2. **Given** un indicador ajeno al proceso, fuera del ámbito o una asignación no vigente, **When** el responsable intenta asociarlo al borrador, **Then** la operación se rechaza sin incorporar el indicador ni revelar información ajena.
3. **Given** un borrador propio dentro de su ámbito, **When** el responsable modifica sus metadatos o indicadores antes de remitirlo, **Then** se conserva la última revisión confirmada sin pérdida silenciosa de cambios.

---

### User Story 2 - Adjuntar archivos o referencias seguras (Priority: P1)

Como responsable de evidencia, quiero adjuntar archivos o referencias externas y conocer cualquier problema de validación para remitir únicamente fuentes identificables e íntegras.

**Why this priority**: Una evidencia no puede remitirse sin al menos una fuente y los archivos deben ser seguros, recuperables y verificables.

**Independent Test**: Se adjunta a un borrador un archivo sintético permitido de hasta 25 MiB, se comprueban sus metadatos y huella, y se ensayan tamaño excesivo, tipo no permitido, discrepancia de tipo e interrupción durante la carga.

**Acceptance Scenarios**:

1. **Given** un borrador editable, **When** se carga un archivo sintético cuyo contenido corresponde a un tipo permitido y cuyo tamaño es menor o igual a 25 MiB, **Then** queda adjunto con nombre, tipo declarado, tipo detectado, tamaño y huella SHA-256 verificable.
2. **Given** un archivo mayor de 25 MiB, de tipo detectado no permitido o cuyo tipo declarado no coincide de forma aceptable con el detectado, **When** se intenta adjuntarlo, **Then** se rechaza con un mensaje seguro y el borrador no contiene una referencia utilizable al archivo.
3. **Given** una carga interrumpida o una promoción fallida, **When** se recupera la operación, **Then** el borrador continúa utilizable, no existe una referencia falsa a un archivo disponible y el responsable puede reintentar.
4. **Given** un borrador editable, **When** el responsable añade una referencia externa con localización válida y descripción identificable, **Then** la referencia queda asociada sin copiar ni presumir la disponibilidad permanente del contenido externo.

---

### User Story 3 - Remitir una versión inmutable y descargarla (Priority: P1)

Como responsable de evidencia, quiero remitir una instantánea completa y descargar sus archivos cuando estoy autorizado para que la versión entregada pueda identificarse y verificarse sin cambios posteriores.

**Why this priority**: La remisión es el resultado central de I3 y establece la unidad inmutable que consumirán incrementos posteriores sin realizar todavía una revisión documental.

**Independent Test**: Se remite una v1 con un archivo sintético válido, se rechazan todos los intentos de alterarla y una persona autorizada descarga exactamente el archivo asociado y verifica su huella.

**Acceptance Scenarios**:

1. **Given** un borrador con todos los metadatos obligatorios, al menos un indicador autorizado y al menos un archivo válido o referencia externa, **When** el responsable lo remite, **Then** se crea una versión numerada e inmutable con autor, fecha, contenido completo y resultado auditable.
2. **Given** un borrador incompleto, sin fuentes, con un archivo no disponible o cuya integridad no puede confirmarse, **When** se solicita la remisión, **Then** se rechaza sin crear una versión remitida parcial y se indican de forma segura los problemas corregibles.
3. **Given** una v1 remitida, **When** cualquier persona intenta cambiar sus metadatos, indicadores, archivos o referencias, **Then** la modificación se impide y v1 permanece idéntica.
4. **Given** una versión remitida y una persona autorizada para su proceso e indicadores, **When** descarga un archivo, **Then** recibe los mismos bytes cuya huella consta en la versión; una persona fuera de ámbito no obtiene el archivo ni sus metadatos protegidos.

---

### User Story 4 - Corregir mediante una versión sucesora (Priority: P1)

Como responsable de evidencia, quiero corregir una evidencia remitida creando una versión sucesora para conservar la entrega original y todo el historial de cambios.

**Why this priority**: La corrección sin sobrescritura protege la trazabilidad exigida y permite demostrar qué se remitió en cada momento.

**Independent Test**: A partir de v1 se crea un borrador sucesor, se corrige y se remite como v2; las consultas de v1, v2 y del historial conservan contenido, autoría, fechas y relación de sucesión.

**Acceptance Scenarios**:

1. **Given** una v1 remitida, **When** el responsable autorizado inicia una corrección, **Then** se crea un nuevo borrador relacionado con v1 sin desbloquear ni modificar v1.
2. **Given** el borrador sucesor completo, **When** se remite, **Then** se crea v2 inmutable, vinculada directamente con v1 y distinguible por número, autor y fecha.
3. **Given** una evidencia con v1 y v2 remitidas, **When** una persona autorizada consulta el historial, **Then** obtiene todas las versiones en orden inequívoco y puede consultar el contenido exacto y las fuentes de cada una.
4. **Given** dos solicitudes concurrentes basadas en la misma revisión del borrador sucesor, **When** ambas intentan modificarlo o remitirlo, **Then** solo la operación basada en la revisión vigente puede confirmarse y la otra recibe un conflicto recuperable.

---

### User Story 5 - Auditar remisiones y fallos (Priority: P2)

Como responsable autorizado de control, quiero consultar eventos de remisión y fallos para reconstruir qué ocurrió sin exponer archivos, secretos ni información fuera de mi ámbito.

**Why this priority**: La auditoría permite investigar remisiones, rechazos, problemas de almacenamiento y descargas fallidas sin mezclar esta función con la revisión documental.

**Independent Test**: Se producen una remisión correcta, validaciones rechazadas, una promoción fallida, un conflicto concurrente y una descarga no autorizada; la consulta muestra eventos correlacionables, ordenables e inmutables con detalles seguros.

**Acceptance Scenarios**:

1. **Given** una remisión aceptada o rechazada, una validación fallida, una falla de carga o promoción, un conflicto concurrente o una descarga fallida, **When** concluye el intento, **Then** queda un evento con actor, fecha, acción, objeto, resultado, motivo seguro y correlación.
2. **Given** una persona autorizada para consultar auditoría dentro de un ámbito, **When** filtra los eventos de una evidencia, **Then** puede reconstruir su secuencia sin acceder al contenido completo de archivos ni a información ajena.
3. **Given** un usuario ordinario, **When** intenta alterar o eliminar eventos de auditoría, **Then** la operación se impide.

### Edge Cases

- Un archivo de exactamente 25 MiB es aceptable si cumple las demás validaciones; uno de 25 MiB más un byte se rechaza.
- La extensión o el tipo declarado indican un tipo permitido, pero el contenido detectado indica otro: prevalece la validación del contenido y el archivo se rechaza si la combinación no está permitida por la política vigente.
- Dos archivos tienen la misma huella SHA-256: ambos pueden conservar su identidad y metadatos de asociación; la coincidencia no convierte automáticamente uno en sustituto del otro ni autoriza acceso cruzado.
- Un archivo vacío solo se acepta si su tipo detectado y la política configurable lo permiten; nunca se infiere validez únicamente por el nombre.
- Una referencia externa tiene sintaxis válida pero su destino puede estar inaccesible: I3 la conserva únicamente como referencia declarada, no realiza solicitudes salientes para comprobarla, no afirma que su contenido fue incorporado y no la confunde con un archivo almacenado.
- Se interrumpe la operación antes de promover el archivo preparado: no se crea una asociación disponible ni una versión remitida; el material temporal queda identificable para recuperación o limpieza segura.
- Se interrumpe la operación después de promover el archivo pero antes de confirmar sus metadatos: la recuperación reconcilia o retira el objeto no referenciado y nunca presenta una referencia falsa.
- Se intenta eliminar un archivo que pertenece a una versión remitida: la operación se impide y la versión continúa descargable mientras rija la política de conservación.
- La asignación del responsable vence mientras edita: las operaciones posteriores se vuelven a autorizar con el ámbito vigente; el borrador y la autoría histórica no se borran.
- Dos remisiones concurrentes parten del mismo borrador: como máximo una crea la siguiente versión y la otra se rechaza como conflicto recuperable.
- Falla el registro obligatorio de auditoría durante una remisión: la evidencia no se presenta como remitida y la operación queda recuperable.
- Un solicitante conoce el identificador de un archivo fuera de su ámbito: la descarga se rechaza sin confirmar existencia ni revelar metadatos.

## Requirements *(mandatory)*

### Scope Boundaries

**Included**:

- Creación y edición de borradores de evidencia por responsables autorizados.
- Asociación con uno o más indicadores del proceso dentro del ámbito vigente.
- Archivos cargados o referencias externas como fuentes de evidencia.
- Validación de tamaño, política de tipos, tipo detectado y huella SHA-256.
- Preparación temporal y promoción completa del archivo a almacenamiento definitivo, con recuperación ante interrupciones.
- Remisión de versiones inmutables, corrección por sucesión e historial completo.
- Consulta de versiones y descarga autorizada de archivos.
- Auditoría de remisiones y fallos de validación, almacenamiento, concurrencia y acceso.

**Excluded**:

- Revisión documental, observación, aceptación o rechazo académico/documental de una versión remitida.
- Valoración académica o inferencia de cumplimiento a partir de una evidencia.
- Cálculo de avance, tableros, informes, cierre o reapertura del proceso.
- Definición fija de tipos permitidos, plazos de conservación, perfiles institucionales de auditoría u otras decisiones CL-001 a CL-010.
- Integración con repositorios documentales, identidad, notificaciones o servicios institucionales externos.
- Eliminación definitiva o política institucional de expurgo de versiones remitidas y sus archivos.

### Functional Requirements

- **FR-016 — Borrador y ámbito**: Un responsable de evidencia DEBE poder crear y editar un borrador con título, descripción, procedencia, período cubierto y uno o más indicadores del proceso; cada creación, consulta o cambio DEBE autorizarse con la función y el ámbito vigentes, y NO DEBE aceptar indicadores ajenos al proceso o al ámbito del responsable.
- **FR-017 — Fuentes y almacenamiento desacoplado**: Un borrador DEBE admitir uno o más archivos o referencias externas. Por cada archivo, el sistema DEBE conservar nombre, tipo declarado, tipo detectado, tamaño, huella SHA-256 y localización, manteniendo el contenido binario fuera de PostgreSQL mediante una capacidad de almacenamiento reemplazable que preserve la misma semántica de preparación, promoción, lectura, verificación y recuperación. Las referencias externas DEBEN conservar localización y descripción sin presentarse como contenido almacenado.
- **FR-018 — Validación y remisión completa**: Antes de remitir, el sistema DEBE exigir título, descripción, procedencia, período cubierto válido, al menos un indicador autorizado del proceso y al menos un archivo disponible o referencia externa. Cada archivo DEBE cumplir la política configurable de tipos, medir como máximo 25 MiB, tener tipo detectado compatible y una huella SHA-256 calculada a partir del contenido; cualquier incumplimiento DEBE impedir la remisión completa.
- **FR-018a — Carga recuperable y promoción atómica**: Una carga DEBE permanecer en preparación hasta superar todas las validaciones y solo entonces promoverse como archivo disponible mediante una operación completa. Una interrupción o falla NO DEBE dejar una referencia que presente como disponible un archivo ausente, incompleto o no validado; el borrador DEBE quedar en condición consistente y permitir reintento, reconciliación o limpieza segura.
- **FR-019 — Versión remitida inmutable**: Remitir un borrador completo DEBE crear una versión numerada e inmutable que capture sus metadatos, indicadores, archivos, referencias, autor y fecha. Después de la remisión, ningún actor DEBE poder editar, sustituir ni retirar elementos de esa versión; el fallo de cualquier paso obligatorio, incluida la auditoría, NO DEBE producir una versión presentada como remitida.
- **FR-019a — Consulta y descarga autorizadas**: Una persona autorizada DEBE poder consultar una versión exacta y descargar sus archivos dentro de su ámbito. La descarga DEBE entregar el contenido correspondiente a la huella registrada; los intentos fuera de ámbito o con integridad no verificable DEBEN rechazarse sin revelar existencia, contenido ni metadatos protegidos.
- **FR-020 — Corrección por sucesión e historial**: Corregir una versión remitida DEBE crear un nuevo borrador sucesor y, al remitirlo, una nueva versión inmutable vinculada directamente con su predecesora. El sistema DEBE conservar y permitir la consulta autorizada de todas las versiones, fuentes, autores, fechas y relaciones de sucesión sin sobrescribir ni eliminar versiones anteriores.
- **FR-020a — Auditoría**: Las remisiones aceptadas o rechazadas y los fallos de validación, preparación, promoción, integridad, concurrencia y descarga DEBEN producir eventos inmutables con actor, fecha, acción, objeto afectado, resultado, motivo seguro e identificador de correlación; su consulta DEBE limitarse al ámbito autorizado y NO DEBE incluir contenido completo de archivos, secretos o datos ajenos.

### Applicable Quality Requirements

- **RNF-004 — Archivos**: Los tipos admitidos DEBEN provenir de una política configurable para el piloto. El límite inicial es de 25 MiB por archivo y siempre se DEBEN registrar nombre, tipo declarado, tipo detectado, tamaño y huella SHA-256; ni la extensión ni el tipo declarado bastan por sí solos para aceptar contenido.
- **RNF-005 — Privacidad**: Mensajes, eventos, registros y respuestas NO DEBEN incluir secretos, contenido completo de archivos ni datos fuera del ámbito autorizado. Un rechazo de consulta o descarga NO DEBE permitir inferir la existencia de una evidencia ajena.
- **RNF-010 — Recuperación**: Toda operación que combine metadatos y archivos DEBE concluir de forma consistente o quedar recuperable y reconciliable, sin referencias falsas. Los materiales preparados y los objetos promovidos sin referencia confirmada DEBEN poder identificarse para tratamiento seguro.
- **RNF-011 — Concurrencia**: Las modificaciones y remisiones concurrentes DEBEN detectar revisiones obsoletas, impedir versiones sucesoras duplicadas para una misma revisión y evitar pérdida silenciosa de cambios.
- **RNF-019 — Datos de prueba**: Desarrollo, prueba, demostración y validación DEBEN utilizar personas, procesos, indicadores, referencias y archivos sintéticos, libres de secretos y datos institucionales reales.

### Key Entities *(include if feature involves data)*

- **Evidencia**: Identidad estable que agrupa un borrador inicial y su secuencia completa de versiones remitidas y correcciones sucesoras dentro de un proceso.
- **Borrador de evidencia**: Contenido editable con título, descripción, procedencia, período cubierto, indicadores, fuentes, autor y revisión de concurrencia; todavía no constituye una remisión.
- **Versión de evidencia**: Instantánea numerada e inmutable de un borrador remitido, con autor, fecha y vínculo opcional a una única predecesora.
- **Indicador asociado**: Referencia a un indicador de la versión del modelo fijada al proceso y comprendido en el ámbito vigente del responsable al operar.
- **Archivo de evidencia**: Fuente binaria con nombre, tipo declarado, tipo detectado, tamaño, huella SHA-256, localización, disponibilidad y asociaciones a versiones, cuyo contenido reside fuera de PostgreSQL.
- **Objeto en preparación**: Archivo todavía no disponible como fuente definitiva mientras se carga, valida o espera promoción; permite distinguir una carga incompleta de un archivo confirmado.
- **Referencia externa**: Fuente declarada mediante localización y descripción; no implica copia local, disponibilidad permanente ni validación académica de su contenido.
- **Relación de sucesión**: Vínculo inmutable y ordenado entre una versión corregida y su predecesora directa, sin sobrescritura de ninguna de ellas.
- **Evento de auditoría**: Registro inmutable y correlacionable de una remisión o fallo, con detalles mínimos y seguros sujetos al ámbito autorizado.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En el recorrido de aceptación, un responsable remite v1 con un archivo sintético válido; el 100 % de los intentos de modificar v1 se rechaza, y la corrección se remite como v2 vinculada a v1 conservando el 100 % del contenido e historial de ambas versiones.
- **SC-002**: El 100 % de los archivos aceptados en la matriz de prueba cumple el tipo configurado, tamaño menor o igual a 25 MiB, compatibilidad con el tipo detectado y huella SHA-256 verificable; el 100 % de archivos que incumple al menos una condición se rechaza antes de la remisión.
- **SC-003**: En el 100 % de las interrupciones simuladas antes, durante y después de la promoción, ninguna consulta presenta como disponible un archivo ausente, incompleto o no validado, y cada operación puede reintentarse o reconciliarse sin perder el borrador confirmado.
- **SC-004**: El 100 % de las descargas autorizadas de la prueba reproduce la huella registrada; el 100 % de los intentos fuera de ámbito se rechaza sin entregar bytes ni metadatos protegidos.
- **SC-005**: El 100 % de las remisiones y fallos incluidos en la aceptación genera un evento inmutable, ordenable y correlacionable con actor, fecha, acción, objeto, resultado y motivo seguro, sin contenido completo de archivos ni secretos.
- **SC-006**: El 100 % de los conflictos concurrentes simulados sobre un borrador o su remisión evita sobrescrituras silenciosas y versiones sucesoras duplicadas, dejando a la persona afectada una opción clara de revisar y reintentar.
- **SC-007**: Al menos 19 de 20 responsables de prueba completan en su primer intento y en menos de 5 minutos la creación, adjunto y remisión de una evidencia sintética usando solo la interfaz y la ayuda disponible.
- **SC-008**: El 100 % de los escenarios de desarrollo, prueba, demostración y aceptación se ejecuta con datos y archivos sintéticos y sin secretos ni datos institucionales reales.

## Assumptions

- I0 proporciona identidades, funciones, ámbitos y autorización; I2 proporciona el proceso activo, su versión fijada del modelo, indicadores y asignaciones vigentes. I3 consume esas referencias sin redefinirlas.
- El período cubierto tiene fechas inicial y final inclusivas y es válido cuando la fecha inicial no es posterior a la final; no se presupone que deba coincidir por completo con el período del proceso.
- Los tipos permitidos, las combinaciones aceptables entre tipo declarado y detectado, la conservación y los perfiles de consulta de auditoría son configuraciones sintéticas del piloto. CL-001 a CL-010 continúan abiertas y ninguna configuración piloto constituye una decisión institucional.
- Una referencia externa se valida solo estructuralmente al registrarla; I3 no intenta acceder a su destino. Su disponibilidad o contenido no se garantiza ni equivale a una aceptación documental.
- La huella SHA-256 demuestra que los bytes descargados coinciden con los registrados; no demuestra autenticidad, calidad ni suficiencia académica del contenido.
- Una versión puede contener archivos, referencias externas o ambos, siempre que tenga al menos una fuente al remitirla.
- Las versiones remitidas y sus archivos se conservan durante la vida del proceso en el piloto; una política definitiva de retención o eliminación depende de CL-008 y queda fuera de este incremento.
- La consulta de auditoría requiere autorización explícita y ámbito compatible; los perfiles y plazos definitivos dependen de CL-009.
