# MKM RPG Blender MCP local installer.
# No changes without -Apply or -InstallUv.
[CmdletBinding()]
param([switch]$Apply, [switch]$InstallUv)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Find-Tool([string]$name) {
    $tool = Get-Command -Name $name -ErrorAction SilentlyContinue
    if ($tool) { return $tool.Source }
    return $null
}

Write-Host 'MKM free animation toolchain check'
foreach ($name in @('blender', 'uvx', 'codex', 'node', 'npx')) {
    $found = Find-Tool $name
    if ($found) { Write-Host "$name : $found" }
    else { Write-Host "$name : not found in PATH" }
}

if ($InstallUv -and -not (Find-Tool 'uvx')) {
    if (-not (Find-Tool 'winget')) {
        throw 'winget missing. Install uv from the official uv site then restart PowerShell.'
    }
    Write-Host 'Installing uv using official winget package, current user only.'
    & winget install --id astral-sh.uv -e --scope user
    if ($LASTEXITCODE -ne 0) { throw "uv installation failed: $LASTEXITCODE" }
    $candidate = Join-Path $env:USERPROFILE '.local\bin'
    if (Test-Path (Join-Path $candidate 'uvx.exe')) {
        $env:Path = "$candidate;$env:Path"
    }
}

if (-not $Apply) {
    Write-Host 'Read-only check complete. Use -Apply after reviewing docs/MKM_FREE_BLENDER_TOOLCHAIN.md.'
    return
}

$uvx = Find-Tool 'uvx'
if (-not $uvx) {
    throw 'uvx not found. Run -InstallUv, reopen PowerShell and then use -Apply.'
}

Write-Host 'Installing MCP for Blender add-on in the local Blender profile.'
& $uvx mcp-for-blender install-addon
if ($LASTEXITCODE -ne 0) { throw "Blender addon installation failed: $LASTEXITCODE" }

$codex = Find-Tool 'codex'
if ($codex) {
    $current = (& $codex mcp list 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0) { throw 'Cannot check Codex MCP list; not changing config.' }
    if ($current -match '(?im)^\s*blender\s') {
        Write-Warning 'Codex Blender MCP already registered. Verify localhost and safe mode manually.'
    } else {
        & $codex mcp add blender --env BLENDER_HOST=localhost --env BLENDER_PORT=9876 --env BLENDER_MCP_SAFE_MODE=1 --env DISABLE_TELEMETRY=true -- $uvx mcp-for-blender
        if ($LASTEXITCODE -ne 0) { throw "Codex MCP registration failed: $LASTEXITCODE" }
    }
} else {
    Write-Warning 'Codex CLI not found; install/register the MCP in Codex Settings manually.'
}

Write-Host 'Next: enable MCP addon in Blender Preferences, restart Codex and test in a disposable scene.'
Write-Host 'This installer does not install Blender, Retarget, Mixamo, Rokoko or third-party skills.'
