package com.bbn.takml_server.model_execution;

import com.bbn.takml_server.lib.TakmlInitializationException;
import com.bbn.takml_server.model_execution.api.model.InferInput;
import com.bbn.takml_server.model_execution.api.model.InferOutput;
import com.bbn.takml_server.model_execution.api.model.ModelTensor;
import com.bbn.takml_server.takml_model.TakmlModel;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.bbn.takml_server.model_execution.ModelsLoaderUtil.initializeMxPlugin;

@Service
public class ModelExecutionService{
    protected static final Logger logger = LogManager.getLogger(ModelExecutionService.class);

    public static final String TAKML_CONFIG_FILE = "takml_config.yaml";

    @Value("${mx_plugins}")
    protected List<String> mxPluginClassNames;

    @Value("${models_directory}")
    protected String modelsDirectory;

    protected final ConcurrentMap<String, MXPlugin> modelFriendlyNameToMxPlugin = new ConcurrentHashMap<>();
    protected final Set<TakmlModel> takmlModels = new HashSet<>();
    protected final ConcurrentMap<String, MXPlugin> extensionToMxPlugin = new ConcurrentHashMap<>();
    protected final ConcurrentMap<String, String> takmlModelNameToWrapperFolderName = new ConcurrentHashMap<>();
    protected final ConcurrentMap<String, ModelsLoaderUtil.ModelImportInfo> takmlModelNameToImportInfo = new ConcurrentHashMap<>();

    @PostConstruct
    public void initialize() throws TakmlInitializationException {
        // 1. Instantiate Mx Plugins for each Model
        for (String mxPluginClassName : mxPluginClassNames){
            MXPlugin mxPlugin = initializeMxPlugin(mxPluginClassName);
            for(String extension : mxPlugin.getApplicableModelExtensions()){
                extensionToMxPlugin.put(extension, mxPlugin);
            }
        }

        // 2. Read the Takml Models and instantiate Mx Plugins for each model
        if (modelsDirectory == null){
            throw new TakmlInitializationException("Property 'models_directory' is missing from config");
        }
        File modelsDirFile = new File(modelsDirectory);
        if (!modelsDirFile.exists() && !modelsDirFile.isDirectory()){
            throw new TakmlInitializationException("Property 'models_directory' is not a valid directory");
        }
        File[] files = modelsDirFile.listFiles();
        if (files == null){
            throw new TakmlInitializationException("No Takml models found in models directory");
        }
        for (File takmlModelFolder : files){
            File takmlConfig = new File(takmlModelFolder.getPath(), TAKML_CONFIG_FILE);
            if(!takmlConfig.exists()){
                logger.warn("Takml Config does not exist for takml model in directory: {}", takmlModelFolder.getPath());
                continue;
            }

            ModelsLoaderUtil.ModelImportInfo modelImportInfo = ModelsLoaderUtil.readFilesAndImport(takmlConfig, extensionToMxPlugin,
                    modelFriendlyNameToMxPlugin, takmlModels);
            if(modelImportInfo != null){
                takmlModelNameToWrapperFolderName.put(modelImportInfo.name, takmlModelFolder.getPath());
                takmlModelNameToImportInfo.put(modelImportInfo.name, modelImportInfo);
                logger.info("Successfully loaded model: {}", modelImportInfo.name);
            }
        }
    }

    public Pair<long[], long[]> importTakmlModel(TakmlModel takmlModel, File modelDir){
        synchronized (takmlModels) {
            takmlModels.add(takmlModel);
        }
        MXPlugin mxPluginTemp = extensionToMxPlugin.get(takmlModel.getModelExtension());
        if(mxPluginTemp == null){
            logger.error("Could not find applicable Mx plugin for model: {}", takmlModel.getName());
            return null;
        }
        MXPlugin mxPlugin;
        try{
            mxPlugin = initializeMxPlugin(mxPluginTemp.getClass().getName());
        } catch (TakmlInitializationException e){
            logger.error("Could not instantiate Mx plugin");
            return null;
        }
        Pair<long[], long[]> modelTensorShapes = null;
        try {
            modelTensorShapes = mxPlugin.instantiate(takmlModel);
        } catch (TakmlInitializationException e) {
            logger.error("Could not instantiate Mx plugin with model: {}", takmlModel.getName());
            return null;
        }

        modelFriendlyNameToMxPlugin.putIfAbsent(takmlModel.getName(), mxPlugin);
        takmlModelNameToWrapperFolderName.put(takmlModel.getName(), modelDir.getPath());

        logger.info("Imported takml model: {}", takmlModel.getName());
        return modelTensorShapes;
    }

    public Pair<List<InferOutput>, HttpStatus> runInference(String modelName, InferInput inferInput) {
        MXPlugin mxPlugin = modelFriendlyNameToMxPlugin.get(modelName);
        if(mxPlugin == null){
            logger.error("Could not find Mx Plugin for model {}", modelName);
            return Pair.of(null, HttpStatus.NOT_FOUND);
        }
        CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicReference<List<InferOutput>> tensorResultsRef =  new AtomicReference<>();
        mxPlugin.execute(inferInput, (tensorResults, success, modelType) -> {
            if(success) {
                tensorResultsRef.set(tensorResults);
            }else{
                logger.error("Error running infer input: {}", inferInput);
            }
            countDownLatch.countDown();
        });
        try{
            if(!countDownLatch.await(10, TimeUnit.SECONDS)){
                logger.error("Timed out waiting for request with infer input: {}", inferInput);
                return Pair.of(null, HttpStatus.BAD_REQUEST);
            }
        } catch (InterruptedException e) {
            logger.error("InterruptedException waiting for request with infer input: {}", inferInput, e);
            return Pair.of(null, HttpStatus.BAD_REQUEST);
        }
        return Pair.of(tensorResultsRef.get(), HttpStatus.OK);
    }

    public Set<TakmlModel> getTakmlModels(){
        return takmlModels;
    }

    public void removeModel(String modelName) {
        synchronized (takmlModels) {
            for (Iterator<TakmlModel> it = takmlModels.iterator(); it.hasNext(); ) {
                TakmlModel takmlModel = it.next();
                if(takmlModel.getName().equals(modelName)) {
                    takmlModels.remove(takmlModel);
                    break;
                }
            }
        }
        modelFriendlyNameToMxPlugin.remove(modelName);

        takmlModelNameToImportInfo.remove(modelName);
        String takmlWrapperFolder = takmlModelNameToWrapperFolderName.remove(modelName);
        if(takmlWrapperFolder == null){
            logger.error("Could not find takml wrapper folder for model: {}", modelName);
            return;
        }
        File dir = new File(takmlWrapperFolder);
        try {
            FileUtils.deleteDirectory(dir);
        } catch (IOException e) {
            logger.error("IOException deleting file: {}", dir, e);
        }
    }
}
