# Fase 13 — Manuales, accesibilidad, migraciones y arquitectura

Fecha: 2026-10-25. Datos exclusivamente sintéticos. Resultado: **PASS técnico
del alcance ejecutado; fase 13 aún abierta**.

| Tarea | Evidencia ejecutada |
|---|---|
| T103 | Índice técnico D1–D8 con rutas de evidencia, migraciones actuales, sesión/CSRF, ETag y conservación conjunta |
| T104 | Manual por función P01–P16 y guías de modelos/planificación, evidencias/revisión e informes/mejora/archivo |
| T105 | 18 recorridos Playwright PASS en Edge headless, backend real SQLite con perfil explícito `sqlite-test,d9-archive-test`, sin mocks HTTP |
| T108 | [20 pruebas PostgreSQL PASS](migrations-p01-p16.md) desde limpio y cada versión previa V008–V025 |
| T109 | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify-synthetic-data.ps1`: PASS |
| T126 | 13 reglas ArchUnit PASS y [revisión de módulos/contrato/permisos](../../../docs/architecture/complete-workflows.md), con diferencias registradas |

Regresión frontend: **103 pruebas PASS en 27 archivos**, ESLint y compilación
de producción PASS. Quarto renderizó **113 documentos HTML**, incluidos los
manuales nuevos, con salida en `backend/target/phase13-docs/index.html`.
El primer intento en aislamiento falló al abrir la caché de Quarto; la repetición
fuera del aislamiento terminó con código 0. No se cambiaron dependencias.
Las verificaciones de contrato y su bloqueo Postman
se registran en [T107](contracts-p01-p16.md).

## Accesibilidad comprobada

El recorrido autentica por teclado, comprueba el salto al contenido sin perder
la URL de retorno, una sola región principal, navegación nombrada, nombres
accesibles de controles visibles, encabezados de tabla con `scope`, selección
del proceso mediante teclado y mensajes de error de inicio de sesión.

Pantallas: carreras, modelos, procesos, planificación, actividades, evidencias,
nuevo borrador, revisión, tablero documental, historial, cierre documental,
valoración manual, cálculo, conciliación, informes, mejora y archivo.

Se corrigieron el salto al contenido (el fragmento relativo a la base de Angular
perdía la URL/consulta), cinco regiones `main` anidadas y el `scope` de la tabla
de procesos. Se añadieron enlaces de navegación a informes, mejora y archivo
para las funciones del flujo; la autorización definitiva permanece en backend.

Reproducción: iniciar backend y frontend como en [D8 archivo](d8-archive.md),
con base de pruebas nueva. Desde `frontend`: `npm run test:accessibility`.
Usa Edge instalado; `SEC_BROWSER_CHANNEL` permite cambiar el canal y
`SEC_WEB_BASE_URL` la URL. Los artefactos de fallo permanecen ignorados por Git.

Estos recorridos no certifican WCAG 2.2 AA: faltan auditoría de contraste,
lectores de pantalla, Firefox y evaluación humana. Las páginas de detalle de
evidencia y sus variantes requieren cobertura adicional. T105 acredita la
adición y ejecución de los recorridos descritos, no esas verificaciones pendientes.

## Pendientes de la fase

T106 (dataset y campaña de rendimiento; el comando heredado apunta a
`evidence-upload.js`, ausente), T107 (ruleset de Postman), T110 (quickstart
completo), T111 (revisión humana), T112 (trazabilidad final) y T125 (48 AC-P y
16 AC-X) siguen abiertas. Las pruebas actuales no se sustituyen por marcadores
PASS nominales. El escáner sintético comprueba patrones de claves privadas y
correos institucionales; no es una prueba exhaustiva de ausencia de secretos.
