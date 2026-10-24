# D6: cortes documentales persistentes e historial

Fecha: 2026-10-24. T061 implementada; avance parcial de T064/T066/T120.
El cierre de fase y la aceptación completa del MVP siguen pendientes.

## Funcionalidad

- POST `/api/v1/processes/{processId}/documentary-snapshots` guarda un corte
  **actual**, con motivo de 10–4000 caracteres e `If-Match` del proceso.
  Exige sesión humana, `DOCUMENTARY_SNAPSHOT_CREATE` y `PROGRESS_READ`
  sobre el proceso completo en `ACTIVE`.
- Captura versión de modelo, estado observado, revisión, actor, fecha, motivo,
  IDs de indicadores, fuentes obligatorias/opcionales/excluidas, cardinalidad,
  candidatas remitidas, decisiones documentales y referencias exactas de
  aplicabilidad. Conserva numeradores, denominador y versión de fórmula.
- La consulta para el corte incluye todas las fuentes, sin truncarla a la
  página del tablero. Un corte puede conservar pendientes o configuración
  documental incompleta; no requiere P12–P15.
- La transacción `SERIALIZABLE` reserva una nueva revisión del proceso y
  confirma juntos el corte y su auditoría. No cambia el estado del proceso.
  Revisiones obsoletas y conflictos concurrentes devuelven 412 `STALE_PROCESS`.
- El JSON canónico almacenado se sella con SHA-256. Las listas se ordenan
  antes del sellado y las fechas se normalizan a milisegundos para conservar
  la misma representación entre JDBC/PostgreSQL/SQLite. La huella incluye
  los metadatos del corte, no solo sus porcentajes.
- GET `/api/v1/processes/{processId}/documentary-snapshots` pagina metadatos.
  GET `/api/v1/documentary-snapshots/{id}` reautoriza antes de cargar el
  contenido, verifica huella y metadatos y devuelve los datos conservados,
  sin recalcular el avance actual. El acceso exige `PROGRESS_READ` sobre
  el proceso completo para no revelar agregados históricos a un ámbito parcial.
- Las respuestas usan `Cache-Control: no-store`; POST y detalle identifican
  el contenido canónico almacenado mediante ETag. Campos adicionales como
  `cutoffAt` se rechazan: no se simula un corte retroactivo de la propuesta v2.
- `/reporting/history` permite guardar y consultar cortes, muestra conteos,
  versiones, decisiones y huella, y retira el contenido ante errores de acceso
  o integridad. La pantalla obtiene la revisión actual antes de guardar;
  un conflicto requiere revisión del usuario, sin reintento automático.

## Persistencia y migración

`V016__documentary_reporting.sql` añade `documentary_snapshot`, restricciones
de conteos, JSON, huella y revisión única por proceso, e índices históricos.
Un trigger PostgreSQL prohíbe UPDATE y DELETE. El esquema SQLite y sus guards
de arranque aplican las restricciones equivalentes. No se editaron migraciones
históricas.

Se ejecutó `DocumentarySnapshotPersistenceTest` sobre un PostgreSQL **18.4**
temporal, aislado en esquemas descartables, en tres escenarios:

1. SQLite con el esquema auxiliar y guards reales.
2. PostgreSQL limpio, migraciones V001–V016 y `validate`.
3. PostgreSQL migrado primero hasta V015 y luego a V016, con `validate`.

Se comprobaron ida/vuelta del contenido, precisión temporal, unicidad por
revisión, conteos inválidos y rechazo de sobrescritura/borrado. Una prueba
adicional hizo competir dos transacciones serializables sobre la misma revisión:
solo una confirmó y el estado se conservó. La prueba de hash determinista y
rechazo de contenido alterado también pasó. **5 pruebas aprobadas, sin
fallos/errores/omisiones** para persistencia/concurrencia en esa ejecución opt-in,
además de las 5 pruebas HTTP. El servidor temporal se detuvo.

Comando de esa verificación, apuntando a una base temporal preparada:

```powershell
$env:SEC_RUN_FILE_DATABASE_TESTS = 'true'
$env:SEC_TEST_DB_URL = 'jdbc:postgresql://127.0.0.1:55436/postgres'
$env:SEC_TEST_DB_USER = 'sec_test'
$env:SEC_TEST_DB_PASSWORD = ''
.\mvnw.cmd -q '-Dtest=DocumentarySnapshotPersistenceTest,DocumentarySnapshotControllerTest' test
```

## Pruebas de aplicación e interfaz

| Verificación | Resultado |
|---|---|
| Suite backend completa (`mvnw.cmd -q test`, JDK 25) | 382 pruebas en 95 suites; 0 fallos, 0 errores, 11 omitidas |
| Suite Angular (`npm.cmd test`) | 57 pruebas aprobadas, 15 archivos |
| Lint Angular | Aprobado |
| Compilación Angular de producción | Aprobada |
| OpenAPI común/004/005, compatibilidad y AsyncAPI | Válidos; sin incompatibilidades |

Las 11 omisiones del backend son verificaciones condicionadas por el entorno,
incluida la carrera PostgreSQL opt-in que sí se ejecutó y pasó por separado.
El lint de contratos conserva la advertencia previa de `ReasonCommand` legado
sin uso. La CLI AsyncAPI avisó sobre telemetría/CIM restringidos, pero terminó
con código 0 y declaró válido el contrato.

- `DocumentarySnapshotServiceTest`: pendientes conservados, referencias acotadas,
  revisión obsoleta, cuenta técnica/sesión/capacidad, ámbito completo, estado
  activo, motivo válido y consulta histórica sin recálculo.
- `DocumentarySnapshotControllerTest`: Location/ETag, correlación textual segura,
  If-Match obligatorio/estricto/sin overflow, CSRF, rechazo de cutoff desconocido,
  lectura/paginación y error concurrente 412.
- `SqliteApplicationContextTest`: guardado mediante los beans reales, proceso
  todavía activo, revisión incrementada, lectura idéntica y rollback completo
  de revisión/corte si falla la auditoría.
- Angular: conteos históricos 75 %/50 %, motivo y revisión de proceso, proceso
  cerrado, contenido retirado ante fallo, cancelación al cambiar de proceso,
  conflicto sin reintento y rutas/cuerpo/cabeceras del cliente HTTP.

## Límites

Este avance conserva cortes; **no implementa la transición `DOCUMENTARY_CLOSED`**.
La revisión documental existente aún necesita coordinar sus escrituras con el
bloqueo de cierre. Antes de T064/T067 deben incorporarse ese bloqueo compartido,
la política de cierre, la comprobación de archivos y la prueba de carrera
cierre-vs-remisión/revisión. No se declara completada T060, T064, T066, T067 ni
T120 por disponer de un corte histórico.

La migración y la concurrencia de reserva se probaron en PostgreSQL. La prueba
de rollback del servicio completo se ejecutó en SQLite. No se ejecutó un
recorrido de navegador contra backend real en este avance ni aceptación
institucional. P12–P15 siguen fuera de este corte documental.
