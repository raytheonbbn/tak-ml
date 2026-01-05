/* Copyright 2025 RTX BBN Technologies */

import { TotalModelsDisplay } from "../TotalModelsDisplay/TotalModelsDisplay";
import { TotalStorageDisplay } from "../TotalStorageDisplay/TotalStorageDisplay";

export const ModelStatsDisplay = ({total, totalStorage} : {total : number, totalStorage : number}) => {
    return (
        <div className="flex make-row flex-space-evenly margin-top margin-bottom">
            <TotalModelsDisplay total={total} />
            <TotalStorageDisplay total={totalStorage} />
        </div>
    );
}