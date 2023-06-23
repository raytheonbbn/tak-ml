package com.bbn.tak.ml.sensor_framework;

import java.util.Collection;
import java.util.UUID;

import de.fraunhofer.iosb.ilt.frostserver.model.Observation;
import de.fraunhofer.iosb.ilt.frostserver.model.Sensor;

/**
 * Interface class that Java-based Sensor Plugins can call directly
 * @author crock
 *
 */
public interface SensorFramework {

	public void recordSensorData(Sensor sensor, Observation observedData);
	
	public void registerSensor(Sensor sensor);
	
	public void deRegisterSensor(Sensor sensor);
	
	public void subscribe(SensorSubscription<?, ?> subscription) throws IllegalArgumentException;
	
	public SensorSubscription<?, ?> unSubscribe(UUID subscriptionId);
	
	public Collection<Sensor> getSensors();
	
	public Collection<SensorSubscription<?, ?>> getSubscriptions();
	
	
}