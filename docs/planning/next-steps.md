# Siguientes pasos de implementación

Fecha: 2026-10-21. Estado: **plan de trabajo pendiente**, no funcionalidades implementadas.

Actualización D1: catálogo implementado y verificado técnicamente con datos sintéticos;
véase [evidencia D1](../../tests/acceptance/results/d1-careers.md). D2–D8 siguen
pendientes; las cifras de backlog siguientes incluyen las tareas ya cerradas.

## Punto de partida y alcance

La [revisión de implementación](../testing/implementation-review.md) es la línea base: acceso y sesiones, modelos con estructura heredada, autorización inicial del proceso, asignaciones individuales y borradores de metadatos. Las correcciones de permisos, estados, concurrencia y auditoría se conservan; no se reconstruyen desde cero.

El backlog ejecutable único es [005/tasks.md](../../specs/005-complete-evaluation-workflows/tasks.md): **126 tareas en 10 historias**, más los prerrequisitos pendientes de [004/tasks.md](../../specs/004-submit-evidence/tasks.md). Los IDs se citan siempre con su paquete: `005/T051` y `004/T051` son tareas distintas. No se duplican aquí sus checklists ni se marcan tareas completas por redactar este documento.

Objetivo: entregar primero **D1–D6, MVP documental P01–P11 y consulta histórica básica de P16**; después D7–D8 para completar evaluación, informes, mejora y archivo. Un único sistema soportará los cinco modelos mediante configuraciones, no módulos por carrera.

## Orden de ejecución

```text
Preparación y bases compartidas
          ↓
 D1 Carreras + D2 Modelos
          ↓
 D3 Procesos y planificación
          ↓
 D4 Evidencias y remisión
          ↓
 D5 Revisión y subsanación
          ↓
 D6 Seguimiento y cierre documental → aceptación del MVP
          ↓
 D7a Valoración → D7b Hallazgos e informes
          ↓
 D8 Mejora → cierre integral y archivo
```

D1 y D2 pueden desarrollarse en paralelo después de estabilizar contratos y bases comunes. La aceptación de D3 depende de ambos; D7 no es dependencia del MVP. La seguridad, auditoría y pruebas se completan con cada entrega, no al final.

## Preparación inmediata

1. **Compatibilidad de contratos — 005/T113 antes de T002.** Inventariar rutas, nombres de campos, códigos HTTP, paginación y ETags del contrato ejecutable frente al objetivo. Resolver expresamente la transición de `ProcessStartAuthorizationCommand/View` al gobierno completo de decisiones; no crear dos controladores con la misma ruta. Registrar decisiones en [migration-p01-p16.qmd](../technical/migration-p01-p16.qmd).
2. **Migraciones — 005/T005 y T012.** V008 incorpora el catálogo; V009/V010 se reservan para modelos/gobierno y V011 para inmutabilidad de evidencias, antes de V012–V018. Comprobar el historial del entorno objetivo y números libres antes de crear archivos nuevos. No editar migraciones históricas ni aplicar futuras migraciones inferiores a una versión aceptada.
3. **Prerrequisitos — 005/T115.** Registrar qué falta de 004/T027–T088: almacenamiento, validación, referencias, remisión, descarga, sucesión, recuperación y auditoría. Solo la comprobación de prerrequisitos es inmediata; su implementación se integra en D4/D5, sin bloquear innecesariamente D1/D2.
4. **Bases compartidas — 005/T001, T003–T011, T114.** Matriz requisito/prueba, límites de módulos, capacidades sobre `OperationCode`, procedencia de decisiones y fixtures normativos identificados. Implementar la parte necesaria para la entrega siguiente y extenderla en las posteriores; no exigir que P12–P16 estén construidos para aceptar D1.
5. **Decisiones institucionales.** Registrar responsables y estado de CL-011–CL-017 en [domain-rules.md](../requirements/domain-rules.md). Lo no corroborado permanece pendiente; el piloto usa datos y políticas sintéticas explícitas. No inventar vigencia, competencias ni fórmulas.

## Módulos y funcionalidades por entrega

### D1 — Carreras y ámbitos · P01–P02 · US1

**Actores:** administrador funcional; usuarios dentro de sus ámbitos.

**Módulos:** `identityaccess`, `selfassessment`; frontend `access` y `careers`.

**Funcionalidades:**

- Crear y consultar unidades académicas y carreras, con búsqueda/paginación.
- Validar código normalizado único y pertenencia a unidad.
- Actualizar y desactivar, sin eliminar carreras referenciadas.
- Conservar historial de relaciones y asignaciones; impedir nuevos procesos sobre carreras inactivas sin alterar consultas históricas.
- Mantener login, expiración, desactivación y permisos; sustituir UUIDs precargados por selectores de carreras autorizadas donde corresponda.

**Tareas:** 005/T013–T020. Rutas principales: `selfassessment/domain/Career.java`, `application/CareerAdministrationService.java`, `api/CareerController.java`; `frontend/src/app/features/careers/pages/career-catalog.page.ts`.

**Cierre verificable:** registrar una carrera, rechazar duplicados, vincularla a un proceso y desactivarla sin borrar historial ni exponer datos de otra carrera. Evidencia: `tests/acceptance/results/d1-careers.md`.

### D2 — Modelos configurables y fuentes · P03–P04 · US2

**Actores:** administrador de modelos, revisor metodológico y aprobador autorizado.

**Módulo:** `evaluationmodel`; frontend `models`.

**Funcionalidades:**

- Migrar la jerarquía rígida a nodos tipados con profundidad variable; admitir criterio → indicador y rechazar ciclos.
- Configurar por indicador estándar, naturaleza, período, fuentes requeridas y cardinalidad.
- Definir escalas cualitativas y variables, unidades, fórmulas y umbrales cuantitativos, sin exigir una fórmula o escala universal.
- Registrar documento, emisor, edición, disposiciones y estado de verificación por versión/indicador; separar publicación de aplicabilidad por carrera.
- Mantener versiones publicadas inmutables y crear sucesoras sin modificar procesos históricos.
- Cargar configuraciones representativas de General, Medicina, Enfermería, Odontología y Derecho; distinguir fixtures sintéticos de modelos institucionalmente corroborados.

**Tareas:** 005/T021–T030 y T116; manifiestos T006/T114. Extender `domain/ModelNode.java`, `domain/PublicationValidator.java`, `application/EvaluationModelService.java`, `infrastructure/ModelCatalogAdapter.java` y el editor existente `frontend/src/app/features/models/pages/model-editor.page.ts`.

**Cierre verificable:** las cinco estructuras se leen/publican con el mismo motor; un indicador directo y otro anidado conservan fuentes y períodos; una versión no corroborada no se presenta como vigente. Evidencia: `tests/acceptance/results/d2-five-models.md`.

### D3 — Gobierno y planificación · P05–P06 · US3

**Actores:** coordinador, autoridad de inicio, responsables y miembros de equipos.

**Módulos:** `selfassessment`, `identityaccess`; frontend `processes`.

**Funcionalidades:**

- Completar la política de inicio, suspensión, reanudación y excepciones sobre la autorización inicial ya implementada.
- Verificar carrera activa y aplicabilidad de la versión antes de activar; conservar versión exacta e historial de decisiones.
- Crear equipos y membresías vigentes; asignar exactamente una persona o equipo por asignación.
- Planificar actividades, dependencias y fechas; identificar vencimientos y conservar reasignaciones.
- Impedir autor/revisor incompatibles salvo excepción expresa; individualizar al actuante aunque participe mediante un equipo.
- Mostrar «mis tareas», actividades del coordinador y referencias de autorización dentro del ámbito permitido.

**Tareas:** 005/T031–T039 y T117. Reutilizar `SelfAssessmentService`, `ProcessTransitionService`, `ProcessAssignmentService`, `InstitutionalAuthorityPort` y controles existentes; añadir `ProcessPlanningService` y la pantalla de planificación sin duplicar rutas de autorización.

**Cierre verificable:** preparar → autorizar → activar → asignar equipo/actividad → suspender → reanudar. Rechazar membresía vencida, asignación persona+equipo, ciclos y plazos inválidos. Evidencia: `tests/acceptance/results/d3-process-planning.md`.

### D4 — Evidencias, fuentes y remisión · P07–P08 · US4

**Actor:** responsable documental autorizado.

**Módulos:** `evidence`, `filestorage`, `audit`; frontend `evidence`.

**Funcionalidades:**

- Asociar cada versión a fuentes requeridas explícitas del modelo fijado; reutilizar un archivo sin inventar cobertura de otras fuentes.
- Cargar archivos con validación de contenido, tamaño máximo de 25 MiB y SHA-256; almacenarlos fuera de PostgreSQL.
- Registrar enlaces, datos estructurados y referencias institucionales, con período/unidad cuando corresponda; no interpretar un enlace como contenido descargado o validado.
- Implementar staging, promoción, recuperación idempotente y errores de almacenamiento sin falsas referencias disponibles.
- Remitir una versión completa e inmutable mediante transacción con auditoría; revalidar permisos, estado, cobertura y ETag.
- Consultar/descargar solo contenido autorizado y verificado; registrar no aplicabilidad mediante decisión justificada y competente.

**Tareas:** 005/T040–T049 y T118; prerrequisitos de 004/T027–T066 y auditoría/recuperación aplicable de 004/T067–T088. D4 entrega la sucesión genérica de versiones de 004/T057–T066; D5 la vincula con las observaciones del revisor. No se declara D4 aceptada sin los prerrequisitos exigidos por T115.

**Cierre verificable:** cargar y remitir una evidencia para fuentes concretas, impedir su edición posterior y verificar descarga/autorización e integridad. Simular interrupción de carga y recuperación. Evidencia: `tests/acceptance/results/d4-source-coverage.md`.

### D5 — Revisión y subsanación · P09–P10 · US5

**Actores:** revisor documental y responsable documental, con funciones separadas.

**Módulos:** `documentreview`, `evidence`, `audit`; frontend `reviews` y `evidence`.

**Funcionalidades:**

- Bandeja paginada y filtrada de versiones remitidas pendientes.
- Aceptar, observar o rechazar por cobertura; derivar el estado global sin mantener dos fuentes de verdad.
- Exigir fundamento para observaciones/rechazos y bloquear autorrevisión no exceptuada.
- Crear sucesora desde una versión remitida, conservar archivos/referencias/historial y vincular la corrección con la revisión que la originó.
- Resolver concurrencia sin dobles decisiones vigentes; una rectificación añade otra decisión.
- No heredar aceptación al remitir la sucesora ni crear valoración académica al aceptar documentación.

**Tareas:** 005/T050–T057 y T119; sucesión y concurrencia de 004/T057–T066. Rutas principales: `documentreview/application/DocumentReviewService.java`, `evidence/application/CreateEvidenceSuccessorService.java` y `frontend/src/app/features/reviews/pages/document-review-inbox.page.ts`.

**Cierre verificable:** rechazar v1 → crear y remitir v2 → aceptar v2; v1 y sus decisiones permanecen idénticas y el resultado académico no cambia. Evidencia: `tests/acceptance/results/d5-document-review.md`.

### D6 — Seguimiento y cierre documental · P11 y P16 básico · US6

**Actores:** coordinador, supervisor y consultor autorizado.

**Módulo:** `reporting`, mediante puertos de modelos, evidencias y revisión; frontend `reporting`.

**Funcionalidades:**

- Tablero de recopilación y revisión separados, con pendientes y desglose por criterio, indicador, fuente y responsable.
- Contar fuentes obligatorias aplicables distintas; no contar borradores, fuentes opcionales ni exclusiones sin autorización.
- Aplicar cardinalidad, versiones candidatas vigentes y reglas temporales; evitar división entre cero y doble conteo.
- Crear corte de seguimiento sin transición y cierre documental mediante política propia, pendientes/excepciones y snapshot verificable.
- Consultar históricamente ese corte con los mismos filtros de permisos, sin depender de evaluación, informes o mejora.

**Tareas:** 005/T058–T067 y T120. Rutas principales: `reporting/domain/DocumentaryProgressCalculator.java`, `application/DocumentaryClosureService.java` y `frontend/src/app/features/reporting/pages/documentary-dashboard.page.ts`.

**Cierre verificable:** cuatro fuentes aplicables, tres remitidas y dos aceptadas muestran **75 % de recopilación, 50 % de revisión y valoración académica pendiente**. Repetir con fuentes excluidas, cardinalidad y sucesoras; aceptar el MVP con P12–P15 deshabilitados. Evidencia: `tests/acceptance/results/d6-documentary-mvp.md`.

### D7a — Valoración académica · P12 · US7

**Actores:** evaluador y comité competente.

**Módulo:** `academicevaluation`; frontend `evaluations`.

**Funcionalidades:** valoración cualitativa fundamentada según escala propia; captura/verificación de variables; registro manual permitido por metodología antes de automatizar; cálculo solo con regla/estrategia validada; conservación de entradas, período, fórmula, umbral y responsable; discrepancias y conciliación sin sobrescribir juicios previos; señales de sustento invalidado.

**Tareas:** 005/T068–T076 y T121. **Cierre verificable:** valorar un indicador cualitativo y calcular uno cuantitativo validado; rechazar variables faltantes y reglas no validadas. Evidencia: `tests/acceptance/results/d7-academic-evaluation.md`.

### D7b — Hallazgos e informes · P13–P14 · US8

**Actores:** comisión de evaluación, participantes en socialización y autoridad aprobadora.

**Módulo:** `governance`; frontend `reports`. `reporting` sigue siendo consulta documental, no dueño de aprobaciones.

**Funcionalidades:** consolidación con resultados pendientes explícitos; fortalezas/debilidades vinculadas a sus sustentos; informe preliminar; socialización, observaciones y respuestas; correcciones como nuevas versiones; remisión, devolución y aprobación de versión final por competencia verificada. Ninguna aprobación equivale a acreditación ni permite editar contenido emitido.

**Tareas:** 005/T077–T085 y T122. **Cierre verificable:** preliminar → observaciones → final → devolución/corrección → aprobación, con trazabilidad hasta fuentes y evidencias exactas. Evidencia: `tests/acceptance/results/d7-reports.md`.

### D8 — Mejora y archivo integral · P15–P16 · US9–US10

**Actores:** responsable de mejora, aprobador, verificador, coordinador y archivista autorizado.

**Módulos:** `improvement`, `archive`; frontend homónimos.

**Funcionalidades de mejora:** acciones desde hallazgos, responsable, plazo, meta y medio de verificación; aprobación separada de ejecución/verificación; avances versionados; retrasos derivados sin alterar fechas históricas; continuidad posterior al cierre cuando la política la permita.

**Funcionalidades de archivo:** vista previa de bloqueantes; política integral versionada, excepciones explícitas y revalidación transaccional; snapshot/hash; conservación y consulta de versiones exactas; respaldo/restauración conjunta de base y archivos; reapertura/rectificación controlada, sin sobrescribir el cierre anterior.

**Tareas:** mejora 005/T086–T093 y T123; archivo 005/T094–T102 y T124. **Cierre verificable:** acción propuesta → aprobada → avance → verificación; bloquear cierre incompleto, completar o exceptuar pendientes, archivar/restaurar y crear una nueva autoevaluación sin alterar la anterior. Evidencias: `tests/acceptance/results/d8-improvement.md` y `d8-archive.md`.

## Primer bloque de trabajo a implementar

La siguiente ejecución debe empezar con **compatibilidad y catálogo de carreras**, no con dashboard o fórmulas:

| Orden | Tareas | Resultado concreto |
|---|---|---|
| 1 | 005/T113, T005, T115 | Colisiones de contrato, migraciones y prerrequisitos identificados. |
| 2 | Bases comunes necesarias: 005/T001, T003, T007–T012 | Permisos/decisiones/arquitectura listos para D1, conservando las regresiones existentes. |
| 3 | 005/T013–T014 | Pruebas de carrera duplicada, unidad, desactivación, historial y autorización negativa. |
| 4 | 005/T015–T018 | Migración, dominio, servicio y API de carreras/unidades. |
| 5 | 005/T019–T020 | Pantalla de carreras y recorrido de aceptación D1 con evidencia. |
| 6 | 005/T021–T030, T116, con T006/T114 | Modelo flexible y fuentes; puede prepararse en paralelo con D1 sin compartir archivos modificados. |

Las bases futuras se extienden por entrega; no es necesario implementar informes o archivo para comenzar carreras. Antes de codificar, ejecutar análisis cruzado de especificación/plan/tareas y resolver hallazgos críticos según la constitución.

## Puerta de calidad de cada entrega

Una entrega se considera terminada solo cuando tenga migración PostgreSQL desde cero y desde el esquema aceptado, pruebas de dominio/servicio/API y permisos negativos, interfaz accesible, recorrido de navegador aplicable, contratos/clientes compatibles, auditoría segura, manuales y trazabilidad actualizados. Los errores, divisores cero, concurrencia y fallos de almacenamiento deben probarse cuando pertenezcan a la entrega.

005/T103–T112 y T125–T126 se ejecutan progresivamente: documentación, accesibilidad, rendimiento, contratos/Postman, migraciones, datos sintéticos, escenarios y revisión humana. No marcar `PASS` por pasar únicamente el lint ni tratar pruebas pasadas como evidencia de funciones nuevas.

## Tamaño del backlog y oportunidades de trabajo paralelo

| Grupo | Historia | Tareas de 005 |
|---|---|---:|
| Preparación, bases y verificación transversal | Sin US | 27 |
| D1 — Carreras | US1 | 8 |
| D2 — Modelos | US2 | 11 |
| D3 — Procesos | US3 | 10 |
| D4 — Cobertura de fuentes | US4 | 11 |
| D5 — Revisión | US5 | 9 |
| D6 — Seguimiento | US6 | 11 |
| D7a — Valoración | US7 | 10 |
| D7b — Hallazgos/informes | US8 | 10 |
| D8 — Mejora | US9 | 9 |
| D8 — Archivo | US10 | 10 |
| **Total** | **10 historias** | **126** |

El total no suma las tareas prerrequisito de 004 ni representa esfuerzo estimado o porcentaje pendiente. Las correcciones existentes son parciales dentro de varias tareas, no entregas completas.

Ejemplos de paralelismo, sin edición simultánea de archivos compartidos: US1 pruebas de contrato/dominio; US2 pruebas de jerarquía/publicación/fixtures; US3 ciclo de proceso y planificación; US4 cobertura/no aplicabilidad/API; US5 dominio de revisión y bandeja; US6 cálculo, consultas y cierre; US7 reglas y variables; US8 hallazgos e informes; US9 dominio y autorización; US10 política de cierre e integridad histórica. Los servicios/UI se integran después de estabilizar sus contratos y dependencias.

No se fijan fechas de calendario sin conocer capacidad del equipo y disponibilidad de validación institucional. Despliegue productivo, integraciones externas, notificaciones y acreditación no se añaden a este alcance.
