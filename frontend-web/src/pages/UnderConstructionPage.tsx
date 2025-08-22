import React from "react";
import { Link } from "react-router-dom";

const UnderConstruction: React.FC = () => {
    return (
        <main className="container my-5 main-container">
            <div className="d-flex align-items-center justify-content-center mx-5">
                <article className="text-center">
                    <h1 className="display-3 fw-bold text-warning">
                        ⚠️ En desarrollo
                    </h1>
                    <p className="fs-4">Estamos trabajando en ello</p>
                    <p className="lead">Página en construcción</p>
                    <Link
                        to="/"
                        className="btn btn-primary"
                        onClick={() => {
                            window.scrollTo(0, 0);
                        }}
                    >
                        Ir a inicio
                    </Link>
                </article>
            </div>
        </main>
    );
};

export default UnderConstruction;
