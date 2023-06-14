package com.atakmap.android.takml_android;

import android.content.Context;
import android.content.Intent;

import android.os.AsyncTask;
import android.util.Log;

import com.atakmap.android.dropdown.DropDownManager;
import com.atakmap.android.importexport.ImportExportMapComponent;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml_android.lib.TakmlInitializationException;
import com.atakmap.android.takml_android.storage.MissionPackageImportResolver;
import com.atakmap.android.takml_android.storage.TakmlModelStorage;
import com.atakmap.android.takml_android.ui.TakmlSettingsReceiver;
import com.atakmap.android.takml_android.util.IOUtils;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
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
import java.util.stream.Collectors;

public class Takml {
    private static final String TAG = Takml.class.getName();

    protected final Set<String> pluginNamesFoundOnClasspath = new HashSet<>();

    private static final String MX_PLUGIN_TOKEN_PREFIX = "mx_plugin_";
    private static final String MX_PLUGIN_TOKEN_SUFFIX = ".txt";
    private static final String TAKML_MODEL_CONFIG_EXTENSION = ".yaml";
    private final Context pluginContext;
    protected final List<TakmlModel> takmlModels = new ArrayList<>();

    protected final ConcurrentMap<String, Set<String>> fileExtensionToMxPluginClassNames = new ConcurrentHashMap<>();

    private TakmlSettingsReceiver takmlSettingsReceiver;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    private final AtomicBoolean finishedInitializing = new AtomicBoolean();
    private static final int INITIALIZATION_TIMEOUT_SECONDS = 10;

    private final UUID uuid = UUID.randomUUID();

    public Takml(Context pluginContext){
        this.pluginContext = pluginContext;

        discoverMxPlugins();
        if(pluginContext != null) {
            TakmlModelStorage.getInstance(this).initialize(countDownLatch);
            takmlSettingsReceiver = new TakmlSettingsReceiver(MapView.getMapView(), pluginContext,
                    this);
            AtakBroadcast.DocumentedIntentFilter ddFilter = new AtakBroadcast.DocumentedIntentFilter();
            ddFilter.addAction(TakmlSettingsReceiver.SHOW_PLUGIN + uuid);
            DropDownManager.getInstance().registerDropDownReceiver(takmlSettingsReceiver, ddFilter);
            ImportExportMapComponent.getInstance().addImporterClass(
                    new MissionPackageImportResolver(TAKML_MODEL_CONFIG_EXTENSION, null,
                            true, true, this));

            TakmlReceiver takmlReceiver = new TakmlReceiver(this);
            AtakBroadcast.DocumentedIntentFilter ddFilter2 = new AtakBroadcast.DocumentedIntentFilter();
            ddFilter2.addAction(TakmlReceiver.RECEIVE);
            ddFilter2.addAction(TakmlReceiver.IMPORT_TAKML_MODEL);
            AtakBroadcast.getInstance().registerReceiver(takmlReceiver, ddFilter2);
        }

        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            Intent intent = new Intent();
            intent.setAction(TakmlReceiver.RECEIVE);
            intent.putExtra(Constants.TAK_ML_UUID, uuid.toString());
            intent.putStringArrayListExtra(Constants.KNOWN_MX_PLUGINS, new ArrayList<>(pluginNamesFoundOnClasspath));
            AtakBroadcast.getInstance().sendBroadcast(intent);
        }, 5, 5, TimeUnit.SECONDS);
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
            return TakmlModelStorage.getInstance(this).importToDisk(takmlModel);
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
        if(takmlModel == null){
            throw new TakmlInitializationException("The TAK ML model specified is null, returning null");
        }

        if(takmlModel.getModelExtension() == null) {
            throw new TakmlInitializationException("The TAK ML model extension is null, returning null");
        }
        Set<String> mxPluginNames = fileExtensionToMxPluginClassNames.get(takmlModel.getModelExtension());
        if(mxPluginNames == null) {
            throw new TakmlInitializationException("Could not find an applicable mx plugin for model with extension '" +
                    "" + takmlModel.getModelExtension() + "', returning null");
        }
        String mxPluginName = mxPluginNames.iterator().next();
        if(mxPluginNames.size() > 1) {
            Log.w(TAG, "more than one mx plugin available, selecting first one");
        }
        MXPlugin mxPlugin;
        try {
            mxPlugin = constructMxPlugin(mxPluginName);
        }catch (Exception e){
            throw new TakmlInitializationException("Could not instantiate mx plugin: " + mxPluginName);
        }

        return new TakmlExecutor(this, pluginContext,
                    mxPlugin, takmlModel);
    }

    protected Set<String> getApplicablePluginClassNames(TakmlModel takmlModel){
        return fileExtensionToMxPluginClassNames.get(takmlModel.getModelExtension());
    }

    protected void discoverMxPlugins(){
        Log.d(TAG, "called instantiate plugins");
        // instantiate new plugins for model
        String[] assetFiles;
        try {
            assetFiles = pluginContext.getAssets().list("");
        } catch (IOException e) {
            Log.e(TAG, "Could not load assets", e);
            return;
        }

        for(String assetFile : assetFiles){
            Log.d(TAG, "instantiateMxPlugins: " + assetFile);
            if(!assetFile.startsWith(MX_PLUGIN_TOKEN_PREFIX)){
                continue;
            }
            String uuidStr = assetFile.replace(MX_PLUGIN_TOKEN_PREFIX, "");
            uuidStr = uuidStr.replace(MX_PLUGIN_TOKEN_SUFFIX, "");
            uuidStr = uuidStr.replaceAll("(.{8})(.{4})(.{4})(.{4})(.+)", "$1-$2-$3-$4-$5");
            try{
                UUID uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e){
                Log.w(TAG, "asset does not have a valid uuid, skipping", e);
                continue;
            }
            Log.d(TAG, "Found raw file: " + assetFile);

            InputStream bytes;
            try {
                bytes = pluginContext.getAssets().open(assetFile);
            } catch (IOException e) {
                Log.w(TAG, "instantiateMxPlugins: " + assetFile, e);
                continue;
            }
            String mxPluginClassName = new BufferedReader(
                    new InputStreamReader(bytes, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
            Log.d(TAG, "Found mx plugin: " + mxPluginClassName);
            pluginNamesFoundOnClasspath.add(mxPluginClassName);
            MXPlugin mxPlugin;
            try {
                mxPlugin = constructMxPlugin(mxPluginClassName);
            }catch (Exception e){
                Log.e(TAG, "Could not instantiate mx plugin: " + mxPluginClassName, e);
                continue;
            }
            if(mxPlugin.getApplicableModelExtensions() == null){
                Log.e(TAG, "Could not instantiate mx plugin: " + mxPluginClassName
                        + ", null applicable model extensions");
                continue;
            }
            for(String extension : mxPlugin.getApplicableModelExtensions()) {
                fileExtensionToMxPluginClassNames.computeIfAbsent(extension, k ->
                        new HashSet<>()).add(mxPluginClassName);
            }
        }
    }


    protected MXPlugin constructMxPlugin(String className) throws Exception{
        Class<? extends MXPlugin> mxPluginClass;
        try {
            mxPluginClass = Class.forName(className).asSubclass(MXPlugin.class);
        } catch (ClassNotFoundException e) {
            throw new TakmlInitializationException("class not found exception instantiateMxPluginViaReflection", e);
        } catch (ClassCastException e) {
            throw new TakmlInitializationException("class cast exception instantiateMxPluginViaReflection", e);
        }
        Log.d(TAG, "Constructing MXPlugin with class: " + mxPluginClass.getName());
        Constructor<? extends MXPlugin> constructor;
        try {
            constructor = mxPluginClass.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new TakmlInitializationException("Could not find constructor in class with name " + className,
                    e);
        } catch (SecurityException e) {
            throw new TakmlInitializationException("Security Exception with name " + className, e);
        }
        MXPlugin mxPlugin;
        try {
            mxPlugin = constructor.newInstance();
        } catch (InstantiationException e) {
            throw new TakmlInitializationException(
                    "Instantiation error, could not create instance from constructor in class " + className,
                    e);
        } catch (IllegalAccessException e) {
            throw new TakmlInitializationException(
                    "Illegal Access, could not create instance from constructor in class " + className, e);
        } catch (IllegalArgumentException e) {
            throw new TakmlInitializationException(
                    "Illegal Argument, could not create instance from constructor in class " + className,
                    e);
        } catch (InvocationTargetException e) {
            throw new TakmlInitializationException(
                    "Invocation Target error, could not create instance from constructor in class "
                            + className,
                    e);
        }
        return mxPlugin;
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
}
