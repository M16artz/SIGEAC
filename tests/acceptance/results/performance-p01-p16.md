# T106 — Dataset y consultas paginadas con carga

Fecha: 2026-10-25. Resultado: **PASS técnico de consultas**.

Entorno: Windows, JDK 25, PostgreSQL 18.4 temporal con migraciones hasta V026,
k6 1.1.0 oficial y backend Spring Boot real en loopback. Sin Docker, sin mocks
y con datos exclusivamente sintéticos. No es una medición de producción.

Dataset verificado al arrancar el perfil `complete-workflow-performance-test`:
100 usuarios distintos, 500 indicadores y fuentes obligatorias, 10.000
evidencias independientes con una versión remitida cada una, cobertura explícita
y referencia externa sintética. La configuración se valida antes de publicar;
la siembra completa es transaccional y solo existe en el classpath de pruebas.

| Ejecución | Resultado |
|---|---|
| Comprobación breve: 1 VU, 5 s | PASS; p95 interactivo 721,65 ms, 110 solicitudes, 0 errores |
| Campaña: 30 VUs, 60 s | PASS; p95 interactivo **1,51 s**, límite <=2 s |
| Sesiones | 100 autenticadas y rotadas entre los 30 VUs |
| Solicitudes totales, incluida preparación | 1.746; 0 errores HTTP |
| Iteraciones | 822 |
| Comprobaciones | 2.471 aprobadas; 0 fallos |
| Duración total, incluida autenticación | Aproximadamente 107 s |

La campaña consulta páginas del avance documental e historial de cierres.
Verifica 500 fuentes distintas a pesar de las 10.000 versiones, recopilación
100 %, aceptación documental 0 %, páginas de 20 elementos y ausencia de
solapamiento entre páginas consecutivas. La autenticación queda fuera del
percentil interactivo. Cada iteración espera un segundo entre ciclos.

Reproducción completa en [README de rendimiento](../../performance/README.md).
El manifiesto está en `tests/fixtures/performance/complete-workflows.json` y
el runner en `tests/performance/complete-workflows.js`. El comando npm ya no
apunta al archivo inexistente `evidence-upload.js`.

Comando ejecutado desde la raíz con el k6 portátil descargado:

```powershell
.\backend\target\k6\k6-v1.1.0-windows-amd64\k6.exe run --quiet --summary-export backend/target/performance-30-sessions.json tests/performance/complete-workflows.js
```

El resumen y log íntegros se generan bajo `backend/target/`, sin cookies ni
tokens exportados. La prueba del modelo `CompleteWorkflowPerformanceDataTest`
también pasó. No se certifica el límite p95 de 15 s para carga de archivos,
otros endpoints ni una configuración distinta del servidor; RNF-007 y la
campaña operacional completa conservan ese alcance pendiente.
