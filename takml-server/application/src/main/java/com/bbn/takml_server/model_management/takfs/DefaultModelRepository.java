package com.bbn.takml_server.model_management.takfs;

import com.bbn.tak.comms.helper.ClientUtil;
import com.bbn.tak_sync_file_manager.FakeFileClientImpl;
import com.bbn.tak_sync_file_manager.HashUtils;
import com.bbn.tak_sync_file_manager.TakFileManagerServer;
import com.bbn.tak_sync_file_manager.model.FileInfo;
import com.bbn.tak_sync_file_manager.model.IndexFile;
import com.bbn.tak_sync_file_manager.model.IndexRow;
import com.bbn.takml_server.ZipUtil;
import com.bbn.takml_server.db.access_token.AccessToken;
import com.bbn.takml_server.db.access_token.AccessTokenRepository;
import com.bbn.takml_server.feedback.InputType;
import com.bbn.takml_server.feedback.model.ModelFeedback;
import com.bbn.takml_server.db.feedback.ModelFeedbackRepository;
import com.bbn.takml_server.lib.TakmlInitializationException;
import com.bbn.takml_server.model_execution.ModelExecutionService;
import com.bbn.takml_server.model_management.TakmlModelParser;
import com.bbn.takml_server.feedback.api.AddFeedbackRequest;
import com.bbn.takml_server.feedback.api.FeedbackResponse;
import com.bbn.takml_server.takml_model.TakmlModel;
import com.google.gson.Gson;
import okhttp3.MediaType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.InetAddress;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.bbn.takml_server.takml_model.MetadataConstants.*;

@Repository
public class DefaultModelRepository implements ModelRepository {
    private static final Logger logger = LogManager.getLogger(DefaultModelRepository.class);
    protected TakFileManagerServer takFileManagerServer;
    private static final String MODELS_PARENT_CATEGORY = "Models";
    private static final String OCTET_STREAM_MEDIA_TYPE = "application/octet-stream";
    private static final int CONCURRENT_SEGMENTED_FILES_DOWNLOAD_LIMIT = 10;
    private static final int TIMEOUT_SEGMENTED_FILES_MILLIS = 120000; // 120 seconds
    private static final int TIMEOUT_FILE_UPLOAD_SECONDS = 120; // 2 minutes
    private static final int TIMEOUT_FILE_DOWNLOAD_SECONDS = 120; // 2 minutes

    private static final int SEGMENTED_UPLOAD_SIZE_THRESHOLD = 9000000;

    private final AccessTokenRepository accessTokenRepository;
    private final ModelFeedbackRepository modelFeedbackRepository;

    @Value("${tak.addr}")
    private String takIpAddr;

    @Value("${tak.port}")
    private Integer takPort;

    @Value("${tak.client_store}")
    private String takClientStorePath;

    @Value("${tak.client_store.password}")
    private String takClientStorePassword;

    @Value("${tak.trust_store}")
    private String takTrustStorePath;

    @Value("${tak.trust_store.password}")
    private String takTrustStorePassword;

    @Value("${takfs.mission_name}")
    private String takfsMissionName;

    @Value("${takfs.takserver.file_limit_size.mb}")
    private Long takserverEnterpriseSyncLimitSize;

    @Value("${use.test.takfs:false}")
    private Boolean useTestTakfs;

    @Value("${takfs.segmented_file_upload_timeout_millis:10000}")
    private Long segmentedFileUploadTimeout;

    @Value("${models_directory}")
    private String modelsDirectory;

    @Value("${server.address:}")
    private String serverAddress;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${server.ssl.enabled:false}")
    private boolean isSslEnabled;

    @Autowired
    private ModelExecutionService modelExecutionService;

    public DefaultModelRepository(AccessTokenRepository accessTokenRepository, ModelFeedbackRepository modelFeedbackRepository) {
        this.accessTokenRepository = accessTokenRepository;
        this.modelFeedbackRepository = modelFeedbackRepository;
    }

    @PostConstruct
    public void initialize() throws TakmlInitializationException {
        if(takIpAddr == null){
            throw new TakmlInitializationException("Missing takIpAddr property");
        }
        logger.info("Loading property tak.addr = {}", takIpAddr);
        if(takPort == null){
            throw new TakmlInitializationException("Missing takPort property");
        }
        logger.info("Loading property tak.port = {}", takPort);
        if(takClientStorePath == null){
            throw new TakmlInitializationException("Missing takClientStorePath property");
        }
        logger.info("Loading property tak.client_store = {}", takClientStorePath);
        if(takClientStorePassword == null){
            throw new TakmlInitializationException("Missing takClientStorePassword property");
        }
        logger.info("Loading property tak.client_store.password = {}", takClientStorePassword);
        if(takTrustStorePath == null){
            throw new TakmlInitializationException("Missing takTrustStorePath property");
        }
        logger.info("Loading property tak.trust_store = {}", takTrustStorePath);
        if(takTrustStorePassword == null){
            throw new TakmlInitializationException("Missing takTrustStorePassword property");
        }
        logger.info("Loading property tak.trust_store.password = {}", takTrustStorePassword);
        if(takfsMissionName == null){
            throw new TakmlInitializationException("Missing takfsMissionName property");
        }
        logger.info("Loading property takfs.mission_name = {}", takfsMissionName);
        if(takserverEnterpriseSyncLimitSize == null){
            throw new TakmlInitializationException("Missing takserverEnterpriseSyncLimitSize property");
        }
        logger.info("Loading property takfs.takserver.file_limit_size.mb = {}", takserverEnterpriseSyncLimitSize);

        byte[] takClientStore;
        try (FileInputStream is = new FileInputStream(takClientStorePath)) {
            takClientStore = IOUtils.toByteArray(is);
        } catch (IOException e) {
            throw new TakmlInitializationException("Could not load tak client store bytes", e);
        }

        byte[] takTrustStore;
        try (FileInputStream is = new FileInputStream(takTrustStorePath)) {
            takTrustStore = IOUtils.toByteArray(is);
        } catch (IOException e) {
            throw new TakmlInitializationException("Could not load tak trust store bytes", e);
        }

        Optional<AccessToken> accessTokenObj = accessTokenRepository.findById(AccessToken.ACCESS_TOKEN_ID);
        String accessToken = null;
        if (accessTokenObj.isPresent()) {
            accessToken = accessTokenObj.get().getTokenId();
            logger.info("Using access token from database: {}", accessToken);
        }

        logger.info("Loading property takfs.segmented_file_upload_timeout_millis = {}", segmentedFileUploadTimeout);

        if (useTestTakfs) {
            takFileManagerServer = new TakFileManagerServer(new FakeFileClientImpl());
        } else {
            takFileManagerServer = new TakFileManagerServer(takIpAddr, takPort, takClientStore, takTrustStore,
                    ClientUtil.CertType.JKS, takClientStorePassword, takTrustStorePassword, accessToken,
                    takfsMissionName, segmentedFileUploadTimeout, segmentedFileUploadTimeout,
                    segmentedFileUploadTimeout,
                    s -> {
                        logger.info("Storing access token: {}", s);
                        accessTokenRepository.save(new AccessToken(s));
                    });
        }

        // 2. Read the Takml Models and add to TAK FS
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

        Set<String> hashesInTakFs = new HashSet<>();
        IndexFile indexFile = null;
        // sleep for small period until index file has been created
        while(indexFile == null){
            indexFile = takFileManagerServer.downloadIndexFile();
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
        if(indexFile.getCategoryToRows() != null) {
            Set<IndexRow> indexRows = indexFile.getCategoryToRows().get(MODELS_PARENT_CATEGORY);
            if (indexRows != null) {
                for (IndexRow indexRow : indexRows) {
                    hashesInTakFs.add(indexRow.getHash());
                }
            }
        }

        for (File takmlModelFolder : files){
            if(!takmlModelFolder.isDirectory()){
                continue;
            }
            logger.info("Reading folder: {}", takmlModelFolder);
            byte[] takmlWrapperZip;
            try {
                takmlWrapperZip = ZipUtil.zipDirectoryToByteArray(takmlModelFolder);
            } catch (IOException e) {
                logger.error("IOException reading takml model: {}", takmlModelFolder, e);
                continue;
            }
            String hash = HashUtils.sha256sum(new ByteArrayInputStream(takmlWrapperZip));
            logger.info("Hash for {} is: {}", takmlModelFolder.getName(), hash);
            if(!hashesInTakFs.contains(hash)) {
                saveModelWrapper(takmlWrapperZip, "TAK ML Server", true, true,
                        null).join();
            }
        }
    }

    private CompletableFuture<String> saveModelWrapper(byte[] takmlModelWrapper, String requesterCallsign,
                                                       boolean runOnServer, boolean skipCopyingTakmlFolder,
                                                       List<String> optionalSupportedDevices){
        ///  Extract takml model wrapper
        TakmlModel takmlModel = TakmlModelParser.parseTakmlModelFromZip(takmlModelWrapper);
        if(takmlModel == null){
            return CompletableFuture.completedFuture(null);
        }

        Map<String, String> additionalMetadata = new HashMap<>();
        additionalMetadata.put(CALLSIGN_META, requesterCallsign);
        if(optionalSupportedDevices != null){
            additionalMetadata.put(SUPPORTED_DEVICES_META, String.join(", ", optionalSupportedDevices));
        }
        additionalMetadata.put(MODEL_TYPE_META, takmlModel.getModelType());
        additionalMetadata.put(MODEL_EXTENSION_META, takmlModel.getModelExtension());
        if (takmlModel.getLabels() != null) {
            additionalMetadata.put(MODEL_LABELS_META, new Gson().toJson(takmlModel.getLabels()));
        }
        if (takmlModel.getProcessingParams() != null) {
            additionalMetadata.put(PROCESSING_PARAMS_META, takmlModel.getProcessingParams());
        }
        additionalMetadata.put(VERSION_META, String.valueOf(takmlModel.getVersionNumber()));

        if(runOnServer) {
            additionalMetadata.put(RUN_ON_SERVER_META, String.valueOf(runOnServer));
            if (takmlModel.getApi() != null) {
                additionalMetadata.put(KSERVE_API, takmlModel.getApi());
            }
            // this is a remote model potentially
            if (takmlModel.getUrl() != null) {
                additionalMetadata.put(KSERVE_URL, takmlModel.getUrl());
            }else{ // implies model runs on takml server, use server ip and port
                String url = getSelfUrl();
                additionalMetadata.put(KSERVE_URL, url);
            }
        }

        if(runOnServer && !skipCopyingTakmlFolder){
            ///  extract to models directory folder
            File modelDir = new File(modelsDirectory, takmlModel.getName());
            if (!modelDir.exists() && !modelDir.mkdirs()) {
                logger.warn("Failed to create model directory: {}", modelDir.getAbsolutePath());
            }

            TakmlModelParser.extractToDirectory(takmlModel, modelDir);
            logger.info("Model extracted to: {}", modelDir);

            Pair<long[], long[]> tensorShapes = modelExecutionService.importTakmlModel(takmlModel, modelDir);
            if(tensorShapes != null){
                additionalMetadata.put(TENSOR_INPUT_SHAPE_META, Arrays.toString(tensorShapes.getLeft()));
                additionalMetadata.put(TENSOR_OUTPUT_SHAPE_META, Arrays.toString(tensorShapes.getRight()));
            }
        }

        CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicReference<String> hash = new AtomicReference<>();

        long modelSizeMb = getLengthOfByteArrayMb(takmlModelWrapper);
        additionalMetadata.put(MODEL_SIZE_META_MB, String.valueOf(modelSizeMb));

        if (modelSizeMb < (takserverEnterpriseSyncLimitSize / 2)) {
            takFileManagerServer.saveFile(takmlModel.getName(), takmlModelWrapper, MediaType.get(OCTET_STREAM_MEDIA_TYPE),
                    MODELS_PARENT_CATEGORY, additionalMetadata, requesterCallsign, (b, fileInfo) -> {
                        hash.set(fileInfo.getHash());
                        countDownLatch.countDown();
                    });
        } else {
            File file;
            try {
                file = File.createTempFile("temp", null);
                file.deleteOnExit();
            } catch (IOException e) {
                logger.error("[SegmentedFile] Failed to create temp file for model '{}': {}", takmlModel.getName(), e.getMessage());
                return CompletableFuture.failedFuture(e);
            }
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(takmlModelWrapper);
            } catch (IOException e) {
                logger.error("IOException writing temp file for model '{}': {}", takmlModel.getName(), e.getMessage());
                return CompletableFuture.failedFuture(e);
            }
            takFileManagerServer.uploadSegmentedFile(takmlModel.getName(), file.getPath(), requesterCallsign, MODELS_PARENT_CATEGORY,
                    MediaType.get(OCTET_STREAM_MEDIA_TYPE), additionalMetadata, SEGMENTED_UPLOAD_SIZE_THRESHOLD,
                    (b, fileInfo) -> {
                        hash.set(fileInfo.getHash());
                        countDownLatch.countDown();
                    }, TIMEOUT_SEGMENTED_FILES_MILLIS);
        }

        return handle(countDownLatch, hash);
    }

    @Override
    public CompletableFuture<String> saveModelWrapper(byte[] takmlModelWrapper, String requesterCallsign,
                                                      boolean runOnServer, List<String> supportedDevices) {
        return saveModelWrapper(takmlModelWrapper, requesterCallsign, runOnServer, false,
                supportedDevices);
    }

    @Override
    public CompletableFuture<String> editModelWrapper(String modelHash, byte[] takmlModelWrapper, String requesterCallsign,
                                                      boolean runOnServer, List<String> optionalSupportedDevices) {
        ///  Extract takml model wrapper
        TakmlModel takmlModel = TakmlModelParser.parseTakmlModelFromZip(takmlModelWrapper);
        if(takmlModel == null){
            return CompletableFuture.completedFuture(null);
        }

        Map<String, String> additionalMetadata = new HashMap<>();
        additionalMetadata.put(CALLSIGN_META, requesterCallsign);
        if(optionalSupportedDevices != null){
            additionalMetadata.put(SUPPORTED_DEVICES_META, String.join(", ", optionalSupportedDevices));
        }
        additionalMetadata.put(MODEL_TYPE_META, takmlModel.getModelType());
        additionalMetadata.put(MODEL_EXTENSION_META, takmlModel.getModelExtension());
        if (takmlModel.getLabels() != null) {
            additionalMetadata.put(MODEL_LABELS_META, new Gson().toJson(takmlModel.getLabels()));
        }
        if (takmlModel.getProcessingParams() != null) {
            additionalMetadata.put(PROCESSING_PARAMS_META, takmlModel.getProcessingParams());
        }
        if(runOnServer) {
            additionalMetadata.put(RUN_ON_SERVER_META, String.valueOf(runOnServer));
            if (takmlModel.getApi() != null) {
                additionalMetadata.put(KSERVE_API, takmlModel.getApi());
            }
            // this is a remote remote model potentially
            if (takmlModel.getUrl() != null) {
                additionalMetadata.put(KSERVE_URL, takmlModel.getUrl());
            }else{ // implies model runs on takml server, use server ip and port
                String url = getSelfUrl();
                additionalMetadata.put(KSERVE_URL, url);
            }
        }

        IndexFile indexFile = takFileManagerServer.downloadIndexFile();
        Map<String, Set<IndexRow>> categoryToRows = indexFile.getCategoryToRows();
        String existingHash = null;
        if(categoryToRows != null){
            Set<IndexRow> indexRows = categoryToRows.get(MODELS_PARENT_CATEGORY);
            if(indexRows != null){
                for(IndexRow indexRow : indexRows){
                    if(indexRow.getHash().equals(modelHash)){
                        existingHash = indexRow.getHash();
                        break;
                    }
                }
            }
        }
        additionalMetadata.put(VERSION_META, String.valueOf(takmlModel.getVersionNumber()));

        if(runOnServer){
            modelExecutionService.removeModel(takmlModel.getName());

            ///  extract to models directory folder
            File modelDir = new File(modelsDirectory, takmlModel.getName());
            if (!modelDir.exists() && !modelDir.mkdirs()) {
                logger.warn("Failed to create model directory: {}", modelDir.getAbsolutePath());
            }

            TakmlModelParser.extractToDirectory(takmlModel, modelDir);
            logger.info("Model extracted to: {}", modelDir);

            Pair<long[], long[]> tensorShapes = modelExecutionService.importTakmlModel(takmlModel, modelDir);
            if(tensorShapes != null){
                additionalMetadata.put(TENSOR_INPUT_SHAPE_META, Arrays.toString(tensorShapes.getLeft()));
                additionalMetadata.put(TENSOR_OUTPUT_SHAPE_META, Arrays.toString(tensorShapes.getRight()));
            }
        }

        CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicReference<String> hash = new AtomicReference<>();

        long modelSizeMb = getLengthOfByteArrayMb(takmlModelWrapper);
        additionalMetadata.put(MODEL_SIZE_META_MB, String.valueOf(modelSizeMb));

        if (modelSizeMb < (takserverEnterpriseSyncLimitSize / 2)) {
            takFileManagerServer.editFile(existingHash, takmlModel.getName(), takmlModelWrapper,
                    MediaType.get(OCTET_STREAM_MEDIA_TYPE), MODELS_PARENT_CATEGORY, additionalMetadata,
                    requesterCallsign, (b, fileInfo) -> {
                        hash.set(fileInfo.getHash());
                        countDownLatch.countDown();
                    });
        } else {
            // remove existing file
            removeModel(modelHash).join();

            File file;
            try {
                file = File.createTempFile("temp", null);
                file.deleteOnExit();
            } catch (IOException e) {
                logger.error("IOException creating temporary file for segmented file writer", e);
                return CompletableFuture.failedFuture(e);
            }
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(takmlModelWrapper);
            } catch (IOException e) {
                logger.error("IOException writing temp file for model '{}': {}", takmlModel.getName(), e.getMessage());
                return CompletableFuture.failedFuture(e);
            }
            takFileManagerServer.uploadSegmentedFile(takmlModel.getName(), file.getPath(), requesterCallsign,
                    MODELS_PARENT_CATEGORY, MediaType.get(OCTET_STREAM_MEDIA_TYPE), additionalMetadata,
                    9000000,
                    (b, fileInfo) -> {
                        hash.set(fileInfo.getHash());
                        countDownLatch.countDown();
                    }, TIMEOUT_SEGMENTED_FILES_MILLIS);

        }

        return handle(countDownLatch, hash);
    }

    @Override
    public CompletableFuture<String> removeModel(String modelHash) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicReference<String> atomicBoolean = new AtomicReference<>();

        IndexFile indexFile = takFileManagerServer.downloadIndexFile();
        if(indexFile != null && indexFile.getCategoryToRows() != null) {
            for (IndexRow indexRow : indexFile.getCategoryToRows().get(MODELS_PARENT_CATEGORY)) {
                if (indexRow.getHash().equals(modelHash)) {
                    modelExecutionService.removeModel(indexRow.getName());
                    break;
                }
            }
        }

        takFileManagerServer.removeFile(modelHash, MODELS_PARENT_CATEGORY, (b, fileInfo) -> {
            atomicBoolean.set(fileInfo.getHash());
            countDownLatch.countDown();
        });
        return handle(countDownLatch, atomicBoolean);
    }

    private CompletableFuture<String> handle(CountDownLatch countDownLatch, AtomicReference<String> hash) {
        try {
            if (!countDownLatch.await(TIMEOUT_FILE_UPLOAD_SECONDS, TimeUnit.SECONDS)) {
                logger.error("Timed out waiting for operation");
                return CompletableFuture.failedFuture(new Exception("Timed out waiting for operation"));
            }
        } catch (InterruptedException e) {
            logger.error("Timed out waiting for operation");
            return CompletableFuture.failedFuture(e);
        }
        return CompletableFuture.completedFuture(hash.get());
    }

    @Override
    public CompletableFuture<byte[]> downloadModel(String modelHash) {
        boolean isSegmentedFile = false;
        Set<IndexRow> indexRows = getModelsMetadata();
        IndexRow indexRowToUse = null;
        if (indexRows != null) {
            for (IndexRow indexRow : indexRows) {
                if(indexRow.getHash().equals(modelHash)){
                    isSegmentedFile = indexRow.getOptionalSegments() != null
                            && !indexRow.getOptionalSegments().isEmpty();
                    indexRowToUse = indexRow;
                    break;
                }
            }
        }
        if(indexRowToUse == null){
            return CompletableFuture.completedFuture(null);
        }
        CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicReference<byte[]> ret = new AtomicReference<>();
        if (isSegmentedFile) {
            FileInfo fileInfo = new FileInfo();
            fileInfo.setOptionalSegments(indexRowToUse.getOptionalSegments());
            fileInfo.setAdditionalMetadata(indexRowToUse.getAdditionalMetadata());
            fileInfo.setFileName(indexRowToUse.getName());
            fileInfo.setHash(indexRowToUse.getHash());

            File finalDirectory;
            try {
                finalDirectory = Files.createTempDirectory("finalDirectory").toFile();
            } catch (IOException e) {
                return CompletableFuture.failedFuture(e);
            }
            File segmentsDirectory;
            try {
                segmentsDirectory = Files.createTempDirectory("segmentsDirectory").toFile();
            } catch (IOException e) {
                return CompletableFuture.failedFuture(e);
            }
            takFileManagerServer.downloadSegmentedFile(finalDirectory, segmentsDirectory, fileInfo,
                    (boolean success, String modelHashRet, String message, File file) -> {
                        if (success) {
                            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                                ret.set(IOUtils.toByteArray(fileInputStream));
                            } catch (IOException e) {
                                logger.error("IOException downloading segmented model", e);
                            }
                        } else {
                            logger.error("Could not download segmented file with hash {}", modelHash);
                        }
                        countDownLatch.countDown();
                    },
                    (modelHashRet, totalSegments, fileDownloadFailed, fetchedSegments, message) -> logger.info(
                            "Downloading large segmented model: {}/{} segments",
                            fetchedSegments, totalSegments),
                    CONCURRENT_SEGMENTED_FILES_DOWNLOAD_LIMIT);
        } else {
            takFileManagerServer.downloadFile(indexRowToUse.getHash(), (b, inputStream) -> {
                if (b) {
                    try {
                        ret.set(IOUtils.toByteArray(inputStream));
                    } catch (IOException e) {
                        logger.error("IOException downloading segmented model", e);
                    }
                } else {
                    logger.error("Could not download file with model hash {}", modelHash);
                }
                countDownLatch.countDown();
            });
        }

        try {
            if (!countDownLatch.await(TIMEOUT_FILE_DOWNLOAD_SECONDS, TimeUnit.SECONDS)) {
                logger.error("Timed out waiting for operation");
                return CompletableFuture.failedFuture(new Exception("Timed out waiting for operation"));
            }
        } catch (InterruptedException e) {
            logger.error("Timed out waiting for operation");
            return CompletableFuture.failedFuture(e);
        }
        return CompletableFuture.completedFuture(ret.get());
    }

    @Override
    public IndexRow getModelMetadata(String modelHash) {
        IndexFile indexFile = takFileManagerServer.downloadIndexFile();
        if (indexFile != null && indexFile.getCategoryToRows() != null) {
            Set<IndexRow> indexRows = indexFile.getCategoryToRows().get(MODELS_PARENT_CATEGORY);
            if (indexRows != null) {
                for (IndexRow indexRow : indexRows) {
                    if(indexRow.getHash().equals(modelHash)){
                        return indexRow;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Set<IndexRow> getModelsMetadata() {
        IndexFile indexFile = takFileManagerServer.downloadIndexFile();
        if (indexFile != null && indexFile.getCategoryToRows() != null) {
            return indexFile.getCategoryToRows().get(MODELS_PARENT_CATEGORY);
        }
        return null;
    }

    @Override
    public CompletableFuture<String> addModelFeedback(AddFeedbackRequest request) {
        CompletableFuture<String> future = new CompletableFuture<>();
        InputType inputType = inferInputType(request);
        ModelFeedback fb = new ModelFeedback();

        fb.setModelName(request.getModelName());
        fb.setModelVersion(request.getModelVersion());
        fb.setCallsign(request.getCallsign());
        fb.setInputType(inputType);
        fb.setOutput(request.getOutput());
        fb.setIsCorrect(request.getIsCorrect());
        fb.setOutputErrorType(request.getOutputErrorType());
        fb.setEvaluationRating(request.getEvaluationRating());
        fb.setEvaluationConfidence(request.getEvaluationConfidence());
        fb.setComment(request.getComment());
        fb.setCreatedAt(Instant.now());

        MultipartFile inputFile = request.getInputFile();

        if (inputType == InputType.TEXT) {
            fb.setInput(request.getInputText());
            ModelFeedback saved = modelFeedbackRepository.save(fb);
            future.complete(saved.getId().toString());
            return future;
        }

        try {
            byte[] data = inputFile.getBytes();

            takFileManagerServer.saveFile("feedback", data, MediaType.get(OCTET_STREAM_MEDIA_TYPE),
                MODELS_PARENT_CATEGORY, new HashMap<>(), "Tak ML Server", (success, fileInfo) -> {
                    if (!success) {
                        future.completeExceptionally(new RuntimeException("Feedback input file upload failed"));
                        return;
                    }

                    fb.setInput(fileInfo.getHash());
                    ModelFeedback saved = modelFeedbackRepository.save(fb);
                    future.complete(saved.getId().toString());
                }
            );

        } catch (IOException e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    private InputType inferInputType(AddFeedbackRequest request) {
        if (request.getInputText() != null) {
            return InputType.TEXT;
        }

        MultipartFile file = request.getInputFile();

        if (file != null && !file.isEmpty()) {
            String contentType = file.getContentType();
            if (contentType == null) return InputType.OTHER;
            if (contentType.startsWith("image/")) return InputType.IMAGE;
            if (contentType.startsWith("audio/")) return InputType.AUDIO;
            return InputType.OTHER;
        }

        throw new IllegalStateException("Invalid input");
    }

    private long getLengthOfByteArrayMb(byte[] model) {
        // Get length of file in bytes
        long fileSizeInBytes = model.length;
        // Convert the bytes to Kilobytes (1 KB = 1024 Bytes)
        long fileSizeInKB = fileSizeInBytes / 1024;
        // Convert the KB to MegaBytes (1 MB = 1024 KBytes)
        return fileSizeInKB / 1024;
    }

    private String getSelfUrl() {
        String scheme = isSslEnabled ? "https" : "http";
        String address;

        if (serverAddress == null || serverAddress.isBlank() || "0.0.0.0".equals(serverAddress)) {
            try {
                address = InetAddress.getLocalHost().getHostAddress(); // fallback
            } catch (Exception e) {
                address = "localhost";
            }
        } else {
            address = serverAddress;
        }

        return String.format("%s://%s:%d", scheme, address, serverPort);
    }
}
