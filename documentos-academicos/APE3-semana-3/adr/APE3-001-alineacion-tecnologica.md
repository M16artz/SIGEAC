# ADR académico APE3-001 — Alinear la guía con la base existente

**Estado: propuesta pendiente de decisión del equipo/docente.** No sustituye ni cambia ADR-0001/0004/0018 del producto.

## Contexto

La guía requiere Python, Django y Django REST Framework. `backend/pom.xml` configura Java 21/Spring Boot 4.1.1, y `.specify/memory/constitution.md` aprueba Angular/Java/Spring/PostgreSQL como base del producto. `docs/architecture/research.md` menciona Java 25, mientras la configuración de compilación es 21; el Java instalado observado es 25. Son tres datos distintos: planificación, nivel del compilador e instalación local.

La conversación solicita continuar las entregas académicas del proyecto. No se ha aportado una aceptación docente para cambiar tecnología ni para exceptuar el paso Django de APE3. Se consultó al usuario sobre mantener Spring o añadir un prototipo aislado; no se atribuye una respuesta inexistente.

## Alternativas

| Alternativa | Ventaja | Coste / consecuencia |
|---|---|---|
| Documentar y demostrar el incremento Spring existente | Conserva reglas, seguridad, contratos y cliente ya presentes | Requiere autorización docente para acreditar el paso de Django; el sobre universal tampoco está implementado. |
| Prototipo DRF aislado para el mismo flujo | Permite ejercitar el framework de la guía sin reemplazar la aplicación | Hay que delimitar sus requisitos, autenticación, almacenamiento, tests y relación con el producto; no atribuirle reglas todavía no portadas. |
| Migrar el producto completo a DRF | Unifica tecnología con guía | Amplía enormemente el alcance, afecta cliente, datos, seguridad y constitución; no se justifica por una práctica de tres horas. |

## Tratamiento en este paquete

Preparar artefactos verificables del sistema existente y señalar el cumplimiento Django como **pendiente**, mientras no exista elección/validación real. No presentar Java como si fuese DRF ni crear un backend alternativo sin selección expresa. El contrato actual conserva DTO directos y Problem Details; el sobre solicitado se define por separado como objetivo no desplegado.

## Confirmación necesaria

Registrar alternativa elegida, quién la confirma, fecha, alcance, consecuencias, evidencia y revisión de requisitos/contrato/pruebas. Si se elige DRF, deberá implementarse y ejecutarse el incremento acordado; esta propuesta documental no lo da por completado.
