# PlayButton para Android

Cliente Android nativo de PlayButton, escrito en Kotlin con Jetpack Compose.
Sustituye a la app React Native de [`../frontend`](../frontend), que se conserva
como referencia.

## Requisitos

- JDK 17
- Android SDK con la plataforma **API 37** (Android 17)
- Un servidor PlayButton accesible (la app pide su URL en el primer arranque)

## Compilar

```bash
./gradlew assembleDebug     # APK de depuración
./gradlew testDebugUnitTest # Pruebas unitarias
./build_apk.sh              # APK de release firmado
./build_aab.sh              # AAB para Google Play
./check_16kb.sh             # Verifica la alineación de 16 KB
```

## Firma

Las credenciales viven en `keystore.properties`, que **no está en el
repositorio**. Para compilar una release hay que crearlo junto a este README:

```properties
storeFile=my-release-key.keystore
storePassword=…
keyAlias=…
keyPassword=…
```

Sin ese fichero el proyecto sigue compilando: la variante de release cae a la
firma de depuración.

## Estructura

```
data/     Acceso a datos: API (Retrofit), almacenes locales y repositorios
domain/   Modelos del dominio
player/   Servicio de reproducción con Media3 y construcción de la cola
ui/       Tema, componentes, pantallas y navegación
di/       Módulos de Hilt
```

Puntos que conviene conocer antes de tocar el código:

- **La URL del servidor no es fija.** Cada usuario apunta la app a su propio
  servidor, así que `BaseUrlInterceptor` reescribe el destino de cada petición
  a partir de lo guardado en DataStore.
- **Los enlaces de audio caducan a los seis minutos.** `SignedUrlResolver` pide
  uno nuevo justo antes de abrir cada stream, de modo que las canciones largas
  y las pausas prolongadas no rompen la reproducción.
- **Un artista es una playlist.** El backend no tiene modelo de artista: son
  playlists con `is_artist_playlist = true` generadas a partir del nombre de
  las canciones, con el formato `"Artista - Título"`.
- **La reproducción vive en un servicio**, no en una pantalla, y el foco de
  audio lo gestiona Media3.
- **Los listados se cachean en memoria** en `PlaylistRepository`, con
  invalidación explícita al crear, editar, borrar o marcar favorito.

## Compatibilidad con páginas de 16 KB

Google Play exige que las librerías nativas estén alineadas a 16 KB. El
proyecto no incluye código nativo propio —solo las librerías de Media3, ya
alineadas— y empaqueta con `useLegacyPackaging = false`. `./check_16kb.sh`
comprueba el APK generado.
