# APE4 — Plan reproducible de pruebas, sin ejecución nueva

El código `pruebas/test_seguridad_api.py` contiene siete tests pytest propuestos para el backend Spring actual; **no utiliza pytest-django ni implementa DRF**. No fue ejecutado. Sin `SDD_SECURITY_TESTS=1`, el módulo se omite; una omisión no es un control aprobado. Con gate activo, requiere fixtures sintéticos y rechaza host remoto, proxy heredado y redirecciones. El test de desafío 401 es una regresión prevista para HTTP-AA-01 y puede fallar hasta resolver el contrato.

La restricción de URL en el cliente no es una sandbox OS ni sustituye sus controles. Antes de ejecutar: entorno vacío con allowlist; fuente/toolchain de solo lectura; escritura solo en scratch; red externa deshabilitada y namespace loopback aislado; límites de CPU, memoria, procesos, archivo, disco y tiempo. Sin ello no ejecutar el backend ni el cliente. No usar servicios compartidos, producción o credenciales reales.

## Datos mínimos del fixture

- Cuenta administradora sintética con CAREER_ADMIN y cuenta lectora con CAREER_READ limitado a carrera A.
- Carrera B válida conocida, no asignada al lector. No obtener un UUID de otra persona real.
- `SDD_ADMIN_USERNAME/PASSWORD`, `SDD_READER_USERNAME/PASSWORD`, `SDD_FOREIGN_CAREER_ID`, `SDD_TEST_BASE_URL` por entorno del sandbox; no versionar sus valores reales.
- Base aislada PostgreSQL para aceptación; SQLite solo auxiliar y etiquetada. Boot/prod sin demo no crea usuarios por defecto: preparar fixture de prueba independiente, no activar demo en producción.
- Registrar versión de código/contrato, entorno y salida JUnit en scratch con límites; revisar antes de retener evidencia, sin secretos.

## Ejecución prevista dentro del sandbox ya preparado

```text
python -m pytest pruebas/test_seguridad_api.py -q --basetemp=<scratch>/pytest --junitxml=<scratch>/resultados.xml
```

Los marcadores `<scratch>` no son una ruta existente. No se instalan dependencias ni se descarga código para esta revisión. pytest/pytest-django/DRF deben prepararse con versiones acordadas y dependencias auditadas; no se declara que estén instalados.

Para expiración y secuencia de intentos, ejecutar en el mismo aislamiento las pruebas existentes con reloj controlado `SessionServiceTest`; para ámbito, `ScopeAuthorizationServiceTest` y `CareerAdministrationServiceTest`; para validez de datos, `CareerControllerTest`, `CareerTest` y servicios de proceso/auditoría. La tabla VT-01–12 detalla cobertura y vacíos. No esperar 30 minutos ni bloquear cuentas ajenas.

## Registro antes→corrección→después

CF-04-01: pendiente de resultado fallido reproducido, control/archivo exactos, cambio de código revisado, reejecución y prueba de regresión. No hay una corrección funcional de seguridad acreditada en esta entrega. El historial APE3 sirve de antecedente, no de ejecución APE4.
