# Contratos objetivo y compatibilidad

**Estado**: diseño planificado, sujeto a revisión antes de implementar. La validación OpenAPI comprueba estructura y referencias; no demuestra que una operación esté implementada ni que un permiso institucional esté aprobado.

## Autoridad de los contratos

- [Contrato común existente](../../../docs/technical/contracts/openapi.yaml): conserva las operaciones ya especificadas de acceso, carga, remisión, sucesión, consultas y auditoría.
- [Contrato objetivo de esta entrega](openapi.yaml): define operaciones nuevas o revisadas de P02–P16 con cuerpos y respuestas tipados. En rutas coincidentes, esta versión es la propuesta objetivo y requiere implementación/migración explícitas.
- Los contratos de `specs/001`–`004` conservan su historia. No se aplican silenciosamente esquemas nuevos a clientes anteriores.

El contrato común conserva `/api/v1`. El objetivo de 005 usa ahora `/api/v2` para sus cambios incompatibles planificados; las rutas D1 de carreras/unidades tienen una excepción explícita `servers: /api/v1` y se comparten por `$ref`, sin copiar DTO divergentes. Ambos mantienen cookie `SEC_SESSION` y `X-CSRF-TOKEN`. Esta decisión se documenta en [ADR-0018](../../../docs/adr/0018-catalog-contract-migration-compatibility.md). La versión v2 aún no está implementada: no se cambia el cliente existente ni se presenta el contrato objetivo como un backend disponible.

## Reglas de operación

1. Toda mutación dirigida a un agregado existente exige `If-Match`; las consultas que lo preceden devuelven su ETag fuerte. Crear un agregado raíz no lo requiere.
2. En recursos nuevos, el servicio asigna actor, fecha, ID y revisión. Nunca confía en un `createdBy` enviado por el cliente.
3. Los listados son paginados (`page >= 0`, `1 <= size <= 100`), con orden estable por fecha/ID; `totalElements` solo cuenta objetos del ámbito autorizado.
4. La capacidad declarada por `x-required-capability` es necesaria, pero también se validan objeto, carrera, proceso, indicador, vigencia, conflicto de funciones y estado. Equipos requieren además membresía vigente y asignación funcional de cada actuante.
5. Los comandos de decisión incluyen `authorityAssignmentId`, fundamento y constancia. El backend verifica que esa asignación corresponde al actor o a una representación permitida por la política.
6. En publicación se verifica integridad de la versión completa; un borrador puede conservar campos pendientes. Una regla sin estrategia soportada conserva su definición documental pero no ejecuta cálculo.
7. En asignaciones se exige exactamente uno de `userId` o `teamId`. Las referencias opcionales se envían como `null`; todos los IDs deben pertenecer al mismo ámbito y versión cuando aplique.
8. Una cobertura solo se declara sobre borradores. Archivo, enlace, dato estructurado y referencia institucional son modalidades de fuente; remisión exige al menos una modalidad que satisfaga las condiciones de la fuente.
9. Una decisión de revisión debe incluir todas las coberturas de la versión, sin duplicados. El resultado global se deriva; el cliente no puede imponerlo.
10. Una evaluación final cualitativa requiere opción de escala y justificación; una cuantitativa requiere `calculationRunId` obtenido de la regla validada. Valores fuera de unidad, período, escala o rango se rechazan. Una limitación puede guardar un borrador, pero no sustituye la regla de finalización.
11. Emitir informe crea contenido inmutable. Socialización, observaciones, respuestas y aprobación se anexan a una versión identificada. No existe `PATCH` del contenido de una versión emitida.
12. Operaciones que crean revisiones de acciones, decisiones o cierres son transaccionales con auditoría obligatoria. Una repetición con ETag antiguo no crea una segunda decisión vigente.

## Errores estables

| HTTP | Uso | Ejemplos de `code` |
|---|---|---|
| 400 | JSON o parámetros mal formados | `INVALID_REQUEST` |
| 401 | Sesión ausente, revocada o expirada | `AUTHENTICATION_REQUIRED` |
| 403 | Falta capacidad para una colección u operación visible | `CAPABILITY_DENIED`, `SELF_REVIEW_FORBIDDEN` |
| 404 | Recurso desconocido o fuera de ámbito, indistinguibles | `RESOURCE_NOT_FOUND` |
| 409 | Conflicto de dominio/estado | `DUPLICATE_CODE`, `INVALID_TRANSITION`, `CLOSURE_BLOCKED`, `ALREADY_DECIDED` |
| 412 | ETag no coincide | `STALE_REVISION` |
| 422 | Contenido incompatible con las reglas | `MODEL_NOT_APPLICABLE`, `AUTHORIZATION_MISSING`, `SOURCE_PERIOD_MISMATCH`, `RULE_NOT_VALIDATED`, `VARIABLE_MISSING`, `SCALE_MISMATCH` |
| 428 | Falta ETag en mutación que lo exige | `PRECONDITION_REQUIRED` |

Todos los errores usan `application/problem+json` con correlación y detalles seguros. Errores de archivo `413/503`, login `429` y mecanismos de recuperación existentes se conservan en el contrato común. No incluir rutas de almacenamiento, secretos ni datos de otro ámbito.

## Validación reproducible

Desde la raíz:

```powershell
& .\frontend\node_modules\.bin\redocly.cmd lint .\specs\005-complete-evaluation-workflows\contracts\openapi.yaml
quarto render
```

Antes de implementar una entrega: comparar el contrato objetivo con el contrato común, revisar autorización por objeto y registrar los cambios compatibles o la estrategia de versionado. Las pruebas de endpoint y Postman CLI requieren una implementación ejecutable; no se marcan satisfechas por pasar el lint.
