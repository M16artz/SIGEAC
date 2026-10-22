# Feature Specification: Flujos completos de evaluación de carreras

**Feature Branch**: `main`

**Created**: 2026-10-21

**Status**: Ready for implementation planning review

**Input**: Completar la documentación de actores, procesos, flujos, requisitos y criterios de aceptación del Sistema de Evaluación de Carreras, preservando un único motor configurable para los modelos General 2024, Medicina, Enfermería, Odontología y Derecho.

**Reglas complementarias obligatorias**: [actores, decisiones, estados y métricas](../../docs/requirements/domain-rules.md). Los criterios AC-X01–AC-X16 completan los escenarios siguientes. [Matriz de aceptación por proceso](../../docs/testing/process-acceptance.md).

## Scope and domain boundaries

Esta especificación reconcilia los procesos P01–P16. Los artefactos `001`–`004` siguen describiendo acceso, modelos, procesos y evidencias, pero esta especificación prevalece cuando exista una contradicción sobre jerarquías variables, fuentes requeridas, autorización institucional, métricas, valoración académica, informes, mejora o archivo.

El sistema diferencia obligatoriamente:

1. **recopilación documental**, que registra cobertura de fuentes requeridas;
2. **revisión documental**, que decide pertinencia, integridad y correspondencia de una versión;
3. **valoración académica**, que analiza el estándar mediante una regla validada o juicio humano fundamentado;
4. **acreditación**, que es una decisión externa y nunca un resultado emitido por el sistema.

El **MVP documental** comprende P01–P11 y consulta histórica básica de P16. P12–P15 y el cierre institucional completo de P16 son incrementos posteriores. El MVP no puede depender de una valoración académica final. Este documento define el alcance; la ejecución requiere aprobar primero la revisión humana y las decisiones de vigencia/competencia identificadas.

## Actors

- **Administrador técnico**: administra operación e identidades técnicas; no decide sobre documentos ni resultados académicos.
- **Administrador funcional**: administra carreras, unidades, permisos y catálogos autorizados.
- **Administrador de modelos**: prepara versiones de modelos con procedencia normativa.
- **Revisor metodológico**: valida la estructura del modelo antes de publicarla.
- **Director de carrera**: registra o confirma la autorización institucional del proceso conforme a la competencia aplicable.
- **Decanato e instancias de calidad**: participan en consulta, revisión y aprobación según el instructivo y las designaciones que se validen; el sistema conserva su constancia.
- **Coordinador de autoevaluación**: organiza el proceso, responsables, actividades, excepciones y seguimiento.
- **Responsable documental**: recopila y remite evidencias para fuentes e indicadores asignados.
- **Revisor documental**: acepta, observa o rechaza versiones documentales sin valorar cumplimiento académico.
- **Comité evaluador**: registra valoraciones cualitativas o valida variables cuantitativas y resultados.
- **Comisión de evaluación**: consolida resultados, fortalezas, debilidades y el informe preliminar.
- **Autoridad aprobadora**: registra decisiones institucionales sobre informes o acciones; el sistema no presume quién ocupa esta función hasta su validación.
- **Responsable de mejora**: ejecuta y reporta acciones de mejora.
- **Consultor, auditor o archivista autorizado**: consulta información histórica dentro de su ámbito.

## User Scenarios & Testing

### User Story 1 - Administrar carreras y ámbitos (Priority: P1)

Como administrador funcional quiero registrar, actualizar y desactivar carreras y unidades académicas para disponer de ámbitos válidos sin borrar su historia.

**Independent Test**: Registrar una carrera con código único, asignarla a una unidad, utilizarla en un proceso y comprobar que después solo puede desactivarse.

**Acceptance Scenarios**:

1. **Given** un código no utilizado y datos obligatorios, **When** se registra una carrera, **Then** queda activa y disponible para permisos y procesos.
2. **Given** un código existente, **When** se intenta registrar otra carrera con el mismo código normalizado, **Then** la operación se rechaza.
3. **Given** una carrera con procesos históricos, **When** se solicita eliminarla, **Then** no se elimina y solo puede desactivarse con auditoría.
4. **Given** un cambio de responsable o unidad, **When** se confirma, **Then** la nueva relación entra en vigor sin alterar el historial anterior.

### User Story 2 - Configurar los cinco modelos sin duplicación (Priority: P1)

Como administrador de modelos quiero representar versiones normativas con jerarquías, fuentes, períodos, escalas y reglas propias para aplicar cualquiera de los cinco modelos mediante el mismo motor.

**Independent Test**: Publicar configuraciones verificadas de General 2024 y estructuras representativas de Medicina 2018, Enfermería 2016, Odontología 2018 y Derecho matricial, demostrando diferentes profundidades y mecanismos sin crear módulos por carrera.

**Acceptance Scenarios**:

1. **Given** una versión en borrador, **When** se configuran nodos de profundidad variable, **Then** un indicador puede depender de cualquier nodo permitido sin exigir subcriterio.
2. **Given** un indicador, **When** se configura, **Then** conserva estándar, naturaleza, período, fuentes requeridas, escala y mecanismo de valoración aplicables.
3. **Given** un indicador cuantitativo, **When** se publica, **Then** sus variables, fórmula, umbrales y tratamiento de datos faltantes están identificados o el cálculo automático queda deshabilitado.
4. **Given** una versión utilizada, **When** cambia la norma, **Then** se crea una sucesora y el proceso histórico conserva la versión anterior.
5. **Given** un modelo sin documento, edición, emisor o vigencia verificada, **When** se intenta declararlo vigente, **Then** se mantiene como borrador o pendiente de validación.

### User Story 3 - Autorizar y planificar una autoevaluación (Priority: P1)

Como coordinador quiero preparar un proceso, registrar su autorización y planificar actividades para que la ejecución respete las competencias institucionales.

**Independent Test**: Crear un proceso en preparación, adjuntar la referencia de autorización, activarlo, asignar equipos y fechas, suspenderlo y reanudarlo conservando el historial.

**Acceptance Scenarios**:

1. **Given** una carrera y versión publicada compatible, **When** se crea un proceso, **Then** permanece en preparación hasta registrar una autorización institucional válida.
2. **Given** una autorización verificada, **When** la autoridad competente confirma el inicio, **Then** el proceso se activa y fija la versión del modelo.
3. **Given** indicadores aplicables, **When** el coordinador asigna responsable, revisor, evaluador, actividad y fecha, **Then** los participantes ven solo sus tareas vigentes.
4. **Given** autor y revisor coincidentes, **When** no existe excepción válida, **Then** la asignación se rechaza.
5. **Given** una suspensión motivada, **When** se confirma, **Then** se bloquean nuevas actuaciones ordinarias hasta su reanudación autorizada.

### User Story 4 - Cubrir fuentes requeridas con evidencias (Priority: P1)

Como responsable documental quiero asociar cada evidencia a una fuente requerida para demostrar qué requisito documental cubre cada versión.

**Independent Test**: Registrar dos fuentes requeridas, remitir evidencias para una sola, verificar 50 % de recopilación y comprobar que una evidencia no aceptada no incrementa el avance documental.

**Acceptance Scenarios**:

1. **Given** una fuente requerida asignada, **When** se crea una evidencia, **Then** queda vinculada al proceso, indicador y fuente exactos.
2. **Given** una evidencia reutilizable, **When** se vincula a varias fuentes autorizadas, **Then** cada cobertura queda explícita y no se duplica el archivo.
3. **Given** una fuente no aplicable, **When** una autoridad la excluye con regla y justificación, **Then** queda fuera del denominador y la decisión es auditable.
4. **Given** una versión remitida, **When** se corrige, **Then** se crea una sucesora sin alterar la cobertura histórica.

### User Story 5 - Revisar y subsanar documentación (Priority: P1)

Como revisor documental quiero decidir sobre una versión y como responsable quiero subsanar observaciones conservando todas las decisiones y versiones.

**Independent Test**: Rechazar una versión con observaciones, crear y remitir una sucesora, aceptarla y verificar que ninguna decisión produjo una valoración académica.

**Acceptance Scenarios**:

1. **Given** una versión en revisión, **When** el revisor autorizado la acepta, **Then** se registra decisión, fecha y versión sin marcar el indicador como cumplido.
2. **Given** una versión incompleta, **When** se observa o rechaza, **Then** la justificación es obligatoria y el responsable puede crear una sucesora.
3. **Given** que autor y revisor coinciden, **When** no hay excepción autorizada, **Then** la revisión se bloquea.
4. **Given** una decisión final, **When** se rectifica, **Then** se añade una nueva decisión y se conserva la anterior.

### User Story 6 - Medir el avance documental (Priority: P1)

Como coordinador quiero consultar cobertura y revisión por carrera, criterio, indicador, fuente y responsable para intervenir sobre pendientes sin confundirlos con cumplimiento académico.

**Independent Test**: Con cuatro fuentes aplicables, tres registradas y dos aceptadas, mostrar 75 % de recopilación, 50 % de revisión documental y valoración académica pendiente.

**Acceptance Scenarios**:

1. **Given** fuentes obligatorias aplicables, **When** se calcula avance, **Then** recopilación cuenta IDs distintos cubiertos por versiones remitidas candidatas vigentes, sin incluir borradores ni fuentes opcionales.
2. **Given** fuentes aplicables, **When** se calcula revisión, **Then** cuenta fuentes que satisfacen su cardinalidad con decisiones vigentes aceptadas por cobertura y divide para fuentes obligatorias aplicables.
3. **Given** cero fuentes aplicables, **When** se consulta el indicador, **Then** se muestra configuración incompleta o no aplicabilidad autorizada y nunca una división por cero.
4. **Given** un usuario acotado, **When** consulta el tablero, **Then** agregados y detalle contienen únicamente su ámbito.

### User Story 7 - Valorar indicadores académicamente (Priority: P2)

Como comité evaluador quiero aplicar la metodología de la versión del modelo para registrar resultados fundamentados sin atribuir acreditación.

**Independent Test**: Valorar un indicador cualitativo y calcular uno cuantitativo con variables verificadas, rechazando una fórmula no validada y conservando ambos fundamentos.

**Acceptance Scenarios**:

1. **Given** un indicador cualitativo, **When** se registra una opción de su escala con justificación, **Then** la valoración conserva evaluador, método, estándar y coberturas pertinentes aceptadas de sus evidencias exactas.
2. **Given** un indicador cuantitativo con regla validada, **When** se confirman todas sus variables, **Then** se calcula y conserva fórmula, entradas, resultado y umbral usados.
3. **Given** variables faltantes o estrategia no validada, **When** se solicita calcular, **Then** no se inventa resultado y la valoración queda pendiente o manual según la metodología.
4. **Given** discrepancia entre evaluadores, **When** se registra, **Then** se conserva el desacuerdo y se aplica el procedimiento institucional configurado.

### User Story 8 - Consolidar hallazgos y tramitar informes (Priority: P2)

Como comisión de evaluación quiero consolidar fortalezas, debilidades y resultados en informes versionados para tramitar observaciones y aprobación institucional.

**Independent Test**: Crear un informe preliminar, registrar socialización y observaciones, emitir una versión final y registrar una decisión de aprobación sin presentarla como acreditación.

**Acceptance Scenarios**:

1. **Given** resultados completos y pendientes, **When** se consolida, **Then** ambos se muestran explícitamente y no se calcula un agregado no autorizado.
2. **Given** un hallazgo, **When** se registra, **Then** se vincula a indicadores, evidencias y valoraciones que lo sustentan.
3. **Given** un informe preliminar, **When** se reciben observaciones, **Then** se conservan y una corrección crea una nueva versión.
4. **Given** aprobación pendiente o devolución, **When** se consulta el informe, **Then** no aparece como aprobado.

### User Story 9 - Gestionar planes de mejora (Priority: P2)

Como responsable de mejora quiero convertir hallazgos en acciones medibles y dar seguimiento a su ejecución.

**Independent Test**: Crear una acción desde una debilidad, aprobarla, registrar avances y medio de verificación, vencer el plazo y conservar el historial.

**Acceptance Scenarios**:

1. **Given** un hallazgo, **When** se propone una acción, **Then** exige responsable, plazo, meta y medio de verificación.
2. **Given** una acción no aprobada, **When** se intenta ejecutar como definitiva, **Then** permanece en propuesta.
3. **Given** una modificación, **When** se confirma, **Then** se conserva una revisión anterior.
4. **Given** un plazo vencido, **When** no existe verificación satisfactoria, **Then** se muestra vencida sin alterar el historial.

### User Story 10 - Cerrar, archivar y consultar el histórico (Priority: P2)

Como coordinador y archivista autorizado quiero cerrar y conservar procesos completos para permitir consultas históricas reproducibles.

**Independent Test**: Bloquear un cierre con pendientes obligatorios, autorizar un cierre excepcional, archivar la instantánea y crear una autoevaluación posterior sin sobrescribirla.

**Acceptance Scenarios**:

1. **Given** pendientes definidos por la política de cierre, **When** se solicita cerrar, **Then** se enumeran y el cierre se bloquea.
2. **Given** una excepción autorizada y motivada, **When** se cierra, **Then** la excepción queda en la instantánea de cierre.
3. **Given** un proceso cerrado, **When** se consulta, **Then** modelo, fuentes, evidencias, decisiones, valoraciones, informes y acciones conservan sus versiones exactas.
4. **Given** una nueva autoevaluación, **When** se crea, **Then** constituye otro proceso y no modifica el histórico.

### Edge Cases

- Una versión normativa puede estar publicada pero marcada como no vigente para nuevos procesos; los históricos continúan usándola.
- Un indicador puede depender directamente de un criterio o de niveles intermedios variables.
- Una misma evidencia puede cubrir más de una fuente, pero cada cobertura requiere una relación explícita.
- Una fuente excluida del denominador requiere decisión autorizada, justificación y vigencia.
- Una nueva decisión documental puede invalidar una evidencia usada por una valoración; esta queda marcada para revisión, no se reescribe.
- Resultados cualitativos y cuantitativos no se promedian salvo que la metodología de esa versión lo autorice.
- El fallo de exportación, notificación o almacenamiento de informe no altera resultados ni estados académicos.
- La corrección posterior al cierre exige reapertura o rectificación controlada y nunca modifica instantáneas históricas.

## Requirements

### Functional Requirements

- **FR-041 — Estrategias validadas**: El sistema DEBE ejecutar únicamente mecanismos de valoración registrados, versionados y validados; una regla ausente o no validada debe quedar pendiente.
- **FR-042 — Catálogo único de cinco modelos**: El sistema DEBE representar General 2024, Medicina 2018, Enfermería 2016, Odontología 2018 y Derecho matricial mediante configuraciones independientes del mismo catálogo, sin código o tablas por carrera.
- **FR-043 — Carreras**: El sistema DEBE administrar carreras y unidades académicas con código único, estado e historial, impidiendo borrar carreras referenciadas.
- **FR-044 — Procedencia normativa**: Cada versión de modelo e indicador DEBE registrar referencias al emisor, documento, edición, fecha conocida, páginas o disposiciones, estado de corroboración y relación con versiones anteriores; aplicabilidad y vigencia se registran por carrera sin inferirlas de la publicación técnica.
- **FR-045 — Jerarquía flexible**: Una versión DEBE admitir un bosque ordenado de nodos tipados y profundidad variable sin exigir subcriterios ni una ruta única.
- **FR-046 — Definición del indicador**: Cada indicador DEBE declarar naturaleza, estándar, período, fuentes requeridas, escala y mecanismo aplicable.
- **FR-047 — Regla cuantitativa**: Un indicador cuantitativo DEBE declarar variables, unidades, fórmula, umbrales y casos límite antes de habilitar cálculo automático.
- **FR-048 — Autorización del proceso**: Activar un proceso DEBE requerir una autorización institucional registrada y verificada según una política configurable.
- **FR-049 — Suspensión**: Un proceso DEBE poder suspenderse y reanudarse mediante decisiones autorizadas, motivadas y auditadas.
- **FR-050 — Planificación**: El sistema DEBE asignar responsables, revisores, evaluadores, equipos, actividades y fechas límite conservando reasignaciones y vencimientos.
- **FR-051 — Cobertura de fuente**: Toda evidencia remitida DEBE identificar al menos una fuente requerida del modelo y conservar la relación exacta en cada versión.
- **FR-052 — No aplicabilidad**: Excluir una fuente o indicador DEBE requerir regla, justificación, autoridad, fecha y vigencia, y afectar denominadores de forma reproducible.
- **FR-053 — Recopilación**: El avance de recopilación DEBE calcularse como fuentes aplicables cubiertas por evidencia remitida dividido para fuentes requeridas aplicables, multiplicado por cien.
- **FR-054 — Revisión documental**: El avance documental DEBE calcularse como fuentes obligatorias aplicables cuya cardinalidad se satisface con coberturas aceptadas vigentes dividido para fuentes obligatorias aplicables, multiplicado por cien; toda decisión documental debe distinguir los resultados por cobertura y derivar el estado global sin duplicar su fuente de verdad.
- **FR-055 — Separación académica**: Ninguna carga, remisión o aceptación documental DEBE inferir cumplimiento académico.
- **FR-056 — Valoración cualitativa**: Una valoración cualitativa DEBE usar una opción de la escala del indicador, justificación y coberturas pertinentes aceptadas; el estado global de una evidencia con otras fuentes observadas no sustituye la decisión por cobertura.
- **FR-057 — Valoración cuantitativa**: Un cálculo DEBE conservar regla, versión, variables verificadas, resultado, umbral y responsable de validación.
- **FR-058 — Consolidación**: La consolidación DEBE mostrar completos, pendientes, discrepancias, fortalezas y debilidades sin agregaciones no autorizadas.
- **FR-059 — Hallazgos**: Cada hallazgo DEBE clasificarse, justificarse y vincularse con indicadores y sustentos.
- **FR-060 — Informe preliminar**: El sistema DEBE versionar informe preliminar, socialización, observaciones y respuestas.
- **FR-061 — Informe final**: El sistema DEBE registrar remisión, devolución y aprobación institucional de cada versión final sin denominarla acreditación.
- **FR-062 — Mejora**: Una acción de mejora DEBE registrar hallazgo de origen, responsable, plazo, meta, estado y medio de verificación.
- **FR-063 — Seguimiento de mejora**: Los avances y modificaciones de una acción DEBEN ser versionados y auditados.
- **FR-064 — Cierre documental MVP**: El MVP DEBE permitir congelar un corte de seguimiento sin cambiar estado y cerrar la fase documental según política con pendientes/excepciones explícitos, sin exigir valoraciones académicas finales.
- **FR-065 — Cierre integral**: El cierre integral DEBE usar una política versionada que declare sus prerrequisitos y excepciones.
- **FR-066 — Archivo histórico**: Un proceso cerrado DEBE conservar versiones exactas y permitir consulta autorizada sin modificación ordinaria.
- **FR-067 — Competencias**: Las decisiones institucionales DEBEN registrar el actor, función y autoridad aplicada sin atribuir competencias automáticamente.
- **FR-068 — No acreditación**: Tableros, informes y exportaciones DEBEN declarar que los resultados apoyan la autoevaluación y no constituyen acreditación.
- **FR-069 — Tipos de fuente**: Las fuentes DEBEN admitir archivo, enlace, dato estructurado o referencia institucional sin suponer que todas son PDF.
- **FR-070 — Evaluación externa**: El alcance inicial solo registra autoevaluación interna; cualquier resultado externo futuro requerirá un contrato y autorización independientes.

### Key Entities

`UnidadAcademica`, `Carrera`, `ModeloEvaluacion`, `VersionModelo`, `NodoModelo`, `Indicador`, `Estandar`, `FuenteRequerida`, `ReglaValoracion`, `VariableValoracion`, `EscalaValoracion`, `ProcesoEvaluacion`, `AutorizacionProceso`, `ActividadProceso`, `Asignacion`, `Evidencia`, `CoberturaFuente`, `VersionEvidencia`, `RevisionDocumental`, `Valoracion`, `ResultadoIndicador`, `Hallazgo`, `VersionInforme`, `ObservacionInforme`, `DecisionAprobacion`, `PlanMejora`, `AccionMejora`, `AvanceAccion`, `CierreProceso`, `EventoAuditoria`.

Se incluyen `ReferenciaNormativa`, `AplicabilidadModeloCarrera`, `Equipo`, `MembresiaEquipo`, `ValorVariable`, `EjecucionCalculo`, `DiscrepanciaAcademica` y `DecisionPorCobertura`. Son relaciones identificables; un estándar puede representarse como definición del indicador y no requiere una tabla o nivel padre adicional.

## Success Criteria

### Measurable Outcomes

- **SC-011**: Las cinco configuraciones representativas se publican usando el mismo motor y el 100 % conserva procedencia, jerarquía y fuentes sin mezcla entre modelos.
- **SC-012**: En todos los casos de prueba, los porcentajes de recopilación y revisión coinciden con el cálculo manual por fuentes aplicables.
- **SC-013**: El 100 % de aceptaciones documentales deja la valoración académica sin cambios.
- **SC-014**: El 100 % de cálculos cuantitativos conserva las entradas y la regla exactas; reglas no validadas producen cero resultados automáticos.
- **SC-015**: Todo informe aprobado puede rastrearse hasta hallazgos, valoraciones, decisiones documentales, evidencias, fuentes y versión del modelo.
- **SC-016**: Toda acción de mejora puede rastrearse hasta su hallazgo y conserva todas sus revisiones y verificaciones.
- **SC-017**: El MVP documental P01–P11 puede demostrarse sin ejecutar P12–P15.
- **SC-018**: El 100 % de cierres y reaperturas conserva una instantánea histórica reproducible y las autorizaciones aplicadas.

## Assumptions and institutional decisions

- Se implementan como configuraciones iniciales las cinco versiones documentales suministradas; su vigencia para nuevos procesos se mantiene como dato pendiente de validación institucional.
- El producto apoya autoevaluación interna. No registra evaluación externa en el alcance inicial.
- Las valoraciones manuales pueden registrarse cuando la metodología lo permita; el cálculo automático solo se habilita para reglas validadas.
- El MVP demuestra una carrera piloto y estructuras representativas; cargar la totalidad de indicadores se planifica como trabajo de datos verificable, no como cinco implementaciones.
- La Resolución 037-R-UNL-2022 y cada documento CACES deben incorporarse al catálogo de fuentes normativas antes de declarar una configuración institucionalmente válida.

