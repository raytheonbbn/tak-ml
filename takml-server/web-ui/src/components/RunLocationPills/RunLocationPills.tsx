
/* Copyright 2025 RTX BBN Technologies */
import "./RunLocationPills.css";

export const RunLocationPills = ({runLocation} : {runLocation : boolean|undefined}) => {
    if (runLocation === undefined) {
        return (
            <div className="error">Unknown</div>
        );
    } else if (runLocation === true) {
        return (
            <div className="run-location-pill">Server</div>
        );
    } else {
        return (
            <div className="run-location-pill">EUD</div>
        );
    }
    // } else {
    //     return (
    //         <div className="flex make-row">
    //             <div className="run-location-pill">Server</div>
    //             <div className="run-location-pill">EUD</div>
    //         </div>
    //     );
    // }
}

export default RunLocationPills;