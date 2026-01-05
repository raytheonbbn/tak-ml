/* Copyright 2025 RTX BBN Technologies */

import { Frameworks, ModelTypes } from "../types/Types";

export const EXTENSIONS_FRAMEWORKS_MAP = new Map([
    [".onnx", Frameworks.ONNX.valueOf()],
    [".torchscript", Frameworks.PYTORCH.valueOf()],
    [".tflite", Frameworks.TFLITE.valueOf()]
])

// TODO fetch model types from the server
export const MODEL_TYPES_DISPLAY_NAMES_MAP = new Map([
    ["GENERIC_RECOGNITION", ModelTypes.GENERIC_RECOGNITION.valueOf()],
    ["LINEAR_REGRESSION", ModelTypes.LINEAR_REGRESSION.valueOf()],
    ["IMAGE_CLASSIFICATION", ModelTypes.IMAGE_CLASSIFICATION.valueOf()],
    ["OBJECT_DETECTION", ModelTypes.OBJECT_DETECTION.valueOf()],
])

export const DISPLAY_NAMES_MODEL_TYPES_MAP = new Map([
    [ModelTypes.GENERIC_RECOGNITION.valueOf(), "GENERIC_RECOGNITION"],
    [ModelTypes.LINEAR_REGRESSION.valueOf(), "LINEAR_REGRESSION"],
    [ModelTypes.IMAGE_CLASSIFICATION.valueOf(), "IMAGE_CLASSIFICATION"],
    [ModelTypes.OBJECT_DETECTION.valueOf(), "OBJECT_DETECTION"]
])