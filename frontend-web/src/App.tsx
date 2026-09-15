import { BrowserRouter, Routes, Route } from "react-router-dom";
import Layout from "./pages/Layout";
import AboutPage from "./pages/AboutPage";
import PrivacyPage from "./pages/PrivacyPage";
import TermsPage from "./pages/TermsPage";
import NoPage from "./pages/NoPage";
//import { authEvents } from "./events/authEvents";
import HomePage from "./pages/HomePage";
import { ToastContainer } from "react-toastify";
import AdminPage from "./pages/AdminPage";
import IconsPreviewPage from "./pages/IconsPreviewPage";

function App() {
  return (
    <>
      <ToastContainer
        className="p-4 p-sm-0"
        position="top-center"
        autoClose={3000}
        theme="dark"
      />
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Layout />}>
            <Route index element={<HomePage />} />
            <Route path="web" element={<AdminPage />} />

            <Route path="about" element={<AboutPage />} />
            <Route path="privacy" element={<PrivacyPage />} />
            <Route path="terms" element={<TermsPage />} />

            {/* Galería de iconos. import.meta.env.DEV es una constante que
                Vite sustituye por false al construir, así que la rama entera
                (y el import de IconsPreviewPage) se elimina del bundle de
                producción por tree-shaking. */}
            {import.meta.env.DEV && (
              <Route path="dev/iconos" element={<IconsPreviewPage />} />
            )}

            <Route path="*" element={<NoPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </>
  );
}

export default App;
