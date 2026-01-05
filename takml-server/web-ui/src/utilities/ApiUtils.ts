/* Copyright 2025 RTX BBN Technologies */

import { EXTENSIONS_FRAMEWORKS_MAP, MODEL_TYPES_DISPLAY_NAMES_MAP } from "../data/ResponseMappings";
import { type ModelDescriptor, type ServerModelDescriptor } from "../types/Types";
import { stringToBoolean } from "./General";

export const toModelDescriptor = (model : ServerModelDescriptor) => {
    const name = model.name !== null && model.name !== undefined ? model.name : "N/A";
    const hash = model.hash !== null && model.hash !== undefined ? model.hash : "";
    const uploadTime = getUploadTime(model);
    if (model.additionalMetadata !== null && model.additionalMetadata !== undefined) {
        const version = model.additionalMetadata.VERSION !== null && model.additionalMetadata.VERSION !== undefined ? model.additionalMetadata.VERSION : "";
        const author = model.additionalMetadata.CALLSIGN !== null && model.additionalMetadata.CALLSIGN !== undefined ? model.additionalMetadata.CALLSIGN : "";
        const modelType = getModelType(model);
        const framework = getFramework(model);
        const modelSize = getModelSize(model);
        const runOnServer = getRunOnServer(model);
        const supportedDevices = getSupportedDevices(model);
        const modelDescriptor : ModelDescriptor = {
            name: name,
            version: version,
            hash: hash,
            numInferences: 0,  // TODO - update
            sizeMegabytes: modelSize,
            createdBy: author,
            modelType: modelType,
            framework: framework,
            supportedDevices: supportedDevices,
            runOnServer: runOnServer,
            uploadTime: uploadTime,
        }
        return modelDescriptor;
    } else {
        const modelDescriptor : ModelDescriptor = {
            name: name,
            version: "",
            hash: hash,
            numInferences: 0,  // TODO - update
            sizeMegabytes: 0,
            createdBy: "",
            modelType: "",
            framework: "",
            supportedDevices: [],
            runOnServer: undefined,
            uploadTime: uploadTime,
        }
        return modelDescriptor;
    }
}

const getModelType = (model : ServerModelDescriptor) => {
    var modelType : string = "";
    if (model.additionalMetadata.MODEL_TYPE !== null && model.additionalMetadata.MODEL_TYPE !== undefined) {
        if (MODEL_TYPES_DISPLAY_NAMES_MAP.get(model.additionalMetadata.MODEL_TYPE) !== undefined) {
            modelType = MODEL_TYPES_DISPLAY_NAMES_MAP.get(model.additionalMetadata.MODEL_TYPE)!;
        }
    }
    return modelType;
}

const getFramework = (model : ServerModelDescriptor) => {
    var framework : string = "";
    if (model.additionalMetadata.MODEL_EXTENSION_META !== null && model.additionalMetadata.MODEL_EXTENSION_META !== undefined) {
        if (EXTENSIONS_FRAMEWORKS_MAP.get(model.additionalMetadata.MODEL_EXTENSION_META) !== undefined) {
            framework = EXTENSIONS_FRAMEWORKS_MAP.get(model.additionalMetadata.MODEL_EXTENSION_META)!;
        }
    }
    return framework;
}

const getModelSize = (model : ServerModelDescriptor) => {
    var modelSize : number = 0;
    if (model.additionalMetadata.MODEL_SIZE_META_MB !== null && model.additionalMetadata.MODEL_SIZE_META_MB !== undefined) {
        modelSize = parseFloat(model.additionalMetadata.MODEL_SIZE_META_MB);
    }
    return modelSize;
}

const getRunOnServer = (model : ServerModelDescriptor) => {
    var runOnServer : boolean = false;
    if (model.additionalMetadata.RUN_ON_SERVER !== null && model.additionalMetadata.RUN_ON_SERVER !== undefined) {
        runOnServer = stringToBoolean(model.additionalMetadata.RUN_ON_SERVER);
    }
    return runOnServer;
}

const getUploadTime = (model : ServerModelDescriptor) => {
    var uploadTime : Date|null = null;
    if (model.timeMillisToCallsign !== null && model.timeMillisToCallsign !== undefined) {
        var keys : string [] = Object.keys(model.timeMillisToCallsign);
        if (keys.length > 0) {
            uploadTime = new Date(parseInt(keys[0]));
        }
    }
    return uploadTime;
}

const getSupportedDevices = (model : ServerModelDescriptor) => {
    var devices : Array<string> = [];
    if (model.additionalMetadata.SUPPORTED_DEVICES !== null && model.additionalMetadata.SUPPORTED_DEVICES !== undefined) {
        // Currently supported devices are enumerated in a comma-separated string.
        const supportedDevicesStr = model.additionalMetadata.SUPPORTED_DEVICES;
        devices = supportedDevicesStr.split(",").map((deviceName) => deviceName.trim());
    }
    return devices;
}


