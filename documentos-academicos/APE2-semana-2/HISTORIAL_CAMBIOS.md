# APE2 — Historial local de preparación y refinamiento

## 1. Alcance de la evidencia

Este registro describe archivos efectivamente elaborados durante esta preparación asistida. No reconstruye una sesión anterior ni certifica quién participó en clase. No se crearon commits ni Pull Requests y no se publicaron archivos en GitHub.

- Rama observada: `main`.
- Commit base inspeccionado: `faf868ea5a8a85285290c06d3baf8fa49889edac`.
- Origin observado: `git@github.com:M16artz/SIGEAC.git`.
- El commit base y sus mensajes históricos no se presentan como evidencia de autoría del APE2.

## 2. Versiones y cambios conservados

| Versión local | Archivo | Contenido real |
|---|---|---|
| v01 — borrador de esta preparación | [specification-v01.md](evidencias/specification-v01.md) | Selección inicial de acceso, catálogo y preparación; historias iniciales y requisitos resumidos, sin aceptación detallada. |
| v02 — especificación refinada | [specification.md](specification.md) | Contexto, actores, recorte, restricciones, historias con prueba independiente, 18 AC, diez FR y diez NFR locales, entidades, éxito, supuestos, CL y contratos por referencia. |
| Diferencia calculada | [refinamiento-v01-v02.diff](evidencias/refinamiento-v01-v02.diff) | Diff unificado calculado a partir de ambos archivos, no una captura o aprobación ficticia. |
| Integridad del paquete | [manifest.json](evidencias/manifest.json) | Hashes SHA-256 de los archivos preparados y referencia a la plantilla utilizada. No acredita identidad humana ni aprobación. |

## 3. Justificación de los principales refinamientos

| Cambio | Necesidad | Resultado observable |
|---|---|---|
| Separar alta/consulta/modificación de carrera. | Verificar capacidades distintas sin un requisito “gestionar” ambiguo. | APE-FR-03–05 y AC-U2-01–08. |
| Explicitar PREPARATION y excluir activación. | Evitar atribuir autorización institucional al alta técnica. | APE-FR-07, AC-U3-01 y criterio de cero activaciones implícitas. |
| Añadir criterios negativos y concurrencia. | Detectar errores y pérdida de cambios. | AC-U2-02/03/05/07/08 y AC-U3-02–04/06. |
| Diferenciar requisitos y decisiones de implementación. | Mantener la especificación revisable sin imponer clases/frameworks al negocio. | Requisitos funcionales observables y contratos técnicos aparte. |
| Mantener seis CL pendientes. | No inventar respuestas del docente ni políticas. | Registro de responsables y efectos en specification.md. |
| Relacionar contratos con código. | Evitar rutas y DTO inexistentes. | CP-01–09, referencias de controlador/servicio y límites por operación. |

## 4. Versionado y publicación que debe completar el equipo

Antes de una entrega remota, revisar los archivos y seleccionar únicamente los de esta práctica. No incorporar indiscriminadamente `.env`, archivos de otras actividades o cambios ajenos. No se ejecutaron los comandos siguientes:

```powershell
git status --short
git add -- documentos-academicos/APE2-semana-2 documentos-academicos/README.md
git diff --cached --stat
git diff --cached -- documentos-academicos/APE2-semana-2
git commit -m "docs: preparar especificacion y reporte APE2"
git log -1 --format="%H %s"
```

El commit debe realizarse con identidad real del autor/revisor correspondiente, después de revisar su contenido. La publicación al remoto y los enlaces de contribuciones se registrarán cuando ocurran. Los comandos son orientación para versionar, no un resultado de ejecución.

| Evidencia remota | Estado |
|---|---|
| URL del specification.md publicado | [PENDIENTE: completar después de publicar la revisión real] |
| Commit/PR de APE2 | [PENDIENTE] |
| Aporte de cada estudiante | [PENDIENTE: enlazar revisión/cambio personal verificable] |
| Validación docente | [PENDIENTE: fecha, observación, IDs y modificación acordada] |

## 5. Comprobaciones de esta preparación

Se comprueban la estructura documental, enlaces locales, sintaxis de ejemplos JSON, correspondencia de IDs, diff reproducible y entornos LaTeX. No se ensaya la aplicación ni se valida el contenido por una autoridad académica. No hay motor TeX local en PATH; no se afirma compilación ni inspección visual de PDF.

