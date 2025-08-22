import { authEvents } from "../events/authEvents";
import {
  ErrorResponse,
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

  if (!responseData || !responseData.is_admin) {
    throw new Error("No tienes permisos de administrador");
  }

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
  name?: string
): Promise<GetSongsResponse | null> => {
  const params = new URLSearchParams({
    offset: String(offset),
    limit: String(limit),
    details: "true",
  });
  if (name) params.append("name", name);
  const response = await customFetch(`${BASE_URL}/songs?${params.toString()}`);
  return response ? await response.json() : null;
};

export const createSong = async (data: {
  name: string;
  mp3: File;
  shown_zenn?: boolean;
}) => {
  const formData = new FormData();
  formData.append("name", data.name);
  formData.append("mp3", data.mp3);
  formData.append("shown_zenn", data.shown_zenn ? "true" : "false");

  const response = await customFetch(`${BASE_URL}/songs`, {
    method: "POST",
    body: formData,
  });

  if (!response) {
    throw new Error("No se pudo conectar con el servidor.");
  }
  if (!response.ok) {
    const errorData: ErrorResponse = await response?.json();
    console.error("Error al crear canción:", errorData);
    throw new Error(errorData?.message || "Error al crear canción");
  }
};

export const deleteSong = async (id: string) => {
  const response = await customFetch(`${BASE_URL}/songs/${id}`, {
    method: "DELETE",
    headers: { "Content-Type": "application/json" },
  });

  if (!response) {
    throw new Error("No se pudo conectar con el servidor.");
  }
  if (!response.ok) {
    const errorData: ErrorResponse = await response?.json();
    console.error("Error al eliminar canción:", errorData);
    throw new Error(errorData?.message || "Error al eliminar canción");
  }
};

export const updateSong = async (
  id: string,
  newName: string,
  shown_zenn: boolean
) => {
  const response = await customFetch(`${BASE_URL}/songs/${id}`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      name: newName,
      shown_zenn: shown_zenn,
    }),
  });

  if (!response) {
    throw new Error("No se pudo conectar con el servidor.");
  }
  if (!response.ok) {
    const errorData: ErrorResponse = await response?.json();
    console.error("Error al actualizar canción:", errorData);
    throw new Error(errorData?.message || "Error al actualizar canción");
  }
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

  if (!response) {
    throw new Error("No se pudo conectar con el servidor.");
  }
  if (!response.ok) {
    const errorData: ErrorResponse = await response?.json();
    console.log("Error al crear usuario:", errorData);
    throw new Error(errorData.message);
  }
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
  if (!response) {
    throw new Error("No se pudo conectar con el servidor.");
  }
  if (!response.ok) {
    const errorData: ErrorResponse = await response?.json();
    console.error("Error al actualizar contraseña:", errorData);
    throw new Error(errorData?.message || "Error al actualizar contraseña");
  }
};

export const deleteUser = async (id: string): Promise<void> => {
  const response = await customFetch(`${BASE_URL}/users/${id}`, {
    method: "DELETE",
    headers: { "Content-Type": "application/json" },
  });

  if (!response) {
    throw new Error("No se pudo conectar con el servidor.");
  }
  if (!response.ok) {
    const errorData: ErrorResponse = await response?.json();
    console.error("Error al eliminar usuario:", errorData);
    throw new Error(errorData?.message || "Error al eliminar usuario");
  }
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

  if (!response) {
    throw new Error("No se pudo conectar con el servidor.");
  }
  if (!response.ok) {
    const errorData: ErrorResponse = await response?.json();
    console.error("Error al eliminar sugerencia:", errorData);
    throw new Error(errorData?.message || "Error al eliminar sugerencia");
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
