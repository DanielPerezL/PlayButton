#!/bin/bash
# Detiene y elimina los contenedores, redes y volúmenes definidos en el archivo docker compose
docker compose down --volumes 

# Inicia el servicio mysql_db en segundo plano
docker compose up --build -d mysql_playbutton

# Construye las imágenes sin usar la caché
docker compose build --no-cache 

# Inicia el servicio backend y se adjunta a la terminal
docker compose up backend_playbutton

docker compose stop 
