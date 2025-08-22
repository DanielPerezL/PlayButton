// EditSongModal.tsx
import React, { useState, useEffect } from "react";
import { updateSong } from "../services/apiService";
import { toast } from "react-toastify";
import { Song } from "../interfaces";
import HelpPopover from "./HelpPopover";
import { getZennModeHelp } from "../services/utils";

interface EditSongModalProps {
  show: boolean;
  onClose: () => void;
  song: Song | null;
  onSongUpdated: () => void;
}

const EditSongModal: React.FC<EditSongModalProps> = ({
  show,
  onClose,
  song,
  onSongUpdated,
}) => {
  const [artist, setArtist] = useState("");
  const [title, setTitle] = useState("");
  const [shownZenn, setShownZenn] = useState(true);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (song) {
      // Suponiendo que el `name` en backend viene con el formato "Artista - Canción"
      const [parsedArtist, parsedTitle] = song.name.split(" - ");
      setArtist(parsedArtist || "");
      setTitle(parsedTitle || "");
      setShownZenn(song.shown_zenn);
    }
  }, [song]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const fullName = `${artist.trim()} - ${title.trim()}`;

    if (!artist.trim() || !title.trim()) {
      toast.error("Debes completar artista y canción");
      return;
    }

    const dashCount = (fullName.match(/ - /g) || []).length;

    if (
      dashCount > 1 ||
      artist.includes(" - ") ||
      title.includes(" - ") ||
      artist.startsWith("- ") ||
      title.startsWith("- ") ||
      artist.endsWith(" -") ||
      title.endsWith(" -")
    ) {
      toast.error("El uso de la cadena ' - ' no está permitido.");
      return;
    }

    if (!song) return;

    setLoading(true);
    try {
      await updateSong(song.id, fullName, shownZenn);
      toast.success(`Canción '${fullName}' actualizada correctamente`);
      onSongUpdated();
      onClose();
    } catch (err: any) {
      console.error(err);
      toast.error(err?.message || "Error al actualizar canción");
    } finally {
      setLoading(false);
    }
  };

  if (!show || !song) return null;

  return (
    <>
      <div
        className="modal-backdrop fade show"
        style={{
          position: "fixed",
          top: 0,
          left: 0,
          width: "100%",
          height: "100%",
          backgroundColor: "rgba(0,0,0,0.5)",
          zIndex: 1040,
        }}
        onClick={onClose}
      />
      <div
        className="modal show d-block"
        tabIndex={-1}
        style={{ zIndex: 1050 }}
      >
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content bg-light">
            <form onSubmit={handleSubmit}>
              <div className="modal-header">
                <h5 className="modal-title">Editar Canción</h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={onClose}
                ></button>
              </div>
              <div className="modal-body">
                <div className="mb-3">
                  <label className="form-label">Artista</label>
                  <input
                    type="text"
                    className="form-control"
                    value={artist}
                    onChange={(e) => setArtist(e.target.value)}
                    required
                  />
                </div>
                <div className="mb-3">
                  <label className="form-label">Canción</label>
                  <input
                    type="text"
                    className="form-control"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    required
                  />
                </div>
                <div className="mb-3 form-check form-switch">
                  <input
                    type="checkbox"
                    className="form-check-input"
                    id="shownZenn"
                    checked={shownZenn}
                    onChange={(e) => setShownZenn(e.target.checked)}
                  />
                  <label className="form-check-label" htmlFor="shownZenn">
                    Mostrar en Zenn
                  </label>
                  <HelpPopover
                    bootstrapColor="primary"
                    content={getZennModeHelp()}
                  />
                </div>
              </div>
              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={onClose}
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="btn btn-outline-success"
                  disabled={loading}
                >
                  {loading ? "Guardando..." : "Guardar cambios"}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </>
  );
};

export default EditSongModal;
