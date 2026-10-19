# Especificación de funcionalidad: Sistema de Evaluación de Carreras

**Directorio de funcionalidad**: `001-career-evaluation-platform`

**Creada**: 2026-10-18

**Estado**: Lista para planificación

**Entrada**: Definir el Sistema de Evaluación de Carreras SEC-UNL en incrementos verticales I0 a I8 para apoyar la autoevaluación institucional, desde acceso y modelos versionados hasta recopilación de evidencias, revisión documental, valoración académica, seguimiento, reportes y extensiones futuras, con trazabilidad, autorización por ámbito, auditoría e inmutabilidad.

## Contexto y alcance

El sistema apoya a una carrera piloto en la preparación, revisión y valoración interna de una autoevaluación. Mantiene separados tres actos: recopilar información, aceptar o rechazar documentalmente una evidencia y valorar académicamente el cumplimiento de un indicador. El producto no acredita carreras, no sustituye a comisiones oficiales y no presupone acceso de organismos externos.

I0–I6 constituyen el MVP. I7–I8 definen contratos de extensión y recorridos posteriores; no son dependencias para operar ni aceptar el MVP. La validación se realizará con una carrera, un modelo versionado, un proceso de autoevaluación y datos sintéticos.

### Actores

- **Administrador técnico**: gestiona cuentas y configuración operativa sin poder alterar decisiones académicas.
- **Administrador funcional**: administra carreras, modelos, procesos, funciones y asignaciones autorizadas.
- **Responsable de evidencia**: recopila información y remite versiones de evidencia en los indicadores asignados.
- **Revisor documental**: determina si una versión remitida es documentalmente aceptable, observable o rechazada, sin valorar cumplimiento académico.
- **Evaluador académico**: registra una valoración de cumplimiento sobre evidencia documental aceptada dentro de su ámbito.
- **Coordinador de autoevaluación**: supervisa avance, atiende excepciones autorizadas y cierra el proceso.
- **Consultor autorizado**: consulta resultados y trazabilidad dentro del ámbito otorgado, sin modificar decisiones.
- **Cuenta técnica**: ejecuta tareas operativas explícitas y nunca adopta decisiones documentales o académicas.

## Escenarios de usuario y pruebas

### Historia de usuario 1 — I0: Acceso y ámbitos autorizados (Prioridad: P0)

Como administrador funcional quiero habilitar usuarios con funciones y ámbitos explícitos para que cada persona solo opere sobre la carrera, proceso e indicadores que le corresponden.

**Prueba independiente**: con datos sintéticos se crean usuarios y asignaciones; cada actor accede únicamente a las operaciones y objetos incluidos en su ámbito, y los intentos fuera de ámbito quedan rechazados y auditados.

**Escenarios de aceptación**:

1. **Dado** un usuario activo con función y ámbito vigentes, **cuando** inicia sesión, **entonces** ve su identidad, funciones y procesos autorizados.
2. **Dado** un usuario sin ámbito para una carrera, **cuando** intenta consultar o modificar datos de esa carrera, **entonces** la operación se rechaza sin revelar datos y se registra el intento.
3. **Dado** un conflicto de funciones no autorizado, **cuando** se intenta guardar la asignación, **entonces** el sistema la rechaza y explica el conflicto.

---

### Historia de usuario 2 — I1: Modelo de evaluación versionado (Prioridad: P0)

Como administrador funcional quiero registrar y publicar una versión inmutable del modelo de evaluación para que un proceso utilice criterios, estándares e indicadores identificables y trazables.

**Prueba independiente**: se crea un modelo borrador con su jerarquía, se valida, se publica y se comprueba que la versión publicada no puede modificarse; una corrección genera otra versión.

**Escenarios de aceptación**:

1. **Dado** un modelo borrador completo, **cuando** se publica, **entonces** queda identificado con versión, fecha, autor y estado inmutable.
2. **Dado** un modelo publicado, **cuando** se solicita corregirlo, **entonces** se crea un nuevo borrador relacionado sin alterar la versión publicada.
3. **Dado** un modelo incompleto o con identificadores duplicados, **cuando** se solicita publicarlo, **entonces** la publicación se rechaza con los problemas encontrados.

---

### Historia de usuario 3 — I2: Proceso y asignaciones (Prioridad: P0)

Como coordinador quiero abrir un proceso para una carrera usando una versión concreta del modelo y asignar responsables por indicador para organizar la autoevaluación sin perder el contexto de las decisiones.

**Prueba independiente**: se crea un proceso con período y modelo publicado, se asignan responsables y revisores y se activa; el proceso conserva la versión elegida aunque exista otra posterior.

**Escenarios de aceptación**:

1. **Dado** una carrera piloto y un modelo publicado, **cuando** el coordinador activa el proceso, **entonces** el proceso conserva una referencia inmutable a esa versión.
2. **Dado** un indicador del proceso, **cuando** se asigna un responsable dentro del ámbito permitido, **entonces** la persona puede trabajar únicamente en ese indicador.
3. **Dado** un proceso cerrado, **cuando** se intenta cambiar su modelo o asignaciones, **entonces** el sistema rechaza el cambio salvo reapertura excepcional autorizada y auditada.

---

### Historia de usuario 4 — I3: Recopilación y remisión de evidencias (Prioridad: P0)

Como responsable de evidencia quiero registrar archivos o referencias, describir su procedencia y remitir una versión completa para revisión documental.

**Prueba independiente**: un responsable crea un borrador, adjunta un archivo sintético, remite la versión y luego corrige una observación creando una nueva versión sin modificar la anterior.

**Escenarios de aceptación**:

1. **Dado** un indicador asignado, **cuando** el responsable guarda una evidencia con procedencia y período, **entonces** queda como borrador editable y trazable.
2. **Dado** una evidencia completa, **cuando** se remite, **entonces** su versión y archivo quedan inmutables a la espera de revisión.
3. **Dado** una evidencia observada o rechazada, **cuando** el responsable la corrige, **entonces** se crea una versión sucesora y se conserva todo el historial.

---

### Historia de usuario 5 — I4: Revisión documental (Prioridad: P0)

Como revisor documental quiero aceptar, observar o rechazar una versión remitida con una justificación para decidir su suficiencia documental sin emitir una valoración académica.

**Prueba independiente**: el revisor decide sobre una versión remitida; la decisión registra actor, fecha y motivo, no cambia la evidencia y no marca el indicador como cumplido.

**Escenarios de aceptación**:

1. **Dado** una versión remitida dentro del ámbito del revisor, **cuando** la acepta documentalmente, **entonces** queda habilitada para valoración académica sin asignar nivel de cumplimiento.
2. **Dado** una versión incompleta, **cuando** el revisor la observa, **entonces** registra una justificación y el responsable puede preparar una versión sucesora.
3. **Dado** una decisión ya emitida, **cuando** se requiere rectificarla, **entonces** se conserva la decisión anterior y se registra una nueva decisión autorizada.

---

### Historia de usuario 6 — I5: Valoración académica (Prioridad: P0)

Como evaluador académico quiero valorar un indicador usando evidencia documental aceptada y una escala vigente para sustentar el resultado sin modificar la revisión documental.

**Prueba independiente**: el evaluador selecciona evidencia aceptada, aplica una opción de la escala configurada, justifica la valoración y el sistema conserva todas las referencias utilizadas.

**Escenarios de aceptación**:

1. **Dado** un indicador con evidencia aceptada, **cuando** el evaluador registra nivel y justificación, **entonces** la valoración referencia exactamente esas versiones de evidencia.
2. **Dado** una evidencia no aceptada, **cuando** se intenta usarla como sustento, **entonces** el sistema impide finalizar la valoración.
3. **Dado** una valoración finalizada, **cuando** se autoriza una rectificación, **entonces** se crea una nueva revisión de la valoración y se conserva la anterior.

---

### Historia de usuario 7 — I6: Seguimiento, cierre y reporte (Prioridad: P0)

Como coordinador quiero consultar el avance trazable, detectar pendientes y cerrar el proceso para obtener un informe interno reproducible de la autoevaluación.

**Prueba independiente**: con un proceso sintético se visualiza avance por estado, se navega desde cada resultado hasta sus evidencias y decisiones, se bloquea un cierre incompleto y se genera un informe al completar los requisitos.

**Escenarios de aceptación**:

1. **Dado** un proceso activo, **cuando** el coordinador consulta el tablero, **entonces** ve conteos y porcentajes cuya fórmula y fecha de cálculo son explícitas.
2. **Dado** un proceso con indicadores pendientes, **cuando** se intenta cerrarlo, **entonces** el sistema identifica los bloqueos y no lo cierra.
3. **Dado** un proceso completo, **cuando** se cierra, **entonces** los resultados quedan congelados y el informe permite rastrear cada valoración hasta el modelo, asignaciones, decisiones y evidencias correspondientes.

---

### Historia de usuario 8 — I7: Interoperabilidad futura (Prioridad: P1, fuera del MVP)

Como administrador funcional quiero disponer de contratos versionados para importar o exportar catálogos y resultados autorizados sin acoplar el MVP a servicios institucionales todavía no confirmados.

**Prueba independiente**: mediante datos sintéticos se valida un paquete de intercambio versionado, se rechazan registros inválidos y el MVP continúa funcionando cuando no existe integración.

**Escenarios de aceptación**:

1. **Dado** un paquete compatible, **cuando** se valida, **entonces** el sistema informa contenido, versión y conflictos antes de cualquier incorporación.
2. **Dado** un servicio externo no disponible, **cuando** opera el MVP, **entonces** ningún recorrido I0–I6 queda bloqueado.

---

### Historia de usuario 9 — I8: Notificaciones y acceso complementario (Prioridad: P2, fuera del MVP)

Como participante quiero recibir recordatorios y consultar tareas desde canales futuros sin trasladar decisiones ni permisos fuera de la autoridad central.

**Prueba independiente**: se genera un evento sintético y un canal desacoplado consume solo la información permitida; una falla del canal no cambia el proceso ni impide usar la aplicación principal.

**Escenarios de aceptación**:

1. **Dado** un cambio relevante, **cuando** se produce un evento de notificación, **entonces** el contenido excluye datos no autorizados y conserva vínculo al registro fuente.
2. **Dado** un canal complementario indisponible, **cuando** continúa el trabajo, **entonces** el proceso principal mantiene su estado y disponibilidad.

### Casos límite y manejo de errores

- Un usuario que pierde su asignación durante una sesión no puede completar nuevas operaciones fuera de ámbito; la autorización se reevalúa en cada operación protegida.
- Dos usuarios que actualizan el mismo borrador reciben detección de conflicto; no se sobrescriben cambios silenciosamente.
- Un archivo con tipo, tamaño, nombre o integridad inválidos se rechaza antes de remitir evidencia y no queda disponible como evidencia válida.
- La falla del almacenamiento de archivos no deja metadatos que aparenten una carga completa; la operación queda revertida o marcada como fallida y recuperable.
- Una versión de evidencia remitida conserva su archivo incluso si existe una versión sucesora.
- La eliminación lógica de una cuenta no borra su autoría histórica ni permite volver a usar ambiguamente su identidad.
- Los cambios de zona horaria no alteran el orden de eventos; las fechas auditables conservan una referencia temporal inequívoca.
- Una valoración no puede finalizar si alguna evidencia referenciada perdió validez documental mediante una decisión posterior autorizada.
- El cierre concurrente con una nueva observación se resuelve de forma consistente: prevalece la validación de prerrequisitos al momento de confirmar el cierre.
- Las fallas de exportación o notificación no modifican resultados académicos ni estados del proceso.

## Requisitos

### Requisitos funcionales

- **FR-001 [I0]**: El sistema DEBE autenticar a toda persona o cuenta técnica antes de una operación protegida.
- **FR-002 [I0]**: El sistema DEBE autorizar cada operación en el servidor por función y, cuando corresponda, por carrera, proceso e indicador.
- **FR-003 [I0]**: El sistema DEBE mantener separados los actores institucionales, las funciones del proceso y los privilegios técnicos.
- **FR-004 [I0]**: El sistema DEBE impedir conflictos de funciones según una política configurable y registrar toda excepción con responsable, motivo, alcance y vigencia.
- **FR-005 [I0]**: El sistema DEBE permitir altas, desactivaciones y asignaciones sin borrar la identidad histórica de los actores.
- **FR-006 [I0]**: El sistema DEBE auditar con actor, fecha, acción, objeto y resultado: inicios y cierres de sesión, intentos de autenticación fallidos o limitados, cambios de permisos, asignaciones y excepciones, rechazos por función o ámbito, conflictos de funciones, transiciones de estado inválidas y fallos al publicar, remitir, decidir, cerrar, descargar o exportar.
- **FR-007 [I1]**: El sistema DEBE representar el modelo mediante una jerarquía versionada de criterios, estándares e indicadores con identificadores únicos dentro de la versión.
- **FR-008 [I1]**: Antes de publicar, el sistema DEBE exigir fuente, autor, aprobador distinto del autor, al menos un criterio, al menos un estándar por criterio, al menos un indicador por estándar, códigos únicos por tipo dentro de la versión, una escala con al menos una opción y una fórmula de avance válida.
- **FR-009 [I1]**: Una versión publicada del modelo DEBE ser inmutable; toda corrección DEBE crear una nueva versión relacionada.
- **FR-010 [I1]**: El sistema DEBE auditar creación, publicación, sustitución y consulta de versiones del modelo.
- **FR-011 [I2]**: El sistema DEBE crear un proceso para una carrera, período y versión publicada del modelo.
- **FR-012 [I2]**: La versión del modelo vinculada a un proceso activo o cerrado DEBE permanecer inmutable e identificable.
- **FR-013 [I2]**: El sistema DEBE asignar responsables, revisores y evaluadores por ámbitos explícitos y vigencias definidas.
- **FR-014 [I2]**: El sistema DEBE administrar los estados borrador, activo, en cierre, cerrado y reabierto del proceso mediante transiciones autorizadas.
- **FR-015 [I2]**: Toda reapertura o cambio crítico de asignación DEBE quedar auditado con motivo y responsable.
- **FR-016 [I3]**: Un responsable DEBE poder crear evidencia en borrador con título, descripción, procedencia, período cubierto y uno o más indicadores relacionados dentro de su ámbito.
- **FR-017 [I3]**: El sistema DEBE admitir evidencia basada en archivo o referencia externa y conservar metadatos, integridad y localización sin guardar el archivo dentro de la base de datos.
- **FR-018 [I3]**: Antes de permitir la remisión, el sistema DEBE exigir título, descripción, procedencia, período cubierto, al menos un indicador del proceso y al menos un archivo o referencia externa; para cada archivo DEBE validar tipo permitido, máximo de 25 MiB, tipo detectado y huella SHA-256.
- **FR-019 [I3]**: Remitir una evidencia DEBE crear una versión inmutable con autor, fecha y vínculo a su versión anterior cuando exista.
- **FR-020 [I3]**: Corregir evidencia remitida DEBE crear una versión sucesora sin alterar ni eliminar versiones anteriores.
- **FR-021 [I4]**: Solo un revisor documental autorizado DEBE poder aceptar, observar o rechazar una versión remitida dentro de su ámbito.
- **FR-022 [I4]**: Toda decisión documental DEBE incluir estado, justificación, actor, fecha y versión exacta de evidencia.
- **FR-023 [I4]**: Aceptar documentalmente una evidencia NO DEBE crear ni inferir una valoración de cumplimiento académico.
- **FR-024 [I4]**: Rectificar una decisión documental DEBE generar una nueva decisión relacionada y conservar la anterior.
- **FR-025 [I5]**: Solo un evaluador académico autorizado DEBE poder valorar un indicador dentro de su ámbito.
- **FR-026 [I5]**: Una valoración final DEBE usar una opción de la escala configurada, incluir justificación y referenciar exclusivamente versiones documentalmente aceptadas.
- **FR-027 [I5]**: La valoración académica NO DEBE modificar el estado ni el contenido de la revisión documental.
- **FR-028 [I5]**: Rectificar una valoración final DEBE generar una nueva revisión relacionada y conservar la anterior.
- **FR-029 [I6]**: El sistema DEBE calcular avance con una fórmula versionada y visible que distinga recopilación, revisión documental y valoración académica.
- **FR-030 [I6]**: El tablero DEBE permitir navegar desde conteos agregados hasta indicadores, decisiones y evidencias autorizadas que los sustentan.
- **FR-031 [I6]**: El sistema DEBE impedir el cierre mientras la política de cierre versionada vinculada al proceso detecte indicadores obligatorios sin valoración final válida, revisiones o valoraciones pendientes de rectificación, o archivos referenciados sin estado disponible. El resumen de cierre DEBE identificar la versión exacta de esta política.
- **FR-032 [I6]**: Cerrar un proceso DEBE congelar un resumen reproducible de versión de modelo, fórmula, resultados y referencias de trazabilidad.
- **FR-033 [I6]**: El informe interno DEBE declarar que apoya la autoevaluación y no constituye acreditación ni decisión de un organismo externo.
- **FR-034 [I6]**: El sistema DEBE exportar un informe autorizado sin exponer información fuera del ámbito del solicitante.
- **FR-035 [I7]**: Los contratos de importación y exportación futuros DEBEN estar versionados, validar el paquete completo antes de incorporar cambios y evitar dependencia de servicios externos en I0–I6.
- **FR-036 [I7]**: Todo intercambio futuro DEBE registrar procedencia, versión, resultado, errores y actor técnico o humano responsable.
- **FR-037 [I8]**: Las notificaciones futuras DEBEN originarse en eventos versionados y contener únicamente la información mínima permitida para el destinatario.
- **FR-038 [I8]**: La indisponibilidad de un canal complementario NO DEBE cambiar estados, decisiones ni disponibilidad del flujo principal.
- **FR-039 [Transversal]**: Toda respuesta de error DEBE usar una estructura estable con código, mensaje comprensible, identificador de correlación y detalles seguros cuando correspondan.
- **FR-040 [Transversal]**: El sistema DEBE conservar trazabilidad explícita entre requisitos, historias, contratos, tareas, pruebas y resultados de validación.

### Requisitos no funcionales

- **RNF-001 Seguridad**: Las contraseñas DEBEN almacenarse mediante un algoritmo adaptativo con sal única; nunca en texto claro ni mediante cifrado reversible.
- **RNF-002 Seguridad**: Las sesiones DEBEN expirar tras 30 minutos de inactividad y todo cierre de sesión o desactivación de cuenta DEBE invalidar el acceso posterior.
- **RNF-003 Seguridad**: Cinco intentos fallidos consecutivos en 15 minutos DEBEN producir una limitación temporal sin revelar si la cuenta existe.
- **RNF-004 Archivos**: Solo se aceptarán tipos configurados para el piloto, con máximo inicial de 25 MiB por archivo; nombre, tipo declarado, tipo detectado, tamaño y huella de integridad DEBEN registrarse.
- **RNF-005 Privacidad**: Los registros, mensajes y exportaciones NO DEBEN incluir contraseñas, secretos, contenido completo de archivos ni datos fuera del ámbito autorizado.
- **RNF-006 Rendimiento**: El 95 % de las operaciones interactivas del MVP, excluyendo transferencia de archivos y generación de informes, DEBE completar su resultado visible en 2 segundos o menos bajo la carga piloto.
- **RNF-007 Rendimiento**: El 95 % de cargas de archivos de hasta 25 MiB DEBE finalizar o reportar un error recuperable en 15 segundos o menos en el entorno de referencia.
- **RNF-008 Capacidad**: El piloto DEBE admitir al menos 100 usuarios registrados, 30 sesiones concurrentes, 500 indicadores, 10 000 versiones de evidencia y 100 GiB de archivos sin incumplir RNF-006.
- **RNF-009 Disponibilidad**: Una falla de exportación, notificación o canal futuro NO DEBE impedir autenticación ni operaciones I0–I6.
- **RNF-010 Recuperación**: Las operaciones que combinan metadatos y archivos DEBEN quedar completas o recuperables sin referencias falsas tras una interrupción.
- **RNF-011 Concurrencia**: Las actualizaciones concurrentes DEBEN detectar versiones obsoletas y evitar pérdida silenciosa de cambios.
- **RNF-012 Auditoría**: Los eventos auditables DEBEN ser inmutables para usuarios ordinarios, consultables por ámbito y ordenables inequívocamente por fecha.
- **RNF-013 Observabilidad**: Cada solicitud DEBE producir un identificador de correlación y los fallos críticos DEBEN registrar contexto técnico suficiente sin divulgar datos sensibles.
- **RNF-014 Accesibilidad**: Los recorridos I0–I6 DEBEN poder completarse con teclado, foco visible, etiquetas comprensibles y contraste conforme a WCAG 2.2 nivel AA.
- **RNF-015 Compatibilidad**: La interfaz web DEBE soportar las dos versiones estables más recientes de Chrome, Edge y Firefox en resoluciones desde 1280 × 720.
- **RNF-016 Portabilidad**: Desarrollo y pruebas DEBEN disponer de un camino documentado sin Docker; cualquier contenedor será opcional.
- **RNF-017 Mantenibilidad**: Las reglas de dominio, autorización y estados DEBEN permanecer separadas de controladores, persistencia e interfaz.
- **RNF-018 Verificabilidad**: Estados, permisos, cálculo de avance, versionado y conflictos de funciones DEBEN contar con pruebas automatizadas y datos sintéticos reproducibles.
- **RNF-019 Protección de datos de prueba**: Ningún entorno de desarrollo, prueba, demostración o CI DEBE requerir datos institucionales reales; los ejemplos y archivos DEBEN ser sintéticos y estar libres de secretos.

### Entidades clave

- **Persona/Usuario**: identidad humana, estado y referencia de autenticación; su desactivación conserva autoría histórica.
- **Función de proceso**: capacidad institucional separada del privilegio técnico, por ejemplo responsable, revisor, evaluador o coordinador.
- **Ámbito/Asignación**: unión temporal entre usuario, función, carrera, proceso e indicador, con vigencia y posibles excepciones auditadas.
- **Carrera**: unidad académica evaluada; el MVP utiliza una carrera piloto sintética.
- **Modelo de evaluación**: definición lógica con versiones; agrupa criterios, estándares, indicadores, escala y reglas configurables.
- **Proceso de autoevaluación**: instancia para una carrera, período y versión inmutable del modelo, con estados y reglas de cierre.
- **Evidencia**: concepto recopilado para uno o más indicadores; posee versiones inmutables.
- **Versión de evidencia**: instantánea remitida con metadatos, archivo o referencia, integridad, procedencia y relación de sucesión.
- **Decisión documental**: dictamen versionado sobre una versión de evidencia, separado de la valoración académica.
- **Valoración académica**: revisión versionada del cumplimiento de un indicador sustentada en evidencias aceptadas.
- **Evento de auditoría**: registro inmutable de actor, fecha, acción, objeto, resultado y correlación.
- **Resumen de cierre**: instantánea reproducible del modelo, fórmula, resultados y trazabilidad al cerrar un proceso.

## Decisiones institucionales abiertas

Estas decisiones se registran para validación institucional. No autorizan a inventar normativa. El piloto utilizará valores sintéticos configurables y conservará la procedencia de cada decisión.

- **CL-001 — Catálogo de funciones y competencias**: validar nombres, competencias y delegaciones; el piloto usa las funciones descritas en Actores.
- **CL-002 — Conflictos y excepciones de funciones**: validar combinaciones prohibidas y quién autoriza excepciones; por defecto, una persona no revisa documentalmente ni valora académicamente evidencia que ella misma remitió.
- **CL-003 — Aprobación del modelo**: validar órgano, quórum y vigencia; el piloto exige aprobación explícita de un administrador funcional distinto del autor.
- **CL-004 — Estados de revisión documental**: validar vocabulario y efectos; el piloto usa aceptada, observada y rechazada.
- **CL-005 — Escala de valoración académica**: validar niveles y reglas; el piloto usa una escala sintética configurable sin equivalencia oficial.
- **CL-006 — Fórmula de avance**: validar ponderaciones y tratamiento de indicadores no aplicables; el piloto muestra por separado porcentajes de recopilación, revisión y valoración, sin un índice oficial combinado.
- **CL-007 — Reapertura y rectificación**: validar autoridades, plazos y motivos; el piloto requiere motivo y autorización del coordinador, preservando versiones anteriores.
- **CL-008 — Archivos y conservación**: validar tipos, tamaños, retención y eliminación; el piloto aplica RNF-004 y conserva archivos durante toda la vida del proceso.
- **CL-009 — Auditoría y acceso a trazas**: validar plazo de conservación y perfiles de consulta; el piloto conserva toda la auditoría y restringe consulta a coordinadores y administradores autorizados.
- **CL-010 — Integraciones y notificaciones**: validar sistemas, responsables y canales; quedan fuera del MVP y solo se definen contratos desacoplados en I7–I8.

## Criterios de éxito

### Resultados medibles

- **SC-001**: En una prueba con la matriz de permisos, el 100 % de operaciones protegidas permitidas se completa dentro del ámbito y el 100 % de intentos fuera de ámbito se rechaza sin revelar datos.
- **SC-002**: El 100 % de modelos publicados, versiones de evidencia remitidas, decisiones documentales y valoraciones finales conserva versión, autor, fecha e historial sin sobrescrituras.
- **SC-003**: En el recorrido sintético I0–I6, un equipo puede configurar carrera, publicar modelo, abrir proceso, remitir evidencia, revisarla, valorarla y cerrar el proceso en 45 minutos o menos sin intervención técnica.
- **SC-004**: El 100 % de valoraciones incluidas en un informe cerrado puede rastrearse hasta indicador, versión de modelo, evaluador, decisión documental y versiones de evidencia utilizadas.
- **SC-005**: Ninguna aceptación documental de la suite de aceptación produce por sí misma un nivel de cumplimiento académico.
- **SC-006**: Los tableros muestran conteos y porcentajes reproducibles; al recalcular el mismo cierre con la misma fórmula y datos se obtiene el mismo resultado en el 100 % de los casos.
- **SC-007**: En una prueba moderada con al menos 20 participantes sintéticos distribuidos entre administrador funcional, responsable, revisor, evaluador y coordinador, al menos 19 completan al primer intento una tarea representativa de su función en I0–I6, sin ayuda operativa distinta de la interfaz y la guía de usuario; inicio, fin, ayuda solicitada y resultado se registran por participante.
- **SC-008**: Se satisfacen los umbrales RNF-006 a RNF-008 en un entorno reproducible con datos sintéticos y un informe de medición conservado.
- **SC-009**: La matriz de trazabilidad vincula el 100 % de FR-001–FR-040 y RNF-001–RNF-019 con al menos una historia, contrato o regla de dominio, tarea y evidencia de validación identificada; toda prueba automatizada crítica declara los IDs que valida.
- **SC-010**: I0–I6 se ejecuta y valida sin Docker, sin servicios institucionales, sin notificaciones y sin ningún componente I7–I8.

## Exclusiones

- Emitir acreditaciones, dictámenes oficiales o decisiones atribuibles a CACES, CES u otra entidad externa.
- Usar datos institucionales reales no autorizados en desarrollo, pruebas, demostraciones o CI.
- Integración productiva con identidad institucional, repositorios documentales, correo, mensajería u otros servicios externos durante el MVP.
- Aplicación móvil, operación sin conexión y notificaciones productivas durante el MVP.
- Analítica predictiva, recomendaciones automáticas o valoración académica generada por inteligencia artificial.
- Despliegue productivo, alta disponibilidad multirregión y migración de datos institucionales heredados.

## Supuestos y dependencias

- La institución designará responsables para validar CL-001–CL-010 antes de un despliegue productivo.
- El piloto usa una carrera, un proceso activo y un modelo publicado; la solución conserva capacidad de crecimiento dentro de RNF-008.
- Los valores sintéticos de escala, fórmula y política de funciones son configuración del piloto, no decisiones institucionales.
- Los usuarios disponen de conexión de red estable y navegador compatible; no se exige funcionamiento sin conexión.
- La documentación normativa que se incorpore se registra como fuente y versión, pero no se interpreta automáticamente como regla ejecutable.
- El almacenamiento de archivos está disponible mediante una abstracción reemplazable; su proveedor concreto se decide durante la planificación.
- Las pruebas automatizadas, la matriz de trazabilidad y la documentación técnica y de usuario son parte del alcance de cada incremento.
