// EditSongModal.tsx
import React, { useState, useEffect } from "react";
import { deleteImage, setImage, updateSong } from "../services/apiService";
import { formatArtists, parseArtists } from "../services/songName";
import ArtistsField from "./ArtistsField";
import CoverField from "./CoverField";
import { toast } from "react-toastify";
import { Song } from "../interfaces";
import HelpPopover from "./HelpPopover";
import { getZenModeHelp } from "../services/utils";
import LoadingButton from "./LoadingButton";

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
  const [artists, setArtists] = useState("");
  const [title, setTitle] = useState("");
  const [shownZen, setShownZen] = useState(true);
  const [coverFile, setCoverFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (song) {
      setArtists(formatArtists(song.artists));
      setTitle(song.title);
      setShownZen(song.shown_zen);
      setCoverFile(null);
    }
  }, [song]);

  /** Quitar la portada surte efecto al momento: no espera a guardar. */
  const handleRemoveCover = async () => {
    if (!song) return;
    try {
      await deleteImage("songs", song.id);
      toast.success("Portada eliminada");
      onSongUpdated();
      onClose();
    } catch (err: any) {
      toast.error(err?.message || "Error al quitar la portada");
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const artistNames = parseArtists(artists);

    if (!artistNames.length || !title.trim()) {
      toast.error("Debes completar artista y canción");
      return;
    }

    if (!song) return;

    setLoading(true);
    try {
      await updateSong(song.id, title.trim(), artistNames, shownZen);
      if (coverFile) await setImage("songs", song.id, coverFile);
      toast.success(`Canción '${title.trim()}' actualizada correctamente`);
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
      <div className="pb-backdrop" onClick={onClose} />
      <div
        className="modal show d-block"
        tabIndex={-1}
        style={{ zIndex: 1050 }}
      >
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content">
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
                <ArtistsField value={artists} onChange={setArtists} />
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
                <CoverField
                  currentUrl={song.own_image_url ?? song.image_url}
                  inherited={!song.own_image_url && Boolean(song.image_url)}
                  file={coverFile}
                  onFileChange={setCoverFile}
                  onRemove={handleRemoveCover}
                />
                <div className="mb-3 form-check form-switch">
                  <input
                    type="checkbox"
                    className="form-check-input"
                    id="shownZen"
                    checked={shownZen}
                    onChange={(e) => setShownZen(e.target.checked)}
                  />
                  <label className="form-check-label" htmlFor="shownZen">
                    Mostrar en Zen
                  </label>
                  <HelpPopover
                    bootstrapColor="primary"
                    content={getZenModeHelp()}
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
                <LoadingButton
                  type="submit"
                  loading={loading}
                  className="btn btn-outline-success"
                >
                  {loading ? "Guardando..." : "Guardar cambios"}
                </LoadingButton>
              </div>
            </form>
          </div>
        </div>
      </div>
    </>
  );
};

export default EditSongModal;
