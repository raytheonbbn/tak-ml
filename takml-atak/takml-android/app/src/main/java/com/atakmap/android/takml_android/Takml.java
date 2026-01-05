package com.atakmap.android.takml_android;

import static android.content.Context.RECEIVER_EXPORTED;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.content.IntentFilter;
import android.os.AsyncTask;
import android.util.Log;

import com.atakmap.android.dropdown.DropDownManager;
import com.atakmap.android.importexport.ImportExportMapComponent;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml_android.configuration.TakmlServerConfiguration;
import com.atakmap.android.takml_android.hooks.HookEndpointState;
import com.atakmap.android.takml_android.hooks.MobileManagementManager;
import com.atakmap.android.takml_android.lib.TakmlInitializationException;
import com.atakmap.android.takml_android.net.SelectedTAKServer;
import com.atakmap.android.takml_android.net.TakmlServerClient;
import com.atakmap.android.takml_android.storage.MissionPackageImportResolver;
import com.atakmap.android.takml_android.storage.TakmlModelStorage;
import com.atakmap.android.takml_android.takml_result.TakmlResult;
import com.atakmap.android.takml_android.tensor_processor.TensorProcessor;
import com.atakmap.android.takml_android.ui.TakmlSettingsReceiver;
import com.atakmap.android.takml_android.util.MxPluginsUtil;
import com.atakmap.android.takml_android.util.TakServerInfo;
import com.atakmap.android.takml_android.util.TakServerUtils;
import com.bbn.takml_server.client.ModelFeedbackApi;
import com.bbn.takml_server.client.ModelManagementApi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Takml {
    private static final String TAG = Takml.class.getName();

    protected final Set<String> pluginNamesFoundOnClasspath = new HashSet<>();
    private static final String TAKML_MODEL_CONFIG_EXTENSION = ".yaml";


    // effectively final
    private Context pluginOrActivityContext;
    protected final List<TakmlModel> takmlModels = new ArrayList<>();

    protected ConcurrentMap<String, Set<String>> fileExtensionToMxPluginClassNames = new ConcurrentHashMap<>();

    private TakmlSettingsReceiver takmlSettingsReceiver;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    private final AtomicBoolean finishedInitializing = new AtomicBoolean();
    private static final int INITIALIZATION_TIMEOUT_SECONDS = 10;

    private final UUID uuid = UUID.randomUUID();
    private static final String PLUGIN_CONTEXT_CLASS = "com.atak.plugins.impl.PluginContext";
    public static final String SERVICE_MODEL_RESULT = Takml.class.getName() + ".SERVICE_MODEL_RESULT";
    private final Set<TakmlExecutor> takmlExecutors = new HashSet<>();
    private TakmlModelStorage takmlModelStorage;
    protected MobileManagementManager mobileManagementManager;
    private final Object takmlLock = new Object();
    private TakmlServerClient takmlServerClient;
    private TakmlServerConfiguration takmlServerConfiguration;

    /**
     * Initializes a Takml object. Accepts ATAK plugin context or generic Activity context.
     * Note, the latter does not currently support {@link Takml#showConfigUI(Intent)}. Assumes
     * default configuration for TAK ML Server (sharing same TAK Server IP and certificates). Note,
     * TAK ML Server is optional.
     *
     * @param pluginOrActivityContext - ATAK plugin or Android Activity context
     *
     * @throws TakmlInitializationException
     */
    public Takml(Context pluginOrActivityContext){
        this(pluginOrActivityContext, TakmlServerConfiguration.DEFAULT_CONFIGURATION);
    }

    /**
     * Initializes a Takml object. Accepts ATAK plugin context or generic Activity context.
     * Note, the latter does not currently support {@link Takml#showConfigUI(Intent)}. Also configures
     * TAK ML Server.
     *
     * @param pluginOrActivityContext - ATAK plugin or Android Activity context
     * @param takmlServerConfiguration - Takml Server configuration
     */
    public Takml(Context pluginOrActivityContext, TakmlServerConfiguration takmlServerConfiguration){
        initialize(pluginOrActivityContext, takmlServerConfiguration);
    }

    private void initialize(Context pluginOrActivityContext, TakmlServerConfiguration takmlServerConfiguration){
        this.pluginOrActivityContext = pluginOrActivityContext;
        this.takmlServerConfiguration = takmlServerConfiguration;

        fileExtensionToMxPluginClassNames = MxPluginsUtil.discoverMxPlugins(this.pluginOrActivityContext);
        for(Set<String> value : fileExtensionToMxPluginClassNames.values()) {
            pluginNamesFoundOnClasspath.addAll(value);
        }

        takmlModelStorage = new TakmlModelStorage(this, pluginOrActivityContext);
        takmlModelStorage.initialize(countDownLatch);
        boolean isUsingPluginContext = pluginOrActivityContext.getClass().getName().equals(PLUGIN_CONTEXT_CLASS);
        if(isUsingPluginContext) {
            takmlSettingsReceiver = new TakmlSettingsReceiver(MapView.getMapView(), pluginOrActivityContext,
                    this);
            AtakBroadcast.DocumentedIntentFilter ddFilter = new AtakBroadcast.DocumentedIntentFilter();
            ddFilter.addAction(TakmlSettingsReceiver.SHOW_PLUGIN + uuid);
            ddFilter.addAction(TakmlSettingsReceiver.IMPORTED_TAKML_MODEL + uuid);
            DropDownManager.getInstance().registerDropDownReceiver(takmlSettingsReceiver, ddFilter);
            ImportExportMapComponent.getInstance().addImporterClass(
                    new MissionPackageImportResolver(TAKML_MODEL_CONFIG_EXTENSION, null,
                            true, true, this));

            TakmlReceiver takmlReceiver = new TakmlReceiver(this, takmlModelStorage);
            AtakBroadcast.DocumentedIntentFilter ddFilter2 = new AtakBroadcast.DocumentedIntentFilter();
            ddFilter2.addAction(TakmlReceiver.RECEIVE);
            ddFilter2.addAction(TakmlReceiver.IMPORT_TAKML_MODEL);
            AtakBroadcast.getInstance().registerReceiver(takmlReceiver, ddFilter2);

            ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            scheduledExecutorService.scheduleAtFixedRate(() -> {
                Intent intent = new Intent();
                intent.setAction(TakmlReceiver.RECEIVE);
                intent.putExtra(Constants.TAK_ML_UUID, uuid.toString());
                intent.putStringArrayListExtra(Constants.KNOWN_MX_PLUGINS, new ArrayList<>(pluginNamesFoundOnClasspath));
                AtakBroadcast.getInstance().sendBroadcast(intent);
            }, 5, 5, TimeUnit.SECONDS);

            createTakmlServerClient(takmlServerConfiguration);
        }

        // Set up Mx Plugin Service BroadcastReceiver
        setupMxPluginBroadcastReceiver();

        try {
            initializeEmm(pluginOrActivityContext);
        } catch (NoClassDefFoundError e) {
            Log.d(TAG, "Takml: not using an EMM, library not found");
        } catch (Throwable t) {
            Log.d(TAG, "Exception loading EMM library", t);
        }

        if(!isUsingPluginContext){
            countDownLatch.countDown();
        }
    }

    /**
     * If applicable, connect to TAKML Server. This assumes TAK Server and TAK ML share the same
     * certificates. If no ip and port are specified, it will default to the connected TAK Server
     * and if there is more than one TAK Server it will ask the user for the correct TAK Server.
     *
     * @param takmlServerConfiguration - optional, takml server configuration
     */
    private void createTakmlServerClient(TakmlServerConfiguration takmlServerConfiguration) {
        if (takmlServerConfiguration.isShareTakServerIpAndCerts()){
            synchronized (takmlLock) {
                TakServerInfo serverInfo = TakServerUtils.getTakServerInfo();
                if(serverInfo == null){
                    // no TAK Server connection
                    return;
                }
                SelectedTAKServer.getInstance().setTAkServer(serverInfo);
                takmlServerClient = createTakmlServerClient(serverInfo.getTakServer()
                        .getURL(false) + ":" + takmlServerConfiguration.getPort());
            }
        } else {
            String ip = takmlServerConfiguration.getIp();
            int port = takmlServerConfiguration.getPort();
            String apiKeyName = takmlServerConfiguration.getApiKeyName();
            String apiKey = takmlServerConfiguration.getApiKey();
            byte[] clientStoreBytes = takmlServerConfiguration.getClientStoreCert();
            byte[] trustStoreBytes = takmlServerConfiguration.getTrustStoreCert();
            String clientStorePass = takmlServerConfiguration.getClientStorePass();
            String trustStorePass = takmlServerConfiguration.getTruststorePass();
            synchronized (takmlLock) {
                takmlServerClient = createTakmlServerClient("https://" + ip + ":" + port,
                        apiKeyName, apiKey, clientStoreBytes, trustStoreBytes,
                        clientStorePass, trustStorePass);
            }
        }
    }

    protected void initializeEmm(Context context) throws ClassNotFoundException{
        mobileManagementManager = new MobileManagementManager();

        // Set up Enterprise Mobile Management Manager Hooks
        mobileManagementManager.start(pluginOrActivityContext);

        Log.d(TAG, "Enterprise Mobile Manager Initialized");
    }

    protected void setupMxPluginBroadcastReceiver(){
        Log.d(TAG, "Attempting to registered receiver for TAK-ML service results");
        BroadcastReceiver mysms=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "mxPluginBCR: " + intent.getAction());
                if(intent.getAction().equals(SERVICE_MODEL_RESULT)){
                    String requestId = intent.getStringExtra(Constants.TAKML_MX_SERVICE_REQUEST_ID);
                    boolean success = intent.getBooleanExtra(Constants.TAKML_MX_SERVICE_REQUEST_SUCCESS, false);
                    String modelName = intent.getStringExtra(Constants.TAKML_MX_SERVICE_REQUEST_MODEL_NAME);
                    String modelType = intent.getStringExtra(Constants.TAKML_MX_SERVICE_REQUEST_MODEL_TYPE);
                    ArrayList<TakmlResult> takmlResults = intent.getParcelableArrayListExtra(Constants.TAKML_RESULT_LIST);
                    if(requestId != null && !requestId.isEmpty()) {
                        synchronized (takmlExecutors) {
                            for (TakmlExecutor takmlExecutor : takmlExecutors) {
                                takmlExecutor.consumeMxServiceResults(requestId, success, modelName, modelType, takmlResults);
                            }
                        }
                    } else{
                        Log.w(TAG, "Mx plugin result had a null request id, ignoring...");
                    }
                }
            }
        };

        pluginOrActivityContext.registerReceiver(mysms, new IntentFilter(SERVICE_MODEL_RESULT), RECEIVER_EXPORTED);
        Log.d(TAG, "Successfully registered receiver for TAK-ML service results");
    }

    public UUID getUuid() {
        return uuid;
    }

    /**
     * Adds an initialization listener with a callback indicating when TAK ML has finished
     * importing models from disk and is fully initialized. This is particularly useful for
     * accessing models from disk via {@link Takml#getModels()} or {@link Takml#getModel(String)}.
     *
     * @param callback - callback indicating when TAK ML is fully initialized.
     */
    public void addInitializationListener(TakmlInitializationListener callback){
        if(finishedInitializing.get()){
            callback.finishedInitializing();
        }else {
            AsyncTask.execute(() -> {
                try {
                    if(!countDownLatch.await(INITIALIZATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)){
                        Log.e(TAG, "Did not finish initializing in timeout of " + INITIALIZATION_TIMEOUT_SECONDS + " seconds");
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "Initialization interrupted ", e);
                }
                finishedInitializing.set(true);
                callback.finishedInitializing();
            });
        }
    }

    /**
     * Adds a TAK ML Model to TAK ML. If desiring to persist model to disk, please see
     * {@link Takml#addTakmlModel(TakmlModel, boolean)}}
     *
     * @param takmlModel - The Model
     */
    public void addTakmlModel(TakmlModel takmlModel){
        addTakmlModel(takmlModel, false);
    }

    /**
     * Adds a TAK ML Model to TAK ML
     *
     * @param takmlModel - The Model
     * @param persistToDisk - Whether to persist to disk
     *
     * @Return boolean - if persistance to disk was successful
     */
    public boolean addTakmlModel(TakmlModel takmlModel, boolean persistToDisk){
        // remove existing model that matches friendlyName
        takmlModels.removeIf(existingModel -> existingModel.getName().equals(takmlModel.getName()));
        // add new the model
        takmlModels.add(takmlModel);

        if(persistToDisk){
            return takmlModelStorage.importToDisk(takmlModel);
        }

        return true;
    }

    /**
     * Returns list of available Model Execution Plugins. Select one to be used for prediction;
     * see {@link TakmlExecutor#selectModelExecutionPlugin(String)}
     *
     * @return list of MXPlugin
     */
    public List<String> getModelExecutionPlugins(){
        return new ArrayList<>(pluginNamesFoundOnClasspath);
    }

    /**
     * Returns list of TAK ML Models available. Select one to be used for prediction;
     * see {@link TakmlExecutor#selectModel(TakmlModel)}
     *
     * @return list of TakmlModel
     */
    public List<TakmlModel> getModels(){
        return new ArrayList<>(takmlModels);
    }

    /**
     * Request TAK ML Model with a given Friendly Name
     *
     * @param friendlyName - name of model
     * @return TakmlModel if exists, or null
     */
    public TakmlModel getModel(String friendlyName){
        for(TakmlModel takmlModel : takmlModels) {
            if (takmlModel.getName().equals(friendlyName)) {
                return takmlModel;
            }
        }
        return null;
    }

    /**
     * Initializes the Takml Executor. See {@link Takml#getModels()} to view all available models
     * on device.
     *
     * @param takmlModel - Takml Model
     * @return TakmlExecutor - Provides a wrapper for executing prediction
     *
     * @throws TakmlInitializationException
     */
    public TakmlExecutor createExecutor(TakmlModel takmlModel) throws TakmlInitializationException{
        return createExecutor(takmlModel, false, null);
    }

    /**
     * Initializes the Takml Executor. See {@link Takml#getModels()} to view all available models
     * on device.
     *
     * @param takmlModel - Takml Model
     * @return TakmlExecutor - Provides a wrapper for executing prediction
     *
     * @throws TakmlInitializationException
     */
    public TakmlExecutor createExecutor(TakmlModel takmlModel, boolean runAsService) throws TakmlInitializationException{
        return createExecutor(takmlModel, runAsService, null);
    }

    /**
     * Initializes the Takml Executor. See {@link Takml#getModels()} to view all available models
     * on device.
     *
     * @param takmlModel - Takml Model
     * @return TakmlExecutor - Provides a wrapper for executing prediction
     *
     * @throws TakmlInitializationException
     */
    public TakmlExecutor createExecutor(TakmlModel takmlModel, boolean runAsService, TensorProcessor tensorProcessor) throws TakmlInitializationException{
        if(takmlModel == null){
            throw new TakmlInitializationException("The TAK ML model specified is null, returning null");
        }
        if(runAsService && takmlModel.isRemoteModel()){
            Log.w(TAG, "Running remote models as a service is not supported, running as a remote call");
        }

        MXPlugin mxPlugin = null;

        if(!takmlModel.isRemoteModel() && !takmlModel.isPseudoModel()) {
            if (takmlModel.getModelExtension() == null) {
                throw new TakmlInitializationException("The TAK ML model extension is null, returning null");
            }
            if (!takmlModel.isPseudoModel()) {
                Set<String> mxPluginNames = fileExtensionToMxPluginClassNames.get(takmlModel.getModelExtension());
                if (mxPluginNames == null) {
                    throw new TakmlInitializationException("Could not find an applicable mx plugin for model with extension '"
                            + takmlModel.getModelExtension() + "', returning null");
                }
                String mxPluginName = mxPluginNames.iterator().next();
                if (mxPluginNames.size() > 1) {
                    Log.w(TAG, "more than one mx plugin available, selecting first one");
                }
                try {
                    mxPlugin = MxPluginsUtil.constructMxPlugin(mxPluginName);
                } catch (Exception e) {
                    throw new TakmlInitializationException("Could not instantiate mx plugin: " + mxPluginName);
                }
            }
        }

        TakmlExecutor takmlExecutor = new TakmlExecutor(this, pluginOrActivityContext,
                    mxPlugin, takmlModel, runAsService, tensorProcessor);

        synchronized (takmlExecutors) {
            takmlExecutors.add(takmlExecutor);
        }
        return takmlExecutor;
    }

    protected Set<String> getApplicablePluginClassNames(TakmlModel takmlModel){
        return fileExtensionToMxPluginClassNames.get(takmlModel.getModelExtension());
    }

    /**
     * Displays the TAK ML Config UI Drop Down Receiver
     *
     * This UI shows all TAKML information on the ATAK device.
     *
     * @param callbackIntent - When the user presses the back button in the TAK ML Config UI, the callback intent is invoked.
     */
    public void showConfigUI(Intent callbackIntent){
        Log.d(TAG, "showConfigUIGeneralSettings");
        Intent intent = new Intent();
        intent.setAction(TakmlSettingsReceiver.SHOW_PLUGIN + uuid.toString());
        takmlSettingsReceiver.setCallbackIntent(callbackIntent);
        AtakBroadcast.getInstance().sendBroadcast(intent);
    }

    public HookEndpointState gethooksInfo(){
        return mobileManagementManager.getHooksInfo();
    }

    public TakmlServerClient createTakmlServerClient(String url) {
        return new TakmlServerClient(url, false);
    }

    public TakmlServerClient createTakmlServerClient(String url, String apiKeyName, String apiKey) {
        return new TakmlServerClient(url, false, apiKeyName, apiKey);
    }

    public TakmlServerClient createTakmlServerClient(String url, String optionalApiKeyName,
                                                     String optionalApiKey, byte[] clientStoreBytes,
                                                     byte[] trustStoreBytes, String clientStorePass,
                                                     String trustStorePass) {
        return new TakmlServerClient(url, optionalApiKeyName, optionalApiKey,
                clientStoreBytes, trustStoreBytes, clientStorePass, trustStorePass);
    }

    public Context getPluginContext() {
        return pluginOrActivityContext;
    }

    public TakmlServerClient getTakmlServerClient() {
        synchronized (takmlLock) {
            return takmlServerClient;
        }
    }

    /**
     * Returns a client for interacting with the remote TAK ML Server
     * <code>/model_management</code> REST endpoints.
     * <p>
     * The returned {@link ModelManagementApi} instance is obtained from the
     * underlying {@code takmlServerClient} and is assumed to already be
     * configured with the correct base URL, authentication, and timeouts
     * for the remote TAK ML Server.
     * </p>
     *
     * <p>Typical usage:</p>
     *
     * <pre>{@code
     * // List all models that are currently indexed by TAK FS / TAK ML Server
     * List<IndexRow> models = modelApi.getModels();
     *
     * // Get metadata (including additionalMetadata entries) for a specific model
     * String modelHash = "..."; // e.g. value returned from addModel(...) or from getModels()
     * IndexRow metadata = modelApi.getModelMetadata(modelHash);
     *
     * // Download the model binary associated with the given hash
     * byte[] modelBytes = modelApi.downloadModel(modelHash);
     *
     * // Add a new model wrapper
     * AddTakmlModelWrapperRequest addRequest = new AddTakmlModelWrapperRequest()
     *         .takmlModelWrapper(modelZipBytes)
     *         .requesterCallsign("MYCALLSIGN")
     *         .runOnServer(true);
     * modelApi.addModel(addRequest);
     *
     * // Edit/replace an existing model wrapper
     * EditTakmlModelWrapperRequest editRequest = new EditTakmlModelWrapperRequest()
     *         .takmlModelWrapper(updatedModelZipBytes)
     *         .requesterCallsign("MYCALLSIGN")
     *         .runOnServer(true);
     * modelApi.editModel(editRequest, modelHash);
     *
     * // Remove a model (and its associated metadata) by hash
     * modelApi.removeModel(modelHash);
     * }</pre>
     *
     * <p>
     * Callers are responsible for handling {@link com.bbn.takml_server.ApiException}
     * which is thrown if the remote server is unreachable or returns a non-2xx status.
     * </p>
     *
     * @return a pre-configured {@link ModelManagementApi} for calling remote
     *         <code>/model_management</code> endpoints on the TAK ML Server.
     */
    public ModelManagementApi getRemoteModelManagementApi(){
        return takmlServerClient.getModelManagementApi();
    }

    /**
     * Returns a client for interacting with the remote TAK ML Server
     * <code>/model_feedback</code> REST endpoints.
     * <p>
     * The returned {@link ModelFeedbackApi} instance is obtained from the
     * underlying {@code takmlServerClient} and is assumed to already be
     * configured with the correct base URL, authentication, and timeouts
     * for the remote TAK ML Server.
     * </p>
     *
     * <p>Typical usage:</p>
     *
     * <pre>{@code
     * // Add new feedback about a model's output
     * modelFeedbackApi.addModelFeedback(
     * "model-a", // modelName
     * 1.0,       // modelVersion
     * "CALLSIGN",// callsign
     * "Input text for the model", // inputText
     * null,      // inputFile (optional file input)
     * "Model's output", // output
     * true,      // isCorrect
     * null,      // outputErrorType
     * 5,         // evaluationConfidence
     * 5,         // evaluationRating
     * "Output was perfect", // comment
     * true       // validInput
     * );
     *
     * // Get all feedback entries for a specific model (and version)
     * List<FeedbackResponse> feedbackList = modelFeedbackApi.getFeedbackForModel("model-a", 1.0);
     * }</pre>
     *
     * <p>
     * Callers are responsible for handling {@link com.bbn.takml_server.ApiException}
     * which is thrown if the remote server is unreachable or returns a non-2xx status.
     * </p>
     *
     * @return a pre-configured {@link ModelFeedbackApi} for calling remote
     * <code>/model_feedback</code> endpoints on the TAK ML Server.
     */
    public ModelFeedbackApi getModelFeedbackApi(){
        return takmlServerClient.getModelFeedbackApi();
    }

    public TakmlServerConfiguration getTakmlServerConfiguration() {
        return takmlServerConfiguration;
    }

    public void shutdown(){
        for(TakmlExecutor takmlExecutor : takmlExecutors){
            takmlExecutor.shutdown();
        }
    }

}
