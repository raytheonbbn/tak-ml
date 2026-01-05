/* Copyright 2025 RTX BBN Technologies */

export const ModelAccuracyDisplay = ({accuracy} : {accuracy : number} /*{modelDescriptor} : {modelDescriptor : ModelDescriptor}*/) => {
    if (accuracy >= 90) {
        return <p className="ninety-percent-accuracy">{accuracy}%</p>
    } else if (accuracy >= 60) {
        return <p className="sixty-percent-accuracy">{accuracy}%</p>
    } else if (accuracy >= 30) {
        return <p className="thirty-percent-accuracy">{accuracy}%</p>
    } else {
        return <p className="zero-percent-accuracy">{accuracy}%</p>
    }
}