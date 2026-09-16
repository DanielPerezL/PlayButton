import React, { useEffect, useState } from "react";
import { MusicNoteIcon, PersonIcon } from "./icons/Icons";
import { fetchCover } from "../services/apiService";

interface CoverProps {
  src: string | null;
  alt: string;
  /** Qué dibujar en el hueco cuando no hay imagen. */
  kind?: "song" | "artist";
  size?: number;
}

/** Las del servidor hay que ir a buscarlas; las locales ya son pintables. */
const isRemote = (src: string | null): src is string =>
  src !== null && /^https?:/i.test(src);

/**
 * Resuelve la portada a algo que un `<img>` pueda pintar.
 *
 * La URL del servidor no vale directamente: el endpoint pide el token como el
 * resto de la API y la etiqueta no manda cabeceras. Se baja aquí con la
 * cabecera puesta y se pinta desde memoria. La vista previa de un fichero
 * recién elegido ya es un object URL, y esa pasa tal cual.
 *
 * Bajarla no cuesta una descarga por vistazo: la respuesta viene marcada como
 * inmutable, así que a partir de la segunda vez el fetch sale de la caché del
 * navegador sin tocar la red.
 */
const useCoverSrc = (src: string | null): string | null => {
  const [objectUrl, setObjectUrl] = useState<string | null>(null);

  useEffect(() => {
    // La de antes ya no vale, y la nueva tarda: sin esto el hueco se queda
    // enseñando una portada que no es la de esta fila.
    setObjectUrl(null);
    if (!isRemote(src)) return;

    let cancelled = false;
    let created: string | null = null;

    fetchCover(src).then((blob) => {
      if (blob === null || cancelled) return;
      created = URL.createObjectURL(blob);
      setObjectUrl(created);
    });

    return () => {
      cancelled = true;
      // El object URL retiene el blob hasta que se revoca.
      if (created) URL.revokeObjectURL(created);
    };
  }, [src]);

  return isRemote(src) ? objectUrl : src;
};

/**
 * Portada con su hueco. Cuando no hay imagen se pinta el mismo cuadro de acento
 * con icono que usa la app móvil en sus tarjetas, para que las dos interfaces
 * se parezcan. Es también lo que se ve mientras la portada se descarga.
 */
const Cover: React.FC<CoverProps> = ({ src, alt, kind = "song", size = 56 }) => {
  const resolved = useCoverSrc(src);

  if (resolved) {
    return (
      <img
        src={resolved}
        alt={alt}
        // Los atributos dan la proporción antes de que cargue, para que la fila
        // no dé un salto. El tamaño va además en línea porque `_base.scss` fija
        // `height: auto` en toda imagen: sin esto la altura queda indefinida y
        // el contenedor flex la estira a lo alto de la fila.
        width={size}
        height={size}
        style={{ width: size, height: size }}
        className="pb-cover"
      />
    );
  }

  return (
    <div
      className="pb-cover pb-cover--empty"
      style={{ width: size, height: size }}
      aria-hidden
    >
      {kind === "artist" ? <PersonIcon size="1.4em" /> : <MusicNoteIcon size="1.4em" />}
    </div>
  );
};

export default Cover;
