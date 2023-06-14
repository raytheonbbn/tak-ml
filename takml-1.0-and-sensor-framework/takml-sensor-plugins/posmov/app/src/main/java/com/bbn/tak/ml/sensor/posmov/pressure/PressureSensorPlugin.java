package com.bbn.tak.ml.sensor.posmov.pressure;

import android.content.Context;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.preference.UnitPreferences;
import com.atakmap.coremap.log.Log;
import com.bbn.tak.ml.TakMlConstants;
import com.bbn.tak.ml.sensor.SensorControlException;
import com.bbn.tak.ml.sensor.SensorFrameworkClient;
import com.bbn.tak.ml.sensor.SensorPlugin;
import com.bbn.tak.ml.sensor.posmov.BasePosMovSensor;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.UUID;

import de.fraunhofer.iosb.ilt.sta.model.Datastream;
import de.fraunhofer.iosb.ilt.sta.model.FeatureOfInterest;
import de.fraunhofer.iosb.ilt.sta.model.Observation;
import de.fraunhofer.iosb.ilt.sta.model.Sensor;
import de.fraunhofer.iosb.ilt.sta.model.TimeObject;
import de.fraunhofer.iosb.ilt.sta.model.ext.UnitOfMeasurement;

public class PressureSensorPlugin extends BasePosMovSensor implements SensorEventListener {
    public static final String TAG = PressureSensorPlugin.class.getSimpleName();
    private SensorManager sensorManager;

    public PressureSensorPlugin(Context context, String clientId, Datastream datastream, FeatureOfInterest featureOfInterest) {
        super(context, clientId, datastream, featureOfInterest);
    }

    @Override
    protected boolean startSensorDataReporting(){

        sensorManager = (SensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);

        sensorManager.registerListener(this,
                        sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_PRESSURE),
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

            // since this is an pressure sensor plugin, only process messages from the pressure
            case android.hardware.Sensor.TYPE_PRESSURE:
                // Sensor data
                float millibarsOfPressure = event.values[0];
                String msg = "{millibarsPressure=" + event.values[0] + "}";

                Log.d(TAG, "Received pressure reading: " + msg);

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
        if (sensor.getType() == android.hardware.Sensor.TYPE_PRESSURE) {
            Log.d(TAG, "Accuracy for the pressure: " + accuracy);
        }
    }

    @Override
    public UnitOfMeasurement getUnitOfMeasurement() {
        return new UnitOfMeasurement("hPa", "hPa", "Ambient air pressure");
    }
}
