import AsyncStorage from "@react-native-async-storage/async-storage";
import { emitLoginStatusChange } from "../events/authEvent";
import { showAlertOutsideReact } from "../services/alertContext";
import { customFetch } from "../services/fetchingService";
import { emitFavPlaylistsModifiedEvent } from "../events/favPlaylistsModifiedEvent";

const API_URL_KEY = "API_BASE_URL";

export const getIsAdmin = async () => {
  const isAdmin = await AsyncStorage.getItem("is_admin");
  return isAdmin === "true";
};

const setIsAdmin = async (isAdmin) => {
  if (isAdmin) {
    await AsyncStorage.setItem("is_admin", "true");
  } else {
    await AsyncStorage.removeItem("is_admin");
  }
};

export const getHideAddSongAlert = async () => {
  const hideAlert = await AsyncStorage.getItem("hideAddSongAlert");
  return hideAlert === "true";
};

export const setHideAddSongAlert = async (hide) => {
  if (hide) {
    await AsyncStorage.setItem("hideAddSongAlert", "true");
  } else {
    await AsyncStorage.removeItem("hideAddSongAlert");
  }
};

export const getApiBaseUrl = async () => {
  const url = await AsyncStorage.getItem(API_URL_KEY);
  return url;
};

export const setApiBaseUrl = async (url) => {
  if (!url) {
    await AsyncStorage.setItem(API_URL_KEY, "");
    return;
  }

  if (!__DEV__) {
    if (!url.startsWith("https://")) {
      url = "https://" + url.replace(/^https?:\/\//, "");
    }
  } else {
    if (!url.startsWith("http://") && !url.startsWith("https://")) {
      url = "http://" + url.replace(/^https?:\/\//, "");
    }
  }

  if (!url.endsWith("/api")) {
    url = url.replace(/\/?$/, "/api");
  }
  await AsyncStorage.setItem(API_URL_KEY, url);
  return url;
};

export const isLoggedIn = async () =>
  (await AsyncStorage.getItem("isLoggedIn")) === "true";

export const getLoggedUserId = async () => {
  const userId = await AsyncStorage.getItem("loggedUserId");
  return userId ? Number(userId) : NaN;
};
export const login = async (nickname, password) => {
  try {
    const baseUrl = await getApiBaseUrl();
    const response = await fetch(`${baseUrl}/auth/login`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ nickname, password }),
    });

    if (!response.ok) {
      throw new Error("Login incorrecto");
    }

    const data = await response.json();
    await AsyncStorage.setItem("access_token", data.access_token);
    await AsyncStorage.setItem("isLoggedIn", "true");
    await AsyncStorage.setItem("loggedUserId", String(data.user_id));

    // Tambien se recibe data.is_admin (para poder actuar sobre todos los recursos)
    await setIsAdmin(data.is_admin || false);
    if (data.is_admin) {
      showAlertOutsideReact(
        "Bienvenido Administrador",
        "Has iniciado sesión como administrador. Puedes gestionar playlists de todos los usuarios.",
      );
    }
    emitLoginStatusChange(true);
  } catch (error) {
    throw new Error("Login incorrecto");
  }
};

export const logout = async () => {
  await AsyncStorage.removeItem("access_token");
  await AsyncStorage.removeItem("isLoggedIn");
  await AsyncStorage.removeItem("loggedUserId");
  await setIsAdmin(false);

  emitLoginStatusChange(false);
  return true;
};

export const changeUserPassword = async (currentPassword, newPassword) => {
  const baseUrl = await getApiBaseUrl();
  const userId = await getLoggedUserId(); // Obtenemos el ID del usuario logueado

  const response = await customFetch(
    `${baseUrl}/users/${userId}`,
    {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        current_password: currentPassword,
        new_password: newPassword,
      }),
    },
    (maxRetries = 2),
  );

  if (response && response.status === 204) {
    return true;
  } else {
    return false;
  }
};

export const deleteUserAccount = async () => {
  try {
    const baseUrl = await getApiBaseUrl();
    const userId = await getLoggedUserId();

    const response = await customFetch(`${baseUrl}/users/${userId}`, {
      method: "DELETE",
    });

    if (response && (response.status === 204 || response.status === 200)) {
      return true;
    }

    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || "No se pudo eliminar la cuenta.");
  } catch (error) {
    if (__DEV__) console.error("Error en deleteUserAccount:", error);
    throw error;
  }
};

export const getSignedSongUrl = async (songId) => {
  try {
    const baseUrl = await getApiBaseUrl();
    const response = await customFetch(`${baseUrl}/songs/${songId}/signed-url`);
    if (!response) {
      return null;
    }

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    const data = await response.json();

    if (data.mp3_url) {
      if (!__DEV__ && data.mp3_url.startsWith("http://")) {
        data.mp3_url = "https://" + data.mp3_url.slice(7); // Quitar 'http://' y agregar 'https://'
      }
      return data.mp3_url;
    } else {
      if (__DEV__) console.error("No 'mp3_url' field in response:", data);
      return null;
    }
  } catch (error) {
    if (__DEV__) console.error("Error fetching signed song URL:", error);
    return null;
  }
};

export const fetchSongsData = async (limit = 20) => {
  try {
    const baseUrl = await getApiBaseUrl();
    const response = await customFetch(`${baseUrl}/songs?limit=${limit}`);
    if (!response) return [];

    const data = await response.json();
    if (data.songs && Array.isArray(data.songs)) {
      return data.songs;
    } else {
      if (__DEV__) console.error("Invalid song data format:", data);
      return [];
    }
  } catch (error) {
    if (__DEV__) console.error("Error fetching song IDs:", error);
    return [];
  }
};

export const getCachedSongs = async (playlistId) => {
  try {
    const jsonValue = await AsyncStorage.getItem(`songs_${playlistId}`);
    return jsonValue != null ? JSON.parse(jsonValue) : null;
  } catch (e) {
    if (__DEV__) console.error("Error reading cached songs:", e);
    return null;
  }
};

export const setCachedSongs = async (playlistId, songs) => {
  try {
    const jsonValue = JSON.stringify(songs);
    await AsyncStorage.setItem(`songs_${playlistId}`, jsonValue);
  } catch (e) {
    if (__DEV__) console.error("Error saving songs to cache:", e);
  }
};

export const clearPlaylistCache = async () => {
  try {
    const keys = await AsyncStorage.getAllKeys();
    const songKeys = keys.filter((key) => key.startsWith("songs_"));
    if (songKeys.length > 0) {
      await AsyncStorage.multiRemove(songKeys);
    }
  } catch (e) {
    if (__DEV__) console.error("Error limpiando la caché:", e);
  }
};

export const clearPlaylistIdCache = async (playlistId) => {
  try {
    const cacheKey = `songs_${playlistId}`;
    await AsyncStorage.removeItem(cacheKey);
  } catch (err) {
    if (__DEV__) console.error("Error al eliminar caché de playlist:", err);
  }
};

export const searchSongsByName = async (name = "", offset = 0, limit = 50) => {
  try {
    const queryParams = new URLSearchParams({
      offset: offset.toString(),
      limit: limit.toString(),
    });

    if (name) {
      queryParams.append("name", name); // Solo agrega el parámetro "name" si no está vacío
    }

    const baseUrl = await getApiBaseUrl();
    const response = await customFetch(`${baseUrl}/songs?${queryParams}`);
    if (!response) return { songs: [], hasMore: false };
    const data = await response.json();

    if (data.songs && Array.isArray(data.songs)) {
      return { songs: data.songs, hasMore: data.has_more };
    } else {
      if (__DEV__)
        console.error(
          "Formato de datos inválido en la búsqueda de canciones:",
          data,
        );
      return { songs: [], hasMore: false };
    }
  } catch (error) {
    if (__DEV__) console.error("Error buscando canciones:", error);
    return { songs: [], hasMore: false };
  }
};

export const getPlaylistSongs = async (playlistId) => {
  try {
    const baseUrl = await getApiBaseUrl();
    const response = await customFetch(
      `${baseUrl}/playlists/${playlistId}/songs`,
      { method: "GET" },
    );
    if (!response) return null;
    if (!response.ok) {
      if (__DEV__)
        console.error(`Error fetching playlist songs: HTTP ${response.status}`);
      return null;
    }
    const data = await response.json();
    if (data.songs && Array.isArray(data.songs)) {
      return data.songs;
    } else {
      if (__DEV__) console.error("Invalid song data format:", data);
      return null;
    }
  } catch (error) {
    if (__DEV__) console.error("Error fetching playlist songs:", error);
    return null;
  }
};

export const fetchUserPlaylists = async (
  userId,
  offset = 0,
  limit = 10,
  search = "",
) => {
  try {
    const baseUrl = await getApiBaseUrl();
    // Construimos la URL con query params para filtro y paginación
    const url = `${baseUrl}/users/${userId}/playlists?offset=${offset}&limit=${limit}&search=${encodeURIComponent(search)}`;

    const response = await customFetch(url, {
      method: "GET",
      headers: { "Content-Type": "application/json" },
    });

    if (!response || !response.ok) {
      if (__DEV__)
        console.error(
          `Error fetching user playlists: HTTP ${response?.status}`,
        );
      return { playlists: [], has_more: false };
    }

    const data = await response.json();
    // Devolvemos el objeto completo para que el componente sepa si hay más
    return {
      playlists: data.playlists || [],
      has_more: data.has_more || false,
    };
  } catch (error) {
    if (__DEV__) console.error("Error fetching user playlists:", error);
    return { playlists: [], has_more: false };
  }
};

export const togglePlaylistFavorite = async (playlistId) => {
  try {
    const baseUrl = await getApiBaseUrl();
    const response = await customFetch(
      `${baseUrl}/playlists/${playlistId}/favorite`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
      },
      (maxRetries = 2),
    );

    if (!response || !response.ok) {
      if (__DEV__)
        console.error(`Error toggling favorite: HTTP ${response?.status}`);
      return null;
    }

    const data = await response.json();
    emitFavPlaylistsModifiedEvent();
    // Retornamos el nuevo contador que viene del backend: {"favorites_count": X}
    return data.favorites_count;
  } catch (error) {
    if (__DEV__) console.error("Error en togglePlaylistFavorite:", error);
    return null;
  }
};

export const fetchUserFavoritePlaylists = async (
  userId,
  offset = 0,
  limit = 10,
  search = "",
) => {
  try {
    const baseUrl = await getApiBaseUrl();

    // Si no viene userId, intentamos sacar el del logueado
    const targetUserId = userId || (await getLoggedUserId());

    if (!targetUserId) return { playlists: [], has_more: false };
    const url = `${baseUrl}/users/${targetUserId}/favorites?offset=${offset}&limit=${limit}&search=${encodeURIComponent(search)}`;
    const response = await customFetch(url, {
      method: "GET",
      headers: { "Content-Type": "application/json" },
    });

    if (!response || !response.ok) {
      if (__DEV__)
        console.error(`Error fetching favorites: HTTP ${response?.status}`);
      return { playlists: [], has_more: false };
    }

    const data = await response.json();
    return {
      playlists: data.playlists || [],
      has_more: data.has_more || false,
    };
  } catch (error) {
    if (__DEV__) console.error("Error en fetchUserFavoritePlaylists:", error);
    return { playlists: [], has_more: false };
  }
};

export const getAllPlaylists = async (offset = 0, limit = 10, search = "") => {
  try {
    const baseUrl = await getApiBaseUrl();
    const url = `${baseUrl}/playlists?offset=${offset}&limit=${limit}&search=${encodeURIComponent(search)}`;

    const response = await customFetch(url, { method: "GET" });

    if (!response || !response.ok) {
      if (__DEV__)
        console.error(
          `Error fetching public playlists: HTTP ${response?.status}`,
        );
      return { playlists: [], has_more: false };
    }
    const data = await response.json();
    return {
      playlists: data.playlists || [],
      has_more: data.has_more || false,
    };
  } catch (error) {
    if (__DEV__) console.error("Error fetching public playlists:", error);
    return { playlists: [], has_more: false };
  }
};

export const getAllArtists = async (offset = 0, limit = 10, search = "") => {
  try {
    const baseUrl = await getApiBaseUrl();
    const url = `${baseUrl}/artists?offset=${offset}&limit=${limit}&search=${encodeURIComponent(search)}`;

    const response = await customFetch(url, { method: "GET" });

    if (!response || !response.ok) {
      if (__DEV__)
        console.error(
          `Error fetching public playlists: HTTP ${response?.status}`,
        );
      return { playlists: [], has_more: false };
    }
    const data = await response.json();
    return {
      playlists: data.playlists || [],
      has_more: data.has_more || false,
    };
  } catch (error) {
    if (__DEV__) console.error("Error fetching public playlists:", error);
    return { playlists: [], has_more: false };
  }
};

export const createPlaylist = async (userId, name, isPublic = true) => {
  const data = { name, is_public: isPublic };
  try {
    const baseUrl = await getApiBaseUrl();
    const response = await customFetch(`${baseUrl}/users/${userId}/playlists`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(data),
    });

    if (!response) return false;
    if (!response.ok) {
      if (__DEV__)
        console.error(`Error creating playlist: HTTP ${response.status}`);
      return false;
    }

    const playlist = await response.json();
    return playlist;
  } catch (error) {
    if (__DEV__) console.error("Error creating playlist:", error);
    return false; // En caso de error, devuelve false
  }
};

export const deletePlaylist = async (playlistId) => {
  try {
    const baseUrl = await getApiBaseUrl();
    const response = await customFetch(`${baseUrl}/playlists/${playlistId}`, {
      method: "DELETE",
    });

    if (!response) return false;
    if (!response.ok) {
      if (__DEV__)
        console.error(`Error deleting playlist: HTTP ${response.status}`);
      return false;
    }
    return true;
  } catch (error) {
    if (__DEV__) console.error(`Error deleting playlist:`, error);
    return false;
  }
};

export const updatePlaylist = async (playlistId, newName, isPublic) => {
  try {
    const baseUrl = await getApiBaseUrl();
    const response = await customFetch(`${baseUrl}/playlists/${playlistId}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ name: newName, is_public: isPublic }),
    });

    if (!response) return false;
    if (!response.ok) {
      if (__DEV__)
        console.error(`Error updating playlist: HTTP ${response.status}`);
      return false;
    }
    return true;
  } catch (error) {
    if (__DEV__) console.error("Error updating playlist:", error);
    return false;
  }
};

export const addSongToPlaylist = async (playlistId, songId) => {
  try {
    const baseUrl = await getApiBaseUrl();
    const response = await customFetch(
      `${baseUrl}/playlists/${playlistId}/songs/${songId}`,
      { method: "POST" },
    );

    if (!response) return false;
    if (!response.ok) {
      if (__DEV__)
        console.error(`Error adding song to playlist: HTTP ${response.status}`);
      return false;
    }
    return true;
  } catch (error) {
    if (__DEV__) console.error(`Error adding song to playlist:`, error);
    return false;
  }
};

export const removeSongFromPlaylist = async (playlistId, songId) => {
  try {
    const baseUrl = await getApiBaseUrl();
    const response = await customFetch(
      `${baseUrl}/playlists/${playlistId}/songs/${songId}`,
      { method: "DELETE" },
    );

    if (!response) return false;
    if (!response.ok) {
      if (__DEV__)
        console.error(
          `Error removing song from playlist: HTTP ${response.status}`,
        );
      return false;
    }
    return true;
  } catch (error) {
    if (__DEV__) console.error("Error removing song from playlist:", error);
    return false;
  }
};

//Crear sugerencias
export const createSuggestion = async (songName, artistName) => {
  if (!songName || !artistName) {
    if (__DEV__)
      console.error("Invalid song or artist name:", songName, artistName);
    return false;
  }

  const fullName = `${artistName} - ${songName}`;

  try {
    const baseUrl = await getApiBaseUrl();
    const response = await customFetch(`${baseUrl}/suggestions`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ song_name: fullName }),
    });

    if (!response) return false;
    if (!response.ok) {
      if (__DEV__)
        console.error(
          `Error suggesting song "${fullName}": HTTP ${response.status}`,
        );
      return false;
    }
    return true;
  } catch (error) {
    if (__DEV__) console.error(`Error suggesting song "${fullName}":`, error);
    return false;
  }
};
