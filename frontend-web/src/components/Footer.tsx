import logo from "../assets/logo.png";
import "../css/Footer.css";
import { useNavigate } from "react-router-dom";

const Footer = () => {
  const navigate = useNavigate();

  const currentYear = new Date().getFullYear();

  return (
    <footer className="bg-dark text-white py-4 mt-5 pb-5 pb-md-0">
      <div className="container pb-5 pb-md-0">
        <div className="row">
          <div className="col-md-4 mb-3 ps-3 ps-md-0">
            <h5>Navegación</h5>
            <ul className="list-unstyled">
              <li>
                <a href="/about" className="text-white text-decoration-none">
                  Acerca de nosotros
                </a>
              </li>

              <li>
                <a href="/privacy" className="text-white text-decoration-none">
                  Política de privacidad
                </a>
              </li>
              <li>
                <a href="/terms" className="text-white text-decoration-none">
                  Términos de uso
                </a>
              </li>
            </ul>
          </div>

          {/* Sección de redes sociales */}
          <div className="col-md-4 mb-3 ps-3 ps-md-0">
            <h5>Síguenos</h5>
            <ul className="list-unstyled">
              <li>
                <a
                  href="https://github.com/DanielPerezL/PlayButton"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-white text-decoration-none"
                >
                  GitHub
                </a>
              </li>
            </ul>
          </div>

          {/* Sección de derechos de autor */}
          <div className="copyright-section col-md-4 text-center">
            <img
              src={logo}
              alt="Logo de PlayButton"
              className="mb-2"
              onClick={() => {
                window.scrollTo(0, 0);
                navigate("/");
              }}
            />
            <p className="mb-4">
              © {currentYear} PlayButton. Licenciado bajo GNU GPL v3.
            </p>
          </div>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
