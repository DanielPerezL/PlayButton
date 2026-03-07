import { DeviceEventEmitter } from "react-native";

// Función para emitir el evento cuando una nueva playlist es creada
export const emitFavPlaylistsModifiedEvent = () => {
  DeviceEventEmitter.emit("favPlaylistsModified");
};

// Función para suscribirse al evento de creación de playlist
export const subscribeToFavPlaylistsModifiedEvent = (callback) => {
  return DeviceEventEmitter.addListener("favPlaylistsModified", callback);
};

// Función para eliminar el listener del evento de creación de playlist
export const unsubscribeFromFavPlaylistsModifiedEvent = (listener) => {
  listener.remove();
};
