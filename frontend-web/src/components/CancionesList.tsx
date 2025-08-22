import React, { useEffect, useState } from "react";
import { getSongs, deleteSong } from "../services/apiService";
import { Song, GetSongsResponse } from "../interfaces";
import AddSongModal from "./AddSongModal";
import EditSongModal from "./EditSongModal";
import NeedConfirmButton from "./NeedConfirmButton";
import { toast } from "react-toastify";
import LoadingButton from "./LoadingButton";

const CancionesList: React.FC = () => {
  const [songs, setSongs] = useState<Song[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);

  const [modalOpen, setModalOpen] = useState(false);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [selectedSong, setSelectedSong] = useState<Song | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [offset, setOffset] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const limit = 20;

  useEffect(() => {
    fetchSongs(true);
  }, []);

  const fetchSongs = async (reset = false, term = searchTerm) => {
    if (reset) setLoading(true);
    else setLoadingMore(true);
    const data: GetSongsResponse | null = await getSongs(
      reset ? 0 : offset,
      limit,
      term || undefined
    );
    if (data) {
      if (reset) {
        setSongs(data.songs);
        setOffset(limit);
      } else {
        setSongs((prev) => [...prev, ...data.songs]);
        setOffset((prev) => prev + limit);
      }
      setHasMore(data.has_more);
    } else {
      setError("Error al cargar canciones");
    }
    setLoading(false);
    setLoadingMore(false);
  };

  const handleDelete = async (song: Song) => {
    await deleteSong(song.id);
    toast.success(`Canción '${song.name}' eliminada correctamente`);
    fetchSongs(true);
  };

  const handleClearSearch = async () => {
    setSearchTerm("");
    fetchSongs(true, "");
  };

  if (error)
    return <div className="alert alert-danger text-center">{error}</div>;

  return (
    <div className="container mt-4">
      <div className="d-flex flex-column flex-md-row justify-content-between mb-4 gap-3">
        <div className="input-group">
          <span
            className="input-group-text bg-white border-end-0"
            onClick={() => fetchSongs(true)}
          >
            <i className="bi bi-search"></i>
          </span>
          <input
            type="text"
            className="form-control border-start-0"
            placeholder="Buscar canciones..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                fetchSongs(true);
              }
            }}
          />
          {searchTerm && (
            <span
              className="input-group-text bg-white border-start-0"
              onClick={handleClearSearch}
            >
              <i className="bi bi-x-lg"></i>
            </span>
          )}
        </div>

        <button
          className="btn btn-primary flex-shrink-0"
          onClick={() => setModalOpen(true)}
        >
          Añadir canción
        </button>
      </div>
      <AddSongModal
        show={modalOpen}
        onClose={() => setModalOpen(false)}
        onSongAdded={() => fetchSongs(true)}
      />
      <EditSongModal
        show={editModalOpen}
        song={selectedSong}
        onClose={() => setEditModalOpen(false)}
        onSongUpdated={() => fetchSongs(true)}
      />
      {loading ? (
        <div className="d-flex justify-content-center align-items-center my-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Cargando...</span>
          </div>
        </div>
      ) : (
        <div className="row g-3">
          {songs.length > 0 ? (
            songs.map((song) => (
              <div key={song.id} className="col-12 col-md-6 col-lg-4">
                <div className="card shadow-sm h-100">
                  <div className="card-body d-flex flex-column justify-content-between">
                    <div>
                      <h5 className="card-title">{song.name}</h5>
                      <p className="card-text">
                        Mostrada en Zenn:{" "}
                        <strong>{song.shown_zenn ? "Sí" : "No"}</strong>
                      </p>
                    </div>
                    <div className="mt-3 d-flex gap-2 flex-wrap">
                      <button
                        className="btn btn-primary btn-sm flex-grow-1"
                        onClick={() => {
                          setSelectedSong(song);
                          setEditModalOpen(true);
                        }}
                      >
                        Editar
                      </button>

                      <NeedConfirmButton
                        className="btn btn-danger btn-sm flex-grow-1"
                        onConfirm={() => handleDelete(song)}
                        title="Eliminar Canción"
                        message={`¿Estás seguro de que quieres eliminar la canción '${song.name}'?`}
                      >
                        Eliminar
                      </NeedConfirmButton>
                    </div>
                  </div>
                </div>
              </div>
            ))
          ) : (
            <div className="col-12">
              <div className="alert alert-info text-center">
                No se han encontrado canciones.
              </div>
            </div>
          )}
        </div>
      )}
      {!loading && hasMore && (
        <div className="d-flex justify-content-center my-4">
          <LoadingButton
            loading={loadingMore}
            onClick={() => fetchSongs(false)}
          >
            {loadingMore ? "Cargando..." : "Cargar más"}
          </LoadingButton>
        </div>
      )}
    </div>
  );
};

export default CancionesList;
