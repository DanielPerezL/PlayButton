const PrivacyPage = () => {
  return (
    <main className="container my-5 main-container">
      <div className="text-center mb-4">
        <h1 className="display-4 text-primary">Política de Privacidad</h1>
        <p className="fs-5 fw-light">
          En PlayButton respetamos tu privacidad. No recopilamos ni utilizamos
          tus datos personales.
        </p>
      </div>
      <div className="col-md-8 offset-md-2">
        <p className="fs-5 mb-3">
          PlayButton es una plataforma que permite a los usuarios desplegar y
          conectar con sus propios servidores privados de streaming. Esto
          significa que no gestionamos, almacenamos ni utilizamos tus datos
          personales.
        </p>
        <ul className="fs-6">
          <li>
            <strong>Datos personales:</strong> No recopilamos información
            personal ya que tú o tu comunidad administran el servidor privado.
          </li>
          <li>
            <strong>Control total:</strong> Tú tienes control absoluto sobre los
            archivos y la configuración de tu servidor, y sobre quién puede
            acceder a él.
          </li>
          <li>
            <strong>Seguridad:</strong> La seguridad de tus datos depende de la
            configuración y medidas que implementes en tu propio servidor.
          </li>
          <li>
            <strong>Privacidad:</strong> PlayButton compone un ecosistema
            descentralizado que permite reproducir audios en via streaming
            gracias a servidores privados gestionados por la comunidad. No
            retiene, ni puede retener, información de sus usuarios.
          </li>
        </ul>

        <hr className="my-5" />
        <div className="bg-light p-4 rounded border">
          <h2 className="h4 text-primary mb-3">Eliminación de cuenta</h2>
          <p>
            Si deseas eliminar tu cuenta y todos los datos asociados, puedes
            hacerlo de forma sencilla a través de dos canales:
          </p>
          <ul className="mb-0">
            <li className="mb-2">
              <strong>Desde la App móvil:</strong> Inicia sesión, ve al apartado
              de <strong>Configuración</strong> y selecciona la última opción
              para eliminar cuenta.
            </li>
            <li>
              <strong>Desde la Web:</strong> Accede a la web de tu servidor,
              inicia sesión en tu perfil y encontrarás la opción para dar de
              baja tu cuenta definitivamente.
            </li>
          </ul>
        </div>

        <p className="fs-5 mt-4">
          En resumen: PlayButton respeta tu privacidad y no almacena ni utiliza
          tus datos personales. Cualquier duda o consulta, estamos a tu
          disposición.
        </p>
      </div>
    </main>
  );
};

export default PrivacyPage;
