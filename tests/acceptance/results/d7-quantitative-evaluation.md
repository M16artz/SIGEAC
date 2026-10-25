# D7 — Variables verificadas y cálculo cuantitativo

Fecha: 2026-10-24. PASS técnico de T068/T069/T071/T072/T074/T075 y SC-014
acotado a la estrategia `ratio-percent/1`. Continúa el
[incremento manual](d7-academic-evaluation.md). Conciliación, resultados
consolidados e informes siguen pendientes; no se declara D7 completo.

## Comportamiento conservado

- Captura de un conjunto de A/B por proceso, indicador y persona, con unidades,
  período y sustentos documentales aceptados por cada variable. El período debe
  estar dentro del proceso. Las correcciones crean revisiones propias enlazadas.
- Una verificación explícita, con persona, fecha y fundamento, independiente
  de la captura y del cálculo. No se acepta `verifiedBy` del cliente.
- Solo regla cuantitativa publicada VALIDATED y estrategia registrada. El
  contrato soportado es `A / B * 100`, versión 1, ambas variables obligatorias,
  unidades iguales, tipos INTEGER/DECIMAL, restricciones NON_NEGATIVE/POSITIVE
  para A/B y política REJECT. No se ejecuta texto arbitrario como fórmula.
- Valores sin exponente, máximo 12 cifras enteras y 6 decimales; A no negativo,
  B positivo y sin fracción cuando el tipo sea INTEGER. No se sustituyen
  variables ausentes por cero. Las reglas/estrategias incompatibles se rechazan.
- Resultado DECIMAL128 de 34 cifras significativas y redondeo al par. El umbral
  se aplica al valor conservado, sin redondeo de pantalla. Variables/resultados
  viajan como texto decimal para evitar pérdida de precisión en JavaScript.
- Una ejecución por conjunto actual verificado. Conserva las entradas, regla,
  escala, umbral, resultado, sustentos y decisiones, verificador, ejecutor,
  justificación, limitaciones y aritmética. No crea una consolidación final ni
  modifica la fase documental.
- Antes de verificar o calcular, se comprueban de nuevo la revisión vigente y
  los sustentos exactos. Un cambio de decisión, exclusión o sucesora remitida
  exige nuevas entradas/verificación; los cálculos históricos no se borran.
- Autorización humana INDICATOR_EVALUATE por indicador, CSRF, If-Match, bloqueo
  común de proceso y estados académicos permitidos. Los fallos revierten la
  escritura y auditoría de éxito. El rechazo se audita después del rollback.

## Verificaciones ejecutadas

| Comprobación | Resultado |
|---|---|
| IndicatorValuationRuleTest | 15 casos PASS sobre clases de producción: fórmulas, precisión, límites y datos inválidos |
| AcademicCalculationServiceTest | 10 casos PASS: captura, verificación, revisión, período, unidades, permisos, fase y sustentos invalidados |
| QuantitativeEvaluationControllerTest | 4 casos PASS: CSRF, If-Match, campos cerrados, identidad de verificador y rechazo auditado |
| ManualEvaluationPersistenceTest opt-in | 4 casos PASS: SQLite y PostgreSQL limpio/desde V020/desde V021, incluidos registros cuantitativos |
| DocumentarySnapshotPersistenceTest opt-in | 6 casos PASS de regresión documental con V022 |
| SqliteApplicationContextTest | 10 casos PASS; flujo real cualitativo/cuantitativo y rollback del cálculo ante fallo de auditoría |
| Backend completo final | 471 pruebas, 0 fallos, 0 errores, 16 omitidas |
| Frontend | 76 pruebas PASS en 21 archivos; build y lint PASS |
| Contratos | OpenAPI común/004/005 y AsyncAPI válidos; 93 operaciones comunes y cero incompatibilidades detectadas |
| Navegador Edge headless | 2 recorridos PASS sin mocks HTTP, 9,6 s totales: cualitativo y cuantitativo |

Las pruebas provisionales de regla/cálculo se reemplazaron por pruebas reales;
la conciliación provisional continúa explícitamente omitida. Las omisiones no
acreditan las funcionalidades pendientes.

PostgreSQL 18.4 temporal verificó V022 desde limpio y desde V020/V021,
roundtrip de entradas/verificación/ejecución, predecesora del mismo ámbito y
capturador, unicidad y rechazo de actualizar/borrar los tres registros. V017–V021
no se reescribieron; no se usó `repair` ni migración fuera de orden.

La integración Spring/SQLite capturó cifras con evidencias y decisiones reales,
rechazó cálculo sin verificación y confirmó rollback completo de una ejecución
si falla su auditoría. La base del navegador conserva dos capturas, dos
verificaciones y dos cálculos cuantitativos; los rechazos por falta de
verificación, revisión obsoleta y repetición se registran como REJECTED.

## Recorrido real

La prueba crea evidencia/cobertura y aceptación por API pública. Entra como
evaluador a `/evaluations/quantitative`, captura A=3/B=4 con sustentos por
variable, verifica y conserva 75 %. Corrige A a 2, vuelve a verificar y conserva
50 %. Comprueba que el primer cálculo y sus entradas siguen idénticos, que
puede recuperar entradas históricas por ID y que se rechazan cálculo sin
verificación (422), revisión obsoleta (412), cálculo repetido (409) y usuario
sin función evaluadora (403). El mismo comando repite el recorrido cualitativo.

Captura inspeccionada:
`tests/acceptance/results/playwright-artifacts/d7-quantitative/quantitative-evaluation.png`
(artefacto generado ignorado por Git).

El navegador usa Angular real y backend Spring con SQLite auxiliar. La
persistencia/migración se verifica por separado en PostgreSQL; no se atribuye
a PostgreSQL un recorrido de navegador que no se ejecutó. El fixture de
classpath de pruebas `D7ManualEvaluationData`, perfil `d7-manual-test`, conserva
un indicador cualitativo y agrega otro cuantitativo sintético. Modelo publicado,
proceso y asignaciones se siembran como prerrequisitos; no acreditan activación
ni validez institucional. Las operaciones documentales y académicas posteriores
se ejecutan mediante API/interfaz.

## Reproducción

Desde `backend`, use una base SQLite nueva para cada recorrido:

```powershell
.\mvnw.cmd -q test
$env:SEC_SQLITE_PATH = Join-Path $env:TEMP "d7-quant-$([guid]::NewGuid()).sqlite"
$env:SEC_TEST_FILE_ROOT = Join-Path $env:TEMP "d7-quant-files-$([guid]::NewGuid())"
$d7TestClasses = Join-Path (Get-Location) 'target/test-classes'
.\mvnw.cmd -q spring-boot:run '-Dspring-boot.run.profiles=sqlite-test,d7-manual-test' '-Dspring-boot.run.useTestClasspath' "-Dspring-boot.run.additional-classpath-elements=$d7TestClasses" '-Dspring-boot.run.jvmArguments=-Dserver.port=18086'
```

Desde otra terminal `frontend`:

```powershell
npm.cmd start -- --host 127.0.0.1 --port 14206 --proxy-config proxy.d6.conf.json
```

Con ambos servidores listos, desde otra terminal `frontend`:

```powershell
npm.cmd run test:d7:quantitative
```

Para PostgreSQL local de pruebas, configure `SEC_TEST_DB_URL`,
`SEC_TEST_DB_USER`, `SEC_TEST_DB_PASSWORD` y `SEC_RUN_FILE_DATABASE_TESTS=true`:

```powershell
.\mvnw.cmd -q '-Dtest=ManualEvaluationPersistenceTest,DocumentarySnapshotPersistenceTest' test
```

## Corrección de regresión detectada

La primera suite completa encontró una carrera previa entre dos conexiones
SQLite que reclaman recuperación de archivos. SQLITE_BUSY/SQLITE_LOCKED pueden
ocurrir antes de completar el compare-and-set. El repositorio ahora devuelve
sin concesión para esa contención SQLite concreta y conserva el trabajo para
otro ciclo; otros errores siguen propagándose. RecoveryClaimContentionTest
verifica ambas situaciones y la prueba concurrente existente confirma una sola
concesión. La suite final pasó con esta corrección.

La regresión de persistencia/recuperación se ejecutó además con la opción de
PostgreSQL: FilePersistenceTest pasó 8 casos (SQLite y PostgreSQL) y
RecoveryClaimContentionTest pasó su caso de contención/error ajeno.

## Pendiente

Actualización posterior (2026-10-25): [conciliación autorizada](d7-academic-reconciliation.md)
completa T070/T121/T076 y las marcas de sustentos invalidados. El texto siguiente
conserva los pendientes identificados al cerrar este incremento cuantitativo.

T070 conserva la parte de discrepancias; T121 y T076 no se marcan completos.
Faltan conciliación, marcado general de valoraciones invalidadas y consolidación
conforme al procedimiento autorizado. La ejecución soporta una estrategia
versionada concreta; otros métodos requieren implementación y pruebas propias.
No se ejecutó campaña de rendimiento/accesibilidad ni validación institucional.
