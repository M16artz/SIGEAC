# T108 — Migraciones limpias e incrementales hasta V026

Fecha: 2026-10-25. Resultado: **PASS técnico** con datos sintéticos.

Entorno: Windows, JDK 25, Maven Wrapper, Flyway y PostgreSQL 18.4 temporal en
loopback, sin Docker. El esquema actual supera la reserva original V020:
existen 27 migraciones versionadas V001–V026, incluida V016.1.

`CompleteWorkflowMigrationTest`: **20 pruebas, 0 fallos, 0 errores, 0 omitidas**.

- Una aplicación desde esquema limpio hasta V026.
- Actualizaciones desde V008, V009, V010, V011, V012, V013, V014, V015, V016,
  V016.1, V017, V018, V019, V020, V021, V022, V023, V024 y V025.
- Cada actualización conserva una unidad académica sintética, los checksums,
  versiones y orden del historial anterior. No se usa `repair`, `clean` ni
  `outOfOrder`. Flyway valida el historial después de aplicar las pendientes.
- Se comprueban las tablas de carrera/unidad, cobertura, revisión, informes,
  mejora, cierre integral y archivo en el esquema final. Cada prueba usa un
  esquema único, que elimina al terminar.

Reproducción desde `backend`, con PostgreSQL exclusivo de pruebas preparado:

```powershell
$env:JAVA_HOME = 'C:/Program Files (x86)/Java/jdk-25'
$env:SEC_RUN_FILE_DATABASE_TESTS = 'true'
$env:SEC_TEST_DB_URL = 'jdbc:postgresql://127.0.0.1:55439/postgres'
$env:SEC_TEST_DB_USER = 'sec_phase13'
# Configure SEC_TEST_DB_PASSWORD según la instancia local de pruebas.
.\mvnw.cmd -q '-Dtest=CompleteWorkflowMigrationTest,ModuleArchitectureTest,OpenApiContractTest' test
```

La misma ejecución aprobó 13 reglas ArchUnit y 6 pruebas de contrato (39 casos
en total). La conservación del catálogo y del historial de migraciones no
equivale a una restauración completa de todos los agregados y archivos; esa
campaña permanece fuera de T108. Los informes completos se generan en
`backend/target/surefire-reports/` y se ignoran por Git.
