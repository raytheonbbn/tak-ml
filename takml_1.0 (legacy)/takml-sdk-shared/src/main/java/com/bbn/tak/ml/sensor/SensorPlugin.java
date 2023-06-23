package com.bbn.tak.ml.sensor;

import de.fraunhofer.iosb.ilt.sta.model.Sensor;
import de.fraunhofer.iosb.ilt.sta.model.ext.UnitOfMeasurement;

public interface SensorPlugin {
	public String getID();
	
	public Sensor getSensorDescription();
	
	public UnitOfMeasurement getUnitOfMeasurement();
	
    public void start() throws SensorControlException;

    public void stop() throws SensorControlException;
}
