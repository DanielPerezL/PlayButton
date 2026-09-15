import React, { useState } from "react";

interface LoginMenuProps {
  onSubmit: (username: string, password: string) => Promise<void>;
}

const LoginMenu: React.FC<LoginMenuProps> = ({ onSubmit }) => {
  const [username, setUsername] = useState<string>("");
  const [password, setPassword] = useState<string>("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      await onSubmit(username, password);
    } catch (err: any) {
      if (err instanceof Error) {
        setError(err.message || "Error al iniciar sesión.");
      } else {
        setError("Error al iniciar sesión.");
      }
    } finally {
      setLoading(false);
      setUsername("");
      setPassword("");
    }
  };

  return (
    // text-start porque la tarjeta que lo envuelve va centrada y, sin esto, las
    // etiquetas del formulario heredan el centrado y quedan flotando.
    <form onSubmit={handleSubmit} className="text-start">
      <div className="mb-3">
        <label htmlFor="username" className="form-label fw-semibold">
          Nombre de usuario
        </label>
        <input
          type="text"
          className="form-control"
          id="username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          required
          autoComplete="username"
          placeholder="ej: admin123"
        />
      </div>

      <div className="mb-4">
        <label htmlFor="password" className="form-label fw-semibold">
          Contraseña
        </label>
        <input
          type="password"
          className="form-control"
          id="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
          autoComplete="current-password"
          placeholder="********"
        />
      </div>

      <button
        type="submit"
        className="btn btn-primary btn-lg w-100"
        disabled={loading}
      >
        {loading ? "Cargando..." : "Iniciar sesión"}
      </button>

      {error && (
        <div className="alert alert-danger mt-3 mb-0" role="alert">
          {error}
        </div>
      )}
    </form>
  );
};

export default LoginMenu;
