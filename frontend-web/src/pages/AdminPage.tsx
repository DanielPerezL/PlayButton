import React, { useEffect, useState } from "react";
import { isLoggedIn, isLoggedUserAdmin, login, logout } from "../services/apiService";
import LoginMenu from "../components/LoginMenu";
import { authEvents } from "../events/authEvents";
import { toast } from "react-toastify";
import UsuariosList from "../components/UsuariosList";
import CancionesList from "../components/CancionesList";
import SugerenciasList from "../components/SugerenciasList";
import NeedConfirmButton from "../components/NeedConfirmButton";
import { useNavigate } from "react-router-dom";
import UserPage from "./UserPage";
import icon from "../assets/icon.png";

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
      <div className="container pb-auth">
        <div className="pb-auth__card pb-panel text-center">
          <img src={icon} alt="" className="pb-auth__mark" width={56} height={56} />
          <h1 className="h3 mb-2">Iniciar sesión</h1>
          <p className="mb-4" style={{ color: "var(--pb-text-3)" }}>
            Accede para gestionar tu servidor
          </p>
          <LoginMenu onSubmit={login} key={refresh} />
        </div>
      </div>
    );
  }

  if(!isLoggedUserAdmin()){
    return <UserPage/>
  }

  return (
    <div className="container pb-section">
      {/* Cabecera */}
      <header className="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
        <div>
          <span className="pb-section__eyebrow">Panel</span>
          <h1 className="pb-section__title mb-0">Administración</h1>
        </div>
        <NeedConfirmButton
          className="btn btn-outline-danger"
          title="Cerrar sesión"
          message="¿Seguro que deseas cerrar sesión?"
          onConfirm={handleLogout}
        >
          Cerrar sesión
        </NeedConfirmButton>
      </header>

      {/* Selector de entidad */}
      <div className="mb-4" role="tablist" aria-label="Entidad a gestionar">
        <div className="pb-tabs">
          {(["usuarios", "canciones", "sugerencias"] as Entity[]).map(
            (entity) => (
              <button
                key={entity}
                type="button"
                role="tab"
                aria-selected={selectedEntity === entity}
                className="pb-tabs__item"
                onClick={() => setSelectedEntity(entity)}
              >
                {entity.charAt(0).toUpperCase() + entity.slice(1)}
              </button>
            )
          )}
        </div>
      </div>

      {/* Listado de la entidad seleccionada */}
      <section className="pb-panel">
        {selectedEntity === "usuarios" && <UsuariosList />}
        {selectedEntity === "canciones" && <CancionesList />}
        {selectedEntity === "sugerencias" && <SugerenciasList />}
      </section>
    </div>
  );
};

export default AdminPage;
