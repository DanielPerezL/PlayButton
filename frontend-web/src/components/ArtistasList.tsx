import { SearchIcon, CloseIcon } from "./icons/Icons";
import React, { useEffect, useState } from "react";
import { getArtists, renameArtist } from "../services/apiService";
import { ArtistSummary, GetArtistsResponse } from "../interfaces";
import { toast } from "react-toastify";
import LoadingButton from "./LoadingButton";

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
    <div>
      <div className="d-flex flex-wrap gap-2 align-items-center mb-4">
        <div className="input-group flex-grow-1" style={{ maxWidth: 420 }}>
          <span
            className="input-group-text border-end-0 pb-clickable"
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
              if (e.key === "Enter") fetchArtists(true);
            }}
          />
          {searchTerm && (
            <span
              className="input-group-text pb-clickable"
              onClick={handleClearSearch}
            >
              <CloseIcon />
            </span>
          )}
        </div>
      </div>

      {loading ? (
        <div className="pb-spinner-container">
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
                        <div>
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
                        <div className="mt-3">
                          <button
                            className="btn btn-primary btn-sm w-100"
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
