import React, { useEffect, useState } from "react";
import { User, GetUsersResponse } from "../interfaces";
import { getUsers, deleteUser } from "../services/apiService";
import AddUserModal from "./AddUserModal";
import { toast } from "react-toastify";
import NeedConfirmButton from "./NeedConfirmButton";
import ChangePasswordModal from "./ChangePasswordModal";
import LoadingButton from "./LoadingButton";

const UsuariosList: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showModal, setShowModal] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [passwordModalUser, setPasswordModalUser] = useState<User | null>(null);

  const [offset, setOffset] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const limit = 20;
  const [searchTerm, setSearchTerm] = useState("");

  const fetchUsers = async (reset = false, term = searchTerm) => {
    if (reset) setLoading(true);
    else setLoadingMore(true);

    const data: GetUsersResponse | null = await getUsers(
      reset ? 0 : offset,
      limit,
      term || undefined
    );
    if (data) {
      if (reset) {
        setUsers(data.users || []);
        setOffset(limit);
      } else {
        setUsers((prev) => [...prev, ...(data.users || [])]);
        setOffset((prev) => prev + limit);
      }
      setHasMore(data.has_more);
    } else {
      setError("Error al cargar usuarios");
    }
    setLoading(false);
    setLoadingMore(false);
  };

  useEffect(() => {
    fetchUsers(true);
  }, []);

  const handleUserAdded = () => {
    fetchUsers(true);
  };

  const handleDelete = async (id: string, nickname: string) => {
    setDeletingId(id);
    try {
      await deleteUser(id);
      toast.success(`Usuario '${nickname}' eliminado`);
      fetchUsers(true);
    } catch (err: any) {
      console.error(err);
      toast.error(err?.message || "Error al eliminar usuario");
    } finally {
      setDeletingId(null);
    }
  };

  const handleClearSearch = async () => {
    setSearchTerm("");
    fetchUsers(true, "");
  };

  if (error)
    return <div className="alert alert-danger text-center">{error}</div>;

  return (
    <div className="container mt-4">
      <div className="d-flex flex-column flex-md-row justify-content-between mb-4 gap-3">
        <div className="input-group">
          <span
            className="input-group-text bg-white border-end-0"
            onClick={() => fetchUsers(true)}
          >
            <i className="bi bi-search"></i>
          </span>
          <input
            type="text"
            className="form-control border-start-0"
            placeholder="Buscar usuarios..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                fetchUsers(true);
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
          onClick={() => setShowModal(true)}
        >
          Añadir usuario
        </button>
      </div>

      <AddUserModal
        show={showModal}
        onClose={() => setShowModal(false)}
        onUserAdded={handleUserAdded}
      />

      {passwordModalUser && (
        <ChangePasswordModal
          show={!!passwordModalUser}
          user={passwordModalUser}
          onClose={() => setPasswordModalUser(null)}
        />
      )}

      {loading ? (
        <div className="d-flex justify-content-center align-items-center my-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Cargando...</span>
          </div>
        </div>
      ) : (
        <div className="row g-3">
          {users.length > 0 ? (
            users.map((user) => (
              <div key={user.id} className="col-12 col-md-6 col-lg-4">
                <div className="card shadow-sm h-100">
                  <div className="card-body d-flex flex-column justify-content-between">
                    <div>
                      <h5 className="card-title">{user.nickname}</h5>
                    </div>
                    <div className="mt-3 d-flex gap-2 flex-wrap">
                      {Number(user.id) !== 1 ? (
                        <>
                          <button
                            className="btn btn-primary btn-sm flex-grow-1"
                            onClick={() => setPasswordModalUser(user)}
                          >
                            Cambiar contraseña
                          </button>

                          <NeedConfirmButton
                            className="btn btn-danger btn-sm flex-grow-1"
                            title="Confirmar eliminación"
                            message={`¿Seguro que quieres eliminar al usuario '${user.nickname}'?`}
                            onConfirm={() =>
                              handleDelete(user.id, user.nickname)
                            }
                          >
                            {deletingId === user.id
                              ? "Eliminando..."
                              : "Eliminar"}
                          </NeedConfirmButton>
                        </>
                      ) : (
                        <span className="fs-6 badge bg-primary flex-grow-1">
                          Administrador
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            ))
          ) : (
            <div className="col-12">
              <div className="alert alert-info text-center">
                No se han encontrado usuarios.
              </div>
            </div>
          )}
        </div>
      )}

      {!loading && hasMore && (
        <div className="d-flex justify-content-center my-4">
          <LoadingButton
            loading={loadingMore}
            onClick={() => fetchUsers(false)}
          >
            {loadingMore ? "Cargando..." : "Cargar más"}
          </LoadingButton>
        </div>
      )}
    </div>
  );
};

export default UsuariosList;
