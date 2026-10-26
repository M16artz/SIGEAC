# Campaña consolidada P01–P16

Fecha UTC: 2026-10-26T00:34:57.900Z. Datos sintéticos; PostgreSQL local.

Resultado: **64 PASS, 0 FAIL, 0 SKIPPED**.

Comando: `npm run test:complete-workflows` desde `frontend`, con backend preparado como en `tests/performance/README.md`.

Cada PASS ejecuta el recorrido de `tests/acceptance/complete-workflows.spec.ts`; no deriva de nombres de tareas ni de resultados anteriores. Esta ejecución cubre los criterios técnicos con fixtures sintéticos; la aprobación institucional corresponde a T111. El quickstart y la restauración tienen evidencias separadas. El JSON detallado se genera en `backend/target/complete-workflows-playwright.json` y no se versiona.

| Escenario | Resultado | Evidencia o motivo |
|---|---|---|
| AC-P01-01 | PASS | 413 ms; API real, sin mocks |
| AC-P01-02 | PASS | 1521 ms; API real, sin mocks |
| AC-P01-03 | PASS | 840 ms; API real, sin mocks |
| AC-P02-01 | PASS | 184 ms; API real, sin mocks |
| AC-P02-02 | PASS | 47 ms; API real, sin mocks |
| AC-P02-03 | PASS | 399 ms; API real, sin mocks |
| AC-P03-01 | PASS | 77 ms; API real, sin mocks |
| AC-P03-02 | PASS | 41 ms; API real, sin mocks |
| AC-P03-03 | PASS | 107 ms; API real, sin mocks |
| AC-P04-01 | PASS | 287 ms; API real, sin mocks |
| AC-P04-02 | PASS | 94 ms; API real, sin mocks |
| AC-P04-03 | PASS | 82 ms; API real, sin mocks |
| AC-P05-01 | PASS | 115 ms; API real, sin mocks |
| AC-P05-02 | PASS | 131 ms; API real, sin mocks |
| AC-P05-03 | PASS | 118 ms; API real, sin mocks |
| AC-P06-01 | PASS | 366 ms; API real, sin mocks |
| AC-P06-02 | PASS | 245 ms; API real, sin mocks |
| AC-P06-03 | PASS | 251 ms; API real, sin mocks |
| AC-P07-01 | PASS | 606 ms; API real, sin mocks |
| AC-P07-02 | PASS | 502 ms; API real, sin mocks |
| AC-P07-03 | PASS | 636 ms; API real, sin mocks |
| AC-P08-01 | PASS | 234 ms; API real, sin mocks |
| AC-P08-02 | PASS | 250 ms; API real, sin mocks |
| AC-P08-03 | PASS | 280 ms; API real, sin mocks |
| AC-P09-01 | PASS | 259 ms; API real, sin mocks |
| AC-P09-02 | PASS | 262 ms; API real, sin mocks |
| AC-P09-03 | PASS | 232 ms; API real, sin mocks |
| AC-P10-01 | PASS | 317 ms; API real, sin mocks |
| AC-P10-02 | PASS | 272 ms; API real, sin mocks |
| AC-P10-03 | PASS | 251 ms; API real, sin mocks |
| AC-P11-01 | PASS | 257 ms; API real, sin mocks |
| AC-P11-02 | PASS | 291 ms; API real, sin mocks |
| AC-P11-03 | PASS | 289 ms; API real, sin mocks |
| AC-P12-01 | PASS | 507 ms; API real, sin mocks |
| AC-P12-02 | PASS | 466 ms; API real, sin mocks |
| AC-P12-03 | PASS | 1759 ms; API real, sin mocks |
| AC-P13-01 | PASS | 237 ms; API real, sin mocks |
| AC-P13-02 | PASS | 220 ms; API real, sin mocks |
| AC-P13-03 | PASS | 311 ms; API real, sin mocks |
| AC-P14-01 | PASS | 324 ms; API real, sin mocks |
| AC-P14-02 | PASS | 270 ms; API real, sin mocks |
| AC-P14-03 | PASS | 326 ms; API real, sin mocks |
| AC-P15-01 | PASS | 415 ms; API real, sin mocks |
| AC-P15-02 | PASS | 359 ms; API real, sin mocks |
| AC-P15-03 | PASS | 330 ms; API real, sin mocks |
| AC-P16-01 | PASS | 696 ms; API real, sin mocks |
| AC-P16-02 | PASS | 461 ms; API real, sin mocks |
| AC-P16-03 | PASS | 500 ms; API real, sin mocks |
| AC-X01 | PASS | 606 ms; API real, sin mocks |
| AC-X02 | PASS | 1072 ms; API real, sin mocks |
| AC-X03 | PASS | 746 ms; API real, sin mocks |
| AC-X04 | PASS | 85 ms; API real, sin mocks |
| AC-X05 | PASS | 246 ms; API real, sin mocks |
| AC-X06 | PASS | 220 ms; API real, sin mocks |
| AC-X07 | PASS | 219 ms; API real, sin mocks |
| AC-X08 | PASS | 237 ms; API real, sin mocks |
| AC-X09 | PASS | 271 ms; API real, sin mocks |
| AC-X10 | PASS | 72 ms; API real, sin mocks |
| AC-X11 | PASS | 1687 ms; API real, sin mocks |
| AC-X12 | PASS | 327 ms; API real, sin mocks |
| AC-X13 | PASS | 324 ms; API real, sin mocks |
| AC-X14 | PASS | 154 ms; API real, sin mocks |
| AC-X15 | PASS | 491 ms; API real, sin mocks |
| AC-X16 | PASS | 198 ms; API real, sin mocks |
