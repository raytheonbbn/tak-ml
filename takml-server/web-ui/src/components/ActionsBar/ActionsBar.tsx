/* Copyright 2025 RTX BBN Technologies */

import type { ModelDescriptor } from "../../types/Types";
import DeleteButton from "../DeleteButton/DeleteButton";
import DetailsButton from "../DetailsButton/DetailsButton";
import { ViewMetricsButton } from "../ViewMetricsButton/ViewMetricsButton";

export const ActionsBar = ({row, onUpdate, onDelete, onSuccess} : {row : any, onUpdate : (model : ModelDescriptor, file : File|null) => void, onDelete : () => void, onSuccess : (msg : string) => void}) => {

    return (
        <div className="flex make-row gap">
            <DetailsButton 
                modelDescriptor={row}
                onUpdate={onUpdate}
                onSuccess={onSuccess}
            />
            <ViewMetricsButton modelDescriptor={row} />
            <DeleteButton onDelete={onDelete} />
        </div>
    );
}

export default ActionsBar;