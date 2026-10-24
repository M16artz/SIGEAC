# D6: bases del cálculo documental

Fecha: 2026-10-24. Alcance: T058/T062 y parte de las reglas de candidatas de T120;
no constituye aceptación de D6 ni del MVP documental.

## Comportamiento implementado

- `DocumentaryProgressCalculator` calcula por IDs distintos de fuentes obligatorias
  aplicables. Conserva denominador, numeradores y política `SOURCE_COVERAGE_V1`;
  deriva porcentajes con `BigDecimal`, dos decimales y `HALF_UP` al presentarlos.
- Cuatro fuentes, tres recopiladas y dos aceptadas producen 75 % y 50 %.
  La recopilación exige una cobertura remitida; la revisión exige la cardinalidad
  mínima de evidencias distintas aceptadas. Varias revisiones o coberturas
  duplicadas de una evidencia no satisfacen una cardinalidad mayor que uno.
- Las fuentes opcionales y las exclusiones autorizadas no suman al denominador
  ni a los numeradores. Sin fuentes obligatorias configuradas se devuelve
  `NOT_CONFIGURED`; si todas las obligatorias están excluidas, `NOT_APPLICABLE`.
  En ambos casos los porcentajes son `null`. Con fuentes aplicables sin
  remisiones, los porcentajes son cero.
- El calculador recibe la aplicabilidad resuelta y coberturas vigentes después
  del filtro de autorización. Rechaza fuentes con definiciones contradictorias,
  resultados contradictorios para la misma evidencia/fuente y coberturas fuera
  del catálogo suministrado. No reemplaza la autorización de la futura consulta.
- `CoverageReviewProjectionService` usa únicamente la última versión remitida
  de cada evidencia. Un borrador no altera la candidata; una remisión reemplaza
  todas sus coberturas y no recupera fuentes retiradas desde versiones antiguas.
  Otras evidencias independientes conservan sus propias candidatas.

## Verificación

Regresión reproducida antes de corregir la proyección: la prueba
`submittedSuccessorDoesNotResurrectWithdrawnCoverageFromAnOlderVersion` falló
porque devolvía una fuente retirada de la sucesora.

Pruebas de este incremento:

- `DocumentaryProgressCalculatorTest`: fórmulas, duplicados, cardinalidad,
  exclusiones, opcionales, redondeo, cero fuentes y entradas contradictorias.
- `CoverageReviewProjectionTest`: sucesora que retira cobertura y evidencia
  que solo tiene borradores, además de las regresiones existentes de D5.
- `DocumentarySnapshotSemanticsTest`: integración de proyección y calculador
  para borrador/remisión, retirada, evidencias independientes y cardinalidad.

Comando de verificación del backend, con JDK 25:

```powershell
cd backend
.\mvnw.cmd -q test
```

Resultado: **343 pruebas, 0 fallos, 0 errores, 10 omitidas**, en 89 suites.
Incluye 12 pruebas del calculador, 6 de proyección, 3 de integración de
candidatas/cálculo y 8 reglas de arquitectura. Las 10 omisiones corresponden
a verificaciones condicionadas por el entorno; no se cuentan como aprobadas.

## Trabajo pendiente

Actualización posterior: [consulta y tablero D6](d6-progress-query.md) verifica
T059/T063/T065. El listado siguiente conserva los pendientes al cerrar las bases
del calculador; los cortes históricos y el cierre siguen sin implementarse.

T059/T063: consulta, autorización y desglose por ámbito. T060/T061/T064:
migración, persistencia de cortes y cierre documental transaccional.
T065/T066: tablero y consulta histórica. T067: recorrido completo con
P12–P15 deshabilitados. T120 sigue abierta hasta verificar también
instantáneas persistidas y transiciones corte/cierre.

No se modificaron endpoints, contratos REST, migraciones ni interfaz.
No se declara validación PostgreSQL ni aceptación institucional.
