# SIGEAC · SEC-UNL

Sistema de apoyo a la autoevaluación de carreras: modelos versionados, evidencias,
revisión, valoración académica, informes, mejora y archivo. No concede acreditación.

**Estado:** existen código y pruebas para P01–P16. La aceptación institucional,
la API objetivo v2 completa y Docker siguen abiertos. Véase [auditoría](docs/audit/report.qmd).

## Ejecución local

Requiere JDK 21–25 (CI usa 25), Maven Wrapper, PostgreSQL 18 y Node compatible
con [package.json](frontend/package.json). Crear base y usuario propios y definir
`SEC_DB_URL`, `SEC_DB_USER`, `SEC_DB_PASSWORD` y `SEC_FILE_ROOT` en el proceso.
No se presupone carga automática de `.env`. Flyway inicializa el esquema.

En PowerShell desde la raíz:

```powershell
Set-Location backend
.\mvnw.cmd spring-boot:run
```

En otra terminal desde la raíz:

```powershell
Set-Location frontend
npm.cmd ci
npm.cmd start
```

Interfaz: `http://localhost:4200`. Swagger:
`http://localhost:8080/api/v1/swagger-ui/index.html` (desactivado en `prod`).
La [guía SQLite](docs/testing/sqlite.md) permite pruebas sintéticas sin PostgreSQL.

## Verificación y documentación

- Backend, desde `backend/`: `.\mvnw.cmd verify`.
- Frontend, desde `frontend/`: `npm.cmd test` y `npm.cmd run contracts`.
- API con Postman: `npm.cmd --prefix frontend run test:api`; [colección y configuración](tests/postman/README.md).
- Portal, desde la raíz con Quarto: `quarto render` o `quarto preview`.

Comandos contrastados con el repositorio; resultados y límites en
[validación](docs/audit/validation.qmd). `infrastructure/compose.yaml` está vacío;
no hay despliegue Docker reproducible definido.

## Fuentes principales

- [Contrato de respuesta, HTTPS y OIDC](docs/api/response-contract.qmd).

- [Portal Quarto](index.qmd), [operación](docs/audit/operation.qmd), [API](docs/audit/api.qmd) y [pruebas](docs/audit/testing.qmd).
- `backend/`: Spring Boot, seguridad, JPA/JDBC y Flyway; `frontend/`: Angular.
- `specs/` y `.specify/`: fuentes SDD; `docs/`: portal, contratos, ADR y diagramas.
- `tests/`: aceptación y evidencia histórica.

Una especificación o contrato planificado no acredita implementación.
