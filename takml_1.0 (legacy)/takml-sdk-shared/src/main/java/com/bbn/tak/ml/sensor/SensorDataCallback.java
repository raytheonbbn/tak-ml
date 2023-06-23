package com.bbn.tak.ml.sensor;

import de.fraunhofer.iosb.ilt.sta.model.Observation;

public interface SensorDataCallback {
    public void dataAvailable(Observation result);
}
