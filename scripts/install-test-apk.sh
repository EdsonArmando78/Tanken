#!/usr/bin/env bash
# Instala el APK debug de Tanken en un móvil Android físico.
set -Eeuo pipefail

usage() {
    cat <<'HELP'
Uso: bash ./scripts/install-test-apk.sh [opciones]

Sin opciones: compila, instala y abre Tanken en el único móvil disponible.
  -l, --list            Lista los móviles conectados, sin compilar ni instalar.
  -s, --serial SERIAL   Selecciona un móvil por su identificador ADB.
      --skip-build      Instala el APK debug existente, sin recompilar.
      --no-launch       Instala sin abrir la aplicación.
  -h, --help            Muestra esta ayuda.

Activa la depuración USB en el móvil y autoriza este equipo en su pantalla.
ADB se busca en PATH, ANDROID_HOME, ANDROID_SDK_ROOT y ~/Android/Sdk.
HELP
}

die() { printf 'Error: %s\n' "$*" >&2; exit 1; }

serial=""
list=false
build=true
launch=true
while (($#)); do
    case "$1" in
        -s|--serial)
            (($# >= 2)) && [[ -n "$2" && "$2" != -* ]] || die "Falta el SERIAL después de $1."
            serial="$2"
            shift 2
            ;;
        -l|--list) list=true; shift ;;
        --skip-build) build=false; shift ;;
        --no-launch) launch=false; shift ;;
        -h|--help) usage; exit 0 ;;
        *) printf 'Opción desconocida: %s\n' "$1" >&2; usage >&2; exit 2 ;;
    esac
done

root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
apk="$root/app/build/outputs/apk/debug/app-debug.apk"
component="mx.udg.alumnos.tanken/.MainActivity"

adb_bin=""
if command -v adb >/dev/null 2>&1; then
    adb_bin="$(command -v adb)"
else
    for sdk in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" "$HOME/Android/Sdk"; do
        if [[ -n "$sdk" && -x "$sdk/platform-tools/adb" ]]; then
            adb_bin="$sdk/platform-tools/adb"
            break
        fi
    done
fi
[[ -n "$adb_bin" ]] || die "No se encontró adb. Instala Android SDK Platform-Tools o configura ANDROID_HOME."

devices="$("$adb_bin" devices -l)" || die "No se pudo consultar ADB."
if $list; then
    printf '%s\n' "$devices" | awk 'NR == 1 || (NF && $1 !~ /^emulator-/)'
    exit 0
fi

mapfile -t ready < <(printf '%s\n' "$devices" | awk 'NR > 1 && $2 == "device" && $1 !~ /^emulator-/ {print $1}')
if [[ -n "$serial" ]]; then
    [[ "$serial" != emulator-* ]] || die "Este script solo instala en móviles físicos."
    found=false
    for candidate in "${ready[@]}"; do
        if [[ "$candidate" == "$serial" ]]; then found=true; break; fi
    done
    $found || die "El móvil $serial no está listo. Ejecuta --list, revisa el USB y acepta la autorización de depuración."
else
    ((${#ready[@]})) || die "No hay móviles listos. Conecta el móvil, activa la depuración USB y autoriza este equipo. Usa --list para revisar su estado."
    ((${#ready[@]} == 1)) || die "Hay varios móviles conectados. Usa --list y selecciona uno con --serial SERIAL."
    serial="${ready[0]}"
fi

# También rechaza emuladores conectados mediante una dirección TCP/IP.
qemu="$("$adb_bin" -s "$serial" shell getprop ro.kernel.qemu)" || die "No se pudo consultar el móvil $serial."
[[ "${qemu//$'\r'/}" != 1 ]] || die "El dispositivo seleccionado es un emulador. Selecciona un móvil físico."

if $build; then
    [[ -f "$root/gradlew" ]] || die "No se encontró el wrapper de Gradle en $root."
    printf 'Compilando el APK debug de Tanken...\n'
    bash "$root/gradlew" -p "$root" :app:assembleDebug
fi
[[ -f "$apk" ]] || die "No existe $apk. Ejecuta el script sin --skip-build para generarlo."

printf 'Instalando Tanken en %s...\n' "$serial"
"$adb_bin" -s "$serial" install -r -t "$apk"
printf 'APK instalado correctamente en %s.\n' "$serial"
if $launch; then
    printf 'Abriendo Tanken...\n'
    "$adb_bin" -s "$serial" shell am start -W -n "$component"
fi
