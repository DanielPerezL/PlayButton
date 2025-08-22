# Usa una imagen base de Python
FROM python:3.9-slim

# Configura el directorio de trabajo
WORKDIR /app

# Copia los archivos necesarios
COPY requirements.txt requirements.txt
RUN pip install -r requirements.txt
RUN apt-get update && apt-get install -y \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*
COPY . .

# Comando para ejecutar la aplicación con Gunicorn
CMD ["gunicorn", "-w", "2", "--timeout", "120", "--bind", "0.0.0.0:5000", "app:app", "--access-logfile", "-", "--error-logfile", "-"]
