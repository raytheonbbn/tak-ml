package com.bbn.takml_server.model_management;

import com.bbn.takml_server.takml_model.ProcessingParams;
import com.bbn.takml_server.takml_model.TakmlModel;
import com.bbn.takml_server.takml_model.TakmlModel.TakmlModelBuilder;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.bbn.takml_server.model_execution.ModelsLoaderUtil.*;
import static com.bbn.takml_server.takml_model.MetadataConstants.*;

public class TakmlModelParser {
    private static final Logger logger = LogManager.getLogger(TakmlModelParser.class);

    public static TakmlModel parseTakmlModelFromZip(byte[] takmlModelZip) {
        File tempDir = null;
        try {
            // 1. Extract to temporary directory
            tempDir = Files.createTempDirectory("takml_model_").toFile();
            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(takmlModelZip))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String entryName = entry.getName();
                    logger.info("zip entry found: {}", entryName);
                    if (entry.isDirectory()) continue;
                    String[] parts = entryName.split("/", 2);
                    if (parts.length < 2) continue;
                    File newFile = new File(tempDir, parts[1]);
                    new File(newFile.getParent()).mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
            }

            // 2. Parse model
            File configFile = new File(tempDir, "takml_config.yaml");

            TakmlModel takmlModel =  readFilesAndImport(configFile);
            if (takmlModel == null) {
                logger.error("Failed to load TakmlModel from zip");
                return null;
            }

            return takmlModel;
        } catch (IOException e) {
            logger.error("Failed to parse and re-zip TakmlModel", e);
            return null;
        }
    }

    private static TakmlModel readFilesAndImport(File yamlConfigFile) {
        if (yamlConfigFile == null) {
            logger.warn("could not read yaml config file");
            return null;
        }
        String friendlyName = null;
        String modelType = null;
        String modelName = null;
        String labelsName = null;
        String processingConfigName = null;
        String kserveUrl = null;
        String kserveApi = null;
        String kserveApiKeyName = null;
        String kserveApiKey = null;

        double version = 1.0;

        try (Scanner scanner = new Scanner(yamlConfigFile)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith(FRIENDLY_NAME_PARAM)) {
                    friendlyName = line.replace(FRIENDLY_NAME_PARAM + ":", "").trim();
                    logger.debug("found {}: {}", FRIENDLY_NAME_PARAM, friendlyName);
                } else if (line.startsWith(MODEL_TYPE_PARAM)) {
                    modelType = line.replace(MODEL_TYPE_PARAM + ":", "").trim();
                    logger.debug("found {}: {}", MODEL_TYPE_PARAM, modelType);
                } else if (line.startsWith(MODEL_NAME_PARAM)) {
                    modelName = line.replace(MODEL_NAME_PARAM + ":", "").trim();
                    logger.debug("found {}: {}", MODEL_NAME_PARAM, modelName);
                }  else if (line.startsWith(VERSION_PARAM)) {
                    String versionStr = line.replace(VERSION_PARAM + ":", "").trim();
                    logger.debug("found {}: {}", VERSION_PARAM, versionStr);
                    try {
                        version = Double.parseDouble(versionStr);
                    } catch (NumberFormatException e){
                        logger.error("NumberFormatException parsing version for takml model: {}", modelName, e);
                    }
                } else if (line.startsWith(LABELS_NAME_PARAM)) {
                    labelsName = line.replace(LABELS_NAME_PARAM + ":", "").trim();
                    logger.debug("found {}: {}", LABELS_NAME_PARAM, labelsName);
                } else if (line.startsWith(PROCESSING_CONFIG_PARAM)) {
                    processingConfigName = line.replace(PROCESSING_CONFIG_PARAM + ":", "").trim();
                    logger.debug("found {}: {}", PROCESSING_CONFIG_PARAM, processingConfigName);
                } else if (line.startsWith(KSERVE_URL)) {
                    kserveUrl = line.replace(KSERVE_URL + ":", "").trim();
                    logger.debug("found {}: {}", KSERVE_URL, kserveUrl);
                } else if (line.startsWith(KSERVE_API_KEY_NAME)) {
                    kserveApiKeyName = line.replace(KSERVE_API_KEY_NAME + ":", "").trim();
                    logger.debug("found {}: {}", KSERVE_API_KEY_NAME, kserveApiKeyName);
                } else if (line.startsWith(KSERVE_API_KEY)) {
                    kserveApiKey = line.replace(KSERVE_API_KEY + ":", "").trim();
                    logger.debug("found {}: {}", KSERVE_API_KEY, kserveApiKey);
                } else if (line.startsWith(KSERVE_API)) {
                    kserveApi = line.replace(KSERVE_API + ":", "").trim();
                    logger.debug("found {}: {}", KSERVE_API, kserveApi);
                }
            }
        } catch (FileNotFoundException e) {
            logger.error("could not read takml_config.yaml", e);
            return null;
        }

        return importTakmlModel(yamlConfigFile.getParentFile(), version, friendlyName, modelType, modelName,
                labelsName, processingConfigName, kserveUrl == null,
                kserveUrl == null && modelName == null, kserveUrl, kserveApi, kserveApiKeyName,
                kserveApiKey);
    }

    private static TakmlModel importTakmlModel(File path, double version, String friendlyName, String modelTypeStr, String modelName,
                                               String labelsName, String processingConfigName, boolean isLocalModel,
                                               boolean isPseudoModel, String url, String api, String apiKeyName,
                                               String apiKey) {
        if (friendlyName == null) {
            logger.warn("friendlyName was not specified, using model file as name: {}", modelName);
            friendlyName = modelName;
        }
        if (modelTypeStr == null) {
            logger.warn("'modelType' was null, not importing TAK ML model: {}", friendlyName);
            return null;
        }
        if (modelName == null && isLocalModel && !isPseudoModel) {
            logger.warn("'modelName' was null, not importing TAK ML model: {}", friendlyName);
            return null;
        }

        String modelExtension = null;
        if (!isPseudoModel) {
            int index = modelName.lastIndexOf(".");
            if (index == -1) {
                logger.warn("Could not find an extension for file with name: {}", modelName);
                return null;
            }
            modelExtension = modelName.substring(index);
            logger.debug("using model extension: {}", modelExtension);
        }

        File modelFile = null;
        if (!isPseudoModel) {
            modelFile = new File(path + File.separator + modelName);
            logger.debug("trying to import model: {}", modelFile.getPath());
        }

        List<String> labels = null;
        if (labelsName != null) {
            File labelsFile = new File(path + File.separator + labelsName);
            labels = readLines(labelsFile);
        }

        TakmlModel takmlModel;
        String processingParams = null;

        if (processingConfigName != null) {
            File processingConfigFile = new File(path + File.separator + processingConfigName);
            StringBuilder stringBuilder = new StringBuilder();
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(processingConfigFile))) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
            } catch (IOException e) {
                logger.error("Error reading processing config", e);
                return null;
            }
            processingParams = stringBuilder.toString();
            /*try {
                JSONObject obj = new JSONObject(stringBuilder.toString());
                String type = obj.getString("type");
                Class<?> test = Class.forName(type);
                Class<? extends ProcessingParams> clazz = test.asSubclass(ProcessingParams.class);
                processingParams = new Gson().fromJson(stringBuilder.toString(), clazz);
            } catch (Exception e) {
                logger.warn("Failed to parse processing config, importing without it", e);
            }*/
        }

        if (isLocalModel) {
            if (isPseudoModel) {
                logger.error("Psuedo models are not supported, only on the client side");
                return null;
            } else {
                TakmlModelBuilder builder = new TakmlModelBuilder(friendlyName, modelFile, modelExtension, modelTypeStr);
                if (labels != null) builder.setLabels(labels);
                if (processingParams != null) builder.setProcessingParams(processingParams);
                builder.setVersionNumber(version);
                return builder.build();
            }
        } else{
            TakmlModel.TakmlRemoteModelBuilder builder = new TakmlModel.TakmlRemoteModelBuilder(friendlyName,
                    modelTypeStr, url, api);
            if(apiKeyName != null && apiKey != null){
                builder.setApiKey(apiKeyName, apiKey);
            }
            return builder.build();
        }
    }

    private static List<String> readLines(File file) {
        List<String> lines = new ArrayList<>();
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                logger.trace("readLines: {}", line);
                lines.add(line);
            }
        } catch (FileNotFoundException e) {
            logger.error("could not read file: {}", file, e);
        }
        return lines;
    }

    public static void extractToDirectory(TakmlModel takmlModel, File outputDir) {
        if (takmlModel == null || outputDir == null) {
            logger.error("TakmlModel or output directory is null");
            return;
        }

        if (!outputDir.exists() && !outputDir.mkdirs()) {
            logger.error("Failed to create output directory: {}", outputDir.getAbsolutePath());
            return;
        }

        // Write takml_config.yaml
        File configFile = new File(outputDir, "takml_config.yaml");
        try (PrintWriter writer = new PrintWriter(configFile)) {
            writer.println("friendlyName: " + takmlModel.getName());
            writer.println("modelType: " + takmlModel.getModelType());

            if (takmlModel.getUrl() != null) writer.println("url: " + takmlModel.getUrl());
            if (takmlModel.getApi() != null) writer.println("api: " + takmlModel.getApi());
            if (takmlModel.getApiKeyName() != null && takmlModel.getApiKey() != null) {
                writer.println("apiKeyName: " + takmlModel.getApiKeyName());
                writer.println("apiKey: " + takmlModel.getApiKey());
            }
            File modelFile = takmlModel.getModelFile();
            if (modelFile != null) {
                writer.println("modelName: " + modelFile.getName());
                // Copy model file
                Files.copy(modelFile.toPath(), new File(outputDir, modelFile.getName()).toPath());
            }

            if (takmlModel.getLabels() != null && !takmlModel.getLabels().isEmpty()) {
                writer.println("labelsName: labels.txt");
                File labelsFile = new File(outputDir, "labels.txt");
                try (PrintWriter labelsWriter = new PrintWriter(labelsFile)) {
                    for (String label : takmlModel.getLabels()) {
                        labelsWriter.println(label);
                    }
                }
            }

            if (takmlModel.getProcessingParams() != null) {
                writer.println("processingConfig: processing.json");
                File procFile = new File(outputDir, "processing.json");
                String json = new Gson().toJson(takmlModel.getProcessingParams());
                Files.writeString(procFile.toPath(), json);
            }
        } catch (IOException e) {
            logger.error("Failed to extract TakmlModel to directory", e);
        }
    }

}
