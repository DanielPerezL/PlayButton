import React, { useState } from "react";
import { Button, Modal } from "react-bootstrap";

interface NeedConfirmButtonProps {
  title: string;
  message: string;
  className: string;
  onConfirm: () => void;
  children: React.ReactNode;
}

const NeedConfirmButton: React.FC<NeedConfirmButtonProps> = ({
  title,
  message,
  className,
  onConfirm,
  children,
}) => {
  const [showConfirmModal, setShowConfirmModal] = useState(false);

  const handleConfirm = () => {
    onConfirm();
    setShowConfirmModal(false);
  };

  const handleCancel = () => {
    setShowConfirmModal(false);
  };

  return (
    <>
      <button className={className} onClick={() => setShowConfirmModal(true)}>
        {children}
      </button>

      <Modal show={showConfirmModal} onHide={handleCancel} centered>
        <Modal.Header className="bg-light text-dark">
          <Modal.Title>{title}</Modal.Title>
        </Modal.Header>
        <Modal.Body className="bg-light">
          <p>{message}</p>
        </Modal.Body>
        <Modal.Footer className="bg-light">
          <Button variant="outline-secondary" onClick={handleCancel}>
            Cancelar
          </Button>
          <Button variant="outline-danger" onClick={handleConfirm}>
            Confirmar
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  );
};

export default NeedConfirmButton;
