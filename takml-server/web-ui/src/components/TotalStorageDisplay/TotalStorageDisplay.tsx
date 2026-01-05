/* Copyright 2025 RTX BBN Technologies */
import "./TotalStorageDisplay.css";

export const TotalStorageDisplay = ({total} : {total : number}) => {
    return (
        <div className="flex make-column model-stats-container">
            <div className="display-header">
                Total Storage
            </div>
            <div className="total-storage-value">
                {total} MB
            </div>
        </div>
    )
}