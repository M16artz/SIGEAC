# D6: consulta de avance y tablero documental

Fecha: 2026-10-24. Implementación y verificación de T059/T063/T065.
Continúan pendientes el cierre documental y la aceptación completa del MVP.

## Comportamiento

- GET `/api/v1/processes/{processId}/documentary-progress` devuelve métricas
  actuales, versión del modelo, indicadores visibles y desglose paginado de
  fuentes con referencias a evidencia, versión y decisión documental.
- Requiere sesión humana y capacidad `PROGRESS_READ`. Comprueba el ámbito
  de cada indicador antes de seleccionar fuentes y calcular. Una fuente o
  indicador ajeno y uno inexistente devuelven el mismo 404 seguro.
- Los filtros v1 son `indicatorId`, `requiredSourceId`, `page` (desde 0) y
  `size` (1–100, por defecto 20). Los totales pertenecen al ámbito filtrado
  completo, no solo a la página. Los parámetros desconocidos devuelven
  422 `UNSUPPORTED_FILTER`; no se simula soporte de `cutoffAt`, criterio
  o responsable del contrato v2 planificado.
- La respuesta usa `Cache-Control: no-store`. Es una consulta actual,
  no una instantánea histórica ni un cierre. No devuelve un estado académico
  supuesto; los DTO del contrato previo de `/progress` no cambian.
- Los puertos públicos `EvidenceProgressPort` y `DocumentReviewProgressPort`
  aíslan reporting de los repositorios de otros módulos. Sus adaptadores
  seleccionan la última remisión por evidencia y la última revisión por versión;
  la aplicabilidad usa la mayor revisión por sujeto, sin depender del orden
  de marcas de tiempo.
- El piloto sintético concede `PROGRESS_READ` al coordinador. El permiso
  habilita también la consulta acotada del proceso para seleccionar el tablero.
  No habilita mutaciones ni concede capacidad documental a cuentas técnicas.
- `/reporting` ofrece selector de proceso, recopilación/revisión con fórmulas,
  casos no configurado/no aplicable, fuentes opcionales/excluidas, detalle por
  fuente y paginación. La valoración académica se explica por separado.
  Cambiar de proceso cancela la petición anterior; los errores eliminan datos
  previamente mostrados.

## Verificación ejecutada

| Comando | Resultado |
|---|---|
| `backend/mvnw.cmd -q test` | 365 pruebas, 0 fallos, 0 errores, 10 omitidas; JDK 25, 92 suites |
| `npm.cmd test` desde frontend | 51 pruebas aprobadas, 14 archivos |
| `npm.cmd run lint` | Aprobado |
| `npm.cmd run build` | Aprobado; carga diferida del tablero |
| `npm.cmd run contracts` | OpenAPI común/004/005, compatibilidad y AsyncAPI válidos |

Casos nuevos: 10 pruebas de ámbito/consulta, 5 HTTP y 3 de adaptadores SQLite;
integración Spring con autorización real del coordinador y rechazo del
responsable sin capacidad; lectura de procesos acotada por `PROGRESS_READ`;
dos reglas ArchUnit para puertos públicos e independencia de P12–P15;
5 pruebas del tablero y una del cliente HTTP.

El lint de OpenAPI mantiene una advertencia previa por `ReasonCommand` legado
sin uso. La CLI AsyncAPI avisó de restricciones de telemetría/CIM en el entorno,
pero terminó con código 0 y confirmó válido el contrato. Las 10 pruebas backend
omitidas dependen de condiciones de entorno y no se cuentan como aprobadas.

## Límites

No se ejecutó aceptación de navegador con backend real ni validación PostgreSQL
para estos adaptadores. No se midió rendimiento ni se certificó accesibilidad
en navegador. Las pruebas Angular cubren renderizado, etiquetas, errores,
paginación y cancelación de peticiones.

T060/T061/T064/T066/T067 y el resto de T120 permanecen abiertos: faltan
migración V016, persistencia histórica, cierre transaccional y recorrido
documental completo con P12–P15 deshabilitados. No se afirma aceptación
institucional ni cierre de D6.
