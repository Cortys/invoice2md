[CmdletBinding()]
param(
    [string]$InstallDir = (Join-Path $env:USERPROFILE ".invoice2md"),
    [switch]$NoPathUpdate
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")
$version = (Get-Content -LiteralPath (Join-Path $repoRoot "VERSION") -Raw).Trim()
$jarName = "invoice2md-$version-standalone.jar"
$builtJar = Join-Path $repoRoot (Join-Path "target" $jarName)
$installedJar = Join-Path $InstallDir "invoice2md-standalone.jar"
$binDir = Join-Path $InstallDir "bin"
$launcher = Join-Path $binDir "invoice2md.cmd"

Push-Location $repoRoot
try {
    & clojure -T:build uber
    if ($LASTEXITCODE -ne 0) {
        throw "Build failed with exit code $LASTEXITCODE"
    }
}
finally {
    Pop-Location
}

New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null
New-Item -ItemType Directory -Force -Path $binDir | Out-Null
Copy-Item -LiteralPath $builtJar -Destination $installedJar -Force

@(
    "@echo off",
    "java -jar `"%~dp0..\invoice2md-standalone.jar`" %*"
) | Set-Content -LiteralPath $launcher -Encoding ASCII

if (-not $NoPathUpdate) {
    $userPath = [Environment]::GetEnvironmentVariable("Path", "User")
    $pathParts = @($userPath -split ";" | Where-Object { $_ })
    $isOnPath = $pathParts | Where-Object { $_.TrimEnd("\") -ieq $binDir.TrimEnd("\") }

    if (-not $isOnPath) {
        $newPath = if ($userPath) { "$userPath;$binDir" } else { $binDir }
        [Environment]::SetEnvironmentVariable("Path", $newPath, "User")
        $env:Path = "$env:Path;$binDir"
        Write-Host "Added $binDir to the user PATH. Open a new terminal to use invoice2md everywhere."
    }
}

Write-Host "Installed invoice2md to $InstallDir"
Write-Host "Run: invoice2md convert --config config/deutsche-bahn.yml --pdf-dir inbox --markdown-dir out_md --receipt-dir out_pdf"
