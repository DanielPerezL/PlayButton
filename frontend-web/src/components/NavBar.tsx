import React from "react";
import logo from "../assets/logo.png";
import "../css/Navbar.css";
import { Link } from "react-router-dom";

const Navbar: React.FC = () => {
  return (
    <nav
      className="navbar"
      style={{ backgroundColor: "#310042" }}
      role="navigation"
      aria-label="Navegación principal"
    >
      <div className="container">
        <div className="d-flex align-items-center justify-content-between w-100">
          {/* Logo con nombre accesible, alineado a la izquierda */}
          <Link
            to="/"
            className="btn"
            title="PlayButton"
            onClick={() => {
              window.scrollTo(0, 0);
            }}
            aria-label="Ir a inicio"
          >
            <img src={logo} className="logo-img" alt="Logo de la app" />
          </Link>

          <div className="d-flex align-items-center">
            <Link className="btn btn-light mx-1" to="/web">
              Acceder
            </Link>
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
