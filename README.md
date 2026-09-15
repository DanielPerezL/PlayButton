# PlayButton

PlayButton es una plataforma de streaming **privada y autoalojada**: tú pones el
servidor y el catálogo, y quien tú digas lo escucha desde su móvil. No hay
registro abierto ni nada que se comparta entre instalaciones —las cuentas las
crea el administrador—, así que cada servidor es una comunidad cerrada con su
propia biblioteca.

En el repositorio están las tres piezas que hacen falta:

| Pieza | Qué es | Dónde |
| --- | --- | --- |
| Backend | API en Flask sobre MySQL. Guarda el catálogo, firma los enlaces de reproducción y sirve el panel web ya construido. | [`backend/`](./backend) |
| Panel web | React 19 con TypeScript y Vite. Landing page pública, administración y zona de usuario. | [`frontend-web/`](./frontend-web) |
| App Android | Kotlin con Jetpack Compose y Media3. Es el cliente de escucha. | [`frontend-kotlin/`](./frontend-kotlin) |

## Cómo está montado

Decisiones explican casi todo lo demás:

- **Todo vive en MySQL**, también el audio y las portadas. Una copia de
  seguridad es un volcado de la base de datos y no hay volúmenes de ficheros que
  sincronizar. A cambio la base de datos crece deprisa: el MP3 se guarda en base64, un
  tercio más de lo que ocupa el fichero.
- **Los enlaces de audio caducan a los seis minutos.** No se sirve una URL fija
  por canción: el cliente pide una firmada justo antes de abrir el stream, así
  que un enlace filtrado deja de valer solo.
- **Cada usuario apunta la app a su servidor.** La URL se configura en el
  primer arranque, y las versiones de release solo admiten HTTPS.

## Qué se puede hacer

**El administrador**, desde el panel web: sube canciones —con normalización de
volumen a −14 dBFS, para que no haya que tocar el volumen entre pista y pista—,
edita título, artistas y portada, decide qué canciones entran en el Modo Zen,
crea y borra cuentas, y revisa las sugerencias que llegan.

**Cada usuario** tiene sus playlists, públicas o privadas, puede marcar como
favoritas las de los demás, navegar por artistas, buscar en el catálogo y
sugerir canciones que le gustaría encontrar.

**En el móvil**, además:

- **Modo Zen**: dale al botón y suena el catálogo entero barajado, sin elegir
  nada. La cola se rellena sola, así que no se acaba nunca.
- **Escucha sin conexión**: descarga una playlist entera —con sus portadas— y
  queda disponible sin cobertura.
- **Caché de audio con límite ajustable**: lo que escuchas se guarda para no
  volver a gastar datos con ello, y al llegar al tope se descarta lo más
  antiguo.
- **Reproducción en segundo plano** con notificación, controles en la pantalla
  de bloqueo y fundido opcional entre canciones.

## Puesta en marcha

El despliegue completo está en [INSTALL.md](./INSTALL.md). En corto: copia
`.env.example` a `.env`, cambia todos los valores y lanza `./launch_docker.sh`.

## Licencia

[GNU General Public License v3.0](./LICENSE).

© 2026 [PlayButton](https://playbutton.danielperezl.com).

> Cada administrador es responsable de los archivos de audio que aloja en su
> servidor. Asegúrate de cumplir con la legalidad y los derechos de autor.
