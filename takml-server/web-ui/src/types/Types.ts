/* Copyright 2025 RTX BBN Technologies */
export enum ModelDetailsPage {
    METRICS,
    FEEDBACK
}

// Common frameworks
export enum Frameworks {
    PYTORCH = "PyTorch",
    TFLITE = "TFLite",
    ONNX = "ONNX",
}

// Common model types
export enum ModelTypes {
    GENERIC_RECOGNITION = "Generic Recognition",
    LINEAR_REGRESSION = "Linear Regression",
    IMAGE_CLASSIFICATION = "Image Classification",
    OBJECT_DETECTION = "Object Detection",
}

export enum RunLocations {
    EUD = "eud",
    SERVER = "server",
    BOTH = "both",
}

export type TopReporter = {
    name: string,
    numReports: number,
}

export type ModelDescriptor = {
    name: string,
    version: string,
    hash: string,
    numInferences: number,
    sizeMegabytes: number,
    createdBy: string,
    modelType: string,
    framework: string,
    supportedDevices: Array<string>,
    runOnServer: boolean|undefined,
    uploadTime: Date|null,
}

// ** Types representing data from backend

// ** Models API
export interface ServerModelDescriptor {
    hash: string,
    name: string, 
    additionalMetadata: AdditionalMetadata,
    timeMillisToCallsign: any,
    optionalSegments: any,
}

export interface AdditionalMetadata {
    CALLSIGN: string,
    MODEL_EXTENSION_META: string,
    MODEL_LABELS: string,  // Note: this is a very long string and should probably not be sent with the descriptor.
    MODEL_TYPE: string,
    MODEL_SIZE_META_MB: string,
    RUN_ON_SERVER: string,
    VERSION: string,
    SUPPORTED_DEVICES: string,
}

// ** Feedback API
export interface FeedbackResponse {
    id: number,
    modelName: string,
    modelVersion: number,
    callsign: string,
    inputType: InputType,
    input: string,
    output: string,
    isCorrect: boolean,
    evaluationConfidence: number,
    evaluationRating: number,
    comment: string,
    createdAt: Date,
}

export enum InputType {
    TEXT,
    IMAGE,
    AUDIO,
    OTHER
}

// ** Metrics
export interface MetricsResponse {
    id: MetricId,
    inferenceMetricList: Array<InferenceMetric>,
    modelName: string,
    modelVersion: number,
}

export interface MetricId {
    modelName: string,
    modelVersion: number,
}

export interface InferenceMetric {
    id: number,
    deviceMetadata: DeviceMetadata,
    startMillis: number,
    durationMillis: number,
    confidence: number,
}

export interface DeviceMetadata {
    id: number,
    model: string,
    brand: string,
    manufacturer: string,
    device: string,
    product: string,
    gpuInfo: GpuInfo,
}

export interface GpuInfo {
    id: number,
    vendor: string,
    renderer: string,
    version: string,
}

export type AverageDevicePerformance = {
    deviceName: string,
    avgDuration: number,
}

export type ModelUsage = {
    interval: string,
    usage: number,
}
