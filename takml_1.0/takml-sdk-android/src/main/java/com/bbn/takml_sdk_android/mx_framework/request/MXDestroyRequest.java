package com.bbn.takml_sdk_android.mx_framework.request;

/**
 * Request to destroy an MX plugin instance.
 */
public class MXDestroyRequest extends MXRequest {
    private String pluginID;
    private String mxpInstanceID;

    /**
     * Create a new request to destroy an MX plugin instance.
     *
     * @param pluginID the ID of the plugin.
     * @param mxpInstanceID the ID of the instance to destroy.
     */
    public MXDestroyRequest(String pluginID, String mxpInstanceID) {
        super(MXRequest.Type.DESTROY);
        this.pluginID = pluginID;
        this.mxpInstanceID = mxpInstanceID;
    }

    /**
     * Get the ID of the plugin.
     *
     * @return the ID of the plugin.
     */
    public String getPluginID() {
        return this.pluginID;
    }

    /**
     * Get the ID of the instance to destroy.
     *
     * @return the ID of the instance to destroy.
     */
    public String getMxpInstanceID() {
        return this.mxpInstanceID;
    }
}
