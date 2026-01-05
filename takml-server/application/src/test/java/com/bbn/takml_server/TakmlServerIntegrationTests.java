package com.bbn.takml_server;

import com.bbn.tak_sync_file_manager.model.IndexRow;
import com.bbn.takml_server.model_execution.api.model.ModelMetadataResponse;
import com.bbn.takml_server.model_execution.api.model.ModelTensor;
import com.bbn.takml_server.model_management.api.AddTakmlModelWrapperRequest;
import com.bbn.takml_server.model_management.api.EditTakmlModelWrapperRequest;
import com.bbn.takml_server.mx.ImagePreprocessor;
import com.bbn.takml_server.mx.PostProcessorUtil;
import com.bbn.takml_server.model_execution.TensorSerializerUtil;
import com.bbn.takml_server.model_execution.api.model.InferInput;
import com.bbn.takml_server.model_execution.api.model.InferOutput;
import com.bbn.takml_server.model_execution.api.model.InferenceRequest;
import com.bbn.takml_server.model_execution.api.model.InferenceResponse;
import com.bbn.takml_server.model_execution.takml_result.Recognition;
import com.bbn.takml_server.takml_model.ModelTypeConstants;
import com.bbn.takml_server.takml_model.TakmlModel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.UUID;

import static com.bbn.takml_server.model_execution.ModelExecutionService.TAKML_CONFIG_FILE;
import static com.bbn.takml_server.takml_model.MetadataConstants.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "file:src/test/resources/test.properties")
@AutoConfigureMockMvc
class TakmlServerIntegrationTests extends BaseTest {
    private static final Logger logger = LogManager.getLogger(TakmlServerIntegrationTests.class);
    private static final Gson gson = new GsonBuilder().setLenient().create();

    // Model constants
    private static final String TEST_MODEL = "test model";
    private static final String TEST_MODEL_2 = "test model 2";
    private static final String MOBILENET_ONNX = "MobileNet Onnx";
    private static final String MOBILENET_ONNX_2 = "MobileNet Onnx 2";
    private static final String DOGS_CATS_PYTORCH = "Dogs and Cats Pytorch";

    // Tester constants
    private static final String TESTER = "tester";
    private static final String TESTER_2 = "tester2";
    private static final String TESTER_4 = "tester4";

    // Model type constants
    private static final String CLASSIFICATION = "classification";
    private static final String CLASSIFICATION_2 = "classification2";
    private static final String IMAGE_CLASSIFICATION = "IMAGE_CLASSIFICATION";

    // File extensions
    private static final String PT_EXTENSION = ".pt";
    private static final String ONNX_EXTENSION = ".onnx";

    // Path constants
    private static final String MODEL_MANAGEMENT_BASE = "/model_management/";
    private static final String ADD_MODEL_WRAPPER = MODEL_MANAGEMENT_BASE + "add_model_wrapper";
    private static final String EDIT_MODEL_WRAPPER = MODEL_MANAGEMENT_BASE + "edit_model_wrapper/";
    private static final String GET_MODEL_METADATA = MODEL_MANAGEMENT_BASE + "get_model_metadata/";
    private static final String GET_MODELS = MODEL_MANAGEMENT_BASE + "get_models";
    private static final String SEARCH_MODELS = MODEL_MANAGEMENT_BASE + "search";
    private static final String REMOVE_MODEL = MODEL_MANAGEMENT_BASE + "remove_model/";
    private static final String GET_MODEL = MODEL_MANAGEMENT_BASE + "get_model/";
    private static final String UI_ADD_MODEL = "/model_management/ui/add_model";
    private static final String UI_EDIT_MODEL = "/model_management/ui/edit_model/";
    private static final String MX_INFER = "/v2/models/%s/versions/1.0/infer";
    private static final String MX_GET_MODELS = "/v2/models/";

    // Test resource paths
    private static final String EXAMPLE_MODELS_DIR = "src/test/resources/example_models/";
    private static final String TEST_MODEL_ZIP = EXAMPLE_MODELS_DIR + "test model.zip";
    private static final String TEST_MODEL_2_ZIP = EXAMPLE_MODELS_DIR + "test model 2.zip";
    private static final String EXAMPLE_ONNX_ZIP = EXAMPLE_MODELS_DIR + "example-onnx-model 2.zip";
    private static final String CAT_IMAGE = "src/test/resources/cat.jpeg";
    private static final String GOLDFISH_IMAGE = "src/test/resources/goldfish.jpeg";
    private static final String LABELS_FILE = "src/test/resources/example_models/example-onnx-model/labels.txt";

    @Autowired
    private MockMvc mockMvc;

    private IndexRow getModelMetadata(String hash) throws Exception {
        MvcResult result = mockMvc.perform(get(GET_MODEL_METADATA + hash))
                .andExpect(status().isOk()).andReturn();
        String metaJson = result.getResponse().getContentAsString();
        return gson.fromJson(metaJson, IndexRow.class);
    }

    private List<IndexRow> getAllModels() throws Exception {
        MvcResult result = mockMvc.perform(get(GET_MODELS))
                .andExpect(status().isOk()).andReturn();
        String metaAllJson = result.getResponse().getContentAsString();
        Type listType = new TypeToken<ArrayList<IndexRow>>() {}.getType();
        return gson.fromJson(metaAllJson, listType);
    }

    private List<IndexRow> searchModels(String modelName, String modelType) throws Exception {
        MockHttpServletRequestBuilder request = get(SEARCH_MODELS);

        if (modelName != null) {
            request = request.param("modelName", modelName);
        }
        if (modelType != null) {
            request = request.param("modelType", modelType);
        }

        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();

        String metaAllJson = result.getResponse().getContentAsString();
        Type listType = new TypeToken<ArrayList<IndexRow>>() {}.getType();
        return gson.fromJson(metaAllJson, listType);
    }

    private void assertMetadata(IndexRow row, String hash, String name, String callsign, String type, String ext) {
        Assertions.assertNotNull(row.getAdditionalMetadata());
        Assertions.assertEquals(hash, row.getHash());
        Assertions.assertEquals(name, row.getName());
        Assertions.assertEquals(callsign, row.getAdditionalMetadata().get(CALLSIGN_META));
        Assertions.assertEquals(type, row.getAdditionalMetadata().get(MODEL_TYPE_META));
        Assertions.assertEquals(ext, row.getAdditionalMetadata().get(MODEL_EXTENSION_META));
    }

    @BeforeEach
    public void before() throws IOException {
        cleanUpTestDirectory(TEST_MODEL);
        cleanUpTestDirectory(MOBILENET_ONNX_2);
    }

    private void cleanUpTestDirectory(String dirName) throws IOException {
        File dir = new File(EXAMPLE_MODELS_DIR + dirName);
        if (dir.exists()) {
            FileUtils.deleteDirectory(dir);
        }
    }

    private void checkFilesExist(File takmlWrapperFolder, Set<String> expectedFiles) {
        File[] files = takmlWrapperFolder.listFiles();
        Assertions.assertNotNull(files);
        for (File file : files) {
            expectedFiles.remove(file.getName());
        }
        Assertions.assertTrue(expectedFiles.isEmpty());
    }

    private String addModelWrapper(MultipartFile inputZip, String requesterCallsign) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "takmlModelWrapper",
                inputZip.getOriginalFilename(),
                "application/zip",
                inputZip.getBytes()
        );

        return mockMvc.perform(multipart(ADD_MODEL_WRAPPER)
                                .file(file)
                                .param("requesterCallsign", requesterCallsign)
                                .param("runOnServer", "true")
                        // optional:
                        // .param("supportedDevices", "pixel6,samsung-sm-s908u")
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String editModelWrapper(byte[] inputZip, String requesterCallsign, String hash) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "takmlModelWrapper",                      // must match field name in EditTakmlModelWrapperRequest
                "model.zip",
                "application/zip",
                inputZip
        );

        return mockMvc.perform(multipart(EDIT_MODEL_WRAPPER + hash)
                                .file(file)
                                .param("requesterCallsign", requesterCallsign)
                                .param("runOnServer", "true")
                        // optional:
                        // .param("supportedDevices", "pixel6,samsung-sm-s908u")
                        // multipart() uses POST by default, so no need to override method
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }


    private String uiAddModelWrapper(byte[] inputZip, String requesterCallsign) throws Exception {
        MvcResult result = mockMvc.perform(multipart(UI_ADD_MODEL)
                        .file(new MockMultipartFile("model", inputZip))
                        .param("requesterCallsign", requesterCallsign)
                        .param("runOnTakmlServer", "true"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        return (String) result.getModelAndView().getModel().get("hash");
    }

    private String uiEditModelWrapper(byte[] inputZip, String requesterCallsign, String hash) throws Exception {
        MvcResult result = mockMvc.perform(multipart(UI_EDIT_MODEL + hash)
                        .file(new MockMultipartFile("model", inputZip))
                        .param("modelHash", hash)
                        .param("requesterCallsign", requesterCallsign)
                        .param("runOnTakmlServer", "true"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        return (String) result.getModelAndView().getModel().get("hash");
    }

    private byte[] readFileContent(String filePath) throws IOException {
        return Files.readAllBytes(Path.of(filePath));
    }

    private BufferedImage loadImage(String imagePath) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(readFileContent(imagePath)));
    }

    private Pair<List<BigDecimal>, BufferedImage> createInputTensor(String imagePath) throws IOException {
        BufferedImage image = loadImage(imagePath);
        return ImagePreprocessor.createInputTensor(image, null);
    }

    private void verifyModelContent(String hash, byte[] expectedContent) throws Exception {
        byte[] output = mockMvc.perform(get(GET_MODEL + hash))
                .andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsByteArray();
        Assertions.assertArrayEquals(expectedContent, output);
    }

    /**
     * Tests insert, edit, and deletion of model
     *
     * <ul>
     *   <li>Adds a test model wrapper and checks if exists/li>
     *   <li>Request has runOnServer, so it should produce files on disk in models folder</li>
     *   <li>Edits wrapper and checks</li>
     *   <li>Request has runOnServer, so it should produce files on disk in models folder</li>
     *   <li>Delete and assure no files on disk</li>
     * </ul>
     */
    @Test
    public void testAddEditRemoveModelWrapper() throws Exception {
        byte[] inputZip = readFileContent(TEST_MODEL_ZIP);
        String hash = addModelWrapper(new MockMultipartFile("test", inputZip), TESTER);

        IndexRow indexRow = getModelMetadata(hash);
        assertMetadata(indexRow, hash, TEST_MODEL, TESTER, CLASSIFICATION, PT_EXTENSION);
        File dir = new File(EXAMPLE_MODELS_DIR + TEST_MODEL);
        checkFilesExist(dir, new HashSet<>(Arrays.asList(TEST_MODEL + PT_EXTENSION, TAKML_CONFIG_FILE)));

        Assertions.assertTrue(getAllModels().contains(indexRow));

        byte[] inputZip2 = readFileContent(TEST_MODEL_2_ZIP);
        hash = editModelWrapper(inputZip2, TESTER_2, hash);

        indexRow = getModelMetadata(hash);
        assertMetadata(indexRow, hash, TEST_MODEL, TESTER_2, CLASSIFICATION_2, PT_EXTENSION);
        checkFilesExist(dir, new HashSet<>(Arrays.asList(TEST_MODEL_2 + PT_EXTENSION, TAKML_CONFIG_FILE)));

        Assertions.assertTrue(getAllModels().contains(indexRow));

        mockMvc.perform(delete(REMOVE_MODEL + hash))
                .andExpect(status().isOk());
        Assertions.assertFalse(dir.exists());
    }

    /**
     * Tests insert, edit, and deletion of model (UI call)
     *
     * <ul>
     *   <li>Adds a test model wrapper and checks if exists/li>
     *   <li>Request has runOnServer, so it should produce files on disk in models folder</li>
     *   <li>Edits wrapper and checks</li>
     *   <li>Request has runOnServer, so it should produce files on disk in models folder</li>
     *   <li>Delete and assure no files on disk</li>
     * </ul>
     */
    @Test
    public void testUIAddEditRemoveModelWrapper() throws Exception {
        byte[] inputZip = readFileContent(TEST_MODEL_ZIP);
        String hash = uiAddModelWrapper(inputZip, TESTER);

        IndexRow indexRow = getModelMetadata(hash);
        assertMetadata(indexRow, hash, TEST_MODEL, TESTER, CLASSIFICATION, PT_EXTENSION);
        File dir = new File(EXAMPLE_MODELS_DIR + TEST_MODEL);
        checkFilesExist(dir, new HashSet<>(Arrays.asList(TEST_MODEL + PT_EXTENSION, TAKML_CONFIG_FILE)));

        Assertions.assertTrue(getAllModels().contains(indexRow));

        byte[] inputZip2 = readFileContent(TEST_MODEL_2_ZIP);
        hash = editModelWrapper(inputZip2, TESTER_2, hash);

        indexRow = getModelMetadata(hash);
        assertMetadata(indexRow, hash, TEST_MODEL, TESTER_2, CLASSIFICATION_2, PT_EXTENSION);
        checkFilesExist(dir, new HashSet<>(Arrays.asList(TEST_MODEL_2 + PT_EXTENSION, TAKML_CONFIG_FILE)));

        Assertions.assertTrue(getAllModels().contains(indexRow));

        mockMvc.perform(delete(REMOVE_MODEL + hash))
                .andExpect(status().isOk());
        Assertions.assertFalse(dir.exists());
    }

    /**
     * Tests insert, run, and deletion of large model
     *
     * <ul>
     *   <li>Adds a test model wrapper and checks if exists/li>
     *   <li>Request has runOnServer, so it should produce files on disk in models folder</li>
     *   <li>Run Inference and check it works</li>
     *   <li>Delete and assure no files on disk</li>
     * </ul>
     */
    @Test
    public void testInsertAndRunLargeModel() throws Exception {
        byte[] inputZip = readFileContent(EXAMPLE_ONNX_ZIP);
        String hash = addModelWrapper(new MockMultipartFile("test", inputZip), TESTER_4);

        IndexRow indexRow = getModelMetadata(hash);
        assertMetadata(indexRow, hash, MOBILENET_ONNX_2, TESTER_4, IMAGE_CLASSIFICATION, ONNX_EXTENSION);
        verifyModelContent(hash, inputZip);
        Assertions.assertTrue(getAllModels().contains(indexRow));

        Pair<List<BigDecimal>, BufferedImage> input = createInputTensor(CAT_IMAGE);
        InferInput inferInput = new InferInput(UUID.randomUUID().toString(),
                List.of(1, 3, 224, 224), ModelTypeConstants.IMAGE_CLASSIFICATION, input.getLeft());
        List<Recognition> recognitions = testModelExecution(DOGS_CATS_PYTORCH, List.of("cat", "dog"), inferInput);

        Assertions.assertFalse(recognitions.isEmpty());
        Assertions.assertEquals("cat", recognitions.getFirst().getLabel());

        mockMvc.perform(delete(REMOVE_MODEL + hash))
                .andExpect(status().isOk());
        mockMvc.perform(get(GET_MODEL + hash))
                .andExpect(status().isNotFound());
    }

    /**
     * Tests insert, run, and deletion of large model (UI call)
     *
     * <ul>
     *   <li>Adds a test model wrapper and checks if exists/li>
     *   <li>Request has runOnServer, so it should produce files on disk in models folder</li>
     *   <li>Run Inference and check it works</li>
     *   <li>Delete and assure no files on disk</li>
     * </ul>
     */
    @Test
    public void testUIInsertAndRunLargeModel() throws Exception {
        byte[] inputZip = readFileContent(EXAMPLE_ONNX_ZIP);
        String hash = uiAddModelWrapper(inputZip, TESTER_4);

        IndexRow indexRow = getModelMetadata(hash);
        assertMetadata(indexRow, hash, MOBILENET_ONNX_2, TESTER_4, IMAGE_CLASSIFICATION, ONNX_EXTENSION);
        verifyModelContent(hash, inputZip);
        Assertions.assertTrue(getAllModels().contains(indexRow));

        Pair<List<BigDecimal>, BufferedImage> input = createInputTensor(CAT_IMAGE);
        InferInput inferInput = new InferInput(UUID.randomUUID().toString(),
                List.of(1, 3, 224, 224), ModelTypeConstants.IMAGE_CLASSIFICATION, input.getLeft());
        List<Recognition> recognitions = testModelExecution(DOGS_CATS_PYTORCH, List.of("cat", "dog"), inferInput);

        Assertions.assertFalse(recognitions.isEmpty());
        Assertions.assertEquals("cat", recognitions.getFirst().getLabel());

        mockMvc.perform(delete(REMOVE_MODEL + hash))
                .andExpect(status().isOk());
        mockMvc.perform(get(GET_MODEL + hash))
                .andExpect(status().isNotFound());
    }

    /**
     * Tests handling of adding a model with missing parameters.
     * <ul>
     *   <li>Attempts to add a model with a missing file</li>
     *   <li>Attempts to add a model with a missing callsign</li>
     * </ul>
     */
    @Test
    public void testAddModelMissingParameters() throws Exception {
        byte[] testInput = "hi".getBytes(StandardCharsets.UTF_8);

        // missing model wrapper
        AddTakmlModelWrapperRequest request = new AddTakmlModelWrapperRequest(null, TESTER_4, true);
        mockMvc.perform(post(ADD_MODEL_WRAPPER)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .content(gson.toJson(request)))
                .andExpect(status().isBadRequest());

        // missing callsign
        request = new AddTakmlModelWrapperRequest(new MockMultipartFile("test", testInput), null, true);
        mockMvc.perform(post(ADD_MODEL_WRAPPER)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .content(gson.toJson(request)))
                .andExpect(status().isBadRequest());
    }

    private List<Recognition> testModelExecution(String modelName, List<String> labels, InferInput inferInput) throws Exception {
        InferenceRequest inferenceRequest = new InferenceRequest();
        inferenceRequest.setId(UUID.randomUUID().toString());
        inferenceRequest.setInputs(Collections.singletonList(inferInput));

        MvcResult result = mockMvc.perform(post(String.format(MX_INFER, modelName))
                        .content(gson.toJson(inferenceRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String responseStr = result.getResponse().getContentAsString();
        InferenceResponse inferenceResponse = gson.fromJson(responseStr, InferenceResponse.class);
        InferOutput inferOutput = inferenceResponse.getOutputs().iterator().next();

        float[] tensorOutputFloat = TensorSerializerUtil.convertToFloatArr(inferOutput.getData());
        List<Recognition> recognitions = PostProcessorUtil.getProbabilities(tensorOutputFloat, true, labels);

        for (Recognition recognition : recognitions) {
            logger.info("Model output: {} {}", recognition.getLabel(), recognition.getConfidence());
        }
        return recognitions;
    }

    private ModelMetadataResponse getModelKServe(String modelName, String version) throws Exception {
        MvcResult result = mockMvc.perform(get(MX_GET_MODELS + modelName + "/versions/" + version)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String responseStr = result.getResponse().getContentAsString();
        return gson.fromJson(responseStr, ModelMetadataResponse.class);
    }

    /**
     * Tests the execution of a Pytorch model using the API.
     * <ul>
     *   <li>Loads an image as input for the model</li>
     *   <li>Prepares an input tensor and sends a request to the inference endpoint</li>
     *   <li>Parses and verifies the recognition results</li>
     * </ul>
     */
    @Test
    public void testModelExecutionPytorch() throws Exception {
        Pair<List<BigDecimal>, BufferedImage> input = createInputTensor(CAT_IMAGE);
        InferInput inferInput = new InferInput(UUID.randomUUID().toString(),
                List.of(1, 3, 224, 224), ModelTypeConstants.IMAGE_CLASSIFICATION, input.getLeft());
        List<Recognition> recognitions = testModelExecution(DOGS_CATS_PYTORCH, List.of("cat", "dog"), inferInput);

        Assertions.assertFalse(recognitions.isEmpty());
        Assertions.assertEquals("cat", recognitions.getFirst().getLabel());
    }

    /**
     * Tests the execution of an Onnx model using the API.
     * <ul>
     *   <li>Loads an image as input for the model</li>
     *   <li>Prepares an input tensor and sends a request to the inference endpoint</li>
     *   <li>Parses and verifies the recognition results</li>
     * </ul>
     */
    @Test
    public void testModelExecutionOnnx() throws Exception {
        List<String> labels = Files.readAllLines(Path.of(LABELS_FILE));
        Pair<List<BigDecimal>, BufferedImage> input = createInputTensor(GOLDFISH_IMAGE);
        InferInput inferInput = new InferInput(UUID.randomUUID().toString(),
                List.of(1, 3, 224, 224), ModelTypeConstants.IMAGE_CLASSIFICATION, input.getLeft());
        List<Recognition> recognitions = testModelExecution(MOBILENET_ONNX, labels, inferInput);

        Assertions.assertFalse(recognitions.isEmpty());
        Assertions.assertEquals("n01443537 goldfish, Carassius auratus", recognitions.getFirst().getLabel());
    }

    /**
     * Tests search filter of models, both name and type of model (e.g. image_classification)
     *
     * <ul>
     *     <li>Search for name of model, expect 1 matching result</li>
     *     <li>Search for type of model, expect 2 matching results</li>
     * </ul>
     */
    @Test
    public void testSearchModels() throws Exception {
        // search for model name
        List<IndexRow> results = searchModels(DOGS_CATS_PYTORCH, null);
        Assertions.assertNotNull(results);
        Assertions.assertEquals(1, results.size());
        IndexRow indexRow = results.getFirst();
        Assertions.assertEquals(DOGS_CATS_PYTORCH, indexRow.getName());

        // search for model type
        List<IndexRow> results2 = searchModels(null, "image_classification");
        Assertions.assertNotNull(results2);
        Assertions.assertEquals(2, results2.size());

        Set<String> modelsNeeded = new HashSet<>(Set.of(DOGS_CATS_PYTORCH, MOBILENET_ONNX));
        for(IndexRow row : results2){
            modelsNeeded.remove(row.getName());
        }
        Assertions.assertEquals(0, modelsNeeded.size());
    }

    /**
     * Tests get models using KServe's API. Checks the model name, platform, and version is returned
     * @throws Exception
     */
    @Test
    public void testGetModelsKServe() throws Exception {
        ModelMetadataResponse response = getModelKServe(DOGS_CATS_PYTORCH, "1.0");
        Assertions.assertNotNull(response);
        Assertions.assertEquals(DOGS_CATS_PYTORCH, response.getName());
        Assertions.assertEquals("1.0", response.getVersions().getFirst());
        Assertions.assertEquals("Server", response.getPlatform());

        ModelTensor modelTensor = new ModelTensor();
        //modelTensor.get
        //response.getInputs()

        response = getModelKServe(MOBILENET_ONNX, "1.0");
        Assertions.assertNotNull(response);
        Assertions.assertEquals(MOBILENET_ONNX, response.getName());
        Assertions.assertEquals("1.0", response.getVersions().getFirst());
        Assertions.assertEquals("Server", response.getPlatform());
    }
}