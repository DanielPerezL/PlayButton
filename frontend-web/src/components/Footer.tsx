import logo from "../assets/logo.png";
import { Link, useNavigate } from "react-router-dom";
import { GithubIcon } from "./icons/Icons";

const Footer = () => {
  const navigate = useNavigate();
  const currentYear = new Date().getFullYear();

  return (
    <footer className="pb-footer">
      <div className="container">
        <div className="row g-4">
          <div className="col-6 col-md-4">
            <h2 className="pb-footer__title">Navegación</h2>
            <ul className="list-unstyled mb-0">
              {/* Antes eran <a href>, que recargaban la SPA entera en cada
                  navegación interna. */}
              <li>
                <Link to="/about" className="pb-footer__link">
                  Acerca de nosotros
                </Link>
              </li>
              <li>
                <Link to="/privacy" className="pb-footer__link">
                  Política de privacidad
                </Link>
              </li>
              <li>
                <Link to="/terms" className="pb-footer__link">
                  Términos de uso
                </Link>
              </li>
            </ul>
          </div>

          <div className="col-6 col-md-4">
            <h2 className="pb-footer__title">Proyecto</h2>
            <ul className="list-unstyled mb-0">
              <li>
                <a
                  href="https://github.com/DanielPerezL/PlayButton"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="pb-footer__link d-inline-flex align-items-center gap-2"
                >
                  <GithubIcon /> GitHub
                </a>
              </li>
              <li>
                <a
                  href="https://github.com/DanielPerezL/PlayButton/blob/main/INSTALL.md"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="pb-footer__link"
                >
                  Guía de despliegue
                </a>
              </li>
            </ul>
          </div>

          <div className="col-12 col-md-4 text-md-end">
            <img
              src={logo}
              alt="PlayButton"
              className="pb-footer__logo mb-3"
              onClick={() => {
                window.scrollTo(0, 0);
                navigate("/");
              }}
            />
            <p className="small mb-0" style={{ color: "var(--pb-text-3)" }}>
              © {currentYear} PlayButton
              <br />
              Licenciado bajo GNU GPL v3
            </p>
          </div>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
