# Validación técnica P01–P16

Fecha local: 2026-10-25. **125 de 126 tareas completadas**; permanece T111,
revisión humana de procedencia, competencias y textos de no acreditación.

| Verificación ejecutada | Resultado |
|---|---|
| T125, [campaña consolidada](process-acceptance.md) | 64 PASS, 0 FAIL, 0 SKIPPED; API real sobre PostgreSQL, cinco modelos sintéticos, actores y cadenas nuevos por escenario |
| T110, [quickstart](p01-p16-quickstart.md) | Recorridos técnicos A–D PASS; revisión institucional separada en T111 |
| Respaldo/restauración | [14 tablas y 30 archivos](p01-p16-restore.md) idénticos; seis consultas de API y descarga autenticada verificadas en segunda instancia |
| Backend | Regresión completa: 563 pruebas, 0 fallos, 0 errores, 17 omitidas opt-in; después `IntegralClosureBytesTest` ejecutada sobre PostgreSQL: 1 PASS |
| Frontend | 107 pruebas PASS en 29 archivos; ESLint y compilación de producción PASS |
| Contratos | 113 operaciones comunes, 77 objetivo, 12 compartidas, 0 incompatibles; Redocly y AsyncAPI PASS; Postman local 12 solicitudes / 24 aserciones PASS |
| Datos sintéticos | `scripts/verify-synthetic-data.ps1`: PASS |
| Documentación | Quarto: 119 documentos HTML generados, sin errores; salida local `backend/target/completion-docs/index.html` |
| Rendimiento previo T106 | 100 usuarios, 30 sesiones, 500 indicadores y 10.000 versiones; p95 de consultas 1,51 s, 0 errores HTTP; no repetido en esta continuación |

Correcciones comprobadas en esta continuación:

- Revocación de asignaciones con If-Match, motivo y auditoría, efectiva en
  sesiones abiertas; pantalla de administración con estado y errores claros.
- Remisión exige coberturas explícitas y contenido del tipo configurado;
  admite snapshots estructurados/institucionales sin archivo. Enlaces locales
  o esquemas ejecutables se rechazan; carga excedida devuelve 413.
- Correlation-ID preservado en respuestas de modelos y procesos; activación
  con modelo no aplicable devuelve 409 y conserva PREPARATION.
- Corrección de informe aprobado continúa la última versión; la aprobación
  previa y sus bytes permanecen, la sucesora requiere nueva tramitación.
- Mejora sin avances no puede someterse a verificación ni darse por verificada.
- Cierre integral conserva los bytes JSON originales en PostgreSQL: se retiró
  la conversión a jsonb que cambiaba espacios/orden después del hash.

Logs transitorios de Maven, Angular, Playwright y PostgreSQL: `backend/target/`.
Las evidencias versionables están en los informes enlazados.

Límites: T111 no está aprobada. La accesibilidad mantiene el alcance de T105
(18 recorridos Edge; pendientes contraste/lector/Firefox); T106 mide consultas,
no carga de archivos. Las pruebas no certifican todas las competencias reales
ni un plan de recuperación institucional. Los cierres generados por versiones
anteriores con JSON reformateado no se reparan ni se vuelven a sellar aquí.
