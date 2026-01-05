import { isLoggedIn } from "./apiService";
import { showAlertOutsideReact } from "../services/alertContext";
import AsyncStorage from "@react-native-async-storage/async-storage";

const access_token = async () => await AsyncStorage.getItem("access_token");
const MAX_RETRIES = 10;
const RETRY_DELAY = 1500;

const FETCH_TIMEOUT = 10000;
const MAX_CONCURRENT_FETCHES = 3;
let activeFetches = 0;
const NETWORK_FAILURE_THRESHOLD = 3;

let networkState = "CLOSED";
let consecutiveNetworkFailures = 0;
let openedAt = 0;
let halfOpenInFlight = false;
const OPEN_TIMEOUT = 5000;

// Custom fetch para manejar errores de token JWT
export const customFetch = async (
  url,
  options = {},
  maxRetries = MAX_RETRIES,
  retryDelay = RETRY_DELAY
) => {
  if (!canAttemptFetch()) {
    return null;
  }

  // Esperar si hay demasiadas solicitudes activas
  while (activeFetches >= MAX_CONCURRENT_FETCHES) {
    await new Promise((res) => setTimeout(res, RETRY_DELAY));
  }

  activeFetches++;
  let attempts = 1;
  let response;

  try {
    response = await _customFetch(url, { ...options }, attempts);

    if (response === undefined) return response;

    while (attempts < maxRetries && response === null) {
      await new Promise((res) => setTimeout(res, retryDelay));
      response = await _customFetch(url, { ...options }, attempts);

      if (response === undefined) return undefined;

      attempts++;
      console.warn(`Attempt ${attempts} failed.`);
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

export const getNetworkState = () => {
  return networkState;
};

export const setNetworkState = (state) => {
  networkState = state;
  if (state === "OPEN") {
    openedAt = Date.now();
  }
  console.info(`[NETWORK] Circuit ${state} (manually set)`);
};

const _customFetch = async (url, options = {}, attempt = 0) => {
  if (!canAttemptFetch()) {
    return null;
  }
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

    //await new Promise((res) => setTimeout(res, 5000));
    const response = await fetch(url, options);
    clearTimeout(id);
    markNetworkSuccess();

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
    if (error?.name === "AbortError") markNetworkFailure();
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

const canAttemptFetch = () => {
  if (networkState === "CLOSED") return true;

  if (networkState === "OPEN") {
    if (Date.now() - openedAt > OPEN_TIMEOUT) {
      networkState = "HALF_OPEN";
      halfOpenInFlight = false;
      console.warn("[NETWORK] Circuit HALF_OPEN");
      return true;
    }
    return false;
  }

  if (networkState === "HALF_OPEN") {
    if (halfOpenInFlight) return false;
    halfOpenInFlight = true;
    return true;
  }

  return false;
};

const markNetworkFailure = () => {
  consecutiveNetworkFailures++;

  if (networkState === "HALF_OPEN") {
    networkState = "OPEN";
    openedAt = Date.now();
    halfOpenInFlight = false;
    console.error("[NETWORK] HALF_OPEN failed → OPEN");
    return;
  }

  if (
    networkState === "CLOSED" &&
    consecutiveNetworkFailures >= NETWORK_FAILURE_THRESHOLD
  ) {
    networkState = "OPEN";
    openedAt = Date.now();
    console.error("[NETWORK] Circuit OPEN");
  }
};

const markNetworkSuccess = () => {
  consecutiveNetworkFailures = 0;

  if (networkState === "HALF_OPEN") {
    networkState = "CLOSED";
    halfOpenInFlight = false;
    console.info("[NETWORK] HALF_OPEN success → CLOSED");
  }
};
