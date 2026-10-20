# Quickstart: I1

## Validate

```powershell
Set-Location backend
.\mvnw.cmd verify
Set-Location ..\frontend
npm test
npm run lint
npm run build
Set-Location ..
node scripts/validate-contracts.mjs
```

Las pruebas PostgreSQL pueden ejecutarse sin Docker contra una base descartable con `SEC_RUN_DATABASE_TESTS=true` y las variables JDBC documentadas.

## Synthetic journey

1. Crear el modelo “Modelo Piloto Sintético”.
2. Crear v1 con fuente, autor, criterio, estándar, indicador, escala/opción y fórmula declarativa.
3. Omitir por turno cada requisito y verificar rechazo agregado, sin publicación parcial.
4. Publicar v1 con aprobador distinto; consultar instantánea y auditoría.
5. Intentar modificar fuente, jerarquía, escala y fórmula de v1; todo se rechaza.
6. Crear v2 desde v1, modificarla, comparar v1/v2 y publicarla como sucesora.
7. Confirmar v1 íntegra, v2 con procedencia y ausencia de mezcla.
8. Repetir escritura con ETag obsoleto y comprobar `412`.

Expected: SC-001–SC-005 y SC-007 alcanzan 100 %. SC-006 requiere 20 participantes y no se infiere de automatización.
