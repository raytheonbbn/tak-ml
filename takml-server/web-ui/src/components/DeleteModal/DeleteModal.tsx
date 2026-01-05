/* Copyright 2025 RTX BBN Technologies */
import { faClose } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import "./DeleteModal.css";
import 'react-toastify/dist/ReactToastify.css';

export const DeleteModal = ({headerText, message, onDelete, onClose} : 
    {headerText : string, message : string, onDelete : () => void, onClose : () => void}) => {
    return (
        <div id="delete-modal" 
            className="modal-backdrop"
        >
            <div 
                className="modal-content flex make-column border" 
                onClick={e =>
                    // Do not close modal if anything inside modal content is clicked
                    e.stopPropagation()
                }
            >
                <div className="modal-header flex flex-space-between">
                    <span>{headerText}</span>
                    <button id="modal-close-btn-id" 
                            className="modal-close-btn"
                            onClick={() => onClose()}>
                        <FontAwesomeIcon icon={faClose} />
                    </button>
                </div>
                <div id="deleteModalPromptId" className="delete-modal-prompt flex make-row margin-left">
                    {message}
                </div>
                <div className="flex make-row margin">
                    <div className="modal-action-buttons-container width-fifty-percent">
                        <button 
                            className="delete-item-modal-submit-button"
                            onClick={() => {
                                // Delete the item associated with this modal.
                                onDelete();
                                // Close the modal.
                                onClose();
                            }}
                        >
                                Yes
                        </button>
                        <button 
                            className="modal-cancel-button" 
                            onClick={() => 
                                onClose()
                            }
                        >
                                No
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default DeleteModal;