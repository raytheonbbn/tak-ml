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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.atakmap.android.atakutils.MiscUtils;
import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml.mvc.Model;
import com.atakmap.android.takml.plugin.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.atakmap.android.takml.receivers.SettingsDropDownReceiver.SHOW_SETTINGS;

public class SetMXPluginDropDownReceiver extends DropDownReceiver
        implements DropDown.OnStateListener {

    private final int MSG_UPDATE_UI = 0;
    private final int UI_UPDATE_INTERVAL_MS = 100;

    public static final String TAG = SetMXPluginDropDownReceiver.class.getSimpleName();
    public static final String SHOW_SET_MX_PLUGIN =
            "com.atakmap.android.android.takml.image_recognition_demo_app.SHOW_SET_MX_PLUGIN";

    private static final String NONE_STRING = "NONE";

    private final View layoutView;
    private Activity parentActivity;
    private Model model;
    private Handler mHandler;

    ArrayAdapter<String> mxPluginListAdapter;
    Spinner mxPluginSelectionSpinner;
    List<String> mxPluginDisplayStrings;

    Button setButton;

    private int selectedMXPluginIndex = 0;

    public SetMXPluginDropDownReceiver(final MapView mapView, final Context context, Model model) {
        super(mapView);

        this.model = model;

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.layoutView = inflater.inflate(R.layout.mx_plugin_selection_layout, null);
        this.parentActivity = (Activity) mapView.getContext();

        mxPluginSelectionSpinner =
                (Spinner) layoutView.findViewById(R.id.mxPluginSelectionSpinner);

        mxPluginSelectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedMXPluginIndex = i;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        mxPluginDisplayStrings = new ArrayList<String>();
        // add a fake mx plugin display string for demonstration purposes
        mxPluginDisplayStrings.add(NONE_STRING);
        mxPluginListAdapter = new ArrayAdapter<String>(context, R.layout.spinner_item_custom, mxPluginDisplayStrings);
        mxPluginListAdapter.setDropDownViewResource(R.layout.spinner_item_custom);
        mxPluginSelectionSpinner.setAdapter(mxPluginListAdapter);
        mxPluginListAdapter.notifyDataSetChanged();

        setButton = (Button) layoutView.findViewById(R.id.setButton);
        setButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String selectedMxPluginDisplayString = mxPluginDisplayStrings.get(selectedMXPluginIndex);

                if (selectedMxPluginDisplayString.equals(NONE_STRING)) {
                    model.imageRecParams.mxPluginName = null;
                    model.imageRecParams.mxPluginId = null;

                    MiscUtils.toast("Successfully set MX Plugin to nothing!");
                } else {
                    // assumes the mxPluginDisplayString is in the format:
                    // MX Plugin Name (Mx plugin id)
                    String mxPluginName = selectedMxPluginDisplayString.substring(
                            0, selectedMxPluginDisplayString.lastIndexOf("(")
                    );
                    String mxPluginId = selectedMxPluginDisplayString.substring(
                            selectedMxPluginDisplayString.lastIndexOf("(") + 1,
                            selectedMxPluginDisplayString.lastIndexOf(")")
                    );

                    model.imageRecParams.mxPluginName = mxPluginName;
                    model.imageRecParams.mxPluginId = mxPluginId;

                    model.currentPredictionResult = "";

                    MiscUtils.toast("Successfully set MX Plugin to " + mxPluginName + "!");
                }

                updateUI();
            }
        });

        Button done = (Button) layoutView.findViewById(R.id.doneButton);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeDropDown();
                Intent nextIntent = new Intent();
                nextIntent.setAction(SHOW_SETTINGS);
                AtakBroadcast.getInstance().sendBroadcast(nextIntent);
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
        Log.d(TAG, "Received intent: " + intent.getAction());

        if (intent.getAction().equals(SHOW_SET_MX_PLUGIN)) {
            showDropDown(this.layoutView, HALF_WIDTH, FULL_HEIGHT,
                    FULL_WIDTH, HALF_HEIGHT, false);

            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MSG_UPDATE_UI:
                            updateUI();
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

    private void updateMxPlugins(Set<String> mxPlugins) {
        mxPluginDisplayStrings.clear();
        for (String mxPlugin : mxPlugins) {
            mxPluginDisplayStrings.add(mxPlugin);
        }
        mxPluginDisplayStrings.add(NONE_STRING);
        mxPluginListAdapter.notifyDataSetChanged();
    }

    private void updateUI() {
        if (model.knownMxPlugins != null) {
            updateMxPlugins(model.knownMxPlugins);
        }
    }
}

