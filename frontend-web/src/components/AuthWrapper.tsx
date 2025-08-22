import React, { ReactNode } from "react";
import AccessMenu from "../components/AccessMenu";
import { isLoggedIn } from "../services/apiService";

interface AuthWrapperProps {
    children: ReactNode;
}

//Renderiza el contenido protegido solo si el usuario está autenticado
const AuthWrapper: React.FC<AuthWrapperProps> = ({ children }) => {
    if (!isLoggedIn()) {
        return <AccessMenu />;
    }

    return <>{children}</>;
};

export default AuthWrapper;
