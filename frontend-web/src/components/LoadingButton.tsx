import React from "react";

interface LoadingButtonProps {
  onClick: () => void | Promise<void>;
  loading: boolean;
  children: React.ReactNode;
  className?: string;
  disabled?: boolean;
}

const LoadingButton: React.FC<LoadingButtonProps> = ({
  onClick,
  loading,
  children,
  className = "btn btn-outline-primary",
  disabled = false,
}) => {
  return (
    <button
      className={`${className} d-flex align-items-center justify-content-center gap-2`}
      onClick={onClick}
      disabled={loading || disabled}
    >
      {loading && (
        <span
          className="spinner-border spinner-border-sm"
          role="status"
          aria-hidden="true"
        ></span>
      )}
      {children}
    </button>
  );
};

export default LoadingButton;
