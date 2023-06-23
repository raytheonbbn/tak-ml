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
import android.widget.Spinner;
import android.widget.TextView;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml.example.R;
import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;

import com.atakmap.coremap.log.Log;
import com.bbn.takml_sdk_android.mx_framework.request.*;
import com.bbn.takml_sdk_android.mx_framework.PredictionExecutor;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * Example machine learning application (MLA) usage of the TAK-ML framework.
 *
 * This application has the following workflow:
 *  - User inputs data
 *  - User selects a plugin and model to use
 *  - User clicks a "Make Prediction" button
 *  - On button click, a plugin is instantiated, a prediction is made,
 *    and the instance is destroyed
 *
 * Applications could have different workflows. For example, an application
 * that does not require user input about the plugin and model to use could
 * have the following workflow:
 *  - Plugin ID and model name are hardcoded or asked for on application startup
 *  - A plugin is instantiated and waiting
 *  - A user inputs data and a prediction is made
 *  - The user repeats the above step as many times as desired
 *  - When the user is finished and the application closes, the plugin
 *    instance is destroyed.
 *
 * Applications could also use multiple plugins, create multiple instances,
 * make multiple simultaneous predictions, etc.
 */
public class ExampleDropDownReceiver extends DropDownReceiver implements OnStateListener {

    public static final String TAG = ExampleDropDownReceiver.class.getSimpleName();

    public static final String SHOW_PLUGIN = "com.atakmap.android.takml.example.SHOW_PLUGIN";

    // ATAK state.
    private final View templateView;
    private final Context pluginContext;
    private final MapView mapView;
    private Activity parentActivity;

    // User input from application. Represents two input text boxes and
    // one input checkbox that is represented as a string.
    String input1, input2, checked;

    // Drop down menus for the available TAK-ML plugins and models.
    // These are not strictly necessary for a TAK-ML application.
    // Some applications may choose to hardcode the plugin and/or model,
    // give users a textbox to type a plugin ID, or something else.
    private ArrayAdapter<String> pluginIDAdapter;
    private Spinner pluginIDDropdown;
    private ArrayAdapter<String> modelAdapter;
    private Spinner modelDropdown;

    // Instance and prediction state for running a single
    // instance and prediction at a time.
    private PredictionExecutor takmlExecutor;
    private String pluginID;        // ID of the plugin we're using
    private String mxpInstanceID;   // ID of the instance that we will create
    private String token;           // token to make sure the instantiate reply is valid
    private String executeID;       // ID to make sure the execution reply is valid

    /**
     * Reset the prediction state.
     */
    private void resetState() {
        this.pluginID = null;
        this.token = null;
        this.mxpInstanceID = null;
        this.executeID = null;
    }

    public ExampleDropDownReceiver(final MapView mapView, final Context context) {
        super(mapView);
        this.pluginContext = context;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        templateView = inflater.inflate(R.layout.main_layout, null);
        this.mapView = mapView;
        this.parentActivity = (Activity)mapView.getContext();

        // Read what the checkbox contains when it is checked.
        this.checked = "0";
        CheckBox checkbox = (CheckBox)templateView.findViewById(R.id.checkbox);
        checkbox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean isChecked) {
                if (isChecked) {
                    checked = "1";
                } else {
                    checked = "0";
                }
            }
        });

        this.pluginIDDropdown = (Spinner)templateView.findViewById(R.id.pluginIdValue);
        this.modelDropdown = (Spinner)templateView.findViewById(R.id.modelValue);

        // Create the PredictionExecutor and subscribe to plugin/model updates.
        try {
            Log.e(TAG, "Creating new prediction executor");
            this.takmlExecutor = new PredictionExecutor("example-app");
            this.takmlExecutor.setMxListResourcesCallback(this::listResourcesCB);
            this.takmlExecutor.requestResourcesList();
            this.takmlExecutor.setFrameworkConnectionStatusCallback(this::frameworkConnectionCB);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Not connected to TAK-ML");
        }

        // Describe what to do when the "Make Prediction" button is pushed.
        Button sendButton = templateView.findViewById(R.id.predict);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // If not already connected to TAK-ML, try again now.
                if (takmlExecutor == null) {
                    try {
                        Log.e(TAG, "Creating new prediction executor 2");
                        takmlExecutor = new PredictionExecutor("example-app");
                        takmlExecutor.setMxListResourcesCallback(
                                ExampleDropDownReceiver.this::listResourcesCB);
                        takmlExecutor.requestResourcesList();
                        takmlExecutor.setFrameworkConnectionStatusCallback(
                                ExampleDropDownReceiver.this::frameworkConnectionCB);
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

                // Read what the user input for the two input boxes.
                TextView i1 = (TextView)templateView.findViewById(R.id.text1input);
                input1 = i1.getText().toString();

                TextView i2 = (TextView)templateView.findViewById(R.id.text2input);
                input2 = i2.getText().toString();

                // Read what the user selected for the plugin and model.
                String pluginLabel = pluginIDDropdown.getSelectedItem().toString();
                pluginID = PredictionExecutor.pluginLabelToID(pluginLabel);

                String modelLabel = modelDropdown.getSelectedItem().toString();
                String model = PredictionExecutor.modelLabelToName(modelLabel);

                // Don't use any special parameters for instantiating the plugin.
                HashMap<String, Serializable> params = new HashMap<String, Serializable>();

                // Instantiate the plugin with the given model.
                token = takmlExecutor.instantiatePlugin(
                        pluginID,  model, params,
                        ExampleDropDownReceiver.this::instantiateCB);
                if (token == null) {
                    connectionError("Error: can't instantiate plugin " + pluginID + " with model " + model);
                    return;
                }
            }
        });
    }

    /**
     * Stop the PredictionExecutor when the application closes.
     */
    public void disposeImpl() {
        if (this.takmlExecutor != null)
            this.takmlExecutor.stop();
    }

    /**
     * Display a connection error in place of the prediction result.
     * @param msg the message to print.
     */
    public void connectionError(String msg) {
        TextView prediction = (TextView)templateView.findViewById(R.id.prediction);
        prediction.setText(msg);
    }

    /**
     * Callback for the instantiate request.
     * <p>
     * When receiving a reply, we make sure that the reply's token matches
     * the request token (for validity), and make sure that the instantiation
     * was successful. Only then can we use the instance ID to make a prediction.
     * @param reply the instantiation reply.
     */
    public void instantiateCB(MXInstantiateReply reply) {
        TextView result = (TextView)templateView.findViewById(R.id.predictionResult);

        if (!reply.getToken().equals(this.token)) {
            result.setText("Error: Token returned by TAK-ML for instantiating plugin does not match expected value");
            return;
        }

        if (reply.isSuccess()) {
            // Create some sample input.
            String request = this.input1 + "," + this.input2 + "," + this.checked;
            this.mxpInstanceID = reply.getMxpInstanceID();
            this.executeID = takmlExecutor.executePrediction(this.mxpInstanceID,
                    request.getBytes(), ExampleDropDownReceiver.this::executeResponse);
            if (this.executeID == null) {
                connectionError("Error: can't execute plugin " + reply.getPluginID() + " instance " + this.mxpInstanceID);
                destroyMxpInstance(this.mxpInstanceID);
                resetState();
            }
        } else {
            result.setText("Error: " + reply.getMsg());
            resetState();
        }
    }

    /**
     * Callback for the prediction execution request.
     * <p>
     * When receiving a reply, we make sure that the reply's execute ID matches
     * the request's execute ID (for validity), and make sure that the prediction
     * was successful. Only then can we report the prediction result.
     */
    public void executeResponse(MXExecuteReply reply) {
        TextView result = (TextView)templateView.findViewById(R.id.predictionResult);

        if (!reply.getExecuteID().equals(this.executeID)) {
            result.setText("Error: execute ID does not match expected value");
            return;
        }

        if (reply.isSuccess()) {
            String resultStr = new String(reply.getBytes());
            result.setText(resultStr);
        } else {
            result.setText("Error: " + reply.getMsg());
        }

        destroyMxpInstance(this.mxpInstanceID);
        resetState();
    }

    /**
     * Keep the application interface updated about the available plugins and models.
     *
     * @param reply the update from the MX framework.
     */
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

    /**
     * Callback for connection status changes to the TAK-ML framework.
     * @param connectedToFramework whether the application is connected to the framework.
     */
    public void frameworkConnectionCB(boolean connectedToFramework) {
        if (connectedToFramework) {
            // On reconnection, re-subscribe to MX framework resources.
            this.takmlExecutor.requestResourcesList();
        } else {
            Log.e(TAG, "Disconnected from TAK-ML framework");
        }
    }

    /**
     * Inform the MX framework that we want to destroy the instance.
     * @param mxpInstanceID the instance to destroy.
     */
    private void destroyMxpInstance(String mxpInstanceID) {
        if (!this.takmlExecutor.destroyPlugin(mxpInstanceID)) {
            Log.e(TAG, "Could not destroy instance " + mxpInstanceID);
        }
    }

    /**
     * Actions to take when the application is opened.
     */
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
