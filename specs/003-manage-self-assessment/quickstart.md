# Quickstart: I2

1. Publicar modelo sintético v1 y crear proceso borrador para carrera/período.
2. Activarlo y verificar versión/indicadores fijados.
3. Asignar responsable, revisor y evaluador con ámbitos/vigencias válidos; rechazar matrices inválidas.
4. Publicar modelo v2 y confirmar que el proceso conserva v1.
5. Ejecutar `ACTIVE→CLOSING→CLOSED`; rechazar mutaciones.
6. Reabrir con autorización/motivo, conservar cierre previo y volver a `ACTIVE`.
7. Simular ETag obsoleto y consultar auditoría.

Validar con Maven, npm, contratos y escaneo sintético. PostgreSQL limpio usa `SEC_RUN_DATABASE_TESTS=true` sobre base descartable.
