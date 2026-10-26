# Respaldo y restauracion conjunta P01-P16

Fecha UTC: 2026-10-26T00:38:03.0111448Z. Resultado: **PASS**. Solo datos sinteticos.

PostgreSQL 18: pg_dump -Fc, creacion de base independiente, pg_restore --exit-on-error y consulta de filas completas.
Se comparan datos de origen/restaurados sin modificar la base de origen ni reescribir hashes.

| Tabla | Filas | SHA-256 de filas completas ordenadas | Resultado |
|---|---:|---|---|
| self_assessment_process | 331 | 2d7e82b916213d1fb6276a3570b1ff8d0c12e2edfce8ef33cb31196adaf6c570 | PASS |
| model_version | 478 | e34a1f60e4d01abc29b0fa41ba8a3dc4290e2419887827fd02944a924f0ac3b0 | PASS |
| evidence_version | 10310 | d0368d44fdc2059713f06f8b58b31a931913c78abc66f62eba932f365d223463 | PASS |
| source_coverage | 10326 | 87fc722b1a05d0fad2a4aae756854eeff1d68004241fa99b943312926f28f758 | PASS |
| document_review_decision | 237 | 1f0d11b72de33daac773b344ca7652d9bf087286a4f6576d1381c0cceb86705b | PASS |
| manual_academic_evaluation | 75 | 4576f51e3f6a0a1a316670815f3fea450435f31397134fbe862f34af3aefb32c | PASS |
| quantitative_calculation | 6 | 230fa1591f56e4780556fbbb634948264e9d1ccd22a062739c8b1fef538da5c9 | PASS |
| governance_report_version | 82 | 3ea4ed8f796600ec5552815721097a74177f5165c26068411667cfe4290be695 | PASS |
| governance_report_decision | 28 | 433b8cdfe1a519b52fc27e8dd74eaab0b472e52cce9446fa3b6e9b569d249095 | PASS |
| improvement_action | 10 | c62e3d19becd801b2ea844053ea6b018f1e36b3290785946abddc1d79dfdaef0 | PASS |
| improvement_progress | 2 | 2567acd6387027a3a8cd98aa54dba33c7cd1b90f4c32184a3879889092287912 | PASS |
| integral_closure | 6 | e89268a9586c525feeb88cd7a1d76a5ac0390421f4738e4bdf8d7b6576c36efd | PASS |
| archive_record | 3 | 45e1cd0a7926c337fe7ad52e03017f57d6692363522d5dd8f4b6490379a41c76 | PASS |
| audit_event | 7020 | facd91dd0ee7412f8de3747545b3f19ae29994e05eb56b417dd59d45c9e880df | PASS |

Archivos copiados y verificados individualmente: **30**.
SHA-256 del manifiesto de rutas relativas y hashes: `f73d7f9137454cba0b42ba86bc8f574ed53661f8abc761397a97763826aa8827`.
SHA-256 del dump: `d2001b296c8e82539ce7847cb0e423ed376b8e8ea5e20a39e0bcfeea88156ae9`.

Alcance: restauracion local descartable y comparacion integra de datos/archivos. No mide RPO/RTO ni sustituye una politica institucional de retencion.

Consulta posterior desde una segunda instancia Spring Boot en puerto 18088: **PASS**, 6 respuestas JSON idénticas (proceso cerrado, cierres, hallazgos, informes y tramitación). Hash de cierre conservado: `20e662ee206c1b091c922b7cb158cf65a376eea39592c010e7e1ddde2edc8ce3`. Descarga autenticada de archivo restaurado: PASS, bytes idénticos y SHA-256 `7fef0d8820b2ec446627e8daa4521746eaf88040ef726e39b32684b995b7f005`.
