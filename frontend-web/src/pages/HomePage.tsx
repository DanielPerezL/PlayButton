const HomePage = () => {
  return (
    <main className="container my-5 main-container text-center">
      <section className="py-4">
        <h1 className="display-3 fw-bold text-primary mb-4">PlayButton</h1>
        <p className="lead fs-4 mb-4">
          Tu reproductor de audio en streaming, <br />
          despliega tu propio servidor privado y reproduce tus grabaciones desde
          cualquier lugar.
        </p>
        <div className="d-flex justify-content-center gap-3 mb-4 flex-wrap">
          <a
            href="https://github.com/DanielPerezL/PlayButton"
            target="_blank"
            rel="noopener noreferrer"
            className="btn btn-lg btn-outline-primary"
          >
            Repositorio Oficial
          </a>
          <a
            href="https://github.com/DanielPerezL/PlayButton/blob/main/INSTALL.md"
            target="_blank"
            rel="noopener noreferrer"
            className="btn btn-lg btn-outline-primary"
          >
            Guía de Despliegue
          </a>
        </div>
        <div className="d-flex justify-content-center mb-5">
          <a
            href="https://github.com/DanielPerezL/PlayButton/releases/latest"
            target="_blank"
            rel="noopener noreferrer"
            className="btn btn-lg btn-success text-white"
          >
            Descargar App Móvil
          </a>
        </div>
      </section>

      <section className="px-4">
        <h2 className="mb-3 text-primary">¿Por qué elegir PlayButton?</h2>
        <div className="row text-start">
          <div className="col-md-4 mb-4">
            <h4 className="text-primary">Servidor privado y seguro</h4>
            <p>
              Despliega tu propio servidor o conéctate al de tu comunidad para
              tener control total de tus ficheros y usuarios.
            </p>
          </div>
          <div className="col-md-4 mb-4">
            <h4 className="text-primary">Sonido uniforme</h4>
            <p>
              Gracias al sistema de normalización de audio, cada pista se ajusta
              automáticamente para mantener un volumen equilibrado.
            </p>
          </div>
          <div className="col-md-4 mb-4">
            <h4 className="text-primary">
              Listas de reproducción personalizadas
            </h4>
            <p>
              Crea, administra y comparte tus playlists para disfrutar tus
              grabaciones a tu manera.
            </p>
          </div>
        </div>
      </section>
    </main>
  );
};

export default HomePage;
