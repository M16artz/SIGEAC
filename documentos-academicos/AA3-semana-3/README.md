# AA3 — ADR de alternativas arquitectónicas

Semana 3 · Actividad Autónoma · 4,5 horas previstas · Individual articulada con SIGEAC / SEC-UNL.

## Entregables

- [Informe de investigación y comparación](AA3_ALTERNATIVAS_ARQUITECTONICAS.md): contexto, tres alternativas, siete criterios, respuesta a requisitos, responsabilidades y cobertura de las nueve actividades.
- [ADR-AA3-001](ADR-AA3-001-adaptacion-respuestas-api.md): adaptación explícita en frontera API, **Propuesto**; selección C condicionada, consecuencias, siete riesgos y mitigaciones.
- [Matriz de trazabilidad](TRAZABILIDAD_ADR.md): requisito/criterio → ADR → C4 → código/contrato → evidencia previa o verificación futura.
- [Fuentes](FUENTES.md): P-01–09 y S-01–08, documentación oficial y localizadores concretos.
- [Nota individual](NOTA_INDIVIDUAL.md): cambios desde ACD3/APE3 y campos para atribución real.
- [Informe institucional LaTeX](AA3_ADR.tex) y [bibliografía](referencias.bib).
- [Manifiesto](evidencias/manifest.json): integridad y comprobaciones documentales.

## Decisión estudiada

Dónde adaptar las respuestas al sobre común sin introducir formato HTTP en negocio ni romper clientes. A: construcción por controlador; B: envoltura transversal; C: adaptadores explícitos y política de presentación compartida. Se selecciona C como propuesta individual, sin implementar ni aceptar el sobre por esa sola elección.

Se estudia Spring como framework existente y DRF como contraste ante la exigencia pendiente de APE3. La decisión Spring/DRF y el contrato final siguen abiertos; no se reemplazan ADR del producto ni se atribuye aprobación docente. Las métricas/test de APE3 son evidencia anterior, no experimentos nuevos de las alternativas AA3.

## Uso del LaTeX

Adaptación de `.agents/skills/informes-latex-unl/assets/FORMATO.tex`, preservando report 12 pt/A4, márgenes, interlineado, portada, cabecera/pie, declaración IA, índice, tres capítulos de desarrollo, bibliografía y anexos. Se agregan soportes para rutas, bloques y contenido extenso. Las tablas anchas de Markdown se presentan como fichas en LaTeX para conservar legibilidad.

Subir `AA3_ADR.tex` y `referencias.bib` juntos a Overleaf; seleccionar el .tex como principal. Incorporar `fondo.jpeg` y `logo_unl.jpeg` originales cuando estén disponibles: faltan en los assets inspeccionados, y se conservan guardas/marcadores de ausencia. Completar estudiante y fecha; asignatura/docente son referencias de APE3 pendientes de confirmar para AA3.

No hay motor TeX local: **no se compiló ni inspeccionó un PDF**. El Mermaid de responsabilidades es fuente editable en Markdown y se conserva como código en el desarrollo LaTeX; no se afirma render de ese diagrama. No se ejecutaron nuevas pruebas de negocio/HTTP ni se implementó código de aplicación, se creó commit o se publicó el paquete.

La documentación queda preparada para revisión personal y discusión del equipo; no acredita autoría individual o aceptación por sí misma.
