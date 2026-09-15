import React from "react";
import { useNavigate } from "react-router-dom";
import { logout, deleteUser, getLoggedUserId } from "../services/apiService";
import NeedConfirmButton from "../components/NeedConfirmButton";
import { toast } from "react-toastify";

const UserPage: React.FC = () => {
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate("/");
  };

  const handleDeleteAccount = async () => {
    try {
      const id = getLoggedUserId();
      if (!id) return;
      await deleteUser(id);
      toast.success("Cuenta eliminada correctamente");
      navigate("/");
      logout();
    } catch (error) {
      toast.error("Error al eliminar la cuenta");
      console.error(error);
    }
  };

  return (
    <div className="container pb-section">
      <section className="pb-panel text-center mx-auto" style={{ maxWidth: "40rem" }}>
        <span className="pb-section__eyebrow">Tu cuenta</span>
        <h1 className="pb-section__title mb-2">Gestiona tu cuenta</h1>
        <p className="pb-section__lead mb-4">
          Desde aquí puedes cerrar sesión o eliminar tu cuenta de forma
          permanente.
        </p>

        <div className="d-flex justify-content-center gap-3 flex-wrap">
          {/* Botón cerrar sesión */}
          <NeedConfirmButton
            className="btn btn-outline-danger btn-lg"
            title="Cerrar sesión"
            message="¿Seguro que deseas cerrar sesión?"
            onConfirm={handleLogout}
          >
            Cerrar sesión
          </NeedConfirmButton>

          {/* Botón eliminar cuenta */}
          <NeedConfirmButton
            className="btn btn-danger btn-lg"
            title="Eliminar cuenta"
            message="¡Atención! Esto eliminará tu cuenta y todos los datos asociados. ¿Deseas continuar?"
            onConfirm={handleDeleteAccount}
          >
            Eliminar cuenta
          </NeedConfirmButton>
        </div>
      </section>
    </div>
  );
};

export default UserPage;
