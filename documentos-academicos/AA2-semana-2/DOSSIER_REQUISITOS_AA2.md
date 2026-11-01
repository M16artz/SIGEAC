# AA2 — Dossier de requisitos y matriz de trazabilidad

| Identificación | Dato |
|---|---|
| Proyecto | SIGEAC / SEC-UNL, autoevaluación interna de carreras |
| Actividad | AA2, Semana 2 — Actividad Autónoma |
| Duración prevista | 4,5 horas; no es un tiempo de trabajo medido |
| Modalidad | Individual articulada con el proyecto grupal |
| Estudiante | [PENDIENTE: nombre completo y contribución revisada personalmente] |
| Asignatura / docente de referencia | Desarrollo Basado en Plataformas / Edison Leonardo Coronel Romero, por continuidad con APE2; confirmar si la guía AA2 corresponde a otra asignatura |
| Ciclo de referencia | 5 A, según APE2 |
| Fecha de preparación | 2026-10-31; fecha de entrega personal pendiente |
| Línea base | `faf868ea5a8a85285290c06d3baf8fa49889edac` y archivos académicos locales de APE2 |
| Estado | Dossier depurado documentalmente; pendiente de revisión individual, validación de dominio y verificaciones indicadas |

**Propósito:** justificar de dónde surge cada requisito y cómo podrá comprobarse después, mediante la cadena actor → necesidad → historia/requisito → criterio de aceptación → fuente.

El dossier es la revisión académica posterior de [specification.md de APE2](../APE2-semana-2/specification.md). No sobrescribe esa línea base ni modifica requisitos canónicos, código o fixtures. La [matriz](MATRIZ_TRAZABILIDAD.md), el [registro de fuentes](FUENTES_REFERENCIAS.md) y la [nota de cambios](NOTA_CAMBIOS_INDIVIDUAL.md) forman parte de esta entrega.

## 1. Revisión de fuentes y fundamentos técnicos

### 1.1. Método y criterios de depuración

Se contrastaron las tres historias y los diez FR/diez NFR de APE2 con las necesidades del proyecto, documentos primarios y fuentes ejecutables. Se separaron comportamientos compuestos, se explicitaron límites de las fuentes y se retiraron de la selección activa compromisos cuya pertinencia al recorte no estaba justificada suficientemente. “Retirar de la selección” no elimina el requisito global ni significa aprobación de un cambio de alcance.

Cada elemento se evalúa por claridad, necesidad, consistencia, verificabilidad, ausencia de redundancia y compatibilidad con su fuente. La revisión diferencia decisiones del producto, fundamento técnico, contexto institucional e implementación observada. Ninguna fuente se utiliza para atribuir una entrevista, designación o aprobación inexistente.

### 1.2. Fuentes consultadas y alcance de la evidencia

El registro completo F-01–F-14 está en [FUENTES_REFERENCIAS.md](FUENTES_REFERENCIAS.md), con autor, localizador, relación y límite. Los principales insumos son APE2, especificación y reglas del equipo, los PDFs oficiales CACES/UNL, RFC 9110 y WCAG 2.2.

El [Modelo Genérico CACES 2024](https://www.caces.gob.ec/wp-content/uploads/2024/04/Modelo-generico-para-la-evaluacio%CC%81n-del-entorno-de-aprendizaje-de-carreras-de-grado.pdf), secciones II y V, respalda contextualizar la evaluación mediante modelo, fuentes y períodos. El [Instructivo UNL 037-R-UNL-2022](https://www.unl.edu.ec/sites/default/files/gaceta/resolucion/2022-06/Resoluci%C3%B3n%20Nro.%20037-R-UNL-2022-%20Instructivo%20para%20el%20Aseguramiento%20de%20la%20Calidad%20de%20la%20UNL-signed.pdf), artículos 9–12, aporta un correlato institucional para funciones de evaluación.

Estas consultas permiten avanzar en procedencia documental respecto a APE2. No corroboran las configuraciones sintéticas ni la vigencia actual de esas disposiciones para el piloto. F-10 mantiene expresamente `institutionalValidity: false`; no se altera ese registro. El vínculo entre órganos institucionales y roles del software continúa pendiente de designaciones y validación.

### 1.3. Lectura de RFC 9110 aplicada al proyecto

Síntesis de la [RFC 9110](https://www.rfc-editor.org/rfc/rfc9110.html), organizada por los seis conceptos solicitados:

| Concepto / localizador | Síntesis técnica |
|---|---|
| Métodos, §§9.2–9.3 | GET recupera; POST procesa; PUT expresa reemplazo; DELETE solicita retirar una asociación. Seguridad e idempotencia son propiedades distintas. |
| Representación, §§8.1–8.3 | El recurso se comunica mediante representaciones y metadatos; Content-Type identifica el tipo. |
| Estados, §15 | El estado distingue éxito, rechazo y error; 201 indica creación y 412 una precondición incumplida. |
| Cabeceras, §§5, 8.8.3, 10.2.2, 12.5.1 | ETag identifica una representación; Location referencia el recurso; Accept expresa preferencias. |
| Contenido, §6.4 | El significado depende de método y estado; 204 no incluye contenido. |
| Seguridad, §§11 y 17 | Considerar autenticación, divulgación e intermediarios; HTTP por sí solo no resuelve la autorización de negocio. |

La definición de PATCH se consulta aparte en F-14. Estas fuentes no fijan los 30 minutos de sesión ni el p95 de 2 s; ambos son decisiones del proyecto F-03.

### 1.4. Decisiones y límites observados en la API

El código C-01 establece sesión con cookie y entrega CSRF; no se deduce soporte JWT/Bearer del ejemplo académico. C-03 separa alta y modificación de carrera. C-05 crea procesos en preparación y no permite que el cliente envíe un estado de activación en su DTO de alta.

En el contrato actual, editar una carrera utiliza PATCH con CareerInput completo; no debe enviarse un subconjunto arbitrario por creer que el método lo admite. La revisión empieza en 1 para carrera y en 0 para proceso. Esas diferencias son de los DTO/controladores, no reglas universales de HTTP.

C-06 acepta un ETag fuerte numérico y produce 400/428 ante formatos o precondición ausente según su contrato. La gramática general de If-Match es más amplia; no se califica una lista o `*` como inválida para todo HTTP por la restricción local. Referencia de contraste: F-09 §13.1.1.

**Observación HTTP-AA-01, pendiente:** RFC 9110 §15.5.2 exige un desafío WWW-Authenticate en 401. C-07/C-08 no lo establecen explícitamente y no se localizó la cabecera en fuentes principales. Falta observar respuestas reales y decidir el desafío aplicable; no se incorpora un Bearer ficticio ni se afirma conformidad integral de autenticación.

La aceptación de los contratos preliminares CP-01–09 de APE2 queda condicionada a ese contraste. Los ejemplos son reconstruidos y no se ejecutaron solicitudes.

## 2. Dossier depurado del incremento

### 2.1. Síntesis del problema y alcance

El proyecto organiza información y decisiones de autoevaluación interna. Para iniciar un proceso necesita identificar carrera, unidad, período, versión de modelo y actores autorizados, conservando el historial sin atribuir acreditación. No se aportó un diagnóstico empírico que mida pérdidas de documentos o tiempos institucionales; tales cifras no se inventan.

Se conserva el recorte propuesto en APE2: acceso, catálogo de unidades/carreras y preparación/consulta de un proceso con modelo publicado. No incluye autorización/activación institucional, evidencias, revisión, valoración, informes, mejora ni archivo. Su aceptación docente sigue pendiente en CL-APE-01.

Las decisiones de tecnología se mantienen fuera de las necesidades: las fuentes muestran Angular, Spring Boot y persistencia, pero el actor necesita una operación identificable y autorizada, no una clase o un framework concreto.

### 2.2. Actores y necesidades justificadas

| ID | Actor documentado | Necesidad | Fuente y límite |
|---|---|---|---|
| A-AA-01 | Usuario habilitado | N-AA-01: conocer su acceso vigente y terminarlo cuando concluya su trabajo. | F-03/F-05; identidad sintética disponible, persona real no identificada. |
| A-AA-02 | Administrador funcional autorizado | N-AA-02: mantener unidades/carreras identificables y con historial. | F-03/F-06; competencia institucional exacta por confirmar. |
| A-AA-03 | Coordinador con capacidad de gestión en la carrera | N-AA-03: preparar una evaluación con referencia exacta de carrera, período y modelo. | F-03/F-04; contexto F-07/F-08. No se equipara a una comisión por inferencia. |
| A-AA-04 | Consultor o lector autorizado | N-AA-04: consultar únicamente recursos visibles y comprender su estado. | F-03/F-05; los permisos de catálogo y proceso no son intercambiables. |
| A-AA-02/03 | Actores que modifican recursos | N-AA-05: evitar pérdida de cambios y poder explicar qué ocurrió. | RNF-011–013 y FR-006/039 de F-03; respaldo técnico F-09. |

Los actores son roles documentados con correlato de dominio parcial, no participantes entrevistados ni nombramientos acreditados. Un rol técnico no concede automáticamente función académica.

### 2.3. Historias de usuario depuradas

| ID / prioridad | Historia | Origen y mejora |
|---|---|---|
| HU-AA-01 / P1 | Como usuario habilitado, quiero iniciar y terminar mi acceso y consultar mis funciones vigentes, para operar mientras mi sesión esté autorizada y finalizarla al concluir. | APE2/US1; conserva beneficio y distingue sesión de permisos. |
| HU-AA-02 / P1 | Como administrador funcional autorizado, quiero registrar unidades y carreras con datos válidos, para disponer de un catálogo identificable antes de preparar evaluaciones. | Alta de APE2/US2; el registro de unidad puede ser prerrequisito con fixture. |
| HU-AA-03 / P1 | Como lector autorizado, quiero consultar catálogo, detalle e historial visibles, para identificar información de mi ámbito sin acceder a datos ajenos. | Consulta separada de APE2/US2; no concede modificación. |
| HU-AA-04 / P2 | Como administrador funcional autorizado, quiero corregir datos de una carrera conservando revisiones, para actualizar el catálogo sin perder el estado anterior ni sobrescribir cambios ajenos. | Edición separada de APE2/US2; criterio propio de concurrencia. |
| HU-AA-05 / P2 | Como administrador funcional autorizado, quiero desactivar una carrera con motivo, para impedir su uso en nuevos procesos conservando identidad y referencias históricas. | Cambio de estado separado de APE2/US2; no se presupone cierre de procesos anteriores. |
| HU-AA-06 / P2 | Como coordinador autorizado para gestionar procesos, quiero preparar y consultar una autoevaluación con carrera, período y modelo publicado, para identificarla antes de tramitar su autorización institucional. | APE2/US3; no incluye activación ni presupone autoridad de inicio. |

La división de US2 no aumenta el alcance; hace revisables sus objetivos de alta, lectura, corrección y desactivación. Cada historia puede comprobarse con prerrequisitos sintéticos existentes.

### 2.4. Requisitos funcionales

Los IDs AA-RF son locales al dossier. La columna de origen conserva el enlace a APE2 y a los FR del producto; no los renumera oficialmente.

| ID | Requisito observable depurado | Origen |
|---|---|---|
| AA-RF-01 | Permitir iniciar sesión a una identidad habilitada con credenciales válidas y rechazar credenciales inválidas sin confirmar si la identidad existe. | APE-FR-01; FR-001/005. |
| AA-RF-02 | Permitir cerrar la sesión actual y rechazar su reutilización posterior para operaciones protegidas. | APE-FR-01; FR-001. |
| AA-RF-03 | Permitir a la identidad autenticada consultar sus funciones y ámbitos vigentes, sin devolver los de otra identidad. | APE-FR-01; FR-001/002. |
| AA-RF-04 | Comprobar capacidad y ámbito vigentes antes de cada operación protegida; leer no concederá modificar y un recurso ajeno no expondrá información protegida. | APE-FR-02; FR-002/003. |
| AA-RF-05 | Permitir al administrador autorizado registrar una unidad con código normalizado único y nombre, conservando identidad y estado del alta. | Alta de unidad de APE-FR-03; FR-043. |
| AA-RF-06 | Permitir registrar una carrera con código normalizado único, nombre, unidad activa y vigencia coherente; rechazar entrada inválida sin crear una carrera parcial. | Alta de carrera de APE-FR-03; FR-043. |
| AA-RF-07 | Permitir consultar catálogo, detalle e historial de carreras/unidades según la operación, filtrando resultados y totales por la visibilidad efectiva del actor. | APE-FR-04/05; FR-043/002. |
| AA-RF-08 | Permitir modificar datos válidos de carrera conservando identidad, nueva revisión e historial anterior; no completar el cambio sobre una revisión obsoleta. | APE-FR-05; FR-043; RNF-011. |
| AA-RF-09 | Permitir desactivar una carrera con motivo y revisión vigente sin eliminación física; no activar nuevos procesos sobre una carrera no utilizable. | APE-FR-05/07; FR-043/011. |
| AA-RF-10 | Permitir consultar e identificar la versión publicada del modelo disponible para preparar el proceso, sin sustituir automáticamente la elegida por otra versión. | APE-FR-06; FR-010/012. |
| AA-RF-11 | Permitir al actor con gestión vigente crear un proceso para carrera/unidad activas, período permitido y versión publicada; guardarlo en preparación sin activación implícita. | APE-FR-07; FR-011. |
| AA-RF-12 | Permitir al actor autorizado consultar el proceso preparado y recuperar sus referencias y estado; rechazar una consulta ajena sin exponer el recurso. | APE-FR-08; FR-002/011/012. |
| AA-RF-13 | Registrar cambios críticos de catálogo y creación de procesos con actor, fecha, acción, objeto y resultado, conforme al catálogo de auditoría aplicable; no declarar completado un cambio cuya auditoría obligatoria no se conserve. | APE-FR-09; FR-006 y refinamiento FR-006a. |
| AA-RF-14 | Rechazar solicitudes inválidas, conflictos o falta de acceso con estructura y código estables que permitan identificar el problema sin detalles técnicos sensibles. | APE-FR-10; FR-039. |

No se establece una revisión inicial uniforme entre agregados ni un código HTTP único para todas las validaciones. Los contratos concretos de APE2 conservan esas diferencias.

### 2.5. Requisitos no funcionales retenidos

| ID | Atributo y condición verificable | Fuente/origen y estado |
|---|---|---|
| AA-RNF-01 | Contraseñas mediante hash adaptativo con sal, sin almacenamiento reversible ni exposición en respuestas. | RNF-001, parte de APE-NFR-01; revisión de controles, no certificación. |
| AA-RNF-02 | La sesión expirará tras 30 minutos sin actividad y la desactivación de identidad invalidará su acceso posterior. | RNF-002, parte de APE-NFR-01; umbral del producto F-03. |
| AA-RNF-03 | Respuestas y registros de los recorridos seleccionados no expondrán contraseñas, tokens completos ni datos ajenos al ámbito permitido. | RNF-005; privacidad separada de login y de estructura de error. |
| AA-RNF-04 | El p95 de resultado visible de las operaciones interactivas seleccionadas será ≤ 2 s bajo el entorno/carga piloto que se acuerde; archivos e informes están excluidos. | RNF-006, APE-NFR-02; en revisión hasta fijar protocolo. |
| AA-RNF-05 | Una actualización concurrente basada en revisión obsoleta será detectada antes de sobrescribir y conservará el cambio exitoso anterior. | RNF-011, APE-NFR-04; propiedad transversal complementaria de AA-RF-08. |
| AA-RNF-06 | La auditoría será inmutable para usuarios ordinarios y consultable únicamente con permisos aplicables. | RNF-012, parte de APE-NFR-05; retención institucional pendiente. |
| AA-RNF-07 | Cada solicitud tendrá correlación que permita relacionar respuesta y diagnóstico técnico sin revelar datos sensibles. | RNF-013, parte de APE-NFR-05; comprobar errores y éxitos. |
| AA-RNF-08 | Los recorridos seleccionados se completarán con teclado, foco visible y controles identificables; el objetivo de accesibilidad es WCAG 2.2 AA con revisión de todos los criterios aplicables. | RNF-014, APE-NFR-06; F-13; no certificado. |
| AA-RNF-09 | Las reglas de estado/autorización estarán separadas de controladores y persistencia y tendrán comprobaciones reproducibles con datos sintéticos. | RNF-017/018, APE-NFR-07; revisión de código y dependencias. |
| AA-RNF-10 | Los intercambios HTTP seleccionados tendrán método, entrada, representación, cabeceras, resultado y errores documentados y coherentes con el contrato aceptado. | APE-NFR-08; F-09/F-14 y FR-039; revisión técnica abierta HTTP-AA-01. |

**Decisiones de depuración:** APE-NFR-10 (datos sintéticos) se conserva como restricción del trabajo y de todas las pruebas, evitando presentarlo además como atributo duplicado del comportamiento productivo. APE-NFR-03 (canales complementarios) se mantiene en backlog global RNF-009: el recorte no contiene un canal operativo que permita justificar su ensayo aquí. APE-NFR-09 (19 de 20 participantes) sigue como objetivo global SC-007 y pregunta metodológica; no se promete un estudio humano dentro del AA individual. No se elimina ninguno del producto ni se afirma aprobación docente del aplazamiento.

### 2.6. Criterios de aceptación funcionales

Los AC-AA reemplazan la agrupación local de criterios APE2 solo en este dossier. Los casos de entrada inválida se separan para que su ejecución posterior sea interpretable.

| ID | Criterio Dado–Cuando–Entonces |
|---|---|
| AC-AA-01 | **Dado que** la identidad está habilitada y las credenciales son válidas, **cuando** inicia sesión, **entonces** obtiene una sesión para esa identidad. |
| AC-AA-02 | **Dado que** la identidad no existe o la contraseña es incorrecta, **cuando** intenta iniciar sesión, **entonces** no se concede sesión ni se revela cuál de las dos causas ocurrió. |
| AC-AA-03 | **Dado que** existe una sesión activa, **cuando** se cierra y se reutiliza, **entonces** la solicitud protegida es rechazada. |
| AC-AA-04 | **Dado que** hay sesión vigente, **cuando** consulta su identidad, **entonces** obtiene sus funciones/ámbitos vigentes y no los de otra persona. |
| AC-AA-05 | **Dado que** no hay sesión válida, **cuando** se pide una operación protegida, **entonces** no se entrega su representación. |
| AC-AA-06 | **Dado que** una identidad carece de capacidad de administrar, **cuando** intenta alta o cambio de catálogo, **entonces** se rechaza y el catálogo permanece igual. |
| AC-AA-07 | **Dado que** existe una unidad con código normalizado, **cuando** se intenta registrar otro equivalente, **entonces** se rechaza sin duplicar la unidad. |
| AC-AA-08 | **Dado que** la unidad está activa y el código de carrera es nuevo, **cuando** el administrador registra datos válidos, **entonces** se conserva la carrera con unidad y vigencia. |
| AC-AA-09 | **Dado que** ya existe el código normalizado de carrera, **cuando** se envía un alta equivalente, **entonces** se rechaza sin crear otra carrera. |
| AC-AA-10 | **Dado que** falta un campo obligatorio o un dato tiene formato inválido, **cuando** se solicita el alta, **entonces** se informa el error y no se crea el recurso. |
| AC-AA-11 | **Dado que** la unidad no existe o no está activa, **cuando** se registra carrera, **entonces** no se crea la carrera. |
| AC-AA-12 | **Dado que** la vigencia termina antes de su inicio, **cuando** se registra carrera, **entonces** se rechaza y no queda una carrera parcial. |
| AC-AA-13 | **Dado que** el lector solo tiene carrera A autorizada, **cuando** consulta lista, total o detalle de B, **entonces** no recibe datos ni totales ajenos a su visibilidad. |
| AC-AA-14 | **Dado que** la revisión está vigente y los nuevos datos son válidos, **cuando** modifica la carrera, **entonces** cambia su revisión conservando identidad y estado anterior consultable. |
| AC-AA-15 | **Dado que** dos clientes parten de la misma revisión y uno guarda, **cuando** el otro envía la revisión anterior, **entonces** su cambio se rechaza y el primero permanece intacto. |
| AC-AA-16 | **Dado que** la carrera tiene historial, **cuando** se desactiva con motivo válido y revisión vigente, **entonces** conserva identidad e historial y no es utilizable para un proceso nuevo. |
| AC-AA-17 | **Dado que** no existe un cambio de estado, **cuando** se solicita repetirlo, **entonces** no se crea una transición ficticia. |
| AC-AA-18 | **Dado que** el actor tiene acceso al modelo, **cuando** consulta una versión publicada identificada, **entonces** recupera su identidad/número/estado sin reemplazo automático por otra. |
| AC-AA-19 | **Dado que** hay gestión autorizada, carrera/unidad activas, período válido y versión publicada, **cuando** se prepara el proceso, **entonces** queda PREPARATION con las referencias elegidas. |
| AC-AA-20 | **Dado que** la carrera está desactivada o no es utilizable, **cuando** se intenta preparar un proceso nuevo, **entonces** no se crea el proceso. |
| AC-AA-21 | **Dado que** el período es inválido o excede la vigencia permitida, **cuando** se prepara el proceso, **entonces** se rechaza sin persistencia parcial. |
| AC-AA-22 | **Dado que** la versión no está publicada/disponible, **cuando** se intenta preparar proceso, **entonces** no se crea ni se elige silenciosamente otra versión. |
| AC-AA-23 | **Dado que** el actor tiene acceso al proceso preparado, **cuando** consulta, **entonces** recupera carrera, período, modelo y estado de preparación sin acreditación inferida. |
| AC-AA-24 | **Dado que** el recurso es ajeno al ámbito del lector, **cuando** intenta acceder al proceso, **entonces** no recibe su representación protegida. |
| AC-AA-25 | **Dado que** una escritura exige auditoría y su persistencia falla, **cuando** se ejecuta el comando, **entonces** no se presenta como cambio completado sin el registro obligatorio. |
| AC-AA-26 | **Dado que** una solicitud debe rechazarse, **cuando** se responde, **entonces** la estructura documentada identifica código, descripción y correlación sin secretos. |
| AC-AA-27 | **Dado que** el código de unidad es nuevo y el nombre válido, **cuando** el administrador registra la unidad, **entonces** obtiene identidad, estado y revisión y puede consultarla. |

AC-AA-27 incorpora el éxito de alta de unidad para no depender únicamente de su rechazo por duplicado. Se distinguen 27 AC funcionales y diez VC de calidad; son verificaciones especificadas, no resultados ejecutados.

### 2.7. Verificaciones de atributos de calidad

| ID | Verificación propuesta, sin ejecución declarada |
|---|---|
| VC-AA-01 | Con credenciales sintéticas, inspeccionar hashes y comprobar algoritmo adaptativo/sal y ausencia de contraseña reversible o en respuestas. |
| VC-AA-02 | Con reloj controlado, comprobar solicitud antes del límite, al cumplir 30 minutos sin actividad y después de desactivar la identidad. El límite se mide desde última actividad válida. |
| VC-AA-03 | Ejecutar casos de éxito/rechazo con secretos de prueba y comprobar cuerpos y registros: no deben contenerlos completos ni datos ajenos. |
| VC-AA-04 | Acordar entorno y recorrido; medir desde acción en navegador hasta resultado visible y calcular p95. Los 2 s vienen de F-03; la campaña histórica F-12 no sustituye esta medición. |
| VC-AA-05 | Repetir AC-AA-15 con dos actores autorizados; una revisión obsoleta debe causar cero sobrescrituras y conservar el cambio confirmado. |
| VC-AA-06 | Con usuario ordinario, intentar modificar auditoría y consultar fuera de ámbito; no debe cambiar eventos ni obtener registros no autorizados. |
| VC-AA-07 | Comprobar correlación presente y consistente en respuestas de éxito/error y diagnóstico autorizado correspondiente, sin secretos. |
| VC-AA-08 | Completar login, catálogo y preparación con teclado; revisar foco, nombres, contraste y demás criterios AA aplicables; registrar criterio, pantalla y evidencia. |
| VC-AA-09 | Revisar dependencias de reglas, controles y persistencia; ejecutar posteriormente reglas de arquitectura y pruebas relevantes, incluyendo revisión de SQL que ArchUnit no interpreta. |
| VC-AA-10 | Comparar contratos con respuestas reales: tipos JSON, método, estado y cabeceras aplicables; comprobar además HTTP-AA-01 antes de afirmar compatibilidad completa de autenticación. |

## 3. Trazabilidad, depuración y decisiones pendientes

### 3.1. Matriz inicial y lectura de estados

La [matriz de trazabilidad](MATRIZ_TRAZABILIDAD.md) contiene una fila por cada AA-RF y AA-RNF retenido. Sus 24 filas vinculan actor, necesidad, historia/requisito, AC o VC, fuente y estado. No se usa “Validado” para un elemento que solo tiene respaldo documental.

Las historias aportan beneficio; los requisitos definen obligación; los AC/VC definen comprobación; las fuentes justifican origen o implementación. Un vínculo a código es contraste, no causa suficiente para conservar una funcionalidad innecesaria.

### 3.2. Ejemplos de justificación completa

**Alta de carrera:** A-AA-02 necesita N-AA-02; HU-AA-02 y AA-RF-06 definen el alta válida; AC-AA-08–12 distinguen éxito, duplicado, formato, unidad y fecha; F-03/F-06 justifican el catálogo y C-03/C-04 permiten contrastar su comportamiento. F-08 aporta contexto institucional, pero no es una orden de implementar un endpoint de carreras.

**Preparación de proceso:** A-AA-03 necesita N-AA-03; HU-AA-06 y AA-RF-11 exigen referencias exactas y PREPARATION; AC-AA-19–22 cubren sus condiciones. F-07/F-08 sostienen el contexto, F-03/F-04 la decisión del producto y C-05 el contraste. Las designaciones y activación siguen pendientes, fuera del recorte.

**Concurrencia:** A-AA-02 necesita N-AA-05; AA-RF-08 es la operación y AA-RNF-05 su calidad transversal. AC-AA-15/VC-AA-05 comprueban pérdida de cambios; RNF-011 de F-03 es la obligación y F-09 el fundamento de la precondición HTTP. No se afirma una prueba superada por existir ETag.

### 3.3. Calidad y ausencia de redundancia

| Aspecto | Acción de depuración | Límite restante |
|---|---|---|
| Claridad | Separar login, logout e identidad; distinguir alta, lectura, edición y desactivación. | Confirmar vocabulario de roles institucionales. |
| Necesidad | Cada requisito retenido se vincula a N-AA-01–05. | Necesidades del equipo documentadas, sin entrevista real aportada. |
| Consistencia | Mantener recorte U1 y no convertir preparación en activación. | CL-APE-01 aún requiere aceptación. |
| Verificabilidad | Separar validaciones compuestas de APE2 en AC-AA-10/11/12 y AC-AA-20/21/22. | Ejecución y protocolos de carga/humanos pendientes. |
| No redundancia | Separar evento auditado de inmutabilidad; operación de edición de concurrencia; condición de ensayo de atributo de producto. | Conservar enlaces de refinamiento para evitar contar dos veces cobertura. |
| Compatibilidad con fuentes | Contexto oficial y política del software separados; HTTP-AA-01 explícito. | Corroborar vigencia, roles y comportamiento de 401 en ejecución. |

### 3.4. Observaciones y decisiones pendientes

| ID | Decisión o vacío | Estado y tratamiento |
|---|---|---|
| P-AA-01 | Confirmar recorte U1 y autoridad de cada rol. | Pendiente docente/equipo y designaciones; hereda CL-APE-01/02. |
| P-AA-02 | Verificar vigencia y aplicabilidad institucional de F-07/F-08. | Fuente primaria consultada; no se cierran CL-011–017 ni se reescriben manifiestos. |
| P-AA-03 | Aportar entrevistas o diagnóstico del proceso actual. | No inventar cifras de tiempo o pérdida documental; hereda CL-APE-04. |
| P-AA-04 | Resolver cinco fallos consecutivos frente a acumulados. | Mantener RNF-003/CL-APE-03 como pendiente; no elegir política por código existente. |
| P-AA-05 | Acordar entorno/carga de p95 y protocolo humano de usabilidad. | AA-RNF-04 en revisión; SC-007 global no medido aquí. |
| P-AA-06 | Comprobar WWW-Authenticate en 401 y desafío aplicable. | HTTP-AA-01: contraste técnico pendiente; no se inventa JWT. |
| P-AA-07 | Confirmar utilidad de ensayar canales complementarios ausentes. | APE-NFR-03 permanece global/diferido, fuera de la selección activa AA2. |
| P-AA-08 | Identificar estudiante, revisión personal y enlace de contribución. | La asistencia de IA no acredita una participación individual. |

### 3.5. Producto, revisión individual y tiempo previsto

El producto local incluye dossier, matriz, fuentes, nota de cambios y versión LaTeX. El remoto observado es [M16artz/SIGEAC](https://github.com/M16artz/SIGEAC); no se verificó acceso externo ni se publicó este AA2. El commit base no es un commit de la actividad.

Distribución sugerida de 4,5 horas, no cronometraje: 45 min para RFC; 45 min para fuentes del dominio; 60 min para historias/requisitos; 60 min para matriz/criterios; 45 min para dossier/nota; 15 min para revisión y evidencia individual. Total: 270 minutos.

El estudiante debe revisar los cambios de la [nota individual](NOTA_CAMBIOS_INDIVIDUAL.md), justificar con sus palabras los elementos modificados e identificar su evidencia real. No se atribuyen correcciones al docente ni a un integrante sin registro.

### 3.6. Declaración de uso de IA y límites

Se utilizó ChatGPT/Codex para revisar archivos y fuentes públicas, depurar textos, construir la matriz y generar documentos. No se realizaron entrevistas, validación con docente, solicitudes al backend, pruebas de negocio, commits ni publicación. Las comprobaciones documentales no equivalen a cumplimiento funcional o institucional. La revisión personal, datos del estudiante y aprobación del equipo permanecen pendientes.

