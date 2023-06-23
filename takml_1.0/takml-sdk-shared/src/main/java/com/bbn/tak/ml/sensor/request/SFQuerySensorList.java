package com.bbn.tak.ml.sensor.request;

// message sent to TAKML Framework to request list of registered sensors
// requesterID uniquely identifies the requester
public class SFQuerySensorList extends SFRequest {

    String requesterID;

    public SFQuerySensorList(String requesterID) {
        super(SFRequest.Type.QUERY_SENSOR_LIST);
        this.requesterID = requesterID;
    }

    public String getRequesterID() {
        return requesterID;
    }
}
