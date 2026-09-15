import React, { useEffect, useState } from "react";
import { Suggestion, GetSuggestionsResponse } from "../interfaces";
import NeedConfirmButton from "./NeedConfirmButton";
import { deleteSuggestion, getSuggestions } from "../services/apiService";
import { toast } from "react-toastify";
import LoadingButton from "./LoadingButton";
import { formatDateTime } from "../services/utils";

const SugerenciasList: React.FC = () => {
  const [suggestions, setSuggestions] = useState<Suggestion[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [offset, setOffset] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const limit = 20;

  const fetchSuggestions = async (reset = false) => {
    setLoading(true);
    const response: GetSuggestionsResponse | null = await getSuggestions(
      reset ? 0 : offset,
      limit
    );
    if (response) {
      if (reset) {
        setSuggestions(response.suggestions);
        setOffset(limit);
      } else {
        setSuggestions((prev) => [...prev, ...response.suggestions]);
        setOffset((prev) => prev + limit);
      }
      setHasMore(response.has_more);
    } else {
      setError("Error al cargar sugerencias");
    }
    setLoading(false);
  };

  useEffect(() => {
    fetchSuggestions(true);
  }, []);

  const handleDelete = async (sugerencia: Suggestion) => {
    try {
      await deleteSuggestion(sugerencia.id);
      toast.success(
        `Sugerencia '${sugerencia.song_name}' eliminada correctamente`
      );
      fetchSuggestions(true); // recargamos desde el inicio
    } catch (error) {
      toast.error(
        `Error al eliminar la sugerencia ${sugerencia.song_name}: ${
          error instanceof Error ? error.message : "Error desconocido"
        }`
      );
    }
  };

  if (error)
    return <div className="alert alert-danger text-center">{error}</div>;

  if (loading && suggestions.length === 0)
    return (
      <div className="d-flex justify-content-center align-items-center my-5">
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Cargando...</span>
        </div>
      </div>
    );

  if (!suggestions || suggestions.length === 0)
    return (
      <div className="d-flex justify-content-center align-items-center">
        <div className="col-6">
          <div className="alert alert-info text-center w-100">
            <p>No se han encontrado sugerencias.</p>
          </div>
        </div>
      </div>
    );

  return (
    <>
      <div className="list-group col-12 col-lg-6 mx-auto">
        {suggestions.map((s) => (
          <div key={s.id} className="list-group-item d-flex align-items-start">
            <div className="flex-grow-1 me-2" style={{ minWidth: 0 }}>
              <div
                className="text-truncate"
                title={s.song_name} // opcional: tooltip con el nombre completo
              >
                {s.song_name}
              </div>

              <div className="small text-muted mt-1">
                Sugerida el {formatDateTime(s.created_at)}
              </div>

              {/* Las sugerencias repetidas se agrupan, asi que aqui salen
                  todos los usuarios que han pedido la misma cancion. */}
              <div className="d-flex flex-wrap gap-1 mt-1">
                {s.suggested_by && s.suggested_by.length > 0 ? (
                  s.suggested_by.map((u) => (
                    <span key={u.id} className="badge text-bg-secondary">
                      {u.nickname}
                    </span>
                  ))
                ) : (
                  <span className="small text-muted fst-italic">
                    Sin usuarios registrados
                  </span>
                )}
              </div>
            </div>

            <NeedConfirmButton
              title="Confirmar eliminación"
              message={`¿Seguro que quieres eliminar la sugerencia '${s.song_name}'?`}
              className="btn btn-danger btn-sm flex-shrink-0 text-nowrap"
              onConfirm={() => handleDelete(s)}
            >
              Eliminar
            </NeedConfirmButton>
          </div>
        ))}
      </div>

      {hasMore && (
        <div className="d-flex justify-content-center my-4">
          <LoadingButton
            loading={loading}
            onClick={() => fetchSuggestions(false)}
          >
            {loading ? "Cargando..." : "Cargar más"}
          </LoadingButton>
        </div>
      )}
    </>
  );
};

export default SugerenciasList;
