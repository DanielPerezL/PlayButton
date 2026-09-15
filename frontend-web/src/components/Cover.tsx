import React from "react";
import { MusicNoteIcon, PersonIcon } from "./icons/Icons";

interface CoverProps {
  src: string | null;
  alt: string;
  /** Qué dibujar en el hueco cuando no hay imagen. */
  kind?: "song" | "artist";
  size?: number;
}

/**
 * Portada con su hueco. Cuando no hay imagen se pinta el mismo cuadro de acento
 * con icono que usa la app móvil en sus tarjetas, para que las dos interfaces
 * se parezcan.
 */
const Cover: React.FC<CoverProps> = ({ src, alt, kind = "song", size = 56 }) => {
  if (src) {
    return (
      <img
        src={src}
        alt={alt}
        // Los atributos dan la proporción antes de que cargue, para que la fila
        // no dé un salto. El tamaño va además en línea porque `_base.scss` fija
        // `height: auto` en toda imagen: sin esto la altura queda indefinida y
        // el contenedor flex la estira a lo alto de la fila.
        width={size}
        height={size}
        style={{ width: size, height: size }}
        className="pb-cover"
        loading="lazy"
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
