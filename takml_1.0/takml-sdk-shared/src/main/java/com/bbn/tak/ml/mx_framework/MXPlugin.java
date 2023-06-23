package com.bbn.tak.ml.mx_framework;

import java.io.Serializable;
import java.util.HashMap;

/**
 * A model execution (MX) plugin.
 * <p>
 * Supports the ability to create multiple instances of the same
 * plugin, each with potentially different models and parameters.
 * <p>
 * Implementing classes must use the {@link MXPluginDescription} annotation
 * to describe the basic attributes of the plugin to the framework.
 * <p>
 * Implementing classes must also use the appropriate instance of the
 * {@link MXFrameworkRegistrar} to register and deregister the plugin.
 */
public interface MXPlugin {

    /**
     * Get the ID string for this MX plugin.
     *
     * @return a string representing the unique ID of this plugin.
     */
    public String getPluginID();

    /**
     * Get the description for this plugin.
     *
     * @return a representation of the description annotation of this plugin.
     */
    public MXPluginDescription getPluginDescription();

    /**
     * Instantiate this plugin with a given model and parameters.
     *
     * @param modelDirectory the directory where the model file is found.
     * @param modelFilename the filename of the model file.
     * @param params a dictionary of parameters for the plugin to use.
     * @return whether the instantiation was successful.
     */
    public boolean instantiate(String modelDirectory, String modelFilename,
                               HashMap<String, Serializable> params);

    /**
     * Execute a prediction against the given data.
     *
     * @param inputData the input data on which to make the prediction.
     * @return a byte array representing the prediction made by the plugin.
     */
    public byte[] execute(byte[] inputData);

    /**
     * Destroy a plugin instance.
     *
     * @return whether the request to destroy was successful.
     */
    public boolean destroy();
}
