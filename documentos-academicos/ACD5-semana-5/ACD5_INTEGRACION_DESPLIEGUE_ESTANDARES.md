# ACD 5 — Integración, despliegue y estándares del backend

Semana 5 · Actividad grupal con evidencia individual · SIGEAC / SEC-UNL. Datos personales, fecha y participación real pendientes; asignatura/docente de referencia APE4, no nuevos datos de una guía extendida no aportada.

## 1. Integración y demostración preparada

El backend real es monolito modular Spring, cliente Angular y PostgreSQL principal. Acceso/catálogo/preparación siguen las referencias AA2, C4 y contrato APE3. Se dispone de código y evidencia previa de 23 tests/22 intercambios en Spring/SQLite; no se presentan como nueva demostración de Semana5. DRF/sobre universal y controles de seguridad pendientes condicionan la aceptación del incremento.

Guion para demostrar en un entorno autorizado: identificar versión/artefacto; iniciar fixture y verificar esquema; autenticar usuario sintético; crear unidad/carrera, consultar/editar con ETag; probar rechazo de duplicado/revisión vieja/CSRF/ámbito; preparar proceso con referencias válidas; cerrar sesión y comprobar rechazo. Registrar estados, cuerpos/headers redactados y persistencia. Health200 o Swagger disponible no sustituyen este flujo. No se realizó esa presentación al docente.

Los [resultados y trazabilidad](../soporte-semanas-4-5/HALLAZGOS_Y_TRAZABILIDAD.md) separan antecedentes y verificaciones pendientes. La [propuesta de despliegue](../APE5-semana-5/INSTRUCCIONES_DESPLIEGUE.md) incluye Dockerfile/Compose locales, no una URL pública ni contenedor ejecutado. Docker/Podman ausentes y sandbox no acreditado impiden afirmar ejecución.

## 2. Revisión de despliegue y discusión de estándares

| Tema | Evidencia / discusión aplicada | Consecuencia para el incremento |
|---|---|---|
| HTTP/REST | GET consulta; POST crea/prepara; PATCH modifica bloque editable; DELETE revoca sesión; 201/204/412/428 | Respetar método/estado/headers y no añadir sobre a204. REST no equivale a CRUD sin reglas ni a rutas que conceden permiso. |
| JSON/OpenAPI | Observado y objetivo APE3 son contratos diferentes; schemas y muestras previas válidos | Cliente debe interpretar versión/shape correcto. YAML válido no demuestra endpoints ni autorización. |
| Interoperabilidad | Cookie/CSRF y CORS/TLS dependen de origen/despliegue; legacy model puede409 | Documentar mecanismo real; no cambiarlo a JWT por conveniencia ni asumir v2 operativa. |
| Mantenibilidad | Módulos api/application/domain/infrastructure, puertos y pruebas ArchUnit; AA3 ADR propuesto | Conservar negocio fuera de serialización; cambios contractuales requieren revisión de cliente/tests. |
| Portabilidad | Java/POM, imagen runtime fijada, PostgreSQL y volúmenes | Imagen no elimina incompatibilidad de CPU, filesystem, schema o DB. SQLite no sustituye semántica PostgreSQL. |
| Contenedores | Contexto allowlist JAR; usuario10001, raíz RO, red interna, recursos, sin secretos en imagen | Son propuestas sin validación de motor, permisos, imagen, arranque o seguridad efectiva. |
| Seguridad | Prod, demo false, cookieSecure, secretos externos y VT; DB owner simplificado | Falta TLS validado y separación de cuenta migradora/runtime para producción; no publicar como seguro. |
| Reproducibilidad | Commit/hash JAR/digest imagen/dependencias, instrucciones de migración y backup/restore | Sin esos registros y ejecución no hay despliegue reproducido, aunque exista Dockerfile. |
| CI | backend-ci usa activación manual | No se activó Actions ni se cambió política. Una definición no acredita job verde nuevo. |

Alternativas de despliegue: ejecución local Java+PostgreSQL conserva menor dependencia de motor pero requiere alinear toolchain; contenedor estandariza empaquetado pero añade imágenes, volúmenes, red y operación. Para esta entrega se prepara la segunda como artefacto, sin hacerla obligatoria para desarrollo ni contradecir la constitución que permite ejecutar sin Docker. No se elige proveedor cloud ni se consumen servicios/pagos.

## 3. Retroalimentación final y evidencia individual

| Registro | Evidencia necesaria | Estado |
|---|---|---|
| DEMO-05-01 | Docente/fecha, entorno, flujo, pasos/respuestas y resultado | Pendiente. |
| FEEDBACK-05-01 | Observación final real, decisión, cambio/justificación y vínculo | Pendiente; no comentario fabricado. |
| REFLEXION-05-01 | Reflexión individual sustentada en un estándar y una diferencia observada | Material de apoyo preparado; autor/revisión real pendientes. |

Argumento disponible para reflexión: mantener códigos correctos y un schema no basta si un cliente espera csrfToken en otra posición; el sobre cambia interoperabilidad. De forma análoga, una imagen arrancada no prueba permisos/DB restaurable. La decisión responsable es conservar trazabilidad y límites en vez de declarar verde una demostración no realizada. El estudiante debe revisar, contrastar y registrar su propio razonamiento, no atribuirse una implementación que no ejecutó.

Evidencia grupal: guion, resultados anteriores identificados, revisión de despliegue, matriz de riesgos y documentación. Criterios: funcionamiento todavía parcial; interpretación de estándares desarrollada con fuentes; participación pendiente de evidencia real. No se modificó el backend ni se ejecutó/desplegó para esta ACD. Fuentes en [registro común](../soporte-semanas-4-5/FUENTES.md); nota individual y LaTeX en esta carpeta.
