# Resultados parciales de validación I1

- Fecha: 2026-10-20
- Datos: exclusivamente sintéticos.

| Verificación | Resultado |
|---|---|
| Backend `mvnw verify` | 53 pruebas, 0 fallos, 3 omitidas condicionadas a PostgreSQL |
| Frontend `npm test` | 2 pruebas, 0 fallos |
| Frontend lint/build | Correctos |
| OpenAPI I1 y contrato global | Válidos con Redocly |
| AsyncAPI | Válido |
| Escaneo sintético | Correcto |

Pendiente para cerrar T034: migración limpia V004 contra PostgreSQL descartable y recorrido Playwright con servicios activos. El equipo no dispone de PostgreSQL ni Docker en este entorno. T035 requiere observación de 20 participantes y T036 se ejecuta después de esas puertas.
