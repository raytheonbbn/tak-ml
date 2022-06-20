package com.bbn.tak.ml.sensor.posmov;

import android.content.Context;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.preference.UnitPreferences;
import com.atakmap.coremap.log.Log;
import com.bbn.tak.ml.sensor.SensorControlException;
import com.bbn.tak.ml.sensor.SensorFrameworkClient;
import com.bbn.tak.ml.sensor.SensorPlugin;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.UUID;

import de.fraunhofer.iosb.ilt.sta.model.Datastream;
import de.fraunhofer.iosb.ilt.sta.model.FeatureOfInterest;
import de.fraunhofer.iosb.ilt.sta.model.Sensor;

public abstract class BasePosMovSensor implements SensorPlugin {
    private static final String TAG = BasePosMovSensor.class.getSimpleName();
    protected Context context;
    protected SensorFrameworkClient sensorFrameworkClient;

    protected FeatureOfInterest localFeatureOfInterest;
    protected Sensor sensorDescription;
    protected String clientID;
    protected Datastream datastream;
    protected FeatureOfInterest featureOfInterest;

    public BasePosMovSensor(Context context, String clientId, Datastream datastream, FeatureOfInterest featureOfInterest) {
        this.context = context;
        this.datastream = datastream;
        this.featureOfInterest = featureOfInterest;

        String encodingType = "http://schema.org/description";

        sensorDescription = new Sensor();
        sensorDescription.setName(clientId); //mandatory
        sensorDescription.setDescription(clientId); //mandatory
        sensorDescription.setEncodingType(encodingType); //mandatory
        //sensorDescription.setId()
        sensorDescription.setMetadata(clientId); //mandatory
        //sensorDescription.setDatastream() //mandatory
        //sensorDescription.setProperties();
    }

    public Datastream getDatastream() {
        if(datastream == null) {
            datastream = new Datastream();
            datastream.setSensor(getSensorDescription());
            datastream.setName(this.sensorDescription.getName());
            Log.d(TAG, "Creating datastream " + datastream.getName());
        }

        return datastream;
    }

    @Override
    public void start() throws SensorControlException {
        sensorFrameworkClient = new SensorFrameworkClient((msg, ex) -> Log.e(TAG, msg, ex), null, sensorDescription.getName());
        Log.d(TAG, "Connected to sensor framework");

        try {
            sensorFrameworkClient.registerSensor(this);
        } catch (MqttException e) {
            throw new SensorControlException("Unable to register sensor", e);
        }
        Log.d(TAG, "Registered with sensor framework");

        Log.d(TAG, "Beginning to emit events");
        if(!startSensorDataReporting()) {
            throw new SensorControlException("Unable to start the sensor listening");
        }
    }

    @Override
    public void stop() throws SensorControlException {

        if(!stopSensorDataReporting()) {
            throw new SensorControlException("Unable to stop the sensor listening");
        }
        Log.d(TAG, "Stopped emitting eventgs to sensor framework");

        try {
            sensorFrameworkClient.deRegisterSensor(this);
        } catch (Exception e) {
            throw new SensorControlException("Unable to unregister sensor", e);
        }
        Log.d(TAG, "Unregistered from sensor framework");
    }

    //============================================
    //  Functions overriden from SensorPlugin
    //============================================
    @Override
    public String getID() {
        return this.getClass().getSimpleName();
    }

    @Override
    public Sensor getSensorDescription() {
        return sensorDescription;
    }


    protected abstract boolean startSensorDataReporting();

    protected abstract boolean stopSensorDataReporting();
}
