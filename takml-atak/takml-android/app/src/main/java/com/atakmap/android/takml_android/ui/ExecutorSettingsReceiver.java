package com.atakmap.android.takml_android.ui;

import static com.atakmap.android.takml_android.MetadataConstants.KSERVE_API;
import static com.atakmap.android.takml_android.MetadataConstants.KSERVE_URL;
import static com.atakmap.android.takml_android.MetadataConstants.MODEL_LABELS_META;
import static com.atakmap.android.takml_android.MetadataConstants.MODEL_TYPE_META;
import static com.atakmap.android.takml_android.MetadataConstants.PROCESSING_PARAMS_META;
import static com.atakmap.android.takml_android.MetadataConstants.RUN_ON_SERVER;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.gui.PluginSpinner;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml_android.ModelTypeConstants;
import com.atakmap.android.takml_android.ProcessingParams;
import com.atakmap.android.takml_android.R;
import com.atakmap.android.takml_android.Takml;
import com.atakmap.android.takml_android.TakmlExecutor;
import com.atakmap.android.takml_android.TakmlModel;
import com.atakmap.android.takml_android.net.SelectedTAKServer;
import com.atakmap.android.takml_android.net.TakFsManager;
import com.atakmap.android.takml_android.tensor_processor.ImageRecognitionTensorProcessor;
import com.atakmap.android.takml_android.tensor_processor.TensorProcessor;
import com.atakmap.android.takml_android.util.TakServerInfo;
import com.atakmap.android.takml_android.util.TakServerUtils;
import com.atakmap.coremap.log.Log;
import com.bbn.tak_sync_file_manager.model.IndexRow;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ExecutorSettingsReceiver extends DropDownReceiver implements DropDown.OnStateListener {
    public static final String TAG = ExecutorSettingsReceiver.class.getName();
    public static final String SHOW_PLUGIN = ExecutorSettingsReceiver.class.getName() + "_SHOW_PLUGIN";
    private final View takmlView;
    private Intent callbackIntent;
    private final Takml TAKML;
    private final TakmlExecutor takmlExecutor;
    private PluginSpinner mxPluginSpinner;
    private final Handler handler = new Handler(Looper.getMainLooper());

    /** It might be the case a TAK ML Model is a Pseudo model, and thus is not associated directly with an Mx Plugin.
        A pseudo model is an abstraction representing a category of models. For example there might be an
        edible plants model labelled as "Edible Plants" with a collection of models available based on
        the location. So, "Edible Plants" + lat, lon might represent on of "Edible Plants CONUS Tflite" | "Edible Plants OCONUS Tflite"

        See {@link com.atakmap.android.takml_android.hooks.MobileManagementManager}
     **/
    private static final String NA_MX_PLUGIN = "NA";

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
            final List<String> modelNames = new ArrayList<>();
            // Add on device models
            for(TakmlModel model : TAKML.getModels()){
                modelNames.add(model.getName());
            }

            SortedMap<String, TakmlModel> remoteModels = new TreeMap<>();
            ArrayAdapter<String> modelAdapter = new ArrayAdapter<String>(
                    context, android.R.layout.simple_spinner_item, modelNames) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View v = super.getView(position, convertView, parent);
                    ((TextView) v).setTextColor(Color.WHITE);
                    return v;
                }

                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    View v = super.getDropDownView(position, convertView, parent);
                    ((TextView) v).setTextColor(Color.WHITE);
                    return v;
                }
            };
            modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            modelSpinner.setAdapter(modelAdapter);
            modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                    if (view instanceof TextView) {
                        ((TextView) view).setTextColor(Color.WHITE);
                    }
                    String modelName = (String) modelSpinner.getSelectedItem();

                    TakmlModel selectedModel = remoteModels.get(modelName);
                    if(selectedModel != null){
                        TAKML.addTakmlModel(selectedModel);
                    }else{
                        selectedModel = TAKML.getModel(modelName);
                    }

                    TakmlModel finalSelectedModel = selectedModel;
                    AsyncTask.execute(() -> {
                        synchronized (takmlExecutor) {
                            try {
                                takmlExecutor.selectModel(finalSelectedModel);
                            } catch (Exception e){
                                Log.e(TAG, "Exception selecting model: " + finalSelectedModel.getName(), e);
                                handler.post(() -> Toast.makeText(context,
                                        "Exception selecting model: " + finalSelectedModel.getName(),
                                        Toast.LENGTH_LONG).show());
                            }
                            handler.post(() -> {
                                if(!finalSelectedModel.isPseudoModel() && !finalSelectedModel.isRemoteModel()) {
                                    mxPluginSpinner.setEnabled(true);
                                    mxPluginSpinner.setVisibility(View.VISIBLE);
                                    if (takmlExecutor.getSelectedModelExecutionPlugin() == null) {
                                        mxPluginSpinner.setSelection(0);
                                    } else {
                                        mxPluginSpinner.setSelection(TAKML.getModelExecutionPlugins().indexOf(
                                                takmlExecutor.getSelectedModelExecutionPlugin().getClass().getName()));
                                    }
                                }else{
                                    mxPluginSpinner.setEnabled(false);
                                    mxPluginSpinner.setVisibility(View.GONE);
                                }
                            });
                        }
                    });
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });

            // Add remote models
            fetchRemoteModels(modelNames, remoteModels, modelAdapter);

            // Mx Plugins
            mxPluginSpinner = takmlView.findViewById(R.id.mx_plugin_spiner);
            final List<String> mxPluginNames = new ArrayList<>();
            Map<String, String> simpleNameToClassName = new HashMap<>();

            // Add local mx plugins
            for(String mxPluginName : TAKML.getModelExecutionPlugins()){
                String result = mxPluginName.substring(mxPluginName.lastIndexOf('.') + 1).trim();
                mxPluginNames.add(result);
                simpleNameToClassName.put(result, mxPluginName);
            }

            mxPluginNames.add(NA_MX_PLUGIN);
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
                    if(mxPluginSelected != null && !NA_MX_PLUGIN.equals(mxPluginSelected)) {
                        Log.d(TAG, "selected mx plugin: " + mxPluginSelected);
                        synchronized (takmlExecutor) {
                            if (takmlExecutor.getSelectedModel().isRemoteModel() || takmlExecutor.getSelectedModel().isPseudoModel()) {
                                mxPluginSpinner.setEnabled(false);
                                mxPluginSpinner.setVisibility(View.GONE);
                            } else {
                                takmlExecutor.selectModelExecutionPlugin(mxPluginSelected);
                            }
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });
            synchronized (takmlExecutor) {
                if (takmlExecutor.getSelectedModelExecutionPlugin() != null) {
                    mxPluginSpinner.setEnabled(true);
                    mxPluginSpinner.setVisibility(View.VISIBLE);
                    mxPluginSpinner.setSelection(TAKML.getModelExecutionPlugins().indexOf(
                            takmlExecutor.getSelectedModelExecutionPlugin().getClass().getName()));
                } else {
                    mxPluginSpinner.setEnabled(false);
                    mxPluginSpinner.setVisibility(View.INVISIBLE);
                }
            }

            takmlView.findViewById(R.id.takmlSettingsBtn).setOnClickListener(view -> {
                Intent callbackIntent = new Intent();
                callbackIntent.setAction(SHOW_PLUGIN + TAKML.getUuid());
                TAKML.showConfigUI(callbackIntent);
            });

            synchronized (takmlExecutor) {
                modelSpinner.setSelection(TAKML.getModels().indexOf(takmlExecutor.getSelectedModel()));
            }
        }
    }

    private void updateModelSpinner(Map<String, TakmlModel> newModels,
                                    List<String> modelNames,
                                    Map<String, TakmlModel> remoteModels,
                                    ArrayAdapter<String> adapter) {
        handler.post(() -> {
            synchronized (remoteModels) {
                for (Map.Entry<String, TakmlModel> entry : newModels.entrySet()) {
                    if (!remoteModels.containsKey(entry.getKey())) {
                        String target = "Apple";
                        boolean contains = adapter.getPosition(target) >= 0;
                        if(!contains){
                        adapter.add(entry.getKey());
                        }
                        remoteModels.put(entry.getKey(), entry.getValue());
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void fetchRemoteModels(List<String> modelNames,
                                   SortedMap<String, TakmlModel> remoteModels,
                                   ArrayAdapter<String> adapter) {
        AsyncTask.execute(() -> {
            SortedMap<String, TakmlModel> fetchedRemoteModels = new TreeMap<>();
            TakServerInfo takServerInfo = SelectedTAKServer.getInstance().getTakServerInfo();

            if (takServerInfo == null) {
                TakServerUtils.getOrSelectNetwork((success, serverInfo) -> {
                    if (!success || serverInfo == null) {
                        Log.e(TAG, "Tak Server information is null");
                        return;
                    }
                    SelectedTAKServer.getInstance().setTAkServer(serverInfo);
                    Map<String, TakmlModel> result = populateListOfRemoteModels(serverInfo);
                    updateModelSpinner(result, modelNames, remoteModels, adapter);
                });
            } else {
                fetchedRemoteModels.putAll(populateListOfRemoteModels(takServerInfo));
                updateModelSpinner(fetchedRemoteModels, modelNames, remoteModels, adapter);
            }
        });
    }

    private SortedMap<String, TakmlModel> populateListOfRemoteModels(TakServerInfo takServerInf){
        SortedMap<String, TakmlModel> ret = new TreeMap<>();

        TakFsManager.getInstance().initialize(takServerInf.getTakserverApiClient());
        Set<IndexRow> indexRows = TakFsManager.getInstance().getModels();
        for(IndexRow indexRow : indexRows) {
            Map<String, String> additionalMetadata = indexRow.getAdditionalMetadata();

            if(additionalMetadata == null){
                continue;
            }

            String runOnServerString = additionalMetadata.get(RUN_ON_SERVER);
            if (!Boolean.parseBoolean(runOnServerString)) {
                continue;
            }

            String name = indexRow.getName();
            String modelType = additionalMetadata.get(MODEL_TYPE_META);

            // KServe URL
            String url = additionalMetadata.get(KSERVE_URL);
            String api = additionalMetadata.get(KSERVE_API);

            String labelsStr = additionalMetadata.get(MODEL_LABELS_META);
            List<String> labels = null;
            if(labelsStr != null){
                labels = new ArrayList<>(Arrays.asList(labelsStr.split(",")));
            }
            // TODO: support processing parameters defined on remote server,
            // we are using default parameters below for now

            /*String processingParamsStr = additionalMetadata.get(PROCESSING_PARAMS_META);
            ProcessingParams processingParams = null;
            if(processingParamsStr != null){
                processingParams = new Gson().fromJson(processingParamsStr, ProcessingParams.class);
            }*/

            // only supports image recognition at this point
            TensorProcessor tensorProcessor = new ImageRecognitionTensorProcessor(labels,null);

            // String name, String modelType, TensorProcessor tensorProcessor, String url, String api
            TakmlModel takmlModel = new TakmlModel.TakmlRemoteModelBuilder(name, modelType,
                    tensorProcessor, url, api).build();
            ret.put(takmlModel.getName(), takmlModel);
        };

        return ret;
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
