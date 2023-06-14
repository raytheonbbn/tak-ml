package com.bbn.takml_sdk_android.mx_framework.request;

import java.io.Serializable;

/**
 * A request for the MX framework.
 * <p>
 * Since the Android implementation of the MX framework uses MQTT
 * to pass messages between machine learning applications, the
 * MX framework, and MX plugins, a message protocol is needed to
 * implement requests and replies. This class is the superclass of
 * many of the request types that go into MQTT messages.
 */
public class MXRequest implements Serializable {
    /**
     * Types of MXRequest messages.
     */
    public enum Type {
        /**
         * Instantiate plugin request.
         */
        INSTANTIATE,
        /**
         * Execute prediction request.
         */
        EXECUTE,
        /**
         * Destroy instance request.
         */
        DESTROY,
        /**
         * Register plugin request.
         */
        REGISTER,
        /**
         * Deregister plugin request.
         */
        DEREGISTER,
        /**
         * List available resources request.
         */
        LIST_RESOURCES,
        /**
         * Destroy all plugin instances.
         */
        DESTROY_ALL_INSTANCES
    };
    private Type type;

    /**
     * Create a new MXRequest
     *
     * @param type the type of request.
     */
    public MXRequest(Type type) {
        this.type = type;
    }

    /**
     * Get the type of request.
     *
     * @return the type of request.
     */
    public Type getType() {
        return this.type;
    }
}
