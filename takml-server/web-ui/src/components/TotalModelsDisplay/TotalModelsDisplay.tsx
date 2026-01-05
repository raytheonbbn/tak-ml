/* Copyright 2025 RTX BBN Technologies */
import "./TotalModelsDisplay.css";

export const TotalModelsDisplay = ({total} : {total : number}) => {
    return (
        <div className="flex make-column model-stats-container">
            <div className="display-header">
                Total Models
            </div>
            <div className="total-models-value">
                 {total}
            </div>
        </div>
    )
}