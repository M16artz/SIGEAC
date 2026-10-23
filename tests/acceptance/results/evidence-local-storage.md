# Almacenamiento local: 004/T028 y T034

Fecha: 2026-10-22. Archivos exclusivamente sintéticos en directorios temporales JUnit.

## Implementación

`LocalFileStorageAdapter` implementa `FileStoragePort` como componente Spring:

- Preparación limitada por bytes, SHA-256 en minúsculas y escritura en staging con `force(true)`.
- Directorios `staging/`, `objects/` y `locks/` bajo la raíz configurada.
- Comprobación al arrancar de mismo `FileStore` y disponibilidad real de `ATOMIC_MOVE`.
- Claves UUID opacas, comprobación de rutas canónicas y apertura sin seguir enlaces.
- Preparación/promoción idempotentes: comparar tamaño/hash y rechazar contenido distinto.
- Exclusión de escritores por carga mediante bloqueos de archivo; nunca reemplazar un objeto final.
- Apertura de sólo lectura y verificación separada de tamaño/hash.
- Limpieza de su escritura temporal ante fallos; conservación de staging ante fallo de promoción.

La raíz debe pertenecer exclusivamente a la cuenta de servicio y quedar fuera de
directorios públicos. Los bloqueos persistentes son metadatos de coordinación,
no archivos de evidencia. No existe una transacción distribuida con PostgreSQL.

## Verificación

Se escribió primero `LocalFileStorageAdapterTest`; Maven falló por ausencia del adaptador.
Tras implementar, se ejecutó la suite completa:

```powershell
cd backend
.\mvnw.cmd test
```

**255 pruebas registradas, 245 aprobadas, 0 fallos, 0 errores y 10 omitidas.**
Las nueve pruebas del adaptador incluyen ocho aprobadas y una omitida.
Se comprobaron bytes/hash exactos, mismo FileStore, reintentos entre instancias,
rechazo de sobrescritura y claves manipuladas, corrupción previa a promoción,
fallos de lectura/tamaño, arranque sin soporte atómico, conservación de staging,
archivos vacíos y exclusión de un escritor concurrente. El contexto Spring arrancó
con una raíz temporal propia y el componente nuevo.

La prueba real con enlaces simbólicos se omitió porque Windows no autorizó su
creación (`FileSystemException`). Las otras nueve omisiones corresponden a pruebas
opcionales preexistentes de bases de datos. No se activó PostgreSQL ni se ejecutó
la interfaz web para este bloque.

## Pendiente

Faltan la carga HTTP y su validación de tipos integrada, persistencia de activos,
registro durable y reconciliación de interrupciones, y descarga autorizada.
`open` por sí solo no compara contra metadatos de evidencia: esa comprobación
anterior al inicio de la respuesta corresponde al futuro servicio de descarga.
La puerta D4 continúa cerrada.
