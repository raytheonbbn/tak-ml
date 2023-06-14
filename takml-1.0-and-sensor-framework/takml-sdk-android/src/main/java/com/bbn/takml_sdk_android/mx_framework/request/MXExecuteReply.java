package com.bbn.takml_sdk_android.mx_framework.request;

import java.io.Serializable;

/**
 * Reply for executing a prediction using an MX plugin.
 */
public class MXExecuteReply implements Serializable {
    private String executeID;
    private byte[] bytes;
    private boolean success;
    private String msg;

    /**
     * Create a new reply to a request to execute a prediction using an MX plugin.
     *
     * @param executeID the value passed by the application to match this asynchronous reply to the request.
     * @param bytes the prediction made by the MX plugin.
     * @param success whether the plugin successfully executed the prediction.
     * @param msg an informational message about the outcome of the execute request.
     */
    public MXExecuteReply(String executeID, byte[] bytes, boolean success, String msg) {
        this.executeID = executeID;
        this.bytes = bytes;
        this.success = success;
        this.msg = msg;
    }

    /**
     * Get the value passed by the application to match this asynchronous reply to the request.
     *
     * @return the value passed by the application to match this asynchronous reply to the request.
     */
    public String getExecuteID() {
        return this.executeID;
    }

    /**
     * Get the prediction made by the MX plugin.
     *
     * @return the prediction made by the MX plugin.
     */
    public byte[] getBytes() {
        return this.bytes;
    }

    /**
     * Whether the plugin successfully executed the prediction.
     *
     * @return whether the plugin successfully executed the prediction.
     */
    public boolean isSuccess() {
        return this.success;
    }

    /**
     * Get an informational message about the outcome of the execute request.
     *
     * @return an informational message about the outcome of the execute request.
     */
    public String getMsg() {
        return this.msg;
    }
}
