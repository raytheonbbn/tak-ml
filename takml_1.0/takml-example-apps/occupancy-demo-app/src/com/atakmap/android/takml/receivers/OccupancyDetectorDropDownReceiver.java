package com.atakmap.android.takml.receivers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml.occupancy.R;
import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;

import com.atakmap.coremap.log.Log;
import com.bbn.takml_sdk_android.mx_framework.MXListResourcesCallback;
import com.bbn.takml_sdk_android.mx_framework.request.*;
import com.bbn.takml_sdk_android.mx_framework.PredictionExecutor;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.*;

public class OccupancyDetectorDropDownReceiver extends DropDownReceiver implements OnStateListener {

    public static final String TAG = OccupancyDetectorDropDownReceiver.class.getSimpleName();

    public static final String SHOW_PLUGIN = "com.atakmap.android.takml.occupancy.SHOW_PLUGIN";

    private final View templateView;
    private final Context pluginContext;
    private final MapView mapView;
    private Activity parentActivity;

    private ArrayAdapter<String> pluginIDAdapter;
    private Spinner pluginIDDropdown;
    private ArrayAdapter<String> modelAdapter;
    private Spinner modelDropdown;

    String co2Predict, humidityPredict, lightsOn;

    private PredictionExecutor takmlExecutor;
    private String pluginID;
    private String token;
    private String mxpInstanceID;
    private String executeID;

    private void resetState() {
        this.pluginID = null;
        this.token = null;
        this.mxpInstanceID = null;
        this.executeID = null;
    }

    public OccupancyDetectorDropDownReceiver(final MapView mapView, final Context context) {
        super(mapView);
        this.pluginContext = context;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        templateView = inflater.inflate(R.layout.main_layout, null);
        this.mapView = mapView;
        this.parentActivity = (Activity)mapView.getContext();

        Button processButton = templateView.findViewById(R.id.confirmButton);
        processButton.setText("Pull Sensor Data");
        processButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView co2 = (TextView)templateView.findViewById(R.id.co2Result);
                co2.setText("" + new Random().nextDouble());
                co2Predict = co2.getText().toString();

                TextView humidity = (TextView)templateView.findViewById(R.id.humidityResult);
                humidity.setText("" + new Random().nextDouble());
                humidityPredict = humidity.getText().toString();
            }
        });

        CheckBox checkbox = (CheckBox)templateView.findViewById(R.id.lightOnCheck);
        checkbox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean isChecked) {
                if (isChecked) {
                    lightsOn = "1";
                } else {
                    lightsOn = "0";
                }
            }
        });

        this.pluginIDDropdown = (Spinner)templateView.findViewById(R.id.pluginIdValue);
        this.modelDropdown = (Spinner)templateView.findViewById(R.id.modelValue);

        try {
            Log.e(TAG, "Creating new prediction executor");
            this.takmlExecutor = new PredictionExecutor("occupancy-demo-app");
            this.takmlExecutor.setMxListResourcesCallback(this::listResourcesCB);
            takmlExecutor.requestResourcesList();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Not connected to TAK-ML");
        }

        Button sendButton = templateView.findViewById(R.id.predict);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (takmlExecutor == null) {
                    try {
                        Log.e(TAG, "Creating new prediction executor 2");
                        takmlExecutor = new PredictionExecutor("occupancy-demo-app");
                        takmlExecutor.setMxListResourcesCallback(
                                OccupancyDetectorDropDownReceiver.this::listResourcesCB);
                        takmlExecutor.requestResourcesList();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Cannot connect to TAK-ML");
                        connectionError("Error: cannot connect to TAK-ML");
                        return;
                    }
                }

                if (!takmlExecutor.isConnected()) {
                    connectionError("Error: cannot connect to TAK-ML");
                    return;
                }

                String pluginLabel = pluginIDDropdown.getSelectedItem().toString();
                pluginID = PredictionExecutor.pluginLabelToID(pluginLabel);

                String modelLabel = modelDropdown.getSelectedItem().toString();
                String model = PredictionExecutor.modelLabelToName(modelLabel);

                HashMap<String, Serializable> params = new HashMap<String, Serializable>();

                token = takmlExecutor.instantiatePlugin(
                        pluginID,  model, params,
                        OccupancyDetectorDropDownReceiver.this::instantiateCB);
                if (token == null) {
                    connectionError("Error: can't instantiate plugin " + pluginID + " with model " + model);
                    return;
                }
            }
        });
    }

    public void disposeImpl() {
        if (this.takmlExecutor != null)
            this.takmlExecutor.stop();
    }

    public void connectionError(String msg) {
        TextView solarFlare = (TextView)templateView.findViewById(R.id.occupancyPrediction);
        solarFlare.setText(msg);
    }

    private void destroyMxpInstance(String mxpInstanceID) {
        if (!this.takmlExecutor.destroyPlugin(mxpInstanceID)) {
            Log.e(TAG, "Could not destroy instance " + mxpInstanceID);
        }
    }

    public void instantiateCB(MXInstantiateReply reply) {
        TextView occupancy = (TextView)templateView.findViewById(R.id.occupancyPredictionResult);

        if (!reply.getToken().equals(this.token)) {
            occupancy.setText("Error: Token returned by TAK-ML for instantiating plugin does not match expected value");
            return;
        }

        if (reply.isSuccess()) {
            String request = this.co2Predict + "," + this.humidityPredict + "," + lightsOn;
            this.mxpInstanceID = reply.getMxpInstanceID();
            this.executeID = takmlExecutor.executePrediction(this.mxpInstanceID,
                    request.getBytes(), OccupancyDetectorDropDownReceiver.this::executeResponse);
            if (this.executeID == null) {
                connectionError("Error: can't execute plugin " + reply.getPluginID() + " instance " + this.mxpInstanceID);
                destroyMxpInstance(this.mxpInstanceID);
                resetState();
            }
        } else {
            occupancy.setText("Error: " + reply.getMsg());
            resetState();
        }
    }

    public void executeResponse(MXExecuteReply reply) {
        TextView occupancy = (TextView)templateView.findViewById(R.id.occupancyPredictionResult);

        if (!reply.getExecuteID().equals(this.executeID)) {
            occupancy.setText("Error: execute ID does not match expected value");
            return;
        }

        if (reply.isSuccess()) {
            String result = new String(reply.getBytes());
            String[] parts = result.split(",");
            occupancy.setText("Probability of occupancy: " + (Double.valueOf(parts[1]) * 100)  + "%");
        } else {
            occupancy.setText("Error: " + reply.getMsg());
        }

        destroyMxpInstance(this.mxpInstanceID);
        resetState();
    }

    public void listResourcesCB(MXListResourcesReply reply) {
        parentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Set<String> pluginLabels = reply.getPlugins();
                String[] pluginStrings = new String[pluginLabels.size()];
                pluginLabels.toArray(pluginStrings);
                pluginIDAdapter = new ArrayAdapter<String>(pluginContext,
                        android.R.layout.simple_spinner_item, pluginStrings);
                pluginIDAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                pluginIDDropdown.setAdapter(pluginIDAdapter);
                pluginIDAdapter.notifyDataSetChanged();

                Set<String> modelLabels = reply.getModels();
                String[] modelStrings = new String[modelLabels.size()];
                modelLabels.toArray(modelStrings);
                modelAdapter = new ArrayAdapter<String>(pluginContext,
                        android.R.layout.simple_spinner_item, modelStrings);
                modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                modelDropdown.setAdapter(modelAdapter);
                modelAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals(SHOW_PLUGIN)) {
            return;
        }

        showDropDown(templateView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                HALF_HEIGHT, false);
    }

    @Override
    public void onDropDownSelectionRemoved() {
        return;
    }

    @Override
    public void onDropDownVisible(boolean v) {
        return;
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
        return;
    }

    @Override
    public void onDropDownClose() {
        return;
    }
}
