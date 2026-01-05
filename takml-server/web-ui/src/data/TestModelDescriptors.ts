/* Copyright 2025 RTX BBN Technologies */

import { type ModelDescriptor } from "../types/Types";

const testModelDescriptors : ModelDescriptor[] = [
    {
        name: "MobileNet Onnx (Remote)",
        version: "1.0",
        hash: "44415ea47f20adbb3f64df4c5808be5d5c205296c8541c81f70d020c5c81e96b",
        numInferences: 500,
        sizeMegabytes: 98,
        createdBy: "TAK ML Server",
        modelType: "Image classification",
        framework: "ONNX",
        supportedDevices: [],
        runOnServer: true,
        uploadTime: new Date(),
    },
    {
        name: "Dogs and Cats Pytorch (Remote)",
        version: "1.0",
        hash: "1ce0caff2cae8b07b722d304c26bc14c4a7b3a816d5b23d2964e052b87b4ec11",
        numInferences: 600,
        sizeMegabytes: 440,
        createdBy: "TAK ML Server",
        modelType: "Image classification",
        framework: "PyTorch",
        supportedDevices: [],
        runOnServer: false,
        uploadTime: new Date(),
    }
];

export default testModelDescriptors;