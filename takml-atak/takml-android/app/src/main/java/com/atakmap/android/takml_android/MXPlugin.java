package com.atakmap.android.takml_android;

import android.content.Context;

import com.atakmap.android.takml_android.lib.TakmlInitializationException;
import com.atakmap.android.takml_android.service.MxPluginService;
import com.atakmap.android.takml_android.tensor_processor.InferInput;

import java.util.List;

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
     * @throws TakmlInitializationException
     */
    void instantiate(TakmlModel takmlModel, Context context) throws TakmlInitializationException;

    /**
     * Execute the plugin
     *
     * @param inputData - the input data(s) to run model on
     * @param callback - callback with response
     */
    void execute(byte[] inputData, MXExecuteModelCallback callback);

    /**
     * Execute the plugin
     *
     * @param inputTensors - the input tensors to run model on
     * @param callback - callback with response
     */
    void execute(List<InferInput> inputTensors, MXExecuteModelCallback callback);


    /**
     * Returns the acceptable ML model extensions (e.g. ".torchscript")
     *
     * @return List of applicable model extensions
     */
    String[] getApplicableModelExtensions();

    /**
     * Returns the acceptable ML model types (e.g. "IMAGE_CLASSIFICATION" or "LINEAR_REGRESSION").
     * See {@link ModelTypeConstants} for common model types, or create your own.
     *
     * @return List of applicable model types
     */
    String[] getSupportedModelTypes();

    /**
     * Returns an optional Mx Plugin Service Class. See {@link MxPluginService}. This supports
     * running as service.
     *
     * @return implementation of MxPluginService
     */
    Class<? extends MxPluginService> getOptionalServiceClass();

    /**
     * Shuts down the plugin
     */
     void shutdown();
}