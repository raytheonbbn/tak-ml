/* Copyright 2025 RTX BBN Technologies */
import "./TotalActiveModelsDisplay.css";

export const TotalActiveModelsDisplay = ({total} : {total : number}) => {
    return (
        <div className="flex make-column model-stats-container">
            <div className="display-header">
                Total Active Models
            </div>
            <div className="total-active-models-value">
                {total}
            </div>
        </div>
    )
}