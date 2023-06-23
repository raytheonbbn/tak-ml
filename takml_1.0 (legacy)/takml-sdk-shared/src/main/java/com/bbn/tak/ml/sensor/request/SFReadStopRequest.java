package com.bbn.tak.ml.sensor.request;

public class SFReadStopRequest extends SFRequest {
    private String readRequesterID;
    private String sensorPluginID;

    public SFReadStopRequest(String readRequesterID, String sensorPluginID) {
        super(Type.READ_STOP_RQST);
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
