package com.bbn.takml_server.controller;

import com.bbn.takml_server.metrics.model.InferenceMetric;
import com.bbn.takml_server.metrics.model.ModelMetrics;
import com.bbn.takml_server.metrics.MetricsService;
import com.bbn.takml_server.metrics.api.AddModelMetricsRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * REST controller for handling Model Metrics requests.
 * Provides endpoints for inserting, retrieving, and listing all model metrics
 * within the TAK ML Server database.
 */
@RestController
@RequestMapping("/metrics")
@Tag(name = "Model Metrics", description = "API for Model Metrics")
public class ModelMetricsController {

    private static final Logger logger = LogManager.getLogger(ModelMetricsController.class);

    // ───────────────────────────────────────────────
    // Request parameter names
    // ───────────────────────────────────────────────
    public static final String MODEL_NAME_REQUEST_PARAM = "modelName";
    public static final String MODEL_VERSION_REQUEST_PARAM = "modelVersion";

    // ───────────────────────────────────────────────
    // Common response messages
    // ───────────────────────────────────────────────
    public static final String ERR_MISSING_REQUEST_BODY = "Missing Model Metrics Request";
    public static final String ERR_MISSING_REQUEST_ID = "Missing Request Id";
    public static final String ERR_MISSING_MODEL_NAME = "Missing Model Name";
    public static final String ERR_MISSING_MODEL_VERSION = "Missing Model Version";
    public static final String ERR_MISSING_INFERENCE_METRICS = "Missing Model Inference Metrics";
    public static final String ERR_INVALID_MODEL_VERSION = "Invalid model version ('modelVersion'), decimal only";
    public static final String ERR_INTERNAL_SERVER = "Internal server error processing request";
    public static final String SUCCESS = "Success";

    @Autowired
    private MetricsService metricsService;

    /**
     * Retrieves all model metrics stored in the system.
     *
     * @return HTTP 200 with a list of all {@link ModelMetrics} entries
     */
    @GetMapping("/get_all_model_metrics")
    @Operation(summary = "Get All Model Metrics")
    public ResponseEntity<List<ModelMetrics>> getAllModelMetrics() {
        logger.info("Received get all model metrics request");
        List<ModelMetrics> modelMetrics = metricsService.getAllModelMetrics();
        return ResponseEntity.ok(modelMetrics);
    }

    /**
     * Retrieves model metrics for a specific model name, and optionally a model version.
     * <p>
     * Example usage:
     * <ul>
     *   <li><code>/metrics/get_model_metrics?modelName=ModelA</code></li>
     *   <li><code>/metrics/get_model_metrics?modelName=ModelA&modelVersion=1.0</code></li>
     * </ul>
     *
     * <pre>
     * Example Response:
     * [
     *   {
     *     "id": {
     *       "modelName": "test model",
     *       "modelVersion": 1
     *     },
     *     "inferenceMetricList": [
     *       {
     *         "id": 1,
     *         "deviceMetadata": {
     *           "id": 1,
     *           "model": "model",
     *           "brand": "brand",
     *           "manufacturer": "manufacturer",
     *           "device": "device",
     *           "product": "product",
     *           "gpuInfo": {
     *             "id": 1,
     *             "vendor": "vendor",
     *             "renderer": "renderer",
     *             "version": "version"
     *           }
     *         },
     *         "startMillis": 1,
     *         "durationMillis": 2,
     *         "confidence": 0.9
     *       },
     *       {
     *         "id": 2,
     *         "deviceMetadata": {
     *           "id": 1,
     *           "model": "model",
     *           "brand": "brand",
     *           "manufacturer": "manufacturer",
     *           "device": "device",
     *           "product": "product",
     *           "gpuInfo": {
     *             "id": 1,
     *             "vendor": "vendor",
     *             "renderer": "renderer",
     *             "version": "version"
     *           }
     *         },
     *         "startMillis": 2,
     *         "durationMillis": 3,
     *         "confidence": 0.85
     *       }
     *     ],
     *     "modelName": "test model",
     *     "modelVersion": 1
     *   }
     * ]
     * </pre>
     *
     * @param modelName    the name of the model to fetch metrics for (required)
     * @param modelVersion optional version of the model; if omitted, all versions are returned
     * @return HTTP 200 with a list of matching {@link ModelMetrics}, or HTTP 400 for invalid parameters
     */
    @GetMapping("/get_model_metrics")
    @Operation(summary = "Get Model Metrics")
    public ResponseEntity<?> getModelMetrics(
            @RequestParam String modelName,
            @RequestParam(required = false) String modelVersion) {
        logger.info("Received get model metrics request: {}", modelName + (modelVersion == null ? "" : ", " + modelVersion));
        List<ModelMetrics> modelMetrics;
        if (modelVersion == null) {
            modelMetrics = new ArrayList<>(metricsService.getModelMetrics(modelName));
        } else {
            try {
                modelMetrics = Collections.singletonList(
                        metricsService.getModelMetrics(modelName, Double.parseDouble(modelVersion))
                );
            } catch (NumberFormatException e) {
                logger.warn("NumberFormatException parsing model version", e);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERR_INVALID_MODEL_VERSION);
            }
        }

        return ResponseEntity.ok(modelMetrics);
    }

    /**
     * Adds a new set of inference metrics for a specific model and version.
     * If the model already exists, the new metrics are appended to the existing collection.
     *
     * @param addModelMetricsRequest the request body containing model name, version, and inference metrics
     * @return HTTP 200 on success, or appropriate HTTP 400/500 on validation or server error
     */
    @PostMapping("/add_model_metrics")
    @Operation(summary = "Add Model Inference Metrics")
    public ResponseEntity<?> addModelMetrics(@RequestBody AddModelMetricsRequest addModelMetricsRequest) {
        logger.info("Received add model metrics request: {}", addModelMetricsRequest);
        if (addModelMetricsRequest == null) {
            logger.warn("Request body was null");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERR_MISSING_REQUEST_BODY);
        }

        if (addModelMetricsRequest.getRequestId() == null) {
            logger.warn("Missing request id: {}", addModelMetricsRequest);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERR_MISSING_REQUEST_ID);
        }

        if (addModelMetricsRequest.getModelName() == null) {
            logger.warn("Missing model name: {}", addModelMetricsRequest);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERR_MISSING_MODEL_NAME);
        }

        if (addModelMetricsRequest.getModelVersion() == null) {
            logger.warn("Missing model version: {}", addModelMetricsRequest);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERR_MISSING_MODEL_VERSION);
        }

        List<InferenceMetric> inferenceMetrics = addModelMetricsRequest.getInferenceMetrics();
        if (inferenceMetrics == null || inferenceMetrics.isEmpty()) {
            logger.warn("Missing model inference metrics: {}", addModelMetricsRequest);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERR_MISSING_INFERENCE_METRICS);
        }

        boolean success = metricsService.consumeMetrics(
                addModelMetricsRequest.getModelName(),
                addModelMetricsRequest.getModelVersion(),
                addModelMetricsRequest.getDeviceMetadata(),
                addModelMetricsRequest.getInferenceMetrics()
        );

        if (!success) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ERR_INTERNAL_SERVER);
        }

        return ResponseEntity.ok().build();
    }

    /**
     * Deletes model metrics for a given model name, and optionally a specific model version.
     *
     * @param modelName    required model name to delete metrics for
     * @param modelVersion optional version to delete; if omitted, all versions of the model are removed
     * @return HTTP 200 with successful deletion, or HTTP 400 if parameters are invalid
     **/
    @DeleteMapping("/delete_model_metrics")
    public ResponseEntity<?> deleteModelMetrics(@RequestParam String modelName,
                                             @RequestParam(required = false) String modelVersion) {
        logger.info("Received delete model metrics request: {}", modelName + (modelVersion == null ?
                "" : ", " + modelVersion));

        if (modelVersion == null) {
            metricsService.removeModelMetrics(modelName);
        } else {
            try {
                metricsService.removeModelMetrics(modelName, Double.parseDouble(modelVersion));
            } catch (NumberFormatException e) {
                logger.warn("NumberFormatException parsing model version", e);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERR_INVALID_MODEL_VERSION);
            }
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/get_model_inference_count")
    @Operation(summary = "Get Model Metrics")
    public ResponseEntity<?> getModelInferenceCounts(
            @RequestParam String modelName,
            @RequestParam(required = false) String modelVersion) {
        int count = 0;
        if (modelVersion == null) {
            count = metricsService.getInferenceCounts(modelName);
        } else {
            try {
                count = metricsService.getInferenceCounts(modelName, Double.parseDouble(modelVersion));
            } catch (NumberFormatException e) {
                logger.warn("NumberFormatException parsing model version", e);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERR_INVALID_MODEL_VERSION);
            }
        }
        return ResponseEntity.ok(String.valueOf(count));
    }
}
