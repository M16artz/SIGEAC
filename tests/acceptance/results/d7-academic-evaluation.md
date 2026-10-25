# D7/P12 — Evaluación académica

## Estado técnico actual (2026-10-25)

P12 está verificado en el alcance soportado: valoración cualitativa, cálculo
`ratio-percent/1` con variables verificadas y conciliación autorizada por
indicador. T070/T076/T121 se completan con
[evidencia de discrepancias y resolución](d7-academic-reconciliation.md), que
incluye regresión real de los tres recorridos. P13–P14 e institucionalización
siguen pendientes; no se declara D7 completo.

## Primer incremento manual (histórico)

Fecha: 2026-10-24. PASS técnico del flujo manual cualitativo (T073 y parte
manual de T074/T075). D7 completo sigue pendiente: variables verificadas,
cálculo cuantitativo, discrepancias, consolidación, hallazgos e informes.

Continuación posterior: [variables verificadas y cálculo cuantitativo](d7-quantitative-evaluation.md).
Implementa ese flujo con `ratio-percent/1`; los apartados siguientes conservan
el estado y los resultados del primer incremento manual. Conciliación e informes
continúan pendientes.

## Alcance ejecutado

El evaluador consulta el método y la escala exactos del modelo publicado fijado
por el proceso. El backend exige una regla `MANUAL_QUALITATIVE` `VALIDATED`,
indicador cualitativo y permiso `INDICATOR_EVALUATE` en ese indicador. La cuenta
técnica y los ámbitos ajenos se rechazan antes de bloquear o leer juicios.

El juicio conserva opción y etiqueta, método, limitaciones, justificación,
actor, fecha y referencias a versión de evidencia, fuente y decisión aceptada.
Los sustentos proceden de remisiones actuales y fuentes aplicables del mismo
indicador; borradores, versiones sustituidas, observadas/rechazadas, fuentes
excluidas y versiones ajenas no pueden seleccionarse como aceptadas.

Cada persona mantiene su cadena propia. If-Match y la predecesora deben
coincidir con su última revisión; una corrección agrega un registro inmutable.
Los juicios de otros evaluadores permanecen independientes. No hay promedio
arbitrario ni consolidación automática. El proceso no cambia de estado ni de
revisión por valorar; acepta juicios en ACTIVE, DOCUMENTARY_CLOSED y REOPENED,
y bloquea escrituras en preparación, suspensión o cierre integral.

La transacción usa el bloqueo común del proceso. Valoración y auditoría de éxito
se confirman juntas; un fallo de auditoría revierte la valoración. El controlador
registra rechazos después del rollback con códigos seguros, sin copiar textos
del juicio a la auditoría de rechazo.

## Evidencia

| Comprobación | Resultado |
|---|---|
| AcademicEvaluationServiceTest | 18 casos PASS sobre implementación real: escala, regla, permisos, fase, sustentos, correcciones y paginación |
| AcademicEvaluationControllerTest | 4 casos PASS: CSRF, If-Match fuerte, DTO cerrado, historial, errores y auditoría de rechazo |
| ManualEvaluationPersistenceTest opt-in | 3 casos PASS: SQLite, PostgreSQL limpio y actualización desde V020 |
| SqliteApplicationContextTest | 10 casos PASS, incluido juicio con sustentos reales y rollback de auditoría |
| Suite backend completa final | 443 pruebas registradas, 0 fallos/errores, 18 omitidas |
| Frontend | 71 pruebas PASS en 20 archivos; build de producción y lint PASS |
| Contratos | OpenAPI común/004/005 y AsyncAPI válidos; 88 operaciones comunes, sin incompatibilidades detectadas |
| Playwright con Edge headless | 1 caso PASS, 5,6 s totales (4,5 s de prueba), sin mocks HTTP |

La suite backend completa pasó inicialmente con 15 omitidas. Después se
marcaron explícitamente como omitidas tres pruebas provisionales que solo
ejercitaban tipos locales o un servicio vacío; su reejecución pasó y deja
443 registradas, 18 omitidas. La suite completa final confirmó esos totales.
Estas omisiones no acreditan cálculo cuantitativo
ni conciliación. Las marcas T068–T072/T074–T076/T121 y las de informes sin
implementación real se reabrieron en el backlog.

El navegador creó evidencia y cobertura por API pública, comprobó rechazo de
juicio antes de aceptación y registró aceptación documental mediante el revisor.
Ingresó como evaluador real, guardó el primer juicio desde `/evaluations`,
registró una corrección y comprobó que el primer JSON histórico seguía igual.
Verificó sustento exacto, revisión obsoleta 412, lectura del responsable 403 y
cálculo provisional 501. Se inspeccionó la captura:
`tests/acceptance/results/playwright-artifacts/d7-manual/manual-evaluation.png`
(artefacto generado ignorado por Git).

PostgreSQL 18.4 temporal verificó V021 limpia y desde V020, la FK compuesta de
predecesora del mismo proceso/indicador/evaluador, unicidad de revisión, rechazo
de actualizaciones/borrados y rollback. Se conservaron V017–V020 sin editar.
La regresión de persistencia documental también pasó con V021 desde limpio,
V015 y V016 (6 casos), y la prueba de migración completa pasó.

## Reproducción

Backend, desde `backend` (base SQLite nueva en cada recorrido de navegador):

```powershell
.\mvnw.cmd -q test
$env:SEC_SQLITE_PATH = Join-Path $env:TEMP "d7-manual-$([guid]::NewGuid()).sqlite"
$env:SEC_TEST_FILE_ROOT = Join-Path $env:TEMP "d7-files-$([guid]::NewGuid())"
$d7TestClasses = Join-Path (Get-Location) 'target/test-classes'
.\mvnw.cmd -q spring-boot:run '-Dspring-boot.run.profiles=sqlite-test,d7-manual-test' '-Dspring-boot.run.useTestClasspath' "-Dspring-boot.run.additional-classpath-elements=$d7TestClasses" '-Dspring-boot.run.jvmArguments=-Dserver.port=18086'
```

En otra terminal, desde `frontend`:

```powershell
npm.cmd start -- --host 127.0.0.1 --port 14206 --proxy-config proxy.d6.conf.json
```

Con ambos servidores listos, desde otra terminal `frontend`:

```powershell
npm.cmd run test:d7:manual
```

`D7ManualEvaluationData` solo está en el classpath de pruebas y requiere el
perfil explícito `d7-manual-test`. Siembra modelo publicado, escala/reglas
cualitativas sintéticas, proceso y asignaciones como prerrequisitos; no acredita
su activación institucional. La evidencia, aceptación y juicios posteriores se
crean por API e interfaz. El navegador usa SQLite; PostgreSQL se verifica en
las pruebas de persistencia/migración, no como backend de ese navegador.

Para PostgreSQL local de pruebas, configure `SEC_TEST_DB_URL`,
`SEC_TEST_DB_USER`, `SEC_TEST_DB_PASSWORD` y `SEC_RUN_FILE_DATABASE_TESTS=true`:

```powershell
.\mvnw.cmd -q '-Dtest=ManualEvaluationPersistenceTest,DocumentarySnapshotPersistenceTest,CompleteWorkflowMigrationTest' test
```

## Límites y siguiente incremento

No se declara SC-014 completo. Falta capturar/verificar variables, ejecutar una
estrategia versionada validada, rechazar variables faltantes con reglas reales
y conservar entradas de cálculo. Las rutas provisionales sin proceso y la
conciliación devuelven 501; no presentan un resultado ficticio como validado.
También falta marcar sustentos invalidados, resolver discrepancias y consolidar
juicios conforme al procedimiento autorizado. La aceptación institucional,
campaña de rendimiento/accesibilidad e informes quedan pendientes.
