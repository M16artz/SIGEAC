# ADR-0020: Congelar el snapshot completo de evidencia remitida

Fecha: 2026-10-24. Estado: aceptada para el software; aceptación integral D4 pendiente.

La revisión del pull hasta `a5594350` detectó que V011 impedía cambiar el estado de
una versión remitida, pero permitía modificar sus metadatos manteniendo
`SUBMITTED` y no protegía sus hijos. La remisión también había perdido la
exigencia de cobertura explícita y rechazaba evidencias exclusivamente estructuradas.

Se conserva V011 y se añade `V015__submitted_evidence_guards.sql`. La transición
solo modifica estado, fecha de remisión y revisión de concurrencia. Después se
rechazan inserciones, modificaciones, movimientos y eliminaciones de indicadores,
archivos, referencias, coberturas y valores estructurados del snapshot remitido.
Los escritores de hijos bloquean las filas padre en orden estable, compartiendo
el bloqueo con la actualización de remisión. La identidad de la evidencia queda
protegida; su contador de revisiones puede avanzar para futuras sucesoras.
El estado operativo del activo binario conserva su recuperación independiente.

La remisión exige acceso de edición, proceso activo, cobertura del modelo fijado,
contenido compatible con cada modalidad declarada y bytes disponibles con tamaño
y SHA-256 verificados mediante el servicio de descarga existente. Los datos
estructurados y referencias institucionales no exigen un archivo o enlace artificial.
La declaración/eliminación de cobertura y el registro estructurado avanzan
atómicamente `lock_revision`; una escritura o remisión con ETag anterior falla.
La respuesta de remisión conserva la ausencia de ETag estipulada en el contrato;
las mutaciones de borrador devuelven el nuevo ETag.

SQLite replica las restricciones mediante el instalador del perfil de pruebas,
siguiendo el patrón existente para triggers que el separador SQL no puede leer.
El contrato común incorpora las seis operaciones v1 de coberturas y snapshots ya
implementadas, sin sustituir los DTO v2 planificados.

La siguiente reserva de migraciones es V016–V021 para D5–D8. No se modifican
checksums históricos ni se usa `repair` o ejecución fuera de orden. Las pruebas
incluyen una actualización V014→V015 con datos remitidos y una carrera entre
remisión y escritura de hijos en PostgreSQL 16.14 descartable. Repetir en la
versión objetivo 18.4 antes de aceptación; no se ha migrado una base institucional.

Esto cierra los guards de 004/T046 y T051, pero no la sucesión, el bloqueo de raíz
completo ni la auditoría independiente tras rollback de 004/T052/T067–T076.
005/T049 permanece abierta hasta demostrar los prerrequisitos de 004 y el
recorrido integral. Véase [evidencia de revisión](../../tests/acceptance/results/2026-10-24-pull-review.md).
