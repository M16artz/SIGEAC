# D7/P12 — Discrepancias y conciliación autorizada

Fecha: 2026-10-25. Resultado: PASS técnico para T070/T121/T076, AC-X11 y
AC-P12-03 en el método soportado. Completa P12 junto con
[valoración manual](d7-academic-evaluation.md) y
[variables/cálculo](d7-quantitative-evaluation.md). P13–P14 e institucionalización
siguen pendientes; no se declara D7 completo.

## Alcance

Los evaluadores autorizados pueden registrar una discrepancia entre los juicios
actuales de dos personas distintas. La discrepancia conserva ambos fundamentos,
resultados, método, limitaciones y referencias de sustento. Las correcciones
propias previas y sus payloads permanecen intactos.

Configurar el procedimiento por proceso/indicador o resolver exige además
`ACADEMIC_RECONCILE` y una asignación vigente que autorice esa operación en ese
ámbito, comprobada mediante `InstitutionalAuthorityPort`. El procedimiento
registra referencia/versión, fundamento de competencia, constancia y autoridad.
No se concede esa capacidad automáticamente al coordinador ni al evaluador.

El método soportado es `SELECT_EXISTING_JUDGEMENT_V1`: selección fundamentada de
un juicio existente, con participación declarada de quienes emitieron los juicios
y de quien resuelve. No genera promedios ni nuevos resultados arbitrarios.
La resolución conserva procedimiento exacto, asignación, participantes, actor,
fecha y juicio seleccionado. Una resolución por discrepancia; corregirla exige
otra discrepancia/predecesora. No constituye acreditación.

La consulta proyecta el estado actual sin modificar registros históricos:

- `NO_EVALUATION`: no hay juicios actuales.
- `UNCONSOLIDATED`: hay juicios, sin una resolución aplicable.
- `PENDING_RECONCILIATION`: diferencia de resultados o caso sin resolución aplicable.
- `REQUIRES_REVIEW`: algún juicio tiene sustentos, regla o variables que exigen revisión.
- `RESOLVED`: la resolución corresponde al conjunto actual, procedimiento y sustentos vigentes.

`SUPERSEDED`, `SUPPORTS_INVALIDATED`, `RULE_MISMATCH` e `INPUTS_SUPERSEDED` se
calculan al consultar juicios manuales y cuantitativos. Una decisión documental
nueva, sucesora remitida o exclusión puede invalidar un sustento exacto; cifras
posteriores invalidan el uso actual de un cálculo anterior. Los payloads de
juicios, entradas y cálculos no cambian. Una resolución previa deja de consolidar
si cambia el conjunto de juicios, la política o sus sustentos.

El conjunto de tipos e IDs de juicios tiene una huella SHA-256 para comprobar
que una resolución se aplica a las mismas referencias. No se presenta esa
huella como firma institucional ni como validación de contenido normativo.

## Verificación ejecutada

| Comprobación | Resultado |
|---|---|
| AcademicReconciliationServiceTest | 10 casos PASS sobre código de producción: fundamentos, autoridad, participantes, política, revisión y sustentos manuales/cuantitativos |
| AcademicReconciliationControllerTest | 3 casos PASS: CSRF/If-Match fuerte, campos cerrados, actor no elegible por cliente y rechazo auditado |
| ManualEvaluationPersistenceTest opt-in | 5 casos PASS: SQLite y PostgreSQL limpio/desde V020/V021/V022, incluyendo conciliación inmutable y proyecciones actuales |
| DocumentarySnapshotPersistenceTest opt-in | 6 casos PASS de regresión documental con V023 |
| SqliteApplicationContextTest | 10 casos PASS: flujo real con dos evaluadores y comité, rollback de resolución por fallo de auditoría y rechazo auditado después del rollback |
| Backend completo final | 483 pruebas, 0 fallos, 0 errores, 15 omitidas |
| Frontend final | 82 pruebas PASS en 23 archivos; build de producción y lint PASS |
| Contratos | OpenAPI común/004/005 y AsyncAPI válidos; 97 operaciones comunes, cero incompatibilidades detectadas |
| Edge headless | 3 recorridos reales PASS en 14,8 s: cualitativo, cuantitativo y conciliación, sin mocks HTTP |

La prueba provisional de conciliación se reemplazó por pruebas reales. Las
omisiones restantes no acreditan informes, mejora ni archivo.

V023 se verificó en PostgreSQL 18.4 temporal desde limpio y desde V020/V021/V022.
Los tests comprueban roundtrip de política, discrepancia y resolución, unicidad,
predecesora del mismo ámbito, FK y rechazo de actualizar/borrar registros.
Las migraciones anteriores se conservaron sin editar, sin `repair` y sin
ejecución fuera de orden. La regresión documental pasó con el esquema nuevo.

La integración Spring/SQLite verificó que un fallo de auditoría revierte la
resolución completa. Después confirmó una resolución válida, rectificó la
aceptación documental y comprobó retirada del resultado actual, conservación de
ambos juicios y persistencia de la resolución histórica. Un rechazo posterior
se auditó fuera de la transacción revertida, sin bloquear la única conexión.

## Recorrido de navegador AC-X11

La prueba crea y acepta evidencia por API pública. Dos cuentas evaluadoras
registran juicios distintos, y se conserva la discrepancia. Un evaluador intenta
registrar el procedimiento con una asignación ajena y recibe 403; un responsable
sin función evaluadora tampoco puede consultar los juicios.

La cuenta con asignación competente entra en `/evaluations/reconciliation`,
consulta ambos fundamentos, registra el procedimiento desde la interfaz,
identifica participantes y selecciona un juicio existente. La resolución queda
aplicable y el JSON del juicio previo sigue idéntico.

El segundo evaluador registra otra revisión: la resolución anterior queda
histórica y el resultado actual pasa a pendiente. Se abre otra discrepancia y
el revisor rectifica la aceptación del sustento a OBSERVED. La consulta marca
los juicios para revisión, retira el resultado actual y conserva la resolución
anterior. Intentar resolver con esos sustentos devuelve 409; una revisión obsoleta
devuelve 412. El mismo comando repite los recorridos manual y cuantitativo.

La base sintética de navegador registra una política y una resolución exitosas,
dos discrepancias y tres rechazos de conciliación. Se inspeccionaron las capturas:

- `tests/acceptance/results/playwright-artifacts/d7-reconciliation/reconciliation-resolved.png`
- `tests/acceptance/results/playwright-artifacts/d7-reconciliation/reconciliation-review.png`

Son artefactos generados ignorados por Git. El navegador usa Angular real y
Spring con SQLite auxiliar; PostgreSQL se verifica por separado en pruebas de
persistencia/migración. No se atribuye a PostgreSQL ese recorrido de navegador.

## Reproducción

Desde `backend`, use una base SQLite nueva por ejecución:

```powershell
.\mvnw.cmd -q test
$env:SEC_SQLITE_PATH = Join-Path $env:TEMP "d7-reconciliation-$([guid]::NewGuid()).sqlite"
$env:SEC_TEST_FILE_ROOT = Join-Path $env:TEMP "d7-reconciliation-files-$([guid]::NewGuid())"
$d7TestClasses = Join-Path (Get-Location) 'target/test-classes'
.\mvnw.cmd -q spring-boot:run '-Dspring-boot.run.profiles=sqlite-test,d7-reconciliation-test' '-Dspring-boot.run.useTestClasspath' "-Dspring-boot.run.additional-classpath-elements=$d7TestClasses" '-Dspring-boot.run.jvmArguments=-Dserver.port=18086'
```

Desde otra terminal `frontend`:

```powershell
npm.cmd start -- --host 127.0.0.1 --port 14206 --proxy-config proxy.d6.conf.json
```

Con ambos servidores listos, desde otra terminal `frontend`:

```powershell
npm.cmd run test:d7:reconciliation
```

`D7ReconciliationData` solo existe en el classpath de pruebas y perfil explícito
`d7-reconciliation-test`. Prepara el modelo/proceso sintético del incremento
anterior y añade `evaluador2` y `conciliador`, con asignaciones de prueba al
proceso. No altera competencias de cuentas de producción. Modelo publicado,
activación y asignaciones son prerrequisitos sembrados; no acreditan autorización
institucional. Las operaciones documentales, juicios y resolución posteriores
del recorrido se ejecutan por API/interfaz.

Para PostgreSQL local de pruebas, configure `SEC_TEST_DB_URL`,
`SEC_TEST_DB_USER`, `SEC_TEST_DB_PASSWORD` y `SEC_RUN_FILE_DATABASE_TESTS=true`:

```powershell
.\mvnw.cmd -q '-Dtest=ManualEvaluationPersistenceTest,DocumentarySnapshotPersistenceTest' test
```

## Límites y siguiente bloque

P12 queda técnicamente verificado en sus métodos soportados. Otros mecanismos
de conciliación requieren implementación y validación propias. No se ha
ejecutado campaña de rendimiento/accesibilidad ni aceptación institucional.
T111 y los requisitos transversales correspondientes permanecen pendientes.
El siguiente bloque D7 es P13–P14: hallazgos e informes trazables, que siguen
sin aceptación técnica completa.
