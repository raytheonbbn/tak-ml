/* Copyright 2025 RTX BBN Technologies */
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import "./DetailsButton.css";
import { faEdit } from "@fortawesome/free-solid-svg-icons";
import { useState } from "react";
import EditModal from "../EditModal/EditModal";
import { createPortal } from "react-dom";
import type { ModelDescriptor } from "../../types/Types";

export const DetailsButton = ({modelDescriptor, onUpdate, onSuccess}  : 
    {modelDescriptor : ModelDescriptor, onUpdate : (model : ModelDescriptor, file : File|null) => void, onSuccess : (msg : string) => void}) => {
    
    const [showEditModal, setShowEditModal] = useState(false);

    return (
        <>
            <button title="details" className="details-button" onClick={() => setShowEditModal(true)}>
                <FontAwesomeIcon icon={faEdit} />
            </button>
            {showEditModal && createPortal(
                <EditModal
                    modelDescriptor={modelDescriptor}
                    onUpdate={onUpdate}
                    onSuccess={onSuccess}
                    onClose={() => setShowEditModal(false)}
                />,
                document.body
            )}
        </>
    );
}

export default DetailsButton;
