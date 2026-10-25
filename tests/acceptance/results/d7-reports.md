# D7/P13–P14 — Hallazgos, consolidación y ciclo del informe

Fecha: 2026-10-25. Resultado: PASS técnico para T077–T085 y T122, incluido el
recorrido de navegador AC-X12 sobre implementación real. P13 y P14 quedan
insertos en D7; la aceptación institucional, la campaña de rendimiento y los
bloques T111/T123/T124–T132 siguen pendientes, y esta hoja no declara D7 completo.

## Alcance implementado

- Hallazgos `STRENGTH`/`WEAKNESS`/`OBSERVATION` inmutables con soporte exacto de
  indicadores, evaluaciones y versiones de evidencia; incapaces de alimentar
  calificaciones y sin agregación arbitraria. `FINDING_WRITE` y `FINDING_READ`
  se exigen por capacidad y ámbito; el actor ajeno recibe 404 encubierto y el
  registro se audita tras rollback.
- Consolidación académica append-only: cadena única por proceso con
  `If-Match` sobre la revisión previa, predecesora, hallazgos registrados no
  consolidados y regla `VALUATED` obligatoria cuando se declara
  `aggregationRuleId`. Duplicados y hallazgos ajenos se rechazan con códigos
  estables antes de escribir.
- Emisión de versiones: `PRELIMINARY` exige consolidación previa y plantilla
  validada; `FINAL` exige predecesora encadenada y la revisión vigente de la
  cadena (`If-Match`). El contenido emitido es inmutable: sin
  `UPDATE`/`DELETE` en SQLite ni PostgreSQL, trigger es guardián de la fila y
  la corrección sólo crea una versión nueva con su propio `contentSha256`.
- Tramitación: socialización con acta sintética, observación del revisor con
  sección y texto, respuesta única por observación con enlace opcional a la
  versión de corrección, y decisión institucional `APPROVED`/`RETURNED` que
  exige versión `FINAL`, asignación vigente con `REPORT_APPROVE`, base legal y
  constancia documental. La cadena cierra tras la decisión y admite 403
  infundados con auditoría de rechazo.
- UI `/reports`: selección de proceso, hallazgos con soportes exactos,
  consolidación con revisión previa, emisión del preliminar, lectura de la
  versión con eventos (ETag/revisión de tramitación), socialización,
  observación, respuesta, emisión final encadenada y decisión institucional.
  La página degrada con gracia cuando una función carece de `FINDING_READ`
  (el comité sólo ve el ciclo del informe) y conserva el aviso de que no
  constituye acreditación.
- Datos sintéticos de prueba: `D7ReportWorkflowData` (clase en `support/`,
  perfil `d7-report-test`) extiende la siembra D7 con el comité
  `REPORT_COMMITTEE` y la asignación `COMMITTEE_ASSIGNMENT` utan conceder
  autoridad a cuentas de producción.

## Verificación ejecutada

| Comprobación | Resultado |
|---|---|
| FindingTest | 10 casos PASS sobre dominio real: soportes exigidos, inmutabilidad del payload, apoyos válidos |
| FindingServiceTest | 8 casos PASS: capacidades, ámbito, consolidación encadenada, regla validada y auditoría |
| ReportWorkflowServiceTest | 12 casos PASS sobre servicios de aplicación: emisión, Is-Match, observación, respuesta, decisión y no-acreditación |
| ReportControllerTest | 9 casos PASS: CSRF, If-Match fuerte/ausente, 428, 412 `STALE_WORKFLOW`, 409, ETags por revisión y auditoría de rechazos |
| IssuedReportImmutabilityTest | 2 casos PASS sobre adaptadores JDBC reales (SQLite): payload/`contentSha256` inmutables y predecesora encadenada |
| OpenApiContractTest | P13/P14 añadidos: 8 referencias de rutas y tag `Reports` validados en OpenAPI común |
| Suite backend completa | 517 pruebas, 0 fallos, 0 errores, 15 omitidas |
| Frontend | 95 pruebas PASS en 25 archivos; build de producción y lint PASS |
| Contratos | OpenAPI común/005 validados; rutas de informes fusionadas sin incompatibilidades |
| Chromium/Edge headless (AC-X12) | 1 recorrido real PASS en 6,6 s, sin mocks HTTP |
| verify-synthetic-data | PASS sin patrones de datos reales ni claves |

La cadena del recorrido: registro de hallazgo por API con ETag `"0"`,
404 encubierto para responsable sin capacidades, 428 sin `If-Match`,
hallazgo por interfaz con soporte exacto del indicador, 412 por revisión
obsoleta de la cadena, consolidación doble rechazada 409, emisión del
preliminar con ETag `"0"`, 404 para el comité que carece de
`REPORT_SOCIALIZE`, socialización/observación/respuesta con ETags
`"1"`/`"2"`/`"3"`, preliminar byte-a-byte idéntico tras la emisión final,
403 para coordinador sin asignación de autoridad, decisión `APPROVED` desde
la interfaz por el presidente y cierre de la cadena 409. La base sintética de
navegador registra dos hallazgos, una consolidación y tres rechazos auditados
(finding/consolidación/decisión) sin 500 niórrores internos. La captura
`tests/acceptance/results/playwright-artifacts/d7-reports/report-approved.png`
es artefacto generado ignorado por Git.

`governance_report_processing` conserva la revisión de tramitación por versión
con el If-Match correspondiente; `state=coalesce(?,state)` evita sobrescribir estados;
trigger SQLite impide actualización/borrado sobre `governance_report_version`
y borrado sobre procesamiento. PostgreSQL se verificó antes mediante el
patrón de persistencia opt-in; la regresión opt-in para V024 completa queda
pendiente de la campaña D8 e informe final.

## Reproducción

Backend, desde `backend` (base SQLite nueva por recorrido):

```powershell
.\mvnw.cmd -q test
$env:SEC_SQLITE_PATH = Join-Path $env:TEMP "d7-reports-$([guid]::NewGuid()).sqlite"
$env:SEC_TEST_FILE_ROOT = Join-Path $env:TEMP "d7-reports-files-$([guid]::NewGuid())"
$d7TestClasses = Join-Path (Get-Location) 'target\test-classes'
.\mvnw.cmd -q spring-boot:run '-Dspring-boot.run.profiles=sqlite-test,d7-report-test' '-Dspring-boot.run.useTestClasspath' "-Dspring-boot.run.additional-classpath-elements=$d7TestClasses" '-Dspring-boot.run.jvmArguments=-Dserver.port=18086'
```

En otra terminal, desde `frontend`:

```powershell
npm.cmd start -- --host 127.0.0.1 --port 14206 --proxy-config proxy.d6.conf.json
```

Con ambos servidores listos, desde otra terminal `frontend`:

```powershell
npm.cmd run test:d7:reports
```

`D7ReportWorkflowData` existe únicamente en el classpath de pruebas con el
perfil explícito `d7-report-test`. Siembra modelo publicado, proceso
sintético, usuarios y asignaciones vía perfil; no acredita su uso
institucional. Hallazgos, consolidación, emisión, socialización, observación,
respuesta, decisión y 403/404/409/412/428 se ejecutan por API pública o por la
interfaz Angular real. El navegador usa SQLite auxiliar; PostgreSQL se
comprueba por separado en pruebas de persistencia/migración.

## Límites y siguiente bloque

`REPORT_REVIEW` queda sembrada pero aún no se usa en estados; el prepago de
descargas/archivos de la U8 pertenece a otro bloque (T111/T123/T124–T132).
No se declara la parte D7 de mejora, cierre integral ni archivo; los errores
auditados de 403 se verifican a nivel de controlador y la
auditoría de éxito con rollback compartido se verificó en pruebas
transaccionales del módulo académico (el mismo patrón `failureAfterRollback`
se aplica aquí). La campaña definitiva de rendimiento/accesibilidad y la
aceptación institucional siguen fuera del alcance de esta hoja.
