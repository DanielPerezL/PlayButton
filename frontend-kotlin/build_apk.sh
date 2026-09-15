#!/usr/bin/env bash
# Genera el APK de release firmado y lo renombra con la versión del proyecto.
set -euo pipefail
cd "$(dirname "$0")"

version=$(grep -oP 'versionName = "\K[^"]+' app/build.gradle.kts)
./gradlew assembleRelease

out="app/build/outputs/apk/release"
cp "$out/app-release.apk" "$out/play-button-v${version}.apk"
echo "APK listo: $out/play-button-v${version}.apk"
