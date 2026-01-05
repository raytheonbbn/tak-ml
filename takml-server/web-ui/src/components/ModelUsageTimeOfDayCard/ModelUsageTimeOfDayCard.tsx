/* Copyright 2025 RTX BBN Technologies */

import type { ModelUsage } from "../../types/Types";
import "./ModelUsageTimeOfDayCard.css";
import { Bar, BarChart, CartesianGrid, Tooltip, XAxis, YAxis } from "recharts";

// ** Based on: https://recharts.github.io/en-US/examples/SimpleBarChart/

const ModelUsageChart = ({data} : {data : Array<ModelUsage>}) => {
    return (
        <BarChart
            style={{ width: '100%', maxWidth: '1000px', maxHeight: '50vh', aspectRatio: 1.8 }}
            barCategoryGap={0}
            responsive
            data={data}
        >
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="interval" />
            <YAxis width="auto" />
            <Bar dataKey="usage" fill="#45b5d2" activeBar={{ fill: '#4585d2', enableBackground: 'false'}} />
            <Tooltip cursor={false} contentStyle={{ backgroundColor: '#333', borderColor: '#333', borderRadius: '5px', color: '#fff' }}/>
        </BarChart>
    );
}

export const ModelUsageTimeOfDayCard = ({modelUsageStats} : { modelUsageStats : Array<ModelUsage> }) => {
    return (
        <>
            <div className="flex make-column model-feedback-stats-container">
                <div className="display-header">
                    <b>Model Usage Statistics</b>
                </div>
                <div className="flex flex-center">
                    <ModelUsageChart 
                        data={modelUsageStats}
                    />
                </div>
                {/* <div className="display-footer">
                    A histogram showing usage by time of day.
                </div> */}
            </div>
        </>
    );
}

export default ModelUsageTimeOfDayCard;