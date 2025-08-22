import React, { createContext, useState, useContext } from "react";
import CustomAlertModal from "../components/CustomAlertModal";

const AlertContext = createContext();

let globalShowAlert = null; // Variable global para exponer showAlert

export const AlertProvider = ({ children }) => {
  const [alertData, setAlertData] = useState({
    visible: false,
    title: "",
    message: "",
    buttons: [],
  });

  const showAlert = (title, message, buttons) => {
    setAlertData({
      visible: true,
      title,
      message,
      buttons: buttons || [
        {
          text: "Cerrar",
          style: "cancel",
        },
      ],
    });
  };

  // Asignamos la función a la variable global para acceso fuera de React
  globalShowAlert = showAlert;

  const hideAlert = () => {
    setAlertData((prev) => ({ ...prev, visible: false }));
  };

  return (
    <AlertContext.Provider value={{ showAlert, hideAlert }}>
      {children}
      <CustomAlertModal
        visible={alertData.visible}
        title={alertData.title}
        message={alertData.message}
        buttons={alertData.buttons}
        onRequestClose={hideAlert}
      />
    </AlertContext.Provider>
  );
};

// Hook para usar dentro de componentes React
export const useAlert = () => useContext(AlertContext);

// Función para usar fuera de React
export const showAlertOutsideReact = (title, message, buttons) => {
  if (globalShowAlert) {
    globalShowAlert(title, message, buttons);
  } else {
    console.warn("showAlert no está disponible todavía");
  }
};
