package com.bbn.takml_sdk_android.mx_framework.request;

import java.io.Serializable;

/**
 * Reply for instantiating an MX plugin.
 */
public class MXInstantiateReply implements Serializable {
    private static final long serialVersionUID = 1L;
    private String token;
    private String mxpInstanceID;
    private String pluginID;
    private boolean success;
    private String msg;

    /**
     * Create a new reply to a request to instantiate an MX plugin.
     *
     * @param token the token given by the application to match this asynchronous reply to the request.
     * @param mxpInstanceID the assigned instance ID.
     * @param pluginID the plugin ID for the plugin that was instantiated.
     * @param success whether the plugin was successfully instantiated.
     * @param msg an informational message about the outcome of the instantiate request.
     */
    public MXInstantiateReply(String token, String mxpInstanceID, String pluginID,
                              boolean success, String msg) {
        this.token = token;
        this.mxpInstanceID = mxpInstanceID;
        this.pluginID = pluginID;
        this.success = success;
        this.msg = msg;
    }

    /**
     * Get the token given by the application to match this asynchronous reply to the request.
     *
     * @return the token given by the application to match this asynchronous reply to the request.
     */
    public String getToken() {
        return this.token;
    }

    /**
     * Get the assigned instance ID.
     *
     * @return the assigned instance ID.
     */
    public String getMxpInstanceID() {
        return this.mxpInstanceID;
    }

    /**
     * Get the plugin ID for the plugin that was instantiated.
     *
     * @return the plugin ID for the plugin that was instantiated.
     */
    public String getPluginID() {
        return this.pluginID;
    }

    /**
     * Whether the plugin was successfully instantiated.
     *
     * @return whether the plugin was successfully instantiated.
     */
    public boolean isSuccess() {
        return this.success;
    }

    /**
     * Get an informational message about the outcome of the instantiate request.
     * @return an informational message about the outcome of the instantiate request.
     */
    public String getMsg() {
        return this.msg;
    }
}
