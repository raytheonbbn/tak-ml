package com.atakmap.android.takml_android;

import com.atakmap.android.takml_android.lib.TakmlInitializationException;

import java.util.Set;

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
    void instantiate(TakmlModel takmlModel) throws TakmlInitializationException;

    /**
     * Execute the plugin
     *
     * @param inputData - the input data to run model on
     * @param callback - callback with response
     */
    void execute(byte[] inputData, MXExecuteModelCallback callback);

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
     * Shuts down the plugin
     */
     void shutdown();
}