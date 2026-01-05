/* Copyright 2025 RTX BBN Technologies */
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faLineChart } from "@fortawesome/free-solid-svg-icons";
import "./ViewMetricsButton.css";
import type { ModelDescriptor } from "../../types/Types";
import { useNavigate } from "react-router";

export const ViewMetricsButton = ({modelDescriptor} : {modelDescriptor : ModelDescriptor}) => {
    const navigate = useNavigate();

    const handleOnClick = () => {
        console.log(modelDescriptor);
        // navigate("/details/" + modelDescriptor.hash);
        navigate("/details/" + modelDescriptor.hash, { state: { model: modelDescriptor } });
    }

    return (
        <button title="metrics and feedback" className="metrics-button" onClick={handleOnClick}>
            <FontAwesomeIcon icon={faLineChart} />
        </button>
    );
}