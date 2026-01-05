/* Copyright 2025 RTX BBN Technologies */
import { faX } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import "./RemovablePill.css";

export const RemovablePill = ({name, key, val, onDelete} : {name : string, key : string, val : any, onDelete : (val : any) => void}) => {
     return (
        <span key={key} className="margin-top margin-bottom add-item-modal-pill" onClick={() => onDelete(val)}>
                {name}
                <FontAwesomeIcon icon={faX} size="xs" className="add-item-modal-pill-close"/>
        </span>
     );
}

export default RemovablePill;