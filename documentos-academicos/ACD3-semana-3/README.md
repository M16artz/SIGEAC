# ACD3 — Clínica de arquitectura y contrato de API

Semana 3 · Trabajo grupal con evidencia individual · SIGEAC / SEC-UNL.

## Entregables

- [Revisión de los diez puntos de la guía](REVISION_GUIA.md): localización de cada evidencia y precisiones posteriores a APE3.
- [Informe principal](ACD3_CLINICA_ARQUITECTURA_API.md): requisitos, dos procesos textuales, decisiones, trazabilidad y cumplimiento de la guía.
- [Arquitectura C4](ARQUITECTURA_C4.md): C1, C2 y C3 en Mermaid, tecnologías y responsabilidades.
- [Contrato inicial](CONTRATO_API_INICIAL.md): 18 operaciones observadas; entradas, validaciones, estados, seguridad y ejemplos; sobre académico propuesto separado del formato actual.
- [Observaciones y ajustes](REGISTRO_OBSERVACIONES.md): nueve hallazgos técnicos, preguntas y registro de retroalimentación pendiente.
- [Evidencia individual](EVIDENCIA_INDIVIDUAL.md): recorridos para revisar y campos de contribución/participación real.
- [Fuentes](FUENTES.md): rutas exactas y referencias técnicas primarias.
- [Informe institucional LaTeX](ACD3_CLINICA.tex) y [bibliografía](referencias.bib).
- [Manifiesto de integridad](evidencias/manifest.json): hashes y límites de la revisión.

## Diferencias que deben resolverse

La guía pide sobre JSON común; el producto devuelve recursos directos y Problem Details. Se documentan ambos sin migrar código ni fingir compatibilidad. También se registra Java 25 en investigación frente a Java 21 configurado, PATCH de catálogo con bloque editable completo y creación de decisión de estado sin Location explícita.

Los requisitos de APE2/AA2 se mantienen intactos. No se modifican código, OpenAPI de producto, ADR existentes ni migraciones. La preparación original ACD3 fue estática; esta revisión incorpora referencias a resultados posteriores de APE3, sin ejecutar solicitudes/pruebas nuevas ni atribuirlas a la clínica. No se registran aprobaciones o comentarios del docente que no se hayan recibido.

## Uso del LaTeX

Adaptación de `.agents/skills/informes-latex-unl/assets/FORMATO.tex`: portada, formato report 12 pt/A4, márgenes, interlineado, cabecera/pie, declaración IA, índice, desarrollo, bibliografía y anexos. Añade soporte para rutas/tablas/bloques extensos. Diagramas Mermaid disponibles visualmente en Markdown; su código fuente se conserva en el anexo LaTeX, sin afirmar renderizado.

Subir `ACD3_CLINICA.tex` y `referencias.bib` a Overleaf; seleccionar el primero como principal y pdfLaTeX. Incorporar `fondo.jpeg` y `logo_unl.jpeg` originales cuando estén disponibles; no están en los assets inspeccionados. Se muestran marcadores mediante guardas de existencia. Completar integrantes, asignatura, docente y fecha real; no se heredan datos de otra asignatura sin confirmación.

No se dispone de motor TeX local en PATH: no se compiló ni inspeccionó un PDF. Se comprobaron estructura, enlaces, ejemplos JSON, operaciones y hashes; revisar paginación/figuras al compilar antes de la entrega.

## Próximo paso académico

Presentar los flujos y decisiones; registrar retroalimentación real; resolver compatibilidad/alcance; refinar contrato para APE3. Los documentos quedan preparados para revisión y socialización, no como evidencia de una sesión ya celebrada. No se creó commit ni se publicó la actividad.
