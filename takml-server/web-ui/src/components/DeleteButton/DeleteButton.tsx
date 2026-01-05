/* Copyright 2025 RTX BBN Technologies */
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import "./DeleteButton.css";
import { faTrash } from "@fortawesome/free-solid-svg-icons";
import { useState } from "react";
import { createPortal } from "react-dom";
import DeleteModal from "../DeleteModal/DeleteModal";

export const DeleteButton = ({onDelete} : {onDelete : () => void}) => {
    const [showDeleteModal, setShowDeleteModal] = useState(false);

    const handleDelete = () => {
        onDelete();
        setShowDeleteModal(false);
    }

    return (
        <>
            <button title="delete model" className="delete-button" onClick={() => setShowDeleteModal(true)}>
                <FontAwesomeIcon icon={faTrash} />
            </button>
            {showDeleteModal && createPortal(
                <DeleteModal
                    headerText="Delete ML Model?"
                    message="Are you sure you want to delete this ML model?"
                    onDelete={handleDelete}
                    onClose={() => setShowDeleteModal(false)}
                />,
                document.body
            )}
        </>

    );
}

export default DeleteButton;