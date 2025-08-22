import { Outlet } from "react-router-dom";
import NavBar from "../components/NavBar";
import Footer from "../components/Footer";
import "react-toastify/dist/ReactToastify.css";

const Layout = () => {
  return (
    <>
      <nav className="top-nav">
        <NavBar />
      </nav>
      <div className="mt-5 d-flex flex-column min-vh-100 bg-light">
        <div className="mt-4 flex-grow-1 d-flex justify-content-center pb-5 container-fluid">
          <div className="container">
            <Outlet />
          </div>
        </div>
        <Footer />
      </div>
    </>
  );
};

export default Layout;
