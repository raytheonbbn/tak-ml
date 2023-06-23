package com.bbn.takml_sdk_android;

/**
 * A callback for the connection status to the TAK-ML framework.
 */
public interface TAKMLFrameworkConnectionStatusCallback {
    /**
     * The status of the connection to the TAK-ML framework changed.
     * @param connectedToFramework whether currently connected to the TAK-ML framework.
     */
    void TAKMLFrameworkConnectionChanged(boolean connectedToFramework);
}
