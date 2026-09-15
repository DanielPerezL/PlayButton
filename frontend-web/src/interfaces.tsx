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

export interface Song {
  id: string;
  name: string;
  shown_zenn: boolean;
}

export interface GetSongsResponse {
  songs: Song[];
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
