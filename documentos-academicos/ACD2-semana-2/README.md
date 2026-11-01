# ACD2 — Semana 2

## Archivos de la entrega

- [ACD2_SEMANA_2.md](ACD2_SEMANA_2.md): informe grupal y actualización académica complementaria de la especificación inicial. Incluye requisitos, RNF de las siete categorías, calidad, escenarios, historias, aceptación, HTTP/JSON, CORS y registro de retroalimentación.
- [EVIDENCIA_INDIVIDUAL.md](EVIDENCIA_INDIVIDUAL.md): dos requisitos corregidos con justificación y ejercicio HTTP resuelto. Cada estudiante debe personalizar su contribución y registrar una observación real.
- [ACD2_SEMANA_2.tex](ACD2_SEMANA_2.tex): adaptación de la plantilla institucional; integra contenido grupal y evidencia individual como anexo.
- [referencias.bib](referencias.bib): fuentes reales del repositorio, RFC, WHATWG y MDN.

Las rutas técnicas escritas en las tablas se interpretan desde la raíz del repositorio, como indica el informe. Los enlaces Markdown entre documentos son relativos.

## Uso en Overleaf

1. Crear un proyecto y subir `ACD2_SEMANA_2.tex` y `referencias.bib`.
2. Seleccionar `ACD2_SEMANA_2.tex` como documento principal y pdfLaTeX como compilador.
3. Completar integrantes, docente, fecha de la sesión y ciclo/paralelo. Los valores del informe anterior incluido en la plantilla no se trasladaron como datos de esta actividad.
4. Incorporar los archivos originales `fondo.jpeg` y `logo_unl.jpeg` si se dispone de ellos, con esos nombres y junto al `.tex`. No están disponibles en los assets inspeccionados. El LaTeX usa comprobaciones de existencia y muestra marcadores si faltan; no inventa logos ni impide continuar por una imagen ausente.
5. Revisar la compilación, bibliografía y distribución final de tablas después de completar los datos.

No hay compilador TeX local disponible en PATH; **no se ha compilado ni inspeccionado un PDF**. Se realizó conversión con Pandoc incluido en Quarto y revisión estática de estructura, referencias y ejemplos. Los bloques HTTP admiten saltos visuales de línea para caber en los márgenes; esos saltos no cambian la solicitud conceptual.

## Procedencia y adaptación del formato

Se utilizó `.agents/skills/informes-latex-unl/assets/FORMATO.tex`, leído e inspeccionado. SHA-256 de la plantilla: `fc8b87a75830dfb4e686b77ef5297b181be5ee652607e8c864d9822a226fbece`.

Se conservaron clase `report`, 12 pt, A4, márgenes, interlineado, estructura de portada, encabezado/pie, declaración de IA, índice, capítulos, bibliografía y anexos. La plantilla contiene un informe previo sobre evaluación de modelos de IA; se sustituyeron sus datos, contenido y títulos temáticos por los de ACD2. No se conservan afirmaciones ni autores ajenos a esta entrega. Los títulos de desarrollo se adaptaron a la guía, manteniendo tres capítulos y anexo.

Se añadieron únicamente soportes necesarios para el contenido: `calc` para tablas generadas, `xurl` para partir rutas largas y `fvextra` para ajustar ejemplos HTTP. Las tablas usan `longtable`, ya presente en la plantilla. No se alteró la plantilla original.

## Pendientes que el texto no puede acreditar por el equipo

- Nombres, docente, ciclo y fecha real.
- Respuestas verificadas de clarificación y decisiones de política pendientes.
- Revisión personal y atribución de contribuciones individuales.
- Observación recibida de una persona durante la sesión y modificación acordada a partir de ella.
- Revisión humana del documento y aceptación del equipo.
- Imágenes institucionales originales y compilación/inspección final del PDF.

No se ejecutaron solicitudes HTTP ni suites para elaborar esta entrega. Los ejemplos están identificados como reconstrucciones sintéticas de rutas y DTO implementados. No se modificaron código de negocio, requisitos canónicos, configuración ni la auditoría anterior.

