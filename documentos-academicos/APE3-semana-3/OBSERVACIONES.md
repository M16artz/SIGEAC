# APE3 — Observaciones, decisiones pendientes y ajustes

## Hallazgos técnicos de esta preparación

| ID | Hallazgo / fuente | Tratamiento ya realizado | Pendiente |
|---|---|---|---|
| APE3-H01 | Guía DRF frente a Spring/constitución existentes | APE3-001 propuesto; consulta al usuario; no migrar aplicación | Elección y acuerdo docente/equipo. |
| APE3-H02 | Sobre universal no implementado | OpenAPI observado y objetivo diferenciados; APE3-002 | Implementación compatible o excepción autorizada. |
| APE3-H03 | Modelo configurado rechaza DTO legado con 409 | Conservar intento inicial; verificar /configured; añadir ruta real al contrato académico | Revisar inventario común de rutas y experiencia cliente con el equipo. |
| APE3-H04 | Contrato compartido de catálogo no detallaba 400 DTO | Añadir respuesta real al subconjunto y validar muestras | Decidir actualización del contrato fuente, fuera de esta entrega documental. |
| APE3-H05 | Referencia CSRF duplicada al generar contrato | Corregir generador y repetir lint sin excepciones | Ninguno respecto de esa duplicación; futuras regeneraciones requieren lint. |
| APE3-H06 | 401 local sin WWW-Authenticate | Relacionar headers capturados con HTTP-AA-01 | Resolver desafío/protocolo compatible con sesión actual; no inventar JWT. |
| APE3-H07 | Prueba disponible sin DB PostgreSQL activa | Usar SQLite sintética aislada y registrar límites | Validar PostgreSQL, migraciones y concurrencia. |
| APE3-H08 | ADR APE3-002 describía paginación en meta; OpenAPI objetivo/generador la conservan dentro de data | Corregir ADR y anexo LaTeX para reflejar la propuesta generada | La política final sigue pendiente de aceptación; no cambia el backend ni los schemas. |

Estas observaciones proceden de inspección/ejecución asistidas; **no son retroalimentación docente ni de otros equipos**. Los H de ACD3 conservan su estado previo; no se reescribe una aceptación inexistente.

## Registro para la sesión real

| ID | Fecha / observador real | Observación recibida | Decisión y motivo | Ajuste o justificación para mantener | Archivo / commit / responsable |
|---|---|---|---|---|---|
| APE3-OBS-01 | Pendiente | Pendiente de sesión | Pendiente | Pendiente | Pendiente de evidencia real |

## Plan para cerrar pendientes

1. Presentar alcance y diferencias tecnológicas/contractuales antes de afirmar cumplimiento.
2. Registrar la alternativa aceptada y actualizar estado de ADR con evidencia.
3. Si se exige DRF, desarrollar el incremento delimitado y verificarlo; no etiquetar la campaña Spring como su sustituto automático.
4. Ejecutar el flujo aceptado con base principal y cliente correspondiente, registrar errores y pruebas de reglas.
5. Renderizar diagramas e informe, incorporar correcciones reales y versionar aportes identificables.
