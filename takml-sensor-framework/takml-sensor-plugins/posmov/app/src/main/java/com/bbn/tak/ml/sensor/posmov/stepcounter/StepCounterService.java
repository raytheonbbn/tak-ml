package com.bbn.tak.ml.sensor.posmov.stepcounter;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;

import com.atakmap.android.filesystem.ResourceFile;
import com.bbn.tak.ml.TakMlConstants;
import com.bbn.tak.ml.sensor.SensorControlException;
import com.bbn.tak.ml.sensor.SensorFrameworkClient;
import com.bbn.tak.ml.sensor.SensorPlugin;
import com.bbn.tak.ml.sensor.posmov.BuildConfig;
import com.bbn.tak.ml.sensor.posmov.R;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.time.ZonedDateTime;
import java.util.UUID;

import de.fraunhofer.iosb.ilt.sta.model.Datastream;
import de.fraunhofer.iosb.ilt.sta.model.FeatureOfInterest;
import de.fraunhofer.iosb.ilt.sta.model.Observation;
import de.fraunhofer.iosb.ilt.sta.model.TimeObject;
import de.fraunhofer.iosb.ilt.sta.model.ext.UnitOfMeasurement;

public class StepCounterService extends Service implements SensorEventListener, SensorPlugin {
    private static final String TAG = StepCounterService.class.getSimpleName();
    private PowerManager.WakeLock wakeLock;

    protected FeatureOfInterest localFeatureOfInterest;
    protected de.fraunhofer.iosb.ilt.sta.model.Sensor sensorDescription;
    protected String clientID;
    protected Datastream datastream;
    protected FeatureOfInterest featureOfInterest;
    private SensorFrameworkClient sensorFrameworkClient;
    private SensorManager sensorManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service bound");
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        startForeground(1 , getNotification());

        Log.d(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");

        localFeatureOfInterest = new FeatureOfInterest();

        sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);

        boolean stepCounterAvailable = sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER),
                SensorManager.SENSOR_DELAY_NORMAL);

        Log.d(TAG, "Step counter sensor available: " + stepCounterAvailable);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PosMov::WakeLock");
        wakeLock.acquire();

        String clientId = intent.getStringExtra("SENSOR_ID");
        if(clientId == null) {
            clientId = "StepCounterSensorPlugin";
        }
        String description = "TAK-ML Step Counter Sensor Plugin";
        String encodingType = "application/json";

        sensorDescription = new de.fraunhofer.iosb.ilt.sta.model.Sensor();
        sensorDescription.setName(clientId); //mandatory
        sensorDescription.setDescription(description); //mandatory
        sensorDescription.setEncodingType(encodingType); //mandatory
        sensorDescription.setMetadata("Standard Android step counter"); //mandatory

        datastream = new Datastream();
        datastream.setSensor(getSensorDescription());
        datastream.setName(this.sensorDescription.getName());

        sensorFrameworkClient = new SensorFrameworkClient((msg, ex) -> Log.e(TAG, msg, ex), null, clientId);
        Log.d(TAG, "Connected to sensor framework");

        try {
            sensorFrameworkClient.registerSensor(this);
        } catch (MqttException e) {
            Log.e(TAG, "Unable to register sensor", e);
        }
        Log.d(TAG, "Registered with sensor framework");
        Log.d(TAG, "Beginning to emit events");



        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if(wakeLock.isHeld()) {
            wakeLock.release();
            stopForeground(true);
        }

        sensorManager.unregisterListener(this);

        try {
            sensorFrameworkClient.deRegisterSensor(this);
        } catch (Exception e) {
            Log.e(TAG, "Unable to unregister sensor");
        }
        Log.d(TAG, "Unregistered from sensor framework");

        super.onDestroy();
    }

    private Notification getNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "com.bbn.tak.ml.sensor.posmov",
                    "PosMov Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT); // correct Constant
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);
        }

        Intent atakFrontIntent = new Intent();

        atakFrontIntent.setComponent(new ComponentName("com.atakmap.app", "com.atakmap.app.ATAKActivity"));
        atakFrontIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        atakFrontIntent.putExtra("internalIntent", new Intent("com.bbn.tak.ml.sensor.posmov.SHOW_POSMOV"));
        PendingIntent appIntent = PendingIntent.getActivity(this, 0,
                atakFrontIntent, 0);

        Notification.Builder nb;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nb = new Notification.Builder(this, "com.bbn.tak.ml.sensor.posmov");
        } else {
            nb = new Notification.Builder(this);
        }

        nb.setContentTitle("TAK-ML PosMov Sensor Plugin").setContentText("PosMov")
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(appIntent);
        nb.setOngoing(false);
        nb.setAutoCancel(true);

        return nb.build();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Log.d(TAG, "Sensor changed");
        if(sensorEvent.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            Observation thisObservation = new Observation();

            String msg = "{stepCount=" + sensorEvent.values[0] + "}";

            thisObservation.setPhenomenonTime(new TimeObject(ZonedDateTime.now()));
            thisObservation.setResultTime(ZonedDateTime.now());
            thisObservation.setDatastream(datastream);
            thisObservation.setFeatureOfInterest(this.localFeatureOfInterest);
            thisObservation.setResult(msg);

            sensorFrameworkClient.publishSensorData(thisObservation);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        Log.d(TAG, "Sensor accuracy changed");
    }

    @Override
    public String getID() {
        return this.getClass().getSimpleName();
    }

    @Override
    public de.fraunhofer.iosb.ilt.sta.model.Sensor getSensorDescription() {
        return sensorDescription;
    }

    @Override
    public UnitOfMeasurement getUnitOfMeasurement() {
        return new UnitOfMeasurement("steps", "steps", "Number of steps taken by the user since the last reboot while the sensor was activated.");
    }

    @Override
    public void start() throws SensorControlException {

    }

    @Override
    public void stop() throws SensorControlException {

    }
}