package com.bbn.takml_sdk_android.mx_framework.request;

/**
 * Request to execute a prediction using an MX plugin.
 */
public class MXExecuteRequest extends MXRequest {
    private String pluginID;
    private String mxpInstanceID;
    private String executeID;
    private byte[] inputData;

    /**
     * Create a new request to execute a prediction using an MX plugin.
     *
     * @param pluginID the ID of the MX plugin to instantiate.
     * @param mxpInstanceID the ID of the instance used to make the prediction.
     * @param executeID the value assigned by the application to match this request to the asynchronous reply it will receive.
     * @param inputData the data over which to make the prediction.
     */
    public MXExecuteRequest(String pluginID, String mxpInstanceID,
                            String executeID, byte[] inputData) {
        super(MXRequest.Type.EXECUTE);
        this.pluginID = pluginID;
        this.mxpInstanceID = mxpInstanceID;
        this.executeID = executeID;
        this.inputData = inputData;
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
     * Get the ID of the instance used to make the prediction.
     *
     * @return the ID of the instance used to make the prediction.
     */
    public String getMxpInstanceID() {
        return this.mxpInstanceID;
    }

    /**
     * Get the value assigned by the application to match this request to the asynchronous reply it will receive.
     *
     * @return the value assigned by the application to match this request to the asynchronous reply it will receive.
     */
    public String getExecuteID() {
        return this.executeID;
    }

    /**
     * Get the data over which to make the prediction.
     *
     * @return the data over which to make the prediction.
     */
    public byte[] getInputData() {
        return this.inputData;
    }
}
