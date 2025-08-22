const AboutPage = () => {
  return (
    <main className="container my-5 main-container">
      <div className="text-center mb-4">
        <h1 className="display-4 text-primary">Acerca de PlayButton</h1>
        <p className="fs-5 fw-light">
          Descubre cómo nuestra plataforma revoluciona la experiencia de
          escuchar auido en streaming.
        </p>
      </div>
      <div className="col-md-8 offset-md-2 text-center">
        <p className="fs-5">
          PlayButton permite a los usuarios desplegar su propio servidor privado
          para gestionar sus archivos de audio y usuarios. De esta forma, puedes
          acceder a tus audios personalizada desde la app móvil vía streaming,
          crear playlists y disfrutar de una experiencia única y controlada.
          Nuestra misión es brindar flexibilidad y control total sobre tus
          ficheros de audio, fomentando comunidades privadas y personalizadas
          alrededor del audio.
        </p>
      </div>
    </main>
  );
};

export default AboutPage;
