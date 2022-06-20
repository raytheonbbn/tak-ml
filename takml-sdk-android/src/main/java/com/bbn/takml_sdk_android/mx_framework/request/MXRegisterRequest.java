package com.bbn.takml_sdk_android.mx_framework.request;

import com.bbn.tak.ml.mx_framework.MXPluginDescription;

/**
 * Request to register a plugin with the MX framework.
 */
public class MXRegisterRequest extends MXRequest {
    private MXPluginDescription desc;
    private String topic;

    /**
     * Create a new request to register an MX plugin.
     *
     * @param desc the MX plugin description annotation.
     * @param topic the MQTT topic that the registering MX plugin listens on.
     */
    public MXRegisterRequest(MXPluginDescription desc, String topic) {
        super(MXRequest.Type.REGISTER);
        this.desc = desc;
        this.topic = topic;
    }

    /**
     * Get the MX plugin description annotation.
     *
     * @return the MX plugin description annotation.
     */
    public MXPluginDescription getPluginDescription() {
        return this.desc;
    }

    /**
     * Get the ID of the registering MX plugin.
     *
     * @return the MX plugin ID.
     */
    public String getPluginID() {
        return this.desc.id();
    }

    /**
     * Get the MQTT topic that the registering MX plugin listens on.
     *
     * @return the MQTT topic that the registering MX plugin listens on.
     */
    public String getTopic() {
        return this.topic;
    }
}
