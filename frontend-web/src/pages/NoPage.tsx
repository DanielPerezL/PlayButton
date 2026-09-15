import React from "react";
import { Link } from "react-router-dom";
import icon from "../assets/icon.png";

const NoPage: React.FC = () => {
    return (
        <div className="container pb-section text-center">
            <img
                src={icon}
                alt=""
                className="pb-auth__mark"
                width={80}
                height={80}
            />
            <p
                className="fw-bold mb-2"
                style={{
                    fontSize: "clamp(3.5rem, 12vw, 6rem)",
                    lineHeight: 1,
                    fontFamily: "var(--pb-font-heading)",
                    color: "var(--pb-text-3)",
                }}
            >
                404
            </p>
            <h1 className="pb-section__title">Esta página no existe</h1>
            <p className="pb-section__lead mx-auto mb-4" style={{ maxWidth: "42ch" }}>
                Puede que la hayamos movido, o que el enlace que has seguido ya
                no sea válido.
            </p>
            <Link
                to="/"
                className="btn btn-primary btn-lg"
                onClick={() => window.scrollTo(0, 0)}
            >
                Volver a inicio
            </Link>
        </div>
    );
};

export default NoPage;
