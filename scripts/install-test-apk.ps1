# Windows equivalent of install-test-apk.sh (Windows PowerShell 5.1 or newer).
$ErrorActionPreference = 'Stop'

function Show-Usage {
    @'
Usage: .\scripts\install-test-apk.cmd [options]
       powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\install-test-apk.ps1 [options]

By default, build, install and open Tanken on the only ready physical device.
  -l, --list            List connected physical devices without building/installing.
  -s, --serial SERIAL   Select a device by its ADB identifier.
      --skip-build      Install the existing debug APK without rebuilding.
      --no-launch       Install without opening the app.
  -h, --help            Show this help.

Enable USB debugging and authorize this computer on the device screen.
ADB is searched in PATH, ANDROID_HOME, ANDROID_SDK_ROOT and
%LOCALAPPDATA%\Android\Sdk.
'@
}

try {
    $serial = ''
    $list = $false
    $build = $true
    $launch = $true
    for ($i = 0; $i -lt $args.Count; $i++) {
        switch -CaseSensitive ($args[$i]) {
            { $_ -ceq '-s' -or $_ -ceq '--serial' } {
                if ($i + 1 -ge $args.Count -or [string]::IsNullOrWhiteSpace($args[$i + 1]) -or $args[$i + 1].StartsWith('-')) {
                    throw "Missing SERIAL after $($args[$i])."
                }
                $serial = $args[++$i]
            }
            { $_ -ceq '-l' -or $_ -ceq '--list' } { $list = $true }
            '--skip-build' { $build = $false }
            '--no-launch' { $launch = $false }
            { $_ -ceq '-h' -or $_ -ceq '--help' } { Show-Usage; exit 0 }
            default { [Console]::Error.WriteLine("Unknown option: $($args[$i])"); Show-Usage; exit 2 }
        }
    }

    $projectRoot = Split-Path -Parent $PSScriptRoot
    $apk = Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk'
    $component = 'mx.udg.alumnos.tanken/.MainActivity'
    $adb = $null
    $command = Get-Command adb -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($command) {
        $adb = $command.Source
    } else {
        $sdkRoots = @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT)
        if ($env:LOCALAPPDATA) { $sdkRoots += Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
        foreach ($sdk in $sdkRoots) {
            if ($sdk) {
                $candidate = Join-Path $sdk 'platform-tools\adb.exe'
                if (Test-Path -LiteralPath $candidate -PathType Leaf) { $adb = $candidate; break }
            }
        }
    }
    if (-not $adb) { throw 'ADB was not found. Install Android SDK Platform-Tools or configure ANDROID_HOME.' }

    $devices = @(& $adb devices -l)
    if ($LASTEXITCODE -ne 0) { throw 'Could not query ADB.' }
    if ($list) {
        $devices | Where-Object { $_ -notmatch '^emulator-' }
        exit 0
    }
    $ready = @(
        foreach ($line in $devices) {
            if ($line -match '^(\S+)\s+device(?:\s|$)' -and $Matches[1] -notlike 'emulator-*') {
                $Matches[1]
            }
        }
    )
    if ($serial) {
        if ($serial -like 'emulator-*') { throw 'This script only installs on physical devices.' }
        if ($ready -cnotcontains $serial) {
            throw "Device $serial is not ready. Use --list, check USB and accept the debugging authorization."
        }
    } else {
        if ($ready.Count -eq 0) { throw 'No ready physical devices. Connect a device, enable USB debugging and authorize this computer. Use --list to check its state.' }
        if ($ready.Count -gt 1) { throw 'Multiple devices are ready. Use --list and select one with --serial SERIAL.' }
        $serial = $ready[0]
    }

    # Also reject emulators connected over TCP/IP.
    $qemu = @(& $adb -s $serial shell getprop ro.kernel.qemu)
    if ($LASTEXITCODE -ne 0) { throw "Could not query device $serial." }
    if (($qemu -join '').Trim() -eq '1') { throw 'The selected device is an emulator. Select a physical device.' }

    if ($build) {
        $gradle = Join-Path $projectRoot 'gradlew.bat'
        if (-not (Test-Path -LiteralPath $gradle -PathType Leaf)) { throw "Gradle wrapper not found: $gradle" }
        Write-Host 'Building the Tanken debug APK...'
        & $gradle -p $projectRoot :app:assembleDebug
        if ($LASTEXITCODE -ne 0) { throw 'Gradle build failed.' }
    }
    if (-not (Test-Path -LiteralPath $apk -PathType Leaf)) { throw "APK not found: $apk. Run without --skip-build to generate it." }

    Write-Host "Installing Tanken on $serial..."
    & $adb -s $serial install -r -t $apk
    if ($LASTEXITCODE -ne 0) { throw 'APK installation failed.' }
    Write-Host "APK installed successfully on $serial."
    if ($launch) {
        Write-Host 'Opening Tanken...'
        & $adb -s $serial shell am start -W -n $component
        if ($LASTEXITCODE -ne 0) { throw 'Could not launch Tanken.' }
    }
    exit 0
} catch {
    [Console]::Error.WriteLine("Error: $($_.Exception.Message)")
    exit 1
}
