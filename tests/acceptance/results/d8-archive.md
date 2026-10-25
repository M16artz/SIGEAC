# D8/P16 - Cierre integral, archivo y reapertura controlada

Fecha: 2026-10-25. Resultado: PASS técnico para T094, T095, T096-T102 y T124,
incluido el recorrido de navegador AC-P16-01/02/03 sobre implementación real.
P16 queda inserto en D8; la aceptación institucional y la campaña formal de
operación siguen pendientes.

## Alcance implementado

- Migración `V026__integral_closure_archive.sql`: reemplaza el andamiaje V020
  con `closure_policy`, `integral_closure` y `archive_record`. Las listas de
  identificadores y el payload se conservan como texto para que el SHA-256 no
  cambie por la normalización de JSON de PostgreSQL.
- Política `INTEGRAL_STRICT_V1`: el cierre integral exige proceso en
  `DOCUMENTARY_CLOSED`, cierre documental conservado, último informe final
  aprobado y ausencia de acciones vencidas salvo excepción autorizada.
  Las acciones abiertas no vencidas continúan su seguimiento.
- Procedencia institucional: el cierre y el archivo exigen una asignación
  vigente del actor para el proceso y la capacidad correspondiente
  (`PROCESS_INTEGRAL_CLOSE` / `ARCHIVE_WRITE`). Las excepciones deben referenciar
  decisiones `process_authorization` existentes del mismo proceso.
- Cierre: bloquea con 409 `CLOSURE_BLOCKED`, 409 `CLOSURE_ALREADY_EXISTS` o
  409 `CLOSURE_STATE_INVALID`; sella payload canónico con SHA-256, transiciona
  `DOCUMENTARY_CLOSED -> CLOSING -> CLOSED`, registra la transición y audita
  `INTEGRAL_CLOSURE_CREATED`.
- Archivo: exige proceso `CLOSED`, recalcula el SHA-256 del payload y responde
  409 `ARCHIVE_INTEGRITY_FAILED` si no coincide, 409 `ARCHIVE_ALREADY_EXISTS`
  ante duplicados y audita `PROCESS_ARCHIVED`.
- Historial: `GET /closures` combina cierres documentales e integrales con
  orden descendente, paginación máxima de 100 y ETag del total de elementos.
- Reapertura: reutiliza la transición controlada existente a `REOPENED`; no
  borra cierres ni archivo. Un nuevo cierre desde `REOPENED` responde
  `CLOSURE_STATE_INVALID`.
- UI `/archive`: selección de proceso, previsualización de bloqueadores,
  cierre integral con excepciones y procedencia, historial con SHA-256 y archivo
  del cierre integral seleccionado.
- Fixture `D9ArchiveData` (`d9-archive-test`): informe final aprobado, cierre
  documental sintético conservado, asignación `PROCESS_AUTHORITY` y decisión de
  excepción deterministas.

## Verificación ejecutada

| Comprobación | Resultado |
|---|---|
| IntegralClosurePolicyTest | 5 casos PASS: elegibilidad, bloqueadores, excepción, acciones abiertas y política |
| ArchiveServiceTest | 5 casos PASS: autoridad, integridad, duplicado, estado y capacidades |
| ClosureReopeningPolicyTest | 3 casos PASS: conservación del historial y cierre tras reapertura |
| OpenApiContractTest | Rutas P16 de previsualización, cierre, archivo e historial publicadas en el contrato común |
| Suite backend completa | 544 pruebas, 0 fallos, 0 errores, 15 omitidas, 127 reportes |
| Frontend | 103 pruebas PASS en 27 archivos; build de producción y lint PASS |
| Chromium/Edge headless (AC-P16-01/02/03) | 1 recorrido real PASS en 2,6 s, sin mocks HTTP |

La cadena del recorrido: previsualización elegible con ETag `"2"`; creación de
plan y acción con plazo vencido; la previsualización vuelve a informar el
bloqueador de acción vencida y el cierre sin excepción responde 409
`CLOSURE_BLOCKED`. El cierre se ejecuta desde la UI con la decisión de excepción
`d9000000-0000-0000-0000-000000000920`; el historial devuelve dos cierres y el
integral con SHA-256 válido. El archivo del cierre devuelve integridad
`VERIFIED`, un segundo archivo responde 409 `ARCHIVE_ALREADY_EXISTS`, y la
reapertura controlada conserva el mismo historial y SHA-256. La captura
`tests/acceptance/results/playwright-artifacts/d8-archive/archive-history.png`
es un artefacto generado ignorado por Git.

Durante la aceptación se detectó y corrigió que `JdbcClosureEvidenceAdapter`
debía aceptar `dueAt` serializado como segundos epoch por el `ObjectMapper`
global, además de ISO-8601. Sin esta corrección las acciones vencidas no
aparecían en la previsualización del cierre integral.

## Reproducción

Backend, desde `backend` (base SQLite nueva por recorrido):

```powershell
.\mvnw.cmd -q test
$env:SEC_SQLITE_PATH = Join-Path $env:TEMP "d8-archive-$([guid]::NewGuid()).sqlite"
$env:SEC_TEST_FILE_ROOT = Join-Path $env:TEMP "d8-archive-files-$([guid]::NewGuid())"
$d8TestClasses = Join-Path (Get-Location) 'target\test-classes'
.\mvnw.cmd -q spring-boot:run '-Dspring-boot.run.profiles=sqlite-test,d9-archive-test' '-Dspring-boot.run.useTestClasspath' "-Dspring-boot.run.additional-classpath-elements=$d8TestClasses" '-Dspring-boot.run.jvmArguments=-Dserver.port=18086'
```

En otra terminal, desde `frontend`:

```powershell
npm.cmd start -- --host 127.0.0.1 --port 14206 --proxy-config proxy.d6.conf.json
```

Con ambos servidores listos, desde `frontend`:

```powershell
npm.cmd run test:d8:archive
```

`D9ArchiveData` existe únicamente en el classpath de pruebas con el perfil
explícito `d9-archive-test`; siembra cierres, excepción y autoridad sintéticas
sin conceder autoridad a cuentas de producción.

## Límites

La previsualización documental es informativa y revalida la cobertura al
ejecutar el cierre documental. La interfaz no expone un endpoint de consulta del
registro de archivo; la evidencia de archivo se acredita por el resultado de la
operación y la conservación del cierre en el historial. La aceptación formal de
competencias institucionales y las campañas de rendimiento y accesibilidad
quedan fuera de esta hoja.
