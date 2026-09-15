# Instalación y despliegue

Esta guía levanta un servidor PlayButton con Docker. Está escrita para Linux,
pero funciona en cualquier sistema con Docker y Docker Compose.

## Requisitos

- Docker y Docker Compose.
- Un dominio propio si quieres escuchar desde fuera de casa. La app Android
  solo admite HTTPS en sus versiones de release, así que sin dominio ni
  certificado el servidor queda limitado a la red local.

## Configuración

```bash
git clone https://github.com/DanielPerezL/PlayButton
cd PlayButton
cp .env.example .env
```

Abre `.env` y cambia **todos** los valores antes de seguir. Para los secretos:

```bash
python3 -c "import secrets; print(secrets.token_urlsafe(48))"
```

| Variable | Para qué sirve |
| --- | --- |
| `ADMIN_PASSWORD` | Contraseña del usuario `admin`, que se crea solo en el primer arranque. |
| `SECRET_KEY` | Firma los enlaces temporales de audio. |
| `JWT_SECRET_KEY` | Firma los tokens de sesión. Distinto del anterior. |
| `MYSQL_ROOT_PASSWORD` | Contraseña de root de MySQL. |
| `MYSQL_DATABASE_NAME`, `MYSQL_USER`, `MYSQL_PASSWORD` | Base de datos y credenciales que usará el backend. |
| `DATABASE_HOST` | `mysql_db_playbutton` si usas el MySQL del compose. |
| `CORS_ENABLED` | `false` salvo que sirvas el panel web desde otro origen. |

Hay más parámetros con valor por defecto razonable —caducidad de los tokens
(90 días), de los enlaces de audio (6 minutos), tamaño y peso máximo de las
portadas (512 px, 10 MB), longitud del nick—. Para cambiar alguno no basta con
añadirlo a `.env`: el backend solo ve lo que Compose le pasa, así que hay que
declararlo también en el bloque `environment` de `backend_playbutton` en
[`docker-compose.yml`](./docker-compose.yml).

En [`backend/Dockerfile`](./backend/Dockerfile) puedes subir el número de
procesos de Gunicorn (`-w 2`) si procede.

## Arranque

```bash
./launch_docker.sh
```

El script reconstruye el backend, levanta MySQL y deja el backend enganchado a
la terminal: es para ver el primer arranque y comprobar que todo sube. La
primera vez tarda varios minutos, casi todo en construir la imagen. Al salir
con `Ctrl+C` para los contenedores.

Cuando ya funcione, para dejarlo corriendo:

```bash
docker compose up -d
```

En ese primer arranque el backend crea las tablas, aplica las migraciones
pendientes y da de alta al administrador. Todo ello va bajo un bloqueo de
MySQL, porque los dos procesos de Gunicorn arrancan a la vez y sin coordinarse
se pisaban.

Comprueba que responde abriendo `http://localhost:5000`: verás la landing page, y
podrás entrar como `admin` con la contraseña que pusiste. Si algo falla, los
logs lo dicen:

```bash
docker compose logs -f backend_playbutton
```

> Si pierdes la contraseña del administrador, cambia `ADMIN_PASSWORD` en `.env`
> y reinicia: en cada arranque se vuelve a aplicar sobre el usuario `admin`.

## Acceso desde fuera

La app Android no admite tráfico en claro, así que necesitas HTTPS. La opción
más cómoda, si tienes dominio, es un **túnel de Cloudflare Zero Trust**: no
hace falta abrir puertos en el router ni gestionar certificados.

- Instala el conector `cloudflared` siguiendo las instrucciones de Cloudflare y
  apunta el túnel a `localhost:5000`.
- Déjalo como servicio del sistema para que se levante solo al arrancar.
- Conviene que ese servicio compruebe si hay versión nueva de `cloudflared`
  antes de levantar el túnel: las incompatibilidades de versión con el conector
  son el fallo más común.

Detrás del túnel, el backend ve las peticiones en claro y genera enlaces
`http://`. No hay que tocar nada: los clientes reescriben el esquema al del
servidor que tienen configurado, porque es el cliente quien sabe qué tiene
permitido.

## Base de datos externa

Si prefieres tu propio MySQL en lugar del contenedor, apunta `DATABASE_HOST`
—y las credenciales— a él en `.env` y levanta solo el backend:

```bash
docker compose up -d backend_playbutton
```

El backend no lee ningún fichero de entorno por su cuenta: todas sus variables
se las pasa Compose desde el `.env` de la raíz.

## Copias de seguridad

Todo está en la base de datos: canciones, portadas, usuarios y playlists. Una
copia es un volcado, y restaurar es volver a cargarlo.

## Actualizar

```bash
git pull
docker compose restart backend_playbutton
```

El código del backend se monta desde el repositorio, así que reiniciar basta;
solo hace falta volver a construir (`./launch_docker.sh`) si han cambiado las
dependencias. Las migraciones se aplican solas al arrancar y quedan registradas,
de modo que reiniciar mil veces no repite trabajo.

## Mantenimiento y seguridad

- **Que se levante solo.** Ni el compose ni el script traen política de
  reinicio: añade `restart: unless-stopped` a los dos servicios de
  `docker-compose.yml`, o gestiona el arranque con systemd.
- **Cierra MySQL.** El compose publica el puerto `3306` en el host. Si la
  máquina está expuesta, protégelo con el cortafuegos o quita ese mapeo: el
  backend llega a la base por la red interna de Docker sin necesitarlo.
- **No reutilices secretos.** `SECRET_KEY` y `JWT_SECRET_KEY` deben ser
  distintas, y cambiarlas invalida los enlaces y las sesiones en curso.
- **Cuentas.** No hay registro público: las crea el administrador desde el
  panel web, y cada usuario puede borrar la suya desde la app.

---

> Cada administrador es responsable de los archivos de audio que aloja en su
> servidor. Asegúrate de cumplir con la legalidad y los derechos de autor.
