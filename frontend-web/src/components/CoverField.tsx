import React, { useEffect, useState } from "react";
import Cover from "./Cover";

interface CoverFieldProps {
  /** Portada actual en el servidor, si la hay. */
  currentUrl?: string | null;
  /** True si `currentUrl` viene heredada del artista y no es propia. */
  inherited?: boolean;
  kind?: "song" | "artist";
  file: File | null;
  onFileChange: (file: File | null) => void;
  /** Sin esto no se ofrece quitar la portada (en un alta no hay nada que quitar). */
  onRemove?: () => void;
}

/**
 * Selector de portada con vista previa. La imagen elegida se previsualiza en
 * local antes de subirla: el servidor la recorta a un cuadrado, y verlo antes
 * evita la sorpresa de descubrir el recorte una vez guardada.
 */
const CoverField: React.FC<CoverFieldProps> = ({
  currentUrl,
  inherited = false,
  kind = "song",
  file,
  onFileChange,
  onRemove,
}) => {
  const [preview, setPreview] = useState<string | null>(null);

  useEffect(() => {
    if (!file) {
      setPreview(null);
      return;
    }
    const url = URL.createObjectURL(file);
    setPreview(url);
    // El object URL retiene el fichero hasta que se revoca.
    return () => URL.revokeObjectURL(url);
  }, [file]);

  const hasOwn = Boolean(currentUrl) && !inherited;

  return (
    <div className="mb-3">
      <label className="form-label" htmlFor="cover">
        Portada
      </label>
      <div className="d-flex align-items-center gap-3">
        <Cover src={preview ?? currentUrl ?? null} alt="" kind={kind} size={64} />
        <div className="flex-grow-1">
          <input
            id="cover"
            type="file"
            accept="image/png,image/jpeg,image/webp"
            className="form-control"
            onChange={(e) => onFileChange(e.target.files?.[0] || null)}
          />
          <div className="form-text">
            {inherited
              ? "Ahora se muestra la del artista. Si subes una, se usará esta."
              : "Se recortará a un cuadrado. Opcional."}
          </div>
        </div>
      </div>

      {(hasOwn || file) && (
        <div className="mt-2 d-flex gap-2">
          {file && (
            <button
              type="button"
              className="btn btn-sm btn-outline-secondary"
              onClick={() => onFileChange(null)}
            >
              Descartar la elegida
            </button>
          )}
          {hasOwn && onRemove && (
            <button
              type="button"
              className="btn btn-sm btn-outline-danger"
              onClick={onRemove}
            >
              Quitar la portada actual
            </button>
          )}
        </div>
      )}
    </div>
  );
};

export default CoverField;
