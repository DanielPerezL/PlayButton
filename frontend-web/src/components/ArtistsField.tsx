import React, { useEffect, useState } from "react";
import { getArtists } from "../services/apiService";
import { parseArtists } from "../services/songName";

interface ArtistsFieldProps {
  value: string;
  onChange: (value: string) => void;
}

/** Sustituye el término que se está escribiendo por el artista elegido. */
const completeLast = (input: string, name: string): string => {
  const parts = input.split(",");
  parts[parts.length - 1] = name;
  return `${parts.map((part) => part.trim()).filter(Boolean).join(", ")}, `;
};

/**
 * Campo de artistas. Se escriben separados por comas y debajo se enseña en qué
 * artistas ha quedado partido lo escrito: es la única forma de ver a tiempo
 * que un nombre con coma, como "Tyler, The Creator", se ha convertido en dos.
 *
 * Las sugerencias vienen del maestro de artistas, para no crear un "Rosalia"
 * nuevo por haber escrito el que ya existe de otra forma.
 */
const ArtistsField: React.FC<ArtistsFieldProps> = ({ value, onChange }) => {
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const parsed = parseArtists(value);

  useEffect(() => {
    const term = (value.split(",").pop() ?? "").trim();
    if (!term) {
      setSuggestions([]);
      return;
    }

    let cancelled = false;
    const timer = setTimeout(async () => {
      const data = await getArtists(0, 6, term);
      if (cancelled) return;
      const names = (data?.artists ?? []).map((artist) => artist.name);
      // El que ya está escrito entero no aporta nada como sugerencia.
      setSuggestions(names.filter((name) => name.toLowerCase() !== term.toLowerCase()));
    }, 250);

    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [value]);

  return (
    <div className="mb-3">
      <label className="form-label" htmlFor="artists">
        Artistas
      </label>
      <input
        id="artists"
        type="text"
        className="form-control"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder="Separa varios artistas con comas"
        autoComplete="off"
        required
      />

      {suggestions.length > 0 && (
        <div className="mt-2 d-flex flex-wrap gap-1">
          {suggestions.map((name) => (
            <button
              key={name}
              type="button"
              className="btn btn-sm btn-outline-secondary py-0"
              onClick={() => onChange(completeLast(value, name))}
            >
              {name}
            </button>
          ))}
        </div>
      )}

      {parsed.length > 0 && (
        <div className="form-text mt-2">
          Se guardarán {parsed.length === 1 ? "como artista" : "como artistas"}:{" "}
          {parsed.map((name) => (
            <span key={name} className="badge text-bg-secondary me-1">
              {name}
            </span>
          ))}
        </div>
      )}
    </div>
  );
};

export default ArtistsField;
