# D8/P15 — Planes de mejora y seguimiento

Fecha: 2026-10-25. Resultado: PASS técnico para T086–T093 y T123, incluido el
recorrido de navegador AC-P15-01/02/03 sobre implementación real. P15 queda
inserto en D8; la aceptación institucional, las competencias formales y el
bloque P16 (cierre y archivo) siguen pendientes, y esta hoja no declara D8
completo.

## Alcance implementado

- Planes de mejora (`improvement_plan`) inmutables, ligados a una versión de
  informe `FINAL`/`APPROVED` del mismo proceso y a la revisión previa de la
  cadena mediante `If-Match`. Sin versión final se responde
  `IMPROVEMENT_REPORT_NOT_FINAL`; el título vacío o la versión ajena se
  rechazan con `IMPROVEMENT_PLAN_INVALID` (422) y auditoría de rechazo.
- Acciones (`improvement_action` + `improvement_action_revision`) con
  hallazgos de origen obligatorios y verificados por proceso, responsable,
  plazo, meta y medio de verificación. Cada modificación crea una revisión
  nueva (`predecessor_revision`, trigger append-only) y devuelve la acción a
  `PROPOSED`; el vencimiento es derivado (`dueAt` pasado y estado distinto de
  `VERIFIED`) y nunca borra el incumplimiento anterior.
- Ciclo de vida separado: `PROPOSED → APPROVED → IN_PROGRESS →
  PENDING_VERIFICATION → VERIFIED`, con `RETURN` y `REQUEST_CORRECTION`.
  Aprobar/devolver/solicitar corrección exigen `IMPROVEMENT_DECIDE`; iniciar y
  enviar a verificación exigen `IMPROVEMENT_ACTION_EXECUTE`; verificar exige
  `IMPROVEMENT_ACTION_VERIFY`, de modo que quien ejecuta no puede declarar
  cumplimiento por sí mismo.
- Avances (`improvement_progress`) y decisiones (`improvement_decision`) son
  append-only, con ETag por revisión y auditoría de éxito/rechazo. Una acción
  `VERIFIED` responde `IMPROVEMENT_ACTION_CLOSED` antes de admitir avances o
  revisiones.
- Política `IMPROVEMENT_CONTINUATION_V1`: tras el cierre documental se bloquean
  planes y acciones nuevas, pero se permiten avances y transiciones desde
  estados aprobados/ejecutados mientras la política siga vigente.
- Autoridad institucional: la asignación que fundamenta una decisión debe
  pertenecer al actor, estar vigente y cubrir el proceso; las asignaciones
  ajenas, los actores técnicos y la falta de capacidad devuelven 403/404
  estables sin filtrar el recurso.
- UI `/improvement`: selección de proceso, creación de plan desde la versión
  emitida, propuesta de acción con hallazgos exactos, lista con vencimiento
  derivado, seguimiento con avances/decisiones inmutables, registro de avance,
  transición con asignación competente y revisión de responsable/plazo/meta.

## Verificación ejecutada

| Comprobación | Resultado |
|---|---|
| ImprovementActionTest | 4 casos PASS: ciclo de vida, transiciones inválidas, vencimiento y revisión |
| PostClosureImprovementTest | 7 casos PASS: capacidades separadas, política post-cierre, avances append-only, autoridad ajena, 404 encubierto, actor técnico y vencimiento |
| ImprovementControllerTest | 6 casos PASS: 201/ETag/no-store, CSRF, 428, ETag de lista, vista con avances/decisiones, validación 400, transición 409, revisión 412 y auditoría de rechazo |
| ImprovementPersistenceTest | 2 casos PASS sobre adaptadores JDBC reales (SQLite): cadena de revisiones 1→2→3, decisiones y avances inmutables, auditoría de éxito y cierre 409 |
| OpenApiContractTest | Rutas P15 y esquemas de plan/acción/avance/decisión validados en OpenAPI común |
| Suite backend completa | 533 pruebas, 0 fallos, 0 errores |
| Frontend | 103 pruebas PASS en 27 archivos; build de producción, lint y contratos PASS |
| Chromium/Edge headless (AC-P15-01/02/03) | 1 recorrido real PASS en 4,2 s, sin mocks HTTP |
| D8ImprovementData | Perfil `d8-improvement-test`: versión FINAL con predecesora, hallazgo `WEAKNESS` y asignaciones deterministas para responsable/revisor/coordinador |

La cadena del recorrido: creación del plan por interfaz con ETag `"0"`,
propuesta de acción ligada al hallazgo sintético, aprobación por coordinador
(ETag `"1"`), 409 al intentar verificar una acción aún no iniciada, inicio y
avance documentado del responsable (ETags `"2"`/`"3"`), envío a verificación
(ETag `"4"`), verificación del revisor (ETag `"5"`) y cierre 409 ante un
avance posterior. Una segunda acción con plazo vencido se muestra como
“Vencida sin verificación”; su revisión (ETag `"1"` → revisión 2) extiende el
plazo sin borrar la revisión previa ni el incumplimiento registrado. La captura
`tests/acceptance/results/playwright-artifacts/d8-improvement/improvement-verified.png`
es artefacto generado ignorado por Git.

## Reproducción

Backend, desde `backend` (base SQLite nueva por recorrido):

```powershell
.\mvnw.cmd -q test
$env:SEC_SQLITE_PATH = Join-Path $env:TEMP "d8-improvement-$([guid]::NewGuid()).sqlite"
$env:SEC_TEST_FILE_ROOT = Join-Path $env:TEMP "d8-improvement-files-$([guid]::NewGuid())"
$d8TestClasses = Join-Path (Get-Location) 'target\test-classes'
.\mvnw.cmd -q spring-boot:run '-Dspring-boot.run.profiles=sqlite-test,d8-improvement-test' '-Dspring-boot.run.useTestClasspath' "-Dspring-boot.run.additional-classpath-elements=$d8TestClasses" '-Dspring-boot.run.jvmArguments=-Dserver.port=18086'
```

En otra terminal, desde `frontend`:

```powershell
npm.cmd start -- --host 127.0.0.1 --port 14206 --proxy-config proxy.d6.conf.json
```

Con ambos servidores listos, desde `frontend`:

```powershell
npm.cmd run test:d8:improvement
```

`D8ImprovementData` existe únicamente en el classpath de pruebas con el perfil
explícito `d8-improvement-test`; siembra versión final, hallazgo y asignaciones
sintéticas sin conceder autoridad a cuentas de producción.

## Límites y siguiente bloque

El contrato no expone `GET /improvement-plans`, por lo que la interfaz mantiene
los planes creados en la sesión y solicita la revisión previa de la cadena como
entrada numérica; la historia de revisiones no tiene endpoint de lectura y se
verifica en pruebas de persistencia. La aceptación formal de competencias
institucionales, la campaña de rendimiento/accesibilidad y el recorrido P16
(cierre integral y archivo, T094/T095/T124) quedan fuera de esta hoja.
