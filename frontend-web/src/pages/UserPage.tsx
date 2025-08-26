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
    <main className="container my-5 main-container">
      <section className="text-center mb-5">
        <h1 className="display-4 fw-bold text-primary mb-2">
          Página de Usuario
        </h1>
        <p className="lead mb-4 fs-5 text-muted">
          Aquí puedes gestionar tu cuenta
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
    </main>
  );
};

export default UserPage;
