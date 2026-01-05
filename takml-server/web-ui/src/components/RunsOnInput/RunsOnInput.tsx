/* Copyright 2025 RTX BBN Technologies */

import { RunLocations } from "../../types/Types";

export const RunsOnInput = ({runsOn, onChange} : {runsOn : string, onChange : (newVal : string) => void}) => {
    return (
        <div className="flex make-column margin-left">
            <span className="form-field">Where can this model be run?</span>
            <label htmlFor={RunLocations.EUD.valueOf()}>
                <input 
                    type="radio" 
                    id="eud" 
                    name="runs_on" 
                    value={RunLocations.EUD.valueOf()}
                    checked={runsOn === RunLocations.EUD.valueOf()}
                    onChange={e => onChange(e.target.value)}
                    required
                />
                End-User Device
            </label>
            <label htmlFor={RunLocations.SERVER.valueOf()}>
                <input 
                    type="radio" 
                    id="server" 
                    name="runs_on" 
                    value={RunLocations.SERVER.valueOf()}
                    checked={runsOn === RunLocations.SERVER.valueOf()}
                    onChange={e => onChange(e.target.value)}
                />
                Server
            </label>
            <label htmlFor={RunLocations.BOTH.valueOf()}>
                <input 
                    type="radio" 
                    id="both" 
                    name="runs_on" 
                    value={RunLocations.BOTH.valueOf()}
                    checked={runsOn === RunLocations.BOTH.valueOf()}
                    onChange={e => onChange(e.target.value)}
                />
                Both
            </label>
        </div>
    );
}

export default RunsOnInput;