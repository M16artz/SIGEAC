[CmdletBinding()]
param(
    [string]$OutputDirectory
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $OutputDirectory = Join-Path $PSScriptRoot '..\tests\fixtures\pilot\evidence\generated'
}
$resolvedOutput = [System.IO.Path]::GetFullPath($OutputDirectory)
[System.IO.Directory]::CreateDirectory($resolvedOutput) | Out-Null

function Write-PatternFile([string]$Name, [long]$Length) {
    $path = Join-Path $resolvedOutput $Name
    $buffer = [byte[]]::new(65536)
    for ($index = 0; $index -lt $buffer.Length; $index++) { $buffer[$index] = $index % 251 }
    $stream = [System.IO.File]::Open($path, [System.IO.FileMode]::Create, [System.IO.FileAccess]::Write)
    try {
        $remaining = $Length
        while ($remaining -gt 0) {
            $count = [int][Math]::Min($buffer.Length, $remaining)
            $stream.Write($buffer, 0, $count)
            $remaining -= $count
        }
    } finally { $stream.Dispose() }
    Get-FileHash -Algorithm SHA256 -LiteralPath $path
}

$small = Join-Path $resolvedOutput 'synthetic-evidence.txt'
[System.IO.File]::WriteAllText($small, "SEC-UNL synthetic evidence fixture`nNo institutional data.`n", [System.Text.UTF8Encoding]::new($false))
Get-FileHash -Algorithm SHA256 -LiteralPath $small
Write-PatternFile 'exact-25-mib.bin' 26214400
Write-PatternFile 'over-25-mib.bin' 26214401
$mismatch = Join-Path $resolvedOutput 'declared-pdf-mismatch.pdf'
[System.IO.File]::WriteAllText($mismatch, 'synthetic plain text intentionally declared as PDF', [System.Text.UTF8Encoding]::new($false))
Get-FileHash -Algorithm SHA256 -LiteralPath $mismatch
