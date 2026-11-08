# Corrección del contrato API y controles HTTP — 2026-11-08

**Resultado:** verificación local satisfactoria con límites explícitos. Base Git
`a61f37b7`, árbol modificado sin commit. No se hicieron commits ni push.

## Resultados ejecutados

| Verificación | Resultado real |
|---|---|
| Suite backend `mvnw.cmd --batch-mode test` | **580 casos: 563 exitosos, 0 fallos, 0 errores, 17 omitidos**. BUILD SUCCESS. |
| HTTP y esquemas OpenAPI | **19/19 PASS** contra instancia Spring Boot real, SQLite sintético aislado y contratos estáticos. |
| Angular | **114 pruebas, 31 archivos, todos PASS** en snapshot de fuentes idénticas en ruta temporal sin paréntesis. |
| Compilación Angular de producción | PASS, `npm run build`, en el mismo snapshot. |
| Adaptador de escenarios | `node --test scripts/api-response.test.mjs`: **4/4 PASS**. |
| Contratos | Seis OpenAPI válidos con Redocly; comparador de superficies sin incompatibilidades en rutas compartidas; AsyncAPI válido. Código final 0. |

Registros conservados y sanitizados:

- [Casos HTTP y resultado de cada esquema](api-response-smoke.json).
- [Resumen de suites, omitidos y hashes de fuentes](api-response-verification.json).
- [Verificación de enlaces y anclas del portal](api-response-docs-check.json).
- [Contrato y operación actual](../../../docs/api/response-contract.qmd).
- [Decisión ADR-0020](../../../docs/adr/0020-universal-api-response.md).

El resumen contiene 337 hashes SHA-256 de fuentes, configuración, contratos y
consumidores seleccionados. No contiene contraseñas, cookies, claves privadas,
tokens JWT ni tokens CSRF. Los informes generados de `target/` no se presentan
como única evidencia durable.

## Alcance HTTP demostrado

La instancia real respondió conforme al esquema documentado en los diecinueve
casos: acceso sin sesión, validación de acceso, login, alta de unidad, rechazo
CSRF, alta de carrera, lectura persistida, duplicado, campos incompletos,
paginación, If-Match ausente, actualización con revisión, revisión obsoleta,
recurso inexistente, login de lector, objeto ajeno, capacidad insuficiente,
logout vacío y sesión revocada. Los estados conservan 200/201/204/400/401/403/
404/409/412/428 según el caso.

El alta conserva Location y ETag. La lectura posterior recupera el UUID creado.
La paginación está en meta y las fechas del DTO siguen ISO tanto individualmente
como dentro de páginas. El 204 tiene cero bytes.

La prueba [ApiTransportIntegrationTest](../../../backend/src/test/java/ec/edu/unl/sec/contract/ApiTransportIntegrationTest.java) comprueba además CORS permitido/rechazado
y Swagger sin sobre de negocio. [OidcTransportIntegrationTest](../../../backend/src/test/java/ec/edu/unl/sec/contract/OidcTransportIntegrationTest.java) prueba en HTTP
real un Bearer firmado con claves efímeras, acceso local, escritura sin cookie,
rechazo de otro firmante/emisor/audiencia/expiración, permiso local insuficiente
e identidad desactivada; también rechaza tokens sin audiencia o sujeto obligatorio.
No contacta un proveedor externo.

[HttpBoundaryFilterTest](../../../backend/src/test/java/ec/edu/unl/sec/identityaccess/infrastructure/HttpBoundaryFilterTest.java) verifica rechazo de HTTP cuando HTTPS es obligatorio,
secretos en query, paginación inválida, 429, Retry-After, recuperación de ventana
y límite de memoria por clientes. [ApiResponseAdviceTest](../../../backend/src/test/java/ec/edu/unl/sec/shared/api/ApiResponseAdviceTest.java) cubre 204, 304, HEAD,
binarios, OpenAPI, fechas de página y sanitización de errores del framework.
La suite Angular verifica además que el token CSRF no se adjunte a URLs externas.

## Reproducción

En `backend/`, con JDK 21–25 y Maven Wrapper:

```powershell
$env:SEC_RUN_DATABASE_TESTS='false'
$env:SEC_RUN_FILE_DATABASE_TESTS='false'
$env:SEC_RUN_GOVERNANCE_DATABASE_TESTS='false'
$env:SEC_USE_TESTCONTAINERS='false'
$env:SEC_OIDC_ENABLED='false'
.\mvnw.cmd --batch-mode test
```

Para la comparación de esquemas, preparar únicamente la instancia sintética
descrita en la [guía SQLite](../../../docs/testing/sqlite.md), con puerto 18085
si se usa el valor predeterminado del script. Esperar a que el backend esté listo
y comprobar health antes de comenzar. Desde la raíz, en otra terminal:

```powershell
curl.exe --fail http://127.0.0.1:18085/api/v1/actuator/health
$env:SEC_API_BASE_URL='http://127.0.0.1:18085/api/v1/'
$env:SEC_API_SMOKE_SYNTHETIC='true'
node scripts/run-api-response-smoke.mjs
node --test scripts/api-response.test.mjs
```

El script crea registros sintéticos y rechaza destinos que no sean loopback.
Utiliza los usuarios sintéticos de la guía, nunca credenciales reales. Por
defecto guarda el informe bajo `backend/target/`; el registro enlazado arriba
corresponde a la ejecución efectuada y conservada en `tests/acceptance/results/`.

En `frontend/`: `npm run contracts`, `npm test` y `npm run build`.
En este entorno, Angular compiló en la ruta original, pero Vitest no encontró
pruebas porque su búsqueda trata los paréntesis de `New folder (2)` como patrón.
Se verificó que el patrón literal encuentra cero archivos y el escapado encuentra
el archivo existente. Se ejecutó una copia de `src` y configuraciones en una
ruta temporal sin paréntesis, enlazada a las mismas dependencias instaladas.
La comparación byte a byte de las fuentes originales y el snapshot dio **0
diferencias**. No se modificaron dependencias para eludir el problema.

## Fallos detectados y corregidos durante el trabajo

- La migración inicial detectó errores de compilación y aserciones del transporte
  anterior; se corrigieron y se repitió la regresión, sin ocultar esos fallos.
- La conversión entre Jackson 2 y Jackson 3 serializaba nodos JSON como objetos
  internos; se cambió el transporte intermedio a mapas/listas Java.
- CORS tenía dos candidatos de configuración: se seleccionó explícitamente el
  bean de la política de API.
- La primera comparación HTTP llegó antes de que el servidor aceptara conexiones;
  no se presentó como fallo de una operación. La reproducción exige esperar health.
- La comparación posterior pasó 4 de 5 casos antes de detectar que un esquema
  Problem omitía `instance/details`; se unificó en la fuente común.
- La siguiente pasó 9 de 10 antes de detectar fechas numéricas/arrays en páginas;
  se fijó formato ISO en una copia del mapper HTTP, preservando el mapper de snapshots.
- La comprobación final de diecinueve casos pasó completa tras ambas correcciones.

Los validadores emitieron advertencias de configuración Node y permisos WMI al
consultar el proceso padre en Windows; las definiciones se validaron y el comando
finalizó con código 0. No se interpretan esas advertencias como una prueba de CI
remota. El intento Maven offline inicial no tenía disponible el parent POM;
la ejecución autorizada con las dependencias resolvió ese bloqueo.

## Límites y pendientes

- Las 17 omitidas están enumeradas en el JSON y conservan sus condiciones de
  PostgreSQL/flags. No equivalen a 17 PASS y no prueban migraciones o concurrencia.
- No se ejecutaron pruebas de rendimiento ni una campaña completa de navegador.
- La configuración Nginx fue revisada y sus cuerpos de error JSON se analizaron,
  pero **no se ejecutó `nginx -t` ni se desplegaron certificados productivos**.
- La firma/claims JWT y el flujo HTTP se probaron localmente; falta conectar y
  comprobar un emisor institucional real con su configuración y provisión de sujetos.
- El cambio de JSON rompe el transporte anterior de v1: migrar consumidores
  externos o publicar versión nueva si v1 ya está disponible fuera del equipo.
- Las participaciones y defensas individuales de ACD5 deben registrarse por el
  equipo; las pruebas automatizadas no las sustituyen.
