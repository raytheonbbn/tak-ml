/* Copyright 2025 RTX BBN Technologies */

import { type FeedbackResponse, type ModelDescriptor, type TopReporter } from "../../types/Types";
import ModelAccuracyCard from "../ModelAccuracyCard/ModelAccuracyCard";
import TopFeedbackProvidersCard from "../TopFeedbackProvidersCard/TopFeedbackProvidersCard";
import { useEffect, useState } from "react";
import "./FeedbackPane.css";
import ErrorPane from "../ErrorPane/ErrorPane";
import TotalInferenceCountCard from "../TotalInferenceCountCard/TotalInferenceCountCard";

const TEST_TOP_REPORTERS : Array<TopReporter> = [
    {"name": "Brandon", "numReports": 40},
    {"name": "Madeline", "numReports": 20},
    {"name": "Ben", "numReports": 10},
    {"name": "Other", "numReports": 30}
];
const TEST_INFERENCE_COUNT : number = 9_600;

const MIN_FEEDBACK_COUNT : number = 1;
// const MAX_NUM_REPORTERS : number = 5;
const MIN_PERCENTAGE : number = 5;

export const FeedbackPane = ({modelDescriptor} : {modelDescriptor : ModelDescriptor}) => {
    const [numTestUsers, setNumTestUsers] = useState(0);
    const [numAccuracyReports, setNumAccuracyReports] = useState(0);
    const [accuracy, setAccuracy] = useState(0);
    const [topReporters, setTopReporters] = useState<Array<TopReporter>>([]);
    const [inferenceCount, setInferenceCount] = useState(0);

    const getFeedbackForModel = () => {
        if (import.meta.env.MODE === "production") {
            const modelName : string = encodeURIComponent(modelDescriptor.name);
            var url : string = `/model_feedback/get_feedback?modelName=${modelName}`;

            var modelVersion : string|null = null;
            if (modelDescriptor.version !== null && modelDescriptor.version !== undefined) {
                modelVersion = encodeURIComponent(modelDescriptor.version);
                url += `&modelVersion=${modelVersion}`;
            }

            console.log("Sending feedback API request. URL: ", url);

            fetch(url)
                .then(response => {
                    if (!response.ok) {
                        throw new Error("Unable to fetch feedback for model");
                    }
                    return response.json();
                })
                .then(json => {
                    console.log("Feedback for model: ", json);
                    if (json !== null && json !== undefined && json.length >= MIN_FEEDBACK_COUNT) {
                        updateDisplayFields(json);
                    }
                })
                .catch(error => {
                    console.error("Error fetching feedback for model: ", error);
                    // TODO - display "error" toast.
                });
        } else {
            // Display test feedback data when in debug mode.
            console.log("Not loading feedback from server");
            setNumTestUsers(25);
            setNumAccuracyReports(100);
            setAccuracy(95);
            setTopReporters(TEST_TOP_REPORTERS);
            setInferenceCount(TEST_INFERENCE_COUNT);
        }
    }

    const updateDisplayFields = (responseData : Array<FeedbackResponse>) => {
        var callsigns = new Set();
        var accurateCount = 0;
        var newTopReporters = new Map<string, number>();
        for (var i = 0; i < responseData.length; ++i) {
            if (responseData[i].callsign !== null && responseData[i].callsign !== undefined) {
                callsigns.add(responseData[i].callsign);
            } else {
                console.warn("Not including feedback report in test users count; callsign was null");
            }
            if (responseData[i].isCorrect) {
                accurateCount += 1;
            }
            if (!newTopReporters.has(responseData[i].callsign)) {
                newTopReporters.set(responseData[i].callsign, 1);
            } else {
                const prev : number = newTopReporters.get(responseData[i].callsign)!;
                newTopReporters.set(responseData[i].callsign, prev + 1);
            }
        }
        setNumTestUsers(callsigns.size);
        setNumAccuracyReports(responseData.length);
        setAccuracy((accurateCount / responseData.length) * 100);

        // Convert top reporters into the correct format for display
        // Add an "other" category for any reporters below minimum threshold.
        const other : TopReporter = {
            "name": "Other",
            "numReports": 0
        }
        var newTopReportersArr = new Array<TopReporter>();
        newTopReporters.forEach((value : number, key : string) => {
            const percentage : number = (value / responseData.length) * 100;
            if (percentage >= MIN_PERCENTAGE) {
                newTopReportersArr.push({
                    "name": key,
                    "numReports": value
                });
            } else {
                other.numReports += value;
            }

        });
        newTopReportersArr.sort((a : TopReporter, b : TopReporter) => {
            if (a.numReports < b.numReports) {
                return -1;
            } else if (a.numReports > b.numReports) {
                return 1;
            } else {
                return 0;
            }
        });
        if (other.numReports > 0) {
            newTopReportersArr = Array.prototype.concat(newTopReportersArr, [other]);
        }
        setTopReporters(newTopReportersArr);
    }

    const getInferenceCountForModel = () => {
        if (import.meta.env.MODE === "production") {
            const url : string = `/metrics/get_model_inference_count?modelName=${modelDescriptor.name}&modelVersion=${modelDescriptor.version}`;
            fetch(url)
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`Unable to fetch reference count for model ${modelDescriptor.name}`);
                    }
                    return response.text();
                })
                .then (text => {
                    setInferenceCount(Number(text));
                })
                .catch(error => {
                    console.error("Error: ", error);
                });
        }
    }

    useEffect(() => {
        getFeedbackForModel();
        getInferenceCountForModel();
    }, []);

    if (numAccuracyReports < MIN_FEEDBACK_COUNT) {
        return (
            <>
                <h1 className="margin-left">{modelDescriptor!.name}</h1>
                <div className="margin-left margin-bottom version-info">(Version 1.0)</div> 
                <ErrorPane message="No feedback to display" />
            </>
        )
    }

    return (
        <>
            <h1 className="margin-left">{modelDescriptor!.name}</h1>
            <div className="margin-left margin-bottom version-info">(Version 1.0)</div>
            <div className="feedback-grid margin-bottom margin-left margin-right">
                <div className="stats-grid">
                    <TotalInferenceCountCard
                        inferenceCount={inferenceCount}
                    />
                    <ModelAccuracyCard
                        numTestUsers={numTestUsers}
                        numAccuracyReports={numAccuracyReports}
                        accuracy={accuracy}
                    />
                </div>
                <TopFeedbackProvidersCard 
                    topReporters={topReporters}
                />
            </div>
        </>
    );
}

export default FeedbackPane;