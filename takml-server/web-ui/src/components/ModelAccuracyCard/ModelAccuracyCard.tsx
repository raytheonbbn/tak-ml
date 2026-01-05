/* Copyright 2025 RTX BBN Technologies */

import "./ModelAccuracyCard.css";

const ModelAccuracyValue = ({accuracy} : {accuracy : number}) => {
    if (accuracy >= 90) {
        return <span className="font-xlarge ninety-percent-accuracy">{accuracy.toFixed(2)}%</span>
    } else if (accuracy >= 60) {
        return <span className="font-xlarge sixty-percent-accuracy">{accuracy.toFixed(2)}%</span>
    } else if (accuracy >= 30) {
        return <span className="font-xlarge thirty-percent-accuracy">{accuracy.toFixed(2)}%</span>
    } else {
        return <span className="font-xlarge zero-percent-accuracy">{accuracy.toFixed(2)}%</span>
    }
}

export const ModelAccuracyCard = ({numTestUsers, numAccuracyReports, accuracy} : {numTestUsers : number, numAccuracyReports : number, accuracy : number}) => {
    return (
        <>
            <div className="flex make-column model-feedback-stats-container">
                <div className="display-header">
                    <b>Reported Accuracy</b>
                </div>
                <div className="flex make-column flex-center margin-auto">
                    <ModelAccuracyValue accuracy={accuracy} />
                    {/* <div className="flex make-column flex-center width-max">
                        <div>False positive rate: <span className="error-rate">-%</span></div>
                        <div>False negative rate: <span className="error-rate">-%</span></div>
                    </div> */}
                </div>
                <div className="display-footer">
                    <div>Reported accuracy for the top result returned by the ML model. Based on <span className="num-accuracy-reports">{numAccuracyReports}</span> reports from <span className="num-test-users">{numTestUsers}</span> users.</div>
                </div>
            </div>
        </>
    );
}

export default ModelAccuracyCard;