package com.bbn.takml.sensor_framework;

import java.util.UUID;

import de.fraunhofer.iosb.ilt.sta.model.Sensor;

public class SensorDescription {
    private String id;
    private String name;
    private String description;

    public SensorDescription() {
        id = UUID.randomUUID().toString();
        name = id;
        description = "";
    }

    public SensorDescription(Sensor sensor) {
        this.id = sensor.getId() == null ? sensor.getName() : sensor.getId().toString();
        this.name = sensor.getName();
        this.description = sensor.getDescription();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
