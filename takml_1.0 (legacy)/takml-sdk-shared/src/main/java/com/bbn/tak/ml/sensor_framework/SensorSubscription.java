package com.bbn.tak.ml.sensor_framework;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * 
 * Consumer is the callback method for getting the result (likely an Observation or ResultSet) to the subscriber. 
 * M - metadata type
 * R - result type
 */
public interface SensorSubscription<M, R> extends Consumer<R>{
	
	public UUID getId();
	
	public boolean matches(M metadata);

}
