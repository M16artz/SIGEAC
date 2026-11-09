# Pruebas de API con Postman

Requiere Node, Postman CLI y el backend iniciado en `http://127.0.0.1:8080/api/v1`.
Para el entorno sintético, seguir [la guía SQLite](../../docs/testing/sqlite.md).

Desde la raíz:

```powershell
npm.cmd --prefix frontend run test:api
```

La colección ejecuta, en orden, validación del ingreso, rechazo de origen externo y su preflight,
ingreso con cookie y token CSRF, identidad actual, unidades académicas, carreras,
modelos, procesos, errores de paginación e identificadores, rechazo del cierre sin
CSRF, cierre válido y rechazo del acceso después del cierre. No crea ni modifica
datos académicos; sí genera una sesión y eventos de auditoría.

Usa las credenciales sintéticas `admin` / `Synthetic-Admin-2026`. Para otra instancia
o credenciales, definirlas en el proceso antes de ejecutar:

```powershell
$env:SEC_API_BASE_URL = 'http://127.0.0.1:8080/api/v1'
$env:SEC_API_USERNAME = 'admin'
$env:SEC_API_PASSWORD = 'Synthetic-Admin-2026'
npm.cmd --prefix frontend run test:api
```

El usuario debe poder consultar los catálogos y procesos. Para comprobar también el
proxy Angular, usar `SEC_API_BASE_URL=http://localhost:4200/api/v1`, con el frontend
iniciado. El runner calcula el origen a partir de esa URL. Un proxy que altera el
host provocará fallos en las peticiones con `Origin` y detectará la regresión CORS.

El comando devuelve un código distinto de cero si falla una prueba y escribe el
reporte JUnit en `backend/target/postman-api/results.xml`. La ejecución usa archivos
locales y desactiva el reporte de eventos a Postman con `--no-report-events`.
Las credenciales personalizadas se escriben en un archivo temporal que se elimina
al finalizar; no se guardan en el repositorio ni se pasan como argumentos de CLI.

Si el CLI no está junto a Node, definir `SEC_POSTMAN_CLI` con la ruta absoluta a
`postman-cli/bin/postman.js` de su instalación (igual que en el runner de contratos).

## Usar la aplicación Postman

Importar `sigeac.postman_collection.json` y `local.postman_environment.json`,
seleccionar el entorno local y ejecutar toda la colección en orden con Collection
Runner. Comenzar con las cookies de ese host vacías. Para otro servidor, ajustar
`baseUrl` y `baseOrigin` (protocolo, host y puerto, sin `/api/v1`) y configurar las
credenciales localmente. No exportar credenciales reales al repositorio.

Estas pruebas verifican respuestas HTTP, cookies y decisiones CORS del servidor.
El bloqueo CORS que aplica el navegador requiere una prueba de navegador adicional.
La colección especializada `npm.cmd --prefix frontend run contracts:postman`
continúa disponible y requiere sus propios fixtures de rendimiento.
