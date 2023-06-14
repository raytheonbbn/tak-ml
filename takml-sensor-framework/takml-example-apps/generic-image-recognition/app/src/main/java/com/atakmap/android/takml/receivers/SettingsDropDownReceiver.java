package com.atakmap.android.takml.receivers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml.mvc.Model;
import com.atakmap.android.takml.plugin.R;

import static com.atakmap.android.takml.receivers.GenericImageRecognitionDropDownReceiver.SHOW_PLUGIN;
import static com.atakmap.android.takml.receivers.SetConfidenceThresholdDropDownReceiver.SHOW_SET_CONFIDENCE_THRESHOLD;
import static com.atakmap.android.takml.receivers.SetMXPluginDropDownReceiver.SHOW_SET_MX_PLUGIN;
import static com.atakmap.android.takml.receivers.SetMxPluginParametersDropDownReceiver.SHOW_SET_MX_PLUGIN_PARAMETERS;

public class SettingsDropDownReceiver extends DropDownReceiver {

    private final int MSG_UPDATE_UI = 0;
    private final int UI_UPDATE_INTERVAL_MS = 100;

    public static final String TAG = SettingsDropDownReceiver.class.getSimpleName();
    public static final String SHOW_SETTINGS =
            "com.atakmap.android.android.takml.image_recognition_demo_app.SHOW_SETTINGS";

    private final View settingsView;
    private Activity parentActivity;
    private Model model;
    private Handler mHandler;

    Button setMxPluginButton;
    Button setConfidenceThresholdButton;
    Button setMxPluginParametersButton;

    TextView currentParametersDisplay;
    TextView takmlFrameworkAppDataDirectoryPathDisplay;

    public SettingsDropDownReceiver(final MapView mapView, final Context context, Model model) {
        super(mapView);

        this.model = model;

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.settingsView = inflater.inflate(R.layout.settings_layout, null);
        this.parentActivity = (Activity) mapView.getContext();

        setMxPluginButton = (Button) settingsView.findViewById(R.id.setMxPluginButton);
        setMxPluginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                model.takmlExecutor.requestResourcesList();
                model.takmlExecutor.requestTAKMLFrameworkAppDataDirectoryResourcesList();
                SettingsDropDownReceiver.this.closeDropDown();
                Intent nextIntent = new Intent();
                nextIntent.setAction(SHOW_SET_MX_PLUGIN);
                AtakBroadcast.getInstance().sendBroadcast(nextIntent);
            }
        });

        setConfidenceThresholdButton = (Button) settingsView.findViewById(R.id.setConfidenceThresholdButton);
        setConfidenceThresholdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                model.takmlExecutor.requestResourcesList();
                model.takmlExecutor.requestTAKMLFrameworkAppDataDirectoryResourcesList();
                closeDropDown();
                Intent nextIntent = new Intent();
                nextIntent.setAction(SHOW_SET_CONFIDENCE_THRESHOLD);
                AtakBroadcast.getInstance().sendBroadcast(nextIntent);
            }
        });

        setMxPluginParametersButton = (Button) settingsView.findViewById(R.id.setParametersButton);
        setMxPluginParametersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                model.takmlExecutor.requestResourcesList();
                model.takmlExecutor.requestTAKMLFrameworkAppDataDirectoryResourcesList();
                closeDropDown();
                Intent nextIntent = new Intent();
                nextIntent.setAction(SHOW_SET_MX_PLUGIN_PARAMETERS);
                AtakBroadcast.getInstance().sendBroadcast(nextIntent);
            }
        });

        Button done = (Button) settingsView.findViewById(R.id.doneButton);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                model.takmlExecutor.requestResourcesList();
                model.takmlExecutor.requestTAKMLFrameworkAppDataDirectoryResourcesList();
                closeDropDown();
                Intent nextIntent = new Intent();
                nextIntent.setAction(SHOW_PLUGIN);
                AtakBroadcast.getInstance().sendBroadcast(nextIntent);
            }
        });

        currentParametersDisplay = (TextView) settingsView.findViewById(R.id.currentParametersDisplay);
        takmlFrameworkAppDataDirectoryPathDisplay = (TextView) settingsView.findViewById(R.id.takmlAppDataDirectoryPathDisplay);
    }

    @Override
    protected void disposeImpl() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received intent: " + intent.getAction());

        if (intent.getAction().equals(SHOW_SETTINGS)) {
            showDropDown(this.settingsView, HALF_WIDTH, FULL_HEIGHT,
                    FULL_WIDTH, HALF_HEIGHT, false);

            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MSG_UPDATE_UI:
                            updateUI();
                            mHandler.removeMessages(MSG_UPDATE_UI);
                            mHandler.sendEmptyMessageAtTime(MSG_UPDATE_UI,
                                    SystemClock.uptimeMillis() + UI_UPDATE_INTERVAL_MS);
                            break;
                    }
                }
            };
            mHandler.sendEmptyMessageAtTime(MSG_UPDATE_UI,
                    SystemClock.uptimeMillis() + UI_UPDATE_INTERVAL_MS);
        }

    }

    private void updateUI() {
        updateCurrentParametersDisplay();
        takmlFrameworkAppDataDirectoryPathDisplay.setText(
                model.takmlAppDataDirectory.substring(0,
                        model.takmlAppDataDirectory.lastIndexOf("/app_data"))
        );
    }

    private void updateCurrentParametersDisplay() {
        String otherParamsString = "";
        for (String paramName : model.imageRecParams.mxPluginParams.keySet()) {
            otherParamsString += paramName + ": " + "\n" +
                    model.imageRecParams.mxPluginParams.get(paramName) + "\n" + "-" + "\n";
        }
        currentParametersDisplay.setText(
                "MX Plugin Name: " + "\n" +
                        model.imageRecParams.mxPluginName + "\n" + "---" + "\n" +
                        "MX Plugin ID: " + "\n" +
                        model.imageRecParams.mxPluginId + "\n" + "---" + "\n" +
                        "Model file: " + "\n" +
                        model.imageRecParams.modelName + "\n" + "---" + "\n" +
                        "Confidence threshold: " + "\n" +
                        model.imageRecParams.minimumConfidence + "\n" + "---" + "\n" +
                        "Metadata lookup file: " + "\n" +
                        model.imageRecParams.metadataLookupFileName + "\n" + "---" + "\n" +
                        "Other parameters: " + "\n" +
                        otherParamsString

        );
    }
}

