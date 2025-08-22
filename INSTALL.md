# 📦 Guía de Instalación y Despliegue de PlayButton

Esta guía detalla cómo instalar y desplegar **PlayButton**, tu servidor de streaming privado, usando **Docker**. Está orientada principalmente a Linux, pero funciona en cualquier sistema operativo con Docker instalado.

---

## 🛠 Requisitos previos

- Docker y Docker Compose instalados y funcionando.
- Acceso a la terminal con permisos para ejecutar Docker.
- Opcional: un dominio en Cloudflare facilitará la configuración de acceso remoto seguro.

---

## 📥 Clonar el repositorio

```bash
git clone https://github.com/DanielPerezL/PlayButton
cd PlayButton
```

---

## ⚙️ Configuración inicial

1. Edita el archivo `.env` y ajusta los valores según tus preferencias:

2. Opcional: en `backend/Dockerfile` puedes ajustar el número de hilos de Gunicorn según tu máquina con la opción -w:

```dockerfile
# Ajusta el número de hilos
CMD ["gunicorn", "-w", "2", "-b", "0.0.0.0:5000", "app:app"]
```

---

## 🚀 Despliegue con Docker

Ejecuta el script de lanzamiento:

```bash
./launch_docker.sh
```

> ⚠️ Nota: La primera vez tardará aproximadamente 10 minutos.
> Si falla al crear las tablas de la base de datos, vuelve a ejecutar el script; esta vez debería completarse correctamente.

---

## 💡 Alternativa: Conectar a una base de datos externa

Si `launch_docker.sh` presenta problemas, puedes usar una base de datos remota o una base ya existente:

```bash
cd backend
nano .env
```

Configura todas las variables que aparecen en el fichero [`docker-compose.yml`](./docker-compose.yml) para el servicio de backend según tu entorno.

---

## ✅ Verificación

Si todo está correcto, el servidor PlayButton estará funcionando y accesible.

---

## 🔄 Mantener el contenedor activo

Para mantener el servidor en ejecución constante:

- Configura un **servicio de sistema** (systemd, init.d, etc.) que inicie Docker y el contenedor al arrancar.

---

## 🌐 Acceso remoto seguro

La app móvil requiere HTTPS; no permite conexiones HTTP. Recomendaciones para acceso remoto:

- **Usar Cloudflare Zero Trust → Tunnels**

  - Necesitas un dominio.
  - Instala el conector según las instrucciones de Cloudflare.
  - Configura un servicio que mantenga el túnel activo y opcionalmente un cron para reiniciar a diario (por ejemplo, a las 4 AM).
  - Es recomendable configurar el servicio de manera que, al arrancar, compruebe si hay actualizaciones de `cloudflared`. Esto ayuda a evitar problemas de compatibilidad de versiones con el conector.

> ✅ De esta forma podrás acceder a tu servidor desde cualquier lugar con seguridad.

---

¡Enhorabuena! 🎉
Tu servidor **PlayButton** está listo para reproducir música desde la app móvil y web.
