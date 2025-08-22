import React, { useState } from "react";
import { updateUserPassword } from "../services/apiService";
import { toast } from "react-toastify";
import { User } from "../interfaces";

interface Props {
  show: boolean;
  onClose: () => void;
  user: User;
}

const ChangePasswordModal: React.FC<Props> = ({ show, onClose, user }) => {
  const [newPassword, setNewPassword] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    if (!newPassword) {
      toast.error("Ingresa la nueva contraseña");
      return;
    }

    try {
      setLoading(true);
      await updateUserPassword(user.id, newPassword); // solo enviamos la nueva contraseña
      toast.success(`Contraseña del usuario '${user.nickname}' actualizada`);
      onClose();
      setNewPassword(""); // limpiar campo
    } catch (err: any) {
      console.error(err);
      toast.error(err?.message || "Error al actualizar contraseña");
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
      <div className="modal d-block" tabIndex={-1} style={{ zIndex: 1050 }}>
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content bg-light">
            <div className="modal-header">
              <h5 className="modal-title">Cambiar Contraseña</h5>
              <button
                type="button"
                className="btn-close"
                onClick={onClose}
              ></button>
            </div>
            <div className="modal-body">
              <label className="form-label">
                Nueva contraseña para <strong>{user.nickname}</strong>
              </label>
              <input
                type="password"
                className="form-control"
                placeholder="Nueva contraseña"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
              />
            </div>
            <div className="modal-footer">
              <button className="btn btn-outline-secondary" onClick={onClose}>
                Cancelar
              </button>
              <button
                className="btn btn-outline-success"
                onClick={handleSubmit}
                disabled={loading}
              >
                {loading ? "Guardando..." : "Guardar"}
              </button>
            </div>
          </div>
        </div>
      </div>
    </>
  );
};

export default ChangePasswordModal;
