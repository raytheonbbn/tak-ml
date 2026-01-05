/* Copyright 2025 RTX BBN Technologies */

import { useEffect, useState } from "react";
import type { AverageDevicePerformance, MetricsResponse, ModelDescriptor, ModelUsage } from "../../types/Types";
import ModelPerformanceByDeviceCard from "../ModelPerformanceByDeviceCard/ModelPerformanceByDeviceCard";
import ModelUsageTimeOfDayCard from "../ModelUsageTimeOfDayCard/ModelUsageTimeOfDayCard";
import "./MetricsPane.css";
import ErrorPane from "../ErrorPane/ErrorPane";

const DEFAULT_MODEL_USAGE_DATA : Array<ModelUsage> = [
    {
        interval: "0:00-1:00",
        usage: 0
    },
    {
        interval: "1:00-2:00",
        usage: 0
    },
    {
        interval: "2:00-3:00",
        usage: 0
    },
    {
        interval: "3:00-4:00",
        usage: 0
    },
    {
        interval: "4:00-5:00",
        usage: 0
    },
    {
        interval: "5:00-6:00",
        usage: 0
    },
    {
        interval: "6:00-7:00",
        usage: 0
    },
    {
        interval: "7:00-8:00",
        usage: 0
    },
    {
        interval: "8:00-9:00",
        usage: 0
    },
    {
        interval: "9:00-10:00",
        usage: 0
    },
    {
        interval: "10:00-11:00",
        usage: 0
    },
    {
        interval: "11:00-12:00",
        usage: 0
    },
    {
        interval: "12:00-13:00",
        usage: 0
    },
    {
        interval: "13:00-14:00",
        usage: 0
    },
    {
        interval: "14:00-15:00",
        usage: 0
    },
    {
        interval: "15:00-16:00",
        usage: 0
    },
    {
        interval: "16:00-17:00",
        usage: 0
    },
    {
        interval: "17:00-18:00",
        usage: 0
    },
    {
        interval: "18:00-19:00",
        usage: 0
    },
    {
        interval: "19:00-20:00",
        usage: 0
    },
    {
        interval: "20:00-21:00",
        usage: 0
    },
    {
        interval: "21:00-22:00",
        usage: 0
    },
    {
        interval: "22:00-23:00",
        usage: 0
    },
    {
        interval: "23:00-0:00",
        usage: 0
    },
];

const TEST_MODEL_USAGE_DATA : Array<ModelUsage> = [
    {
        interval: "0:00-1:00",
        usage: 0
    },
    {
        interval: "1:00-2:00",
        usage: 0
    },
    {
        interval: "2:00-3:00",
        usage: 0
    },
    {
        interval: "3:00-4:00",
        usage: 0
    },
    {
        interval: "4:00-5:00",
        usage: 0
    },
    {
        interval: "5:00-6:00",
        usage: 1
    },
    {
        interval: "6:00-7:00",
        usage: 2
    },
    {
        interval: "7:00-8:00",
        usage: 30
    },
    {
        interval: "8:00-9:00",
        usage: 55
    },
    {
        interval: "9:00-10:00",
        usage: 100.7
    },
    {
        interval: "10:00-11:00",
        usage: 230.5
    },
    {
        interval: "11:00-12:00",
        usage: 250.4
    },
    {
        interval: "12:00-13:00",
        usage: 239.5
    },
    {
        interval: "13:00-14:00",
        usage: 220
    },
    {
        interval: "14:00-15:00",
        usage: 240.2
    },
    {
        interval: "15:00-16:00",
        usage: 200.1
    },
    {
        interval: "16:00-17:00",
        usage: 150.5
    },
    {
        interval: "17:00-18:00",
        usage: 100
    },
    {
        interval: "18:00-19:00",
        usage: 50
    },
    {
        interval: "19:00-20:00",
        usage: 20
    },
    {
        interval: "20:00-21:00",
        usage: 10
    },
    {
        interval: "21:00-22:00",
        usage: 2
    },
    {
        interval: "22:00-23:00",
        usage: 2
    },
    {
        interval: "23:00-0:00",
        usage: 1
    },
];

const TEST_MODEL_PERFORMANCE_DATA : Array<AverageDevicePerformance> = [
    {
        deviceName: "Samsung S9",
        avgDuration: 5
    },
    {
        deviceName: "Samsung S10",
        avgDuration: 4
    },
    {
        deviceName: "Samsung S20",
        avgDuration: 4.1
    },
    {
        deviceName: "Samsung S21",
        avgDuration: 3.9
    },
    {
        deviceName: "Samsung S22",
        avgDuration: 3.65
    },
    {
        deviceName: "Samsung S23",
        avgDuration: 3.57
    },
    {
        deviceName: "Samsung S24",
        avgDuration: 3.57
    },
    {
        deviceName: "Samsung S25",
        avgDuration: 3
    },
    {
        deviceName: "Nexus 7",
        avgDuration: 8
    },
];

export const MetricsPane = ({modelDescriptor} : {modelDescriptor : ModelDescriptor}) => {
    const [modelUsageStats, setModelUsageStats] = useState<Array<ModelUsage>>([]);
    const [modelPerformanceStats, setModelPerformanceStats] = useState<Array<AverageDevicePerformance>>([]);

    const getMetricsData = () => {
        if (import.meta.env.MODE === "production") {
            const modelName : string = encodeURIComponent(modelDescriptor.name);
            var url : string = `/metrics/get_model_metrics?modelName=${modelName}`;

            var modelVersion : string|null = null;
            if (modelDescriptor.version !== null && modelDescriptor.version !== undefined) {
                modelVersion = encodeURIComponent(modelDescriptor.version);
                url += `&modelVersion=${modelVersion}`;
            }

            console.log("Sending metrics API request. URL: ", url);

            fetch(url)
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`Unable to fetch metrics for model ${modelDescriptor.name}`);
                    }
                    return response.json();
                })
                .then(json => {
                    console.log("METRICS: ", json);
                    // The JSON will contain one element with all of the metrics data.
                    updateDisplayFields(json[0]);
                })
                .catch(error => {
                    console.error("Error: ", error);
                });
            
        } else {
            setModelUsageStats(TEST_MODEL_USAGE_DATA);
            setModelPerformanceStats(TEST_MODEL_PERFORMANCE_DATA);
        }
    }

    const updateDisplayFields = (responseData : MetricsResponse) => {
        const modelUsageMap : Map<number, number> = new Map();
        for (var i = 0; i < 24; ++i) {
            modelUsageMap.set(i, 0);
        }
        const deviceToDuration : Map<string, Array<number>> = new Map();
        const inferenceMetrics = responseData.inferenceMetricList;
        for (var i = 0; i < inferenceMetrics.length; ++i) {
            // Model usage
            const startTime : Date = new Date(inferenceMetrics[i].startMillis);
            const startHour : number = startTime.getHours();
            if (!modelUsageMap.has(startHour)) {
                console.warn("Model usage map does not contain value: ", startHour);
            } else {
                const newCount = modelUsageMap.get(startHour)! + 1;
                modelUsageMap.set(startHour, newCount);
            }

            // Model performance
            const deviceBrand : string = inferenceMetrics[i].deviceMetadata.brand;
            const deviceModel : string = inferenceMetrics[i].deviceMetadata.model;
            const deviceName : string = deviceBrand + " - " + deviceModel;
            const durationMillis : number = inferenceMetrics[i].durationMillis;
            if (!deviceToDuration.has(deviceName)) {
                deviceToDuration.set(deviceName, [durationMillis]);
            } else {
                deviceToDuration.get(deviceName)?.push(durationMillis);
            }
        }

        // Model usage
        const newModelUsageStats : Array<ModelUsage> = structuredClone(DEFAULT_MODEL_USAGE_DATA);
        modelUsageMap.forEach((count : number, hour : number) => {
            newModelUsageStats[hour].usage = count;
        });

        setModelUsageStats(newModelUsageStats);

        // Model performance
        const devicePerformanceAverages : Array<AverageDevicePerformance> = [];
        deviceToDuration.forEach((durations : number[], deviceName : string) => {
            const sum = durations.reduce((acc, curr) => acc + curr, 0);
            const avg = sum / durations.length;
            console.log(`deviceName: ${deviceName}, sum: ${sum}, avg: ${avg}`);
            devicePerformanceAverages.push({"deviceName": deviceName, "avgDuration": avg});
        });

        setModelPerformanceStats(devicePerformanceAverages);
    }
    
    useEffect(() => {
        // Loads metrics data for model.
        getMetricsData();
    }, []);

    // Note: Would technically be more correct to display one of the charts if data for the other isn't available.
    // However, we assume that when one chart has no data, the other chart doesn't either. This is reasonable since
    // they're based on the same array coming from the server.
    if (modelUsageStats.length === 0 || modelPerformanceStats.length === 0) {
        return (
            <>
                <h1 className="margin-left">{modelDescriptor!.name}</h1>
                <div className="margin-left margin-bottom version-info">(Version 1.0)</div> 
                <ErrorPane message="No metrics to display" />
            </>
        )
    }

    return (
        <>
            <h1 className="margin-left">{modelDescriptor!.name}</h1>
            <div className="margin-left margin-bottom version-info">(Version 1.0)</div>
            <div className="metrics-grid margin-bottom margin-left margin-right">
                <ModelUsageTimeOfDayCard modelUsageStats={modelUsageStats} />
                <ModelPerformanceByDeviceCard modelPerformanceStats={modelPerformanceStats} />
            </div>
        </>
    );
}