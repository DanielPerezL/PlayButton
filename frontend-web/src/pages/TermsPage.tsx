const TermsPage = () => {
  return (
    <main className="container my-5 main-container">
      <div className="text-center mb-4">
        <h1 className="display-4 text-primary">Términos de uso</h1>
        <p className="fs-5 fw-light">
          Antes de utilizar PlayButton para reproducir y compartir audio vía
          streaming, te presentamos algunas normas esenciales para asegurar una
          experiencia segura y confiable.
        </p>
      </div>
      <div className="row">
        <div className="col-md-8 offset-md-2">
          <ul className="list-group list-group-flush">
            <li className="list-group-item">
              <strong>1. Uso responsable:</strong> PlayButton es una herramienta
              para reproducir contenido autorizado y personal en servidores
              privados. No debe usarse para distribuir contenido ilegal o
              infringir derechos de terceros.
            </li>
            <li className="list-group-item">
              <strong>2. Privacidad y seguridad:</strong> Eres responsable de la
              configuración y seguridad de tu servidor privado y del acceso que
              permitas a otros usuarios.
            </li>
            <li className="list-group-item">
              <strong>3. Respeto a los derechos de autor:</strong> Solo debes
              subir y compartir contenido del cual poseas los derechos o
              permisos necesarios.
            </li>
            <li className="list-group-item">
              <strong>4. Limitaciones de responsabilidad:</strong> PlayButton no
              se responsabiliza por el contenido alojado en servidores privados
              ni por su uso por parte de terceros.
            </li>
          </ul>
          <div className="mt-4">
            <p className="fs-5">
              Al usar PlayButton aceptas estos términos, contribuyendo a una
              experiencia segura, legal y respetuosa para todos los usuarios.
            </p>
          </div>
        </div>
      </div>
    </main>
  );
};

export default TermsPage;
