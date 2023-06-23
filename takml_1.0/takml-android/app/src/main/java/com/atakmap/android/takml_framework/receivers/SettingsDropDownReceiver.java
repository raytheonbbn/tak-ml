package com.atakmap.android.takml_framework.receivers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml_framework.plugin.R;
import com.atakmap.coremap.log.Log;
import com.bbn.takml.framework.TakMlFramework;
import com.bbn.takml.sensor_framework.SensorFramework;


public class SettingsDropDownReceiver extends DropDownReceiver implements OnStateListener {

    public static final String TAG = SettingsDropDownReceiver.class.getSimpleName();
    public static final String SHOW_SETTINGS = "com.atakmap.android.takml_framework.SHOW_SETTINGS";
    public static final String TAK_ML_PREFS = "TAK_ML_PREFS";
    public static final String ML_PORT = "ML_PORT";
    public static final String SENSOR_PORT = "SENSOR_PORT";
    public static final Integer DEFAULT_ML_PORT_VALUE = 9020;
    public static final Integer DEFAULT_SENSOR_PORT_VALUE = 9021;
    public static final Integer DEFAULT_STREAM_BATCH_SIZE = 300;
    public static final String HOST_ADDR = "HOST_ADDR";
    public static final String USE_TLS = "USE_TLS";
    public static final String STREAM_BATCH_SIZE = "STREAM_BATCH_SIZE";
    public static final boolean DEFAULT_USE_TLS_VALUE = false;

    private final View settingsView;
    private TakMlFramework framework;
    private Activity parentActivity;

    private EditText serverIPText;
    private EditText serverPortText;
    private EditText sensorServerPortText;
    private EditText streamBatchSizeText;

    public SettingsDropDownReceiver(final MapView mapView, final Context context) {
        super(mapView);
        LayoutInflater inflater = (LayoutInflater)context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.settingsView = inflater.inflate(R.layout.settings_layout, null);
        this.parentActivity = (Activity)mapView.getContext();

        SharedPreferences takmlPrefs = mapView.getContext().getSharedPreferences(TAK_ML_PREFS, Context.MODE_PRIVATE);
        Log.d(TAG, "Loading prefs: " + takmlPrefs.getAll());
        Integer mlPort = takmlPrefs.getInt(ML_PORT, DEFAULT_ML_PORT_VALUE);
        Integer sensorPort = takmlPrefs.getInt(SENSOR_PORT, DEFAULT_SENSOR_PORT_VALUE);
        String hostAddr = takmlPrefs.getString(HOST_ADDR, "");
        Boolean useTLS = takmlPrefs.getBoolean(USE_TLS, DEFAULT_USE_TLS_VALUE);
        Integer streamBatchSize = takmlPrefs.getInt(STREAM_BATCH_SIZE, DEFAULT_STREAM_BATCH_SIZE);

        this.serverIPText = (EditText)settingsView.findViewById(R.id.serverIPValue);
        serverIPText.setText(hostAddr);
        this.serverPortText = (EditText)settingsView.findViewById(R.id.serverPortValue);
        serverPortText.setText(mlPort.toString());
        this.sensorServerPortText = (EditText)settingsView.findViewById(R.id.sensorServerPortValue);
        sensorServerPortText.setText(sensorPort.toString());
        this.streamBatchSizeText = (EditText)settingsView.findViewById(R.id.streamBatchSizeEditText);
        streamBatchSizeText.setText(streamBatchSize.toString());

        Switch useTLSSwitch = settingsView.findViewById(R.id.tlsSwitch);

        useTLSSwitch.setChecked(useTLS);

        Button reconnect = (Button)settingsView.findViewById(R.id.reconnect);
        reconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String remoteServIP = serverIPText.getText().toString();
                int remoteServPort = Integer.valueOf(serverPortText.getText().toString());
                if (framework != null) {
                    framework.reconnectRemoteServer(remoteServIP, remoteServPort);
                }
            }
        });

        Button saveSettings = (Button)settingsView.findViewById(R.id.saveSettings);
        saveSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = takmlPrefs.edit();
                editor.putString(HOST_ADDR, serverIPText.getText().toString()).commit();
                editor.putInt(ML_PORT, Integer.valueOf(serverPortText.getText().toString())).commit();
                editor.putInt(SENSOR_PORT, Integer.valueOf(sensorServerPortText.getText().toString())).commit();
                editor.putBoolean(USE_TLS, useTLSSwitch.isChecked()).commit();
                editor.putInt(STREAM_BATCH_SIZE, Integer.valueOf(streamBatchSizeText.getText().toString())).commit();
                SensorFramework.invalidateSettings();
                Toast.makeText(mapView.getContext(), "Settings saved", Toast.LENGTH_LONG).show();
            }
        });

        Button back = (Button)settingsView.findViewById(R.id.backButton);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeDropDown();
                Intent nextIntent = new Intent();
                nextIntent.setAction(TakMlFrameworkDropDownReceiver.SHOW_FRAMEWORK_STANDUP);
                AtakBroadcast.getInstance().sendBroadcast(nextIntent);
            }
        });
    }

    public void setFramework(TakMlFramework framework) {
        this.framework = framework;
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
        if (intent.getAction().equals(SHOW_SETTINGS)) {
            showDropDown(this.settingsView, HALF_WIDTH, FULL_HEIGHT,
                    FULL_WIDTH, HALF_HEIGHT, false);
        }

        if (this.framework != null) {
            this.serverIPText.setText(this.framework.getRemoteServIP());
            this.serverPortText.setText("" + this.framework.getRemoteServPort());
        }
    }

}
