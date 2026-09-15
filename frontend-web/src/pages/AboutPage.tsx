const AboutPage = () => {
  return (
    <div className="container pb-section">
      <header className="text-center mb-5">
        <span className="pb-section__eyebrow">Acerca de</span>
        <h1 className="pb-section__title">Qué es PlayButton</h1>
      </header>

      <div className="pb-doc">
        <p className="fs-5">
          PlayButton permite desplegar tu propio servidor privado para gestionar
          tus ficheros de audio y tus usuarios. Desde la app móvil accedes a tu
          biblioteca vía streaming, creas playlists y escuchas tus grabaciones
          desde donde quieras.
        </p>
        <p>
          La idea es sencilla: darte flexibilidad y control total sobre tus
          ficheros de audio, sin depender de una plataforma ajena. El servidor es
          tuyo, los datos son tuyos y decides quién entra.
        </p>
        <p>
          El proyecto es software libre bajo licencia GNU GPL v3. Puedes
          consultar el código, desplegarlo por tu cuenta o contribuir en{" "}
          <a
            href="https://github.com/DanielPerezL/PlayButton"
            target="_blank"
            rel="noopener noreferrer"
          >
            el repositorio de GitHub
          </a>
          .
        </p>
      </div>
    </div>
  );
};

export default AboutPage;
