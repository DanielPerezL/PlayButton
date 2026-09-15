#!/usr/bin/env bash
# Genera el AAB de release firmado para subir a Google Play.
set -euo pipefail
cd "$(dirname "$0")"

version=$(grep -oP 'versionName = "\K[^"]+' app/build.gradle.kts)
./gradlew bundleRelease

out="app/build/outputs/bundle/release"
cp "$out/app-release.aab" "$out/play-button-v${version}.aab"
echo "AAB listo: $out/play-button-v${version}.aab"
