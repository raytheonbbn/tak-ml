package com.bbn.takml.framework;

import com.bbn.tak.ml.sensor.SensorDataStream;

public interface SensorListChangedCallback {
    public void sensorChangeOccurred(SensorDataStream sensorDataStream, ChangeType changeType);
}
