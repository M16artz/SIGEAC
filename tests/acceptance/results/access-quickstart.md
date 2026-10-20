# Resultados de validación rápida I0

- Fecha: 2026-10-20
- Entorno: Windows, JDK 25, Maven Wrapper, Node.js con Angular 22, PostgreSQL local no disponible.
- Datos: sintéticos (`sec.demo-data=true`), sin datos institucionales.

## Comandos ejecutados

| Comando | Resultado |
|---|---|
| `cd backend && .\mvnw.cmd test` | 40 pruebas, 0 fallos, 2 omitidas (migración PostgreSQL condicionada) |
| `cd frontend && npm run build` | Compilación de producción correcta |
| `cd frontend && npm run lint` | Sin hallazgos |
| `cd frontend && npm test` | 2 pruebas, 0 fallos |
| `node scripts/validate-contracts.mjs` | OpenAPI 3.1 y AsyncAPI válidos |
| `.\scripts\verify-synthetic-data.ps1` | Verificación sintética correcta |

## Pendientes de ejecución

| Actividad | Motivo |
|---|---|
| Migración limpia y prueba append-only contra PostgreSQL | No hay PostgreSQL ni Testcontainers disponibles en este entorno. Ejecutar con `SEC_RUN_DATABASE_TESTS=true` sobre base descartable. |
| Aceptación Playwright | Requiere backend y frontend en ejecución con `SEC_DEMO_DATA=true`. El recorrido está en `tests/acceptance/access-scopes.spec.ts`. |
| SC-007 usabilidad con 20 participantes | Requiere sesión presencial y protocolo de observación. |
| Revisión constitucional final | Se realizará después de la migración limpia y la aceptación. |

## Observaciones

- Las pruebas unitarias cubren sesión, autorización por ámbito, conflictos, excepciones, auditoría, controladores de sesión y arquitectura modular.
- La auditoría usa `audit_event.metadata_json` como `jsonb`; la entidad `AuditEventEntity` mapea `Map<String,Object>` con `@JdbcTypeCode(SqlTypes.JSON)`.
- La aceptación incluye el flujo de inicio de sesión, alta de identidad, asignación, excepción, consulta de auditoría y cierre de sesión.
