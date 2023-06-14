package com.bbn.tak.ml.sensor.posmov.magnetometer;

import android.content.Context;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.atakmap.coremap.log.Log;
import com.bbn.tak.ml.TakMlConstants;
import com.bbn.tak.ml.sensor.posmov.BasePosMovSensor;

import java.time.ZonedDateTime;

import de.fraunhofer.iosb.ilt.sta.model.Datastream;
import de.fraunhofer.iosb.ilt.sta.model.FeatureOfInterest;
import de.fraunhofer.iosb.ilt.sta.model.Observation;
import de.fraunhofer.iosb.ilt.sta.model.TimeObject;
import de.fraunhofer.iosb.ilt.sta.model.ext.UnitOfMeasurement;

public class MagnetometerSensorPlugin extends BasePosMovSensor implements SensorEventListener {
    public static final String TAG = MagnetometerSensorPlugin.class.getSimpleName();
    private SensorManager sensorManager;
    public MagnetometerSensorPlugin(Context context, String clientId, Datastream datastream, FeatureOfInterest featureOfInterest) {
        super(context, clientId, datastream, featureOfInterest);
    }

    @Override
    protected boolean startSensorDataReporting(){

        sensorManager = (SensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_NORMAL);
        return true;
    }

    @Override
    protected boolean stopSensorDataReporting() {
        sensorManager.unregisterListener(this);
        return true;
    }
    //==================================================
    //  Functions overriden from SensorEventListener
    //==================================================
    @Override
    public void onSensorChanged(final SensorEvent event) {

        switch(event.sensor.getType()) {

            // since this is an accelerometer sensor plugin, only process messages from the accelerometer
            case android.hardware.Sensor.TYPE_MAGNETIC_FIELD:
                // Sensor data
                String msg = "{x=" + event.values[0] + ";" +
                        "y=" + event.values[1] + ";" +
                        "z=" + event.values[2] + "}";

                Log.d(TAG, "Received magnetometer reading: " + msg);

                // Create a SensorThings Observation object
                Observation thisObservation = new Observation();

                thisObservation.setPhenomenonTime(new TimeObject(ZonedDateTime.now()));
                thisObservation.setResultTime(ZonedDateTime.now());
                thisObservation.setDatastream(getDatastream());
                thisObservation.setFeatureOfInterest(this.localFeatureOfInterest);
                thisObservation.setResult(msg);

                sensorFrameworkClient.publishSensorData(thisObservation);
                break;
            default:
                break;
        }

        return;
    }


    @Override
    public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {
        if (sensor.getType() == android.hardware.Sensor.TYPE_MAGNETIC_FIELD) {
            Log.d(TAG, "Accuracy for the magnetometer: " + accuracy);
        }
    }

    @Override
    public UnitOfMeasurement getUnitOfMeasurement() {
        return new UnitOfMeasurement("muT","muT","Raw field strength in mu T for each coordinate axis");
    }
}
