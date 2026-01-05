/* Copyright 2025 RTX BBN Technologies */

import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import "./EditModal.css";
import { faClose } from "@fortawesome/free-solid-svg-icons";
import { useState } from "react";
import { TextFormField } from "../TextFormField/TextFormField";
import { AddItemsInput } from "../AddItemsInput/AddItemsInput";
import { RunLocations, type ModelDescriptor } from "../../types/Types";
import RunsOnInput from "../RunsOnInput/RunsOnInput";
import HashDisplay from "../HashDisplay/HashDisplay";
import UploadModelPopupFileInput from "../UploadModelPopupFileInput/UploadModelPopupFileInput";

export const EditModal = ({modelDescriptor, onUpdate, onClose, onSuccess} : 
    {modelDescriptor : ModelDescriptor, onUpdate : (model : ModelDescriptor, file : File|null) => void, onClose : () => void, onSuccess : (msg :string) => void}) => {

    // const [modelName, setModelName] = useState<string>(modelDescriptor.name !== null  && modelDescriptor.name !== undefined ? modelDescriptor.name : "");
    // const [version, setVersion] = useState<string>(modelDescriptor.version !== null && modelDescriptor.version !== undefined ? modelDescriptor.version : "");
    const [author, setAuthor] = useState<string>(modelDescriptor.createdBy !== null && modelDescriptor.createdBy !== undefined ? modelDescriptor.createdBy : "");
    // const [modelType, setModelType] = useState<string>(modelDescriptor.modelType !== null && modelDescriptor.modelType !== undefined ? modelDescriptor.modelType : "");
    // const [modelFramework, setModelFramework] = useState<string>(modelDescriptor.framework !== null && modelDescriptor.framework !== undefined ? modelDescriptor.framework : "");
    const [runsOn, setRunsOn] = useState<string>(modelDescriptor.runOnServer === true ? RunLocations.SERVER.valueOf() : RunLocations.EUD.valueOf());

    const [supportedDevices, setSupportedDevices] = useState<Array<string>>(modelDescriptor.supportedDevices !== null && modelDescriptor.supportedDevices !== undefined ? modelDescriptor.supportedDevices : []);

    const [selectedFile, setSelectedFile] = useState<File|null>(null);
    const [fileUploadDisabled, setFileUploadDisabled] = useState(true);

    const getName = () => {
        return modelDescriptor.name !== null  && modelDescriptor.name !== undefined ? modelDescriptor.name : "";
    }

    const uploadData = () => {
        console.log("Uploading data...");
        var runOnServer : boolean = false;
        if (runsOn === RunLocations.SERVER || runsOn === RunLocations.BOTH) {
            runOnServer = true;
        }
        const newModelDescriptor : ModelDescriptor = {
            ...modelDescriptor,
            createdBy: author,
            runOnServer: runOnServer,
            supportedDevices: supportedDevices
        }
        // ** Until the server allows the frontend to leave out the model file,
        // we should include it in the uploaded data.
        if (selectedFile === null) {
            console.error("Model file cannot be null");
            return;
        }
        onUpdate(newModelDescriptor, selectedFile);
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
                        <span>Upload "{getName()}"</span>
                        <button id="modal-close-btn-id" 
                            className="modal-close-btn"
                            onClick={() => onClose()}>
                                <FontAwesomeIcon icon={faClose} />
                        </button>
                    </div>
                    <HashDisplay
                        hash={modelDescriptor.hash}
                        onCopied={(msg) => onSuccess(msg)}
                    />
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

export default EditModal;