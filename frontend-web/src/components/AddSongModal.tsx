// AddSongModal.tsx
import React, { useEffect, useState } from "react";
import { createSong, setImage } from "../services/apiService";
import { parseArtists } from "../services/songName";
import ArtistsField from "./ArtistsField";
import CoverField from "./CoverField";
import { toast } from "react-toastify";
import { Popover } from "bootstrap";
import HelpPopover from "./HelpPopover";
import { getNormalizeHelp, getZenModeHelp } from "../services/utils";
import LoadingButton from "./LoadingButton";

interface AddSongModalProps {
  show: boolean;
  onClose: () => void;
  onSongAdded: () => void;
}

const AddSongModal: React.FC<AddSongModalProps> = ({
  show,
  onClose,
  onSongAdded,
}) => {
  const [artists, setArtists] = useState("");
  const [title, setTitle] = useState("");
  const [mp3File, setMp3File] = useState<File | null>(null);
  const [coverFile, setCoverFile] = useState<File | null>(null);
  const [shownZen, setShownZen] = useState(true);
  const [normalize, setNormalize] = useState(true);
  const [loading, setLoading] = useState(false);
  const [hasCopyrightConsent, setHasCopyrightConsent] = useState(false);

  useEffect(() => {
    const popoverTriggerList = document.querySelectorAll(
      '[data-bs-toggle="popover"]'
    );
    popoverTriggerList.forEach((el) => new Popover(el as HTMLElement));
  }, []);

  const setDefaults = () => {
    setArtists("");
    setTitle("");
    setMp3File(null);
    setCoverFile(null);
    setShownZen(true);
    setNormalize(true);
    setHasCopyrightConsent(false);
  };

  const handleClose = () => {
    setDefaults();
    onClose();
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!mp3File) {
      toast.error("Debes seleccionar un archivo MP3");
      return;
    }

    if (!hasCopyrightConsent) {
      toast.error("Debes confirmar que tienes derechos sobre la canción.");
      return;
    }

    const artistNames = parseArtists(artists);
    if (!artistNames.length || !title.trim()) {
      toast.error("Debes completar artista y canción");
      return;
    }

    setLoading(true);
    try {
      const songId = await createSong({
        title: title.trim(),
        artists: artistNames,
        mp3: mp3File,
        shown_zen: shownZen,
        normalize: normalize,
      });

      // La portada va aparte porque hasta aquí no hay id al que colgarla. Si
      // falla, la canción ya está creada: se avisa y se puede añadir editando.
      if (coverFile && songId) {
        try {
          await setImage("songs", songId, coverFile);
        } catch {
          toast.warning("La canción se creó, pero no se pudo subir la portada");
        }
      }

      toast.success(`Canción '${title.trim()}' creada correctamente`);
      onSongAdded();
      setDefaults();
      onClose();
    } catch (err: any) {
      console.error(err);
      toast.error(err?.message || "Error al crear canción");
    } finally {
      setLoading(false);
    }
  };

  if (!show) return null;

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
                <h5 className="modal-title">Añadir Canción</h5>
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
                  file={coverFile}
                  onFileChange={setCoverFile}
                />
                <div className="mb-3">
                  <label className="form-label">Archivo MP3</label>
                  <input
                    type="file"
                    accept="audio/mp3"
                    className="form-control"
                    onChange={(e) => setMp3File(e.target.files?.[0] || null)}
                    required
                  />
                </div>
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
                <div className="mb-3 form-check form-switch">
                  <input
                    type="checkbox"
                    className="form-check-input"
                    id="normalize"
                    checked={normalize}
                    onChange={(e) => setNormalize(e.target.checked)}
                  />
                  <label className="form-check-label" htmlFor="normalize">
                    Normalizar audio
                  </label>
                  <HelpPopover
                    bootstrapColor="primary"
                    content={getNormalizeHelp()}
                  />
                </div>
                <div className="mb-3 form-check">
                  <input
                    type="checkbox"
                    className="form-check-input"
                    id="copyrightConsent"
                    checked={hasCopyrightConsent}
                    onChange={(e) => setHasCopyrightConsent(e.target.checked)}
                  />
                  <label
                    className="form-check-label"
                    htmlFor="copyrightConsent"
                  >
                    Declaro que poseo los derechos necesarios para subir esta
                    canción y que su contenido no infringe derechos de autor.
                  </label>
                </div>
              </div>
              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={handleClose}
                >
                  Cancelar
                </button>
                <LoadingButton
                  type="submit"
                  loading={loading}
                  className="btn btn-outline-success"
                >
                  {loading ? "Creando..." : "Crear Canción"}
                </LoadingButton>
              </div>
            </form>
          </div>
        </div>
      </div>
    </>
  );
};

export default AddSongModal;
