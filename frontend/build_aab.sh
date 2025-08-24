#!/bin/bash

# Ruta al app.json (ajusta si hace falta)
APP_JSON_PATH="./app.json"

# Leer la versión (campo 'version' en expo)
version=$(jq -r '.expo.version' "$APP_JSON_PATH")

echo "Versión detectada: $version"

# Ejecutar build release gradle desde la carpeta android
cd android || { echo "No se encontró la carpeta android"; exit 1; }

./gradlew bundleRelease || { echo "Error en build gradle"; exit 1; }

# Volver a la carpeta padre (proyecto raíz)
cd ..

# Ruta al AAB generado
AAB_PATH="./android/app/build/outputs/bundle/release"
AAB_NAME="app-release.aab"

if [ ! -f "$AAB_PATH/$AAB_NAME" ]; then
  echo "AAB no encontrado en $AAB_PATH"
  exit 1
fi

# Nuevo nombre del AAB
NEW_AAB_NAME="play-button-v${version}.aab"

# Mover/renombrar el AAB
mv "$AAB_PATH/$AAB_NAME" "$AAB_PATH/$NEW_AAB_NAME"

echo "AAB renombrado a $NEW_AAB_NAME"
