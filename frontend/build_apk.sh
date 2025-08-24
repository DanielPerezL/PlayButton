#!/bin/bash

# Ruta al app.json (ajusta si hace falta)
APP_JSON_PATH="./app.json"

# Leer la versión (campo 'version' en expo)
version=$(jq -r '.expo.version' "$APP_JSON_PATH")

echo "Versión detectada: $version"

# Ejecutar build release gradle desde la carpeta android
cd android || { echo "No se encontró la carpeta android"; exit 1; }

./gradlew assembleRelease || { echo "Error en build gradle"; exit 1; }

# Volver a la carpeta padre (proyecto raíz)
cd ..

# Ruta al APK generado
APK_PATH="./android/app/build/outputs/apk/release"
APK_NAME="app-release.apk"

if [ ! -f "$APK_PATH/$APK_NAME" ]; then
  echo "APK no encontrado en $APK_PATH"
  exit 1
fi

# Nuevo nombre del APK
NEW_APK_NAME="play-button-v${version}.apk"

# Mover/renombrar el APK
mv "$APK_PATH/$APK_NAME" "$APK_PATH/$NEW_APK_NAME"

echo "APK renombrado a $NEW_APK_NAME"
