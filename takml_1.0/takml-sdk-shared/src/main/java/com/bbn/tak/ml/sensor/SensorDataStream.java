package com.bbn.tak.ml.sensor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.fraunhofer.iosb.ilt.sta.model.Sensor;
import de.fraunhofer.iosb.ilt.sta.model.ext.UnitOfMeasurement;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SensorDataStream {
	private Sensor sensor;
	private UnitOfMeasurement unitOfMeasurement;
	private String streamName;
	private String streamDescription;
	
	public String getID() {
		if(sensor.getId() == null) {
			return sensor.getName();
		} else {
			return sensor.getId().toString();
		}
	}
	
	public Sensor getSensor() {
		return sensor;
	}
	public void setSensor(Sensor sensor) {
		this.sensor = sensor;
	}
	public UnitOfMeasurement getUnitOfMeasurement() {
		return unitOfMeasurement;
	}
	public void setUnitOfMeasurement(UnitOfMeasurement unitOfMeasurement) {
		this.unitOfMeasurement = unitOfMeasurement;
	}
	public String getStreamName() {
		return streamName;
	}
	public void setStreamName(String streamName) {
		this.streamName = streamName;
	}
	public String getStreamDescription() {
		return streamDescription;
	}
	public void setStreamDescription(String streamDescription) {
		this.streamDescription = streamDescription;
	}
}
