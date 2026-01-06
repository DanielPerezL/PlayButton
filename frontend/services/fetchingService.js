import { isLoggedIn } from "./apiService";
import { showAlertOutsideReact } from "../services/alertContext";
import AsyncStorage from "@react-native-async-storage/async-storage";

const access_token = async () => await AsyncStorage.getItem("access_token");
const MAX_RETRIES = 10;
const RETRY_DELAY = 1000;

const FETCH_TIMEOUT = 10000;
const MAX_CONCURRENT_FETCHES = 3;
let activeFetches = 0;

// Custom fetch para manejar errores de token JWT
export const customFetch = async (
  url,
  options = {},
  maxRetries = MAX_RETRIES,
  retryDelay = RETRY_DELAY
) => {
  // Esperar si hay demasiadas solicitudes activas
  while (activeFetches >= MAX_CONCURRENT_FETCHES) {
    await new Promise((res) => setTimeout(res, RETRY_DELAY * activeFetches));
  }

  activeFetches++;
  let attempts = 1;
  let response;

  try {
    response = await _customFetch(url, { ...options }, attempts);

    if (response === undefined) return response;

    while (attempts < maxRetries && response === null) {
      if (__DEV__) console.warn(`Attempt ${attempts} failed.`);

      await new Promise((res) => setTimeout(res, retryDelay));
      response = await _customFetch(url, { ...options }, attempts);
      if (response === undefined) return undefined;
      attempts++;
    }

    if (response === null) {
      showAlertOutsideReact(
        "Error de Conexión",
        "Hubo un problema al conectar con el servidor. Inténtalo nuevamente."
      );
      return null;
    }

    return response;
  } finally {
    activeFetches--; // liberar slot de fetch
  }
};

const _customFetch = async (url, options = {}, attempt = 0) => {
  const logged = await isLoggedIn();
  if (!logged) return undefined;

  const controller = new AbortController();
  const id = setTimeout(() => controller.abort(), FETCH_TIMEOUT);

  try {
    const token = await access_token();
    options.headers = {
      ...options.headers,
      Authorization: `Bearer ${token}`,
    };
    options.signal = controller.signal;

    const response = await fetch(url, options);
    clearTimeout(id);

    if (!response.ok) {
      console.error("peticion con error: ", response.status);
      console.error(await response.text());
      if (response.status === 401 || response.status === 422) {
        await logout();
        showAlertOutsideReact(
          "Sesión Expirada",
          "Por favor inicia sesión nuevamente."
        );
        return undefined;
      }
      return null;
    }
    return response;
  } catch (error) {
    //console.error("Error en customFetch:", error);
    clearTimeout(id);
    if (__DEV__) {
      const message =
        "Hubo un problema al conectar con el servidor.\n\n" +
        "📡 Request:\n" +
        `• URL: ${url}\n` +
        `• Método: ${options?.method || "GET"}\n` +
        `• Opciones: ${JSON.stringify(options)}\n` +
        `• Timeout: ${FETCH_TIMEOUT} ms\n` +
        `• Intento: ${attempt} \n\n` +
        "⚠️ Error:\n" +
        `• Tipo: ${error?.name || "desconocido"}\n` +
        `• Mensaje: ${error?.message || "sin mensaje"}\n\n` +
        (error?.name === "AbortError" ? "⏱ Timeout alcanzado.\n\n" : "") +
        "Inténtalo nuevamente.";

      showAlertOutsideReact("Error de Conexión", message);
    }
    return null;
  }
};
