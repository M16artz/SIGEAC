# APE3 — Verificación reproducible y resultados

## Alcance y condiciones

Campaña sobre el backend **Spring Boot existente**, no DRF. Base auxiliar sintética y separada: `backend/target/ape3-verification-20261031.sqlite`; puerto local 18093; archivos bajo backend/target. No se usa ni se modifica una base PostgreSQL institucional. La fecha/hora exactas figuran en los JSON; no se atribuyen a una sesión de clase.

No se modificó código de aplicación, migraciones ni contratos originales. La ejecución Maven puede actualizar `backend/target`; la prueba HTTP crea unidades/carreras/procesos sintéticos en la base aislada. Esa base no se entrega como evidencia normativa ni se versiona. Las credenciales del perfil demo están ya en fixtures del repositorio; usar exclusivamente este entorno.

## 1. Pruebas existentes ejecutadas

Desde `backend`, se ejecutó:

```powershell
.\mvnw.cmd "-Dtest=CareerControllerTest,CareerAdministrationServiceTest,CareerTest,ModuleArchitectureTest" test
```

Resultado: **BUILD SUCCESS; 23 tests, 0 fallos, 0 errores, 0 omitidos**. Desglose: controlador 3; aplicación 5; dominio 2; arquitectura 13. [Resumen y nombres](evidencias/pruebas-maven.json), extraídos de los XML Surefire de esa ejecución. No se copiaron reportes antiguos de otras suites como si se hubieran ejecutado en APE3.

El primer intento offline falló por parent no disponible para ese contexto; el intento de red dentro del sandbox falló con `Permission denied: getsockopt`. Tras la autorización de acceso, el comando terminó correctamente. Estos fallos de entorno no son fallos de las 23 pruebas ni se ocultan como ejecución exitosa inicial.

MockMvc standalone usa servicio simulado en CareerControllerTest; no acredita por sí solo filtros, DB o recorrido extremo a extremo. Las pruebas de aplicación/dominio y ArchUnit tienen límites diferentes.

## 2. Arranque temporal reproducido

Desde `backend`:

```powershell
.\mvnw.cmd -o "-Dspring-boot.run.useTestClasspath=true" "-Dspring-boot.run.arguments=--spring.profiles.active=sqlite-test --server.address=127.0.0.1 --server.port=18093 --spring.datasource.url=jdbc:sqlite:./target/ape3-verification-20261031.sqlite --sec.files.root=./target/ape3-files" spring-boot:run
```

Requiere dependencias previamente disponibles; el arranque final autorizado usó el contexto que las tenía. Se observó Tomcat en 127.0.0.1:18093 con base `/api/v1`, Java 25 y Spring Boot 4.1.1. El proceso temporal se detuvo al terminar las comprobaciones; no es un servicio productivo instalado. Para repetir, iniciar de nuevo y detener con Ctrl+C al finalizar.

Si la ruta de DB ya existe, el cliente genera códigos únicos y no borra datos. Para ensayos sobre PostgreSQL, preparar un entorno de pruebas distinto y seguir la configuración principal; no extrapolar garantías de SQLite.

## 3. Recorrido HTTP ejecutado

Desde la raíz, con el servidor iniciado:

```powershell
python documentos-academicos/APE3-semana-3/verificacion/verificar_http.py
```

El script exige el host/puerto local indicados y usa urllib, cookie jar, CSRF y aserciones. No necesita Postman ni Django. Guarda metadatos y JSON de respuestas redactados; no conserva cookie, contraseña ni csrfToken auténticos. No muestra tokens en consola.

| Casos | Comprobación | Resultado final |
|---|---|---|
| HTTP-01/02 | Acceso sin sesión 401; login 201 | Correcto. |
| HTTP-03 | Crear unidad; Location corresponde al ID | 201 correcto. |
| HTTP-04/05 | CSRF ausente y DTO incompleto | 403 y 400 correctos. |
| HTTP-06/07 | Alta de carrera, ETag 1, Location; duplicado normalizado | 201 y 409 correctos. |
| HTTP-08–12 | Consultar; PATCH sin precondición; cambiar; revisión obsoleta; historial | 200/428/200/412/200; revisión 2 e historial de dos elementos. |
| HTTP-13/14 | Búsqueda por código e ID inexistente | Página con un resultado; 404. |
| HTTP-15 | Registrar desactivación con revisión 2 | 201, DISABLED, revisión 3. |
| HTTP-16/16B | Leer modelo sintético configurado mediante DTO legado / completo | 409 MODEL_VERSION_REQUIRES_V2; 200 PUBLISHED en /configured. |
| HTTP-17–19 | Preparar en carrera piloto autorizada; consultar detalle/lista | 201 PREPARATION/revisión 0/ETag 0; 200 y 200. |
| HTTP-20/21 | Logout y acceso posterior | 204, cero bytes; 401 posterior. |

**Total final: 22 casos completados.** [Resultado final](evidencias/http-resultados.json). El [intento inicial](evidencias/http-intento-inicial.json) conserva la expectativa errónea de 200 ante el modelo legado y el 409 observado; no se reescribe para esconder el hallazgo. La corrección fue del escenario, con fuente `LegacyModelVersion.from` / `EvaluationModelService.requireLegacy`, no del código de negocio.

La prueba de concurrencia utiliza revisiones secuenciales dentro de un cliente. No demuestra dos solicitudes simultáneas, bloqueo de DB ni resistencia a carga. La prueba no induce fallo de auditoría ni verifica todos los permisos de lector/ámbito.

## 4. Contratos y muestras

Generación reproducible del subconjunto, desde raíz:

```powershell
node documentos-academicos/APE3-semana-3/verificacion/generar_contratos.mjs
node frontend/node_modules/@redocly/cli/bin/cli.js lint documentos-academicos/APE3-semana-3/contratos/openapi-observado.yaml documentos-academicos/APE3-semana-3/contratos/openapi-objetivo.yaml --format=stylish
node documentos-academicos/APE3-semana-3/verificacion/validar_respuestas.mjs
```

Se usan dependencias ya instaladas en frontend/node_modules. Redocly acepta ambos OpenAPI, cada uno con 14 operaciones. El objetivo declara explícitamente que no está implementado. Ajv verifica los cuerpos de las 22 muestras contra el observado; [resultado](evidencias/esquemas-resultados.json). La comprobación también exige estado/media type documentados y cuerpo ausente en 204. No equivale a validar todas las solicitudes, headers, casos negativos, seguridad ni totalidad de esquemas del producto.

La generación inicial duplicaba CSRF cuando ya estaba por referencia; el lint lo detectó y se corrigió la detección de parámetros referenciados. La validación de muestras detectó 400 de DTO ausente en la respuesta de catálogo heredada; se añadió al subconjunto académico según el handler real. No se modificaron los contratos originales ni se ocultaron reglas de lint.

## 5. Swagger, diagramas y documentación

GET a `/api/v1/swagger-ui/index.html` y `/api/v1/v3/api-docs` devolvió 200 con sus tipos de contenido. [Evidencia](evidencias/swagger-disponibilidad.json). Se verificó disponibilidad HTTP, no interacción visual ni envío de formularios Swagger. El OpenAPI académico es un artefacto separado del documento generado por SpringDoc.

Structurizr DSL y PlantUML se contrastaron con código y sintaxis documentada; sin parser/render oficial local. Quarto renderizó `tecnico/APE3.qmd` a HTML autónomo. Se abrió el HTML local en Edge mediante Playwright y se comprobó la presencia de tres SVG, ausencia de errores JavaScript y de desbordamiento horizontal a 1440 px; también se inspeccionó una captura. [Comprobación](evidencias/quarto-verificacion.json) y [captura de documentación](evidencias/quarto-documentacion.png), que no son evidencia de ejecución de la interfaz Angular. LaTeX se revisa estáticamente, sin motor ni PDF local.

## 6. Límites para la entrega

Pendientes: decisión Spring/DRF; implementación del sobre universal; aceptación PostgreSQL y migraciones; navegador Angular; validaciones negativas restantes; auditoría/privacidad completas; render de diagramas; compilación PDF; revisión docente; commits/PR y contribuciones. El 401 local no incluyó WWW-Authenticate entre los headers recibidos: aporta evidencia al hallazgo HTTP-AA-01, sin resolverlo ni introducir Bearer ficticio.
