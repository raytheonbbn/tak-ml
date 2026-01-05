/* Copyright 2025 RTX BBN Technologies */

import { ONE_BILLION, ONE_MILLION, ONE_THOUSAND } from "../../utilities/Constants";
import "./TotalInferenceCountCard.css";

const InferenceCount = ({inferenceCount} : {inferenceCount : number}) => {
    if (inferenceCount < ONE_THOUSAND) {
        return <span className="font-xlarge">{inferenceCount}</span>;
    } else if (inferenceCount >= ONE_THOUSAND && inferenceCount < ONE_MILLION) {
        return <span className="font-xlarge">{(inferenceCount / ONE_THOUSAND).toFixed(1)}K</span>
    } else if (inferenceCount >= ONE_MILLION && inferenceCount < ONE_BILLION) {
        return <span className="font-xlarge">{(inferenceCount / ONE_MILLION).toFixed(1)}M</span>
    } else {
        return <span className="font-xlarge">{(inferenceCount / ONE_BILLION).toFixed(1)}B</span>
    }
}

export const TotalInferenceCountCard = ({inferenceCount} : {inferenceCount : number}) => {
    return (
        <>
            <div className="flex make-column model-feedback-stats-container">
                <div className="display-header">
                    <b>Total Inference Count</b>
                </div>
                <div className="flex make-column flex-center margin-auto">
                    <InferenceCount inferenceCount={inferenceCount} />
                </div>
            </div>
        </>
    );
}

export default TotalInferenceCountCard;