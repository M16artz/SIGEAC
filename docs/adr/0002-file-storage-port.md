# ADR-0002: Puerto para almacenamiento externo de archivos

**Estado**: Aceptado
**Fecha**: 2026-10-18

## Contexto

Los binarios no pueden almacenarse en PostgreSQL y el desarrollo no puede depender de Docker o de un servicio S3.

## Decisión

El dominio usa un `FileStoragePort` con operaciones de preparar, promover, abrir y marcar para reconciliación. El MVP incluye adaptador local con claves opacas y raíz configurable; el contrato permite un adaptador S3 posterior. PostgreSQL conserva metadatos, estado y SHA-256.

## Consecuencias

- Desarrollo y pruebas funcionan con infraestructura local mínima.
- Las descargas siempre pasan por autorización del backend.
- Deben existir reconciliación, limpieza de temporales y pruebas de interrupción.
- El adaptador S3 no forma parte de la aceptación I0–I6.

## Implementación local comprobada (2026-10-22)

`LocalFileStorageAdapter` implementa preparación, promoción, apertura y verificación.
Al arrancar comprueba que `staging/` y `objects/` pertenecen al mismo `FileStore`
y ejecuta una promoción de prueba con `ATOMIC_MOVE`; no ofrece alternativa mediante copia.
Un UUID de carga generado por el servidor determina claves opacas. Los reintentos
comparan tamaño y SHA-256; un contenido diferente no sustituye objetos existentes.
Los bloqueos por carga se conservan en `locks/` y coordinan escritores entre procesos;
un bloqueo solapado en la misma JVM devuelve `FILE_UPLOAD_BUSY`, reintentable por el llamador.

La raíz y sus directorios deben ser de uso exclusivo de la cuenta del servicio,
fuera de cualquier raíz pública del servidor web. Se rechazan rutas no canónicas,
enlaces y claves que no cumplen el formato. La cuenta del servicio controla los
permisos del directorio: las comprobaciones de ruta no sustituyen ese aislamiento
frente a otros procesos que puedan cambiar entradas mientras se realiza una operación.

La preparación limita bytes y calcula SHA-256 con memoria acotada; la validación
de tipos sigue siendo responsabilidad de la orquestación con `ContentInspectionService`.
La promoción conserva staging si falla. El registro durable de recuperación,
la reconciliación y la descarga autorizada siguen pendientes.

Ver resultados y limitaciones en
[`evidence-local-storage.md`](../../tests/acceptance/results/evidence-local-storage.md).

## Alternativas descartadas

- `bytea`/large objects: contradice la constitución.
- S3 obligatorio: convertiría infraestructura externa en requisito del MVP.
