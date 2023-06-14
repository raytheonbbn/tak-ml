package com.bbn.takml.mx_framework;

import com.bbn.tak.ml.mx_framework.Model;
import com.bbn.takml.framework.ChangeType;

public interface ModelChangedCallback {
    public void modelChangeOccurred(String modelLabel, Model m, ChangeType changeType);
}
