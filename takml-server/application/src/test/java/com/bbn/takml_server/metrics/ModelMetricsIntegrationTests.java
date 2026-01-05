package com.bbn.takml_server.metrics;

import com.bbn.takml_server.BaseTest;
import com.bbn.takml_server.controller.ModelMetricsController;
import com.bbn.takml_server.metrics.model.InferenceMetric;
import com.bbn.takml_server.metrics.model.ModelMetrics;
import com.bbn.takml_server.metrics.api.AddModelMetricsRequest;
import com.bbn.takml_server.metrics.model.device_metadata.DeviceMetadata;
import com.bbn.takml_server.metrics.model.device_metadata.GpuInfo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(locations = "classpath:test.properties")
@AutoConfigureMockMvc
public class ModelMetricsIntegrationTests extends BaseTest {
    private static final Logger logger = LogManager.getLogger(ModelMetricsIntegrationTests.class);

    private static final Gson gson = new GsonBuilder().setLenient().create();

    private static final String MODEL_METRICS_BASE = "/metrics/";
    private static final String ADD_MODEL_METRICS = MODEL_METRICS_BASE + "add_model_metrics";
    private static final String GET_MODEL_METRICS = MODEL_METRICS_BASE + "get_model_metrics";
    private static final String DELETE_MODEL_METRICS = MODEL_METRICS_BASE + "delete_model_metrics";

    @Autowired
    private MockMvc mockMvc;

    private static final DeviceMetadata TEST_DEVICE_METADATA = new DeviceMetadata("model", "brand",
            "manufacturer", "device", "product",
            new GpuInfo("vendor", "renderer", "version"));;

    /**
     * End-to-end validation of all expected bad request scenarios for the
     * {@link com.bbn.takml_server.controller.ModelMetricsController}.
     * <p>
     * This test verifies that invalid or malformed API requests consistently
     * return <b>HTTP 400 Bad Request</b> responses with appropriate error messages.
     * <p>
     * The following scenarios are validated:
     * <ul>
     *   <li><b>Missing request body:</b> Ensures an empty POST body returns a 400 with the correct error text.</li>
     *   <li><b>Missing required fields:</b> Verifies missing <code>requestId</code>, <code>modelName</code>, or <code>modelVersion</code> each trigger the proper validation message.</li>
     *   <li><b>Missing inference metrics:</b> Confirms both empty and null inference metric lists are rejected.</li>
     *   <li><b>Invalid modelVersion parameter:</b> Validates that a non-numeric modelVersion query parameter yields a 400 error.</li>
     *   <li><b>Missing modelName in GET request:</b> Checks that the modelName parameter is mandatory for GET requests.</li>
     * </ul>
     */
    @Test
    public void testBadRequests() {
        logger.info("Starting testBadRequests...");

        /// Missing Request Body
        try {
            MockHttpServletResponse response = mockMvc.perform(post(ADD_MODEL_METRICS)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andReturn().getResponse();

            Assertions.assertEquals(400, response.getStatus());
        } catch (Exception e) {
            Assertions.fail("Exception testing missing request body", e);
        }

        /// Missing Request ID
        AddModelMetricsRequest missingId = new AddModelMetricsRequest(null,
                "test model", 1.0, TEST_DEVICE_METADATA,
                        List.of(new InferenceMetric(1L, 2L, 0.9f))
        );
        assertBadRequest(missingId, ModelMetricsController.ERR_MISSING_REQUEST_ID);

        /// Missing Model Name
        AddModelMetricsRequest missingName = new AddModelMetricsRequest(
                UUID.randomUUID().toString(), null,
                1.0, TEST_DEVICE_METADATA, List.of(new InferenceMetric(1L, 2L,
                0.9f))
        );
        assertBadRequest(missingName, ModelMetricsController.ERR_MISSING_MODEL_NAME);

        /// Missing Inference Metrics (empty / null)
        AddModelMetricsRequest emptyMetrics = new AddModelMetricsRequest(
                UUID.randomUUID().toString(), "test model",
                1., TEST_DEVICE_METADATA, new ArrayList<>()
        );
        assertBadRequest(emptyMetrics, ModelMetricsController.ERR_MISSING_INFERENCE_METRICS);

        AddModelMetricsRequest nullMetrics = new AddModelMetricsRequest(
                UUID.randomUUID().toString(), "test model",
                1., TEST_DEVICE_METADATA, null
        );
        assertBadRequest(nullMetrics, ModelMetricsController.ERR_MISSING_INFERENCE_METRICS);

        /// Invalid modelVersion parameter (non-numeric)
        try {
            MockHttpServletResponse response = mockMvc.perform(
                            get(GET_MODEL_METRICS)
                                    .param(ModelMetricsController.MODEL_NAME_REQUEST_PARAM, "test model")
                                    .param(ModelMetricsController.MODEL_VERSION_REQUEST_PARAM, "abc"))
                    .andReturn().getResponse();

            Assertions.assertEquals(400, response.getStatus());
            Assertions.assertTrue(response.getContentAsString().contains(ModelMetricsController.ERR_INVALID_MODEL_VERSION));
        } catch (Exception e) {
            Assertions.fail("Exception testing invalid modelVersion GET param", e);
        }

        /// Missing modelName in GET
        try {
            MockHttpServletResponse response = mockMvc.perform(get(GET_MODEL_METRICS))
                    .andReturn().getResponse();

            Assertions.assertEquals(400, response.getStatus());
            logger.info(response.getContentAsString());
        } catch (Exception e) {
            Assertions.fail("Exception testing missing modelName GET param", e);
        }

        ///  missing modelName in Delete
        try {
            MockHttpServletResponse response = mockMvc.perform(delete(DELETE_MODEL_METRICS))
                    .andReturn().getResponse();

            Assertions.assertEquals(400, response.getStatus());
            logger.info(response.getContentAsString());
        } catch (Exception e) {
            Assertions.fail("Exception testing missing modelName GET param", e);
        }

        ///  invalid version in Delete
        try {
            MockHttpServletResponse response = mockMvc.perform(delete(DELETE_MODEL_METRICS)
                            .param(ModelMetricsController.MODEL_NAME_REQUEST_PARAM, "test model")
                            .param(ModelMetricsController.MODEL_VERSION_REQUEST_PARAM, "abc"))
                    .andReturn().getResponse();

            Assertions.assertEquals(400, response.getStatus());
            logger.info(response.getContentAsString());
        } catch (Exception e) {
            Assertions.fail("Exception testing missing modelName GET param", e);
        }

        logger.info("Completed testBadRequests successfully.");
    }

    private void assertBadRequest(AddModelMetricsRequest request, String expectedErrorMessage) {
        try {
            MockHttpServletResponse response = mockMvc.perform(post(ADD_MODEL_METRICS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(gson.toJson(request)))
                    .andReturn().getResponse();

            Assertions.assertEquals(400, response.getStatus(),
                    "Expected BAD_REQUEST but got " + response.getStatus());
            Assertions.assertTrue(response.getContentAsString().contains(expectedErrorMessage),
                    "Expected error message: " + expectedErrorMessage + " but got: " + response.getContentAsString());
        } catch (Exception e) {
            Assertions.fail("Exception testing bad request for model " + request.getModelName(), e);
        }
    }

    /**
     * End-to-end integration test validating the full model metrics workflow for the
     * {@link com.bbn.takml_server.controller.ModelMetricsController}.
     * <p>
     * This test performs and verifies five major scenarios:
     * <ul>
     *   <li><b>Initial metrics (new model/version):</b> Inserts and retrieves baseline metrics for a new model.</li>
     *   <li><b>Append metrics to existing model/version:</b> Ensures additional metrics merge correctly into the same record.</li>
     *   <li><b>Add metrics for different model/version:</b> Verifies separate models are stored independently.</li>
     *   <li><b>Add metrics with different version (same model name):</b> Confirms metrics for different versions remain isolated.</li>
     *   <li><b>Retrieve all metrics (global fetch):</b> Validates aggregate retrieval across all models and versions.</li>
     * </ul>
     */
    @Test
    public void testInsertDeleteAndGetModelMetrics() {
        // ───────────────────────────────────────────────
        // Initial metrics (new model/version)
        // ───────────────────────────────────────────────
        AddModelMetricsRequest request1 = buildMetricsRequest(
                "test model", 1.0,
                TEST_DEVICE_METADATA,
                List.of(
                        new InferenceMetric(1L, 2L, 0.9f),
                        new InferenceMetric(2L, 3L, 0.85f)
                )
        );
        performAddAndVerify(request1);
        MockHttpServletResponse responseGet1 = performGetAndVerify(request1);
        compareInputsVsResponse(responseGet1, request1);

        // ───────────────────────────────────────────────
        // Retrieve model inference count
        // ───────────────────────────────────────────────
        try {
            MockHttpServletResponse inferenceCount = mockMvc.perform(get(
                    "/metrics/get_model_inference_count")
                    .param(ModelMetricsController.MODEL_NAME_REQUEST_PARAM, "test model")
                    .param(ModelMetricsController.MODEL_VERSION_REQUEST_PARAM, String.valueOf(1.0)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse();

            MockHttpServletResponse inferenceCount2 = mockMvc.perform(get(
                            "/metrics/get_model_inference_count")
                            .param(ModelMetricsController.MODEL_NAME_REQUEST_PARAM, "test model"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse();

            String countStr = inferenceCount.getContentAsString();
            Assertions.assertEquals("2", countStr);
            String countStr2 = inferenceCount2.getContentAsString();
            Assertions.assertEquals("2", countStr2);
        } catch (Exception e) {
            Assertions.fail("Exception fetching model metrics count", e);
        }

        // ───────────────────────────────────────────────
        // Append metrics to existing model/version
        // ───────────────────────────────────────────────
        AddModelMetricsRequest request2 = buildMetricsRequest(
                "test model", 1.0,
                TEST_DEVICE_METADATA,
                List.of(new InferenceMetric(4L, 5L, 0.88f))
        );
        performAddAndVerify(request2);
        MockHttpServletResponse responseGet2 = performGetAndVerify(request2);
        compareInputsVsResponse(responseGet2, request1, request2);

        // ───────────────────────────────────────────────
        // Add metrics for different model/version
        // ───────────────────────────────────────────────
        AddModelMetricsRequest request3 = buildMetricsRequest(
                "test model 2", 1.0,
                TEST_DEVICE_METADATA,
                List.of(new InferenceMetric(7L, 22L, 0.2f))
        );
        performAddAndVerify(request3);
        MockHttpServletResponse responseGet3 = performGetAndVerify(request3);
        compareInputsVsResponse(responseGet3, request3);

        // ───────────────────────────────────────────────
        // Add metrics for different model/version
        // ───────────────────────────────────────────────
        AddModelMetricsRequest request4 = buildMetricsRequest(
                "test model 3", 1.0,
                TEST_DEVICE_METADATA,
                List.of(new InferenceMetric(7L, 22L, 0.2f))
        );
        performAddAndVerify(request4);
        MockHttpServletResponse responseGet4 = performGetAndVerify(request4);
        compareInputsVsResponse(responseGet4, request4);

        // ───────────────────────────────────────────────
        // Append metrics to existing model/version
        // ───────────────────────────────────────────────
        AddModelMetricsRequest request5 = buildMetricsRequest(
                "test model 2", 1.0,
                TEST_DEVICE_METADATA,
                List.of(new InferenceMetric(4L, 5L, 0.88f))
        );
        performAddAndVerify(request5);
        MockHttpServletResponse responseGet5 = performGetAndVerify(request5);
        compareInputsVsResponse(responseGet5, request3, request5);

        // ───────────────────────────────────────────────
        // Delete model metrics given name and version
        // ───────────────────────────────────────────────
        try {
            MockHttpServletResponse response = mockMvc.perform(delete(DELETE_MODEL_METRICS)
                            .param(ModelMetricsController.MODEL_NAME_REQUEST_PARAM, "test model")
                            .param(ModelMetricsController.MODEL_VERSION_REQUEST_PARAM, "1.0"))
                    .andReturn().getResponse();

            Assertions.assertEquals(200, response.getStatus());
        } catch (Exception e) {
            Assertions.fail("Exception calling delete", e);
        }

        // ───────────────────────────────────────────────
        // Retrieve all metrics (global fetch)
        // ───────────────────────────────────────────────
        try {
            MockHttpServletResponse responseAll = mockMvc.perform(get("/metrics/get_all_model_metrics"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse();

            String jsonAll = responseAll.getContentAsString();
            ModelMetrics[] allMetrics = gson.fromJson(jsonAll, ModelMetrics[].class);

            Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
            logger.info("Final output get all model metrics: {}", prettyGson.toJson(allMetrics));

            Assertions.assertEquals(2, allMetrics.length, "Expected multiple metrics entries in global list");
        } catch (Exception e) {
            Assertions.fail("Exception fetching all model metrics", e);
        }
    }

    @AfterEach
    public void cleanup(){
        try {
            MockHttpServletResponse response = mockMvc.perform(delete(DELETE_MODEL_METRICS)
                            .param(ModelMetricsController.MODEL_NAME_REQUEST_PARAM, "test model"))
                    .andReturn().getResponse();

            Assertions.assertEquals(200, response.getStatus());
        } catch (Exception e) {
            Assertions.fail("Exception calling delete", e);
        }
        try {
            MockHttpServletResponse response = mockMvc.perform(delete(DELETE_MODEL_METRICS)
                            .param(ModelMetricsController.MODEL_NAME_REQUEST_PARAM, "test model 3"))
                    .andReturn().getResponse();

            Assertions.assertEquals(200, response.getStatus());
        } catch (Exception e) {
            Assertions.fail("Exception calling delete", e);
        }
    }

    private AddModelMetricsRequest buildMetricsRequest(String modelName, double modelVersion, DeviceMetadata deviceMetadata, List<InferenceMetric> metrics) {
        return new AddModelMetricsRequest(UUID.randomUUID().toString(), modelName, modelVersion, deviceMetadata, metrics);
    }

    private void performAddAndVerify(AddModelMetricsRequest request) {
        try {
            MockHttpServletResponse response = executeExampleAddModelMetricsRequest(request);
            Assertions.assertEquals(200, response.getStatus(),
                    "POST /add_model_metrics failed for model " + request.getModelName());
        } catch (Exception e) {
            Assertions.fail("Exception during addModelMetrics for " + request.getModelName(), e);
        }
    }

    private MockHttpServletResponse performGetAndVerify(AddModelMetricsRequest request) {
        try {
            MockHttpServletResponse response = executeGetModelMetrics(request.getModelName(),
                    request.getModelVersion());
            Assertions.assertEquals(200, response.getStatus(),
                    "GET /get_model_metrics failed for model " + request.getModelName());
            return response;
        } catch (Exception e) {
            Assertions.fail("Exception during getModelMetrics for " + request.getModelName(),
                    e);
            return null;
        }
    }

    private void compareInputsVsResponse(MockHttpServletResponse responseGet, AddModelMetricsRequest... requests){
        Assertions.assertNotNull(responseGet);
        String jsonBody = null;
        try {
            jsonBody = responseGet.getContentAsString();
        } catch (UnsupportedEncodingException e) {
            Assertions.fail("UnsupportedEncodingException parsing json response", e);
        }
        ModelMetrics[] metricsArray = null;
        try {
            metricsArray = gson.fromJson(jsonBody, ModelMetrics[].class);
        } catch (Exception e) {
            Assertions.fail("Failed to parse JSON response into ModelMetrics list", e);
        }
        Assertions.assertNotNull(metricsArray, "Model Metrics was null");

        // Collect and merge requests for same model name and version
        Map<String, List<InferenceMetric>> requestMap = new HashMap<>();
        for (AddModelMetricsRequest request : requests) {
            requestMap.computeIfAbsent(request.getModelName()
                            + "-" + request.getModelVersion(), k -> new ArrayList<>())
                    .addAll(request.getInferenceMetrics());
        }

        // Compare
        Assertions.assertEquals(requestMap.size(), metricsArray.length);
        for(ModelMetrics modelMetrics : metricsArray){
            List<InferenceMetric> inferenceMetrics = requestMap.get(modelMetrics.getModelName() + "-" + modelMetrics.getModelVersion());
            Assertions.assertNotNull(inferenceMetrics, "Model name/version in db does not match request");
            assertMetricListsEqual(inferenceMetrics, modelMetrics.getInferenceMetricList());
            for(InferenceMetric inferenceMetric : modelMetrics.getInferenceMetricList()){
                Assertions.assertEquals(TEST_DEVICE_METADATA, inferenceMetric.getDeviceMetadata());
            }
        }
    }

    private void assertMetricListsEqual(List<InferenceMetric> expected, List<InferenceMetric> actual) {
        Assertions.assertEquals(expected.size(), actual.size(), "size mismatch");

        for (int i = 0; i < expected.size(); i++) {
            Assertions.assertTrue(equalWithoutId(expected.get(i), actual.get(i)),
                    "Metric mismatch at index " + i);
        }
    }

    private boolean equalWithoutId(InferenceMetric a, InferenceMetric b) {
        return a.getStartMillis() == b.getStartMillis()
                && a.getDurationMillis() == b.getDurationMillis()
                && Float.compare(a.getConfidence(), b.getConfidence()) == 0;
    }

    private MockHttpServletResponse executeExampleAddModelMetricsRequest(AddModelMetricsRequest request)
            throws Exception {
        return mockMvc.perform(post(ADD_MODEL_METRICS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gson.toJson(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse();
    }

    private MockHttpServletResponse executeGetModelMetrics(String modelName, Double modelVersion) throws Exception {
        return mockMvc.perform(get(GET_MODEL_METRICS)
                .param(ModelMetricsController.MODEL_NAME_REQUEST_PARAM, modelName)
                .param(ModelMetricsController.MODEL_VERSION_REQUEST_PARAM, String.valueOf(modelVersion)))
                .andExpect(status().isOk())
                .andReturn().getResponse();
    }
}
