import React from "react";
import LoginMenu from "./LoginMenu";
import { login } from "../services/apiService";
import "../css/AcessMenu.css";

const AccessMenu: React.FC = () => {
  // Función que maneja el envío del formulario de login
  const handleLoginSubmit = async (username: string, password: string) => {
    try {
      await login(username, password);
    } catch (err: unknown) {
      if (err instanceof Error) {
        throw new Error(err.message);
      } else {
        throw new Error("An unknown error occurred");
      }
    }
  };

  return (
    <div className="login-grid">
      <div className="access-menu">
        <h2 className="text-center mb-4">Iniciar Sesión</h2>
        <LoginMenu onSubmit={handleLoginSubmit} />
      </div>
    </div>
  );
};

export default AccessMenu;
