package com.bbn.tak.ml.sensor.request;

import java.io.Serializable;

public class SFRequest implements Serializable {
    public enum Type {
        READ_START_RQST,
        READ_STOP_RQST,
        READ_START_CMD,
        READ_STOP_CMD,
        QUERY_SENSOR_LIST
    };
    private Type type;

    public SFRequest(Type type) {
        this.type = type;
    }

    public Type getType() {
        return this.type;
    }
}
