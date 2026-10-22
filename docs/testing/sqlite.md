# Pruebas locales con SQLite

El perfil `sqlite-test` permite ejecutar la suite rápida y levantar el backend
con datos sintéticos sin instalar PostgreSQL ni Docker. SQLite es exclusivo de
pruebas; PostgreSQL 18 sigue siendo la base de producción y la referencia para
migraciones, bloqueos, triggers y concurrencia.

## Ejecutar la suite

Desde `backend/`:

```powershell
Remove-Item Env:SEC_RUN_DATABASE_TESTS -ErrorAction SilentlyContinue
Remove-Item Env:SEC_USE_TESTCONTAINERS -ErrorAction SilentlyContinue
.\mvnw.cmd test
```

La suite activa `sqlite-test`, crea el esquema compatible y comprueba la carga
de fixtures mediante `SqliteSyntheticDataTest`. Las siete pruebas de migración
PostgreSQL permanecen omitidas.

Para ejecutar solo la comprobación SQLite:

```powershell
.\mvnw.cmd -Dtest=SqliteSyntheticDataTest test
```

## Levantar el backend con SQLite

El controlador y el dialecto tienen alcance `test`, por lo que el arranque debe
usar el classpath de pruebas:

```powershell
Set-Location backend
.\mvnw.cmd spring-boot:run `
  "-Dspring-boot.run.profiles=sqlite-test" `
  "-Dspring-boot.run.useTestClasspath=true"
```

La base se crea en `backend/target/sec-unl-test.sqlite`. Para usar otra ruta:

```powershell
$env:SEC_SQLITE_PATH='C:/temp/sec-unl-test.sqlite'
```

En otra terminal, inicie la interfaz. El servidor Angular usa automáticamente
`frontend/proxy.conf.json` y reenvía `/api` al backend en el puerto 8080:

```powershell
Set-Location frontend
npm.cmd start
```

Abra `http://localhost:4200/access/login`. No es necesario configurar un
archivo `.env` para este recorrido SQLite.

Para reiniciar los datos, detener el backend y eliminar únicamente el archivo
de pruebas:

```powershell
Remove-Item -LiteralPath .\target\sec-unl-test.sqlite
```

## Datos sintéticos disponibles

| Recurso | Valor |
|---|---|
| Carrera | `11111111-1111-1111-1111-111111111111` |
| Proceso activo | `40000000-0000-0000-0000-000000000001` |
| Indicador A | `50000000-0000-0000-0000-000000000001` |
| Indicador B | `50000000-0000-0000-0000-000000000002` |
| Clave de administrador | `Synthetic-Admin-2026` |
| Clave de los demás usuarios | `Synthetic-User-2026` |

Usuarios: `admin`, `coordinador`, `responsable`, `revisor` y `evaluador`. El
usuario `responsable` tiene asignación vigente sobre ambos indicadores del
proceso sintético, de modo que puede usarse en las pruebas de evidencia.

Los cargadores son idempotentes: reiniciar el backend conserva el archivo y no
duplica los fixtures.

El catálogo incluye además `PROCESS_AUTHORITY`, sin asignación automática. Para
probar la creación de nuevos procesos, el administrador debe asignar esa función
en el ámbito piloto a una identidad humana. Esa identidad registra el inicio con
su asignación, fundamento y referencia documental; después un gestor activa el
proceso. El proceso activo precargado es un fixture legado, no una autorización
institucional real. Véase [revisión de implementación](implementation-review.md).

## Qué debe seguir probándose con PostgreSQL

SQLite no sustituye las pruebas de:

- migraciones Flyway desde una base limpia;
- tipos `uuid`, `jsonb` y `timestamptz`;
- funciones y triggers PL/pgSQL;
- índices parciales y restricciones específicas;
- bloqueo de filas, carreras y semántica de concurrencia;
- respaldo, restauración y aceptación final I0–I6.

Para esas validaciones usar una base PostgreSQL descartable con
`SEC_RUN_DATABASE_TESTS=true`, o Testcontainers con
`SEC_USE_TESTCONTAINERS=true`. Nunca apuntarlas a una base de desarrollo,
demostración o producción porque las pruebas de migración limpian el esquema.
