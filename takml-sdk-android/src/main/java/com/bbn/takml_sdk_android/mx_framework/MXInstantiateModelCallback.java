package com.bbn.takml_sdk_android.mx_framework;

import com.bbn.takml_sdk_android.mx_framework.request.MXInstantiateReply;

/**
 * Callback for a request to instantiate an MX plugin.
 */
public interface MXInstantiateModelCallback {
    /**
     * Callback when a reply to a request to instantiate an MX plugin is available.
     *
     * @param reply reply to a request to instantiate an MX plugin.
     */
    public void instantiateCB(MXInstantiateReply reply);
}
