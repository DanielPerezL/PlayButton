import { DeviceEventEmitter } from "react-native";

// Función para emitir el evento cuando una nueva playlist es creada
export const emitPlaylistsModifiedEvent = () => {
  DeviceEventEmitter.emit("playlistsModified");
};

// Función para suscribirse al evento de creación de playlist
export const subscribeToPlaylistsModifiedEvent = (callback) => {
  return DeviceEventEmitter.addListener("playlistsModified", callback);
};

// Función para eliminar el listener del evento de creación de playlist
export const unsubscribeFromPlaylistsModifiedEvent = (listener) => {
  listener.remove();
};
