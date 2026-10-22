# ADR-0006: Modelo fijado y máquina de estados del proceso

**Fecha**: 2026-10-20  
**Estado**: Aceptada

Al activar, I2 copia la identidad/número de la versión publicada y sus indicadores al proceso. La referencia no se vuelve a resolver. Los estados solo avanzan por la matriz explícita; reapertura y cambios críticos requieren motivo. ETags y registros append-only evitan pérdida silenciosa y preservan cierres/asignaciones.

La ampliación P05–P16 está propuesta en [ADR-0017](0017-source-coverage-and-process-governance.md): autorización institucional, suspensión, cierre documental independiente y cierre integral. Su [matriz objetivo](../requirements/domain-rules.md#proceso) requiere migración explícita desde los estados anteriores; no afirma que esos cambios estén desplegados.
