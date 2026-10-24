# D6: MVP documental y cierre

Fecha: 2026-10-24. Resultado: PASS técnico con datos sintéticos para
T060/T064/T066/T067/T120 y SC-012/SC-017 en el alcance documental.
No constituye aceptación institucional ni completa D7–D8.

## Comportamiento verificado

- Cuatro fuentes obligatorias aplicables: tres remitidas y dos aceptadas
  producen exactamente 75 % de recopilación y 50 % de revisión. El tablero
  muestra ambos resultados sin crear valoración académica.
- Un corte conserva esos conteos, versiones, decisiones y SHA-256. Las
  revisiones posteriores y el cierre no modifican su contenido.
- `DOCUMENTARY_STRICT_V1` bloquea pendientes, observadas/rechazadas,
  cardinalidad insuficiente, configuración ausente y archivos indisponibles
  o con tamaño/hash incorrectos. No admite excepciones arbitrarias.
- La cardinalidad cuenta evidencias distintas. Opcionales y exclusiones
  autorizadas no bloquean; todas excluidas conserva `NOT_APPLICABLE`, sin
  fabricar 100 %. Las sucesoras remitidas reemplazan las coberturas anteriores;
  los borradores sucesores conservan la candidata remitida.
- El cierre guarda un corte completo y un registro inmutable enlazado,
  pasa de `ACTIVE` a `DOCUMENTARY_CLOSED` y avanza dos revisiones: reserva del
  corte y transición. Un corte independiente solo reserva una revisión.
- Permisos humanos por capacidad y ámbito completo, CSRF e If-Match son
  obligatorios. Las lecturas históricas no exponen datos a ámbitos parciales.
- El cierre y las escrituras documentales comparten el bloqueo del proceso.
  Las carreras PostgreSQL de revisión y escritura esperan y, tras el cierre,
  reciben `PROCESS_NOT_ACTIVE` sin persistir cambios.
- Los fallos revierten corte, revisión, transición y auditoría de éxito.
  El rechazo se audita después del rollback, con resultado `REJECTED` y
  metadatos seguros. También se verificó rollback ante fallo de auditoría.

## Ejecuciones

| Verificación | Resultado |
|---|---|
| Suite backend `mvnw.cmd -q test` | 401 pruebas, 0 fallos, 0 errores, 12 omitidas |
| Reejecución final EvidenceDraftServiceTest / ApplicabilityDecisionServiceTest | PASS tras ordenar autorización antes del bloqueo |
| Persistencia opt-in DocumentarySnapshotPersistenceTest | 6 casos PASS: SQLite, PostgreSQL limpio, upgrades V015/V016, integridad y reserva concurrente |
| DocumentaryClosureConcurrencyTest opt-in | 2 carreras PostgreSQL PASS |
| SqliteApplicationContextTest | 9 casos PASS, incluido cierre real y rollback |
| Angular | 62 pruebas PASS; lint y build de producción PASS |
| Contratos | OpenAPI común/004/005 y AsyncAPI válidos; 86 operaciones comunes compatibles |
| Playwright D6 con Edge headless | 1 caso PASS, 6,4 s totales (5,4 s de prueba), sin mocks HTTP |

PostgreSQL 18.4 se ejecutó en un clúster temporal local sin Docker y se detuvo
al finalizar. Las pruebas opt-in usan esquemas desechables. V016_1 se aplicó
en orden ascendente, sin modificar V016, sin `repair` ni ejecución fuera de orden.

El navegador usó Angular real y Spring Boot con SQLite auxiliar. Registró
evidencias, coberturas, remisiones y decisiones por API pública; verificó el
tablero 75/50, guardó el primer corte desde la interfaz y comprobó el bloqueo
de cierre sin corte adicional ni cambio de revisión. Completó las cuatro
aceptaciones y cerró desde la interfaz. Consultó el enlace histórico al corte
75/50 después del cierre, comprobó SHA-256 sin cambios, escrituras tardías
rechazadas y lectura histórica no autorizada denegada. No invocó P12–P15.
ArchUnit comprueba además que reporting no depende de los módulos posteriores.

La base sintética final conserva dos cortes (4/3/2 y 4/4/4), estado cerrado y
revisión 4. La auditoría del proceso registra cuatro remisiones, cinco decisiones
de revisión, dos cortes, un cierre y un rechazo de cierre.
Captura inspeccionada: `tests/acceptance/results/playwright-artifacts/d6/documentary-closure.png`
(artefacto generado ignorado por Git).

## Reproducción del navegador

Desde `backend`, con una base SQLite nueva y una carpeta temporal de archivos:

```powershell
.\mvnw.cmd -q test-compile
$env:SEC_SQLITE_PATH = Join-Path $env:TEMP "d6-mvp-$([guid]::NewGuid()).sqlite"
$env:SEC_TEST_FILE_ROOT = Join-Path $env:TEMP "d6-files-$([guid]::NewGuid())"
$d6TestClasses = Join-Path (Get-Location) 'target/test-classes'
.\mvnw.cmd -q spring-boot:run '-Dspring-boot.run.profiles=sqlite-test,d6-mvp-test' '-Dspring-boot.run.useTestClasspath' "-Dspring-boot.run.additional-classpath-elements=$d6TestClasses" '-Dspring-boot.run.jvmArguments=-Dserver.port=18086'
```

En otra terminal, desde `frontend`:

```powershell
npm.cmd start -- --host 127.0.0.1 --port 14206 --proxy-config proxy.d6.conf.json
```

Con ambos servidores listos, desde otra terminal `frontend`:

```powershell
npm.cmd run test:d6
```

`D6DocumentaryMvpData` existe solo en el classpath de pruebas y requiere el
perfil explícito `d6-mvp-test`. Siembra los prerrequisitos P01–P06: cuentas,
modelo publicado, proceso activo y asignaciones sintéticas. El caso D6 no
repite su creación desde interfaz ni acredita su activación institucional;
los recorridos previos se documentan en las entregas D1–D3. Todas las acciones
documentales posteriores del caso se ejecutan mediante API o interfaz.

Para las pruebas PostgreSQL, configure `SEC_TEST_DB_URL`, `SEC_TEST_DB_USER` y
`SEC_TEST_DB_PASSWORD` para una instancia local de pruebas; active
`SEC_RUN_FILE_DATABASE_TESTS=true` y ejecute:

```powershell
.\mvnw.cmd -q '-Dtest=DocumentarySnapshotPersistenceTest,DocumentaryClosureConcurrencyTest' test
```

## Límites

Se completa el checkpoint de software del MVP documental. No se ha ejecutado
una campaña de rendimiento o accesibilidad, restauración integral/archivo ni
validación institucional. El contrato objetivo v2, las excepciones de cierre
y los módulos académicos, informes y mejora conservan sus tareas pendientes.
El navegador usa SQLite; la migración y los bloqueos se verifican por separado
en PostgreSQL, sin atribuirle una ejecución de navegador que no se hizo.
