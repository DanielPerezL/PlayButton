import { DeviceEventEmitter } from "react-native";

// Definir el evento
export const emitLoginStatusChange = (status) => {
    DeviceEventEmitter.emit("authStatusChanged", status);
};

// Exportar el EventEmitter
export default DeviceEventEmitter;
