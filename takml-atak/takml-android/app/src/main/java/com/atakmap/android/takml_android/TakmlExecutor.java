package com.atakmap.android.takml_android;

import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;
import android.util.Pair;

import com.atakmap.android.dropdown.DropDownManager;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml_android.configuration.MetricsConfiguration;
import com.atakmap.android.takml_android.hooks.PermissionReply;
import com.atakmap.android.takml_android.hooks.TakmlOperation;
import com.atakmap.android.takml_android.lib.TakmlInitializationException;
import com.atakmap.android.takml_android.metrics.DeviceInfoUtil;
import com.atakmap.android.takml_android.metrics.MetricsManager;
import com.atakmap.android.takml_android.net.MetricsCallback;
import com.atakmap.android.takml_android.net.SelectedTAKServer;
import com.atakmap.android.takml_android.net.TakmlServerClient;
import com.atakmap.android.takml_android.service.MxPluginService;
import com.atakmap.android.takml_android.takml_result.Recognition;
import com.atakmap.android.takml_android.takml_result.Regression;
import com.atakmap.android.takml_android.takml_result.Segmentation;
import com.atakmap.android.takml_android.takml_result.TakmlResult;
import com.atakmap.android.takml_android.tensor_processor.InferInput;
import com.atakmap.android.takml_android.tensor_processor.KserveTensorConverterUtil;
import com.atakmap.android.takml_android.tensor_processor.TensorProcessor;
import com.atakmap.android.takml_android.ui.ExecutorSettingsReceiver;
import com.atakmap.android.takml_android.util.MxPluginsUtil;
import com.bbn.takml_server.ApiCallback;
import com.bbn.takml_server.ApiException;
import com.bbn.takml_server.client.models.AddModelMetricsRequest;
import com.bbn.takml_server.client.models.DeviceMetadata;
import com.bbn.takml_server.client.models.GpuInfo;
import com.bbn.takml_server.client.models.InferenceMetric;
import com.bbn.takml_server.client.models.InferenceRequest;
import com.atakmap.android.takml_android.util.SecureFileTransfer;
import com.bbn.takml_server.client.models.ModelMetrics;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TakmlExecutor {
    protected static final String TAG = TakmlExecutor.class.getName();

    protected ExecutorSettingsReceiver executorSettingsReceiver;

    protected static final String PLUGIN_CONTEXT_CLASS = "com.atak.plugins.impl.PluginContext";

    // required parameters
    protected TakmlModel selectedPseudoModel = null;
    protected TakmlModel selectedModel = null;
    protected MXPlugin selectedModelExecutionPlugin = null;
    protected final Takml takml;
    protected final Context pluginContext;
    protected final ExecutorService executorService = Executors.newSingleThreadExecutor();
    protected boolean runAsService = false;
    protected final ConcurrentMap<String, TakmlResultsCallback> requestIdToCallback = new ConcurrentHashMap<>();
    protected final Map<String, Integer> requestIdToRequestsLeft = new HashMap<>();
    protected final ConcurrentMap<String, SortedMap<Integer, List<? extends TakmlResult>>> requestIdToIndexToTakmlResults = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ServiceConnection> mxPluginToServiceConnections = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, IMxPluginService> mxPluginTIMxPluginService = new ConcurrentHashMap<>();

    protected TensorProcessor tensorProcessor;
    private Intent serviceIntent;
    private boolean isUsingPluginContext = false;
    MetricsManager metricsManager;

    interface ServiceCallback{
        void getServiceBinding(ServiceConnection serviceConnection, IMxPluginService iMxPluginService);
    }

    private void bindOrCreateServiceConnection(Class<? extends MxPluginService> mxPluginClass, ServiceCallback callback){
        // Create and bind to the Service
        serviceIntent = new Intent(pluginContext, mxPluginClass);
        serviceIntent.addFlags(FLAG_GRANT_READ_URI_PERMISSION);
        pluginContext.startForegroundService(serviceIntent);

        ServiceConnection serviceConnection = mxPluginToServiceConnections.get(mxPluginClass.getName());
        IMxPluginService mxPluginService = mxPluginTIMxPluginService.get(mxPluginClass.getName());
        if(serviceConnection != null && mxPluginService != null){
            callback.getServiceBinding(serviceConnection, mxPluginService);
            return;
        }
        serviceConnection = new ServiceConnection() {
            // Called when the connection with the service is established.
            public void onServiceConnected(ComponentName className, IBinder service) {
                Log.d(TAG, "onServiceConnected: " + className.getClassName());

                // Following the preceding example for an AIDL interface,
                // this gets an instance of the IRemoteInterface, which we can use to call on the service.
                IMxPluginService iRemoteService = IMxPluginService.Stub.asInterface(service);

                mxPluginTIMxPluginService.put(mxPluginClass.getName(), iRemoteService);
                callback.getServiceBinding(this, iRemoteService);
            }

            // Called when the connection with the service disconnects unexpectedly.
            public void onServiceDisconnected(ComponentName className) {
                Log.e(TAG, "Service has unexpectedly disconnected");
                mxPluginToServiceConnections.remove(className.getClassName());
                mxPluginTIMxPluginService.remove(className.getClassName());
            }
        };
        mxPluginToServiceConnections.put(mxPluginClass.getName(), serviceConnection);
        pluginContext.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    protected void initializeUIDropdownReceiver() throws NoClassDefFoundError{
        executorSettingsReceiver = new ExecutorSettingsReceiver(MapView.getMapView(),
                pluginContext, takml, this);
        AtakBroadcast.DocumentedIntentFilter ddFilter = new AtakBroadcast.DocumentedIntentFilter();
        ddFilter.addAction(ExecutorSettingsReceiver.SHOW_PLUGIN + takml.getUuid());
        DropDownManager.getInstance().registerDropDownReceiver(executorSettingsReceiver, ddFilter);
    }

    /**
     * Initializes the Takml Executor. Accepts ATAK plugin context or generic Activity context.
     * Note, the latter does not currently support {@link TakmlExecutor#showConfigUI(Intent)}
     *
     * @param takml - TAK ML model
     * @param pluginOrActivityContext - ATAK plugin or Android Activity context
     * @param mxPlugin - MX Plugin
     * @param takmlModel - TAK ML Model
     *
     * @throws TakmlInitializationException
     */
    protected TakmlExecutor(Takml takml, Context pluginOrActivityContext, MXPlugin mxPlugin, TakmlModel takmlModel,
                            boolean runAsService, TensorProcessor tensorProcessor) throws TakmlInitializationException {
        this.takml = takml;
        this.pluginContext = pluginOrActivityContext;
        this.selectedModelExecutionPlugin = mxPlugin;
        this.selectedModel = takmlModel;
        if(takmlModel.isPseudoModel()){
            selectedPseudoModel = takmlModel;
        }
        this.runAsService = runAsService;
        this.tensorProcessor = tensorProcessor;
        if(pluginContext != null) {
            isUsingPluginContext = pluginContext.getClass().getName().equals(PLUGIN_CONTEXT_CLASS);
            if(isUsingPluginContext) {
                initializeUIDropdownReceiver();
            }
        }

        metricsManager = new MetricsManager(takml);
        metricsManager.start();

        if(!takmlModel.isPseudoModel() && !takmlModel.isRemoteModel()) {
            if (runAsService) {
                startOrBindService(mxPlugin);
                registerModelWithService(takmlModel);
            } else {
                Log.d(TAG, "TakmlExecutor: " + selectedModelExecutionPlugin + " " + takmlModel.getName());
                selectedModelExecutionPlugin.instantiate(takmlModel, pluginOrActivityContext);
            }
        }
    }

    private void startOrBindService(MXPlugin mxPlugin) throws TakmlInitializationException{
        CountDownLatch countDownLatch = new CountDownLatch(1);
        bindOrCreateServiceConnection(mxPlugin.getOptionalServiceClass(),
                (serviceConnection, iMxPluginService) -> {
            countDownLatch.countDown();
        });
        try {
            if (!countDownLatch.await(30, TimeUnit.SECONDS)) {
                throw new TakmlInitializationException("Timed out waiting for service to start");
            }
        } catch (InterruptedException e) {
            throw new TakmlInitializationException("InterruptedException waiting for service to start", e);
        }
    }

    private IMxPluginService waitForBoundService(Class<? extends MxPluginService> mxPluginClass, long timeoutSecs) throws TakmlInitializationException {
        String pluginClassName = mxPluginClass.getName();

        IMxPluginService service = mxPluginTIMxPluginService.get(pluginClassName);
        if (service != null) return service;

        CountDownLatch latch = new CountDownLatch(1);

        bindOrCreateServiceConnection(mxPluginClass, (conn, boundService) -> {
            mxPluginTIMxPluginService.put(pluginClassName, boundService);
            latch.countDown();
        });

        try {
            if (!latch.await(timeoutSecs, TimeUnit.SECONDS)) {
                throw new TakmlInitializationException("Timeout: Service not connected for " + pluginClassName);
            }
        } catch (InterruptedException e) {
            throw new TakmlInitializationException("Interrupted while waiting for service " + pluginClassName, e);
        }

        service = mxPluginTIMxPluginService.get(pluginClassName);
        if (service == null) {
            throw new TakmlInitializationException("Service is still null after wait: " + pluginClassName);
        }

        return service;
    }

    private void registerModelWithService(TakmlModel takmlModel) throws TakmlInitializationException {
        String processingParams = takmlModel.getProcessingParams() != null ?
                new Gson().toJson(takmlModel.getProcessingParams()) : null;

        IMxPluginService service = waitForBoundService(
                selectedModelExecutionPlugin.getOptionalServiceClass(), 10);

        try {
            Uri contentURI = takmlModel.getModelUri();
            String mlaPluginPackageName = takml.getPluginContext().getPackageName();

            Log.d(TAG, "About to register model. Granting access to " + contentURI);

            if (isUsingPluginContext) {
                MapView.getMapView().getContext().grantUriPermission(
                        mlaPluginPackageName, contentURI,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
            }

            service.registerModel(
                    takmlModel.getModelUUID().toString(),
                    takmlModel.getName(),
                    takmlModel.getModelExtension(),
                    takmlModel.getModelType(),
                    contentURI.toString(),
                    processingParams,
                    takmlModel.getLabels()
            );
        } catch (RemoteException e) {
            throw new TakmlInitializationException("RemoteException registering model", e);
        }
    }

    /**
     * Execute the prediction
     *
     * @param inputData - input data to run prediction
     * @param callback - callback with result of execution
     */
    public void executePrediction(byte[] inputData, TakmlResultsCallback callback){
        executePrediction(Collections.singletonList(inputData), callback);
    }

    /**
     * Execute the prediction
     *
     * @param inputDatas - input data to run prediction, multiple different inputs
     * @param callback - callback with result of execution
     */
    public void executePrediction(List<byte[]> inputDatas, TakmlResultsCallback callback){
        if(selectedPseudoModel != null){
            selectedModel = selectedPseudoModel;
        }

        if(selectedModel.isPseudoModel()){
            Log.d(TAG, "executePrediction with model (Pseudo Model): " + selectedModel.getName());
        }else if(selectedModel.isRemoteModel()){
            Log.d(TAG, "executePrediction with model (Remote Model): " + selectedModel.getName());
        }else {
            Log.d(TAG, "executePrediction with model " + selectedModel.getName()
                    + " and mx plugin " + getSelectedModelExecutionPlugin().getClass().getSimpleName());
        }
        PermissionReply permissionReply = takml.mobileManagementManager.checkIfAllowedToRun(TakmlOperation.INFERENCE, selectedModel.getName());
        if(!permissionReply.isPermitted()){
            Log.w(TAG, "executePrediction: not allowed to run inference");
            callback.modelResults(null, false, null, null);
            return;
        }else{
            Log.d(TAG, "executePrediction: received permission reply from EMM: " + new Gson().toJson(permissionReply));
            boolean modelDifferent = false;
            if(permissionReply.getModelSelected() != null) {
                if(takml.getModel(permissionReply.getModelSelected()) != selectedModel) {
                    modelDifferent = true;
                    selectedModel = takml.getModel(permissionReply.getModelSelected());
                    if (selectedModel == null) {
                        Log.e(TAG, "executePrediction: For model" + getSelectedModel() +
                                ", invalid model was selected by the mobile management manager with value: "
                                + permissionReply.getModelSelected());
                        return;
                    }
                }
            }

            if(!selectedModel.isRemoteModel()) {
                if(selectedModel.getModelExtension() == null){
                    Log.e(TAG, "executePrediction: selected model does not have a valid model extension \"" + selectedModel.getName() + "\"");
                    callback.modelResults(null, false, null, null);
                    return;
                }
                Set<String> mxPluginNames = takml.getApplicablePluginClassNames(selectedModel);
                if (mxPluginNames == null) {
                    Log.e(TAG, "Could not find applicable plugin classname for TAK ML Model: " + selectedModel.getName());
                } else {
                    if (mxPluginNames.size() > 1) {
                        Log.w(TAG, "More than one mx plugin is applicable, picking first one");
                    }
                    String mxPluginName = mxPluginNames.iterator().next();

                    // If Mx Plugin is different from before, instantiate it
                    if(modelDifferent || selectedModelExecutionPlugin == null || !mxPluginName.equals(selectedModelExecutionPlugin.getClass().getName())) {
                        selectModelExecutionPlugin(mxPluginName, true);
                    }
                }
            }
        }

        /** Run Remotely */
        if(selectedModel.isRemoteModel()){
            TensorProcessor tensorProcessor = selectedModel.getTensorProcessor();
            if(tensorProcessor == null){
                Log.e(TAG, "Could not execute remote inference, TensorProcessor is null");
                return;
            }

            /** Step 1. Preprocess */
            List<InferInput> inferInputs = tensorProcessor.processInputTensor(inputDatas);

            /** Step 2. Execute Remote Inference */
            String modelName = selectedModel.getName();
            String modelVersion = "1.0"; // TODO: support versioning
            InferenceRequest inferenceRequest = new InferenceRequest();
            inferenceRequest.setId(UUID.randomUUID().toString()); // TODO: this serves no purpose
            inferenceRequest.setInputs(KserveTensorConverterUtil.convertInferInputs(inferInputs));

            takml.createTakmlServerClient(selectedModel.getUrl(), selectedModel.getApiKeyName(),
                    selectedModel.getApiKey()).executeModelAsync(
                    inferenceRequest, modelName, modelVersion, outputs -> {
                if(outputs == null){
                    callback.modelResults(null, false, modelName, "remote");
                }else {
                    /** Step 3. Postprocess */
                    List<List<? extends TakmlResult>> results = tensorProcessor.processOutputTensor(KserveTensorConverterUtil.convertInferOutputs(outputs));
                    /** Step 4. Return the results! */
                    callback.modelResults(results, true, modelName, selectedModel.getModelType());
                }
            });
            return;
        }

        /** Run Locally */
        String modelName = selectedModel.getName();
        String modelType = selectedModel.getModelType();
        executorService.execute(() -> {
            long startTime = System.currentTimeMillis();

            // run custom tensor processor code if applicable
            List<InferInput> inferInputs;
            if(tensorProcessor != null){
                inferInputs = tensorProcessor.processInputTensor(inputDatas);
            } else {
                inferInputs = null;
            }

            List<List<? extends TakmlResult>> takmlResultsAll = new ArrayList<>();
            AtomicBoolean successful = new AtomicBoolean(true);
            CountDownLatch countDownLatchInferencesOps = new CountDownLatch(inputDatas.size());
            if(runAsService){
                synchronized (requestIdToRequestsLeft) {
                    requestIdToRequestsLeft.put(permissionReply.getRequestId(), inputDatas.size());
                }
            }

            for(int i=0; i < inputDatas.size(); i++) {
                byte[] inputData = inputDatas.get(i);
                if (runAsService) {
                    requestIdToCallback.put(permissionReply.getRequestId(),
                            (takmlResults, success, modelName1, modelType1) -> {
                        if(!success){
                            successful.set(false);
                        }
                        takmlResultsAll.addAll(takmlResults);
                        countDownLatchInferencesOps.countDown();
                    });
                    IMxPluginService service = mxPluginTIMxPluginService.get(selectedModelExecutionPlugin.getOptionalServiceClass().getName());
                    if(inferInputs != null){
                        String json = new Gson().toJson(inferInputs);
                        SecureFileTransfer tempFileDataTransfer = new SecureFileTransfer(MapView.getMapView().getContext());
                        ParcelFileDescriptor parcelFileDescriptor = tempFileDataTransfer.writeJsonToTempFile(json);
                        Log.d(TAG, "created file descriptor: " + parcelFileDescriptor.toString());
                        try {
                            service.executeTensor(permissionReply.getRequestId() + "_" + i, selectedModel.getModelUUID().toString(), parcelFileDescriptor);
                        } catch (RemoteException e) {
                            Log.e(TAG, "RemoteException running prediction", e);
                        }
                    }else {
                        try {
                            service.execute(permissionReply.getRequestId() + "_" + i, selectedModel.getModelUUID().toString(), inputData);
                        } catch (RemoteException e) {
                            Log.e(TAG, "RemoteException running prediction", e);
                        }
                    }
                } else {
                    if(inferInputs != null){
                        selectedModelExecutionPlugin.execute(inferInputs, (takmlResults, success, model, type) -> {
                            if (!success) {
                                successful.set(false);
                            }
                            takmlResultsAll.add(takmlResults);
                            countDownLatchInferencesOps.countDown();
                        });
                    }else {
                        selectedModelExecutionPlugin.execute(inputData,
                                (takmlResults, success, model, type) -> {
                                    if (!success) {
                                        successful.set(false);
                                    }
                                    takmlResultsAll.add(takmlResults);
                                    countDownLatchInferencesOps.countDown();
                                });
                    }
                }
            }
            try {
                if(!countDownLatchInferencesOps.await(20, TimeUnit.SECONDS)){
                    Log.e(TAG, "Timed out waiting for all inferences for request");
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Timed out waiting for prediction", e);
            }
            if(permissionReply.getRequestId() != null) {
                takml.mobileManagementManager.maybeInvokeInferenceEndRequests(permissionReply.getRequestId(), successful.get());
            }
            callback.modelResults(takmlResultsAll, successful.get(), modelName, modelType);

            // return metrics for highest confidence results
            metricsManager.consumeMetrics(selectedModel, startTime, takmlResultsAll);
        });
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

        if(!takmlModel.isPseudoModel() && !takmlModel.isRemoteModel()) {
            Set<String> mxPluginNames = takml.getApplicablePluginClassNames(selectedModel);
            if (mxPluginNames == null) {
                Log.e(TAG, "Could not find applicable plugin classname for TAK ML Model: " + takmlModel.getName());
            } else {
                if (mxPluginNames.size() > 1) {
                    Log.w(TAG, "More than one mx plugin is applicable, picking first one");
                }
                selectModelExecutionPlugin(mxPluginNames.iterator().next());
            }
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
        selectModelExecutionPlugin(mxPlugin, true);
    }

    private void selectModelExecutionPlugin(String mxPlugin, boolean keepPseudoModelSelectedIfNotNull){
        if(!keepPseudoModelSelectedIfNotNull){
            selectedPseudoModel = null;
        }

        if(selectedModelExecutionPlugin != null) {
            selectedModelExecutionPlugin.shutdown();
        }

        if (selectedModel.isPseudoModel()){
            Log.d(TAG, "Selected TAK ML Model is a pseudo model, not instantiating mx plugin");
            return;
        }

        try {
            this.selectedModelExecutionPlugin = MxPluginsUtil.constructMxPlugin(mxPlugin);
        } catch (Exception e) {
            Log.e(TAG, "could not instantiate model execution plugin with name: " + mxPlugin, e);
        }

        if(runAsService){
            try {
                waitForBoundService(selectedModelExecutionPlugin.getOptionalServiceClass(), 20);
                registerModelWithService(selectedModel);
            } catch (TakmlInitializationException e) {
                throw new RuntimeException(e);
            }
        }else {
            try {
                selectedModelExecutionPlugin.instantiate(selectedModel, pluginContext);
            } catch (TakmlInitializationException e) {
                throw new RuntimeException(e);
            }
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
        executorService.shutdown();
        try {
            // Wait a while for existing tasks to terminate
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                // Cancel currently executing tasks forcefully
                executorService.shutdownNow();
                // Wait a while for tasks to respond to being cancelled
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS))
                    Log.e(TAG, "Pool did not terminate");
            }
        } catch (InterruptedException ex) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        if(serviceIntent != null) {
            pluginContext.stopService(serviceIntent);
        }
        metricsManager.shutdown();
    }

    protected void consumeMxServiceResults(String requestId, boolean success, String modelName,
                                           String modelType, List<? extends TakmlResult> takmlResults) {
        String[] requestIdSplit = requestId.split("_");
        String requestIdBase = requestIdSplit[0];
        String index = requestIdSplit[1];
        synchronized (requestIdToRequestsLeft) {
            Log.d(TAG, "consumeMxServiceResults: " + requestIdBase);
            for(Map.Entry<String, Integer> i : requestIdToRequestsLeft.entrySet()){
                Log.d(TAG, "consumeMxServiceResults has: " + i.getKey() + " " + i.getValue());
            }
            Integer count = requestIdToRequestsLeft.get(requestIdBase);
            if(count == null){
                return;
            }
            SortedMap<Integer, List<? extends TakmlResult>> output = requestIdToIndexToTakmlResults.computeIfAbsent(requestIdBase, k -> new TreeMap<>());
            output.putIfAbsent(Integer.parseInt(index), takmlResults);
            Log.d(TAG, "consumeMxServiceResults: count " + count);
            count--;
            if (count == 0) {
                TakmlResultsCallback callback = requestIdToCallback.remove(requestIdBase);
                if(callback != null) {
                    // clean up
                    requestIdToRequestsLeft.remove(requestIdBase);
                    requestIdToIndexToTakmlResults.remove(requestIdBase);

                    // Notify results
                    //takml.mobileManagementManager.maybeInvokeInferenceEndRequests(requestId, success);
                    Log.d(TAG, "consumeMxServiceResults: notify results " + output.values().size());
                    callback.modelResults(new ArrayList<>(output.values()), success, modelName, modelType);
                }
            }
        }
    }

    public boolean hasRequestId(String requestId) {
        return requestIdToCallback.containsKey(requestId);
    }

}
