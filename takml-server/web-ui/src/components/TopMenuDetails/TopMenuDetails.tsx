/* Copyright 2025 RTX BBN Technologies */
import "./TopMenuDetails.css";
import logo from "../../assets/TAK-ML-logo.png";
import { faHome } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { useNavigate } from "react-router";
import { ModelDetailsPage } from "../../types/Types";

export const TopMenuDetails = ({activeIndex, setActiveIndex} : {activeIndex : ModelDetailsPage, setActiveIndex : (m : ModelDetailsPage) => void}) => {
    const navigate = useNavigate();

    const navigateToHome = () => {
        navigate("/");
    }

    return (
        <>
            <div className="flex make-row flex-start width-max top-menu">
                <div className="flex make-row width-sixty-percent">
                    <img src={logo} className="top-menu-logo margin-left" alt="logo" />
                    <div className="flex make-column margin-left">
                        <h2 className="site-name">TAK-ML</h2>
                        <h5>Model Management Platform</h5>
                    </div>
                </div>
                <div className="flex make-row width-thirty-percent height-max flex-space-evenly">
                    <button className={`top-menu-tab height-max ${activeIndex === ModelDetailsPage.METRICS ? "selected" : "" }`} onClick={() => setActiveIndex(ModelDetailsPage.METRICS)}>
                        Metrics
                    </button>
                    <button className={`top-menu-tab height-max ${activeIndex === ModelDetailsPage.FEEDBACK ? "selected" : ""}`} onClick={() => setActiveIndex(ModelDetailsPage.FEEDBACK)}>
                        Feedback
                    </button>
                    {/* <button className="top-menu-home" onClick={navigateToHome}>
                        <FontAwesomeIcon icon={faHome} size="2x" />
                    </button> */}
                </div>
                <div className="flex make-row flex-center width-ten-percent">
                    <button className="top-menu-home" onClick={navigateToHome}>
                        <FontAwesomeIcon icon={faHome} size="2x" />
                    </button>
                </div>
            </div>
        </>
    );
};

export default TopMenuDetails;