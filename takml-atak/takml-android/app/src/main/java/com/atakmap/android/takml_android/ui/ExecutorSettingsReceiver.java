package com.atakmap.android.takml_android.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.gui.PluginSpinner;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml_android.R;
import com.atakmap.android.takml_android.Takml;
import com.atakmap.android.takml_android.TakmlExecutor;
import com.atakmap.android.takml_android.TakmlModel;
import com.atakmap.coremap.log.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExecutorSettingsReceiver extends DropDownReceiver implements DropDown.OnStateListener {
    public static final String TAG = ExecutorSettingsReceiver.class.getName();
    public static final String SHOW_PLUGIN = ExecutorSettingsReceiver.class.getName() + "_SHOW_PLUGIN";
    private final View takmlView;
    private Intent callbackIntent;
    private final Takml TAKML;
    private final TakmlExecutor takmlExecutor;
    private PluginSpinner mxPluginSpinner;

    public ExecutorSettingsReceiver(MapView mapView, Context pluginContext, Takml takml,
                                    TakmlExecutor takmlExecutor) {
        super(mapView);
        this.TAKML = takml;
        this.takmlExecutor = takmlExecutor;
        takmlView = PluginLayoutInflater.inflate(pluginContext,
                R.layout.executor_layout, null);
        takmlView.findViewById(R.id.backButton).setOnClickListener(view -> onBackButtonPressed());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action == null)
            return;

        if (action.equals(SHOW_PLUGIN + TAKML.getUuid())) {
            Log.d(TAG, "showing plugin drop down");
            showDropDown(takmlView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                    HALF_HEIGHT, false, this);

            // TAK ML Models
            PluginSpinner modelSpinner = takmlView.findViewById(R.id.model_spinner);
            List<String> modelNames = new ArrayList<>();
            for(TakmlModel model : TAKML.getModels()){
                modelNames.add(model.getName());
            }
            ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(
                    context, android.R.layout.simple_spinner_item, modelNames);
            modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            modelSpinner.setAdapter(modelAdapter);
            modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                    if (view instanceof TextView) {
                        ((TextView) view).setTextColor(Color.WHITE);
                    }
                    takmlExecutor.selectModel(TAKML.getModels().get(position));
                    mxPluginSpinner.setSelection(TAKML.getModelExecutionPlugins().indexOf(
                            takmlExecutor.getSelectedModelExecutionPlugin().getClass().getName()));
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });

            // Mx Plugins
            mxPluginSpinner = takmlView.findViewById(R.id.mx_plugin_spiner);
            List<String> mxPluginNames = new ArrayList<>();
            Map<String, String> simpleNameToClassName = new HashMap<>();
            for(String mxPluginName : TAKML.getModelExecutionPlugins()){
                String result = mxPluginName.substring(mxPluginName.lastIndexOf('.') + 1).trim();
                mxPluginNames.add(result);
                simpleNameToClassName.put(result, mxPluginName);
            }
            ArrayAdapter<String> mxPluginsAdapter = new ArrayAdapter<>(
                    context, android.R.layout.simple_spinner_item, mxPluginNames);
            mxPluginsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mxPluginSpinner.setAdapter(mxPluginsAdapter);
            mxPluginSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                    if (view instanceof TextView) {
                        ((TextView) view).setTextColor(Color.WHITE);
                    }
                    String mxPluginSelected = simpleNameToClassName.get(mxPluginNames.get(position));
                    android.util.Log.d(TAG, "selected mx plugin: " + mxPluginSelected);
                    takmlExecutor.selectModelExecutionPlugin(mxPluginSelected);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });
            mxPluginSpinner.setSelection(TAKML.getModelExecutionPlugins().indexOf(
                    takmlExecutor.getSelectedModelExecutionPlugin().getClass().getName()));

            takmlView.findViewById(R.id.takmlSettingsBtn).setOnClickListener(view -> {
                Intent callbackIntent = new Intent();
                callbackIntent.setAction(SHOW_PLUGIN + TAKML.getUuid());
                TAKML.showConfigUI(callbackIntent);
            });

            modelSpinner.setSelection(TAKML.getModels().indexOf(takmlExecutor.getSelectedModel()));
        }
    }

    @Override
    public void disposeImpl() {

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
    protected boolean onBackButtonPressed() {
        closeDropDown();
        if(callbackIntent != null){
            AtakBroadcast.getInstance().sendBroadcast(callbackIntent);
        }
        return false;
    }

    public void setCallbackIntent(Intent callbackIntent) {
        this.callbackIntent = callbackIntent;
    }
}
