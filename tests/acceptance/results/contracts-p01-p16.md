# T107 — Contratos P01–P16

Fecha: 2026-10-25. Resultado: **PASS técnico de T107** con lint de contratos y
colección Postman local contra la API real. El lint remoto de Postman conserva
el 401 documentado; la colección local permite verificar respuestas sin ese ruleset.

| Verificación ejecutada | Resultado |
|---|---|
| Redocly, contrato común | PASS; un aviso por `ReasonCommand` legado, deprecado y conservado para compatibilidad |
| Redocly, contrato 004 | PASS |
| Redocly, contrato objetivo 005 | PASS |
| Comparación de superficies | PASS: 113 operaciones comunes, 77 objetivo, 12 compartidas, 0 incompatibles; incluye revocación de asignación con If-Match |
| AsyncAPI | PASS, sin problemas de gobernanza del documento |
| OpenApiContractTest | 6 pruebas PASS |
| Postman CLI, `api lint` del contrato común | BLOCKED: HTTP 401 al obtener el ruleset |
| Postman CLI, colección local generada desde el contrato común | PASS: 12 solicitudes, 24 comprobaciones, 0 fallos |

Comandos desde la raíz:

```powershell
node scripts/validate-contracts.mjs
node scripts/run-postman-contracts.mjs
```

El primer intento de Postman en aislamiento falló con `AggregateError`.
Se repitió fuera del aislamiento y respondió:
`Status code:401 Something went wrong when fetching the ruleset. Try again.`
No se suprimió el código de salida ni se presentó el lint remoto como aprobado.
En su lugar se ejecutó `collection run --no-report-events` con la colección
generada por `scripts/run-postman-contracts.mjs`. Resuelve los `$ref` internos
y externos, exige que cada operación/status exista en OpenAPI y usa el esquema
de esa respuesta en las aserciones de Postman. No publica resultados ni contratos.

Se verificaron sesión, identidad, avance documental paginado, cierres,
paginación inválida (422), recurso fuera de ámbito/inexistente (404), historial
configurado, creación de sucesora configurada, cierre de sesión y rechazo
posterior (401). El aviso sobre credenciales cloud no impidió la ejecución local:
código 0, 12 solicitudes y 24 aserciones PASS. Se generó una sucesora sintética
en borrador para comprobar la nueva ruta. El contrato común incluye esas dos
rutas aditivas y conserva las rutas legadas.

El backend se prepara como en `tests/performance/README.md`. La colección
temporal se escribe en `backend/target/postman-contracts/`. No acredita todos
los recorridos de las 112 operaciones ni reemplaza T125; acredita lint completo
de la superficie y comprobaciones de respuestas reales en los casos enumerados.

AsyncAPI informó fallos al enviar métricas anónimas y restricciones de consulta
de procesos en aislamiento; terminó con código 0 y documento válido. Estos
avisos no alteran el resultado de validación local. El comparador comprueba
colisiones compartidas; no certifica toda la implementación de la API objetivo.
