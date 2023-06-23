package com.bbn.tak.ml.sensor.request;

public class SFReadStartRequest extends SFRequest {
    private String readRequesterID;
    private String sensorPluginID;

    public SFReadStartRequest(String readRequesterID, String sensorPluginID) {
        super(Type.READ_START_RQST);
        this.readRequesterID = readRequesterID;
        this.sensorPluginID = sensorPluginID;
    }

    public String getReadRequesterID() {
        return readRequesterID;
    }

    public String getSensorPluginID() {
        return sensorPluginID;
    }
}
