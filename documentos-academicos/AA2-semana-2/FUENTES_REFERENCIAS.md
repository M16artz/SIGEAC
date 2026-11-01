# AA2 — Registro de fuentes y referencias

**Fecha de consulta documental:** 2026-10-31. Las rutas locales son relativas a la raíz del repositorio. El texto identifica qué se consultó y qué permite justificar; no equipara consulta con vigencia institucional ni con ejecución de pruebas.

## 1. Fuentes del proyecto y de la actividad

| ID | Fuente identificable | Localizador consultado | Relación y límite |
|---|---|---|---|
| F-01 | Guía AA2 proporcionada por el usuario | Actividades 1–7 y producto esperado | Define el dossier individual, revisión de fuentes y cadena actor–necesidad–requisito–aceptación–fuente. No es aprobación del producto. |
| F-02 | Especificación APE2 | `documentos-academicos/APE2-semana-2/specification.md`: US1–3, APE-FR-01–10, APE-NFR-01–10 y CL-APE | Línea base académica para depuración; sigue pendiente de validación docente. |
| F-03 | Especificación general SEC-UNL | `docs/requirements/spec.md`: Contexto, Actores, FR-001–040, RNF-001–019, SC y CL | Fuente de requisitos del equipo. Los 30 minutos y p95 de 2 s provienen de este documento, no de RFC 9110. |
| F-04 | Reglas del dominio | `docs/requirements/domain-rules.md`: responsabilidades, estados, decisiones y procedencia | Fuente de política del software y límites institucionales; no constituye normativa oficial por sí sola. |
| F-05 | Procesos del proyecto | `docs/requirements/process-flows.md`: P01, P02 y P05 | Escenarios y actores del incremento. |
| F-06 | Especificación reconciliada | `specs/005-complete-evaluation-workflows/spec.md`: FR-043, FR-044, FR-048, FR-067–070 | Catálogo, procedencia y separación entre publicación, autorización y alcance. |
| F-10 | Manifiestos normativos sintéticos | `tests/fixtures/models/normative-verification.json` y `tests/fixtures/models/normative-manifest.json` | Declaran `synthetic: true`, `institutionalValidity: false` y verificaciones pendientes. No justifican vigencia ni equivalencia de los cinco modelos. |
| F-11 | Fuentes ejecutables | Archivos C-01–C-10 de la sección 4 | Permiten comprobar DTO y controles observados; el código no sustituye la necesidad del actor ni prueba corrección normativa. |
| F-12 | Pruebas y resultados históricos | `docs/testing/traceability.md`; `tests/acceptance/results/performance-p01-p16.md`; pruebas indicadas en el dossier | Orientan verificaciones posteriores. No se reejecutaron y su cobertura no se extiende automáticamente al recorte AA2. |

## 2. Fuentes primarias del dominio consultadas

### F-07 — Modelo Genérico CACES, marzo de 2024

- Autor institucional: Consejo de Aseguramiento de la Calidad de la Educación Superior (CACES).
- Identificación: Modelo genérico para la evaluación del entorno de aprendizaje de carreras de grado, marzo de 2024.
- [PDF publicado por CACES](https://www.caces.gob.ec/wp-content/uploads/2024/04/Modelo-generico-para-la-evaluacio%CC%81n-del-entorno-de-aprendizaje-de-carreras-de-grado.pdf).
- Localizadores: sección II, Introducción; sección V, Estructura del modelo y definiciones. Lectura acotada, no transcripción exhaustiva de indicadores.
- Hallazgo pertinente: relaciona la evaluación con estándares, fuentes y períodos. Se utiliza como respaldo contextual de N-AA-03: identificar el modelo y período elegidos para un proceso.
- Límite: la consulta del PDF oficial no corrobora los fixtures ni establece qué versión debe usarse para cada carrera hoy. No impone por sí sola los campos técnicos del backend ni exige un algoritmo de autenticación.
- Estado: **documento primario localizado y consultado; aplicabilidad al piloto por confirmar**.

### F-08 — Instructivo UNL, Resolución 037-R-UNL-2022

- Autor institucional: Universidad Nacional de Loja.
- Identificación: resolución 037-R-UNL-2022, instructivo de aseguramiento de la calidad institucional, carreras y posgrados.
- [PDF publicado en la gaceta de la UNL](https://www.unl.edu.ec/sites/default/files/gaceta/resolucion/2022-06/Resoluci%C3%B3n%20Nro.%20037-R-UNL-2022-%20Instructivo%20para%20el%20Aseguramiento%20de%20la%20Calidad%20de%20la%20UNL-signed.pdf).
- Localizadores: artículos 1–2 (objeto/ámbito), 9–10 (comisión de carrera/programa) y 12 (responsabilidades de comités). La revisión no abarca todas sus disposiciones.
- Hallazgo pertinente: identifica comisiones y comités con responsabilidades en autoevaluación. Respalda el contexto institucional de N-AA-03 y la necesidad de explicitar quién interviene.
- Límite: no se convierte automáticamente a una comisión en el rol `coordinador`, ni a cada miembro en administrador de catálogo. No se certifican vigencia actual, designaciones o delegaciones del piloto.
- Estado: **documento primario localizado y consultado; correspondencia de roles y vigencia por validar**.

### Fuentes aún insuficientes

No se aportaron entrevistas al requirente, actas de designación ni una decisión institucional vigente para el piloto. Las ediciones específicas de Medicina, Enfermería, Odontología y Derecho continúan fuera de la corroboración de este AA2. La localización de F-07/F-08 mejora la procedencia documental, pero no resuelve CL-011–017 ni modifica los manifiestos originales.

## 3. Fuentes técnicas

### F-09 — RFC 9110, HTTP Semantics

- Autores: R. Fielding, M. Nottingham y J. Reschke; junio de 2022.
- [Documento oficial RFC Editor](https://www.rfc-editor.org/rfc/rfc9110.html).
- Localizadores revisados: §§5 y 6.3–6.4; §§8.1–8.3 y 8.8.3; §§9.2–9.3; §§10.2.2, 11.6, 12.5.1 y 13.1.1; §15; §17.
- Uso: fundamento de decisiones HTTP y revisión de compatibilidad; no fuente de necesidades institucionales ni de umbrales de rendimiento/sesión del proyecto.
- Estado: **referencia técnica consultada**; conformidad completa de la aplicación no acreditada.

### F-13 — WCAG 2.2

- Autor institucional: W3C, Web Content Accessibility Guidelines 2.2.
- [Recomendación W3C](https://www.w3.org/TR/WCAG22/).
- Localizadores de inicio para verificación: teclado 2.1.1, foco visible 2.4.7 y contraste 1.4.3; el nivel AA exige revisar todos los criterios aplicables, no solo esos tres.
- Uso: delimitar verificaciones de AA-RNF-08; RNF-014 del proyecto expresa el nivel objetivo.
- Estado: **referencia técnica consultada; accesibilidad no certificada**.

### F-14 — RFC 5789, PATCH Method for HTTP

- Autores: L. Dusseault y J. Snell; marzo de 2010.
- [Documento oficial RFC Editor](https://www.rfc-editor.org/rfc/rfc5789.html), sección 2.
- Uso: aclarar que PATCH tiene especificación propia. No atribuir su definición a RFC 9110 ni suponer JSON Merge Patch sin contrato.
- Estado: **referencia complementaria consultada**.

## 4. Índice del contraste con implementación

Todas las rutas se expresan desde la raíz del proyecto.

| Código | Archivo exacto | Qué permite observar |
|---|---|---|
| C-01 | `backend/src/main/java/ec/edu/unl/sec/identityaccess/api/SessionController.java` | Login, logout, consulta de identidad, DTO y códigos. |
| C-02 | `backend/src/main/java/ec/edu/unl/sec/identityaccess/application/SessionService.java` | Vigencia/revocación, hash y control de intentos. |
| C-03 | `backend/src/main/java/ec/edu/unl/sec/selfassessment/api/CareerController.java` | Alta/consulta/cambios de unidades y carreras. |
| C-04 | `backend/src/main/java/ec/edu/unl/sec/selfassessment/application/CareerAdministrationService.java` | Código único, revisión, vigencia, consulta y permisos. |
| C-05 | `backend/src/main/java/ec/edu/unl/sec/selfassessment/application/SelfAssessmentService.java` | Creación en preparación, selección publicada y acceso al proceso. |
| C-06 | `backend/src/main/java/ec/edu/unl/sec/shared/api/EntityTagSupport.java` | Formato local de ETag y precondición. |
| C-07 | `backend/src/main/java/ec/edu/unl/sec/identityaccess/infrastructure/AccessProblemWriter.java` | Construcción de errores de acceso; no añade WWW-Authenticate. |
| C-08 | `backend/src/main/java/ec/edu/unl/sec/identityaccess/api/IdentityAccessExceptionHandler.java` | Errores de autenticación; no declara desafío de autenticación. |
| C-09 | `backend/src/main/java/ec/edu/unl/sec/shared/api/ApiProblemHandler.java` | Errores comunes con correlación. |
| C-10 | `backend/src/main/java/ec/edu/unl/sec/identityaccess/infrastructure/JpaIdentityAccessRepository.java` | Conteo de fallos por ventana, sin reinicio por éxito en esa consulta. |

## 5. Reglas de uso de las fuentes

1. Un documento del equipo respalda una decisión del producto; una norma primaria puede respaldar contexto, pero requiere revisar aplicabilidad.
2. Una norma HTTP no justifica quién administra una carrera; un instructivo institucional no define automáticamente rutas o tokens.
3. El código muestra implementación observada; una prueba existente no equivale a un resultado ejecutado en esta actividad.
4. El estado de una fila de trazabilidad debe reflejar sus límites de fuente, diseño y ejecución. En esta entrega no se asigna “Validado institucionalmente”.

