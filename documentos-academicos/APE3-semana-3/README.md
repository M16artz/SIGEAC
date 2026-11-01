# APE3 — Arquitectura, OpenAPI y endpoints base

Desarrollo Basado en Plataformas · 5 A · Unidad 1 · Semana 3 · Práctica 003 · Docente Edison Leonardo Coronel Romero.

## Archivos que abrir

- [Revisión de cumplimiento de la guía](REVISION_CUMPLIMIENTO.md): dictamen por paso, resultados y acciones para cerrar los pendientes.
- [Reporte principal](APE3_SEMANA_3.md): guía desarrollada, procedimientos, resultados y siete preguntas de control.
- [Workspace C4](arquitectura/workspace.dsl): C1/C2/C3, responsabilidades y tecnologías.
- [UML de clases](arquitectura/catalogo-clases.puml) y [secuencia](arquitectura/alta-carrera-secuencia.puml).
- [ADR de tecnología](adr/APE3-001-alineacion-tecnologica.md) y [contrato](adr/APE3-002-contrato-universal.md): propuestas, no acuerdos aprobados.
- [OpenAPI observado](contratos/openapi-observado.yaml) y [objetivo](contratos/openapi-objetivo.yaml): 14 operaciones cada uno; el objetivo no está implementado.
- [Trazabilidad](TRAZABILIDAD.md), [verificación](VERIFICACION.md), [fuentes](FUENTES.md), [observaciones](OBSERVACIONES.md) y [nota individual](NOTA_INDIVIDUAL.md).
- [Página técnica Quarto](tecnico/APE3.qmd) y [HTML](tecnico/APE3.html).
- [Informe LaTeX](APE3_SEMANA_3.tex), logo institucional original y bibliografía, para Overleaf.
- [Manifiesto de integridad](evidencias/manifest.json) y resultados JSON bajo evidencias/.

## Resultado y diferencia pendiente con la guía

Se ejecutaron 23 tests existentes, 22 intercambios HTTP finales y validación de esas 22 respuestas contra OpenAPI; ambos contratos pasan Redocly. Se conservó el intento inicial que detectó el rechazo del DTO legado de un modelo configurado. No se implementaron endpoints nuevos ni se alteró la aplicación existente.

**La guía exige Django REST Framework y sobre universal; el sistema verificado usa Spring Boot y respuestas directas/Problem Details.** La elección de tecnología y migración del sobre requieren decisión explícita; no se presentan como cumplidas. Las pruebas utilizaron SQLite sintética auxiliar, sin aceptación PostgreSQL ni demostración Angular. El cierre académico no está completo: faltan validación docente, observaciones reales, participación y evidencia versionada individual.

## Uso en Overleaf y diagramas

Subir `APE3_SEMANA_3.tex`, `referencias.bib` y `reporte_tecnico_overleaf_logo.png` juntos; seleccionar el .tex como principal y pdfLaTeX. El formato proviene de `.agents/skills/informes-latex-unl/assets/FORMATOAPE.tex`; conserva article 11 pt/A4, tipografía, márgenes, cabecera/pie, tabla institucional y once secciones originales. Solo se añade soporte de tablas/rutas/bloques extensos. Completar nombres y fecha real.

No hay motor TeX local: **sin PDF compilado ni inspección visual de PDF**. Structurizr/PlantUML quedan como fuentes editables, sin parser/render oficial ejecutado; no se reemplazan por una supuesta captura. La página HTML incluye ayudas visuales Mermaid diferentes de los archivos fuente oficiales.

## Versionado

No se hicieron commits, ramas GitFlow, PR ni push. La rama observada es main; no se atribuye el commit de base a esta práctica. Preservadas las entregas anteriores y código de producto. Consultar NOTA_INDIVIDUAL.md para completar contribuciones reales y OBSERVACIONES.md para registrar la revisión del docente.
