import icon from "../assets/icon.png";
import {
  DownloadIcon,
  GithubIcon,
  PhoneIcon,
  PlaylistIcon,
  ServerIcon,
  ShieldIcon,
  ShuffleIcon,
  UploadIcon,
  WavesIcon,
} from "../components/icons/Icons";

const REPO = "https://github.com/DanielPerezL/PlayButton";
const INSTALL_GUIDE = `${REPO}/blob/main/INSTALL.md`;
const PLAY_STORE =
  "https://play.google.com/store/apps/details?id=com.zice.playbutton";

const features = [
  {
    icon: <ShieldIcon size="1.35em" />,
    title: "Servidor privado y seguro",
    text: "Despliega tu propio servidor o conéctate al de tu comunidad. Tus ficheros y tus usuarios no salen de donde tú decidas.",
  },
  {
    icon: <WavesIcon size="1.35em" />,
    title: "Sonido uniforme",
    text: "La normalización de audio ajusta cada pista automáticamente, así que no tienes que tocar el volumen entre una y otra.",
  },
  {
    icon: <PlaylistIcon size="1.35em" />,
    title: "Listas a tu manera",
    text: "Crea y administra tus playlists para organizar tus grabaciones y escucharlas exactamente como quieras.",
  },
];

const steps = [
  {
    icon: <ServerIcon />,
    title: "Despliega el servidor",
    text: "Con Docker Compose y la guía de despliegue, tu instancia queda funcionando en unos minutos.",
  },
  {
    icon: <UploadIcon />,
    title: "Sube tus grabaciones",
    text: "Desde el panel de administración añades tus MP3, das de alta usuarios y decides qué entra en cada rotación.",
  },
  {
    icon: <PhoneIcon />,
    title: "Escucha donde quieras",
    text: "Instala la app en tu móvil, apunta a tu servidor e inicia sesión. Tu biblioteca te acompaña.",
  },
];

const HomePage = () => {
  return (
    <>
      {/* Hero */}
      <section className="pb-hero">
        <div className="container">
          <img
            src={icon}
            alt=""
            className="pb-hero__mark"
            width={320}
            height={320}
          />
          <h1 className="pb-hero__title">
            Tu biblioteca,{" "}
            <span className="pb-gradient-text">en tu propio servidor</span>
          </h1>
          <p className="pb-hero__lead">
            PlayButton es un reproductor de audio en streaming que se autoaloja.
            Despliega tu servidor privado y escucha tus grabaciones desde
            cualquier lugar, sin intermediarios.
          </p>
          <div className="d-flex justify-content-center gap-3 flex-wrap">
            <a
              href={PLAY_STORE}
              target="_blank"
              rel="noopener noreferrer"
              className="btn btn-primary btn-lg d-inline-flex align-items-center gap-2"
            >
              <DownloadIcon /> Descargar app móvil
            </a>
            <a
              href={REPO}
              target="_blank"
              rel="noopener noreferrer"
              className="btn btn-outline-light btn-lg d-inline-flex align-items-center gap-2"
            >
              <GithubIcon /> Ver en GitHub
            </a>
          </div>
        </div>
      </section>

      {/* Características */}
      <section className="pb-section" id="caracteristicas">
        <div className="container">
          <div className="text-center mb-5">
            <span className="pb-section__eyebrow">Por qué PlayButton</span>
            <h2 className="pb-section__title">Todo bajo tu control</h2>
          </div>
          <div className="row g-4">
            {features.map((f) => (
              <div className="col-md-4" key={f.title}>
                <article className="pb-card">
                  <span className="pb-card__icon">{f.icon}</span>
                  <h3 className="h5 mb-2">{f.title}</h3>
                  <p className="mb-0" style={{ color: "var(--pb-text-2)" }}>
                    {f.text}
                  </p>
                </article>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Cómo funciona */}
      <section className="pb-section pt-0" id="como-funciona">
        <div className="container">
          <div className="text-center mb-5">
            <span className="pb-section__eyebrow">Cómo funciona</span>
            <h2 className="pb-section__title">Tres pasos y a escuchar</h2>
          </div>
          <div className="row g-4">
            {steps.map((s, i) => (
              <div className="col-md-4" key={s.title}>
                <div className="h-100">
                  <span className="pb-step__number">{i + 1}</span>
                  <h3 className="h5 mb-2 d-flex align-items-center gap-2">
                    <span style={{ color: "var(--pb-brand-400)" }}>
                      {s.icon}
                    </span>
                    {s.title}
                  </h3>
                  <p className="mb-0" style={{ color: "var(--pb-text-2)" }}>
                    {s.text}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Modo Zenn */}
      <section className="pb-section pt-0">
        <div className="container">
          <div className="pb-highlight">
            <div className="row align-items-center g-4">
              <div className="col-md-8">
                <span className="pb-section__eyebrow">Modo Zenn</span>
                <h2 className="pb-section__title mb-3">
                  Dale al play y ya está
                </h2>
                <p className="mb-0" style={{ color: "var(--pb-text-2)" }}>
                  A veces no quieres elegir. El modo Zenn es el que se abre por
                  defecto en la app: reproduce tu biblioteca en aleatorio, sin
                  seleccionar ninguna lista. Tú decides desde el panel qué
                  pistas entran en esa rotación.
                </p>
              </div>
              <div className="col-md-4 text-center">
                <span
                  className="d-inline-flex align-items-center justify-content-center"
                  style={{
                    width: "6rem",
                    height: "6rem",
                    fontSize: "2.5rem",
                    borderRadius: "50%",
                    color: "#fff",
                    background: "var(--pb-brand-gradient)",
                    boxShadow: "var(--pb-glow)",
                  }}
                  aria-hidden="true"
                >
                  <ShuffleIcon />
                </span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* CTA final */}
      <section className="pb-section pt-0">
        <div className="container text-center">
          <h2 className="pb-section__title">¿Montamos tu servidor?</h2>
          <p className="pb-section__lead mx-auto mb-4" style={{ maxWidth: "48ch" }}>
            El proyecto es software libre bajo licencia GNU GPL v3. La guía de
            despliegue te lleva de cero a tu instancia funcionando.
          </p>
          <div className="d-flex justify-content-center gap-3 flex-wrap">
            <a
              href={INSTALL_GUIDE}
              target="_blank"
              rel="noopener noreferrer"
              className="btn btn-primary btn-lg"
            >
              Guía de despliegue
            </a>
            <a
              href={REPO}
              target="_blank"
              rel="noopener noreferrer"
              className="btn btn-outline-light btn-lg"
            >
              Repositorio oficial
            </a>
          </div>
        </div>
      </section>
    </>
  );
};

export default HomePage;
