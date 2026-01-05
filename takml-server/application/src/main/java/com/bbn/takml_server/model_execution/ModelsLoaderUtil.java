package com.bbn.takml_server.model_execution;

import com.bbn.takml_server.lib.TakmlInitializationException;
import com.bbn.takml_server.model_execution.api.model.ModelTensor;
import com.bbn.takml_server.takml_model.ProcessingParams;
import com.bbn.takml_server.takml_model.TakmlModel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import static org.apache.commons.io.FileUtils.readLines;

public class ModelsLoaderUtil {
    private static final Logger logger = LogManager.getLogger(ModelsLoaderUtil.class);

    private static final Gson gson = new GsonBuilder().setLenient().create();

    // TAKML Model Parameters
    public static final String FRIENDLY_NAME_PARAM = "friendlyName";
    public static final String VERSION_PARAM = "version";
    public static final String MODEL_TYPE_PARAM = "modelType";
    public static final String MODEL_NAME_PARAM = "modelName";
    public static final String LABELS_NAME_PARAM = "labelsName";
    public static final String PROCESSING_CONFIG_PARAM = "processingConfig";

    public static class ModelImportInfo{
        public String name;
        public long[] inputTensorShape;
        public long[] outputTensorShape;
    }

    public static ModelImportInfo readFilesAndImport(File yamlConfigFile, Map<String, MXPlugin> extensionToMxPlugin,
                                            ConcurrentMap<String, MXPlugin> modelFriendlyNameToMxPlugin,
                                            Set<TakmlModel> takmlModels){
        String friendlyName = null;
        String version = null;
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
                    logger.info("beginImport, found " + FRIENDLY_NAME_PARAM +": " + friendlyName);
                }else if(line.startsWith(MODEL_TYPE_PARAM)){
                    modelType = line.replace(MODEL_TYPE_PARAM + ":", "").
                            replaceFirst("\\s", "");
                    logger.info("beginImport, found " + MODEL_TYPE_PARAM +": " + modelType);
                }else if(line.startsWith(VERSION_PARAM)){
                    version = line.replace(VERSION_PARAM + ":", "").
                            replaceFirst("\\s", "");
                    logger.info("beginImport, found " + VERSION_PARAM +": " + modelType);
                }else if(line.startsWith(MODEL_NAME_PARAM)){
                    modelName = line.replace(MODEL_NAME_PARAM + ":", "").
                            replaceFirst("\\s", "");
                    logger.info("beginImport, found " + MODEL_NAME_PARAM +": " + modelName);
                }else if(line.startsWith(LABELS_NAME_PARAM)){
                    labelsName = line.replace(LABELS_NAME_PARAM + ":", "").
                            replaceFirst("\\s", "");
                    logger.info("beginImport, found " + LABELS_NAME_PARAM +": " + labelsName);
                }else if(line.startsWith(PROCESSING_CONFIG_PARAM)) {
                    processingConfigName = line.replace(PROCESSING_CONFIG_PARAM + ":", "").
                            replaceFirst("\\s", "");
                    logger.info("beginImport, found " + PROCESSING_CONFIG_PARAM +": " + processingConfigName);
                }
            }
        } catch (FileNotFoundException e) {
            logger.error("beginImport, could not read takml_config.yaml", e);
            return null;
        }

        Pair<long[], long[]> modelTensors = importTakml(yamlConfigFile.getParentFile(),friendlyName,modelType, modelName, labelsName,
                processingConfigName, extensionToMxPlugin, modelFriendlyNameToMxPlugin, takmlModels, version);

        if(modelTensors != null) {
            ModelImportInfo modelImportInfo = new ModelImportInfo();
            modelImportInfo.name = friendlyName;
            modelImportInfo.inputTensorShape = modelTensors.getLeft();
            modelImportInfo.outputTensorShape = modelTensors.getRight();

            return modelImportInfo;
        }

        return null;
    }

    private static Pair<long[], long[]> importTakml(File path, String friendlyName, String modelTypeStr, String modelName,
                                    String labelsName, String processingConfigName, Map<String, MXPlugin> extensionToMxPlugin,
                                    ConcurrentMap<String, MXPlugin> modelFriendlyNameToMxPlugin,
                                    Set<TakmlModel> takmlModels, String version){
        // friendlyName
        if(friendlyName == null){
            logger.warn("friendlyName was not specified, using model file as name: " + modelName);
            friendlyName = modelName;
        }

        // modelType
        if(modelTypeStr == null){
            String warning = "'modelType' was null, not importing TAK ML model: " + friendlyName;
            logger.warn(warning);
            return null;
        }

        // modelName
        if(modelName == null){
            String warning = "'modelName' was null, not importing TAK ML model: " + friendlyName;
            logger.warn(warning);
            return null;
        }
        // modelExtension
        int index = modelName.lastIndexOf(".");
        if(index == -1){
            logger.warn("Could not find an extension for file with name: " + modelName);
            return null;
        }
        String modelExtension = modelName.substring(index);
        logger.info("importTakml: using model extension: " + modelExtension);

        File modelFile = new File(path + File.separator + modelName);
        logger.info("importTakml: trying to import model: {}", modelFile.getPath());

        // labelsName
        List<String> labels = null;
        if(labelsName != null) {
            File labelsFile = new File(path + File.separator + labelsName);
            try {
                labels = readLines(labelsFile);
            } catch (IOException e){
                logger.error("IOException reading labels file: {}", labelsFile.getPath(), e);
            }
        }

        TakmlModel takmlModel;
        String processingParams = null;
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
                logger.error("File not found while reading processing config", e);
                return null;
            } catch (IOException e) {
                logger.error("IO exception while reading processing config", e);
                return null;
            }

            JSONObject obj;
            try {
                obj = new JSONObject(processingConfigStr);
            } catch (JSONException e) {
                logger.error("JSON exception parsing processing config", e);
                return null;
            }
            String type;
            try {
                type = obj.getString("type");
            } catch (JSONException e) {
                logger.error("JSON exception parsing processing config type", e);
                return null;
            }

            Class<?> test;
            try {
                test = Class.forName(type);

                Class<? extends ProcessingParams> processingParamsClass = test.asSubclass(ProcessingParams.class);
                logger.error("importTakml: " + processingConfigStr);
                logger.error("importTakml: " + processingParamsClass);

                processingParams = processingConfigStr;
            } catch (ClassNotFoundException e) {
                // This can happen if one TAK ML instance from on ATAK plugin tries importing
                // a class defined extending ProcessingParams.java that is not defined in that plugin.
                // For example Plugin A has TFLite Processing Config, Plugin B does not. Plugin B
                // would import this TAK ML model without the processing config. This is a limitation
                // with the current implementation. Given that Plugin B is configured to operate without
                // TF Lite in this example, this scenario is acceptable. As such Plugin B would simply
                // have some knowledge of the TAK ML Model.
                logger.error("Class not found exception reading type of processing config, " +
                        "importing without processing config", e);
                return null;
            }
        }

        double versionNumber = 1;
        if(version != null && !version.isEmpty()) {
            try {
                versionNumber = Double.parseDouble(version);
            } catch (NumberFormatException e) {
                logger.error("Could not parse version number, defaulting to 1 for takml model: {}", modelName, e);
            }
        }

        if(processingParams != null){
            if(labels != null) {
                takmlModel = new TakmlModel.TakmlModelBuilder(friendlyName, modelFile, modelExtension,
                        modelTypeStr)
                        .setLabels(labels)
                        .setProcessingParams(processingParams)
                        .setVersionNumber(versionNumber)
                        .build();
            }else{
                takmlModel = new TakmlModel.TakmlModelBuilder(friendlyName, modelFile, modelExtension,
                        modelTypeStr)
                        .setProcessingParams(processingParams)
                        .setVersionNumber(versionNumber)
                        .build();
            }
        }else{
            if(labels != null) {
                takmlModel = new TakmlModel.TakmlModelBuilder(friendlyName, modelFile, modelExtension,
                        modelTypeStr)
                        .setLabels(labels)
                        .setVersionNumber(versionNumber)
                        .build();
            }else{
                takmlModel = new TakmlModel.TakmlModelBuilder(friendlyName, modelFile, modelExtension,
                        modelTypeStr)
                        .setVersionNumber(versionNumber)
                        .build();
            }
        }

        takmlModels.add(takmlModel);

        logger.info("Imported takml model: " + friendlyName);
        MXPlugin mxPluginTemp = extensionToMxPlugin.get(modelExtension);
        if(mxPluginTemp == null){
            logger.error("Could not find applicable Mx plugin for model: {}", friendlyName);
            return null;
        }
        MXPlugin mxPlugin;
        try{
            mxPlugin = initializeMxPlugin(mxPluginTemp.getClass().getName());
        } catch (TakmlInitializationException e){
            logger.error("Could not instantiate Mx plugin");
            return null;
        }
        Pair<long[], long[]> ret = null;
        try {
            ret = mxPlugin.instantiate(takmlModel);
        } catch (TakmlInitializationException e) {
            logger.error("Could not instantiate Mx plugin with model: {}", friendlyName);
            return null;
        }

        modelFriendlyNameToMxPlugin.putIfAbsent(friendlyName, mxPlugin);
        return ret;
    }

    public static MXPlugin initializeMxPlugin(String mxPluginClassName) throws TakmlInitializationException {
        Class<? extends MXPlugin> mxPluginClass;
        try {
            mxPluginClass = Class.forName(mxPluginClassName).asSubclass(MXPlugin.class);
        } catch (ClassNotFoundException e) {
            throw new TakmlInitializationException("Could not find class " + mxPluginClassName, e);
        } catch (ClassCastException e) {
            throw new TakmlInitializationException("Class cast exception for class " + mxPluginClassName, e);
        }
        logger.info("Constructing MxPlugin with class '{}'", mxPluginClass.getName());
        Constructor<? extends MXPlugin> constructor;
        try {
            constructor = mxPluginClass.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new TakmlInitializationException("Could not find constructor in class with name " + mxPluginClassName,
                    e);
        } catch (SecurityException e) {
            throw new TakmlInitializationException("Security Exception with name " + mxPluginClassName, e);
        }
        MXPlugin mxPlugin;
        try {
            mxPlugin = constructor.newInstance();
        } catch (InstantiationException e) {
            throw new TakmlInitializationException(
                    "Instantiation error, could not create instance from constructor in class " + mxPluginClassName,
                    e);
        } catch (IllegalAccessException e) {
            throw new TakmlInitializationException(
                    "Illegal Access, could not create instance from constructor in class " + mxPluginClassName, e);
        } catch (IllegalArgumentException e) {
            throw new TakmlInitializationException(
                    "Illegal Argument, could not create instance from constructor in class " + mxPluginClassName,
                    e);
        } catch (InvocationTargetException e) {
            throw new TakmlInitializationException(
                    "Invocation Target error, could not create instance from constructor in class "
                            + mxPluginClassName,
                    e);
        }
        return mxPlugin;
    }
}
