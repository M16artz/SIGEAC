# Quickstart: I0

## Validate

```powershell
Set-Location backend
.\mvnw.cmd verify
Set-Location ..\frontend
npm test
npm run lint
```

## Synthetic journey

1. Crear identidad activa y credencial BCrypt; iniciar sesión y consultar `/me`.
2. Crear asignación con carrera, proceso, indicador y vigencia; comprobar permiso y rechazo indistinguible fuera de ámbito.
3. Revocar asignación durante la sesión y confirmar reevaluación inmediata; cerrar sesión y rechazar reutilización.
4. Simular 30 minutos de inactividad y cinco fallos en 15 minutos con reloj controlado.
5. Rechazar una combinación conflictiva; aceptar una excepción sintética autorizada y comprobar su expiración.
6. Consultar auditoría dentro del ámbito y confirmar ausencia de secretos/datos ajenos.

Expected: SC-001–SC-006 alcanzan 100 % y ningún privilegio técnico concede competencia funcional.
