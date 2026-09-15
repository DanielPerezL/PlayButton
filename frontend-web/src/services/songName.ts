import { Artist } from "../interfaces";

/** Cómo se muestran los artistas de una canción allá donde no caben en varias líneas. */
export const formatArtists = (artists: Artist[]): string =>
  artists.map((artist) => artist.name).join(", ");

/**
 * Parte lo que el administrador escribe en el campo de artistas.
 *
 * La coma separa, así que un artista que la lleve en el nombre ("Tyler, The
 * Creator") se partiría en dos. No hay forma de adivinarlo, por eso el
 * formulario enseña debajo los artistas que han salido de aquí.
 */
export const parseArtists = (input: string): string[] =>
  input
    .split(",")
    .map((name) => name.trim())
    .filter(Boolean);
