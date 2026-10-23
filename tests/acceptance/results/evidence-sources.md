# Asociaciones y referencias de evidencia: 004/T036, T038 y T041

Fecha: 2026-10-22. Datos exclusivamente sintéticos.

## Comportamiento implementado

- `VersionFile` conserva la ficha del archivo y su posición como instantánea independiente.
  Asociar exige un activo `AVAILABLE` cuyos metadatos coincidan. Retirar una asociación
  no elimina el activo ni sus bytes.
- `ExternalReference` valida una URI absoluta de hasta 2048 caracteres y descripción
  obligatoria de hasta 1000. Se admiten URI como `https:` y `urn:` sin consultarlas.
  No se registra disponibilidad ni integridad del recurso externo.
- El repositorio de fuentes completa T038 junto con los repositorios anteriores de
  activos, políticas y operaciones de recuperación. Las posiciones se asignan después
  de reclamar la revisión del borrador; editar o retirar no renumera las demás fuentes.
- Crear, modificar o retirar referencias y retirar archivos exige autorización vigente,
  autoría del borrador, proceso activo e `If-Match`. La revisión se incrementa con una
  actualización condicional, en la misma transacción que la fuente. Una revisión
  obsoleta devuelve `412`; omitir `If-Match` devuelve `428`; una versión remitida no
  admite cambios. Los identificadores ajenos/no visibles se ocultan con `404`.

Se implementaron los endpoints existentes en el contrato:

```text
POST   /evidence-versions/{versionId}/external-references
PATCH  /evidence-versions/{versionId}/external-references/{referenceId}
DELETE /evidence-versions/{versionId}/external-references/{referenceId}
DELETE /evidence-versions/{versionId}/files/{fileId}
```

Las respuestas de lectura/actualización de versiones y de remisión incluyen las
fuentes almacenadas, en lugar de colecciones vacías. Las fichas públicas tienen
el estado operativo actual y no contienen claves de almacenamiento. Las referencias
exponen únicamente `id`, `uri` y `description` según el contrato.

## Integración y persistencia

Se respeta el formato de UUID binario de evidencia de Hibernate en SQLite y los
UUID nativos en PostgreSQL. Los identificadores de fuentes/activos usan sus columnas
propias. La prueba integrada detectó y corrigió la afinidad `TEXT` de fechas de
evidencia en el esquema auxiliar SQLite: `TIMESTAMP` y `DATE` conservan los valores
que escribe Hibernate y permiten leerlos posteriormente. No se modificaron
migraciones PostgreSQL aplicadas.

Las escrituras del repositorio exigen transacción y comprueban además que la versión
siga siendo borrador. Estas comprobaciones no sustituyen las pruebas pendientes de
inmutabilidad frente a SQL directo de 004/T046.

## Verificación

Se escribieron primero las pruebas de dominio, servicio, repositorio y controlador;
fallaron por ausencia de sus implementaciones antes de completarlas. La suite incluye:

- URI/descripcion y límites; instantánea de archivo disponible y posición positiva.
- Autorización previa a leer fuentes; proceso activo, autoría, borrador y ETag vigente.
- `Location`, ETag de alta/edición/eliminación, CSRF, `412` y `428` mediante MockMvc.
- Asociaciones sin eliminación del activo, edición sin cambio de posición y rechazo de
  cambios sobre versiones remitidas en SQLite y PostgreSQL.
- Reversión de la revisión cuando falla una escritura de fuente.
- Recorrido con servicios Spring reales, autorización sintética, JPA y JDBC: crear
  evidencia, agregar/leer/editar/eliminar referencia, rechazar ETag obsoleto y revertir
  una URI inválida sin alterar la revisión.

Comando final:

```powershell
cd backend
$env:SEC_RUN_FILE_DATABASE_TESTS='true'
$env:SEC_TEST_DB_URL='jdbc:postgresql://127.0.0.1:55443/postgres'
$env:SEC_TEST_DB_USER='sec_d3'
.\mvnw.cmd test
```

**281 pruebas registradas, 271 aprobadas, 0 fallos, 0 errores y 10 omitidas.**
Las omisiones son las nueve pruebas opcionales anteriores de bases de datos y la
prueba de enlaces simbólicos no autorizada por Windows. Los escenarios PostgreSQL
usan esquemas desechables, migrados hasta V014 y eliminados al terminar.

`node scripts/validate-contracts.mjs` terminó correctamente: los tres OpenAPI,
compatibilidad compartida sin incompatibilidades y AsyncAPI válidos. Persiste el
aviso conocido del componente legado `ReasonCommand` sin uso; los diagnósticos de
telemetría/consulta de procesos del CLI no impidieron validar los contratos.

## Alcance pendiente

T030 y T042 permanecen parciales: falta carga multipart y sus pruebas de tamaño,
tipo y fallos de almacenamiento. No se implementó la interfaz de fuentes ni se
ejecutó el navegador. La recuperación automática y sus escenarios siguen pendientes.
La puerta D4 permanece cerrada.
