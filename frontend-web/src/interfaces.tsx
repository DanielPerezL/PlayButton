export interface LoginResponse {
  access_token: string;
  user_id: string;
  is_admin: boolean;
}

export interface ErrorResponse {
  error: string;
  message: string;
}

export interface User {
  id: string;
  nickname: string;
  password?: string;
}

export interface GetUsersResponse {
  users: User[];
  has_more: boolean;
}

export interface Artist {
  id: string;
  name: string;
}

export interface Song {
  id: string;
  title: string;
  artists: Artist[];
  shown_zenn: boolean;
  /** Portada que se muestra. Puede venir heredada del primer artista. */
  image_url: string | null;
  /** Portada propia. Null si la que se ve es la del artista. */
  own_image_url: string | null;
}

export interface GetSongsResponse {
  songs: Song[];
  has_more: boolean;
}

/** Artista del listado: trae con qué playlist se abre y cuánto tiene dentro. */
export interface ArtistSummary extends Artist {
  playlist_id: string | null;
  songs_count: number;
  favorites_count: number;
  is_favorite: boolean;
  image_url: string | null;
}

export interface GetArtistsResponse {
  artists: ArtistSummary[];
  has_more: boolean;
}

export interface Suggestion {
  id: string;
  song_name: string;
  // Fecha de la primera vez que se sugirio la cancion, en UTC (ISO 8601).
  created_at: string | null;
  suggested_by: User[];
}

export interface GetSuggestionsResponse {
  suggestions: Suggestion[];
  has_more: boolean;
}
