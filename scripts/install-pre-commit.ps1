[CmdletBinding()]
param(
    [switch]$Force
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")
$gitDir = Join-Path $repoRoot ".git"
$sourceHook = Join-Path $repoRoot "scripts\hooks\pre-commit"
$targetDir = Join-Path $gitDir "hooks"
$targetHook = Join-Path $targetDir "pre-commit"

if (-not (Test-Path -LiteralPath $gitDir -PathType Container)) {
    throw "Not a git repository: $repoRoot"
}

if ((Test-Path -LiteralPath $targetHook) -and -not $Force) {
    $sourceHash = (Get-FileHash -LiteralPath $sourceHook -Algorithm SHA256).Hash
    $targetHash = (Get-FileHash -LiteralPath $targetHook -Algorithm SHA256).Hash

    if ($sourceHash -ne $targetHash) {
        throw "Refusing to overwrite existing hook: $targetHook. Run with -Force to overwrite."
    }
}

New-Item -ItemType Directory -Force -Path $targetDir | Out-Null
Copy-Item -LiteralPath $sourceHook -Destination $targetHook -Force

Write-Host "Installed pre-commit hook to $targetHook"
