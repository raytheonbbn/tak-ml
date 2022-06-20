package com.bbn.takml.mx_framework;

import com.bbn.tak.ml.mx_framework.MXPluginDescription;
import com.bbn.takml.framework.ChangeType;

public interface MXPluginChangedCallback {
    public void mxPluginChangeOccurred(MXPluginDescription desc, ChangeType changeType);
}
