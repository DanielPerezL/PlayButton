import { SearchIcon, CloseIcon } from "./icons/Icons";
import React, { useEffect, useState } from "react";
import {
  deleteImage,
  getArtists,
  renameArtist,
  setImage,
} from "../services/apiService";
import { ArtistSummary, GetArtistsResponse } from "../interfaces";
import { toast } from "react-toastify";
import LoadingButton from "./LoadingButton";
import Cover from "./Cover";

const ArtistasList: React.FC = () => {
  const [artists, setArtists] = useState<ArtistSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [editingId, setEditingId] = useState<string | null>(null);
  const [editingName, setEditingName] = useState("");
  const [saving, setSaving] = useState(false);

  const [offset, setOffset] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const limit = 20;

  useEffect(() => {
    fetchArtists(true);
  }, []);

  const fetchArtists = async (reset = false, term = searchTerm) => {
    if (reset) setLoading(true);
    else setLoadingMore(true);

    const data: GetArtistsResponse | null = await getArtists(
      reset ? 0 : offset,
      limit,
      term || undefined
    );

    if (data) {
      if (reset) {
        setArtists(data.artists);
        setOffset(limit);
      } else {
        setArtists((prev) => [...prev, ...data.artists]);
        setOffset((prev) => prev + limit);
      }
      setHasMore(data.has_more);
    } else {
      setError("Error al cargar artistas");
    }

    setLoading(false);
    setLoadingMore(false);
  };

  const handleClearSearch = async () => {
    setSearchTerm("");
    setOffset(0);
    await fetchArtists(true, "");
  };

  const startEditing = (artist: ArtistSummary) => {
    setEditingId(artist.id);
    setEditingName(artist.name);
  };

  const handleImage = async (artist: ArtistSummary, file: File) => {
    try {
      await setImage("artists", artist.id, file);
      toast.success(`Foto de '${artist.name}' actualizada`);
      await fetchArtists(true);
    } catch (err: any) {
      console.error(err);
      toast.error(err?.message || "Error al subir la imagen");
    }
  };

  const handleRemoveImage = async (artist: ArtistSummary) => {
    try {
      await deleteImage("artists", artist.id);
      toast.success(`Foto de '${artist.name}' eliminada`);
      await fetchArtists(true);
    } catch (err: any) {
      console.error(err);
      toast.error(err?.message || "Error al quitar la imagen");
    }
  };

  const handleRename = async (artist: ArtistSummary) => {
    const name = editingName.trim();
    if (!name || name === artist.name) {
      setEditingId(null);
      return;
    }

    setSaving(true);
    try {
      await renameArtist(artist.id, name);
      toast.success(`Artista '${artist.name}' renombrado a '${name}'`);
      setEditingId(null);
      // El nombre viaja también a su playlist, así que se recarga entero.
      await fetchArtists(true);
    } catch (err: any) {
      console.error(err);
      toast.error(err?.message || "Error al renombrar artista");
    } finally {
      setSaving(false);
    }
  };

  if (error) {
    return <div className="alert alert-danger">{error}</div>;
  }

  return (
    <div className="container mt-4">
      <div className="d-flex flex-column flex-md-row justify-content-between mb-4 gap-3">
        <div className="input-group">
          <span
            className="input-group-text border-end-0"
            onClick={() => fetchArtists(true)}
          >
            <SearchIcon />
          </span>
          <input
            type="text"
            className="form-control border-start-0"
            placeholder="Buscar artistas..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                fetchArtists(true);
              }
            }}
          />
          {searchTerm && (
            <span
              className="input-group-text border-start-0"
              onClick={handleClearSearch}
            >
              <CloseIcon />
            </span>
          )}
        </div>
      </div>

      {loading ? (
        <div className="d-flex justify-content-center align-items-center my-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Cargando...</span>
          </div>
        </div>
      ) : (
        <div className="row g-3">
          {artists.length > 0 ? (
            artists.map((artist) => (
              <div key={artist.id} className="col-12 col-md-6 col-lg-4">
                <div className="card shadow-sm h-100">
                  <div className="card-body d-flex flex-column justify-content-between">
                    {editingId === artist.id ? (
                      <div className="d-flex gap-2">
                        <input
                          type="text"
                          className="form-control"
                          value={editingName}
                          autoFocus
                          onChange={(e) => setEditingName(e.target.value)}
                          onKeyDown={(e) => {
                            if (e.key === "Enter") handleRename(artist);
                            if (e.key === "Escape") setEditingId(null);
                          }}
                        />
                        <LoadingButton
                          loading={saving}
                          className="btn btn-outline-success btn-sm"
                          onClick={() => handleRename(artist)}
                        >
                          Guardar
                        </LoadingButton>
                        <button
                          type="button"
                          className="btn btn-outline-secondary btn-sm"
                          onClick={() => setEditingId(null)}
                        >
                          Cancelar
                        </button>
                      </div>
                    ) : (
                      <>
                        <div className="d-flex align-items-center gap-3">
                          <Cover src={artist.image_url} alt="" kind="artist" />
                          <div className="pb-truncate flex-grow-1">
                            <h5 className="card-title mb-1">{artist.name}</h5>
                            <p className="card-text text-body-secondary mb-0">
                              {artist.songs_count}{" "}
                              {artist.songs_count === 1 ? "canción" : "canciones"}
                              {" · "}
                              {artist.favorites_count}{" "}
                              {artist.favorites_count === 1
                                ? "favorito"
                                : "favoritos"}
                            </p>
                          </div>
                        </div>
                        <div className="mt-3 d-flex flex-column gap-2">
                          <label
                            className="btn btn-outline-primary btn-sm mb-0 pb-clickable"
                            htmlFor={`cover-${artist.id}`}
                          >
                            {artist.image_url ? "Cambiar foto" : "Subir foto"}
                          </label>
                          <input
                            id={`cover-${artist.id}`}
                            type="file"
                            accept="image/png,image/jpeg,image/webp"
                            className="d-none"
                            onChange={(e) => {
                              const file = e.target.files?.[0];
                              // Se limpia para poder volver a elegir el mismo fichero.
                              e.target.value = "";
                              if (file) handleImage(artist, file);
                            }}
                          />
                          {artist.image_url && (
                            <button
                              className="btn btn-outline-danger btn-sm"
                              onClick={() => handleRemoveImage(artist)}
                            >
                              Quitar foto
                            </button>
                          )}
                          <button
                            className="btn btn-primary btn-sm"
                            onClick={() => startEditing(artist)}
                          >
                            Renombrar
                          </button>
                        </div>
                      </>
                    )}
                  </div>
                </div>
              </div>
            ))
          ) : (
            <div className="col-12">
              <div className="alert alert-info text-center">
                No se han encontrado artistas.
              </div>
            </div>
          )}
        </div>
      )}

      {!loading && hasMore && (
        <div className="d-flex justify-content-center my-4">
          <LoadingButton
            loading={loadingMore}
            onClick={() => fetchArtists(false)}
          >
            {loadingMore ? "Cargando..." : "Cargar más"}
          </LoadingButton>
        </div>
      )}
    </div>
  );
};

export default ArtistasList;
