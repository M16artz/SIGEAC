# D1 — Catálogo de carreras entregado

Fecha: 2026-10-21. Entrega técnica con datos sintéticos; no es aprobación institucional ni aceptación del MVP completo.

## Alcance implementado

- Unidades académicas: alta, consulta/búsqueda paginada, edición y desactivación/reactivación.
- Carreras: código normalizado único, unidad, vigencia, consulta paginada, edición y cambios de estado con motivo.
- Historial de carreras paginado; revisiones persistentes de carreras/unidades append-only, sin DELETE.
- Capacidad global explícita `CAREER_ADMIN`; lectura por `CAREER_READ` y carrera. Totales, unidades e historial respetan ese filtro; cuenta técnica denegada.
- ETag fuerte y revisión optimista en cambios; CSRF; errores sanitizados y rechazo de campos desconocidos del comando.
- Carrera y unidad activas, clasificación completa y período válido antes de crear/activar procesos; el historial sigue consultable al desactivar.
- Pantalla `/careers`, navegación y selector de carrera del formulario de procesos, sin UUID fijo.
- V008 preserva referencias legadas; las deja pendientes de clasificación sin fabricar metadatos institucionales. Fixture piloto idempotente condicionado a datos demo.

## Resultados ejecutados

| Verificación | Resultado |
|---|---|
| Backend completo con PostgreSQL descartable habilitado | 119 pruebas, 0 fallos, 0 errores, 0 omitidas. |
| Angular | 16 pruebas en 6 archivos, todas aprobadas. |
| Compilación Angular y ESLint | Aprobados. |
| OpenAPI común, 004 y 005; AsyncAPI | Válidos. |
| Compatibilidad de contratos | 70 operaciones comunes, 77 objetivo; 12 operaciones D1 compartidas, 0 definiciones incompatibles bajo el mismo método/versión/ruta. |
| Playwright, Edge headless con backend SQLite | 2 recorridos aprobados: API negativa/positiva y recorrido de interfaz. |
| Los mismos recorridos con backend PostgreSQL 18.4 | 2 recorridos aprobados, 0 fallos. |
| Migración desde V007 y esquema limpio | Aprobados por `CareerCatalogMigrationTest` y `CleanDatabaseMigrationTest`. |
| Arranque PostgreSQL desde V007 | `CareerCatalogStartupTest` prueba migración antes de validación JPA y carga del catálogo piloto. |

Las pruebas de API incluyen código duplicado con mayúsculas/espacios, ausencia
de CSRF, ausencia de If-Match, revisión obsoleta, DELETE denegado, referencia a
carrera deshabilitada, 404 indistinguible para ID desconocido/ajeno y ausencia
de datos ajenos en conteos de carreras/unidades.

Las pruebas de migración conservaron UUID legados, verificaron que no se activan
automáticamente, conservaron revisión anterior al clasificar el fixture y
rechazaron edición/borrado de historial. Las pruebas de dominio/servicio cubren
fechas, unidad obligatoria, estados, paginación y cuentas técnicas.

## Historial Flyway inspeccionado

Solo se inspeccionó la base **descartable** `127.0.0.1:55439/postgres`, con un
clúster independiente en `backend/target/pg-alignment-review`. No se inspeccionó
ni migró una base institucional. Checksums Flyway observados después del arranque:

| Versión | Descripción | Checksum | Estado |
|---|---|---:|---|
| 001 | foundation | -536177221 | success |
| 002 | audit | -1368906236 | success |
| 003 | identity access | -1298792723 | success |
| 004 | evaluation model | 2087563287 | success |
| 005 | self assessment | -914773217 | success |
| 006 | evidence | -1929968511 | success |
| 007 | process preparation and suspension | 1973367546 | success |
| 008 | career catalog | -723813154 | success |

## Reproducción

Backend y pruebas de migración requieren una base **descartable**, pues los
tests de migración limpian el esquema. Nunca habilitar estas variables sobre
datos institucionales. El bootstrap del PostgreSQL local se documenta en la
[guía técnica](../../../docs/technical/quickstart.md).

Desde `backend/`, configurando la base descartable:

```powershell
$env:SEC_RUN_DATABASE_TESTS='true'
$env:SEC_TEST_DB_URL='jdbc:postgresql://127.0.0.1:55439/postgres'
$env:SEC_TEST_DB_USER='sec_alignment'
.\mvnw.cmd test -q
```

Para el navegador se usaron backend aislado en 18081 y Angular en 14200.
Desde `frontend/`, con el backend de pruebas ya iniciado:

```powershell
npm.cmd start -- --port 14200 --host 127.0.0.1 --proxy-config proxy.d1.conf.json
```

En otra terminal de `frontend/`:

```powershell
npm.cmd run test:d1
npm.cmd test -- --watch=false
npm.cmd run build
npm.cmd run lint
npm.cmd run contracts
```

El runner usa dependencias ya instaladas en `frontend/node_modules` para resolver
los tests ubicados en la raíz. `SEC_WEB_BASE_URL` y `SEC_BROWSER_CHANNEL` permiten
cambiar servidor/canal; el canal utilizado fue Edge. No instala navegadores ni
despliega datos. Se deben detener las instancias auxiliares al terminar.

## Límites y siguiente entrega

La comprobación de navegador cubre Edge headless, no una matriz completa de
navegadores o accesibilidad WCAG. No se midieron carga/rendimiento, restauración
conjunta ni producción. D2–D8, la vigencia normativa y las competencias
institucionales permanecen pendientes. La separación v1/v2 no implementa las
operaciones futuras. Continuar con D2 sobre V009, sin reescribir este catálogo.
