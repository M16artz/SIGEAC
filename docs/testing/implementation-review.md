# Revisión de implementación frente a los flujos P01–P16

Fecha: 2026-10-21. Alcance: contrastar código existente con `docs/requirements/spec.md`, `process-flows.md`, `domain-rules.md` y la entrega `005-complete-evaluation-workflows`. Esta revisión no declara implementadas las 126 tareas del plan completo.

Actualización posterior: el [catálogo D1](../../tests/acceptance/results/d1-careers.md)
ya está implementado y verificado; reemplaza el estado «catálogo pendiente» de
P02 en esta fotografía inicial. También agrega V008 y el arranque automático de
Flyway, sin completar los módulos restantes.

## Desviaciones corregidas

| Hallazgo | Corrección y evidencia |
|---|---|
| P05 permitía activar sin autorización institucional | Creación en `PREPARATION`; registro de asignación autorizadora propia, fundamento, referencia documental, motivo, decisor y fecha. El backend verifica vigencia, operación `PROCESS_AUTHORIZE`, ámbito y conflictos. Pruebas `SelfAssessmentServiceTest`, `InstitutionalAuthorityAdapterTest`. |
| La transición genérica podía omitir el flujo de activación/cierre | Activación exclusiva por su operación; cierre genérico bloqueado con `CLOSURE_POLICY_REQUIRED`. Suspensión/reanudación con motivo; reapertura exige además competencia autorizadora. `ProcessTransitionServiceTest`. |
| P06 no actualizaba la revisión del proceso | Asignación transaccional con avance de revisión, rechazo de revisiones obsoletas y duplicados superpuestos. El cliente vuelve a consultar el proceso. `ProcessAssignmentServiceTest`, `process-api.service.spec.ts`. |
| Consulta de proceso/asignaciones insuficientemente filtrada | Procesos e indicadores se filtran por operación y ámbito; asignaciones visibles solo para gestores/coordinadores o su participante autorizado. Los accesos directos a procesos ajenos se ocultan. |
| P07 admitía crear/editar metadatos en procesos no activos | Creación y edición solo en `ACTIVE`; edición exige autoría; consulta histórica autorizada permanece disponible. `EvidenceDraftServiceTest`. |
| Clientes enviaban `If-Match` sin comillas y existían varios parsers permisivos | Parser común de ETag fuerte: 428 ausente, 400 inválido/débil, 412 obsoleto. Clientes de usuarios, modelos y procesos envían comillas. `EntityTagValidationTest`, `SelfAssessmentControllerTest`. |
| Datos HTTP malformados podían acabar como error 500 | UUID/JSON/parámetros inválidos producen error 400 sanitizado. Pruebas de controlador y `ApiProblemHandlerTest`. |
| Asignaciones persistentes podían autorizar identidades deshabilitadas | La autorización vuelve a comprobar identidad activa y humana. `ScopeAuthorizationServiceTest`. |
| Auditoría de procesos omitía la carrera | Los eventos conservan carrera y proceso para permitir consulta autorizada por ámbito. `SelfAssessmentAuditServiceTest`. |
| Modelo del piloto contenía `{}` y autor igual a aprobador | Fixture consultable con dos indicadores y aprobador distinto. Reparación limitada al placeholder conocido, solo con datos demo habilitados; no reemplaza modelos de usuarios. Pruebas SQLite y PostgreSQL. |
| Esquema PostgreSQL no reconocía los estados nuevos | V007 agrega estados y convierte `DRAFT` a `PREPARATION` en columna y snapshot. `ProcessPreparationMigrationTest` comprueba V006→V007, autorización, activación y suspensión. |

## Estado real por proceso

| Procesos | Estado después de las correcciones |
|---|---|
| P01 | Login, sesión y administración de usuarios existentes, con pruebas automatizadas. No equivale a auditoría formal de seguridad. |
| P02 | Permisos con ámbitos existentes; catálogo completo de carreras/unidades, desactivación y conservación referencial pendientes. |
| P03–P04 | Versiones y publicación existentes sobre estructura heredada rígida. Jerarquías variables, fuentes requeridas y reglas por indicador de los cinco modelos siguen pendientes; no se afirma compatibilidad completa. |
| P05 | Autorización inicial y activación protegidas. Falta gobierno completo de decisiones, aplicabilidad normativa y reconciliación de procesos legados. |
| P06 | Asignaciones individuales protegidas. Equipos, tareas, vencimientos, reasignación histórica completa y excepciones institucionales pendientes. |
| P07 | Borradores de metadatos. Cargas de archivos, referencias, coberturas por fuente y recuperación de almacenamiento pendientes. |
| P08–P10 | Remisión, revisión documental y subsanación completas pendientes. No confundir entidades/puertos con casos de uso ejecutables. |
| P11 | Métricas por fuentes requeridas y aceptadas pendientes; no calcular porcentajes académicos a partir de adjuntos. |
| P12–P15 | Valoración, consolidación, informes institucionales y mejora pendientes. Los paquetes base no prueban implementación. |
| P16 | Cierre condicionado, snapshots, archivo y restauración pendientes. El bloqueo de cierre inseguro no constituye implementación del cierre. |

## Compatibilidad y operación

- `docs/technical/contracts/openapi.yaml` describe el adaptador inicial de autorización y los campos ejecutables `state`, `revision`, `indicators`, `authorization`. El contrato objetivo de 005 conserva su condición planificada: no se sustituye silenciosamente su `DecisionInput` ni sus reglas completas por este adaptador.
- V007 es forward-only. No se modificaron V001–V006 ni se migraron bases institucionales. Procesos activos antiguos sin constancia se conservan sin inventar autorizaciones; requieren reconciliación antes de aceptación institucional.
- El catálogo demo agrega `PROCESS_AUTHORITY`, sin asignarlo automáticamente a nadie. Un administrador debe asignarlo explícitamente al usuario y ámbito de prueba; la autorización de inicio se registra después por ese usuario. En producción el catálogo de competencias requiere configuración y validación institucional.
- Las fechas y justificaciones del formulario de asignación se introducen explícitamente. Las acciones de cierre no se presentan como disponibles.
- El PostgreSQL de verificación es un clúster desechable en `backend/target/pg-alignment-review`, puerto local 55439, separado de bases existentes. Debe permanecer detenido al terminar la verificación.

## Verificación ejecutada

| Comprobación | Resultado |
|---|---|
| `mvnw.cmd test -q` con `SEC_RUN_DATABASE_TESTS=true` y PostgreSQL 18.4 desechable | 107 pruebas, 0 fallos, 0 errores, 0 omitidas. Incluye actualización V006→V007 y migraciones desde cero. |
| `npm.cmd test -- --watch=false` | 11 pruebas aprobadas en 4 archivos. |
| `npm.cmd run build` | Compilación Angular aprobada. |
| `npm.cmd run lint` | Aprobado. |
| `npm.cmd run contracts` | OpenAPI común, contrato de evidencias y AsyncAPI válidos. Validar sintaxis no demuestra implementación de sus operaciones planificadas. |
| `git diff --check` | Sin errores de espacios. |

No se ejecutaron recorridos de navegador extremo a extremo, carga/rendimiento,
restauración conjunta de archivos/base ni despliegue sobre datos institucionales.
No se verificó la autenticidad de documentos de autorización ni la vigencia de
los cinco modelos normativos. Los tests de migración limpian su base: habilitar
`SEC_RUN_DATABASE_TESTS` únicamente con una base **descartable**.

## Próximo trabajo requerido

Continuar las tareas abiertas de 005 y los prerrequisitos de 004 en su orden de dependencias: catálogo de carreras, modelo flexible/fuentes, gobierno institucional completo, carga/remisión de evidencias, revisión/subsanación, métricas y cierres documentales; después valoración, informes, mejora y archivo integral. No marcar D1–D8 ni el MVP completos con esta revisión parcial.
