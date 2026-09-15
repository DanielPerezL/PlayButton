// Galería de los iconos de components/icons/Icons.tsx.
//
// Solo se monta en desarrollo (ver la guarda import.meta.env.DEV en App.tsx),
// así que Vite la elimina del bundle de producción.
//
// Los iconos NO se listan a mano: se leen de los exports del propio módulo con
// `import * as Icons`. Al añadir un icono nuevo aparece aquí solo, sin tocar
// este fichero.

import { useState } from "react";
import * as Icons from "../components/icons/Icons";

type IconComponent = (props: {
    size?: string | number;
    className?: string;
}) => React.ReactElement;

const entries = Object.entries(Icons)
    .filter(([name]) => name.endsWith("Icon"))
    .sort(([a], [b]) => a.localeCompare(b)) as [string, IconComponent][];

const SIZES = ["1rem", "1.5rem", "2rem", "3rem"];

const COLORS = [
    ["Texto principal", "var(--pb-text-1)"],
    ["Texto atenuado", "var(--pb-text-3)"],
    ["Acento de marca", "var(--pb-brand-400)"],
    ["Peligro", "var(--pb-danger-text)"],
];

const IconsPreviewPage = () => {
    const [size, setSize] = useState("2rem");
    const [color, setColor] = useState("var(--pb-text-1)");
    const [copied, setCopied] = useState<string | null>(null);

    const copy = async (name: string) => {
        await navigator.clipboard.writeText(`<${name} />`);
        setCopied(name);
        setTimeout(() => setCopied(null), 1200);
    };

    return (
        <div className="container pb-section">
            <header className="mb-4">
                <span className="pb-section__eyebrow">Solo desarrollo</span>
                <h1 className="pb-section__title mb-2">
                    Iconos ({entries.length})
                </h1>
                <p className="pb-section__lead mb-0">
                    Se leen de <code>components/icons/Icons.tsx</code>. Heredan{" "}
                    <code>currentColor</code> y escalan con <code>size</code>.
                    Haz clic en uno para copiar su etiqueta JSX.
                </p>
            </header>

            {/* Controles */}
            <div className="pb-panel mb-4">
                <div className="row g-4">
                    <div className="col-sm-6">
                        <label className="form-label fw-semibold d-block" htmlFor="size">
                            Tamaño
                        </label>
                        <div className="pb-tabs" role="group" id="size">
                            {SIZES.map((s) => (
                                <button
                                    key={s}
                                    type="button"
                                    className="pb-tabs__item"
                                    aria-selected={size === s}
                                    onClick={() => setSize(s)}
                                >
                                    {s}
                                </button>
                            ))}
                        </div>
                    </div>
                    <div className="col-sm-6">
                        <label className="form-label fw-semibold d-block" htmlFor="color">
                            Color heredado
                        </label>
                        <div className="pb-tabs flex-wrap" role="group" id="color">
                            {COLORS.map(([label, value]) => (
                                <button
                                    key={value}
                                    type="button"
                                    className="pb-tabs__item d-inline-flex align-items-center gap-2"
                                    aria-selected={color === value}
                                    onClick={() => setColor(value)}
                                >
                                    <span
                                        aria-hidden="true"
                                        style={{
                                            width: ".7rem",
                                            height: ".7rem",
                                            borderRadius: "50%",
                                            backgroundColor: value,
                                        }}
                                    />
                                    {label}
                                </button>
                            ))}
                        </div>
                    </div>
                </div>
            </div>

            {/* Rejilla */}
            <div className="row g-3">
                {entries.map(([name, Icon]) => (
                    <div className="col-6 col-sm-4 col-md-3 col-lg-2" key={name}>
                        <button
                            type="button"
                            onClick={() => copy(name)}
                            title={`Copiar <${name} />`}
                            className="pb-card w-100 d-flex flex-column align-items-center justify-content-center gap-3 text-center border-0"
                            style={{
                                minHeight: "9rem",
                                padding: "1rem",
                                background: "var(--pb-surface-2)",
                                border: "1px solid var(--pb-border)",
                                cursor: "pointer",
                            }}
                        >
                            <span style={{ color, lineHeight: 0 }}>
                                <Icon size={size} />
                            </span>
                            <span
                                className="small font-monospace"
                                style={{
                                    color:
                                        copied === name
                                            ? "var(--pb-success-text)"
                                            : "var(--pb-text-3)",
                                    wordBreak: "break-word",
                                }}
                            >
                                {copied === name ? "¡copiado!" : name}
                            </span>
                        </button>
                    </div>
                ))}
            </div>

            {/* Prueba en contexto: dentro de botones y texto corrido */}
            <h2 className="pb-section__title mt-5 mb-3">En contexto</h2>
            <div className="pb-panel">
                <div className="d-flex flex-wrap gap-3 mb-4">
                    <button className="btn btn-primary d-inline-flex align-items-center gap-2">
                        <Icons.DownloadIcon /> Botón primario
                    </button>
                    <button className="btn btn-outline-light d-inline-flex align-items-center gap-2">
                        <Icons.GithubIcon /> Botón outline
                    </button>
                    <button className="btn btn-outline-danger d-inline-flex align-items-center gap-2">
                        <Icons.CloseIcon /> Botón de peligro
                    </button>
                </div>
                <p className="mb-0" style={{ color: "var(--pb-text-2)" }}>
                    Alineación con texto corrido: un <Icons.SearchIcon /> y un{" "}
                    <Icons.ShuffleIcon /> deben sentarse sobre la línea base sin
                    empujar el interlineado.
                </p>
            </div>
        </div>
    );
};

export default IconsPreviewPage;
