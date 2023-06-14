package com.atakmap.android.takml_android.storage;

import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml_android.Constants;
import com.atakmap.android.takml_android.ProcessingParams;
import com.atakmap.android.takml_android.Takml;
import com.atakmap.android.takml_android.TakmlModel;
import com.atakmap.android.takml_android.util.IOUtils;
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
    private static final String MODEL_TYPE_PARAM = "modelType";
    private static final String MODEL_NAME_PARAM = "modelName";
    private static final String LABELS_NAME_PARAM = "labelsName";
    private static final String PROCESSING_CONFIG_PARAM = "processingConfig";

    private final Set<TakmlModel> modelsOnDisk = new HashSet<>();

    public static TakmlModelStorage getInstance(Takml takml){
        if(takmlModelStorage == null){
            takmlModelStorage = new TakmlModelStorage(takml);
        }
        return takmlModelStorage;
    }

    private TakmlModelStorage(Takml takml){
        this.takml = takml;
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
                loadMissionPackagesOnDisk();
                loadFromDisk.set(true);
                initializationCountdownLatch.countDown();
            });
        }
    }

    private SettingsFile getAndMaybeCreateSettingsFile(){
        if(!takmlSettingsFile.exists()){
            try (Writer writer = new FileWriter(Constants.TAKML_SETTINGS_FILE)) {
                Gson gson = new GsonBuilder().create();
                gson.toJson(new SettingsFile(), writer);
            } catch (IOException e) {
                Log.e(TAG, "Could not create takml settings file", e);
            }
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
        return new Gson().fromJson(takmlSettings, SettingsFile.class);
    }

    private void writeSettingsFile(SettingsFile settingsFile){
        try (Writer writer = new FileWriter(Constants.TAKML_SETTINGS_FILE)) {
            Gson gson = new GsonBuilder().create();
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
            os.write(takmlModel.getModelBytes());
        } catch (IOException e) {
            Log.e(TAG, "IO Exception writing model labels to disk", e);
            return false;
        }

        // (optionally) create processing config file
        File processingConfig = null;
        if(takmlModel.getProcessingParams() != null) {
            String serializedProcessingParams = new Gson().toJson(takmlModel.getProcessingParams());
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
            }
        });

    }

    private void loadMissionPackagesOnDisk(){
        File[] missionPackages = takmlStorageDir.listFiles();
        if(missionPackages != null){
            for(File missionPackageFolder : missionPackages){
                Log.d(TAG, "loadMissionPackagesOnDisk: " + missionPackageFolder);
                File[] files = missionPackageFolder.listFiles();
                if(files != null) {
                    File takmlConfigFile = null;
                    for(File file : files) {
                        if(file.getName().equals(Constants.TAKML_CONFIG_FILE)){
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
        String modelType = null;
        String modelName = null;
        String labelsName = null;
        String processingConfigName = null;
        try(Scanner scanner = new Scanner(yamlConfigFile)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if(line.startsWith(FRIENDLY_NAME_PARAM)){
                    friendlyName = line.replace(FRIENDLY_NAME_PARAM + ":", "").
                            replaceFirst("\\s", "");
                    Log.d(TAG, "beginImport, found " + FRIENDLY_NAME_PARAM +": " + friendlyName);
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
                }
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "beginImport, could not read takml_config.yaml", e);
            return null;
        }

        importTakml(yamlConfigFile.getParentFile(),friendlyName,modelType, modelName, labelsName,
                processingConfigName, countDownLatch);

        return friendlyName;
    }

    private void importTakml(File path, String friendlyName, String modelTypeStr, String modelName,
                             String labelsName, String processingConfigName,
                             CountDownLatch countDownLatch){
        // friendlyName
        if(friendlyName == null){
            Log.w(TAG, "friendlyName was not specified, using model file as name: " + modelName);
            friendlyName = modelName;
        }
        String finalFriendlyName = friendlyName;

        // modelType
        if(modelTypeStr == null){
            String warning = "'modelType' was null, not importing TAK ML model: " + friendlyName;
            Log.w(TAG, warning);
            handler.post(() -> Toast.makeText(MapView.getMapView().getContext(), warning,
                    Toast.LENGTH_LONG).show());
            return;
        }

        // modelName
        if(modelName == null){
            String warning = "'modelName' was null, not importing TAK ML model: " + friendlyName;
            Log.w(TAG, warning);
            handler.post(() -> Toast.makeText(MapView.getMapView().getContext(), warning,
                    Toast.LENGTH_LONG).show());
            return;
        }
        // modelExtension
        int index = modelName.lastIndexOf(".");
        if(index == -1){
            Log.w(TAG, "Could not find an extension for file with name: " + modelName);
            return;
        }
        String modelExtension = modelName.substring(index);
        Log.d(TAG, "importTakml: using model extension: " + modelExtension);

        File modelFile = new File(path + File.separator + modelName);
        Log.d(TAG, "importTakml: trying to import model: " + modelFile.getPath());
        byte[] modelBytes = readBytes(modelFile);

        // labelsName
        List<String> labels = null;
        if(labelsName != null) {
            File labelsFile = new File(path + File.separator + labelsName);
            labels = readLines(labelsFile);
        }

        TakmlModel takmlModel;
        ProcessingParams processingParams = null;
        if(processingConfigName != null) {
            File processingConfigFile = new File(path + File.separator + processingConfigName);

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
                Log.e(TAG, "JSON exception parsing processing config type", e);
                return;
            }

            Class<?> test;
            try {
                test = Class.forName(type);

                Class<? extends ProcessingParams> processingParamsClass = test.asSubclass(ProcessingParams.class);
                Gson gson = new GsonBuilder()
                        .setLenient()
                        .create();
                Log.d(TAG, "importTakml: " + processingConfigStr);
                Log.d(TAG, "importTakml: " + processingParamsClass);

                processingParams = gson.fromJson(processingConfigStr,
                        processingParamsClass);
            } catch (ClassNotFoundException e) {
                // This can happen if one TAK ML instance from on ATAK plugin tries importing
                // a class defined extending ProcessingParams.java that is not defined in that plugin.
                // For example Plugin A has TFLite Processing Config, Plugin B does not. Plugin B
                // would import this TAK ML model without the processing config. This is a limitation
                // with the current implementation. Given that Plugin B is configured to operate without
                // TF Lite in this example, this scenario is acceptable. As such Plugin B would simply
                // have some knowledge of the TAK ML Model.
                Log.w(TAG, "Class not found exception reading type of processing config, " +
                        "importing without processing config", e);
                return;
            }
        }

        if(processingParams != null){
            if(labels != null) {
                takmlModel = new TakmlModel.TakmlModelBuilder(friendlyName, modelBytes, modelExtension,
                        modelTypeStr)
                        .setLabels(labels)
                        .setProcessingParams(processingParams)
                        .build();
            }else{
                takmlModel = new TakmlModel.TakmlModelBuilder(friendlyName, modelBytes, modelExtension,
                        modelTypeStr)
                        .setProcessingParams(processingParams)
                        .build();
            }
        }else{
            if(labels != null) {
                takmlModel = new TakmlModel.TakmlModelBuilder(friendlyName, modelBytes, modelExtension,
                        modelTypeStr)
                        .setLabels(labels)
                        .build();
            }else{
                takmlModel = new TakmlModel.TakmlModelBuilder(friendlyName, modelBytes, modelExtension,
                        modelTypeStr)
                        .build();
            }
        }

        if(countDownLatch != null) {
            AsyncTask.execute(() -> {
                try {
                    if (countDownLatch.await(10, TimeUnit.SECONDS)) {
                        handler.post(() -> Toast.makeText(MapView.getMapView().getContext(), "Imported TAK ML Model '"
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

    private byte[] readBytes(File file){
        byte[] bytes = new byte[(int) file.length()];
        try(FileInputStream fis = new FileInputStream(file)) {
            fis.read(bytes);
        } catch (IOException e) {
            Log.e(TAG, "could not read file: " + file, e);
        }
        return bytes;
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
