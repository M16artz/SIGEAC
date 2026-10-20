# Feature Specification: I1 — Modelo de evaluación versionado

**Feature Branch**: `main` (no se creó una rama porque no existe un hook `before_specify`)

**Created**: 2026-10-19

**Status**: Draft

**Input**: User description: "Módulo I1 evaluationmodel — Modelo de evaluación versionado del Sistema de Evaluación de Carreras SEC-UNL. Un administrador funcional registra y publica una versión inmutable del modelo con jerarquía de criterios, estándares e indicadores, escala de valoración y fórmula de avance. Antes de publicar se exige fuente, autor, aprobador distinto del autor, al menos un criterio, al menos un estándar por criterio, al menos un indicador por estándar, códigos únicos por tipo dentro de la versión, una escala con al menos una opción y una fórmula de avance válida. La versión publicada no puede modificarse; toda corrección crea una nueva versión relacionada. Incluye consulta y comparación de versiones y auditoría de creación, publicación, sustitución y consulta. Excluye procesos, asignaciones, evidencias, revisión documental y valoración académica. Requisitos de referencia: FR-007 a FR-010; RNF-011, RNF-012, RNF-018. Resultado medible: publicar la versión 1, rechazar su modificación directa y crear la versión 2 conservando relación y procedencia."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Registrar una versión en borrador (Priority: P1)

Como administrador funcional autorizado, quiero registrar una versión en borrador con su fuente, jerarquía, escala y fórmula para preparar un modelo completo antes de someterlo a publicación.

**Why this priority**: El borrador concentra la definición que debe revisarse y validarse antes de quedar disponible como referencia inmutable.

**Independent Test**: Con datos sintéticos se crea un modelo y una primera versión en borrador, se incorporan criterios con estándares e indicadores, una escala y una fórmula, y se comprueba que todo puede consultarse en el orden registrado.

**Acceptance Scenarios**:

1. **Given** un administrador funcional autorizado, **When** crea una versión en borrador con denominación, fuente y autor identificados, **Then** la versión recibe una identidad propia, número de versión y estado de borrador.
2. **Given** una versión en borrador, **When** registra criterios, estándares e indicadores con sus códigos y orden, **Then** la consulta reproduce la jerarquía completa sin mezclar nodos de otras versiones.
3. **Given** una versión en borrador, **When** registra una escala con opciones y una fórmula de avance identificable, **Then** ambas quedan vinculadas exclusivamente a esa versión.
4. **Given** dos administradores que parten de la misma revisión del borrador, **When** ambos intentan guardar cambios, **Then** se detecta el cambio concurrente y ninguno sobrescribe silenciosamente el trabajo del otro.

---

### User Story 2 - Validar y publicar la versión (Priority: P1)

Como administrador funcional habilitado para aprobar, quiero publicar únicamente una versión completa cuya autoría y procedencia estén claras, para que pueda utilizarse como referencia estable y verificable.

**Why this priority**: La publicación establece la instantánea oficial del modelo dentro del sistema y debe impedir estructuras incompletas o aprobaciones sin independencia.

**Independent Test**: Se intenta publicar una versión sintética omitiendo por turno cada condición obligatoria y se comprueba su rechazo; al completar todas las condiciones y usar un aprobador distinto del autor, la publicación tiene éxito.

**Acceptance Scenarios**:

1. **Given** una versión con fuente y autor, al menos un criterio, al menos un estándar en cada criterio, al menos un indicador en cada estándar, códigos únicos por tipo, una escala no vacía y una fórmula válida, **When** una persona distinta del autor la aprueba y publica, **Then** la versión queda publicada con autor, aprobador y fecha de publicación identificables.
2. **Given** una versión que incumple una o más condiciones de publicación, **When** se solicita publicarla, **Then** la operación se rechaza y se informan todas las condiciones incumplidas que la persona está autorizada a conocer.
3. **Given** que el autor y el aprobador son la misma identidad, **When** se solicita publicar, **Then** la publicación se rechaza aunque el contenido esté completo.
4. **Given** códigos repetidos entre dos nodos del mismo tipo y versión, **When** se solicita publicar, **Then** la publicación se rechaza; el mismo código en tipos diferentes no constituye por sí solo una duplicidad.
5. **Given** una fórmula sin definición completa, con referencias inexistentes, resultado indeterminado para un caso declarado o sin tratamiento explícito de ausencia de elementos aplicables, **When** se solicita publicar, **Then** la publicación se rechaza como fórmula inválida.

---

### User Story 3 - Corregir mediante una versión sucesora (Priority: P1)

Como administrador funcional autorizado, quiero corregir un modelo publicado creando una nueva versión relacionada para preservar exactamente lo que estuvo vigente y mantener la procedencia de cada cambio.

**Why this priority**: La inmutabilidad y la relación entre versiones son esenciales para que decisiones posteriores puedan referirse sin ambigüedad al modelo utilizado.

**Independent Test**: Se publica la versión 1, se intenta modificar directamente cada grupo de datos protegido y se verifica el rechazo; después se crea la versión 2 como sucesora, se modifica su borrador y se comprueba que la versión 1 permanece intacta.

**Acceptance Scenarios**:

1. **Given** una versión publicada, **When** se intenta cambiar su fuente, jerarquía, escala, fórmula, autor, aprobador o datos de publicación, **Then** el cambio se rechaza y la versión conserva exactamente su contenido anterior.
2. **Given** una versión publicada, **When** un administrador inicia una corrección, **Then** se crea un nuevo borrador con número distinto, relación explícita con la versión precedente y procedencia conservada, sin alterar la versión publicada.
3. **Given** una versión sucesora completa, **When** se publica como sustituta, **Then** la relación de sustitución queda registrada y ambas versiones siguen identificables y consultables.
4. **Given** una versión anterior sustituida, **When** se consulta o compara, **Then** su contenido histórico permanece disponible y no se presenta como borrador editable.

---

### User Story 4 - Consultar y comparar versiones (Priority: P2)

Como administrador funcional o consultor autorizado, quiero consultar una versión y compararla con otra del mismo modelo para reconocer cambios de estructura, escala, fórmula y procedencia antes de usarla como referencia.

**Why this priority**: La comparación reduce errores de interpretación y hace visible qué cambió sin requerir revisar manualmente cada versión completa.

**Independent Test**: Se crean dos versiones sintéticas relacionadas con diferencias conocidas y se verifica que la comparación identifica adiciones, eliminaciones y modificaciones sin combinar sus contenidos.

**Acceptance Scenarios**:

1. **Given** dos versiones del mismo modelo dentro del ámbito autorizado, **When** se comparan, **Then** se muestran sus identificadores, estados, fuentes, autores, aprobadores y diferencias de criterios, estándares, indicadores, escala y fórmula.
2. **Given** un elemento sin cambios entre versiones, **When** se genera la comparación, **Then** se reconoce como sin cambio y no se presenta como agregado o eliminado.
3. **Given** versiones de modelos distintos, **When** se solicita una comparación directa, **Then** la operación se rechaza para evitar una equivalencia no declarada.
4. **Given** una versión fuera del ámbito de consulta, **When** se intenta consultar o comparar, **Then** no se revela su contenido ni se confirma información protegida sobre ella.

---

### User Story 5 - Auditar el ciclo de vida del modelo (Priority: P3)

Como responsable autorizado de control, quiero consultar la trazabilidad de creación, publicación, sustitución y consulta de versiones para reconstruir el ciclo de vida del modelo sin modificar su historial.

**Why this priority**: La auditoría demuestra quién realizó cada acción crítica y permite revisar la procedencia de las versiones publicadas.

**Independent Test**: Se ejecuta con identidades sintéticas una creación, una publicación, varias consultas y una sustitución; luego se comprueba que cada evento puede ordenarse y correlacionarse, y que un usuario ordinario no puede modificarlo.

**Acceptance Scenarios**:

1. **Given** una creación, publicación, sustitución o consulta autorizada, **When** la acción concluye, **Then** queda un evento inmutable con actor, fecha, acción, versión afectada, resultado y correlación.
2. **Given** un intento de publicación o modificación rechazado, **When** se consulta la auditoría dentro del ámbito autorizado, **Then** el rechazo y su motivo seguro pueden reconstruirse.
3. **Given** un usuario ordinario, **When** intenta alterar o eliminar un evento de auditoría, **Then** la operación se impide.

### Edge Cases

- Un criterio no contiene estándares o un estándar no contiene indicadores: el borrador puede guardarse incompleto, pero no publicarse.
- Una versión contiene códigos repetidos con diferencias de mayúsculas o espacios no significativos: se consideran duplicados según la forma normalizada usada por el catálogo.
- Dos tipos distintos reutilizan el mismo código: no existe conflicto de unicidad porque la regla se aplica separadamente por tipo dentro de la versión.
- La jerarquía contiene un nodo sin padre válido, un ciclo o un nodo asociado a otra versión: la versión no puede publicarse.
- La escala contiene opciones duplicadas, sin identificación o sin orden inequívoco: la versión no puede publicarse.
- La fórmula produce una división por cero, carece de resultado para cero elementos aplicables o referencia elementos ausentes: se considera inválida para publicación.
- Dos administradores publican o modifican simultáneamente el mismo borrador: solo puede prevalecer una revisión vigente y la otra operación recibe un conflicto recuperable sin pérdida silenciosa.
- Se intenta crear una versión sucesora desde un borrador o desde una versión de otro modelo: la relación se rechaza.
- Se consulta una versión mientras otra persona publica su sucesora: cada respuesta representa una versión identificada y consistente, sin combinar datos entre ambas.
- Una auditoría de consulta no puede registrarse: la consulta sensible no se presenta como completada.

## Requirements *(mandatory)*

### Scope Boundaries

**Included**:

- Registro del modelo y de versiones en borrador, publicadas o sustituidas.
- Jerarquía ordenada de criterios, estándares e indicadores por versión.
- Escala de valoración y opciones asociadas a la versión.
- Definición y validación previa de una fórmula de avance vinculada a la versión.
- Publicación, inmutabilidad y creación de versiones sucesoras relacionadas.
- Consulta y comparación de versiones del mismo modelo.
- Auditoría de creación, publicación, sustitución, consulta y rechazos relacionados.

**Excluded**:

- Creación o gestión de procesos de autoevaluación y vinculación de una versión a un proceso.
- Asignación de responsables, revisores o evaluadores.
- Registro, carga, remisión o versionado de evidencias.
- Revisión documental y valoración académica.
- Ejecución de la fórmula sobre un proceso real, cálculo de tableros o cierre de procesos.
- Definición institucional definitiva de escalas, ponderaciones, fórmulas, órganos de aprobación o equivalencias entre modelos.
- Mezcla automática, homologación o comparación de versiones pertenecientes a modelos distintos.

### Functional Requirements

- **FR-007 — Estructura versionada**: El sistema DEBE representar cada versión mediante una jerarquía ordenada de criterios, estándares e indicadores, una escala de valoración y una fórmula de avance; cada elemento DEBE pertenecer a una sola versión, tener identidad estable dentro de ella y poder consultarse o compararse sin mezclar contenido de otras versiones.
- **FR-008 — Validación de publicación**: Antes de publicar, el sistema DEBE exigir fuente, autor, aprobador distinto del autor, al menos un criterio, al menos un estándar por criterio, al menos un indicador por estándar, códigos únicos por tipo dentro de la versión, una escala con al menos una opción y una fórmula válida; la solicitud DEBE rechazarse informando todas las condiciones incumplidas que el solicitante esté autorizado a conocer.
- **FR-008a — Validez de fórmula**: Para publicación, una fórmula DEBE estar identificada, declarar los resultados de avance que produce, usar únicamente elementos definidos en la misma versión, producir un resultado determinista para iguales entradas, establecer el tratamiento de elementos no aplicables o de totales iguales a cero y superar casos sintéticos representativos; esta validación NO implica ejecutar la fórmula sobre un proceso ni establecer una fórmula institucional definitiva.
- **FR-009 — Inmutabilidad y sucesión**: Una versión publicada DEBE ser inmutable en su fuente, estructura, escala, fórmula, autoría, aprobación y datos de publicación; toda corrección DEBE originar un nuevo borrador con número e identidad propios, relación explícita con su predecesora y procedencia conservada. Designar una sucesora como sustituta NO DEBE eliminar ni modificar la versión anterior.
- **FR-010 — Consulta, comparación y auditoría**: El sistema DEBE permitir a personas autorizadas consultar una versión y comparar dos versiones del mismo modelo, identificando adiciones, eliminaciones y modificaciones; además DEBE registrar de forma inmutable creación, publicación, sustitución, consulta, comparación y rechazos críticos con actor, fecha, acción, versión, resultado y correlación.

### Applicable Quality Requirements

- **RNF-011 — Concurrencia**: Las actualizaciones o publicaciones concurrentes de un borrador DEBEN detectar que una persona partió de una revisión obsoleta y evitar la pérdida silenciosa de cambios.
- **RNF-012 — Auditoría**: Los eventos del ciclo de vida y consulta del modelo DEBEN ser inmutables para usuarios ordinarios, consultables únicamente dentro del ámbito autorizado y ordenables inequívocamente por fecha y correlación.
- **RNF-018 — Verificabilidad**: Las reglas de estructura, publicación, fórmula, unicidad, inmutabilidad, sucesión y concurrencia DEBEN contar con pruebas automatizadas reproducibles basadas en datos sintéticos.

### Key Entities *(include if feature involves data)*

- **Modelo de evaluación**: Identidad lógica y denominación del modelo que agrupa sus versiones sin combinar su contenido.
- **Versión del modelo**: Instantánea numerada con estado, fuente, autor, aprobador, fechas y relación opcional con una versión precedente; es editable solo como borrador e inmutable al publicarse.
- **Criterio**: Nivel superior de la jerarquía de una versión; tiene código, denominación, descripción y orden, y contiene uno o más estándares para poder publicar.
- **Estándar**: Nivel subordinado a un criterio de la misma versión; tiene código, denominación, descripción y orden, y contiene uno o más indicadores para poder publicar.
- **Indicador**: Unidad terminal de la jerarquía de una versión; tiene código, denominación, descripción, orden y las propiedades necesarias para ser referenciado posteriormente sin formar parte todavía de un proceso.
- **Escala de valoración**: Conjunto identificado y ordenado de opciones perteneciente a una versión; en I1 se define, pero no se utiliza para emitir valoraciones académicas.
- **Opción de escala**: Alternativa identificada dentro de una escala, con código, etiqueta y orden inequívoco; puede incluir un valor declarado sin atribuirle equivalencia institucional no validada.
- **Fórmula de avance**: Definición identificada y validable de los resultados de avance, sus entradas y el tratamiento de casos límite; en I1 se publica con el modelo, pero no se ejecuta sobre procesos.
- **Relación de sucesión**: Vínculo dirigido entre una nueva versión y su predecesora que expresa corrección o sustitución sin borrar el historial.
- **Comparación de versiones**: Resultado derivado que identifica elementos agregados, eliminados, modificados o sin cambio entre dos versiones del mismo modelo; no crea una nueva versión ni altera las comparadas.
- **Evento de auditoría**: Registro inmutable de actor, fecha, acción, versión afectada, resultado y correlación, sujeto al ámbito autorizado de consulta.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En el recorrido de aceptación se publica la versión 1, se rechaza el 100 % de los intentos de modificar directamente cualquiera de sus componentes y se crea la versión 2 conservando relación con la versión 1, fuente y procedencia de ambas.
- **SC-002**: El 100 % de los casos de publicación que omiten por turno fuente, autor, aprobador distinto, criterio, estándar, indicador, unicidad de códigos, opción de escala o fórmula válida se rechaza y señala las condiciones incumplidas sin publicar parcialmente.
- **SC-003**: El 100 % de las comparaciones de prueba entre dos versiones del mismo modelo clasifica correctamente cada diferencia conocida como adición, eliminación, modificación o ausencia de cambio, sin alterar las versiones comparadas.
- **SC-004**: El 100 % de los conflictos concurrentes de prueba evita sobrescrituras silenciosas y conserva una versión íntegra del borrador para que la persona pueda revisar o reintentar su cambio.
- **SC-005**: El 100 % de las creaciones, publicaciones, sustituciones, consultas, comparaciones y rechazos críticos del recorrido de aceptación tiene un evento de auditoría correlacionable, ordenable e inmutable para usuarios ordinarios.
- **SC-006**: Al menos 19 de 20 administradores funcionales de prueba completan correctamente, en su primer intento y en menos de 10 minutos, la preparación y publicación de una versión sintética válida usando únicamente la interfaz y la ayuda disponible.
- **SC-007**: El 100 % de las reglas críticas de estructura, publicación, fórmula, unicidad, inmutabilidad, sucesión y concurrencia tiene al menos una prueba automatizada reproducible con datos sintéticos.

## Assumptions

- El incremento I0 proporciona identidades, sesiones y autorización; I1 utiliza esas capacidades, pero no modifica sus reglas.
- Una versión puede guardarse incompleta mientras sea borrador. Todas las condiciones de FR-008 se exigen de forma conjunta al publicar.
- Los códigos se comparan en una forma normalizada que ignora diferencias no significativas de mayúsculas y espacios exteriores; la convención exacta de presentación se definirá durante la planificación sin cambiar la regla de unicidad por tipo.
- La numeración de versiones es única y creciente dentro de cada modelo; crear una sucesora no implica publicarla automáticamente.
- La publicación registra como autor a quien creó o mantiene la autoría declarada de la versión y como aprobador a la identidad distinta que confirma la publicación. El órgano, quórum y autoridad definitivos siguen sujetos a validación institucional.
- La versión sucesora puede partir del contenido de su predecesora para facilitar una corrección, pero recibe identidades propias para la versión y sus elementos; la versión anterior permanece sin cambios.
- La comparación se limita a versiones del mismo modelo y utiliza la identidad y los códigos de los elementos como referencias de correspondencia; no declara equivalencias entre modelos diferentes.
- La fórmula de avance se define y valida en I1, pero su ejecución sobre datos de un proceso pertenece a incrementos posteriores. Los valores piloto son sintéticos y no constituyen una fórmula oficial.
- La escala de valoración es configurable y sintética. Sus opciones no se interpretan como niveles oficiales hasta que la institución valide las decisiones correspondientes.
- Las consultas y comparaciones están sujetas al ámbito de autorización proporcionado por I0; los plazos de conservación de auditoría siguen la política institucional vigente.
- Todas las pruebas, demostraciones y ejemplos de I1 utilizan modelos, fuentes, jerarquías, escalas y fórmulas sintéticos, sin datos institucionales reales ni secretos.
