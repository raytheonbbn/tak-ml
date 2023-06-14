package com.bbn.takml.mx_framework;

import com.bbn.takml.framework.ChangeType;

public interface InstanceChangedCallback {
    public void instanceChangeOccurred(String pluginID, String mxpInstanceID, ChangeType changeType);
}
