package com.bbn.takml_sdk_android.mx_framework.request;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Request to instantiate an MX plugin.
 */
public class MXInstantiateRequest extends MXRequest {
    private String pluginID;
    private String modelDirectory;
    private String modelFilename;
    private String token;
    private String mxpInstanceID;
    private HashMap<String, Serializable> params;

    /**
     * Create a new request to instantiate an MX plugin.
     *
     * @param pluginID the ID of the MX plugin to instantiate.
     * @param modelDirectory the directory of the model file.
     * @param modelFilename the filename of the model file.
     * @param token the token created by the application to match this instantiation request to the response it will asynchronously receive.
     * @param params the dictionary of parameters to instantiate the plugin.
     */
    public MXInstantiateRequest(String pluginID, String modelDirectory,
                                String modelFilename, String token,
                                HashMap<String, Serializable> params) {
        super(MXRequest.Type.INSTANTIATE);
        this.pluginID = pluginID;
        this.modelDirectory = modelDirectory;
        this.modelFilename = modelFilename;
        this.token = token;
        this.params = params;
    }

    /* Use whatever the default directory for models is. */
    public MXInstantiateRequest(String pluginID, String modelFilename, String token,
                                HashMap<String, Serializable> params) {
        this(pluginID, null, modelFilename, token, params);
    }

    /**
     * Get the ID of the MX plugin to instantiate.
     *
     * @return the ID of the MX plugin to instantiate.
     */
    public String getPluginID() {
        return this.pluginID;
    }

    /**
     * Get the directory of the model file.
     *
     * @return the directory of the model file.
     */
    public String getModelDirectory() {
        return this.modelDirectory;
    }

    /**
     * Set the directory of the model file.
     * <p>
     * The directory of the model can be changed by the framework,
     * or set to a default, when the directory is not given by the application.
     *
     * @param modelDirectory the directory of the model file.
     */
    public void setModelDirectory(String modelDirectory) {
        this.modelDirectory = modelDirectory;
    }

    /**
     * Set the ID of the instance of thie plugin.
     * <p>
     * After successfully assigning an instance ID to this request,
     * the framework can add it to the request.
     *
     * @param mxpInstanceID the ID of the instance of the plugin.
     */
    public void setMxpInstanceID(String mxpInstanceID) {
        this.mxpInstanceID = mxpInstanceID;
    }

    /**
     * Get the ID of the instance of the plugin.
     *
     * @return the ID of the instance of the plugin.
     */
    public String getMxpInstanceID() {
        return this.mxpInstanceID;
    }

    /**
     * Get the filename of the model file.
     *
     * @return the filename of the model file.
     */
    public String getModelFilename() {
        return this.modelFilename;
    }

    /**
     * Get the token created by the application to match this instantiation request to the response it will asynchronously receive.
     *
     * @return the token created by the application to match this instantiation request to the response it will asynchronously receive.
     */
    public String getToken() {
        return this.token;
    }

    /**
     * Get the dictionary of parameters to instantiate the plugin.
     *
     * @return the dictionary of parameters to instantiate the plugin.
     */
    public HashMap<String, Serializable> getParams() {
        return this.params;
    }
}
