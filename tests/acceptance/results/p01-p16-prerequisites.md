# Puerta de prerrequisitos P01–P16

Fecha: 2026-10-21. Tarea: 005/T115. Establecer esta puerta no implementa sus prerrequisitos pendientes.

Actualización 2026-10-22: 004/T027, T032 y T033 completan la validación aislada
de archivos y la lectura de políticas versionadas. Evidencia:
[`evidence-file-validation.md`](evidence-file-validation.md). La carga, promoción
y recuperación de T028–T031 y T034–T043 siguen pendientes; D4 permanece cerrado.

Actualización posterior 2026-10-22: T028 y T034 añaden el adaptador local de
preparación/promoción atómica. Resultados y omisión de la prueba con enlaces
simbólicos en [`evidence-local-storage.md`](evidence-local-storage.md).
Siguen pendientes T029–T031 y T035–T043; D4 permanece cerrado.

Actualización posterior 2026-10-22: T035 y T037 implementan las fichas de archivo
y operaciones de recuperación persistentes. T038 tiene los repositorios de archivos,
políticas y bloqueos temporales; faltan asociaciones y referencias de evidencia.
La competencia entre trabajadores se comprobó en SQLite y PostgreSQL:
[`evidence-file-records.md`](evidence-file-records.md). T029–T031, T036 y T038–T043
siguen pendientes de completarse; D4 permanece cerrado.

Actualización posterior 2026-10-22: T036, T038 y T041 completan instantáneas de
archivos, referencias, persistencia y edición autorizada de fuentes. Se añadieron
los endpoints de referencias/retirada de archivos y el recorrido integrado JPA/JDBC.
Evidencia: [`evidence-sources.md`](evidence-sources.md). T030/T042 siguen parciales
por multipart; faltan T029, T031, T039–T040 y T043. D4 permanece cerrado.

## Reutilizable para D1

Login/sesión, CSRF, autorización funcional y por ámbito, auditoría y ETags.
Las regresiones existentes permanecen en la suite; D1 añade carreras/unidades
sin exigir almacenamiento de archivos ni módulos académicos.

## Puerta D4 — permanece cerrada

| Tareas de 004 | Funcionalidad requerida | Estado |
|---|---|---|
| T027–T043 | Validación, carga/promoción, recuperación, archivos y enlaces | Pendiente |
| T044–T056 | Remisión inmutable, descarga verificada y pruebas | Pendiente |
| T057–T066 | Sucesora, historial y concurrencia | Pendiente |
| T067–T076 | Auditoría completa de remisiones y fallos | Pendiente |
| T077–T080, T084 | ADR, manuales, contratos y trazabilidad de esas funcionalidades | Pendiente |
| T081–T083 | Rendimiento, respaldo/restauración y datos sintéticos | Pendiente |
| T085–T088 | Regresión final, quickstart y puertas de aceptación restantes de 004 | Pendiente |

Los borradores de metadatos ya existentes no satisfacen remisión, carga ni
recuperación. La reserva de inmutabilidad es ahora V011, después de V008–V010 y
antes de la cobertura por fuentes V012. No aceptar 005/D4 hasta guardar resultados
de los contratos/pruebas aplicables anteriores; no bloquear por ello D1/D2.
