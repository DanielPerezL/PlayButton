//import { useEffect, useState } from "react";
import "./css/App.css";
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

function App() {
  return (
    <>
      <ToastContainer
        className="p-4 p-sm-0"
        position="top-center"
        autoClose={3000}
      />
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Layout />}>
            <Route index element={<HomePage />} />
            <Route path="web" element={<AdminPage />} />

            <Route path="about" element={<AboutPage />} />
            <Route path="privacy" element={<PrivacyPage />} />
            <Route path="terms" element={<TermsPage />} />
            <Route path="*" element={<NoPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </>
  );
}

export default App;
