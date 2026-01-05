package com.bbn.takml_server.model_execution;

import com.bbn.takml_server.lib.TakmlInitializationException;
import com.bbn.takml_server.model_execution.api.model.InferInput;
import com.bbn.takml_server.model_execution.api.model.ModelTensor;
import com.bbn.takml_server.takml_model.ModelTypeConstants;
import com.bbn.takml_server.takml_model.TakmlModel;
import org.apache.commons.lang3.tuple.Pair;

public interface MXPlugin {
    /**
     * Description of the plugin
     *
     * @return description of plugin
     */
    String getDescription();

    /**
     * Version of the plugin (e.g. 1.0)
     *
     * @return version
     */
    String getVersion();

    /**
     * Whether the plugin is designed for server or client side
     *
     * @return isClientSide
     */
    boolean isServerSide();

    /**
     * Instantiates the plugin
     *
     * @param takmlModel - the ML model to execute with
     *
     * @return List of ModelTensor inputs and outputs (e.g. shape expected)
     *
     * @throws TakmlInitializationException
     */
    Pair<long[], long[]> instantiate(TakmlModel takmlModel) throws TakmlInitializationException;

    /**
     * Execute the plugin
     *
     * @param inputTensorData - the input tensor data to run model on
     * @param callback - callback with response
     */
    void execute(InferInput inputTensorData, MXExecuteModelCallback callback);

    /**
     * Returns the acceptable ML model extensions (e.g. ".torchscript")
     *
     * @return List of applicable model extensions
     */
    String[] getApplicableModelExtensions();

    /**
     * Returns the acceptable ML model types (e.g. "IMAGE_CLASSIFICATION" or "LINEAR_REGRESSION"). See
     * {@link ModelTypeConstants} for common model types, or create your own.
     *
     * @return List of applicable model types
     */
    String[] getSupportedModelTypes();

    /**
     * Shuts down the plugin
     */
    void shutdown();
}