/* Copyright 2025 RTX BBN Technologies */
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import "./UploadModelButton.css";
import { faAdd } from "@fortawesome/free-solid-svg-icons";
import { useState } from "react";
import { createPortal } from "react-dom";
import type { ModelDescriptor } from "../../types/Types";
import UploadModelPopup from "../UploadModelPopup/UploadModelPopup";

export const UploadModelButton = ({onUpload} : {onUpload : (m : ModelDescriptor, selectedFile : File) => void}) => {
    const [showUploadModal, setShowUploadModal] = useState<boolean>(false);

    return (
        <>
            <button id="upload-modal-btn" title="Upload New Model" className="upload-model-btn" onClick={() => setShowUploadModal(true)}>
                <FontAwesomeIcon icon={faAdd} />
                Upload Model
            </button>
            {showUploadModal && createPortal(
                <UploadModelPopup
                    onUpload={(model : ModelDescriptor, selectedFile : File) => onUpload(model, selectedFile)}
                    onClose={() => setShowUploadModal(false)}
                />,
                    document.body
            )}
        </>
        
    );
}

export default UploadModelButton;