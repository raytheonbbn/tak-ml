/* Copyright 2025 RTX BBN Technologies */

import { ModelDetailsPage, type ModelDescriptor } from "../types/Types";
import TopMenuDetails from "../components/TopMenuDetails/TopMenuDetails";
import { useEffect, useState } from "react";
import { useLocation, useParams } from "react-router";
import ErrorPane from "../components/ErrorPane/ErrorPane";
import { MetricsPane } from "../components/MetricsPane/MetricsPane";
import FeedbackPane from "../components/FeedbackPane/FeedbackPane";


export const ModelDetails = () => {
    const [activeIndex, setActiveIndex] = useState<ModelDetailsPage>(ModelDetailsPage.METRICS);
    const [modelDescriptor, setModelDescriptor] = useState<ModelDescriptor|null>(null);
    const params = useParams();
    let location = useLocation();

    useEffect(() => {
        console.log("params: ", params);
        console.log("params.modelId: ", params.modelId);
        if (location !== null && location !== undefined) {
            const state = location.state;
            if (state !== null && state !== undefined) {
                const model = state.model;
                if (model !== null && model !== undefined) {
                    console.log(model);
                    setModelDescriptor(model);
                }
            }
        }
    }, [params, location]);

    if (modelDescriptor === null) {
        return (
            <div className="flex make-column height-max-view-port">
                <TopMenuDetails
                    activeIndex={activeIndex} 
                    setActiveIndex={setActiveIndex}
                />
                {activeIndex === ModelDetailsPage.METRICS &&
                    <ErrorPane message={"Model does not exist on server."} />
                }
                {activeIndex === ModelDetailsPage.FEEDBACK &&
                    <ErrorPane message={"Model does not exist on server."} />
                }
            </div>
        );
    }

    return (
        <div className="flex make-column height-max-view-port">
            <TopMenuDetails
                activeIndex={activeIndex} 
                setActiveIndex={setActiveIndex}
            />
            {activeIndex === ModelDetailsPage.METRICS &&
                <MetricsPane modelDescriptor={modelDescriptor} />
            }
            {activeIndex === ModelDetailsPage.FEEDBACK &&
                <FeedbackPane modelDescriptor={modelDescriptor} />
            }
        </div>
    );
}

export default ModelDetails;