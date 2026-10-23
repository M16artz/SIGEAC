# Validación de archivos: 004/T027, T032 y T033

Fecha: 2026-10-22. Datos exclusivamente sintéticos.

Se implementa la política de validación versionada con UUID, código, límite configurable
hasta 26 214 400 bytes, aceptación configurable de archivos vacíos, tipos detectados
permitidos y compatibilidades explícitas entre tipos declarados y detectados.
El repositorio lee por código activo y por UUID histórico; no modifica políticas anteriores.

`ContentInspectionService` recorre el flujo una vez, calcula SHA-256 hexadecimal
en minúsculas y copia a un destino de staging proporcionado por su llamador.
Lee como máximo un byte adicional al límite para rechazar exceso, conserva sólo
una muestra de 65 536 bytes para detección Tika sin nombre ni tipo declarado,
y no carga el archivo entero en memoria. El llamador conserva la responsabilidad
de cerrar los flujos y eliminar el staging si la inspección falla.

La detección utiliza firmas de contenido de Tika Core; no inspecciona contenedores
comprimidos para autorizar sus formatos internos. Las compatibilidades se configuran
explícitamente, incluso para `application/octet-stream`.

## Verificación

Las pruebas se escribieron antes de la implementación: Maven falló por ausencia
de `FileValidationPolicy`, `ContentInspectionService` y posteriormente del repositorio.
Tras implementar, el siguiente comando terminó correctamente:

```powershell
cd backend
.\mvnw.cmd test "-Dtest=FileValidationPolicyTest,ContentInspectionServiceTest,JpaFileValidationPolicyRepositoryTest,ModuleArchitectureTest"
```

Resultado: **16 pruebas, 0 fallos, 0 errores, 0 omitidas**. Incluye ocho pruebas
de arquitectura, tres de política, cuatro de inspección y una de persistencia SQLite
con el esquema real. Se comprueban los límites exactos, rechazo temprano, integridad
de copia y hash, discrepancias de tipos, archivos vacíos y errores de lectura/escritura.

Regresión posterior: `.\mvnw.cmd test` terminó con **246 pruebas registradas,
237 aprobadas, 0 fallos, 0 errores y 9 omitidas**. No se activaron las pruebas
opcionales de PostgreSQL; no se inició ningún servidor ni base externa en esta ejecución.

## Alcance pendiente

No implementa la carga multipart, promoción atómica, reconciliación, descarga ni
interfaz de fuentes. No se ejecutó esta lectura de políticas contra PostgreSQL.
La puerta D4 continúa cerrada y estos resultados no constituyen aceptación integral de I3.
