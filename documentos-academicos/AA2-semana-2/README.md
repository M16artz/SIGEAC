# AA2 — Dossier de requisitos y matriz de trazabilidad

Semana 2, Actividad Autónoma individual articulada con el proyecto grupal. Duración prevista: **4,5 horas**, no cronometraje ejecutado.

## Entregables

- [Dossier depurado](DOSSIER_REQUISITOS_AA2.md): identificación, problema, actores, alcance, seis historias, catorce RF, diez RNF, 27 criterios funcionales, diez verificaciones de calidad y decisiones pendientes.
- [Matriz inicial](MATRIZ_TRAZABILIDAD.md): 24 filas con actor → necesidad → historia/requisito → aceptación/verificación → fuente → estado.
- [Fuentes y referencias](FUENTES_REFERENCIAS.md): registros F-01–14 y código C-01–10; RFC, documentos primarios del dominio y límites de aplicación.
- [Nota individual de cambios](NOTA_CAMBIOS_INDIVIDUAL.md): equivalencia con todos los FR/NFR locales de APE2 y campos para identificar revisión personal y aporte real.
- [LaTeX del dossier](AA2_DOSSIER.tex) y [bibliografía](referencias.bib): adaptación de FORMATO.tex con anexos legibles.
- [Manifiesto](evidencias/manifest.json): integridad del paquete y hashes de la línea base APE2; no acredita autoría o validación.

La matriz es parte del dossier por referencia en Markdown y está incorporada como anexo en LaTeX. En LaTeX, cada fila se presenta como ficha de dos columnas para conservar todos los campos sin reducir la letra ni desbordar los márgenes. También se incorporan el registro de fuentes y la nota de cambios.

## Fuentes y resultados que sí se obtuvieron

Se consultaron fuentes primarias publicadas por CACES y UNL, además de RFC 9110, RFC 5789 y WCAG 2.2. Se revisó su relación con el incremento y se mantuvieron explícitos los límites de vigencia, competencias y uso de fixtures sintéticos. No se modificaron los manifiestos normativos del proyecto para presentarlos como corroborados.

Se revisaron estructura, enlaces locales, IDs, correspondencia de matriz, cobertura de la guía, entornos LaTeX y hashes. No se ejecutaron pruebas de negocio ni solicitudes al backend; no se entrevistaron actores ni se validó el dossier con una autoridad institucional.

## Uso en Overleaf y preservación del formato

La plantilla original es `.agents/skills/informes-latex-unl/assets/FORMATO.tex`. Se conservan `report`, 12 pt, A4, márgenes, interlineado, portada, cabecera/pie, declaración de IA, índice, capítulos, bibliografía y anexos. Se reemplazan datos y contenido del informe previo de la plantilla por los del AA2. Las tres partes de desarrollo se adaptan temáticamente a revisión de fuentes, dossier y trazabilidad.

Se añaden `calc`, `xurl` y `fvextra` para tablas, rutas largas y bloques técnicos. No se modifica la plantilla original.

1. Subir `AA2_DOSSIER.tex` y `referencias.bib` al mismo proyecto.
2. Seleccionar `AA2_DOSSIER.tex` como documento principal y pdfLaTeX como compilador.
3. Incorporar `fondo.jpeg` y `logo_unl.jpeg`, con sus nombres originales, si se dispone de ellos. No están disponibles en los assets inspeccionados; las comprobaciones de existencia permiten continuar y muestran marcadores de ausencia. No se fabrican ni se sustituyen por imágenes de otro diseño.
4. Completar estudiante, fecha real y revisión personal; confirmar asignatura/docente por continuidad con APE2.
5. Compilar y revisar tablas, referencias y distribución de páginas antes de entregar.

No hay motor TeX local disponible en PATH. **No se compiló ni se inspeccionó un PDF.** La conversión utiliza Pandoc incluido en Quarto y comprobaciones estáticas.

## Enlace y evidencia individual

Ubicación local del dossier: `documentos-academicos/AA2-semana-2/DOSSIER_REQUISITOS_AA2.md`. El remoto observado corresponde a `https://github.com/M16artz/SIGEAC`; no se verificó acceso externo ni se publicó la actividad.

El estudiante debe identificar qué elementos revisó personalmente y aportar un enlace real cuando los versione/publique. El commit base inspeccionado no es un commit del AA2. Se conserva APE2 sin cambios y no se presenta asistencia de IA como participación personal demostrada.

