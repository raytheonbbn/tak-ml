package com.atakmap.android.takml_framework.receivers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ListView;

import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml_framework.TakMlFrameworkSensorsExpandable;
import com.atakmap.android.takml_framework.plugin.R;
import com.atakmap.coremap.log.Log;
import com.bbn.tak.ml.sensor.SensorDataStream;
import com.bbn.takml.framework.ChangeType;
import com.bbn.takml.framework.TakMlFramework;
import com.bbn.takml.sensor_framework.SensorTagUpdateListener;

public class SensorsDropDownReceiver extends DropDownReceiver implements OnStateListener, SensorTagUpdateListener {

    public static final String TAG = SensorsDropDownReceiver.class.getSimpleName();
    public static final String SHOW_SENSORS = "com.atakmap.android.takml_framework.SHOW_SENSORS";

    private final View sensorsView;
    private TakMlFrameworkSensorsExpandable sensorsAdapter;
    private Activity thisActivity;
    private ListView sensorsListView;
    private TakMlFramework framework;

    public SensorsDropDownReceiver(final MapView mapView, final Context context) {
        super(mapView);
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.sensorsView = inflater.inflate(R.layout.sensors_layout, null);
        thisActivity = (Activity)mapView.getContext();

        ExpandableListView expListView = (ExpandableListView)sensorsView.findViewById(R.id.sensorsListView);
        this.sensorsAdapter = new TakMlFrameworkSensorsExpandable(context, this);
        expListView.setAdapter(this.sensorsAdapter);


        Button back = (Button)sensorsView.findViewById(R.id.backButton);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeDropDown();
                Intent nextIntent = new Intent();
                nextIntent.setAction(TakMlFrameworkDropDownReceiver.SHOW_FRAMEWORK_STANDUP);
                AtakBroadcast.getInstance().sendBroadcast(nextIntent);
            }
        });

        ImageButton refreshSensorsBtn = (ImageButton)sensorsView.findViewById(R.id.refreshSensorsBtn);
        refreshSensorsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Refreshing sensors");
            }
        });
    }

    public void updateSensorList(final SensorDataStream sensorDataStream, ChangeType changeType) {
        Log.d(TAG, "Sensor change occurred: " + sensorDataStream.getSensor().toString() + " : " + changeType);
        thisActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (changeType) {
                    case ADDED:
                    case CHANGED:
                        SensorsDropDownReceiver.this.sensorsAdapter.add(sensorDataStream);
                        break;
                    case REMOVED:
                        SensorsDropDownReceiver.this.sensorsAdapter.remove(sensorDataStream);
                        break;
                }
            }
        });
    }

    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownClose() {
    }

    @Override
    public void onDropDownSizeChanged(double v, double v1) {
    }

    @Override
    public void onDropDownVisible(boolean b) {
    }

    @Override
    protected void disposeImpl() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(SHOW_SENSORS)) {
            showDropDown(this.sensorsView, HALF_WIDTH, FULL_HEIGHT,
                    FULL_WIDTH, HALF_HEIGHT, false);
        }
    }

    @Override
    public void sensorTagUpdated(String sensorID, String tag) {
        Log.d(TAG, "Sensor tag updated for " + sensorID + " to " + tag);
        framework.getSensorFramework().sensorTagUpdated(sensorID, tag);
    }

    public void setFramework(TakMlFramework framework) {
        this.framework = framework;
    }
}
