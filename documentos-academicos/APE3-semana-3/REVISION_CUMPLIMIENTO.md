# APE3 — Revisión de cumplimiento de la guía 003

## Dictamen

**La entrega tiene desarrollo documental y evidencia técnica parcial, pero no cumple íntegramente la práctica tal como está redactada.** Los dos incumplimientos principales son el paso 6 con Django REST Framework y la aplicación efectiva del sobre universal del paso 4. El backend demostrado es Spring Boot y devuelve DTO directos/Problem Details. Una propuesta OpenAPI válida no implementa ese comportamiento ni concede una excepción docente.

Esta revisión no migra el producto, no acepta ADR, no publica cambios ni inventa retroalimentación. Se contrastaron los archivos existentes con la guía aportada, se repitieron validaciones estáticas/de muestras y se corrigió una inconsistencia documental de paginación.

**Estados:** Documentado = artefacto disponible; Verificado parcialmente = comprobación con límites identificados; Pendiente = falta evidencia o ejecución; No cumplido = hay una diferencia comprobada respecto de una exigencia expresa. Estos estados no equivalen a calificación docente.

## 1. Datos generales y objetivos

`APE3_SEMANA_3.md`, tabla inicial, y `APE3_SEMANA_3.tex`, tabla institucional, corresponden a Desarrollo Basado en Plataformas, 5 A, Unidad 1, práctica 003, Edison Leonardo Coronel Romero, Semana 3, horario por definir, aula/laboratorio y tres horas planificadas. El resultado de aprendizaje y los cinco objetivos están incorporados. Integrantes y fecha real siguen pendientes, sin reutilizar nombres de una plantilla ajena.

El objetivo de arquitectura/trazabilidad tiene desarrollo; el de contrato tiene propuesta y contraste; el de endpoints aporta ejecución de implementación previa Spring, **no nueva implementación DRF**. No se debe presentar la actividad completa como aprobada.

## 2. Procedimiento: evidencia por actividad

| Punto de la guía | Evidencia exacta | Estado | Qué falta / acción concreta |
|---|---|---|---|
| Inicio 1: revisar Semana 2 | Reporte, “Inicio”; `../APE2-semana-2/specification.md`; dossier/matriz AA2; `TRAZABILIDAD.md` | Documentado | Confirmar recorte/actores con equipo; los estados de validación institucional no se infieren. |
| Inicio 2: seleccionar flujos | Reporte, “Inicio” y Paso 1; F-ACD3-01/02 | Documentado | Ratificar que catálogo y preparación corresponden al incremento de Unidad 1. |
| Inicio 3: revisar retroalimentación ACD3 | `../ACD3-semana-3/REGISTRO_OBSERVACIONES.md`; `OBSERVACIONES.md` | Pendiente de retroalimentación real | Hay hallazgos técnicos, no observaciones docentes acreditadas. Incorporar las reales cuando se reciban. |
| Paso 1: análisis textual | Reporte, Paso 1; informe ACD3 §2 | Documentado | Incluye participantes, actividades en infinitivo, decisiones, datos, excepciones y resultados; validar con actores reales. |
| Paso 2: C4 y UML | `arquitectura/workspace.dsl`, vistas C1/C2/C3; `catalogo-clases.puml`; `alta-carrera-secuencia.puml` | Documentado; verificación de motores pendiente | Fuentes editables disponibles. Abrir/validar/renderizar con herramientas correspondientes y corregir si se detectan errores; los SVG Mermaid del HTML no prueban el parseo de estos archivos. |
| Paso 3: ADR | `adr/APE3-001-alineacion-tecnologica.md`; `adr/APE3-002-contrato-universal.md`; ADR-0001/0004/0018 del producto | Documentado como propuesta | Elegir y registrar decisiones reales; no cambiar estado a aceptado sin evidencia. |
| Paso 4: contrato y sobre universal | `contratos/openapi-observado.yaml`; `openapi-objetivo.yaml`; generador | No cumplido en ejecución del sobre; documentación disponible | El sobre está solo propuesto. Implementarlo en la superficie aceptada, incluyendo errores de filtros/handlers, cliente y exclusiones sin cuerpo; o registrar excepción docente expresa. |
| Paso 5: OpenAPI | Dos YAML de 14 operaciones; `evidencias/openapi-lint.json`; revalidación de esta revisión | Verificado parcialmente | Ambos archivos válidos; observado y objetivo tienen estados diferentes. Tras implementar el contrato aceptado, contrastar especificación definitiva y backend. |
| Paso 6: endpoints DRF | Código actual `backend/src/main/java/ec/edu/unl/sec/`; POM; ADR APE3-001 | No cumplido | Implementación comprobada Spring; Django/DRF no encontrados en el intérprete Python inspeccionado. Elegir incremento DRF o justificar excepción aceptada, sin etiquetar Java como DRF. |
| Paso 7: solicitudes y validación | `verificacion/verificar_http.py`; `evidencias/http-resultados.json`; `esquemas-resultados.json`; `pruebas-maven.json` | Verificado parcialmente | 22 intercambios previos y 23 tests seleccionados sobre Spring/SQLite; se revalidaron solo muestras. Repetir flujo del backend/contrato aceptados, completar negativos pertinentes y aceptación PostgreSQL. |
| Paso 8: trazabilidad/documentación | `TRAZABILIDAD.md`; `tecnico/APE3.qmd` y HTML; reporte y fuentes | Documentado | Conectar futuras implementaciones/commits con cada AC; no marcar AC completo por cobertura parcial. |
| Cierre 1: presentar flujo al docente | Reporte, “Paso 8 y cierre”; registro vacío de sesión | Pendiente | Hay recorrido por cliente HTTP→API→servicios→SQLite; no hay evidencia de presentación docente. La guía no exige expresamente Angular: definir el extremo a extremo del backend seleccionado, sin añadir una obligación de UI no indicada. |
| Cierre 2: observaciones/ajustes | `OBSERVACIONES.md`, APE3-OBS-01 | Pendiente | Registrar persona/fecha, observación, decisión, ajuste o justificación y evidencia real. |
| Cierre 3: versionado/aporte | `NOTA_INDIVIDUAL.md`; Git observado en main sin commit de la entrega | Pendiente | Archivos versionables no son historial de cambios comprometidos. Aplicar estrategia GitFlow acordada, revisión y contribuciones identificables; no fabricar commits o autores. |

## 3. Herramientas y resultados esperados

| Exigencia / resultado | Hallazgo verificable | Evaluación |
|---|---|---|
| Git/GitHub y GitFlow | Existe repositorio; paquete académico no comprometido; sin evidencia nueva de feature/develop/PR | Falta evidencia del flujo y contribución. No se publica por el hecho de generar Markdown. |
| Spec Kit | Constitución, plantillas `.specify/` e insumos SDD presentes | Insumos disponibles; no se acredita nueva ejecución de comandos Spec Kit durante APE3. |
| Python/Django/DRF | Python se utilizó como cliente HTTP; backend es Java | La herramienta cliente no satisface el requisito de implementar con DRF. |
| PostgreSQL | Configuración principal existente; campaña APE3 usó perfil SQLite | No hay aceptación PostgreSQL de esta campaña. |
| C4/UML inicial | Un DSL con tres vistas y dos fuentes PlantUML | Artefactos disponibles; render/parseo oficial no acreditados. |
| Decisiones justificadas | ADR con contexto, alternativas y consecuencias | Cumple elaboración documental, sin aceptación atribuida. |
| OpenAPI/Swagger | Dos contratos válidos; disponibilidad HTTP Swagger/api-docs 200 en evidencia previa | No equivale a implementar sobre, ejecutar operaciones desde Swagger ni validar todos los caminos. |
| Cliente HTTP equivalente | urllib con cookie jar/CSRF y aserciones de casos | Es herramienta equivalente admisible; no es obligatorio repetir con Postman solo por estar instalado. |
| Endpoints implementados/ejecutables | Spring existente ejecutado, no código DRF nuevo | Resultado funcional parcial, tecnología exigida sin cumplir. |
| Respuestas/códigos/validaciones | Evidencia HTTP y muestras de cuerpo con Ajv | Cobertura acotada; headers/errores/reglas restantes y sobre objetivo requieren prueba. |
| Quarto/documentación | QMD, HTML autónomo y verificación de documentación con tres SVG | Render y revisión de documentación acreditados; no confundir con UI del producto. |
| Historial y evidencia individual | Nota preparada, integrantes/enlaces pendientes | Falta atribución real y versionado. |

## 4. Preguntas de control

Las siete preguntas están respondidas individualmente en `APE3_SEMANA_3.md`, sección “Preguntas de Control”, y en el LaTeX. Se verificó su presencia y correspondencia temática:

| N.º | Tema | Estado |
|---|---|---|
| 1 | Diferenciar C1/C2/C3 | Respondida, aplicada a SIGEAC. |
| 2 | Diferenciar clases y arquitectura | Respondida, UML como complemento. |
| 3 | Relacionar requisito/recurso/endpoint | Respondida con AA-RF-06 y carrera. |
| 4 | Seleccionar método HTTP | Respondida sin inventar PUT implementado. |
| 5 | Información mínima OpenAPI | Respondida con método, ruta, entradas, respuestas, schemas y seguridad. |
| 6 | Motivo para usar ADR | Respondida, distingue propuesta y aceptación. |
| 7 | Comprobar trazabilidad | Respondida con AA-RF-08, AC y casos de revisión; reconoce límites. |

Tener respuestas correctas documentadas no prueba participación individual ni compensa los pasos de implementación pendientes.

## 5. Correcciones y comprobaciones de esta revisión

1. **Corregida inconsistencia de paginación:** APE3-002 decía paginación en meta, pero el objetivo generado conserva CatalogPage dentro de data. ADR y anexo LaTeX ahora describen esa forma real de la propuesta, mantienen la elección pendiente y no duplican totales. No se modificó el contrato ni el negocio.
2. **Diferenciados los niveles de evidencia:** un YAML válido no es una API implementada; SVG de documentación no son parseo Structurizr/PlantUML; 200 de Swagger no es flujo interactivo; SQLite no es aceptación PostgreSQL.
3. **Repetidos lint y validación de muestras:** Redocly acepta ambos YAML y Ajv acepta las 22 respuestas guardadas. No se levantó backend ni se repitieron Maven o las solicitudes HTTP. `esquemas-resultados.json` registra la nueva fecha de validación de la misma evidencia; los JSON HTTP/Maven originales permanecen intactos.
4. **Conservado el manifiesto previo** en `evidencias/manifest-pre-revision.json`; el nuevo manifiesto registra los cambios documentales y límites.

No se resolvieron por documentación las diferencias DRF/sobre, ni se recibió autorización docente. Las propuestas de AA3 ayudan a discutir responsabilidad de adaptación, pero no constituyen aprobación ni implementación en APE3.

## 6. Plan concreto para cerrar la práctica

| Prioridad | Acción | Evidencia necesaria para cerrarla |
|---|---|---|
| Alta | Resolver diferencia Spring/DRF | Decisión explícita del equipo/docente. Si se mantiene la guía, incremento DRF delimitado, ejecutable y probado; una excepción debe quedar registrada realmente. |
| Alta | Adoptar e implementar el contrato universal acordado | Backend/cliente compatibles, política de paginación, manejo de errores y 204, OpenAPI alineado y pruebas del sobre real. |
| Alta | Demostrar flujo con persistencia principal | Entorno PostgreSQL de prueba, migraciones pertinentes y solicitudes de éxito/rechazo con resultado guardado; sin datos institucionales no autorizados. |
| Alta | Versionar y atribuir aportes | Ramas/commits/revisión del flujo acordado, autoría/contribución real y enlaces por integrante. |
| Media | Verificar y visualizar DSL/UML | Resultado de parser/render y diagramas revisados, sin presentar Mermaid como captura de otra herramienta. |
| Media | Completar cobertura de AC del recorte | Pruebas de ámbitos, vigencia, modelo no publicado, auditoría y concurrencia cuando corresponda; actualizar matriz con resultados reales. |
| Media | Realizar cierre docente | Demostración del flujo, observaciones recibidas, cambios o justificaciones y responsables. |
| Presentación | Completar nombres/fecha y compilar informe | Datos reales y revisión de PDF si se entrega LaTeX; la guía no exige por sí sola PDF, el formato es apoyo institucional del paquete. |

La falta de un recorrido Angular es un límite adicional de las pruebas previas, **no una exigencia independiente de esta guía**. Si el incremento acordado incluye interfaz, comprobarla; si la demostración solicitada es de backend, el cliente HTTP puede ser su punto de entrada.
