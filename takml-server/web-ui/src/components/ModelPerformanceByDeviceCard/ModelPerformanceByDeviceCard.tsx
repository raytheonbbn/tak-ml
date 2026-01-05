/* Copyright 2025 RTX BBN Technologies */

import type { AverageDevicePerformance } from "../../types/Types";
import "./ModelPerformanceByDeviceCard.css";
import { Bar, BarChart, CartesianGrid, Tooltip, XAxis, YAxis } from "recharts";

// ** Based on: https://recharts.github.io/en-US/examples/SimpleBarChart/

const ModelPerformanceChart = ({data} : { data : Array<AverageDevicePerformance> } ) => {
    return (
        <BarChart
            style={{ width: '100%', maxWidth: '1000px', maxHeight: '50vh', aspectRatio: 1.8 }}
            responsive
            data={data}
        >
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="deviceName" />
            <YAxis width="auto" />
            {/* <Brush dataKey="deviceName" height={30} stroke="#8884d8" /> */}
            <Bar dataKey="avgDuration" fill="#45b5d2" activeBar={{ fill: '#4585d2', enableBackground: 'false'}} />
            <Tooltip cursor={false} contentStyle={{ backgroundColor: '#333', borderColor: '#333', borderRadius: '5px', color: '#fff' }}/>
        </BarChart>
    )
}

export const ModelPerformanceByDeviceCard = ({modelPerformanceStats} : { modelPerformanceStats : Array<AverageDevicePerformance> }) => {
    return (
        <>
            <div className="flex make-column model-feedback-stats-container">
                <div className="display-header">
                    <b>Average Prediction Time (milliseconds) </b>
                </div>
                <div className="flex flex-center">
                    <ModelPerformanceChart 
                        data={modelPerformanceStats}
                    />
                </div>
                {/* <div className="display-footer">
                    A bar chart showing average prediction time in seconds, organized by device type.
                </div> */}
            </div>
        </>
    );
}

export default ModelPerformanceByDeviceCard;