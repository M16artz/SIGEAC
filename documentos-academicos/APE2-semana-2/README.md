# APE2 — Guía Práctico-Experimental 002

Asignatura: **Desarrollo Basado en Plataformas**, ciclo **5 A**, Unidad **1**. Docente: **Edison Leonardo Coronel Romero**. Se conservan estos datos de la guía actual.

## Entregables

- [Reporte completo](APE2_SEMANA_2.md): once secciones de la plantilla APE, metodología, resultados y siete preguntas de control resueltas.
- [specification.md refinado](specification.md): producto principal en estructura de Spec Kit, con alcance propuesto U1, actores, diez FR, diez NFR, tres historias, 18 AC, criterios de éxito y clarificaciones.
- [Contratos preliminares](CONTRATOS_PRELIMINARES.md): nueve fichas vinculadas a requisitos y código existente.
- [Historial de cambios](HISTORIAL_CAMBIOS.md): borrador v01, versión refinada, diferencia real entre archivos y límites de evidencia.
- [Nota individual](NOTA_INDIVIDUAL.md): contenido orientativo y campos de contribución personal, enlace verificable y retroalimentación real.
- [LaTeX para Overleaf](APE2_SEMANA_2.tex): reporte con especificación, contratos y nota individual incorporados como anexos.
- [Manifiesto de integridad](evidencias/manifest.json): hashes del paquete y plantilla original.

Los enlaces de los documentos son relativos. Las rutas de código escritas en tablas se interpretan desde la raíz del repositorio.

## Formato institucional y Overleaf

Se utiliza `.agents/skills/informes-latex-unl/assets/FORMATOAPE.tex`, preservando `article`, 11 pt, A4, Helvetica, márgenes, cabecera/pie, tabla de identificación, once secciones y su orden. La plantilla original contiene datos de otra práctica de Simulación; se reemplazan por los de APE2, sin atribuir nombres de su informe anterior a esta entrega.

SHA-256 de la plantilla original: `8e5f8f4da3167de3d6e81a5de9b8ccd76c9f4e6eec4b8fce5ab44cff5a0b966d`.

Se copia sin editar `reporte_tecnico_overleaf_logo.png`, recurso de la plantilla disponible. Se añaden únicamente `longtable`/`calc` para tablas extensas y `fvextra` para bloques técnicos; se conservan los paquetes originales. Los nombres de archivos se mantienen.

Para usarlo:

1. Subir `APE2_SEMANA_2.tex` y `reporte_tecnico_overleaf_logo.png` al mismo proyecto Overleaf.
2. Seleccionar `APE2_SEMANA_2.tex` como principal y pdfLaTeX como compilador. No requiere `.bib` externo: referencias están en su sección institucional.
3. Completar integrantes y fecha real. Revisar y personalizar la nota individual.
4. Registrar la validación docente y la contribución con su evidencia real.
5. Compilar y comprobar distribución de tablas, anexos y enlaces antes de entregar.

**No se ha compilado un PDF:** no hay motor TeX local disponible en PATH. Se revisa estructura y sintaxis documental, sin afirmar verificación visual.

## Límites de la entrega

No se ha contactado al docente, ejecutado Spec Kit CLI, realizado una reunión de clarificación, corrido pruebas del sistema, creado commit/PR ni publicado en GitHub. El repositorio y su commit base se identifican mediante Git local; no acreditan que el nuevo APE2 ya esté publicado.

Pendientes reales: integrantes, fecha de sesión, confirmación del recorte U1, respuestas CL-APE, retroalimentación docente, revisión personal, commit/enlace remoto y compilación visual. Los pendientes no se reemplazan por firmas, aprobaciones ni aportes inventados.

