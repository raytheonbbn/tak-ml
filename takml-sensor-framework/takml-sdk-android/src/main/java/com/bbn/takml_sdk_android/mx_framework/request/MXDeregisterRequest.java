package com.bbn.takml_sdk_android.mx_framework.request;

/**
 * Request to deregister a plugin with the MX framework.
 */
public class MXDeregisterRequest extends MXRequest {
    private String pluginID;

    /**
     * Create a request to deregister an MX plugin.
     *
     * @param pluginID the ID of the plugin requesting to deregister.
     */
    public MXDeregisterRequest(String pluginID) {
        super(MXRequest.Type.DEREGISTER);
        this.pluginID = pluginID;
    }

    /**
     * Get the ID of the plugin requesting to deregister.
     *
     * @return the ID of the plugin requesting to deregister.
     */
    public String getPluginID() {
        return this.pluginID;
    }
}
