import { isLoggedIn } from "./apiService";
import { showAlertOutsideReact } from "../services/alertContext";
import AsyncStorage from "@react-native-async-storage/async-storage";

const MAX_RETRIES = 10;
const RETRY_DELAY = 1500;
const FETCH_TIMEOUT = 10000;
const MAX_CONCURRENT_FETCHES = 3;
let activeFetches = 0;
let consecutiveNetworkFailures = 0;
const NETWORK_FAILURE_THRESHOLD = 3;
let networkState = "OK"; // OK | BROKEN

const access_token = async () => await AsyncStorage.getItem("access_token");

// Custom fetch para manejar errores de token JWT
export const customFetch = async (
  url,
  options = {},
  maxRetries = MAX_RETRIES,
  retryDelay = RETRY_DELAY
) => {
  if (networkState === "BROKEN") {
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

const _customFetch = async (url, options = {}, attempt = 0) => {
  if (networkState === "BROKEN") {
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

const waitForAllFetchesToFinish = async () => {
  while (activeFetches > 0) {
    await new Promise((res) => setTimeout(res, RETRY_DELAY));
  }
};

const markNetworkFailure = async () => {
  consecutiveNetworkFailures++;
  console.warn(
    `[NETWORK] failure ${consecutiveNetworkFailures}/${NETWORK_FAILURE_THRESHOLD}`
  );
  if (consecutiveNetworkFailures >= NETWORK_FAILURE_THRESHOLD) {
    networkState = "BROKEN";
    console.error("[NETWORK] Network marked as BROKEN");
  }
  await waitForAllFetchesToFinish();
  markNetworkSuccess();
};

const markNetworkSuccess = () => {
  if (networkState !== "OK") {
    console.info("[NETWORK] Network restored");
  }
  consecutiveNetworkFailures = 0;
  networkState = "OK";
};
