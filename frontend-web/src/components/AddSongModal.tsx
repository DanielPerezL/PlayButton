// AddSongModal.tsx
import React, { useEffect, useState } from "react";
import { createSong } from "../services/apiService";
import { toast } from "react-toastify";
import { Popover } from "bootstrap";
import HelpPopover from "./HelpPopover";
import { getZennModeHelp } from "../services/utils";
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
  const [artist, setArtist] = useState("");
  const [song, setSong] = useState("");
  const [mp3File, setMp3File] = useState<File | null>(null);
  const [shownZenn, setShownZenn] = useState(true);
  const [loading, setLoading] = useState(false);
  const [hasCopyrightConsent, setHasCopyrightConsent] = useState(false);

  useEffect(() => {
    const popoverTriggerList = document.querySelectorAll(
      '[data-bs-toggle="popover"]'
    );
    popoverTriggerList.forEach((el) => new Popover(el as HTMLElement));
  }, []);

  const setDefaults = () => {
    setArtist("");
    setSong("");
    setMp3File(null);
    setShownZenn(true);
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

    const fullName = `${artist.trim()} - ${song.trim()}`;
    if (!artist || !song) {
      toast.error("Debes completar artista y canción");
      return;
    }

    const dashCount = (fullName.match(/ - /g) || []).length;
    if (dashCount > 1) {
      toast.error("El uso de la cadena ' - ' no está permitido.");
      return;
    }

    setLoading(true);
    try {
      await createSong({ name: fullName, mp3: mp3File, shown_zenn: shownZenn });
      toast.success(`Canción '${fullName}' creada correctamente`);
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
                <h5 className="modal-title">Añadir Canción</h5>
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
                    value={song}
                    onChange={(e) => setSong(e.target.value)}
                    required
                  />
                </div>
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
                <div className="mb-3">
                  <label className="ms-1 d-flex align-items-center">
                    <span className="me-2">
                      <i
                        onClick={() => setHasCopyrightConsent((prev) => !prev)}
                        className={`bi ${
                          hasCopyrightConsent
                            ? "bi-check-square-fill text-primary"
                            : "bi-square"
                        }`}
                      ></i>
                    </span>
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
