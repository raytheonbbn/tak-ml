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
import android.widget.Toast;

import com.atakmap.android.atakutils.MiscUtils;
import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml.mvc.ImageRecognitionParams;
import com.atakmap.android.takml.mvc.Model;
import com.atakmap.android.takml.plugin.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.atakmap.android.takml.receivers.SettingsDropDownReceiver.SHOW_SETTINGS;
import static com.bbn.tak.ml.mx_framework.MXFrameworkConstants.MX_PLUGIN_PARAM_INPUT_PROCESSING_CONFIG;
import static com.bbn.tak.ml.mx_framework.MXFrameworkConstants.MX_PLUGIN_PARAM_LABELS;

public class SetMxPluginParametersDropDownReceiver extends DropDownReceiver
        implements DropDown.OnStateListener {

    private static final int MSG_UPDATE_UI = 0;

    private static final int UI_UPDATE_INTERVAL_MS = 100;

    public static final String TAG = SetMxPluginParametersDropDownReceiver.class.getSimpleName();
    public static final String SHOW_SET_MX_PLUGIN_PARAMETERS =
            "com.atakmap.android.android.takml.image_recognition_demo_app.SHOW_SET_MX_PLUGIN_PARAMETERS";

    public static final String PARAM_MODEL_FILE = "PARAM_MODEL_FILE";
    public static final String PARAM_METADATA_LOOKUP = "PARAM_METADATA_LOOKUP";

    private static final String NONE_STRING = "NONE";

    private final View layoutView;
    private Activity parentActivity;
    private Model model;
    private Handler mHandler;

    ArrayAdapter<String> paramsListAdapter;
    Spinner paramSelectionSpinner;
    List<String> paramStrings;

    ArrayAdapter<String> paramFilesAdapter;
    Spinner paramFileSelectionSpinner;
    List<String> paramFileNames;

    Button setParameterButton;
    Button clearParametersButton;

    int selectedParamIndex;
    int selectedParamFileIndex;

    public SetMxPluginParametersDropDownReceiver(final MapView mapView, final Context context, Model model) {
        super(mapView);

        this.model = model;

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.layoutView = inflater.inflate(R.layout.mx_plugin_parameter_setting_layout, null);
        this.parentActivity = (Activity) mapView.getContext();

        paramSelectionSpinner =
                (Spinner) layoutView.findViewById(R.id.paramSelectionSpinner);
        paramFileSelectionSpinner =
                (Spinner) layoutView.findViewById(R.id.paramFileSelectionSpinner);

        paramSelectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedParamIndex = i;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        paramFileSelectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedParamFileIndex = i;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        paramStrings = new ArrayList<>();
        paramStrings.add(MX_PLUGIN_PARAM_LABELS);
        paramStrings.add(PARAM_MODEL_FILE);
        paramStrings.add(PARAM_METADATA_LOOKUP);
        paramStrings.add(MX_PLUGIN_PARAM_INPUT_PROCESSING_CONFIG);
        paramsListAdapter = new ArrayAdapter<String>(context, R.layout.spinner_item_custom, paramStrings);
        paramsListAdapter.setDropDownViewResource(R.layout.spinner_item_custom);
        paramSelectionSpinner.setAdapter(paramsListAdapter);
        paramsListAdapter.notifyDataSetChanged();

        paramFileNames = new ArrayList<>();
        paramFileNames.add(NONE_STRING);
        paramFilesAdapter = new ArrayAdapter<String>(context, R.layout.spinner_item_custom, paramFileNames);
        paramFilesAdapter.setDropDownViewResource(R.layout.spinner_item_custom);
        paramFileSelectionSpinner.setAdapter(paramFilesAdapter);
        paramFilesAdapter.notifyDataSetChanged();

        setParameterButton = (Button) layoutView.findViewById(R.id.setParametersButton);
        setParameterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String selectedParamString = paramStrings.get(selectedParamIndex);
                String selectedParamFileDisplayString = paramFileNames.get(selectedParamFileIndex);

                if (selectedParamFileDisplayString.equals(NONE_STRING)) {
                    if (selectedParamString.equals(PARAM_MODEL_FILE)) {
                        model.imageRecParams.modelName = null;
                    } else if (selectedParamString.equals(PARAM_METADATA_LOOKUP)) {
                        model.imageRecParams.metadataLookupFileName = null;
                        model.imageRecParams.metadataLookup = null;
                    } else if (selectedParamString.equals(MX_PLUGIN_PARAM_LABELS)) {
                        model.imageRecParams.mxPluginParams.remove(MX_PLUGIN_PARAM_LABELS);
                    } else if (selectedParamString.equals(MX_PLUGIN_PARAM_INPUT_PROCESSING_CONFIG)) {
                        model.imageRecParams.mxPluginParams.remove(MX_PLUGIN_PARAM_INPUT_PROCESSING_CONFIG);
                    } else {
                        MiscUtils.toast("Failed to set parameter: unrecognized parameter string " + selectedParamString);
                        return;
                    }
                } else {
                    // if the selected parameter file string ends with a location (i.e. "(Client)"),
                    // then remove it - otherwise, just take the string as is
                    String selectedParamFile = selectedParamFileDisplayString.substring(
                            0,
                            selectedParamFileDisplayString.contains("(") ?
                                    selectedParamFileDisplayString.lastIndexOf(" (") :
                                    selectedParamFileDisplayString.length()
                    );

                    if (selectedParamString.equals(PARAM_MODEL_FILE)) {
                        model.imageRecParams.modelName = selectedParamFile;
                    } else if (selectedParamString.equals(PARAM_METADATA_LOOKUP)) {
                        model.imageRecParams.metadataLookupFileName = selectedParamFile;
                        model.imageRecParams.metadataLookup =
                                ImageRecognitionParams.loadModelMetaData(
                                        model.takmlAppDataDirectory,
                                        selectedParamFile);
                    } else if (selectedParamString.equals(MX_PLUGIN_PARAM_LABELS)) {
                        model.imageRecParams.mxPluginParams.put(MX_PLUGIN_PARAM_LABELS, selectedParamFile);
                    } else if (selectedParamString.equals(MX_PLUGIN_PARAM_INPUT_PROCESSING_CONFIG)) {
                        model.imageRecParams.mxPluginParams.put(MX_PLUGIN_PARAM_INPUT_PROCESSING_CONFIG, selectedParamFile);
                    } else {
                        Toast.makeText(context, "Failed to set parameter: unrecognized parameter string " + selectedParamString,
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                updateUI();

                MiscUtils.toast("Successfully set parameter!");

            }
        });

        clearParametersButton = (Button) layoutView.findViewById(R.id.clearParametersButton);
        clearParametersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                model.imageRecParams.mxPluginParams.clear();

                MiscUtils.toast("Successfully cleared parameters!");
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

        if (intent.getAction().equals(SHOW_SET_MX_PLUGIN_PARAMETERS)) {
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

    private void updateParamFiles(Set<String> newParamFileNames) {
        paramFileNames.clear();
        for (String paramFileName : newParamFileNames) {
            paramFileNames.add(paramFileName);
        }
        paramFileNames.add(NONE_STRING);
        paramFilesAdapter.notifyDataSetChanged();
    }

    private void updateUI() {
        Set<String> allKnownFileNames = new HashSet<>();
        if (model.knownModelFiles != null) {
            for (String fileName : model.knownModelFiles) {
                allKnownFileNames.add(fileName);
            }
        }
        if (model.knownAppDataFiles != null) {
            for (String fileName : model.knownAppDataFiles) {
                allKnownFileNames.add(fileName);
            }
        }
        updateParamFiles(allKnownFileNames);
    }
}

