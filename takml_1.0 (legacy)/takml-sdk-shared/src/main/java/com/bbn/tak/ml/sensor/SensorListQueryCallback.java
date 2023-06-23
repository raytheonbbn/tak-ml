package com.bbn.tak.ml.sensor;

import java.util.List;

import de.fraunhofer.iosb.ilt.frostserver.model.Sensor;

public interface SensorListQueryCallback {
    void receivedSensorList(List<Sensor> sensors);
}
