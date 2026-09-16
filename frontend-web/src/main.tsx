import { StrictMode } from "react";
import { createRoot } from "react-dom/client";

// Punto de entrada único de estilos: Vite compila el SCSS y de ahí sale
// Bootstrap, los tokens de marca, el CSS propio y los overrides de terceros,
// en ese orden. No añadir más imports de CSS aquí ni en los componentes.
import "./styles/main.scss";

import App from "./App.tsx";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <App />
  </StrictMode>
);
