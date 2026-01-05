/* Copyright 2025 RTX BBN Technologies */

import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import "./UploadModelPopup.css";
import { faClose } from "@fortawesome/free-solid-svg-icons";
import { useState } from "react";
import { TextFormField } from "../TextFormField/TextFormField";
import { AddItemsInput } from "../AddItemsInput/AddItemsInput";
import { RunLocations, type ModelDescriptor } from "../../types/Types";
import { generateSimpleHash } from "../../utilities/General";
import RunsOnInput from "../RunsOnInput/RunsOnInput";
import UploadModelPopupFileInput from "../UploadModelPopupFileInput/UploadModelPopupFileInput";

export const UploadModelPopup = ({onUpload, onClose} : 
    {onUpload : (model : ModelDescriptor, selectedFile : File) => void, onClose : () => void}) => {

    // const [modelName, setModelName] = useState<string>("");
    // const [version, setVersion] = useState<string>("");
    const [author, setAuthor] = useState<string>("");
    // const [modelType, setModelType] = useState<string>("");
    // const [modelFramework, setModelFramework] = useState<string>("");
    const [runsOn, setRunsOn] = useState<string>("");

    const [supportedDevices, setSupportedDevices] = useState<Array<string>>([]);

    const [selectedFile, setSelectedFile] = useState<File|null>(null);
    const [fileUploadDisabled, setFileUploadDisabled] = useState(true);

    const uploadData = () => {
        console.log("Uploading data...");
        var runOnServer : boolean = false;
        if (runsOn === RunLocations.SERVER || runsOn === RunLocations.BOTH) {
            runOnServer = true;
        }
        const modelDescriptor : ModelDescriptor = {
            name: "N/A",
            version: "N/A",
            hash: generateSimpleHash(),
            numInferences: 0,
            sizeMegabytes: 0,
            createdBy: author,
            modelType: "N/A",
            framework: "N/A",
            supportedDevices: supportedDevices,
            runOnServer: runOnServer,
            uploadTime: new Date(),
        }
        if (selectedFile === null) {
            console.error("Model file cannot be null");
            return;
        }
        onUpload(modelDescriptor, selectedFile);
        onClose();
    }

    return(
        <div id="add-modal" className="modal-backdrop">
            <div 
                className="upload-modal-content flex make-column border" 
                onClick={e =>
                    // Do not close modal if anything inside modal content is clicked
                    e.stopPropagation()
                }
            >
                <form onSubmit={e => {
                    // Prevent the form from closing by default.
                    e.preventDefault();
                    uploadData();
                }}>
                    <div className="modal-header flex flex-space-between">
                        <span>Upload ML Model</span>
                        <button id="modal-close-btn-id" 
                            className="modal-close-btn"
                            onClick={() => onClose()}>
                                <FontAwesomeIcon icon={faClose} />
                        </button>
                    </div>
                    {/* Fields below are commented out until we decide to add them to the request */}
                    {/* <TextFormField
                        id="ml-model-name"
                        placeholder="Model Name"
                        label="Model Name"
                        value={modelName}
                        onChange={(newModelName) => setModelName(newModelName)}
                    /> */}
                    {/* <TextFormField
                        id="ml-model-type"
                        placeholder="Model Type"
                        label="Model Type"
                        value={modelType}
                        onChange={(newModelType) => setModelType(newModelType)}
                    /> */}
                    {/* <TextFormField
                        id="ml-model-framework"
                        placeholder="Model Framework"
                        label="Model Framework"
                        value={modelFramework}
                        onChange={(newModelFramework) => setModelFramework(newModelFramework)}
                    /> */}
                    {/* <TextFormField
                        id="ml-model-version"
                        placeholder="Version"
                        label="Version"
                        value={version}
                        onChange={(newVersion) => setVersion(newVersion)}
                    /> */}
                    <TextFormField
                        id="ml-model-author"
                        placeholder="Author"
                        label="Created By"
                        value={author}
                        onChange={(newAuthor) => setAuthor(newAuthor)}
                    />
                    <RunsOnInput 
                        runsOn={runsOn}
                        onChange={(newVal) => setRunsOn(newVal)}
                    />
                    <AddItemsInput
                        id="add-support-devices"
                        header="Supported Devices"
                        initialItemsList={supportedDevices}
                        placeholder="Enter a device"
                        onItemsListChanged={(newList) => {setSupportedDevices(newList)}}
                    />
                    <div className="form-field margin-left">
                        Model File
                    </div>
                    <UploadModelPopupFileInput
                        header="Upload ML Model"
                        fileTypes=".zip"
                        setSelectedFile={setSelectedFile}
                        setFileUploadDisabled={setFileUploadDisabled}
                    />
                    <div className="flex make-row margin">
                        <div className="modal-action-buttons-container width-fifty-percent">
                            <button className="add-item-submit-button" disabled={fileUploadDisabled}>
                                Upload
                            </button>
                            <button
                                className="modal-cancel-button"
                                onClick={() =>
                                    onClose()
                                }
                            >
                                Cancel
                            </button>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    );
}

export default UploadModelPopup;