$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$nodeScript = Join-Path $PSScriptRoot 'validate-contracts.mjs'
Push-Location (Join-Path $root 'frontend')
try {
    node $nodeScript
    if ($LASTEXITCODE -ne 0) { throw "Contract validation failed with exit code $LASTEXITCODE" }
} finally { Pop-Location }
