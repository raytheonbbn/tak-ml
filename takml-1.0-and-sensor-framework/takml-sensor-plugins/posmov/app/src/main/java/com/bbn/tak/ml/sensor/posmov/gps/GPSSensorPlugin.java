package com.bbn.tak.ml.sensor.posmov.gps;

import android.content.Context;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;

import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;
import com.bbn.tak.ml.TakMlConstants;
import com.bbn.tak.ml.sensor.posmov.BasePosMovSensor;

import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.fraunhofer.iosb.ilt.sta.model.Datastream;
import de.fraunhofer.iosb.ilt.sta.model.FeatureOfInterest;
import de.fraunhofer.iosb.ilt.sta.model.Observation;
import de.fraunhofer.iosb.ilt.sta.model.TimeObject;
import de.fraunhofer.iosb.ilt.sta.model.ext.UnitOfMeasurement;

public class GPSSensorPlugin extends BasePosMovSensor{
    public static final String TAG = GPSSensorPlugin.class.getSimpleName();
    private LocationManager locationManager;
    private ScheduledExecutorService scheduler;
    private MapView mapView;

    public GPSSensorPlugin(Context context, MapView mapView, String clientId, Datastream datastream, FeatureOfInterest featureOfInterest) {
        super(context, clientId, datastream, featureOfInterest);

        this.mapView = mapView;
    }

    @Override
    protected boolean startSensorDataReporting() {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        final boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!gpsEnabled) {
            Log.d(TAG, "GPS is disabled, unable to register listener");
            return false;
        } else {
            scheduler = Executors.newScheduledThreadPool(1);
            Log.d(TAG, "Scheduling GPS listener");
            scheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    if(mapView == null || mapView.getSelfMarker() == null || mapView.getSelfMarker().getPoint() == null) {
                        Log.d(TAG, "No location data currently available, not reporting.");
                        return;
                    }
                    double lat = mapView.getSelfMarker().getPoint().getLatitude();
                    double lon = mapView.getSelfMarker().getPoint().getLongitude();
                    Log.d(TAG, "Current lat: " + lat);
                    Log.d(TAG, "Current lon: " + lon);

                    // Sensor data
                    String msg = "{" +
                            "'lat': " + lat + "," +
                            "'lon': " + lon + "}";

                    Log.d(TAG, "Received GPS reading: " + msg);

                    // Create a SensorThings Observation object
                    Observation thisObservation = new Observation();

                    thisObservation.setPhenomenonTime(new TimeObject(ZonedDateTime.now()));
                    thisObservation.setResultTime(ZonedDateTime.now());
                    thisObservation.setDatastream(GPSSensorPlugin.this.getDatastream());
                    thisObservation.setFeatureOfInterest(GPSSensorPlugin.this.localFeatureOfInterest);
                    thisObservation.setResult(msg);

                    sensorFrameworkClient.publishSensorData(thisObservation);
                }
            }, 0,1000, TimeUnit.MILLISECONDS );
        }

        return true;
    };

    @Override
    protected boolean stopSensorDataReporting() {
        scheduler.shutdown();

        return true;
    }

    @Override
    public UnitOfMeasurement getUnitOfMeasurement() {
        return new UnitOfMeasurement("lat/lon", "lat/lon" ,"Latitude and longitude");
    }
}
