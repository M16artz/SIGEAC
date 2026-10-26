param(
  [Parameter(Mandatory=$true)][int]$Port,
  [Parameter(Mandatory=$true)][string]$DatabaseUser,
  [Parameter(Mandatory=$true)][string]$Database,
  [Parameter(Mandatory=$true)][string]$FileRoot,
  [string]$PostgresBin = 'C:/Program Files/PostgreSQL/18/bin'
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$work = Join-Path ([IO.Path]::GetTempPath()) ('sec-restore-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $work | Out-Null
$restoredDatabase = 'sec_restore_' + [guid]::NewGuid().ToString('N')
$connection = @('-h','127.0.0.1','-p',"$Port",'-U',$DatabaseUser)
$dump = Join-Path $work 'synthetic.dump'
& (Join-Path $PostgresBin 'pg_dump.exe') @connection -Fc -d $Database -f $dump
if ($LASTEXITCODE -ne 0) { throw 'pg_dump failed' }
& (Join-Path $PostgresBin 'createdb.exe') @connection $restoredDatabase
if ($LASTEXITCODE -ne 0) { throw 'createdb failed' }
& (Join-Path $PostgresBin 'pg_restore.exe') @connection -d $restoredDatabase --exit-on-error $dump
if ($LASTEXITCODE -ne 0) { throw 'pg_restore failed' }

function Query([string]$db, [string]$sql) {
  $result = & (Join-Path $PostgresBin 'psql.exe') @connection -X -q -t -A -d $db -c $sql
  if ($LASTEXITCODE -ne 0) { throw 'verification query failed' }
  return ($result -join "`n")
}
function Digest([string]$value) {
  $sha = [Security.Cryptography.SHA256]::Create()
  try { return ([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($value)))).Replace('-','').ToLowerInvariant() }
  finally { $sha.Dispose() }
}
$tables = @('self_assessment_process','model_version','evidence_version','source_coverage',
  'document_review_decision','manual_academic_evaluation','quantitative_calculation',
  'governance_report_version','governance_report_decision','improvement_action','improvement_progress',
  'integral_closure','archive_record','audit_event')
$rows = foreach ($table in $tables) {
  # Sorted complete rows compare exact references, revisions, JSON bytes, decisions and hashes.
  $sql = "select row_to_json(t)::text from $table t order by row_to_json(t)::text"
  $before = Query $Database $sql
  $after = Query $restoredDatabase $sql
  if ($before -cne $after) { throw "Restored rows differ: $table" }
  $count = Query $Database "select count(*) from $table"
  if ([long]$count -eq 0) { throw "No synthetic acceptance coverage in $table" }
  '| ' + $table + ' | ' + $count + ' | ' + (Digest $before) + ' | PASS |'
}
$sourceRoot = (Resolve-Path -LiteralPath $FileRoot).Path
$files = @(Get-ChildItem -LiteralPath $sourceRoot -Recurse -File)
if ($files.Count -eq 0) { throw 'No synthetic files to restore' }
$copyRoot = Join-Path $work 'files'
New-Item -ItemType Directory -Path $copyRoot | Out-Null
$fileRows = foreach ($file in $files) {
  $relative = $file.FullName.Substring($sourceRoot.Length).TrimStart('\','/')
  $destination = Join-Path $copyRoot $relative
  New-Item -ItemType Directory -Path (Split-Path $destination -Parent) -Force | Out-Null
  Copy-Item -LiteralPath $file.FullName -Destination $destination
  $before = (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash
  $after = (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash
  if ($before -ne $after) { throw "Restored file differs: $relative" }
  "$relative $before"
}
$report = @(
  '# Respaldo y restauracion conjunta P01-P16', '',
  ('Fecha UTC: ' + [DateTime]::UtcNow.ToString('o') + '. Resultado: **PASS**. Solo datos sinteticos.'), '',
  'PostgreSQL 18: pg_dump -Fc, creacion de base independiente, pg_restore --exit-on-error y consulta de filas completas.',
  'Se comparan datos de origen/restaurados sin modificar la base de origen ni reescribir hashes.', '',
  '| Tabla | Filas | SHA-256 de filas completas ordenadas | Resultado |', '|---|---:|---|---|'
) + $rows + @('',
  ('Archivos copiados y verificados individualmente: **' + $files.Count + '**.'),
  ('SHA-256 del manifiesto de rutas relativas y hashes: `' + (Digest ($fileRows -join "`n")) + '`.'),
  ('SHA-256 del dump: `' + (Get-FileHash -LiteralPath $dump -Algorithm SHA256).Hash.ToLowerInvariant() + '`.'), '',
  'Alcance: restauracion local descartable y comparacion integra de datos/archivos. No mide RPO/RTO ni sustituye una politica institucional de retencion.'
)
$report | Set-Content -LiteralPath (Join-Path $root 'tests/acceptance/results/p01-p16-restore.md') -Encoding UTF8
@{database=$restoredDatabase; fileRoot=$copyRoot; dump=$dump} | ConvertTo-Json |
  Set-Content -LiteralPath (Join-Path $root 'backend/target/restored-environment.json') -Encoding UTF8
Write-Output ('PASS: restored ' + $tables.Count + ' tables and ' + $files.Count + ' files into disposable storage.')
