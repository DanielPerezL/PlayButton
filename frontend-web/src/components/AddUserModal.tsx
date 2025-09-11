// AddUserModal.tsx
import React, { useState } from "react";
import { createUser } from "../services/apiService";
import { toast } from "react-toastify";
import LoadingButton from "./LoadingButton";

interface AddUserModalProps {
  show: boolean;
  onClose: () => void;
  onUserAdded: () => void;
}

const AddUserModal: React.FC<AddUserModalProps> = ({
  show,
  onClose,
  onUserAdded,
}) => {
  const [nickname, setNickname] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await createUser({ nickname, password });
      toast.success(`Usuario '${nickname}' creado correctamente`);
      onUserAdded();
      setNickname("");
      setPassword("");
      onClose();
    } catch (err: any) {
      console.error(err);
      toast.error(err?.message || "Error al crear usuario");
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
                <h5 className="modal-title">Añadir Usuario</h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={onClose}
                ></button>
              </div>
              <div className="modal-body">
                <div className="mb-3">
                  <label className="form-label">Usuario</label>
                  <input
                    type="text"
                    className="form-control"
                    value={nickname}
                    onChange={(e) =>
                      setNickname(e.target.value.replace(/\s/g, ""))
                    }
                    required
                  />
                </div>
                <div className="mb-3">
                  <label className="form-label">Contraseña</label>
                  <input
                    type="password"
                    className="form-control"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
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
                  {loading ? "Creando..." : "Crear Usuario"}
                </LoadingButton>
              </div>
            </form>
          </div>
        </div>
      </div>
    </>
  );
};

export default AddUserModal;
