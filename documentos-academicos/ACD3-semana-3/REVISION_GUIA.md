# ACD3 — Revisión de los diez puntos de la guía

Proyecto SIGEAC / SEC-UNL · Semana 3 · Trabajo grupal con evidencia individual.

El paquete ACD3 ya estaba elaborado. Esta revisión lo contrasta con la guía nuevamente aportada y precisa el contrato mediante evidencia posterior de APE3. No crea una actividad distinta, no modifica código ni acredita una sesión docente realizada. Los resultados de implementación/pruebas posteriores no se presentan como decisiones previas a implementar.

## Localización de cada actividad

| N.º | Actividad | Archivo y sección | Desarrollo disponible | Pendiente real |
|---|---|---|---|---|
| 1 | Revisar requisitos y flujo | `ACD3_CLINICA_ARQUITECTURA_API.md`, §§1 y 6 | Flujos de catálogo y preparación; actores A-AA-01–04; RF, historias y AC de AA2; operaciones de negocio. | Ratificar alcance de Unidad 1 y asignaciones funcionales con equipo/docente. |
| 2 | Análisis textual | Informe, §2 | Participantes y responsabilidades, inicio, precondiciones, tareas en infinitivo, decisiones, datos, excepciones y resultado de F-ACD3-01/02. | Revisar el proceso con los participantes reales; no sustituir validación del dominio por código. |
| 3 | C4 C1/C2/C3 | `ARQUITECTURA_C4.md`, secciones C1–C3 | Personas/sistema; Angular/Spring/PostgreSQL; componentes API, aplicación, dominio, identidad, persistencia y auditoría. No se inventa una integración activa CACES/SSO. | Revisión de fronteras y responsabilidades durante la clínica. |
| 4 | Decisiones arquitectónicas | Informe, §4 | DA-01–08 con evidencia, alternativas, ventajas/desventajas y estado; monolito modular, capas, tecnologías, persistencia, puertos, sesiones, revisiones y compatibilidad. | Decidir diferencias y registrar acuerdos reales. Las alternativas son análisis preparado, no discusión ya celebrada. |
| 5 | Recursos API | Informe, §5; `CONTRATO_API_INICIAL.md`, §2 | Sesión, identidad propia, unidad, carrera, historia, decisión de estado, modelo/versión y proceso. | Confirmar representación del recurso de decisión de estado, sin inventar una ruta de consulta. |
| 6 | Operaciones preliminares | Contrato, §§2 y 6 | 18 operaciones implementadas identificadas con método/ruta/requisito; GET, POST, PATCH y DELETE. PUT se explica, sin inventar un endpoint. | Revisar PATCH con bloque editable completo y compatibilidad de clientes. |
| 7 | Contrato inicial de respuesta | Contrato, §§4–5 | Sobre success/message/data/errors/meta propuesto para éxito, errores, autorización, ausencia, colecciones y metadatos. 204 sin cuerpo. Separación explícita frente a DTO directos/Problem Details existentes. | Aceptar política de migración o excepción; el sobre no se da por implementado. |
| 8 | Códigos HTTP | Contrato, §§2, 5 y 6 | Estados asociados a causas: entrada, autenticación, permisos, inexistencia, duplicado, revisión, reglas de vigencia y fallos. | Validar escenarios no ejecutados; no afirmar que todo estado ocurre en todo endpoint. |
| 9 | Entradas y validaciones | Contrato, §§2–3 | UUID, filtros/páginas, DTO, obligatoriedad/longitud/fechas, unidad activa, unicidad, modelo publicado, cookie, CSRF y ETag/If-Match. | Confirmar reglas institucionales y completar pruebas de variantes. |
| 10 | Clínica y ajustes | Informe, §7; `REGISTRO_OBSERVACIONES.md` | Guion de exposición, preguntas, hallazgos técnicos y tabla para observación→decisión→cambio/justificación→evidencia. | Exposición, observaciones auténticas del docente/otros equipos y responsables identificados. No se inventan. |

## Cadena representativa para exponer

**Problema/necesidad:** corregir carreras sin perder identidad e historia, N-AA-02/05. **Requisito:** AA-RF-08. **Historia:** HU-AA-04. **Aceptación:** AC-AA-14/15. **Proceso:** F-ACD3-01. **Arquitectura:** API de catálogo → CareerAdministrationService → dominio/repositorio/auditoría. **Recurso:** carrera. **Operación:** PATCH `/api/v1/careers/{id}` con datos editables completos e If-Match. **Resultado esperado:** 200 y revisión nueva, o 412 si quedó obsoleta.

La prueba posterior APE3 HTTP-09–12 verifica precondición, edición e historial de forma secuencial en SQLite auxiliar. No acredita por sí sola concurrencia simultánea, permisos de dos personas, atomicidad ante fallo de auditoría ni aceptación PostgreSQL.

## Precisión incorporada desde APE3

Fuentes de evidencia: `documentos-academicos/APE3-semana-3/evidencias/http-intento-inicial.json` y `http-resultados.json`; casos HTTP-16/16B. Se observó 409 MODEL_VERSION_REQUIRES_V2 al pedir la representación legada de una versión configurada, y 200 al consultar `/api/v1/model-versions/{id}/configured`.

El comportamiento se corresponde con `backend/src/main/java/ec/edu/unl/sec/evaluationmodel/api/LegacyModelVersion.java`, método from, que llama a `EvaluationModelService.requireLegacy`. Se corrigió OP-14 y su explicación en Markdown/LaTeX; se añadió H-09. La ruta configurada permanece complementaria al inventario original de 18 operaciones. No se cambia la versión base ni se supone un servidor v2 desplegado porque el código de error lo mencione.

APE3 también aporta 23 tests seleccionados y 22 casos HTTP finales, documentados en su propia carpeta. Se citan como evidencia posterior, no se reejecutaron en esta revisión ACD3 y no reemplazan la clínica. El sobre universal sigue sin implementar en el producto.

## Evidencia grupal e individual

**Grupal preparada:** informe, análisis textual, C4, tabla de recursos/operaciones/entradas/estados, decisiones y registro de observaciones. Abrir el [índice](README.md) para acceder a todos los archivos.

**Individual pendiente de atribución:** cada integrante debe identificar una cadena RF→AC→proceso→componente→endpoint, explicar una decisión y alternativa, registrar el aporte efectivamente realizado y enlazar evidencia real. Utilizar [EVIDENCIA_INDIVIDUAL.md](EVIDENCIA_INDIVIDUAL.md); no copiar un comentario docente ficticio ni utilizar el commit base como prueba de contribución a esta actividad.

## Estado de entrega

Los diez puntos tienen desarrollo documental o mecanismo explícito de registro. La celebración de la clínica, la aprobación de decisiones y la participación individual aún requieren evidencia real. El informe LaTeX conserva FORMATO.tex y no se compiló localmente. Esta revisión conserva los documentos de APE2, AA2 y APE3 sin cambios.
