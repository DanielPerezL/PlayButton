import React from "react";
import { Link } from "react-router-dom";

const NoPage: React.FC = () => {
    return (
        <main className="container my-5 main-container">
            <div className="d-flex align-items-center justify-content-center">
                <div className="text-center">
                    <h1 className="display-1 fw-bold text-danger">404</h1>
                    <p className="fs-3">
                        <span className="text-danger">¡Vaya!</span> La página
                        que buscas no se encuentra disponible.
                    </p>
                    <p className="lead">
                        Es posible que haya sido eliminada o movida a otra
                        ubicación.
                    </p>
                    <Link
                        to="/"
                        className="btn btn-primary"
                        onClick={() => {
                            window.scrollTo(0, 0);
                        }}
                    >
                        Volver a inicio
                    </Link>
                </div>
            </div>
        </main>
    );
};

export default NoPage;
