
package com.bbn.tak.ml.sensor.posmov;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;

import com.atakmap.coremap.log.Log;
import com.bbn.tak.ml.sensor.SensorPlugin;
import com.bbn.tak.ml.sensor.posmov.accelerometer.AccelerometerSensorPlugin;
import com.bbn.tak.ml.sensor.posmov.gps.GPSSensorPlugin;
import com.bbn.tak.ml.sensor.posmov.gyroscope.GyroscopeSensorPlugin;
import com.bbn.tak.ml.sensor.posmov.magnetometer.MagnetometerSensorPlugin;
import com.bbn.tak.ml.sensor.posmov.pressure.PressureSensorPlugin;
import com.bbn.tak.ml.sensor.posmov.stepcounter.StepCounterService;

import org.eclipse.paho.client.mqttv3.MqttException;

import de.fraunhofer.iosb.ilt.sta.model.Datastream;
import de.fraunhofer.iosb.ilt.sta.model.FeatureOfInterest;

public class PosMovPluginDropDownReceiver extends DropDownReceiver implements OnStateListener {

    public static final String TAG = PosMovPluginDropDownReceiver.class
            .getSimpleName();

    public static final String SHOW_PLUGIN = "com.bbn.tak.ml.sensor.posmov.SHOW_PLUGIN";
    private static final int PERMISSIONS_REQUEST_ACTIVITY_RECOGNITION = 1;
    private final View templateView;
    private final Context pluginContext;

    private MagnetometerSensorPlugin magnetometerSensorPlugin;
    private AccelerometerSensorPlugin accelerometerSensorPlugin;
    private GyroscopeSensorPlugin gyroscopeSensorPlugin;
    private PressureSensorPlugin pressureSensorPlugin;
    private GPSSensorPlugin gpsSensorPlugin;

    private static final String MAG_SENSOR_ID = MagnetometerSensorPlugin.class.getSimpleName();
    private static final String ACCEL_SENSOR_ID = AccelerometerSensorPlugin.class.getSimpleName();
    private static final String GYRO_SENSOR_ID = GyroscopeSensorPlugin.class.getSimpleName();
    private static final String PRESSURE_SENSOR_ID = PressureSensorPlugin.class.getSimpleName();
    private static final String GPS_SENSOR_ID = GPSSensorPlugin.class.getSimpleName();
    private static final String STEP_COUNTER_SENSOR_ID = "StepCounterSensorPlugin";

    /**************************** CONSTRUCTOR *****************************/

    public PosMovPluginDropDownReceiver(final MapView mapView,
                                                    final Context context) {
        super(mapView);
        this.pluginContext = context;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        templateView = inflater.inflate(R.layout.main_layout, null);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No permissions");
        }

        try {
            createSensorInstances();
        } catch (MqttException e) {
            Log.e(TAG, "Unable to create sensor plugins");
            Toast.makeText(context, "Unable to create sensor plugins", Toast.LENGTH_LONG);
            return;
        }


        // ** setup handlers for sensor on/off toggles
        setupToggleForSensor((Switch)templateView.findViewById(R.id.magnetometerToggle_btn), magnetometerSensorPlugin);
        setupToggleForSensor((Switch)templateView.findViewById(R.id.accelerometerToggle_btn), accelerometerSensorPlugin);
        setupToggleForSensor((Switch)templateView.findViewById(R.id.gyroscopeToggle_btn), gyroscopeSensorPlugin);
        setupToggleForSensor((Switch)templateView.findViewById(R.id.pressureToggle_btn), pressureSensorPlugin);
        setupToggleForSensor((Switch)templateView.findViewById(R.id.gpsToggle_btn), gpsSensorPlugin);

        ((Switch)templateView.findViewById(R.id.stepCountToggle_btn)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked) {
                    startStepCounterService();
                } else {
                    stopStepCounterService();
                }
            }
        });

        /*
        if(ContextCompat.checkSelfPermission(pluginContext, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED){
            Log.d(TAG, "No ACTIVITY_RECOGNITION permission available");
            //ActivityCompat.requestPermissions((Activity)mapView.getContext(), new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, PERMISSIONS_REQUEST_ACTIVITY_RECOGNITION);

            Intent intent = new Intent();
            intent.setClassName("com.bbn.tak.ml.sensor.posmov",
                    "com.bbn.tak.ml.sensor.posmov.ABCMainActivity");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            Log.d(TAG, "Showing new activity");
            getMapView().getContext().startActivity(intent);
            Log.d(TAG, "Requested activity start");

        }*/
    }

    private void stopStepCounterService() {
        Log.d(TAG, "Stopping Step Counter Service");
        Intent stopServiceIntent = new Intent(StepCounterService.class.getName());
        stopServiceIntent.setPackage("com.bbn.tak.ml.sensor.posmov");

        getMapView().getContext().stopService(stopServiceIntent);
    }

    private void startStepCounterService() {
        Log.d(TAG, "Starting Step Counter Service");

        Intent startServiceIntent = new Intent(StepCounterService.class.getName());
        startServiceIntent.setPackage("com.bbn.tak.ml.sensor.posmov");
        startServiceIntent.putExtra("SENSOR_ID", STEP_COUNTER_SENSOR_ID);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "Service launch path 1");
            ComponentName svcName = getMapView().getContext().startForegroundService(startServiceIntent);
            Log.d(TAG, "Service create: " + svcName);
        } else {
            Log.d(TAG, "Service launch path 2");
            ComponentName svcName = getMapView().getContext().startService(startServiceIntent);
            Log.d(TAG, "Service create: " + svcName);
        }
    }

    private void setupToggleForSensor(Switch toggle, SensorPlugin plugin) {
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                try {
                    if (checked) {
                        plugin.start();
                    } else {
                        plugin.stop();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error starting or stopping " + plugin.getClass().getSimpleName() + ": " + e.getMessage());
                    e.printStackTrace();
                    toggle.setChecked(!checked);
                    Toast.makeText(pluginContext, "Error starting or stopping " + plugin.getClass().getSimpleName() + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void createSensorInstances() throws MqttException {
        Log.d(TAG, "Creating mangetometer sensor plugin with TAK-ML");
        FeatureOfInterest magnetometerSensorFeatureOfInterest = new FeatureOfInterest();

        magnetometerSensorPlugin = new MagnetometerSensorPlugin(pluginContext, MAG_SENSOR_ID, null, magnetometerSensorFeatureOfInterest);
        // ** TODO: start defaulted to on or off? we should persist the state of this switch probably
        //magnetometerSensorPlugin.start();

        Log.d(TAG, "Creating accelerometer sensor plugin with TAK-ML");
        FeatureOfInterest accelerometerSensorFeatureOfInterest = new FeatureOfInterest();

        accelerometerSensorPlugin = new AccelerometerSensorPlugin(pluginContext, ACCEL_SENSOR_ID, null, accelerometerSensorFeatureOfInterest);
        //accelerometerSensorPlugin.start();

        Log.d(TAG, "Creating gyroscope sensor plugin with TAK-ML");
        FeatureOfInterest gyroSensorFeatureOfInterest = new FeatureOfInterest();

        gyroscopeSensorPlugin = new GyroscopeSensorPlugin(pluginContext, GYRO_SENSOR_ID, null, gyroSensorFeatureOfInterest);
        //gyroscopeSensorPlugin.start();

        Log.d(TAG, "Creating pressure sensor plugin with TAK-ML");
        FeatureOfInterest pressureSensorFeatureOfInterest = new FeatureOfInterest();

        pressureSensorPlugin = new PressureSensorPlugin(pluginContext, PRESSURE_SENSOR_ID, null, pressureSensorFeatureOfInterest);
        //pressureSensorPlugin.start();

        Log.d(TAG, "Creating GPS sensor plugin with TAK-ML");
        FeatureOfInterest gpsSensorFeatureOfInterest = new FeatureOfInterest();

        gpsSensorPlugin = new GPSSensorPlugin(pluginContext, this.getMapView(), GPS_SENSOR_ID, null, gpsSensorFeatureOfInterest);
        //pressureSensorPlugin.start();
    }

    /**************************** PUBLIC METHODS *****************************/

    public void disposeImpl() {
    }

    /**************************** INHERITED METHODS *****************************/

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(TAG, "showing plugin drop down");
        if (intent.getAction().equals(SHOW_PLUGIN)) {

            showDropDown(templateView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                    HALF_HEIGHT, false);
        }
    }

    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownVisible(boolean v) {
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
    }

    @Override
    public void onDropDownClose() {
    }

    /************************* Helper Methods *************************/
}
