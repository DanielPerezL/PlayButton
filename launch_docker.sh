#!/bin/bash

# Detiene todos los servicios
docker compose stop 

# Elimina el contenedor del servicio backend
docker compose rm -f backend_playbutton 

# Construye la imagen del servicio backend (usando la caché)
docker compose build backend_playbutton

# Inicia el servicio mysql en segundo plano
docker compose up --remove-orphans -d mysql_playbutton 

# Inicia el servicio backend y se adjunta a la terminal
docker compose up backend_playbutton

docker compose stop 
