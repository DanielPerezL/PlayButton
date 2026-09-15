#!/usr/bin/env bash
# Comprueba que todas las librerias nativas del APK estan alineadas a 16 KB,
# requisito de Google Play para dispositivos con ese tamano de pagina.
set -euo pipefail
cd "$(dirname "$0")"

apk="${1:-app/build/outputs/apk/release/app-release.apk}"
[ -f "$apk" ] || { echo "No existe $apk. Ejecuta antes ./build_apk.sh"; exit 1; }

objdump=$(find "${ANDROID_HOME:-$HOME/Android/Sdk}/ndk" -name llvm-objdump -type f 2>/dev/null | head -1)
[ -n "$objdump" ] || { echo "No se encontro llvm-objdump en el NDK"; exit 1; }

tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
unzip -q -o "$apk" 'lib/*' -d "$tmp" 2>/dev/null || true

if [ ! -d "$tmp/lib" ]; then
    echo "El APK no contiene librerias nativas: nada que alinear."
    exit 0
fi

status=0
while IFS= read -r so; do
    align=$("$objdump" -p "$so" | awk '/LOAD/ {print $NF; exit}')
    if [ "$align" = "2**14" ]; then
        echo "OK    $align  ${so#"$tmp"/}"
    else
        echo "FALLO $align  ${so#"$tmp"/}"
        status=1
    fi
done < <(find "$tmp/lib" -name '*.so')

# Las .so deben ir sin comprimir para poder mapearse directamente en memoria.
if unzip -lv "$apk" | grep '\.so' | grep -qv 'Stored'; then
    echo "FALLO: hay librerias comprimidas en el APK (useLegacyPackaging deberia ser false)"
    status=1
fi

[ $status -eq 0 ] && echo "Todas las librerias nativas cumplen el requisito de 16 KB."
exit $status
