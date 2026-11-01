# AA 5 — Dossier técnico del backend y reflexión sobre estándares

Semana5 · Actividad Autónoma · 4,5 horas previstas · Individual articulada con el proyecto. Estudiante/fecha/aporte reales pendientes. La guía resumida exige organizar, cerrar trazabilidad/bitácora y reflexionar con evidencia; no se inventan preguntas o rúbricas detalladas no aportadas.

## 1. Identificación, especificación y arquitectura

SIGEAC / SEC-UNL apoya autoevaluación de carreras con referencias, control por ámbito y trazabilidad, sin emitir acreditaciones. El incremento priorizado contiene acceso, catálogo y preparación/consulta de procesos; no se amplía a cargas/revisión/evaluación por existir más código. Actores funcionales A-AA-01–04 y necesidades N-AA-01–05 se conservan, sin aprobación institucional simulada.

| Elemento del dossier | Ruta desde la raíz | Estado que debe conservarse |
|---|---|---|
| Especificación inicial | `documentos-academicos/APE2-semana-2/specification.md` | Propuesta del recorte, no inventar aceptación. |
| Requisitos/HU/AC/fuentes | `documentos-academicos/AA2-semana-2/DOSSIER_REQUISITOS_AA2.md` y matriz | Trazabilidad inicial con pendientes de dominio/medición. |
| Procesos, C4 y recursos | `documentos-academicos/ACD3-semana-3/ACD3_CLINICA_ARQUITECTURA_API.md`, `ARQUITECTURA_C4.md` | Análisis académico con revisión posterior diferenciada. |
| DSL/UML y contratos | `documentos-academicos/APE3-semana-3/arquitectura/`, `contratos/` | Backend Spring actual; sobre objetivo no implementado; render oficial pendiente. |
| Decisión arquitectónica | `documentos-academicos/AA3-semana-3/ADR-AA3-001-adaptacion-respuestas-api.md` | Propuesto, no aceptado ni implementado. |
| Seguridad y riesgos | `documentos-academicos/ACD4-semana-4/`, `APE4-semana-4/`, `AA4-semana-4/`; soporte compartido | Modelo, hallazgos fuente/potenciales, tests propuestos; ejecución/patch pendientes. |
| Integración/despliegue | `documentos-academicos/APE5-semana-5/INSTRUCCIONES_DESPLIEGUE.md`, `despliegue/` | Receta y archivos nuevos, no build/URL/publicación realizados. |
| Trazabilidad de cierre | `MATRIZ_CIERRE.md` y `BITACORA.md` en esta carpeta | Corte administrativo con estados abiertos, no todo terminado. |

La arquitectura sigue monolito modular, Angular↔API Spring↔PostgreSQL, con archivos fuera de DB en el producto. SessionCookie/CSRF y scopes son fronteras relevantes. No se denomina microservicio a un componente C3 ni se presenta DRF como tecnología actual.

## 2. Contrato, pruebas, seguridad y despliegue

OpenAPI observado APE3 describe14 operaciones seleccionadas y DTO/Problem Details. El objetivo con success/message/data/errors/meta está separado y no implementado. El cliente actual consume csrfToken/ítems/errores en su forma vigente; cambiar servidor sin migrar cliente rompe interoperabilidad. AA3 propone adaptación explícita, pero no decide por el equipo.

Evidencia previa: 23 tests seleccionados y22 intercambios HTTP finales APE3 sobre Spring/SQLite; hubo un intento inicial de modelo legado409 y corrección de escenario. No se recuenta como tests nuevos APE4/5 ni parche de seguridad. Nuevos artefactos: siete tests pytest propuestos, Dockerfile/Compose, escaneo de metadatos y validaciones estáticas. Omisión, sintaxis válida o código escrito no significan control funcional ni despliegue.

Modelo de amenazas: A-01–08, T-01–09 y SEC-01–05 con P×I de planificación. Se conocen controles de sesión, scopes, CSRF, revisión y auditoría por fuente; no se confirma toda su operación. Auditoría formal incompleta por sandbox no establecido; antecedentes needs_validation no son vulnerabilidades confirmadas. Protección de secretos se revisó acotadamente sin retener valores, no se certificó ausencia total.

Despliegue: receta para artefacto nuevo, imagen por digest, contexto solo JAR, prod/env, API loopback y DB no pública. Sin motor/aislamiento, no build ni URL. La cuenta DB del fixture es dueña: falta separación migrador/runtime para producción. TLS/permisos/volúmenes/restore no se infieren del YAML. Publicar documentación estática separada evita habilitar Swagger en prod sin decisión.

## 3. Reflexión sustentada, cierre y contribución

Este es material argumentado de apoyo, no relato personal ya acreditado. La reflexión individual debe identificar qué leyó, cambió, probó y cómo afectó una decisión; no atribuir resultados ajenos o asistidos a una participación no registrada.

**Interoperabilidad:** HTTP aporta significado compartido a métodos, estados y headers, pero requiere aplicarlo: un204 no admite sobre; una revisión obsoleta usa precondición; JSON válido no prueba autorización. SEC-04 muestra que incluso401 necesita revisar el desafío. OpenAPI reduce ambigüedad, aunque mantener contrato observado y objetivo distintos obliga a explicitar qué es ejecutable.

**Mantenibilidad:** los límites api/application/domain/infrastructure y los ADR permiten localizar decisiones, no garantizan calidad por su nombre. SEC-01 exige revisar consulta/ámbito/cliente conjuntamente: agregar paginación solo al schema no limita consumo. SEC-02 enseña que una palabra del requisito, “consecutivos”, cambia el algoritmo y sus pruebas. SDD necesita esa relación de evidencia, no documentos decorativos.

**Portabilidad y reproducibilidad:** contenedor e imagen por digest ayudan a identificar runtime, pero no sustituyen JAR verificado, migraciones, DB/files restaurables ni permisos efectivos. SQLite auxiliar fue útil para evidencia previa, sin acreditar semántica PostgreSQL. El criterio responsable para Semana5 es registrar lo que falta y preparar una receta verificable; escribir “desplegado” sin motor o URL impediría reproducir el trabajo.

**Seguridad y responsabilidad:** distinguir amenaza, causa, evidencia y riesgo evita acusar una vulnerabilidad por pertenecer a OWASP. También evita cerrar una práctica por un escaneo sin coincidencias o por tests omitidos. Es razonable priorizar controles de identidad sin clasificar todos los candidatos como críticos. La revisión cruzada real, los commits y la reflexión personal son datos por aportar, no campos que pueda completar una herramienta con nombres ficticios.

La [matriz de cierre](MATRIZ_CIERRE.md) cierra el registro del corte, **no los requisitos pendientes**. La bitácora registra análisis, archivos y resultados existentes con fechas de sus evidencias; no inventa sesiones, horas, demos o feedback. Falta DRF/decisión, sobre universal, ejecución segura, aceptación PostgreSQL, correcciones, build/despliegue/publicación y contribución versionada.

Plan individual sugerido de4,5h: inventario45min, contraste60, cierre de matriz45, análisis de estándares60, reflexión y revisión60; total270min, sin atribuir ese tiempo como ejecutado. Acompañar entrega con enlace real cuando se versione y con [NOTA_INDIVIDUAL.md](NOTA_INDIVIDUAL.md).

Fuentes: registro compartido F/O/D/H, documentos exactos del dossier y guías aportadas. Se utilizó ChatGPT/Codex para análisis/organización y comprobación estática; no se declara implementación o despliegue que no se realizó.
