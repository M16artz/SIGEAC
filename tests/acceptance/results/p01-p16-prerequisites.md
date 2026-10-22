# Puerta de prerrequisitos P01–P16

Fecha: 2026-10-21. Tarea: 005/T115. Establecer esta puerta no implementa sus prerrequisitos pendientes.

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
