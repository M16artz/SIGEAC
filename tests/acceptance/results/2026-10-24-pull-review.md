# Revisión del pull y continuación D4 — 2026-10-24

Base revisada: `a5594350` (`Implementacion Avance`). Árbol de trabajo limpio al
comenzar. Datos de pruebas sintéticos; no se accedió a una base institucional.

## Trabajo recibido de los compañeros

Se revisaron historial, cambios de código, contratos y evidencias desde `b263a568`:

| Revisión | Avance recibido |
|---|---|
| `0e8f3c81` y posteriores | Continuación D2: modelos configurables, procedencia y aplicabilidad normativa, publicación y editor, V009 y endpoints aditivos v1. |
| `2aa31160`, `861e5334`, `a4d488d7` | Avances D3/D4: planificación, equipos y asignaciones, coberturas y snapshots estructurados; V010–V014. La evidencia D3 del 22 de octubre documenta PostgreSQL 18.4 y Edge. |
| `a5594350` | Carga de archivos, reconciliación/recuperación, remisión, descarga verificada y conexión de interfaz. |

Referencias conservadas: [modelos D2](d2-five-models.md),
[planificación D3](d3-process-planning.md) y [cobertura D4](d4-source-coverage.md).
Se revisó su evidencia; esta sesión no volvió a ejecutar el recorrido de Edge.
El repositorio también versiona artefactos en `backend/target` y
`frontend/node_modules`. La compilación de esta revisión se hizo en una copia
aislada bajo `backend/target/review-20261024/backend` para no modificarlos.

## Hallazgos y correcciones

1. **Remisión sin cobertura ni acceso de edición.** Antes del cambio se ejecutaron
   dos pruebas negativas sobre el servicio recibido: ambas fallaron porque el
   servicio remitía cuando debía rechazar. Se recuperó la cobertura explícita y
   el control de autor/proceso activo mediante `requireEditableVersion`.
2. **Modalidades incompatibles.** Se sustituyó la exigencia genérica de archivo o
   enlace por la validación de cada fuente declarada del modelo fijado. Los datos
   estructurados y referencias institucionales requieren snapshot compatible y
   pueden remitirse sin archivo artificial. Se rechazan fuentes ajenas o snapshots
   sin cobertura. Los archivos se verifican por estado, tamaño y SHA-256 usando
   `EvidenceDownloadService` antes de remitir.
3. **Borradores con ETag reutilizado.** Declarar/eliminar cobertura y registrar
   snapshots ahora incrementa la revisión con comparación atómica. Un ETag previo
   deja de servir para escribir o remitir; la transacción revierte si pierde la carrera.
   La pantalla existente recarga el recurso después de cada mutación.
4. **Inmutabilidad incompleta.** V011 permitía actualizar metadatos manteniendo
   `SUBMITTED` y no protegía los hijos. V015 congela versión, cinco tablas hijas e
   identidad de la evidencia, incluyendo movimientos entre padres. Los escritores
   bloquean las filas padre y comprueban el estado tras esperar una remisión.
   Se conserva la recuperación operativa del activo binario y el contador para
   futuras sucesoras. V001–V014 permanecen sin cambios.
5. **Pruebas contradictorias de remisión.** Un controlador antiguo esperaba ETag y
   otro su ausencia. Se conserva el contrato vigente: respuesta remitida sin ETag;
   mutaciones de borrador con el nuevo ETag. El contrato común incorpora las seis
   operaciones de cobertura y datos estructurados que el código ya exponía.

Decisión: [ADR-0020](../../../docs/adr/0020-submitted-evidence-snapshot-guards.md).

## Verificación ejecutada

Entorno: Windows, JDK 25.0.4.1, Maven Wrapper y PostgreSQL **16.14** instalado
localmente. Se creó un clúster descartable independiente en
`backend/target/review-20261024/pg16`, puerto 55447. Las pruebas crearon/eliminaron
sus propios esquemas UUID. El servidor temporal quedó detenido al finalizar.

| Comprobación | Resultado |
|---|---|
| Regresión backend completa | 323 casos: **314 aprobados, 0 fallos, 0 errores, 9 omitidos**. |
| `SubmittedEvidenceImmutabilityTest` | 8 casos aprobados: SQLite/PostgreSQL, transición legal, versión/hijos/identidad, CAS, actualización V014→V015 conservando datos y escritor concurrente bloqueado/rechazado. |
| Integración de gobierno y archivos | Habilitadas las pruebas PostgreSQL de `SEC_RUN_GOVERNANCE_DATABASE_TESTS` y `SEC_RUN_FILE_DATABASE_TESTS`, además de `SEC_RUN_EVIDENCE_GUARD_DATABASE_TESTS`. |
| Contratos OpenAPI común, 004 y 005 | Los tres lints aprobados; un aviso previo por `ReasonCommand` legado sin uso. |
| `check-contract-compatibility.mjs` | 85 operaciones comunes, 77 objetivo, 12 compartidas, 0 incompatibles en la misma ruta/método/versión. |
| `git diff --check` y `verify-synthetic-data.ps1` | Aprobados. |

Las nueve omisiones son ocho pruebas históricas bajo `SEC_RUN_DATABASE_TESTS` y
una prueba de enlaces simbólicos condicionada por Windows. Las nuevas pruebas de
migración sí ejecutaron Flyway limpio hasta V015 y V014→V015. No se declara que
esas ocho pruebas históricas ni la prueba de enlaces se hayan ejecutado.

Para repetir el bloque PostgreSQL, usar un servidor descartable y configurar
`SEC_TEST_DB_URL`, `SEC_TEST_DB_USER` y los tres flags específicos anteriores a
`true`, después ejecutar `./mvnw.cmd test` desde `backend` con JDK 25. Esta sesión utilizó
`-f target/review-20261024/backend/pom.xml` y el repositorio Maven local existente;
los logs quedan en `backend/target/review-20261024/`.

## Puerta de continuación

Se cierran 004/T046 y T051 por los guards y pruebas ejecutados. Se reabren
004/T045 y T052: los mocks/CAS no demuestran por sí solos todas las carreras,
rollback de auditoría ni bloqueo completo de raíz; falta auditoría independiente
tras rollback. 005/T049 se reabre porque los prerrequisitos de 004 siguen abiertos.

El siguiente bloque debe completar sucesores e historial 004/T057–T066, auditoría
de fallos T067–T076 y aceptación integral, además de las comprobaciones de interfaz
y operación aún abiertas. D5–D8 reservan V016–V021 después de V015.
No se declara D4 ni el MVP terminados. Tampoco aceptación institucional.

No se ejecutaron Angular, navegador, carga, restauración ni PostgreSQL 18.4 en esta
sesión; la evidencia previa de los compañeros conserva su propio entorno y fecha.
Repetir las pruebas de V015 en la versión objetivo antes de aceptar o desplegar.
