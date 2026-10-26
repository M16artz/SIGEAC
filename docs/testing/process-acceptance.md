# Matriz de aceptación por proceso

**Estado**: catálogo de escenarios; ejecución técnica consolidada completa en
[resultados P01–P16](../../tests/acceptance/results/process-acceptance.md).
**Fecha**: 2026-10-21.  
**Referencia**: [reglas de dominio](../requirements/domain-rules.md), [plan](../../specs/005-complete-evaluation-workflows/plan.md).

Los IDs AC-Pxx-01/02/03 corresponden respectivamente al recorrido principal,
rechazo y recuperación/alternativa. La suite `complete-workflows.spec.ts`
conserva los 64 IDs: 64 PASS, 0 FAIL y 0 SKIPPED en la ejecución de 2026-10-25.
Cada PASS ejecuta su recorrido con datos sintéticos sobre la API real;
la revisión humana T111 sigue pendiente. Los criterios AC-X01–AC-X16 complementan
estos recorridos. Este catálogo define el alcance; el informe enlazado acredita
lo ejecutado y no constituye aprobación institucional.

## P01. Autenticación y usuarios

**Historias**: specs/001-identity-access-scopes/spec.md US1–US5. Los números 005 y 004 se refieren a los directorios bajo `specs/`.

- **AC-P01-01**. **Dado** identidad activa con credenciales válidas, **cuando** inicia sesión y consulta su ámbito, **entonces** sesión revocable y solo funciones vigentes.
- **AC-P01-02**. **Dado** credenciales erróneas, cuenta deshabilitada o usuario desconocido, **cuando** intenta autenticarse, **entonces** rechazo equivalente sin enumerar cuentas ni permitir acceso.
- **AC-P01-03**. **Dado** asignación revocada con sesión abierta, **cuando** reintenta una operación protegida, **entonces** backend reautoriza, rechaza y conserva historial/auditoría.

## P02. Carreras y permisos

**Historias**: 005 US1. Los números 005 y 004 se refieren a los directorios bajo `specs/`.

- **AC-P02-01**. **Dado** administrador autorizado y unidad activa, **cuando** registra carrera con código único, **entonces** carrera disponible con unidad y ámbitos explícitos.
- **AC-P02-02**. **Dado** código repetido o unidad ajena, **cuando** intenta guardar carrera, **entonces** rechazo sin duplicados ni exposición de ámbitos ajenos.
- **AC-P02-03**. **Dado** carrera utilizada en un proceso, **cuando** solicita eliminación o cambio de responsable, **entonces** impedir eliminación física; desactivación/reasignación conserva referencias históricas.

## P03. Modelos y versiones

**Historias**: 005 US2. Los números 005 y 004 se refieren a los directorios bajo `specs/`.

- **AC-P03-01**. **Dado** fuente normativa identificada y borrador completo, **cuando** revisor distinto publica la versión, **entonces** snapshot con procedencia; la aplicabilidad no se presume.
- **AC-P03-02**. **Dado** documento incompleto o versión sin metodología verificable, **cuando** solicita publicación, **entonces** rechazo de publicación; borrador conservado con problemas específicos.
- **AC-P03-03**. **Dado** modelo utilizado y documento reemplazado, **cuando** crea versión sucesora, **entonces** histórico y procesos previos conservan versión exacta.

## P04. Indicadores, estándares y fuentes

**Historias**: 005 US2. Los números 005 y 004 se refieren a los directorios bajo `specs/`.

- **AC-P04-01**. **Dado** dos modelos con distinta profundidad, **cuando** configura indicador directo al criterio y otro anidado, **entonces** ambos válidos con fuentes, estándar y período propios.
- **AC-P04-02**. **Dado** ciclo, código duplicado o regla sin validación, **cuando** solicita publicar o calcular, **entonces** rechazar estructura inválida; regla no soportada conserva definición y no calcula.
- **AC-P04-03**. **Dado** fuente o período ambiguo, **cuando** registra aclaración metodológica, **entonces** mantener pendiente hasta resolver con procedencia y revisión.

## P05. Crear y autorizar proceso

**Historias**: 005 US3. Los números 005 y 004 se refieren a los directorios bajo `specs/`.

- **AC-P05-01**. **Dado** carrera y versión aplicables, autorización verificada, **cuando** activa el proceso, **entonces** fijar modelo y registrar autoridad/fecha.
- **AC-P05-02**. **Dado** sin autorización, versión borrador o modelo institucional no aplicable, **cuando** intenta activar, **entonces** proceso permanece en preparación, sin cambio parcial.
- **AC-P05-03**. **Dado** proceso activo suspendido con motivo, **cuando** autoridad registra reanudación, **entonces** reanudar según política conservando decisiones y plazos; no sustituir modelo.

## P06. Responsables y planificación

**Historias**: 005 US3. Los números 005 y 004 se refieren a los directorios bajo `specs/`.

- **AC-P06-01**. **Dado** participantes y equipos vigentes, **cuando** asigna indicadores, fuentes y actividades con fecha, **entonces** tareas visibles solo para asignados y membresías verificadas.
- **AC-P06-02**. **Dado** autor y revisor coincidentes sin excepción o dependencia circular de tareas, **cuando** confirma asignación, **entonces** rechazar conflicto y mantener planificación anterior.
- **AC-P06-03**. **Dado** responsable no disponible o actividad vencida, **cuando** reasigna o modifica fecha, **entonces** nueva revisión, vencimiento/historial preservados; permisos efectivos reevaluados.

## P07. Registro de evidencias

**Historias**: 005 US4 y 004 US1–US2. Los números 005 y 004 se refieren a los directorios bajo `specs/`.

- **AC-P07-01**. **Dado** proceso activo y fuente requerida asignada, **cuando** registra archivo, enlace, dato o referencia, **entonces** borrador con cobertura explícita y metadatos/período.
- **AC-P07-02**. **Dado** archivo excedido, url inválida o fuente ajena, **cuando** intenta guardar, **entonces** rechazo seguro sin fuente falsa disponible.
- **AC-P07-03**. **Dado** falla almacenamiento o encuentra archivo repetido, **cuando** reintenta o declara reutilización autorizada, **entonces** recuperación consistente; evidencia reutilizada no duplica el denominador.

## P08. Remisión

**Historias**: 004 US3 y 005 US4. Los números 005 y 004 se refieren a los directorios bajo `specs/`.

- **AC-P08-01**. **Dado** borrador completo con fuente y contenido validables, **cuando** remite versión, **entonces** snapshot inmutable, autor/fecha y bandeja de revisión.
- **AC-P08-02**. **Dado** campos faltantes, recepción cerrada o responsable vencido, **cuando** intenta remitir, **entonces** bloquear envío, conservar borrador y problemas corregibles.
- **AC-P08-03**. **Dado** dos remisiones concurrentes del mismo borrador, **cuando** confirman con misma revisión, **entonces** una sola remisión; otra recibe conflicto y no duplica versión.

## P09. Revisión documental

**Historias**: 005 US5. Los números 005 y 004 se refieren a los directorios bajo `specs/`.

- **AC-P09-01**. **Dado** revisor autorizado y versión con dos coberturas, **cuando** acepta una y observa la otra, **entonces** decisiones por cobertura, global observado; solo cobertura aceptada suma documentalmente.
- **AC-P09-02**. **Dado** revisor es autor sin excepción o criterio documental indefinido, **cuando** intenta aceptar, **entonces** bloquear autorrevisión o dejar pendiente aclaración; nunca inferir aceptación.
- **AC-P09-03**. **Dado** decisión vigente requiere rectificación autorizada, **cuando** registra sucesora con motivo, **entonces** conservar decisión anterior, actualizar proyección y señalar valoraciones afectadas.

## P10. Subsanación

**Historias**: 004 US4 y 005 US5. Los números 005 y 004 se refieren a los directorios bajo `specs/`.

- **AC-P10-01**. **Dado** versión observada o rechazada, **cuando** responsable corrige y remite sucesora, **entonces** predecesora intacta, vínculo a decisión y nueva revisión requerida.
- **AC-P10-02**. **Dado** plazo vencido, evidencia archivada o cambio basado en revisión obsoleta, **cuando** intenta corregir, **entonces** denegar o requerir excepción/reapertura; no modificar histórico.
- **AC-P10-03**. **Dado** borrador v2 nace de v1 aceptada y luego se remite, **cuando** consulta métricas antes y después, **entonces** v1 suma hasta remitir v2; después no heredar aceptación a v2.

## P11. Seguimiento

**Historias**: 005 US6. Los números 005 y 004 se refieren a los directorios bajo `specs/`.

- **AC-P11-01**. **Dado** cuatro fuentes aplicables, tres remitidas y dos aceptadas, **cuando** consulta tablero, **entonces** 75 % recopilación, 50 % documental, académico separado y detalle navegable.
- **AC-P11-02**. **Dado** sin fuentes configuradas o todas excluidas válidamente, **cuando** calcula porcentajes, **entonces** null con NOT_CONFIGURED o NOT_APPLICABLE; ninguna división por cero.
- **AC-P11-03**. **Dado** duplicados, cardinalidad > 1 o aceptación rectificada, **cuando** recalcula al mismo corte, **entonces** fuentes distintas y cardinalidad correcta; snapshots anteriores invariables.

## P12. Valoración académica

**Historias**: 005 US7. Los números 005 y 004 se refieren a los directorios bajo `specs/`.

- **AC-P12-01**. **Dado** indicador cualitativo o cuantitativo con metodología válida, **cuando** registra juicio o calcula con variables verificadas, **entonces** conservar escala/fórmula, entradas, sustentos, autor y resultado.
- **AC-P12-02**. **Dado** variable faltante, unidad incompatible, escala ajena o regla no validada, **cuando** solicita finalizar, **entonces** rechazar resultado automático; guardar pendiente/limitación según método.
- **AC-P12-03**. **Dado** evaluadores discrepan o evidencia pierde aceptación, **cuando** solicita consolidación, **entonces** conservar discrepancia y marcar revisión; resolución posterior no borra valoraciones previas.

## P13. Consolidación y hallazgos

**Historias**: 005 US8. Los números 005 y 004 se refieren a los directorios bajo `specs/`.

- **AC-P13-01**. **Dado** valoraciones y pendientes identificados, **cuando** comisión consolida y registra fortalezas/debilidades, **entonces** hallazgos vinculados a indicadores, resultados y evidencias exactas.
- **AC-P13-02**. **Dado** metodología sin regla de agregación, **cuando** solicita porcentaje global, **entonces** no producir promedio arbitrario; mostrar resultados por indicador.
- **AC-P13-03**. **Dado** valoración rectificada o períodos distintos, **cuando** crea nueva consolidación, **entonces** conservar versión previa, separar referencias temporales y pendientes.

## P14. Informes y aprobación

**Historias**: 005 US8. Los números 005 y 004 se refieren a los directorios bajo `specs/`.

- **AC-P14-01**. **Dado** consolidación disponible, **cuando** emite preliminar, socializa, corrige y tramita final, **entonces** historial completo con aprobación de versión exacta por autoridad competente.
- **AC-P14-02**. **Dado** aprobación pendiente, devolución o plantilla institucional no validada, **cuando** consulta informe, **entonces** no presentar aprobación ni validez oficial inexistentes.
- **AC-P14-03**. **Dado** observación posterior o cambio tras aprobación, **cuando** emite corrección, **entonces** crear versión sucesora con nueva tramitación; contenido aprobado anterior intacto.

## P15. Planes de mejora

**Historias**: 005 US9. Los números 005 y 004 se refieren a los directorios bajo `specs/`.

- **AC-P15-01**. **Dado** hallazgo y responsable autorizados, **cuando** propone, aprueba y ejecuta acción con meta/plazo/medio, **entonces** avances y verificación trazables a la evaluación.
- **AC-P15-02**. **Dado** acción no aprobada o verificación insuficiente, **cuando** intenta darla por cumplida, **entonces** mantener propuesta o solicitar corrección; no marcar verificada.
- **AC-P15-03**. **Dado** cambio de responsable/plazo o vencimiento, **cuando** consulta historial y avance, **entonces** nueva revisión auditada y marca vencida derivada; no borrar incumplimiento anterior.

## P16. Cierre y archivo

**Historias**: 005 US10 y US6 documental. Los números 005 y 004 se refieren a los directorios bajo `specs/`.

- **AC-P16-01**. **Dado** política, autorizaciones y consistencia verificadas, **cuando** confirma cierre/archivo, **entonces** snapshot íntegro y consulta histórica por ámbito.
- **AC-P16-02**. **Dado** pendiente obligatorio o archivo perdido/corrupto, **cuando** intenta cerrar, **entonces** bloquear cierre y mostrar causa; excepción solo si política autoriza.
- **AC-P16-03**. **Dado** proceso cerrado requiere corrección o nueva evaluación, **cuando** autoriza rectificación o crea proceso distinto, **entonces** preservar snapshot, modelo y aprobaciones; registrar nueva cadena histórica.

## Registro de resultados

| Campo | Contenido requerido |
|---|---|
| Escenario | ID AC-Pxx o AC-Xxx y FR/RNF/SC relacionados |
| Preparación | Estado inicial, fuente/modelo/período y asignaciones sintéticas |
| Ejecución | Comando o pasos, fecha, revisión y entorno |
| Resultado | Esperado, obtenido y PASS/FAIL/SKIPPED con motivo |
| Evidencia | Referencias a logs sanitizados, eventos, conteos o hashes |
| Competencias | Política aplicada; asuntos institucionales pendientes se indican expresamente |

La aceptación de D1–D6 exige ejecutar P01–P11 y el recorrido documental P16 con P12–P15 deshabilitados. D7–D8 añade los demás recorridos; la aprobación institucional requiere además resolver las decisiones de competencia aplicables.

