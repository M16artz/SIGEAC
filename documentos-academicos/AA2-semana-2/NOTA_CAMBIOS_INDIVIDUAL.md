# AA2 — Nota de cambios y evidencia individual

| Identificación | Registro |
|---|---|
| Estudiante | [PENDIENTE: nombre completo] |
| Proyecto | SIGEAC / SEC-UNL |
| Actividad | AA2, dossier y matriz inicial; 4,5 horas previstas |
| Documento previo | `documentos-academicos/APE2-semana-2/specification.md` |
| Revisión actual | [DOSSIER_REQUISITOS_AA2.md](DOSSIER_REQUISITOS_AA2.md) y [MATRIZ_TRAZABILIDAD.md](MATRIZ_TRAZABILIDAD.md) |
| Revisión personal / fecha | [PENDIENTE] |

## 1. Principales cambios efectivamente preparados

1. Se consultaron RFC 9110, dos documentos primarios CACES/UNL y referencias de accesibilidad/PATCH, registrando localizadores y límites en F-01–14.
2. La historia de catálogo APE2/US2 se separó en alta, lectura, edición y desactivación: HU-AA-02–05. Se conservaron acceso y preparación como HU-AA-01/06, sin ampliar alcance.
3. Los diez FR locales de APE2 se desglosaron en catorce obligaciones para evitar que login, logout, consulta de identidad o cambios de carrera se consideren un único caso probado.
4. Se separaron seguridad, expiración y privacidad, y evento de auditoría frente a su inmutabilidad/correlación. Se retienen diez RNF con verificaciones VC-AA-01–10.
5. Se separaron los rechazos compuestos de APE2 en criterios por formato, unidad y fechas; el dossier contiene 27 AC y diez VC, no resultados de ejecución.
6. Se construyeron 24 relaciones completas en la matriz; el estado expresa trazado, revisión, medición o validación de dominio pendiente.
7. Se registró HTTP-AA-01: contraste pendiente de WWW-Authenticate en respuestas 401. No se inventó un esquema Bearer ni se modificó código.

## 2. Equivalencia de requisitos funcionales

| Origen APE2 | Elementos AA2 | Razón de la revisión |
|---|---|---|
| APE-FR-01 | AA-RF-01/02/03 | Separar iniciar sesión, cerrarla y consultar funciones. |
| APE-FR-02 | AA-RF-04 | Conservar autorización por capacidad/ámbito sin repetirla como beneficio de todas las historias. |
| APE-FR-03 | AA-RF-05/06 | Separar alta de unidad y carrera y sus validaciones. |
| APE-FR-04 | AA-RF-07 | Precisar catálogo, detalle e historial visible. |
| APE-FR-05 | AA-RF-08/09 | Distinguir edición y desactivación; comparten conservación histórica, no resultado. |
| APE-FR-06 | AA-RF-10 | Identificar versión publicada sin inferir vigencia institucional. |
| APE-FR-07 | AA-RF-11 | Preparación con referencias exactas, sin activación. |
| APE-FR-08 | AA-RF-12 | Consulta de proceso según permiso aplicable. |
| APE-FR-09 | AA-RF-13 | Distinguir registro obligatorio de la calidad de inmutabilidad. |
| APE-FR-10 | AA-RF-14 | Mantener error estable, con privacidad/correlación verificadas aparte. |

## 3. Equivalencia y disposición de requisitos no funcionales

| Origen APE2 | Tratamiento en AA2 | Justificación |
|---|---|---|
| APE-NFR-01 | AA-RNF-01/02/03 | Hash, expiración y privacidad son comprobaciones diferentes. |
| APE-NFR-02 | AA-RNF-04 | Se conserva el umbral del proyecto y se declara falta de protocolo. |
| APE-NFR-03 | Global RNF-009, diferido del recorte | No hay canal complementario operativo en el incremento para justificar su ensayo local. No se elimina del producto. |
| APE-NFR-04 | AA-RNF-05 | Concurrencia definida como ausencia de pérdida silenciosa, no como presencia de una cabecera. |
| APE-NFR-05 | AA-RNF-06/07 | Inmutabilidad y correlación requieren evidencias diferentes. |
| APE-NFR-06 | AA-RNF-08 | Criterios aplicables y revisión humana; no certificar AA por etiquetas. |
| APE-NFR-07 | AA-RNF-09 | Mantener reglas reproducibles y revisar acoplamiento SQL. |
| APE-NFR-08 | AA-RNF-10 | Contraste de contrato y protocolo, incluida la observación HTTP-AA-01. |
| APE-NFR-09 | Objetivo global SC-007 y P-AA-05 | No trasladar una campaña de 20 personas al trabajo individual sin diseño ni aprobación. |
| APE-NFR-10 | Restricción de pruebas/demostración | Datos sintéticos se mantienen como condición transversal; evita duplicar un atributo de calidad de ejecución. |

Estos cambios son una propuesta de depuración académica; no se presentan como aprobación del equipo ni modifican los documentos previos.

## 4. Evidencia y contribución que debe identificar el estudiante

La preparación fue asistida por ChatGPT/Codex. Para que sea evidencia individual, completar:

- Elementos revisados personalmente: [PENDIENTE: IDs de historias/requisitos/filas].
- Fuentes comprobadas personalmente y localizador: [PENDIENTE].
- Corrección adoptada y justificación propia: [PENDIENTE].
- Pregunta que permanece abierta y razón: [PENDIENTE].
- Fecha y enlace a revisión/commit/PR real, si se publica: [PENDIENTE].

Texto de nota breve para completar: “Revisé personalmente [IDs]. A partir de [fuente/localizador] cambié [antes] por [después], porque [motivo]. Relacioné el resultado con [AC/VC] y dejé pendiente [decisión]. Mi evidencia verificable es [ruta o enlace real]”.

El commit base `faf868ea5a8a85285290c06d3baf8fa49889edac` identifica el código inspeccionado; **no acredita autoría del AA2**. No se crearon commits, PR ni publicaciones. El manifiesto de esta carpeta identifica integridad y línea base, no quién participó ni cuánto tiempo trabajó.

