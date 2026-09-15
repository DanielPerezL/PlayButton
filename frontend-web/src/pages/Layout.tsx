import { Outlet } from "react-router-dom";
import NavBar from "../components/NavBar";
import Footer from "../components/Footer";

const Layout = () => {
  return (
    // El <nav> lo pone NavBar. Antes había un <nav> aquí envolviendo a otro
    // <nav>, que es HTML inválido y duplica el landmark de navegación.
    <div className="d-flex flex-column min-vh-100">
      <NavBar />
      {/* Cada página decide su propio ancho: el hero necesita ir a sangre y
          las páginas legales van a medida de lectura. */}
      <main className="flex-grow-1">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
};

export default Layout;
