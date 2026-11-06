# Unificación visual de vistas internas

## Alcance

Referencia: «Usuarios y asignaciones» del propio proyecto. No se proporcionaron
las dos capturas mencionadas. Impeccable no está disponible entre las skills
registradas ni en los archivos del proyecto.

Se conservaron los cambios previos del usuario. Los cuatro archivos protegidos
del layout son idénticos a la instantánea tomada al iniciar este trabajo.
No se editaron backend, autenticación, servicios, modelos, rutas ni validadores.
Los cambios en TypeScript se limitan a sus plantillas HTML.

## Páginas revisadas

| Módulo | Vistas | Resultado |
| --- | --- | --- |
| Acceso | Acceso y ámbitos autorizados | Encabezado, asignaciones y accesos rápidos dentro de tarjetas blancas; métricas homogéneas |
| Acceso | Usuarios y asignaciones; Conflictos y excepciones; Auditoría de acceso | Ya cumplen la referencia; sin cambios |
| Carreras | Catálogo de carreras y unidades académicas | Se conservan las tarjetas; se completan estilos tipográficos y de acciones que faltaban |
| Modelos | Editor de modelo; Historial de versiones; Comparar versiones; Auditoría de modelos | Encabezado y contenido en tarjetas |
| Procesos | Procesos de autoevaluación; Detalle; Autorización y planificación; Actividades | Tarjetas, secciones, formularios y tablas adaptables |
| Evidencias | Listado; Nueva evidencia; Editar borrador; Cobertura y versión; Historial; Auditoría | Tarjetas y scroll de tablas, incluidas cobertura y datos estructurados |
| Revisión | Revisión documental | Tarjetas para bandeja, decisión e historial; scroll de tablas |
| Valoración | Valoración manual; Variables y cálculo cuantitativo; Discrepancias y conciliación | Tarjetas para encabezados, formularios e historial |
| Seguimiento | Seguimiento documental; Cortes e historial; Cierre de fase documental | Tarjetas y tablas con scroll |
| Informes | Hallazgos, consolidación y ciclo del informe | Tarjetas y jerarquía de secciones |
| Mejora | Planes de mejora y seguimiento | Tarjetas y jerarquía de secciones |
| Archivo | Archivo y cierre integral | Tarjetas y jerarquía de secciones |
| Sesión | Login | Revisado, sin cambios |

## Archivos modificados

Estilos compartidos:
- `src/styles.scss`: únicamente se añade la importación del parcial opt-in.
- `src/app/shared/styles/_internal-pages.scss`: nuevo patrón acotado a
  `#main-content .sigeac-page`; no alcanza sidebar, login ni vistas ya terminadas.

Estilos de páginas:
- `src/app/features/access/pages/access-home.page.scss`
- `src/app/features/careers/pages/career-catalog.page.scss`

Plantillas dentro de `src/app/features/` (archivos `.ts`, sin cambios de lógica):
- `models/pages/`: `model-editor.page.ts`, `model-history.page.ts`,
  `model-compare.page.ts`, `model-audit.page.ts`.
- `processes/pages/`: `process-create.page.ts`, `process-detail.page.ts`,
  `process-planning.page.ts`, `process-activities.page.ts`.
- `evidence/pages/`: `evidence-list.page.ts`, `evidence-editor.page.ts`,
  `evidence-version.page.ts`, `evidence-history.page.ts`, `evidence-audit.page.ts`.
- `evidence/ui/`: `evidence-source-coverage.component.ts`,
  `structured-evidence.component.ts`.
- `reviews/pages/document-review-inbox.page.ts`.
- `evaluations/pages/`: `indicator-evaluation.page.ts`,
  `quantitative-evaluation.page.ts`, `academic-disagreement.page.ts`.
- `reporting/pages/`: `documentary-dashboard.page.ts`,
  `documentary-history.page.ts`, `documentary-closure.page.ts`.
- `reports/pages/report-workflow.page.ts`.
- `improvement/pages/improvement-plan.page.ts`.
- `archive/pages/process-archive.page.ts`.

Este documento: `frontend/VISUAL_REVIEW.md`.

## Cambios visuales

- Fondo lavanda y tipografías existentes conservados.
- Tarjetas blancas con radio de 24 px, bordes discretos y sombras suaves.
- Etiquetas de módulo, títulos azules grandes y descripciones en el encabezado.
- Secciones con espacios consistentes e iconos decorativos sobre azul claro.
- Tablas semánticas conservadas dentro de contenedores de scroll horizontal
  enfocables por teclado, con encabezados azul claro y filas más cómodas.
- Controles y botones redondeados, foco azul y acciones de quitar en rojo suave.

## Verificación

- Cuatro compilaciones de producción correctas durante los grupos de cambios;
  sin errores de plantilla ni advertencias de presupuesto.
- Última compilación: `CI=true npm run build -- --output-path=/tmp/opencode/sigeac-build-final`.
- Pruebas existentes: `CI=true npm test`: **29 archivos, 107 pruebas correctas**.
- Comparación con la instantánea inicial: layout protegido intacto y código TS
  fuera de las plantillas intacto (ignorando espacios finales).
- Chrome headless instalado en el equipo: 30 rutas a 1440 y 390 px, incluidas
  las seis rutas de evidencias y el login; 60 capturas/comprobaciones.
- Respuestas HTTP sintéticas interceptadas exclusivamente en el navegador:
  sin llamadas al backend ni escrituras reales.
- Sin errores JavaScript en esos escenarios; encabezados blancos, tablas
  dentro de contenedores desplazables y sin desbordamiento horizontal de página.
- Filtro de auditoría comprobado con `objectType=USER`; enlace de acceso rápido
  a Usuarios y asignaciones comprobado.
- Se inspeccionaron capturas representativas comparándolas con Usuarios.
- Evidencias temporales: `/tmp/opencode/sigeac-visual/`, con `results.json`.
  Las capturas muestran el área visible del contenedor de scroll del layout,
  no necesariamente todo el contenido situado debajo del primer viewport.

## Pendientes y límites

**Bloqueo previo del layout en móvil:** `.app-sidebar` permanece visible y cubre
el contenido aunque `mobileOpen` sea falso. El SCSS actual no define su
ocultación móvil. No se corrigió porque el layout está expresamente protegido.
Para comprobar aisladamente las tarjetas a 390 px, se ocultó el sidebar solo en
el navegador de pruebas, sin modificar archivos. Las capturas móviles del informe
son diagnósticas: no certifican que la navegación móvil funcione correctamente.

No se verificaron todos los estados condicionales, roles, operaciones de escritura,
descargas ni recorridos completos contra una API real. Las pruebas unitarias
existentes pasan, pero no sustituyen esa aceptación funcional y visual.
No se instalaron dependencias ni se realizó commit o push.
