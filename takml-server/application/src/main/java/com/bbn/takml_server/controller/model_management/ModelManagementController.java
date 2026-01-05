package com.bbn.takml_server.controller.model_management;

import com.bbn.tak_sync_file_manager.model.IndexRow;
import com.bbn.takml_server.feedback.FeedbackService;
import com.bbn.takml_server.metrics.MetricsService;
import com.bbn.takml_server.model_management.api.AddTakmlModelWrapperRequest;
import com.bbn.takml_server.model_management.api.EditTakmlModelWrapperRequest;
import com.bbn.takml_server.model_management.takfs.ModelRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.bbn.takml_server.takml_model.MetadataConstants.*;

@RestController
@RequestMapping("/model_management")
@Tag(name = "Model Management", description = "API for managing TAK ML models")
public class ModelManagementController {
    private static final Logger logger = LogManager.getLogger(ModelManagementController.class);

    @Autowired
    private ModelRepository modelRepository;

    @Autowired
    private MetricsService metricsService;

    @Autowired
    private FeedbackService feedbackService;

    public ModelManagementController(ModelRepository modelRepository, MetricsService metricsService, FeedbackService feedbackService) {
        this.modelRepository = modelRepository;
        this.metricsService = metricsService;
        this.feedbackService = feedbackService;
    }

    @GetMapping("/get_models")
    @Operation(summary = "Get metadata for all models")
    public ResponseEntity<Set<IndexRow>> getModels() {
        return ResponseEntity.ok(modelRepository.getModelsMetadata());
    }

    // TODO: support searching for modelName and modelType as url arguments
    @GetMapping("/search")
    @Operation(summary = "Search Models")
    public ResponseEntity<Set<IndexRow>> searchModels(
            @RequestParam(required = false) String modelName,
            @RequestParam(required = false) String modelType) {
        Set<IndexRow> models = modelRepository.getModelsMetadata();

        if (modelName != null && !modelName.isEmpty()) {
            models = models.stream()
                    .filter(r -> r.getName() != null &&
                            r.getName().toLowerCase().contains(modelName.toLowerCase()))
                    .collect(Collectors.toSet());
        }

        if (modelType != null && !modelType.isEmpty()) {
            models = models.stream()
                    .filter(r -> r.getAdditionalMetadata() != null &&
                            r.getAdditionalMetadata().get(MODEL_TYPE_META).toLowerCase().contains(modelType.toLowerCase()))
                    .collect(Collectors.toSet());
        }

        return ResponseEntity.ok(models);
    }

    @GetMapping("/get_model_metadata/{modelHash}")
    @Operation(summary = "Get metadata for a specific model by model hash")
    public ResponseEntity<IndexRow> getModelMetadata(@PathVariable String modelHash) {
        IndexRow row = modelRepository.getModelMetadata(modelHash);

        if (row == null) {
            return ResponseEntity.notFound().build();
        }

        // Safely get or create mutable metadata map
        Map<String, String> metadata = row.getAdditionalMetadata();
        if (metadata == null) {
            metadata = new HashMap<>();
        } else if (!(metadata instanceof HashMap)) {
            // In case it's an unmodifiable or special map, copy to a real one
            metadata = new HashMap<>(metadata);
        }

        String modelName = row.getName();
        String modelVersion = metadata.get(VERSION_META);

        int count = 0;
        if (modelName != null && modelVersion != null) {
            try {
                double version = Double.parseDouble(modelVersion);
                count = metricsService.getInferenceCounts(modelName, version);
            } catch (NumberFormatException e) {
                logger.error("NumberFormatException parsing modelVersion: {}", modelVersion, e);
            }
        } else {
            logger.warn("Missing modelName or VERSION_META for modelHash {}", modelHash);
        }

        metadata.put(MODEL_INFERENCE_COUNT, String.valueOf(count));

        // write the updated map back to the IndexRow
        row.setAdditionalMetadata(metadata);

        return ResponseEntity.ok(row);
    }


    @GetMapping("/get_model/{modelHash}")
    @Operation(summary = "Download a model binary by model hash")
    public ResponseEntity<byte[]> downloadModel(@PathVariable String modelHash) {
        CompletableFuture<byte[]> future = modelRepository.downloadModel(modelHash);
        CompletableFuture.allOf(future).join();
        try {
            byte[] ret = future.get();
            if (ret == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(ret);
        } catch (InterruptedException e) {
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
        } catch (ExecutionException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping(
            value = "/add_model_wrapper",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(summary = "Add a new TAKML model, returns id for TAK ML Model")
    public ResponseEntity<?> addModel(@ModelAttribute AddTakmlModelWrapperRequest takmlModelRequest) {
        logger.info("Received add model request");

        if (takmlModelRequest.getTakmlModelWrapper() == null) {
            logger.error("Missing Model");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing Model");
        }

        if (takmlModelRequest.getRequesterCallsign() == null) {
            logger.error("Missing Callsign");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing Requester Callsign");
        }

        // Normalize input into a List
        if(takmlModelRequest.getSupportedDevices() != null) {
            takmlModelRequest.setSupportedDevices(
                    takmlModelRequest.getSupportedDevices().stream()
                            .flatMap(devices -> Stream.of(devices.split(",")))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList()
            );
        }

        CompletableFuture<String> future = null;
        try {
            future = modelRepository.saveModelWrapper(
                    takmlModelRequest.getTakmlModelWrapper().getBytes(),
                    takmlModelRequest.getRequesterCallsign(),
                    takmlModelRequest.getRunOnServer(),
                    takmlModelRequest.getSupportedDevices());
        } catch (IOException e) {
            logger.error("IOException parsing input model bytes", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid model");
        }

        CompletableFuture.allOf(future).join();

        try {
            String modelHash = future.get();
            return ResponseEntity.of(Optional.of(modelHash));
        } catch (InterruptedException e) {
            logger.error("Request timed out", e);
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body("Timeout while saving model");
        } catch (ExecutionException e) {
            logger.error("Error saving model", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error saving model");
        }
    }

    @PostMapping(
            value = "/edit_model_wrapper/{modelHash}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(summary = "Edit or replace an existing TAKML model by model hash")
    public ResponseEntity<?> editModel(@PathVariable String modelHash, @ModelAttribute EditTakmlModelWrapperRequest takmlModelRequest) {
        logger.info("Received edit model request for hash: {}", modelHash);

        if (takmlModelRequest.getTakmlModelWrapper() == null) {
            logger.error("Missing Model");
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Missing Model");
        }

        if (takmlModelRequest.getRequesterCallsign() == null) {
            logger.error("Missing Callsign");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing Requester Callsign");
        }

        // Normalize input into a List
        if(takmlModelRequest.getSupportedDevices() != null) {
            takmlModelRequest.setSupportedDevices(
                    takmlModelRequest.getSupportedDevices().stream()
                            .flatMap(devices -> Stream.of(devices.split(",")))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList()
            );
        }

        CompletableFuture<String> future = null;
        try {
            future = modelRepository.editModelWrapper(
                    modelHash,
                    takmlModelRequest.getTakmlModelWrapper().getBytes(),
                    takmlModelRequest.getRequesterCallsign(),
                    takmlModelRequest.getRunOnServer(),
                    takmlModelRequest.getSupportedDevices()
            );
        } catch (IOException e) {
            logger.error("IOException parsing input model bytes", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid model");
        }

        CompletableFuture.allOf(future).join();

        try {
            String hash = future.get();
            return ResponseEntity.of(Optional.of(hash));
        } catch (InterruptedException e) {
            logger.error("Request timed out", e);
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body("Timeout while saving model");
        } catch (ExecutionException e) {
            logger.error("Error saving model", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error saving model");
        }
    }

    @DeleteMapping("/remove_model/{hash}")
    @Operation(summary = "Remove a model by hash")
    public ResponseEntity<?> removeModel(@PathVariable String hash) {
        logger.info("Received remove model request: {}", hash);
        if (hash == null || hash.isEmpty()) {
            return ResponseEntity.badRequest().body("Missing hash");
        }

        IndexRow row = modelRepository.getModelMetadata(hash);
        if (row == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Model not found");
        }

        // remove feedback for model by name and version
        try {
            Map<String, String> additionalMetadata = row.getAdditionalMetadata();
            String version = additionalMetadata != null ? additionalMetadata.get(VERSION_META) : null;

            if (version != null && !version.isEmpty()) {
                feedbackService.removeModelFeedback(row.getName(), Double.parseDouble(version));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Model metadata is invalid (missing version); model was not removed");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to remove model feedback; model was not removed");
        }

        CompletableFuture<String> future = modelRepository.removeModel(hash);
        CompletableFuture.allOf(future).join();

        try {
            String result = future.get();
            return ResponseEntity.ok(Map.of("message", "Removed", "result", result));
        } catch (Exception e) {
            logger.error("Failed to remove model", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to remove model");
        }
    }
}
