export const getZennModeHelp = (): string => {
  return "El modo Zenn es el modo de reproducción aleatoria en que el usuario no escoge ninguna playlist. Es el modo en que se inicia la aplicación móvil.";
};
export const getNormalizeHelp = (): string => {
  return "La normalización de audio ajusta el volumen de la canción para que sea consistente con otras canciones. Se recomienda aplicar siempre que el fichero de audio no tenga un gran rango dinámico, puede provocar pérdida de calidad en algunos casos.";
};

// Las fechas llegan del backend en UTC (ISO 8601 con sufijo Z); el navegador
// las pasa a la zona horaria local al formatearlas.
export const formatDateTime = (value: string | null | undefined): string => {
  if (!value) return "Fecha desconocida";
  const date = new Date(value);
  if (isNaN(date.getTime())) return "Fecha desconocida";
  return date.toLocaleString("es-ES", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
};
