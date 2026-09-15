import { authEvents } from "../events/authEvents";
import {
  ErrorResponse,
  GetArtistsResponse,
  GetSongsResponse,
  GetSuggestionsResponse,
  GetUsersResponse,
  LoginResponse,
} from "../interfaces";

const PRODUCCION = import.meta.env.PROD;
let BASE_URL: string;
if (PRODUCCION) {
  BASE_URL = `${window.location.protocol}/api`;
} else {
  BASE_URL = `http://${window.location.hostname}:5000/api`;
}

export const isLoggedIn = (): boolean => {
  return localStorage.getItem("isLoggedIn") === "true";
};

export const getLoggedUserId = (): string | null => {
  return localStorage.getItem("loggedUserId");
};

export const getToken = (): string | null => {
  return localStorage.getItem("access_token");
};

export const isLoggedUserAdmin = (): boolean => {
  return getLoggedUserId() === "1";
};

export const login = async (
  username: string,
  password: string
): Promise<void> => {
  let response: Response;
  try {
    response = await fetch(`${BASE_URL}/auth/login`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        nickname: username, // el backend espera nickname
        password: password,
      }),
    });
  } catch {
    throw new Error("Error al contactar con el servidor");
  }

  console.log(response);

  if (!response.ok) {
    throw new Error("Credenciales incorrectas");
  }

  const responseData: LoginResponse = await response.json();

  localStorage.setItem("isLoggedIn", "true");
  localStorage.setItem("loggedUserId", responseData.user_id);
  localStorage.setItem("access_token", responseData.access_token);
  authEvents.emit("login");
};

export const logout = () => {
  if (!isLoggedIn()) return;
  localStorage.removeItem("isLoggedIn");
  localStorage.removeItem("loggedUserId");
  localStorage.removeItem("roles");
  localStorage.removeItem("access_token");
  authEvents.emit("logout");
};

export const getSongs = async (
  offset = 0,
  limit = 20,
  query?: string
): Promise<GetSongsResponse | null> => {
  const params = new URLSearchParams({
    offset: String(offset),
    limit: String(limit),
    details: "true",
  });
  // El backend busca en el título y en el nombre de los artistas.
  if (query) params.append("q", query);
  const response = await customFetch(`${BASE_URL}/songs?${params.toString()}`);
  return response ? await response.json() : null;
};

export const getArtists = async (
  offset = 0,
  limit = 20,
  search?: string
): Promise<GetArtistsResponse | null> => {
  const params = new URLSearchParams({
    offset: String(offset),
    limit: String(limit),
  });
  if (search) params.append("search", search);
  const response = await customFetch(`${BASE_URL}/artists?${params.toString()}`);
  return response ? await response.json() : null;
};

export const renameArtist = async (id: string, name: string): Promise<void> => {
  const response = await customFetch(`${BASE_URL}/artists/${id}`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ name }),
  });
  await throwIfFailed(response, "renombrar artista");
};

/**
 * Sube la portada de una canción, artista o playlist. Las tres rutas tienen la
 * misma forma, así que el recurso viaja como parte del camino.
 */
export const setImage = async (
  resource: "songs" | "artists" | "playlists",
  id: string,
  image: File
): Promise<void> => {
  const formData = new FormData();
  formData.append("image", image);

  const response = await customFetch(`${BASE_URL}/${resource}/${id}/image`, {
    method: "PUT",
    body: formData,
  });
  await throwIfFailed(response, "subir la imagen");
};

export const deleteImage = async (
  resource: "songs" | "artists" | "playlists",
  id: string
): Promise<void> => {
  const response = await customFetch(`${BASE_URL}/${resource}/${id}/image`, {
    method: "DELETE",
  });
  await throwIfFailed(response, "quitar la imagen");
};

export const createSong = async (data: {
  title: string;
  artists: string[];
  mp3: File;
  shown_zenn?: boolean;
  normalize?: boolean;
}): Promise<string | null> => {
  const formData = new FormData();
  formData.append("title", data.title);
  // Como array JSON y no separados por comas: un artista puede llevarlas en
  // el nombre y el backend no tiene forma de saber cuáles separan.
  formData.append("artists", JSON.stringify(data.artists));
  formData.append("shown_zenn", data.shown_zenn ? "true" : "false");
  formData.append("normalize", data.normalize ? "true" : "false");
  formData.append("mp3", data.mp3);

  const response = await customFetch(`${BASE_URL}/songs`, {
    method: "POST",
    body: formData,
  });

  await throwIfFailed(response, "crear canción");

  // El id sale de la cabecera Location del 201, que es lo único que devuelve el
  // backend. Hace falta para colgarle la portada después de crearla.
  const location = response!.headers.get("Location");
  return location?.split("/").pop() ?? null;
};

export const deleteSong = async (id: string) => {
  const response = await customFetch(`${BASE_URL}/songs/${id}`, {
    method: "DELETE",
    headers: { "Content-Type": "application/json" },
  });

  await throwIfFailed(response, "eliminar canción");
};

export const updateSong = async (
  id: string,
  title: string,
  artists: string[],
  shown_zenn: boolean
) => {
  const response = await customFetch(`${BASE_URL}/songs/${id}`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ title, artists, shown_zenn }),
  });

  await throwIfFailed(response, "actualizar canción");
};

export const getUsers = async (
  offset = 0,
  limit = 20,
  name?: string
): Promise<GetUsersResponse | null> => {
  const params = new URLSearchParams({
    offset: String(offset),
    limit: String(limit),
  });
  if (name) params.append("name", name);

  const response = await customFetch(`${BASE_URL}/users?${params.toString()}`);
  return response ? await response.json() : null;
};

export const createUser = async (data: {
  nickname: string;
  password: string;
}): Promise<void> => {
  const response = await customFetch(`${BASE_URL}/users`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  await throwIfFailed(response, "crear usuario");
};

export const updateUserPassword = async (
  userId: string,
  newPassword: string
) => {
  const response = await customFetch(`${BASE_URL}/users/${userId}`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      new_password: newPassword,
    }),
  });
  await throwIfFailed(response, "actualizar contraseña");
};

export const deleteUser = async (id: string): Promise<void> => {
  const response = await customFetch(`${BASE_URL}/users/${id}`, {
    method: "DELETE",
    headers: { "Content-Type": "application/json" },
  });

  await throwIfFailed(response, "eliminar usuario");
};

export const getSuggestions = async (
  offset = 0,
  limit = 20
): Promise<GetSuggestionsResponse | null> => {
  const params = new URLSearchParams({
    offset: String(offset),
    limit: String(limit),
  });
  const response = await customFetch(
    `${BASE_URL}/suggestions?${params.toString()}`
  );
  return response ? await response.json() : null;
};

export const createSuggestion = async (
  songArtist: string,
  songName: string
) => {
  const fullName = `${songArtist.trim()} - ${songName.trim()}`;
  return await customFetch(`${BASE_URL}/suggestions`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ song_name: fullName }),
  });
};

export const deleteSuggestion = async (id: string): Promise<void> => {
  const response = await customFetch(`${BASE_URL}/suggestions/${id}`, {
    method: "DELETE",
  });

  await throwIfFailed(response, "eliminar sugerencia");
};

/**
 * Traduce la respuesta de una mutación a excepción. Cada endpoint repetía este
 * mismo bloque, con el texto de la acción como única diferencia.
 */
const throwIfFailed = async (response: Response | null, accion: string) => {
  if (!response) {
    throw new Error("No se pudo conectar con el servidor.");
  }
  if (!response.ok) {
    const errorData: ErrorResponse = await response.json();
    console.error(`Error al ${accion}:`, errorData);
    throw new Error(errorData?.message || `Error al ${accion}`);
  }
};

const customFetch = async (url: string, options: RequestInit = {}) => {
  if (!isLoggedIn()) {
    return null;
  }

  try {
    options.headers = {
      ...options.headers,
      Authorization: `Bearer ${getToken()}`,
    };

    const response = await fetch(url, options);
    if (!PRODUCCION) {
      await new Promise((resolve) => setTimeout(resolve, 1000));
    }

    if (response.status === 401 || response.status === 422) {
      logout();
      return null;
    }

    return response;
  } catch (error) {
    console.error("Error en customFetch:", error);
    return null;
  }
};
