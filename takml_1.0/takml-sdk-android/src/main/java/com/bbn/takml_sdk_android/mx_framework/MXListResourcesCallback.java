package com.bbn.takml_sdk_android.mx_framework;

import com.bbn.takml_sdk_android.mx_framework.request.MXListResourcesReply;

import java.util.Set;

/**
 * Callback for a request to list resources available in the MX framework.
 */
public interface MXListResourcesCallback {
    /**
     * Callback when a reply to a request to list resources available in the MX framework is available.
     *
     * @param reply reply to a request to list resources available in the MX framework.
     */
    public void listResourcesCB(MXListResourcesReply reply);
}
