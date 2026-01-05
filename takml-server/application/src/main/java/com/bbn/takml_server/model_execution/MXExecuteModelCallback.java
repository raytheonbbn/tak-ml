package com.bbn.takml_server.model_execution;

import com.bbn.takml_server.model_execution.api.model.InferOutput;

import java.util.List;

public interface MXExecuteModelCallback {
    /**
     * Returns a List of TAK ML Model Results
     *
     * @param tensorResults - List of tensor results
     * @param success - Whether the model execution was successful
     * @param modelType - The type of model output
     */
    void modelResult(List<InferOutput> tensorResults, boolean success, String modelType);
}