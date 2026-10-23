# Registros de archivos y recuperación: 004/T035, T037 y parte de T038

Fecha: 2026-10-22. Datos exclusivamente sintéticos.

## Implementación

`FileAsset` conserva UUID de archivo/carga, claves opacas, nombre sin rutas ni
caracteres de control, tipos declarado/detectado separados, tamaño 0–26 214 400,
SHA-256 hexadecimal en minúsculas, política exacta y fecha de creación.
Los estados son `STAGED`, `AVAILABLE`, `QUARANTINED` y `MISSING`.
El repositorio inserta metadatos una vez y sólo cambia el estado mediante comparación
con el estado esperado. No permite volver a `STAGED` desde otro estado.
Las restricciones existentes de V006 garantizan unicidad de carga y claves;
el hash admite duplicados.

`FileRecoveryOperation` y su repositorio persisten las operaciones `PROMOTE`,
`VERIFY`, `QUARANTINE` y `CLEANUP` con estados V006 `PENDING`, `LEASED`,
`SUCCEEDED` y `FAILED`. Admiten huérfanos sin archivo asociado y conservan
correlación, fechas, programación del siguiente intento y códigos de motivo seguros.

La reclamación utiliza una actualización condicional que incrementa el intento.
Se pueden retomar bloqueos vencidos. Confirmar éxito o fallo exige propietario,
número de intento y bloqueo vigente; un trabajador anterior no puede finalizar
una operación retomada aunque el nuevo propietario use el mismo nombre.
Las operaciones terminadas no vuelven a la cola.

Se reutiliza V006 sin modificar migraciones aplicadas. `data-model.md` se reconcilia
con sus columnas y estados. T038 permanece incompleta porque faltan las asociaciones
de versiones de evidencia y referencias externas.

## Verificación

Las pruebas se escribieron primero y fallaron por ausencia de los nuevos tipos
y repositorios. La primera comprobación de unicidad mostró la traducción propia
de SQLite de una restricción SQL; se ajustó la expectativa al tipo común
`DataAccessException`. Se limpiaron sólo las clases de pruebas generadas cuando
el editor dejó bytecode con errores de compilación, y Maven recompiló las pruebas.

Comando final, con PostgreSQL local de pruebas en `127.0.0.1:55443`:

```powershell
cd backend
$env:SEC_RUN_FILE_DATABASE_TESTS='true'
$env:SEC_TEST_DB_URL='jdbc:postgresql://127.0.0.1:55443/postgres'
$env:SEC_TEST_DB_USER='sec_d3'
.\mvnw.cmd test
```

**268 pruebas registradas, 258 aprobadas, 0 fallos, 0 errores y 10 omitidas.**
Las omisiones son las nueve pruebas opcionales anteriores de bases de datos
y la prueba anterior de enlaces simbólicos sin permiso de creación en Windows.
Las 13 pruebas nuevas se aprobaron: dos de archivo, tres de operación de recuperación
y cuatro escenarios de persistencia ejecutados tanto en SQLite como en PostgreSQL.

Cada escenario PostgreSQL aplicó V001–V014 en un esquema `file_<UUID>` desechable,
eliminado al terminar. Se comprobaron unicidad independiente de carga y clave,
hashes repetidos, cambios de estado obsoletos, referencias a archivos inexistentes,
programación de reintentos, exclusión entre dos conexiones simultáneas y rechazo
de confirmaciones de trabajadores vencidos/sustituidos.

## Pendiente

La cola persistente todavía no ejecuta operaciones sobre el almacenamiento.
Faltan el servicio de reconciliación y su planificador, los seis escenarios de
interrupción de T029/T040, las asociaciones/referencias de T036/T038 y la carga HTTP.
No se ejecutó la interfaz web. La puerta D4 permanece cerrada.
