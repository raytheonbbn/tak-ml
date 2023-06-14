package com.bbn.takml_sdk_android.mx_framework;

import com.bbn.takml_sdk_android.mx_framework.request.MXExecuteReply;

/**
 * Callback for a request to execute a prediction using an MX plugin.
 */
public interface MXExecuteModelCallback {
    /**
     * Callback when a reply to a request to execute a prediction is available.
     *
     * @param reply reply to a request to execute a prediction.
     */
    public void executeCB(MXExecuteReply reply);
}
