package com.atakmap.android.takml_android.storage;

import static com.atakmap.android.takml_android.processing_params.compatibility_layer.PytorchProcessingConfigParser.parseImageRecognitionProcessingParams;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml_android.Constants;
import com.atakmap.android.takml_android.ModelTypeConstants;
import com.atakmap.android.takml_android.ProcessingParams;
import com.atakmap.android.takml_android.Takml;
import com.atakmap.android.takml_android.TakmlModel;
import com.atakmap.android.takml_android.processing_params.ImageRecognitionProcessingParams;
import com.atakmap.android.takml_android.tensor_processor.ImageRecognitionTensorProcessor;
import com.atakmap.android.takml_android.tensor_processor.TensorProcessor;
import com.atakmap.android.takml_android.ui.TakmlSettingsReceiver;
import com.atakmap.android.takml_android.util.IOUtils;
import com.atakmap.app.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TakmlModelStorage {
    private static final String TAG = TakmlModelStorage.class.getName();
    public static final String DATA_PACKAGE_DIR = Environment
            .getExternalStorageDirectory() + File.separator + "atak" + File.separator
            + "tools" + File.separator + "datapackage";

    private final File takmlStorageDir = new File(Constants.TAKML_MP_STORAGE_DIR);
    private final File takmlSettingsFile = new File(Constants.TAKML_SETTINGS_FILE);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean loadFromDisk = new AtomicBoolean(false);
    private boolean writeTakmlFolderLock;
    private final UUID uuid = UUID.randomUUID();
    private final Takml takml;
    private static TakmlModelStorage takmlModelStorage;
    private static final long TAKML_WRITE_STORAGE_TIMEOUT = 5000;

    // TAKML Model Parameters
    private static final String FRIENDLY_NAME_PARAM = "friendlyName";
    private static final String VERSION_PARAM = "version";
    private static final String MODEL_TYPE_PARAM = "modelType";
    private static final String MODEL_NAME_PARAM = "modelName";
    private static final String LABELS_NAME_PARAM = "labelsName";
    private static final String PROCESSING_CONFIG_PARAM = "processingConfig";

    // Remote TAKML Model Parameters
    private static final String KSERVE_URL = "url";
    private static final String KSERVE_API = "api";
    private static final String KSERVE_API_KEY_NAME = "apiKeyName";
    private static final String KSERVE_API_KEY = "apiKey";

    private final Set<TakmlModel> modelsOnDisk = new HashSet<>();

    private final Gson gson = new GsonBuilder().setLenient().create();
    private final Context pluginOrActivityContext;
    private static final String PLUGIN_CONTEXT_CLASS = "com.atak.plugins.impl.PluginContext";

    public TakmlModelStorage(Takml takml, Context pluginOrActivityContext){
        this.takml = takml;
        this.pluginOrActivityContext = pluginOrActivityContext;
    }

    public void initialize(CountDownLatch initializationCountdownLatch){
        if(!takmlStorageDir.exists()){
            if(takmlStorageDir.mkdirs()){
                Log.d(TAG, "Created takml mp storage directory");
            }else{
                Log.e(TAG, "Could not create takml mp storage directory");
            }
        }

        if(!loadFromDisk.get()) {
            AsyncTask.execute(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                }
                loadTakmlModelsOnDisk();
                loadFromDisk.set(true);
                initializationCountdownLatch.countDown();
            });
        }
    }

    private Context getMapViewOrActivityContext(){
        boolean isUsingPluginContext = pluginOrActivityContext.getClass().getName().equals(PLUGIN_CONTEXT_CLASS);
        if(isUsingPluginContext){
            return MapView.getMapView().getContext();
        }
        return pluginOrActivityContext;
    }

    private SettingsFile getAndMaybeCreateSettingsFile(){
        if(!takmlSettingsFile.exists()){
            SettingsFile settingsFile = new SettingsFile();
            try (Writer writer = new FileWriter(Constants.TAKML_SETTINGS_FILE)) {
                gson.toJson(settingsFile, writer);
                writer.flush();
            } catch (IOException e) {
                Log.e(TAG, "Could not create takml settings file", e);
            }
            return settingsFile;
        }
        String takmlSettings = null;
        try(FileReader fileReader = new FileReader(takmlSettingsFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader)) {
            StringBuilder stringBuilder = new StringBuilder();
            String line = bufferedReader.readLine();
            while (line != null) {
                stringBuilder.append(line).append("\n");
                line = bufferedReader.readLine();
            }
            takmlSettings = stringBuilder.toString();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException reading takml settings file", e);
        } catch (IOException e) {
            Log.e(TAG, "IO Exception reading takml settings file", e);
        }
        if(takmlSettings == null || takmlSettings.isEmpty()){
            Log.w(TAG, "Takml Settings File was empty, creating new one");
            return new SettingsFile();
        }
        return gson.fromJson(takmlSettings, SettingsFile.class);
    }

    private void writeSettingsFile(SettingsFile settingsFile){
        try (Writer writer = new FileWriter(Constants.TAKML_SETTINGS_FILE)) {
            gson.toJson(settingsFile, writer);
        } catch (IOException e) {
            Log.e(TAG, "Could not write to takml settings file", e);
        }
    }

    public boolean importToDisk(TakmlModel takmlModel){
        File takmlStorageDir = new File(Constants.TAKML_MP_STORAGE_DIR);

        String modelNameShortened = takmlModel.getName().toLowerCase().replaceAll("\\s", "_");

        File dir = new File(takmlStorageDir + File.separator + takmlModel.getName());
        if(dir.exists()){
            IOUtils.deleteDir(dir);
        }
        if(!dir.mkdirs()) {
            Log.w(TAG, "Could not create mp storage dir: " + dir.getPath());
            return false;
        }

        // create labels file
        File labelsFile = null;
        if(takmlModel.getLabels() != null) {
            labelsFile = new File(dir + File.separator + modelNameShortened + "_" + "labels");
            try (FileWriter fileWriter = new FileWriter(labelsFile);
                 PrintWriter printWriter = new PrintWriter(fileWriter)) {
                for (String label : takmlModel.getLabels()) {
                    printWriter.println(label);
                }
            } catch (IOException e) {
                Log.e(TAG, "IO Exception writing model labels to disk", e);
                return false;
            }
        }

        // model file
        File modelFile = new File(dir + File.separator + modelNameShortened + takmlModel.getModelExtension());
        try(FileOutputStream os = new FileOutputStream(modelFile)){
            try(FileInputStream is = new FileInputStream(new File(takmlModel.getModelUri().toString()))) {
                byte[] targetArray = new byte[is.available()];
                is.read(targetArray);
                os.write(targetArray);
            }
        } catch (IOException e) {
            Log.e(TAG, "IO Exception writing model labels to disk", e);
            return false;
        }

        // (optionally) create processing config file
        File processingConfig = null;
        if(takmlModel.getProcessingParams() != null) {
            String serializedProcessingParams = gson.toJson(takmlModel.getProcessingParams());
            if(serializedProcessingParams == null){
                Log.e(TAG, "Could not serialize processing params");
                return false;
            }

            processingConfig = new File(dir + File.separator + modelNameShortened + "_" + "processing_config" + ".txt");
            try (FileWriter fileWriter = new FileWriter(processingConfig)) {
                fileWriter.write(serializedProcessingParams);
            } catch (IOException e) {
                Log.e(TAG, "IO Exception writing processing params to disk", e);
                return false;
            }
        }

        // yaml file
        File yamlFile = new File(dir + File.separator + "takml_config.yaml");
        /*
            Example:

            friendlyName: Visdrone Pytorch
            modelType: OBJECT_DETECTION
            modelName: visdrone.torchscript
            labelsName: visdrone_labels.txt
            processingConfig: visdrone_input_processing_config.txt
         */
        try(FileWriter fileWriter = new FileWriter(yamlFile);
            PrintWriter printWriter = new PrintWriter(fileWriter)){
            printWriter.println("friendlyName: " + takmlModel.getName());
            printWriter.println("modelType: " + takmlModel.getModelType());
            printWriter.println("modelName: " + modelFile.getName());
            printWriter.println("version: " + takmlModel.getVersionNumber());
            if(labelsFile != null) {
                printWriter.println("labelsName: " + labelsFile.getName());
            }
            if(processingConfig != null){
                printWriter.println("processingConfig: " + processingConfig.getName());
            }
        } catch (IOException e) {
            Log.e(TAG, "IO Exception writing yaml file disk", e);
            return false;
        }

        Log.d(TAG, "Imported model \"" + takmlModel.getName() + "\" to disk successfully!");
        Log.d(TAG, "Model contents are written to: " + dir.getPath());

        modelsOnDisk.add(takmlModel);
        return true;
    }

    public void importModel(String takmlModelPath){
        File file = new File(takmlModelPath);

        Log.d(TAG, "beginImport " + file);
        File parentFile = file.getParentFile();
        if(parentFile == null){
            Log.e(TAG, "parent file is null for file: " + file);
            return;
        }

        /*
            Check if the mission package has already been copied to the device. This prevents
            the mission package contents from being copied more than once (e.g. in context of more
            than one TAK ML instance and MissionPackageImportResolver)
         */
        SettingsFile settingsFile = getAndMaybeCreateSettingsFile();
        Pair<UUID, Long> writeLock = settingsFile.getWriteToDiskLock();
        // if the write lock exists and uuid matches current uuid and not expired, then don't write to takml folder
        if (writeLock != null && !writeLock.first.equals(uuid) && System.currentTimeMillis() < writeLock.second) {
            Log.d(TAG, "Lock exists");
            writeTakmlFolderLock = false;
        } else {
            Log.d(TAG, "'Grabbing' Takml Sdcard write lock");
            settingsFile.setWriteToDiskLock(Pair.create(uuid, System.currentTimeMillis() + TAKML_WRITE_STORAGE_TIMEOUT));
            writeSettingsFile(settingsFile);
            writeTakmlFolderLock = true;
        }

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        AsyncTask.execute(() -> {
            // sleep small period of time to allow for ATAK to close it's zip input stream,
            // before trying to delete the zip file
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {
            }
            if(!writeTakmlFolderLock) {
                String zipFilePath = DATA_PACKAGE_DIR + File.separator + parentFile.getName() + ".zip";
                File zipFile = new File(zipFilePath);
                if (zipFile.delete()) {
                    Log.d(TAG, "beginImport, removed mission package with name: " + zipFilePath);
                } else {
                    Log.e(TAG, "beginImport, (likely already being imported by another TAKML instance) could not remove mission package with name: " + zipFilePath);
                }
            }
            countDownLatch.countDown();

            String friendlyName = readFilesAndImport(file, countDownLatch);
            if (friendlyName == null) {
                Log.e(TAG, "friendly name is null for file: " + file);
            } else {
                // Only copy if haven't done so already
                if (writeTakmlFolderLock) {
                    // copy mission package to mp storage directory
                    try {
                        File dir = new File(takmlStorageDir + File.separator + friendlyName);
                        if (dir.exists()) {
                            IOUtils.deleteDir(dir);
                        }
                        if (dir.mkdirs()) {
                            IOUtils.copyDirectory(parentFile, dir);
                        } else {
                            Log.w(TAG, "Could not create mp storage dir: " + dir.getPath());
                        }
                    } catch (IOException e) {
                        Log.d(TAG, "Could not copy file to mp dir", e);
                    }

                    // remove the lock
                    settingsFile.setWriteToDiskLock(null);
                    writeSettingsFile(settingsFile);


                    Log.d(TAG, "'Releasing' Takml Sdcard write lock");
                } else {
                    Log.d(TAG, "Already copied mission package files");
                }

                Intent intent2 = new Intent();
                intent2.setAction(TakmlSettingsReceiver.IMPORTED_TAKML_MODEL + takml.getUuid().toString());
                AtakBroadcast.getInstance().sendBroadcast(intent2);
            }
        });

    }

    private void loadTakmlModelsOnDisk(){
        File[] missionPackages = takmlStorageDir.listFiles();
        if(missionPackages != null){
            for(File outerFile : missionPackages){
                Log.d(TAG, "loadTakmlModelsOnDisk: " + outerFile);
                if(outerFile.getName().endsWith(Constants.TAKML_CONFIG_EXTENSION)){
                    readFilesAndImport(outerFile, null);
                    continue;
                }

                File[] files = outerFile.listFiles();
                if(files != null) {
                    File takmlConfigFile = null;
                    for(File file : files) {
                        if(file.getName().endsWith(Constants.TAKML_CONFIG_EXTENSION)){
                            Log.d(TAG, "found takml_config file: " + file.getName());
                            takmlConfigFile = file;
                            break;
                        }
                    }
                    readFilesAndImport(takmlConfigFile, null);
                }
            }
        }
    }

    private String readFilesAndImport(File yamlConfigFile, CountDownLatch countDownLatch){
        if(yamlConfigFile == null){
            Log.w(TAG, "could not read yaml config file");
            return null;
        }

        String friendlyName = null;
        String version = null;
        String modelType = null;
        String modelName = null;
        String labelsName = null;
        String processingConfigName = null;

        String kserveUrl = null;
        String kserveApi = null;
        String kserveApiKeyName = null;
        String kserveApiKey = null;
        try(Scanner scanner = new Scanner(yamlConfigFile)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if(line.startsWith(FRIENDLY_NAME_PARAM)){
                    friendlyName = line.replace(FRIENDLY_NAME_PARAM + ":", "").
                            replaceFirst("\\s", "");
                    Log.d(TAG, "beginImport, found " + FRIENDLY_NAME_PARAM +": " + friendlyName);
                }else if(line.startsWith(VERSION_PARAM)){
                    version = line.replace(VERSION_PARAM + ":", "").
                            replaceFirst("\\s", "");
                    Log.d(TAG, "beginImport, found " + VERSION_PARAM +": " + version);
                }else if(line.startsWith(MODEL_TYPE_PARAM)){
                    modelType = line.replace(MODEL_TYPE_PARAM + ":", "").
                            replaceFirst("\\s", "");
                    Log.d(TAG, "beginImport, found " + MODEL_TYPE_PARAM +": " + modelType);
                }else if(line.startsWith(MODEL_NAME_PARAM)){
                    modelName = line.replace(MODEL_NAME_PARAM + ":", "").
                            replaceFirst("\\s", "");
                    Log.d(TAG, "beginImport, found " + MODEL_NAME_PARAM +": " + modelName);
                }else if(line.startsWith(LABELS_NAME_PARAM)){
                    labelsName = line.replace(LABELS_NAME_PARAM + ":", "").
                            replaceFirst("\\s", "");
                    Log.d(TAG, "beginImport, found " + LABELS_NAME_PARAM +": " + labelsName);
                }else if(line.startsWith(PROCESSING_CONFIG_PARAM)) {
                    processingConfigName = line.replace(PROCESSING_CONFIG_PARAM + ":", "").
                            replaceFirst("\\s", "");
                    Log.d(TAG, "beginImport, found " + PROCESSING_CONFIG_PARAM +": " + processingConfigName);

                    /// Remote Model Configs
                }else if(line.startsWith(KSERVE_URL)) {
                    kserveUrl = line.replace(KSERVE_URL + ":", "").
                            replaceFirst("\\s", "");
                    Log.d(TAG, "beginImport, found " + KSERVE_URL +": " + kserveUrl);

                }else if(line.startsWith(KSERVE_API_KEY_NAME)) {
                    kserveApiKeyName = line.replace(KSERVE_API_KEY_NAME + ":", "").
                            replaceFirst("\\s", "");
                    Log.d(TAG, "beginImport, found " + KSERVE_API_KEY_NAME +": " + kserveApiKey);
                }else if(line.startsWith(KSERVE_API_KEY)) {
                    kserveApiKey = line.replace(KSERVE_API_KEY + ":", "").
                            replaceFirst("\\s", "");
                    Log.d(TAG, "beginImport, found " + KSERVE_API_KEY +": " + kserveApiKey);
                }else if(line.startsWith(KSERVE_API)) {
                    kserveApi = line.replace(KSERVE_API + ":", "").
                            replaceFirst("\\s", "");
                    Log.d(TAG, "beginImport, found " + KSERVE_API +": " + kserveApi);
                }
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "beginImport, could not read takml_config.yaml", e);
            return null;
        }

        importTakmlModel(yamlConfigFile.getParentFile(), friendlyName, modelType, modelName,
                labelsName, processingConfigName, kserveUrl == null,
                kserveUrl == null && modelName == null, kserveUrl, kserveApi, kserveApiKeyName,
                kserveApiKey, version, countDownLatch);

        return friendlyName;
    }

    private void importTakmlModel(File takmlWrapperFolder, String friendlyName, String modelTypeStr, String modelName,
                               String labelsName, String processingConfigName, boolean isLocalModel,
                                  boolean isPseudoModel, String url, String api, String apiKeyName,
                                  String apiKey, String version, CountDownLatch countDownLatch){
        // friendlyName
        if(friendlyName == null){
            Log.w(TAG, "friendlyName was not specified, using model file as name: " + modelName);
            friendlyName = modelName;
        }
        String finalFriendlyName = friendlyName;

        double versionNumber = 1;
        if(version != null && !version.isEmpty()) {
            try {
                versionNumber = Double.parseDouble(version);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Could not parse version number, defaulting to 1 for takml model: " + modelName, e);
            }
        }

        // modelType
        if(modelTypeStr == null){
            String warning = "'modelType' was null, not importing TAK ML model: " + friendlyName;
            Log.w(TAG, warning);
            handler.post(() -> Toast.makeText(getMapViewOrActivityContext(), warning,
                    Toast.LENGTH_LONG).show());
            return;
        }

        // modelName
        if(modelName == null && isLocalModel && !isPseudoModel){
            String warning = "'modelName' was null, not importing TAK ML model: " + friendlyName;
            Log.w(TAG, warning);
            handler.post(() -> Toast.makeText(getMapViewOrActivityContext(), warning,
                    Toast.LENGTH_LONG).show());
            return;
        }
        // modelExtension
        String modelExtension = null;
        if(isLocalModel && !isPseudoModel) {
            int index = modelName.lastIndexOf(".");
            if (index == -1) {
                Log.w(TAG, "Could not find an extension for file with name: " + modelName);
                return;
            }
            modelExtension = modelName.substring(index);
            Log.d(TAG, "importTakml: using model extension: " + modelExtension);
        }

        File modelFile = null;
        if (isLocalModel && !isPseudoModel) {
            modelFile = new File(takmlWrapperFolder + File.separator + modelName);
            Log.d(TAG, "importTakml: trying to import model: " + modelFile.getPath());
        }

        // labelsName
        List<String> labels = null;
        if(labelsName != null) {
            File labelsFile = new File(takmlWrapperFolder + File.separator + labelsName);
            labels = readLines(labelsFile);
        }

        TakmlModel takmlModel = null;
        ProcessingParams processingParams = null;
        if (processingConfigName != null) {
            File processingConfigFile = new File(takmlWrapperFolder + File.separator + processingConfigName);

            String processingConfigStr = null;
            try(FileReader fileReader = new FileReader(processingConfigFile);
                BufferedReader bufferedReader = new BufferedReader(fileReader)) {
                StringBuilder stringBuilder = new StringBuilder();
                String line = bufferedReader.readLine();
                while (line != null) {
                    stringBuilder.append(line).append("\n");
                    line = bufferedReader.readLine();
                }
                processingConfigStr = stringBuilder.toString();
            } catch (FileNotFoundException e) {
                Log.e(TAG, "File not found while reading processing config", e);
                return;
            } catch (IOException e) {
                Log.e(TAG, "IO exception while reading processing config", e);
                return;
            }

            JSONObject obj;
            try {
                obj = new JSONObject(processingConfigStr);
            } catch (JSONException e) {
                Log.e(TAG, "JSON exception parsing processing config", e);
                return;
            }
            String type;
            try {
                type = obj.getString("type");
            } catch (JSONException e) {
                type = ImageRecognitionProcessingParams.class.getName();
                Log.e(TAG, "JSON exception parsing processing config 'type', using default: " + type, e);
            }
            // the below is deprecated, replace with generic version
            boolean pytorchCompatibility = false;
            if(type.equals("com.atakmap.android.takml_android.pytorch_mx_plugin.PytorchObjectDetectionParams")){
                type = ImageRecognitionProcessingParams.class.getName();
                pytorchCompatibility = true;
            }

            Class<?> test;
            try {
                test = Class.forName(type);

                Class<? extends ProcessingParams> processingParamsClass = test.asSubclass(ProcessingParams.class);
                Log.d(TAG, "importTakml: " + processingConfigStr);
                Log.d(TAG, "importTakml: " + processingParamsClass);

                if(pytorchCompatibility){
                    processingParams = parseImageRecognitionProcessingParams(obj);
                }else {
                    processingParams = gson.fromJson(processingConfigStr,
                            processingParamsClass);
                }
            } catch (ClassNotFoundException e) {
                // This can happen if one TAK ML instance from on ATAK plugin tries importing
                // a class defined extending ImageRecognitionProcessingParams.java that is not defined in that plugin.
                // For example Plugin A has TFLite Processing Config, Plugin B does not. Plugin B
                // would import this TAK ML model without the processing config. This is a limitation
                // with the current implementation. Given that Plugin B is configured to operate without
                // TF Lite in this example, this scenario is acceptable. As such Plugin B would simply
                // have some knowledge of the TAK ML Model.
                Log.w(TAG, "Class not found exception reading type of processing config, " +
                        "importing without processing config", e);
                return;
            } catch (JSONException e) {
                Log.e(TAG, "JSONException parsing processing config: " + processingConfigStr, e);
            }
        }

        if (isLocalModel) {
            if(isPseudoModel){
                TakmlModel.TakmlPsuedoModelBuilder builder = new TakmlModel.TakmlPsuedoModelBuilder(friendlyName);
                takmlModel = builder.build();
            }else{
                Log.d(TAG, "Generating content URI for " + BuildConfig.APPLICATION_ID);

                // TODO if sd card use this:
                Uri contentURI;

                try {
                    boolean isUsingPluginContext = pluginOrActivityContext.getClass().getName().equals(PLUGIN_CONTEXT_CLASS);

                    if (isUsingPluginContext) {
                        contentURI = FileProvider.getUriForFile(
                                MapView.getMapView().getContext(),
                                BuildConfig.APPLICATION_ID + ".provider",
                                modelFile
                        );
                    } else {
                        contentURI = FileProvider.getUriForFile(
                                pluginOrActivityContext,
                                pluginOrActivityContext.getPackageName() + ".provider",
                                modelFile
                        );
                    }

                    Log.d(TAG, "Using FileProvider URI: " + contentURI);
                } catch (IllegalArgumentException | NullPointerException e) {
                    // Fallback for files on external/SD card or if FileProvider fails
                    Log.w(TAG, "FileProvider failed, falling back to Uri.fromFile: " + e.getMessage());
                    contentURI = Uri.fromFile(modelFile);
                }

                TakmlModel.TakmlModelBuilder builder = new TakmlModel.TakmlModelBuilder(friendlyName, contentURI, modelExtension, modelTypeStr);

                builder.setVersionNumber(versionNumber);

                if (labels != null) {
                    builder.setLabels(labels);
                }

                if (processingParams != null) {
                    builder.setProcessingParams(processingParams);
                }
                takmlModel = builder.build();
            }
        } else { // is remote
            // TODO support remote psuedo models

            TensorProcessor tensorProcessor = null;
            if (modelTypeStr.equals(ModelTypeConstants.IMAGE_CLASSIFICATION) || modelTypeStr.equals(ModelTypeConstants.OBJECT_DETECTION)) {
                if(processingParams == null){
                    Log.e(TAG, "Remote TAKML Model config is missing required processing params (e.g. ImageRecognitionProcessingParams)");
                    return;
                }
                ImageRecognitionProcessingParams params = (ImageRecognitionProcessingParams) processingParams;
                tensorProcessor = new ImageRecognitionTensorProcessor(labels, params);
            } else{
                // TODO: support other default processors and a default one
            }
            TakmlModel.TakmlRemoteModelBuilder builder = new TakmlModel.TakmlRemoteModelBuilder(friendlyName,
                    modelTypeStr, tensorProcessor, url, api);
            if(apiKeyName != null && apiKey != null){
                builder.setApiKey(apiKeyName, apiKey);
            }
            takmlModel = builder.build();
        }

        if(countDownLatch != null) {
            AsyncTask.execute(() -> {
                try {
                    if (countDownLatch.await(10, TimeUnit.SECONDS)) {
                        handler.post(() -> Toast.makeText(getMapViewOrActivityContext(), "Imported TAK ML Model '"
                                + finalFriendlyName + "' successfully!", Toast.LENGTH_LONG).show());
                    } else {
                        Log.e(TAG, "Could not import takml model");
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "Could not import takml model", e);
                }
            });
        }

        Log.d(TAG, "Imported takml model: " + friendlyName);
        modelsOnDisk.add(takmlModel);
        takml.addTakmlModel(takmlModel);
    }

    public Set<TakmlModel> getModelsOnDisk(){
        return modelsOnDisk;
    }

    private List<String> readLines(File file){
        List<String> ret = new ArrayList<>();
        try(Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String nextLine = scanner.nextLine();
                Log.d(TAG, "readLines: " + nextLine);
                ret.add(nextLine);
            }
        }catch (FileNotFoundException e) {
            Log.e(TAG, "could not read file: " + file, e);
        }
        return ret;
    }
}
