#Requires -Version 5.1
param(
    [string]$GameDir = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$MinecraftVersion = '26.3'
$FabricLoaderVersion = '0.20.1'
$InstallerVersion = '1.1.2'
$ModFilename = 'mkm-rpg-0.0.5-fabric.7.jar'
$InstallerUrl = "https://maven.fabricmc.net/net/fabricmc/fabric-installer/$InstallerVersion/fabric-installer-$InstallerVersion.exe"
$InstallerHashUrl = "$InstallerUrl.sha256"

function Say([string]$message) { Write-Host "[MKM] $message" }

function Download-VerifiedInstaller {
    $tempExe = Join-Path ([System.IO.Path]::GetTempPath()) "mkm-fabric-installer-$InstallerVersion.exe"
    Say 'Downloading official Fabric installer from maven.fabricmc.net ...'
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    $officialHashText = [string](Invoke-WebRequest -Uri $InstallerHashUrl -UseBasicParsing -TimeoutSec 60).Content
    $expectedHash = ($officialHashText.Trim() -split '\s+')[0].ToLowerInvariant()
    if ($expectedHash -notmatch '^[a-f0-9]{64}$') {
        throw 'Could not validate the official Fabric installer SHA-256 checksum.'
    }
    Invoke-WebRequest -Uri $InstallerUrl -UseBasicParsing -OutFile $tempExe -TimeoutSec 120
    $actualHash = (Get-FileHash -LiteralPath $tempExe -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actualHash -ne $expectedHash) {
        Remove-Item -LiteralPath $tempExe -Force -ErrorAction SilentlyContinue
        throw 'The downloaded Fabric installer checksum does not match the official checksum.'
    }
    return $tempExe
}

try {
    Say 'MKM 0.0.5-fabric.7 installer - official Minecraft Launcher'
    Say "Minecraft $MinecraftVersion | Fabric Loader $FabricLoaderVersion"

    $modSource = Join-Path $PSScriptRoot $ModFilename
    $checksums = Join-Path $PSScriptRoot 'SHA256SUMS.txt'
    if (-not (Test-Path -LiteralPath $modSource -PathType Leaf)) {
        throw "Missing $ModFilename. Extract ALL files from the MKM ZIP first."
    }
    if (-not (Test-Path -LiteralPath $checksums -PathType Leaf)) {
        throw 'Missing SHA256SUMS.txt. Extract ALL files from the MKM ZIP first.'
    }
    $expectedModHash = ((Get-Content -LiteralPath $checksums -TotalCount 1).Trim() -split '\s+')[0].ToLowerInvariant()
    $actualModHash = (Get-FileHash -LiteralPath $modSource -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($expectedModHash -notmatch '^[a-f0-9]{64}$' -or $actualModHash -ne $expectedModHash) {
        throw 'MKM mod integrity check failed. Download the archive again.'
    }

    if ([string]::IsNullOrWhiteSpace($GameDir)) {
        if ([string]::IsNullOrWhiteSpace($env:APPDATA)) {
            throw 'APPDATA was not found. Specify -GameDir manually.'
        }
        $GameDir = Join-Path $env:APPDATA '.minecraft'
    }
    $GameDir = [System.IO.Path]::GetFullPath([Environment]::ExpandEnvironmentVariables($GameDir))
    Say "Minecraft location: $GameDir"
    if (-not (Test-Path -LiteralPath $GameDir -PathType Container)) {
        throw 'Minecraft directory does not exist. Install/run official Minecraft 26.3 first, or specify -GameDir.'
    }
    $profiles = Get-ChildItem -LiteralPath $GameDir -Filter 'launcher_profiles*.json' -File -ErrorAction SilentlyContinue
    if (-not $profiles -and -not (Test-Path -LiteralPath (Join-Path $GameDir 'versions') -PathType Container)) {
        throw 'This directory does not look like an official Minecraft Launcher installation. No changes made.'
    }

    $launcher = Get-Process -Name 'MinecraftLauncher' -ErrorAction SilentlyContinue
    if ($launcher) {
        throw 'Close Minecraft Launcher completely before installation, then run Install_MKM.cmd again.'
    }

    $installerExe = Download-VerifiedInstaller
    Say 'Installing Fabric Loader and creating the launcher profile ...'
    & $installerExe client -dir $GameDir -mcversion $MinecraftVersion -loader $FabricLoaderVersion
    if ($LASTEXITCODE -ne 0) {
        throw "Official Fabric installer failed with exit code $LASTEXITCODE. No MKM files were copied."
    }

    $versionRoot = Join-Path $GameDir 'versions'
    $expectedProfile = "fabric-loader-$FabricLoaderVersion-$MinecraftVersion"
    $profilePath = Join-Path (Join-Path $versionRoot $expectedProfile) "$expectedProfile.json"
    if (-not (Test-Path -LiteralPath $profilePath -PathType Leaf)) {
        $alternates = @(Get-ChildItem -LiteralPath $versionRoot -Directory -ErrorAction SilentlyContinue | Where-Object {
            $_.Name -like "*fabric*loader*$FabricLoaderVersion*$MinecraftVersion*" -and
            (Get-ChildItem -LiteralPath $_.FullName -Filter '*.json' -File -ErrorAction SilentlyContinue).Count -gt 0
        })
        if ($alternates.Count -eq 0) {
            throw 'Fabric installer returned success, but no Fabric 26.3 launcher version was found. No MKM files were copied.'
        }
    }

    $modsDir = Join-Path $GameDir 'mods'
    if (-not (Test-Path -LiteralPath $modsDir)) {
        New-Item -ItemType Directory -Path $modsDir -Force | Out-Null
    }
    $older = @(Get-ChildItem -LiteralPath $modsDir -Filter '*.jar' -File | Where-Object {
        $_.Name -match '^(mkm-rpg-|mkm-).*\.jar$'
    })
    if ($older.Count -gt 0) {
        $backup = Join-Path $modsDir ('.mkm-backup\' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
        New-Item -ItemType Directory -Path $backup -Force | Out-Null
        foreach ($oldMod in $older) {
            Move-Item -LiteralPath $oldMod.FullName -Destination (Join-Path $backup $oldMod.Name) -ErrorAction Stop
        }
        Say "Older MKM files backed up to: $backup"
    }
    Copy-Item -LiteralPath $modSource -Destination (Join-Path $modsDir $ModFilename) -ErrorAction Stop

    Say 'SUCCESS: Fabric Loader installed; MKM with embedded Fabric API copied to mods.'
    Say "Open Minecraft Launcher and select Fabric Loader $FabricLoaderVersion for Minecraft $MinecraftVersion."
    Say 'An internet connection is needed for the first install; no Minecraft game files are redistributed.'
    exit 0
}
catch {
    Write-Host ('[MKM] ERROR: ' + $_.Exception.Message) -ForegroundColor Red
    exit 1
}
