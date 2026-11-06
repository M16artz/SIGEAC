# Revisión del backend de SIGEAC — DocumentacionInformes

Fecha: 6 de noviembre de 2026. Versión revisada: **14bb3e6643a0ffaa64b10580ff87395a171bc776**.

## Verificación posterior contra GitHub

Se consultaron directamente las cabeceras remotas mediante `git ls-remote --heads origin`. DocumentacionInformes sigue apuntando exactamente a 14bb3e66: la revisión sí cubre la última versión publicada de esa rama al momento de la consulta. Sin embargo, **no cubre todos los cambios posteriores publicados en otras ramas**.

La cabecera con fecha de commit más reciente es **Cambios, b4141c31792baba57acdf979cbc84544b66d7514**, del 5 de noviembre de 2026 a las 20:39:59 -05:00. Git no proporciona aquí la hora del push. main sigue en faf868ea y Richard_ChambaS en 1f970caf. DocumentacionInformes no es antecesora directa de Cambios; se compararon sus contenidos completos.

Entre DocumentacionInformes y Cambios, el único archivo de producción del backend modificado es SessionService.java: registra los rechazos de autenticación en la misma transacción, con documentación de la corrección para SQLite. También cambian SessionServiceTest y SqliteApplicationContextTest. Las migraciones, el pom y los demás archivos de producción del backend no cambian; por comparación de código, los hallazgos principales sobre ellos siguen siendo aplicables. **Las pruebas de este informe se ejecutaron en 14bb3e66, no se han vuelto a ejecutar en b4141c31.**

Cambios incorpora modificaciones amplias del frontend y elimina del índice los artefactos de backend/target y frontend/node_modules: en esa rama el conteo es cero. Por tanto, la observación de higiene del repositorio queda resuelta allí; las observaciones de interfaz necesitan contrastarse con la nueva versión. No se hizo pull ni se cambió ninguna rama durante esta verificación.

## Dictamen

El backend contiene una implementación amplia de P01–P16: acceso, modelos, procesos, evidencias, revisión, seguimiento, evaluación académica, informes, mejora y archivo. No corresponde describir D5–D8 como módulos ausentes en esta rama. Hay una base aprovechable y reglas de negocio bien separadas, pero **todavía hay defectos de autorización, conservación de evidencias, actualización de bases y consistencia de flujos que impiden considerarlo listo para operar con datos institucionales**.

La prioridad es cerrar esos defectos y hacer que las pruebas los detecten. No hace falta reconstruir el sistema ni iniciar una reestructuración general.

## Versión y alcance real

Se detectaron dos copias distintas del proyecto:

| Copia | Estado observado | Uso en esta revisión |
|---|---|---|
| C:/Users/ariel/OneDrive/Documentos/Desarollo Basado/SIGEAC | Richard_ChambaS, 17ed5078 | Espacio de trabajo inicial; versión anterior. |
| C:/Users/ariel/OneDrive/Documentos/DesarrolloBP/SIGEAC | Richard_ChambaS, 1f970caf; referencia origin/DocumentacionInformes disponible | Origen de la versión solicitada. |
| Copia aislada bajo backend/target/review-20261106/documentacion-informes | Contenido extraído de 14bb3e66 | Código y documentación usados para las conclusiones finales. |

No fue necesario otro pull para inspeccionar **esa versión concreta**. Esto no verifica si posteriormente aparecieron nuevos commits en el servidor. No se cambiaron ramas, no se hicieron merges, commits ni push. Se conservaron los dos repositorios de origen; únicamente se agregó este informe al espacio de trabajo y se generaron archivos temporales de revisión dentro de target.

Se inventariaron 311 archivos Java de producción, 129 archivos *Test.java, 14 paquetes principales y 27 migraciones SQL, hasta V026 incluida V016.1. Se revisaron las rutas, servicios centrales, controles de acceso, transiciones, repositorios y migraciones relevantes, configuración, contratos, pruebas y documentación. El frontend se inspeccionó para contrastar llamadas y pantallas relacionadas. Esto no equivale a una inspección exhaustiva de cada línea de todos los archivos.

Los documentos adjuntos y del repositorio se trataron como material a contrastar. Los resultados históricos no se presentaron como ejecuciones nuevas. El audio adjunto no fue transcrito ni usado como fuente de requisitos adicionales.

## Arquitectura y recorrido de los datos

Es un monolito modular Spring Boot. La API usa /api/v1; el código separa controladores, servicios de aplicación, dominio e infraestructura. PostgreSQL es la base principal; SQLite ofrece una variante local con esquema y protecciones propios. Flyway gestiona el historial PostgreSQL. El almacenamiento de archivos mantiene metadatos y hashes en base de datos y contenido en disco.

~~~mermaid
flowchart TD
    A[Identidad y permisos por ámbito] --> B[Carreras y modelo publicado]
    B --> C[Proceso autorizado y planificación]
    C --> D[Borrador de evidencia y fuentes]
    D --> E[Remisión de versión]
    E --> F[Revisión por cobertura]
    F -->|Observaciones| G[Borrador sucesor]
    G --> E
    F --> H[Seguimiento y cierre documental]
    F --> I[Valoración académica y conciliación]
    I --> J[Hallazgos e informes]
    J --> K[Plan y acciones de mejora]
    H --> L[Cierre integral y archivo]
    K --> L
    A -. Autorización y auditoría transversal .-> L
~~~

El diagrama resume relaciones; la evaluación académica no es un prerrequisito del MVP documental. Tampoco sustituye las transiciones exactas de cada servicio.

| Bloque | Qué existe | Evaluación de la revisión |
|---|---|---|
| Identidad y auditoría | Sesiones, asignaciones, capacidades, ámbitos, excepciones y eventos | Mecanismos presentes; aplicación desigual del ámbito y dos vías de consulta demasiado amplias. |
| Carreras y modelos | Catálogo, publicación, configuración por indicador, fuentes y procedencia normativa | Implementado; aplicabilidad permite eludir ámbito y tiene un bloqueo indebido al reutilizar referencias verificadas. |
| Procesos | Preparación, autorización, activación, suspensión, equipos y planificación | Implementado; controles explícitos de estado y autoridad en servicios principales. |
| Evidencias | Versiones, archivos, enlaces, fuentes estructuradas, remisión y sucesión | Implementado; fallos importantes en remisión, protección SQL y corrección de datos estructurados. |
| Revisión documental | Bandeja, decisiones por fuente, rectificaciones y correcciones | Implementado; lectura sin ámbito, aceptación incompleta y desacuerdo sobre la revisión que se debe enviar. |
| Seguimiento y cierre documental | Cardinalidad por fuente, cortes inmutables, cierre y hashes | Separación correcta entre recopilación, aceptación documental y valoración académica; pruebas de concurrencia de cierre pasan en PostgreSQL. |
| Evaluación académica | Valoración manual, variables verificadas, cálculo y conciliación | Hay validación por indicador y sustentos aceptados; cálculo usa estrategia explícita y precisión declarada, no fórmulas arbitrarias. |
| Hallazgos e informes | Registro, consolidación, versiones, observaciones, respuesta y aprobación | Implementado; falta verificar algunos vínculos a sustentos. Aprobación comprueba asignación competente. |
| Mejora y archivo | Planes, avances, verificación, cierre integral y conservación | Implementado; avances permiten procesos suspendidos y sustentos inexistentes. Cierre/archivo comprueban ámbito, autoridad y hash de la instantánea. |

## Hallazgos prioritarios

P1: corregir antes de usar datos reales o actualizar una instalación existente. P2: corregir antes de declarar completado y aceptado el flujo correspondiente. Las reproducciones de servicios usaron código real con repositorios/permisos sintéticos; no se presentan como ataques ejecutados por HTTP.

### H01 · P1 · Una base de la versión anterior no puede actualizarse normalmente

La rama cambia V011 y sustituye V015__submitted_evidence_guards.sql por V015__document_review.sql. Una base creada con las migraciones del checkout 17ed5078 falla al validar las sumas de V011 y V015 contra DocumentacionInformes, incluso ignorando correctamente las nuevas migraciones pendientes. **Reproducido en PostgreSQL 16.14**. Una instalación desde cero sí aplica las 27 migraciones; eso no demuestra compatibilidad con la instalación anterior.

Fuentes: [V011__evidence_immutability.sql:4](https://github.com/M16artz/SIGEAC/blob/14bb3e6643a0ffaa64b10580ff87395a171bc776/backend/src/main/resources/db/migration/V011__evidence_immutability.sql#L4); [V015__document_review.sql:9](https://github.com/M16artz/SIGEAC/blob/14bb3e6643a0ffaa64b10580ff87395a171bc776/backend/src/main/resources/db/migration/V015__document_review.sql#L9). Evidencia: prueba de actualización e inmutabilidad (evidencia local de la revisión; no incluida en este commit).

Corrección: inventariar los historiales realmente instalados, conservar las migraciones históricas de cada línea soportada y diseñar una migración de reconciliación hacia delante. No resolverlo borrando datos ni aceptando checksums distintos sin analizar las diferencias de esquema y protecciones.

### H02 · P1 · La base permite modificar contenido de una evidencia remitida

La protección de V011 solo impide cambiar el estado SUBMITTED a otro estado. Permite modificar el título conservando SUBMITTED. Se creó una evidencia sintética, se remitió y se ejecutó UPDATE del título: **PostgreSQL aceptó el cambio con todas las migraciones de la rama aplicadas**. Es una brecha de defensa en la base; no se afirma que el endpoint ordinario de edición permita ese mismo cambio.

Fuente: [V011__evidence_immutability.sql:12](https://github.com/M16artz/SIGEAC/blob/14bb3e6643a0ffaa64b10580ff87395a171bc776/backend/src/main/resources/db/migration/V011__evidence_immutability.sql#L12). Corrección: proteger todos los campos inmutables y sus relaciones mediante una nueva migración; probar también traslado de hijos entre versiones y escrituras concurrentes. La prueba existente no llega a comprobar esto por un error de preparación de datos, descrito más abajo.

### H03 · P1 · Administración de asignaciones sin comprobar el ámbito destino

IdentityAdministrationService comprueba ACCESS_ASSIGN_SCOPE con hasOperation, pero no autoriza la carrera/proceso destino. Un administrador con permiso limitado a la carrera A pudo persistir una asignación para B en la reproducción. Además, la ruta de asignación creó una excepción de conflicto con un motivo de texto, sin exigir ACCESS_MANAGE_EXCEPTIONS, aunque el endpoint específico de excepciones sí exige esa capacidad.

Fuentes: [IdentityAdministrationService.java:157](https://github.com/M16artz/SIGEAC/blob/14bb3e6643a0ffaa64b10580ff87395a171bc776/backend/src/main/java/ec/edu/unl/sec/identityaccess/application/IdentityAdministrationService.java#L157); [IdentityAdministrationService.java:204](https://github.com/M16artz/SIGEAC/blob/14bb3e6643a0ffaa64b10580ff87395a171bc776/backend/src/main/java/ec/edu/unl/sec/identityaccess/application/IdentityAdministrationService.java#L204). Corrección: comprobar el ámbito destino y aplicar el mismo permiso de excepciones en todas las entradas. Reproducciones confirmadas a nivel de servicio.

### H04 · P1 · Aplicabilidad de modelos fuera de la carrera autorizada

ModelApplicabilityService exige la capacidad general y que la carrera exista, pero no autoriza esa carrera concreta. Se autorizó un modelo para B con un actor limitado a A. Fuente: [ModelApplicabilityService.java:64](https://github.com/M16artz/SIGEAC/blob/14bb3e6643a0ffaa64b10580ff87395a171bc776/backend/src/main/java/ec/edu/unl/sec/evaluationmodel/application/ModelApplicabilityService.java#L64). Corrección: usar la decisión de autorización con el ámbito destino antes de registrar la aplicabilidad. Reproducido a nivel de servicio.

### H05 · P1 · La consulta de auditoría de modelos puede devolver eventos ajenos

Sin modelId/versionId, ModelAuditController envía filtros de tipo y objeto nulos a una consulta de alcance global. Un actor con MODEL_AUDIT_READ, sin ACCESS_READ_AUDIT, obtuvo un evento SESSION_LOGIN. **Reproducido con el controlador real y una consulta SQLite real**. No se probó por HTTP.

Fuente: [ModelAuditController.java:19](https://github.com/M16artz/SIGEAC/blob/14bb3e6643a0ffaa64b10580ff87395a171bc776/backend/src/main/java/ec/edu/unl/sec/evaluationmodel/api/ModelAuditController.java#L19). Corrección: limitar siempre los tipos de evento del módulo y aplicar ámbitos; los filtros opcionales no deben ampliar la autorización.

### H06 · P1 · Lectura de decisiones documentales sin ámbito

GET /evidence-versions/{versionId}/document-review llama a versionReview, que solo comprueba que exista un actor y consulta la última decisión. No verifica carrera, proceso ni permiso de lectura. Se recuperó la decisión y sus resultados aunque canReview denegaba el acceso. Fuente: [DocumentReviewService.java:117](https://github.com/M16artz/SIGEAC/blob/14bb3e6643a0ffaa64b10580ff87395a171bc776/backend/src/main/java/ec/edu/unl/sec/documentreview/application/DocumentReviewService.java#L117). Corrección: verificar el acceso a la versión para los actores de lectura admitidos antes de consultar/devolver datos. No basta con exigir permiso de escritura de revisor a todos los lectores. Reproducido a nivel de servicio.

### H07 · P1 · Un responsable puede remitir el borrador de otro y la auditoría atribuye mal la acción

Un responsable vigente sobre el mismo indicador puede ver el borrador de otro autor. requireEditableVersion le impide editarlo, pero submit utiliza getVersion y no aplica esa comprobación de autor. La reproducción consiguió remitirlo. El evento EVIDENCE_SUBMITTED registró al autor del borrador, en lugar del actor que ejecutó la remisión.

Fuentes: [SubmitEvidenceService.java:48](https://github.com/M16artz/SIGEAC/blob/14bb3e6643a0ffaa64b10580ff87395a171bc776/backend/src/main/java/ec/edu/unl/sec/evidence/application/SubmitEvidenceService.java#L48); [EvidenceDraftService.java:161](https://github.com/M16artz/SIGEAC/blob/14bb3e6643a0ffaa64b10580ff87395a171bc776/backend/src/main/java/ec/edu/unl/sec/evidence/application/EvidenceDraftService.java#L161). Corrección: centralizar autorización de edición/remisión y tomar el actor de la sesión para la auditoría. Ambos comportamientos fueron reproducidos.

### H08 · P1 · Exclusiones de fuentes/indicadores aceptan una autoridad inexistente

ApplicabilityDecisionService recibe authorityAssignmentId, pero no verifica esa asignación al decidir una exclusión. La capacidad de coordinación no demuestra que el identificador aportado corresponda a una autoridad vigente. Se aceptó un UUID aleatorio. Estas exclusiones alteran qué fuentes entran en el seguimiento.

Fuente: [ApplicabilityDecisionService.java:131](https://github.com/M16artz/SIGEAC/blob/14bb3e6643a0ffaa64b10580ff87395a171bc776/backend/src/main/java/ec/edu/unl/sec/evidence/application/ApplicabilityDecisionService.java#L131). Corrección: aplicar una comprobación de asignación competente comparable a la utilizada en conciliación, aprobación y archivo. Reproducido a nivel de servicio.

## Otros defectos de flujo confirmados

| ID / prioridad | Caso e impacto | Evidencia y corrección |
|---|---|---|
| H09 · P2 | Una evidencia con dos coberturas recibe estado global ACCEPTED al enviar una sola cobertura aceptada; la otra queda sin resultado. Esto no implica que el calculador de avance cuente la fuente omitida como aceptada. | Reproducido. [DocumentReviewService.java:167](https://github.com/M16artz/SIGEAC/blob/14bb3e6643a0ffaa64b10580ff87395a171bc776/backend/src/main/java/ec/edu/unl/sec/documentreview/application/DocumentReviewService.java#L167). Exigir igualdad entre el conjunto de coberturas declaradas y el de resultados. |
| H10 · P2 | La rectificación rechaza la revisión devuelta por GET/POST: el servicio exige revisión anterior + 1. El frontend envía siempre If-Match: \"0\" para decidir. | Reproducido el rechazo en el servicio. [DocumentReviewService.java:66](https://github.com/M16artz/SIGEAC/blob/14bb3e6643a0ffaa64b10580ff87395a171bc776/backend/src/main/java/ec/edu/unl/sec/documentreview/application/DocumentReviewService.java#L66); [document-review-inbox.page.ts:233](https://github.com/M16artz/SIGEAC/blob/14bb3e6643a0ffaa64b10580ff87395a171bc776/frontend/src/app/features/reviews/pages/document-review-inbox.page.ts#L233). Unificar ETag de estado vigente y cargarlo antes de rectificar. |
| H11 · P2 | Hallazgos aceptan IDs inexistentes de evaluaciones/evidencias; avances de mejora aceptan IDs inexistentes de evidencia. Esos vínculos se conservan en JSON, sin una clave foránea que los compruebe. | Reproducido con servicios; repositorios contrastados. [FindingService.java:57](https://github.com/M16artz/SIGEAC/blob/14bb3e6643a0ffaa64b10580ff87395a171bc776/backend/src/main/java/ec/edu/unl/sec/governance/application/FindingService.java#L57); [ActionProgressService.java:64](https://github.com/M16artz/SIGEAC/blob/14bb3e6643a0ffaa64b10580ff87395a171bc776/backend/src/main/java/ec/edu/unl/sec/improvement/application/ActionProgressService.java#L64). Validar existencia, pertenencia, vigencia y permiso sobre cada sustento. |
| H12 · P2 | Se pudo registrar un avance de mejora con el proceso SUSPENDED. La excepción de continuación tras cierre no explica permitir escrituras durante suspensión. | Reproducido. [ActionProgressService.java:178](https://github.com/M16artz/SIGEAC/blob/14bb3e6643a0ffaa64b10580ff87395a171bc776/backend/src/main/java/ec/edu/unl/sec/improvement/application/ActionProgressService.java#L178). Autorizar estados de escritura expresamente y conservar la política específica de continuación tras cierre. |
| H13 · P2 | La sucesora copia el valor estructurado, pero registrar otro para la misma fuente devuelve STRUCTURED_SNAPSHOT_DUPLICATE; no hay operación de modificación/eliminación de ese valor. La subsanación de ese dato queda bloqueada. | Contraste estático. [CreateEvidenceSuccessorService.java:62](https://github.com/M16artz/SIGEAC/blob/14bb3e6643a0ffaa64b10580ff87395a171bc776/backend/src/main/java/ec/edu/unl/sec/evidence/application/CreateEvidenceSuccessorService.java#L62); [StructuredEvidenceService.java:77](https://github.com/M16artz/SIGEAC/blob/14bb3e6643a0ffaa64b10580ff87395a171bc776/backend/src/main/java/ec/edu/unl/sec/evidence/application/StructuredEvidenceService.java#L77). Permitir corregir datos de la sucesora en borrador, conservando intacto el original y comprobando autor, estado y revisión. |
| H14 · P2 | La primera decisión de aplicabilidad usa revisión 0, igual que la ausencia de decisión. Un segundo cliente con la revisión inicial 0 puede sustituirla sin detectar que ya cambió. | Reproducido. [ApplicabilityDecisionService.java:54](https://github.com/M16artz/SIGEAC/blob/14bb3e6643a0ffaa64b10580ff87395a171bc776/backend/src/main/java/ec/edu/unl/sec/evidence/application/ApplicabilityDecisionService.java#L54). Diferenciar estado vacío y primera revisión persistida. |
| H15 · P2 | Otro decisor autorizado no puede reutilizar referencias normativas ya verificadas por una persona distinta: se exige que verifiedBy sea el actor actual. | Reproducido. [ModelApplicabilityService.java:46](https://github.com/M16artz/SIGEAC/blob/14bb3e6643a0ffaa64b10580ff87395a171bc776/backend/src/main/java/ec/edu/unl/sec/evaluationmodel/application/ModelApplicabilityService.java#L46). Distinguir verificar/crear una referencia de reutilizar una verificación existente con procedencia conservada. |

## Pruebas ejecutadas y sus límites

Entorno: Windows, JDK 25.0.4.1 con compilación Java 21, Maven wrapper 3.9.16 y PostgreSQL temporal 16.14. La configuración CI declara PostgreSQL 18; no se verificó esa versión del motor. Mockito se cargó al iniciar la JVM y se usó un directorio temporal dentro de la copia aislada. Los ajustes fueron del entorno de ejecución, sin modificar el código ni el pom de la rama.

| Ejecución | Resultado | Interpretación |
|---|---|---|
| Suite base de la rama, con PostgreSQL deshabilitado | **563 casos: 546 aprobados, 17 omitidos, 0 fallos/errores** | Incluye pruebas de servicios, controladores, arquitectura y SQLite. Las omisiones incluyen una prueba de enlaces simbólicos no disponible en este Windows; no todas son de base de datos. |
| Activación de 16 clases con casos omitidos | **50 casos: 41 aprobados, 6 fallos, 2 errores, 1 omitido** | Se ejecutaron las variantes PostgreSQL, migraciones y concurrencia. |
| Cinco clases adicionales de persistencia, ampliadas a PostgreSQL | **33 casos: 31 aprobados, 2 errores** | Incluye valoración manual, revisión, fuentes, archivos y gobierno del proceso. |
| Pruebas específicas de esta revisión | **17 comprobaciones reproducidas** | 14 a nivel de servicios, 1 controlador con SQLite real y 2 con PostgreSQL real; no equivalen a 17 vulnerabilidades independientes ni a una campaña HTTP completa. |
| Seis archivos OpenAPI | Válidos; una advertencia por ReasonCommand legado sin uso | La validez sintáctica no garantiza correspondencia con las rutas implementadas. |
| AsyncAPI | Válido | No se ejecutó un intercambio con sistemas externos. |

Los conteos de las ejecuciones no deben sumarse como casos únicos: algunas pruebas se repiten y las pruebas parametrizadas amplían sus variantes cuando se activa PostgreSQL.

Fallos concretos de las pruebas ampliadas:

- CareerCatalogMigrationTest espera una migración adicional, pero ahora se aplican veinte.
- CareerCatalogStartupTest espera V008 y obtiene V026.
- CleanDatabaseMigrationTest espera quince migraciones y obtiene veintisiete.
- DocumentarySnapshotPersistenceTest espera V023 y obtiene V026 en tres variantes. Esos fallos ocurren antes de sus comprobaciones posteriores de conservación.
- EvidenceConcurrencyIntegrationTest y SubmittedEvidenceImmutabilityTest pasan cinco parámetros a un INSERT con cuatro marcadores. Fallan antes de comprobar lo que sus nombres prometen.
- DocumentReviewPersistenceTest envía texto a columnas DATE en PostgreSQL; dos variantes fallan preparando evidencias.

La prueba CompleteWorkflowMigrationTest sí pasó veinte escenarios de instalación/actualización con las migraciones actuales. No reproduce el historial antiguo de otra rama: por eso no detectó H01. Las dos pruebas de concurrencia de cierre documental también pasaron. Esto permite conservar partes validadas sin extrapolar sus resultados a todo el sistema.

Evidencias: suite base (evidencia local de la revisión; no incluida en este commit); pruebas PostgreSQL (evidencia local de la revisión; no incluida en este commit); persistencia adicional (evidencia local de la revisión; no incluida en este commit); permisos y aplicabilidad (evidencia local de la revisión; no incluida en este commit); flujos (evidencia local de la revisión; no incluida en este commit); migraciones y protección SQL (evidencia local de la revisión; no incluida en este commit).

## Contratos, documentación y estado de entrega

**H16 · P2 — Contrato general y controladores divergentes.** El inventario estático detectó 129 declaraciones de operación en controladores y 113 en el contrato general: 14 operaciones declaradas sin coincidencia y 30 implementadas sin declaración equivalente. Se normalizaron nombres de parámetros. Son diferencias de mapeo estático, no 44 llamadas HTTP fallidas; pueden existir rutas alternativas y dos rutas académicas heredadas responden 501 explícitamente.

Ejemplo comprobado: el contrato usa /evidence-versions/{id}/document-reviews y el controlador y frontend usan /evidence-versions/{id}/document-review. El historial/bandeja, fuentes estructuradas y acciones de mejora también necesitan conciliación. Evidencia: inventario completo de rutas (evidencia local de la revisión; no incluida en este commit). Corrección: distinguir contrato ejecutable, rutas obsoletas y objetivos futuros, y comprobar automáticamente método/ruta/esquemas frente a la aplicación.

**H17 · P2 — La verificación continua permite omitir controles esenciales.** [backend-ci.yml:4](https://github.com/M16artz/SIGEAC/blob/14bb3e6643a0ffaa64b10580ff87395a171bc776/.github/workflows/backend-ci.yml#L4) solo se ejecuta manualmente y habilita SEC_RUN_DATABASE_TESTS; no habilita las variantes SEC_RUN_FILE_DATABASE_TESTS ni SEC_RUN_GOVERNANCE_DATABASE_TESTS. Además, tres pruebas de aceptación contienen únicamente expect(true).toBe(true). Corregir sus datos y expectativas, activar las variantes pertinentes y ejecutar comprobaciones al abrir/cambiar una propuesta de código. Los placeholders no deben contarse como evidencia funcional.

La documentación es extensa y sirve como base, pero mezcla estados de momentos distintos:

| Documento/artefacto | Evaluación | Cambio recomendado |
|---|---|---|
| [next-steps.md:3](https://github.com/M16artz/SIGEAC/blob/14bb3e6643a0ffaa64b10580ff87395a171bc776/docs/planning/next-steps.md#L3) | Continúa diciendo que D2–D8 están pendientes desde el 21 de octubre. | Marcarlo histórico o actualizarlo con la versión y defectos pendientes actuales. |
| [AUDITORIA_DOCUMENTACION_SDD.md:7](https://github.com/M16artz/SIGEAC/blob/14bb3e6643a0ffaa64b10580ff87395a171bc776/AUDITORIA_DOCUMENTACION_SDD.md:7) | Auditoría estática previa útil, sobre faf868ea. Sus cifras son históricas. | Mantener fecha/commit y vincular nuevas pruebas; no sustituirlas por una afirmación genérica de cierre. |
| [tasks.md:310](https://github.com/M16artz/SIGEAC/blob/14bb3e6643a0ffaa64b10580ff87395a171bc776/specs/005-complete-evaluation-workflows/tasks.md#L310) | 125 de 126 tareas marcadas; T111 abierto. | Separar implementación, prueba técnica y aceptación humana/institucional. Una tarea marcada no acredita ausencia de defectos. |
| [p01-p16-review-gate.md:3](https://github.com/M16artz/SIGEAC/blob/14bb3e6643a0ffaa64b10580ff87395a171bc776/tests/acceptance/results/p01-p16-review-gate.md#L3) | Revisión institucional expresamente pendiente. | Conservarla pendiente hasta que exista validación real de competencias, vigencia, excepciones y retención. |
| [feature-map.md:1](https://github.com/M16artz/SIGEAC/blob/14bb3e6643a0ffaa64b10580ff87395a171bc776/feature-map.md:1) y pantallas de evidencia | Historial y auditoría se presentan como capacidades, pero las dos pantallas examinadas son estáticas. | Completar la conexión a datos o describir honestamente su estado. |
| [REPORT.md:3](https://github.com/M16artz/SIGEAC/blob/14bb3e6643a0ffaa64b10580ff87395a171bc776/documentos-academicos/ACD4-semana-4/evidencias/auditoria/REPORT.md#L3) | El propio documento dice que no se ejecutó la auditoría completa. | No presentarlo como certificación de seguridad ni como sustituto de resultados reproducibles. |

Las cifras históricas de 64 escenarios técnicos aprobados se conservaron como antecedentes. **No se volvió a ejecutar la campaña completa de aceptación HTTP/Playwright, carga, restauración ni interfaz gráfica.** La documentación académica se contrastó mediante sus fuentes de texto; no se realizó revisión visual de cada PDF.

## Otros puntos que conviene cerrar

- **Integridad de archivos al remitir:** el servicio actual comprueba el estado AVAILABLE del metadato, sin volver a comprobar los bytes. Una pérdida/corrupción externa posterior a la carga puede pasar esa comprobación hasta que otra operación verifique el archivo. Hallazgo estático; no se simuló pérdida de disco en esta campaña.
- **Recuperación de cargas:** la operación de recuperación se agenda en ciertos errores de promoción; una terminación abrupta entre etapas no queda cubierta de forma evidente. CLEANUP marca éxito sin borrar. Revisar la recuperación tras reinicio y probar fallos entre cada etapa antes de afirmar recuperación completa. No se ejecutó una campaña de caída de procesos.
- **Autorrevisión:** el servicio acepta un indicador de excepción y un motivo aportados por el revisor. Debe verificarse contra una excepción autorizada y vigente según la política institucional; esta revisión no confirmó un recorrido completo que eluda las restricciones de asignación previas.
- **Sesión:** comprobar que la renovación del vencimiento del servidor coincida con la duración de la cookie y con la política de inactividad. No se hizo una prueba temporal de navegador.
- **Higiene del repositorio:** en 14bb3e66 siguen versionados **60.864 archivos** bajo backend/target y frontend/node_modules. Quitar artefactos generados del índice mediante un cambio revisable y reconstruirlos en CI. La revisión inicial encontró clases compiladas antiguas que interferían con el arranque; la copia extraída y compilada de cero evita usar esos resultados como evidencia del código actual.
- **Validación de contratos en Windows:** el lanzador usa cmd con rutas sin un manejo correcto de espacios. Se validaron los contratos invocando las herramientas directamente. Ajustar el lanzador y probarlo en una carpeta como Desarollo Basado.

## Orden propuesto para corregir

1. **Datos y actualización:** H01–H02. Definir el historial soportado, corregir con migraciones nuevas y ejecutar instalación limpia, actualización desde bases anteriores y rechazo de mutaciones después de remisión.
2. **Permisos y trazabilidad:** H03–H08. Añadir casos con dos carreras, dos autores, permisos limitados, decisiones sin autoridad y consultas sin filtros. Probar también las rutas HTTP correspondientes.
3. **Flujos de negocio:** H09–H15. Corregir revisiones, coberturas completas, referencias reales, suspensión y subsanación de datos estructurados. Repetir los recorridos de corrección y mejora.
4. **Pruebas y contratos:** corregir las diez incidencias de las ejecuciones ampliadas, habilitar las variantes de base de datos en CI, reemplazar pruebas vacías y alinear OpenAPI con backend/frontend.
5. **Documentación y entrega:** publicar una única matriz de estado por flujo y commit, retirar afirmaciones obsoletas, documentar instalación/actualización/restauración reales y mantener explícitas las decisiones institucionales pendientes.

Cada bloque debe cerrarse con una reproducción del defecto que falle antes del arreglo y pase después, más la regresión afectada. Una suite verde con casos omitidos o sin aserciones de negocio no cierra el bloque.

## Límites de esta revisión

No se modificó código de producción ni se arreglaron automáticamente los defectos encontrados. No se probaron datos reales, servicios institucionales, despliegue público, PostgreSQL 18, navegadores de extremo a extremo, carga, recuperación de desastre ni todas las combinaciones de permisos. No se hizo búsqueda de vulnerabilidades conocidas de dependencias. Las comprobaciones PostgreSQL usaron una instancia temporal, independiente de las bases existentes. Este informe y las reproducciones aportan una revisión técnica con evidencia concreta, no una certificación de seguridad o aceptación institucional.

Nota de publicación: los enlaces de código apuntan al commit auditado en GitHub. Los registros de ejecución y reproducciones se conservan localmente; este commit publica únicamente el informe.
