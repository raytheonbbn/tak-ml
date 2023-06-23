package com.bbn.tak.ml.sensor.info;

import java.io.Serializable;
import java.util.List;

import de.fraunhofer.iosb.ilt.frostserver.model.Sensor;

// a list of sensors the TAKML Framework returns in response to an SFQuerySensorList
public class SensorList implements Serializable {

    // each String element here is a serialized Sensor object
    List<String> serializedSensors;

    public SensorList(List<String> serializedSensors) {
        this.serializedSensors = serializedSensors;
    }

    public List<String> getSerializedSensors() {
        return serializedSensors;
    }
}
