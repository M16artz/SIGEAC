# Inventario y revisión de distribución de formularios

## Inventario previo

| Módulo / perfiles que lo utilizan | Formularios y paneles | Decisión |
| --- | --- | --- |
| Acceso / administrador y coordinador | Usuarios, asignaciones, conflictos, filtros de auditoría | Conservar: ya tienen grids y acciones alineadas |
| Carreras / consulta general y gestión autorizada | Carrera, unidad académica, búsqueda e historial | Conservar: ya están distribuidos |
| Procesos / administrador, coordinador, autoridad de proceso | Crear proceso, autorización/transiciones, participantes, equipos, integrantes, responsabilidades y actividades | Reorganizar |
| Modelos / administrador y coordinador | Creación, borrador JSON, aplicabilidad, publicación, comparación y consultas | Reorganizar campos relacionados; JSON y textos largos a ancho completo |
| Evidencias / responsable | Metadatos, referencias, cobertura, datos estructurados y filtros | Reorganizar; conservar adjuntos y acciones existentes |
| Revisión / revisor | Resultados por cobertura, justificación y excepción de autorrevisión | Reorganizar sin cambiar los campos condicionales |
| Valoración / evaluador y comité académico | Juicio manual, variables, verificación, cálculo, discrepancias, procedimiento y resolución | Reorganizar; conservar sustentos y participantes |
| Seguimiento / coordinador | Proceso, cortes e historial, cierre documental | Agrupar consulta y acciones; conservar motivos largos |
| Informes / coordinador y autoridad | Hallazgos, consolidación, emisión, socialización, observaciones, respuestas y decisiones | Reorganizar |
| Mejora / coordinador, autoridad, responsable y revisor | Plan, acción, avances, decisiones y revisiones | Reorganizar todos los estados condicionales |
| Archivo / coordinador y autoridad | Cierre integral y política de retención | Reorganizar |
| Login | Inicio de sesión | Excluido |

Los perfiles se identificaron en rutas, navegación, plantillas y componentes,
no únicamente a partir del menú del administrador. Esto no constituye una
verificación de autorización contra el backend.

## Criterios

- Cambios exclusivamente de HTML/clases y SCSS, sin modificar código de lógica.
- Grid según el ancho del formulario, no solo el ancho de la ventana:
  una columna estrecha, dos cuando caben y tres solo para campos adecuados.
- Etiqueta y control siempre agrupados; textos largos ocupan todo el ancho.
- Separación de 16–20 px; acciones al final, buscador y botón juntos si caben.
- Layout principal, login, backend y componentes ya bien distribuidos intactos.

## Verificación

### Resultados por grupo

| Grupo | Resultado |
| --- | --- |
| Procesos | Compilación correcta; creación, planificación, autorización y participantes reorganizados |
| Modelos, evidencias y revisión | Compilación correcta; consultas, metadatos y campos condicionales conservados |
| Valoración, informes, mejora, archivo y seguimiento | Compilación correcta; formularios secundarios e historial incluidos |
| Regresión automatizada | 29 archivos y 107 pruebas existentes correctas |

En total se reorganizaron 40 formularios, además de paneles de consulta que no
utilizan una etiqueta `form`. Se conservaron los formularios ya bien distribuidos.

### Revisión visual y eventos

- Chrome: 29 escenarios a 1440, 900 y 390 px, 87 comprobaciones.
- Contextos sintéticos para administrador, coordinador, autoridad de proceso,
  responsable, revisor, evaluador y comité académico.
- Se utilizaron respuestas HTTP interceptadas y estados de componentes de una
  compilación de desarrollo; **no se iniciaron sesiones reales de esos perfiles**
  ni se comprobaron sus permisos contra el backend.
- Sin errores JavaScript, desbordamientos de grids ni campos fuera del ancho
  visible en los escenarios comprobados. En escritorio no se detectó contenido
  oculto detrás del sidebar.
- Se inspeccionaron capturas de creación y planificación de procesos, evidencias,
  revisión, informes, mejora y archivo.
- Crear proceso: buscador, botón de creación y payload HTTP comprobados con
  respuestas sintéticas, sin escribir datos reales.
- Campos condicionales comprobados: asignación persona/equipo, excepción de
  autorrevisión y tipos numérico/booleano/texto de datos estructurados.
- Crear proceso: dos columnas a 1440 y 900 px; una a 390 px. El plan de mejora
  utiliza tres, dos y una columna respectivamente según el espacio disponible.
- Comparación con la instantánea inicial: backend, layout protegido, login,
  lógica TypeScript, bindings y atributos de campos/validaciones intactos.
- Evidencias temporales: `/tmp/opencode/sigeac-forms-review/results.json` y
  capturas en ese mismo directorio.

### Límites y pendientes

El sidebar aprobado presenta un problema previo en tablet/móvil: permanece
visible aun estando cerrado y tapa el contenido. Se mantuvieron intactos sus
cuatro archivos protegidos. Para revisar los formularios a 900 y 390 px se ocultó
el sidebar **solo dentro del navegador de pruebas**, sin cambiar el proyecto.
Esas capturas no certifican el funcionamiento de la navegación móvil.

No se verificaron sesiones reales de los perfiles, todas las combinaciones de
estados ni recorridos completos contra una API real. Archivo contiene algunos
textos con problemas previos de codificación; no se cambiaron durante esta tarea.

## Archivos modificados

Único parcial SCSS modificado:
- `src/app/shared/styles/_internal-pages.scss`.

Plantillas dentro de `src/app/features/` (solo HTML/clases, sin cambios de lógica):
- `processes/pages/`: `process-create.page.ts`, `process-planning.page.ts`.
- `processes/ui/`: `process-state-controls.component.ts`,
  `process-assignment-form.component.ts`.
- `models/pages/`: `model-editor.page.ts`, `model-history.page.ts`,
  `model-compare.page.ts`, `model-audit.page.ts`.
- `models/ui/publication-panel.component.ts`.
- `evidence/pages/`: `evidence-editor.page.ts`, `evidence-audit.page.ts`,
  `evidence-list.page.ts`, `evidence-version.page.ts`.
- `evidence/ui/`: `evidence-metadata-form.component.ts`,
  `evidence-sources.component.ts`, `evidence-source-coverage.component.ts`,
  `structured-evidence.component.ts`.
- `reviews/pages/document-review-inbox.page.ts`.
- `evaluations/pages/`: `indicator-evaluation.page.ts`,
  `quantitative-evaluation.page.ts`, `academic-disagreement.page.ts`.
- `reports/pages/report-workflow.page.ts`.
- `improvement/pages/improvement-plan.page.ts`.
- `archive/pages/process-archive.page.ts`.
- `reporting/pages/`: `documentary-dashboard.page.ts`,
  `documentary-history.page.ts`, `documentary-closure.page.ts`.

Inventario y resultados: `frontend/FORM_LAYOUT_REVIEW.md` (este archivo).
