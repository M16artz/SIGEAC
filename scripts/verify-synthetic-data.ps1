$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$targets = @((Join-Path $root 'tests/fixtures'), (Join-Path $root 'tests/acceptance/results')) | Where-Object { Test-Path $_ }
$patterns = @('-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----', '(?i)@(unl\.edu\.ec|caces\.gob\.ec)')
$violations = @()
foreach ($target in $targets) {
    Get-ChildItem -LiteralPath $target -Recurse -File | ForEach-Object {
        foreach ($pattern in $patterns) {
            $violations += Select-String -LiteralPath $_.FullName -Pattern $pattern -ErrorAction SilentlyContinue
        }
    }
}
if ($violations.Count -gt 0) {
    $violations | ForEach-Object { Write-Error "$($_.Path):$($_.LineNumber): prohibited real-data/secret pattern" }
    exit 1
}
Write-Host 'Synthetic-data verification passed.'
