# Rendimiento reproducible P01–P16

The I3 load runner is k6 **1.1.0**. Install that exact release from the
official Grafana k6 distribution for the host platform, then verify it with
`k6 version`. Docker is not required or assumed.

Desde `backend/`, inicie una instancia PostgreSQL exclusiva y descartable con
migraciones hasta V026. Configure `SEC_DB_URL`, `SEC_DB_USER`, `SEC_DB_PASSWORD`
y `SEC_FILE_ROOT` para esa instancia; nunca use una base institucional.

```powershell
$env:SEC_DEMO_DATA = 'true'
$classes = Join-Path (Get-Location) 'target/test-classes'
.\mvnw.cmd -q test-compile spring-boot:run '-Dspring-boot.run.profiles=complete-workflow-performance-test,complete-workflow-acceptance-test' '-Dspring-boot.run.useTestClasspath' "-Dspring-boot.run.additional-classpath-elements=$classes" '-Dspring-boot.run.jvmArguments=-Dserver.port=18087'
```

El fixture vive exclusivamente en el classpath de pruebas y exige PostgreSQL.
El segundo perfil registra el rol sintético de conciliación para la campaña
de 64 criterios; sus usuarios y asignaciones se crean mediante la API.
En una transacción crea un modelo sintético válido con 500 indicadores/fuentes,
100 usuarios distintos (`perf001`–`perf100`) y 10.000 versiones remitidas con
coberturas y referencias explícitas. Su verificación de arranque bloquea un
dataset incompleto. Las versiones son 10.000 evidencias independientes; no se
simula un historial de sucesoras. La publicación es técnica y sintética.

Desde `frontend/`, ejecute `npm run test:performance` con k6 en PATH. Por defecto
consulta `http://127.0.0.1:18087/api/v1`; `SEC_API_BASE_URL` cambia el destino.
`SEC_PERF_PASSWORD` cambia la clave sintética de la carga (debe coincidir con el
fixture). El script autentica 100 sesiones y las rota entre 30 VUs durante 60 s,
con pausa de 1 s por iteración. Comprueba denominadores, páginas disjuntas,
límites de tamaño y ausencia de aceptación inventada, además del historial
paginado. Exige 0 errores, todas las comprobaciones correctas y p95 <=2 s en
consultas interactivas. La autenticación se excluye de ese percentil.

Para una comprobación rápida, `SEC_PERF_VUS=1` y `SEC_PERF_DURATION=5s` reducen
la carga; no equivalen a la aceptación de 30 sesiones. Conserve el resumen con
`k6 run --summary-export <resultado.json> ../tests/performance/complete-workflows.js`.
No exporte cookies ni datos de sesión. El contrato sigue exigiendo p95 <=15 s
para cargas de hasta 25 MiB; este runner mide consultas y no certifica cargas.

Playwright is pinned to **1.64.1** in `frontend/package.json`. Install its
pinned Chromium and Firefox binaries from `frontend/` with
`npx playwright install chromium firefox`, then run `npm run test:acceptance`.
