package com.atakmap.android.takml_android;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.atakmap.android.dropdown.DropDownManager;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml_android.lib.TakmlInitializationException;
import com.atakmap.android.takml_android.ui.ExecutorSettingsReceiver;
import com.atakmap.coremap.maps.coords.GeoPoint;

import java.util.Set;

public class TakmlExecutor {
    private static final String TAG = TakmlExecutor.class.getName();

    private ExecutorSettingsReceiver executorSettingsReceiver;

    // required parameters

    private TakmlModel selectedModel = null;
    private MXPlugin selectedModelExecutionPlugin = null;
    private final Takml takml;
    private final Context pluginContext;


    protected void initializeUIDropdownReceiver(){
        executorSettingsReceiver = new ExecutorSettingsReceiver(MapView.getMapView(),
                pluginContext, takml, this);
        AtakBroadcast.DocumentedIntentFilter ddFilter = new AtakBroadcast.DocumentedIntentFilter();
        ddFilter.addAction(ExecutorSettingsReceiver.SHOW_PLUGIN + takml.getUuid());
        DropDownManager.getInstance().registerDropDownReceiver(executorSettingsReceiver, ddFilter);
    }

    /**
     * Initializes the Takml Executor.
     *
     * @param takml - TAK ML model
     * @param pluginContext - ATAK plugin context
     * @param mxPlugin - MX Plugin
     * @param takmlModel - TAK ML Model
     *
     * @throws TakmlInitializationException
     */
    protected TakmlExecutor(Takml takml, Context pluginContext, MXPlugin mxPlugin, TakmlModel takmlModel) throws TakmlInitializationException {
        this.takml = takml;
        this.pluginContext = pluginContext;
        this.selectedModelExecutionPlugin = mxPlugin;
        this.selectedModel = takmlModel;
        if(pluginContext != null)
            initializeUIDropdownReceiver();
        selectedModelExecutionPlugin.instantiate(takmlModel);
    }

    /**
     * Selects the TAK ML Model to be used for prediction, and instantiates with an applicable MX Plugin. See
     * {@link Takml#getModels()} for a list of available TAK ML Models. See {@link TakmlExecutor#selectModelExecutionPlugin
     * for changing MX Plugin.
     *
     * @param takmlModel - the Takml Model
     */
    public void selectModel(TakmlModel takmlModel){
        this.selectedModel = takmlModel;
        Log.d(TAG, "selectModel: " + selectedModel.getName());

        Set<String> mxPluginNames = takml.getApplicablePluginClassNames(selectedModel);
        if(mxPluginNames == null){
            Log.e(TAG, "Could not find applicable plugin classname for TAK ML Model: " + takmlModel.getName());
        }else{
            if(mxPluginNames.size() > 1){
                Log.w(TAG, "More than one mx plugin is applicable, picking first one");
            }
            selectModelExecutionPlugin(mxPluginNames.iterator().next());
        }
    }

    /**
     * Returns the selected TAK ML Model
     *
     * @return Takml Model
     */
    public TakmlModel getSelectedModel(){
        return selectedModel;
    }

    /**
     * Selects the Model Execution Plugin to use for prediction. See {@link Takml#getModelExecutionPlugins()} ()}.
     * <pre>A TakmlModel must be selected prior, see {@link TakmlExecutor#selectModel(TakmlModel)}
     * </pre>
     *
     * @param mxPlugin - MX plugin
     */
    public void selectModelExecutionPlugin(String mxPlugin){
        if(selectedModelExecutionPlugin != null){
            selectedModelExecutionPlugin.shutdown();
        }

        try {
            this.selectedModelExecutionPlugin = takml.constructMxPlugin(mxPlugin);
        } catch (Exception e) {
            Log.e(TAG, "could not instantiate model execution plugin with name: " + mxPlugin, e);
        }

        try {
            selectedModelExecutionPlugin.instantiate(selectedModel);
        } catch (TakmlInitializationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the selected Model Execution Plugin.
     *
     * @return selected MX Plugin
     */
    public MXPlugin getSelectedModelExecutionPlugin(){
        return selectedModelExecutionPlugin;
    }


    /**
     * Execute the prediction
     *
     * @param inputData - input data to run prediction
     * @param callback - callback with result of execution
     */
    public void executePrediction(byte[] inputData, MXExecuteModelCallback callback){
        Log.d(TAG, "executePrediction with model " + getSelectedModel().getName()
                + " and mx plugin " + getSelectedModelExecutionPlugin().getClass().getSimpleName());
        selectedModelExecutionPlugin.execute(inputData, callback);
    }

    /// Config UI
    public void showConfigUI(Intent callbackIntent){
        Log.d(TAG, "showConfigUI");
        Intent intent = new Intent();
        intent.setAction(ExecutorSettingsReceiver.SHOW_PLUGIN + takml.getUuid());
        executorSettingsReceiver.setCallbackIntent(callbackIntent);
        AtakBroadcast.getInstance().sendBroadcast(intent);
    }

    public void shutdown(){
        selectedModelExecutionPlugin.shutdown();
    }
}
