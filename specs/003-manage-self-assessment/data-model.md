# Data Model: I2

- **SelfAssessmentProcess**: UUID, carrera, código `1..100`, nombre `1..300`, período `[start,end]`, modelVersionId/número, estado `DRAFT|ACTIVE|CLOSING|CLOSED|REOPENED`, revisión y fechas.
- **ProcessIndicator**: processId, indicatorId, código/nombre copiados desde la versión fijada; inmutable después de activar.
- **ProcessAssignment**: UUID, processId, indicatorId, identityId, función `RESPONSIBLE|REVIEWER|EVALUATOR`, `validFrom < validTo`, createdBy, reason opcional/obligatorio si crítico, revokedAt, revisión.
- **ProcessTransition**: UUID, processId, from/to, actor, fecha, motivo, correlación y revisión resultante; append-only.
- **ProcessClosureReference**: identifica cada cierre previo y se conserva tras reapertura.

Invariantes: versión publicada y visible; período válido; indicador pertenece a instantánea; vigencia solapa período; proceso cerrado no muta; modelo fijado nunca cambia tras activación; ETag requerido en mutaciones.
