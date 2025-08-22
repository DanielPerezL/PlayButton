import React, { useEffect, useState } from "react";
import { isLoggedIn, login, logout } from "../services/apiService";
import LoginMenu from "../components/LoginMenu";
import { authEvents } from "../events/authEvents";
import { toast } from "react-toastify";
import UsuariosList from "../components/UsuariosList";
import CancionesList from "../components/CancionesList";
import SugerenciasList from "../components/SugerenciasList";
import NeedConfirmButton from "../components/NeedConfirmButton";
import { useNavigate } from "react-router-dom";

type Entity = "usuarios" | "canciones" | "sugerencias";

const AdminPage: React.FC = () => {
  const navigate = useNavigate();
  const [refresh, setRefresh] = useState<number>(0);
  const [selectedEntity, setSelectedEntity] = useState<Entity>("usuarios");

  const handleLogout = () => {
    logout();
    navigate("/");
  };

  useEffect(() => {
    const handleLogin = () => {
      setRefresh((prev) => prev + 1);
      toast.success("Sesión iniciada correctamente");
    };
    const handleLogout = () => {
      setRefresh((prev) => prev + 1);
      toast.info("Sesión cerrada satisfactoriamente");
    };

    authEvents.on("login", handleLogin);
    authEvents.on("logout", handleLogout);

    return () => {
      authEvents.off("login", handleLogin);
      authEvents.off("logout", handleLogout);
    };
  }, []);

  useEffect(() => {
    window.scrollTo(0, 0);
  }, [selectedEntity]);

  if (!isLoggedIn()) {
    return (
      <main className="container my-5 main-container">
        <div className="d-flex align-items-center justify-content-center min-vh-50">
          <div className="text-center w-100" style={{ maxWidth: 400 }}>
            <h3 className="display-5 fw-bold text-primary mb-3">
              Iniciar Sesión
            </h3>
            <LoginMenu onSubmit={login} key={refresh} />
          </div>
        </div>
      </main>
    );
  }

  return (
    <main className="container my-5 main-container">
      {/* Cabecera */}
      <section className="text-center mb-5">
        <h1 className="display-4 fw-bold text-primary mb-2">
          Panel de Administración
        </h1>
        <p className="lead mb-4 fs-4 text-muted">
          <i>Gestiona tu servidor</i>
        </p>
        <div className="d-flex justify-content-center gap-3 flex-wrap">
          <NeedConfirmButton
            className="btn btn-outline-danger btn-lg"
            title="Cerrar sesión"
            message="¿Seguro que deseas cerrar sesión?"
            onConfirm={handleLogout}
          >
            Cerrar sesión
          </NeedConfirmButton>
        </div>
      </section>

      {/* Selector de entidad */}
      <section className="text-center mb-5">
        <div className="d-flex justify-content-center gap-2 flex-wrap">
          {["usuarios", "canciones", "sugerencias"].map((entity) => (
            <button
              key={entity}
              className={`btn btn-lg ${
                selectedEntity === entity
                  ? "btn-primary"
                  : "btn-outline-primary"
              }`}
              onClick={() => setSelectedEntity(entity as Entity)}
            >
              {entity.charAt(0).toUpperCase() + entity.slice(1)}
            </button>
          ))}
        </div>
      </section>

      {/* Listado de la entidad seleccionada */}
      <section className="px-3 px-md-0">
        {selectedEntity === "usuarios" && <UsuariosList />}
        {selectedEntity === "canciones" && <CancionesList />}
        {selectedEntity === "sugerencias" && <SugerenciasList />}
      </section>
    </main>
  );
};

export default AdminPage;
