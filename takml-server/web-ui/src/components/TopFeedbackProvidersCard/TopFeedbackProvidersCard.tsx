/* Copyright 2025 RTX BBN Technologies */

import type { TopReporter } from "../../types/Types";
import "./TopFeedbackProvidersCard.css";
import { Cell, Pie, PieChart, type PieLabelRenderProps } from 'recharts';
import * as d3 from "d3";

// ** Based on: https://recharts.github.io/en-US/examples/PieChartWithCustomizedLabel/
const RADIAN = Math.PI / 180;

const renderCustomizedLabel = ({ cx, cy, midAngle, innerRadius, outerRadius, percent, name }: PieLabelRenderProps) => {
  if (cx == null || cy == null || innerRadius == null || outerRadius == null) {
    return null;
  }
  const radius = innerRadius + (outerRadius - innerRadius) * 0.5;
  const x = cx + radius * Math.cos(-(midAngle ?? 0) * RADIAN);
  const y = cy + radius * Math.sin(-(midAngle ?? 0) * RADIAN);

  return (
    <text x={x} y={y} fill="white" textAnchor="middle" dominantBaseline="central">
      {`${name} ${((percent ?? 1) * 100).toFixed(0)}%`}
    </text>
  );
};

const PieChartWithCustomizedLabel = ({ data, isAnimationActive = true }: { data: Array<TopReporter>, isAnimationActive?: boolean }) => {
    const color = d3.quantize(t => d3.interpolateCool(t * 0.4), data.length);
    console.log("COLOR: ", color);

    return (
        <PieChart style={{ width: '100%', maxWidth: '500px', maxHeight: '80vh', aspectRatio: 1, outline: 'none' }} responsive>
          <Pie
                data={data}
                innerRadius="30%"
                outerRadius="80%"
                labelLine={false}
                label={renderCustomizedLabel}
                fill="#8884d8"
                dataKey="numReports"
                isAnimationActive={isAnimationActive}
                style={{outline: 'none'}}
            >
                {data.map((entry, index) => (
                    <Cell key={`cell-${entry.name}`} fill={color[index % color.length]} style={{ outline: 'none' }}/>
                ))}
          </Pie>
        </PieChart>
  );
}

export const TopFeedbackProvidersCard = ({topReporters} : {topReporters : Array<TopReporter>}) => {
    return (
        <>
            <div className="flex make-column model-feedback-stats-container">
                <div className="display-header">
                    <b>Top Feedback Providers</b>
                </div>
                <div className="flex make-column width-max flex-center">
                  <PieChartWithCustomizedLabel 
                      data={topReporters}
                  />
                </div>
                {/* <div className="display-footer">
                     A pie chart of the top N users by number of accuracy reports.
                </div> */}
            </div>
        </>
    );
}

export default TopFeedbackProvidersCard;