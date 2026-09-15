import React from "react";
import logo from "../assets/logo.png";
import { Link } from "react-router-dom";

const Navbar: React.FC = () => {
  return (
    <nav className="pb-navbar" aria-label="Navegación principal">
      <div className="container d-flex align-items-center justify-content-between gap-3">
        <Link
          to="/"
          title="PlayButton"
          aria-label="PlayButton, ir a inicio"
          onClick={() => window.scrollTo(0, 0)}
        >
          <img src={logo} className="pb-navbar__logo" alt="PlayButton" />
        </Link>

        <Link className="btn btn-primary px-3" to="/web">
          Acceder
        </Link>
      </div>
    </nav>
  );
};

export default Navbar;
