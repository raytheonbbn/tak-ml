/* Copyright 2025 RTX BBN Technologies */

import type { ModelDescriptor } from "../../types/Types";
import { getTime } from "../../utilities/General";
import "./ModelNameCell.css";

export const ModelNameCell = ({modelDescriptor} : {modelDescriptor : ModelDescriptor}) => {

    return (
        <div className="flex make-column">
            <div>
                <b>
                    {
                        modelDescriptor.name !== null && modelDescriptor.name !== undefined ? modelDescriptor.name : "N/A"
                    }
                </b>
            </div>
            <div className="upload-time">
                <i>
                    {
                        modelDescriptor.uploadTime !== null && modelDescriptor.uploadTime !== undefined ? getTime(modelDescriptor.uploadTime) : "N/A"
                    }
                </i>
            </div>
        </div>
    )
}