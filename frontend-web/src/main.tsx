import { StrictMode } from "react";
import { createRoot } from "react-dom/client";

//COMPILADO CON: sass -q src/scss/custom.scss src/css/customBootstrap/custom.css
import "./css/customBootstrap/custom.css";

import "bootstrap/dist/js/bootstrap.bundle.min.js";
import "./css/index.css";

import App from "./App.tsx";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <App />
  </StrictMode>
);
