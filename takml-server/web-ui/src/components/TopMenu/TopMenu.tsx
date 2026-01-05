/* Copyright 2025 RTX BBN Technologies */
import "./TopMenu.css";
import logo from "../../assets/TAK-ML-logo.png";
import UploadModelButton from "../UploadModelButton/UploadModelButton";
import type { ModelDescriptor } from "../../types/Types";
export const TopMenu = ({onUpload} : {onUpload : (model : ModelDescriptor, selectedFile : File) => void}) => {

    return (
        <>
            <div className="flex make-row flex-start width-max top-menu">
                <div className="flex make-row width-eighty-five-percent">
                    <img src={logo} className="top-menu-logo margin-left" alt="logo" />
                    <div className="flex make-column margin-left">
                        <h2 className="site-name">TAK-ML</h2>
                        <h5>Model Management Platform</h5>
                    </div>
                </div>
                <div className="flex make-row width-fifteen-percent margin-right">
                    <UploadModelButton 
                        onUpload={onUpload}
                    />
                </div>
            </div>
        </>
    );
};

export default TopMenu;