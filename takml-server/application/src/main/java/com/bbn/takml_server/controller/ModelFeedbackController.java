package com.bbn.takml_server.controller;

import com.bbn.takml_server.feedback.FeedbackService;
import com.bbn.takml_server.feedback.api.AddFeedbackRequest;
import com.bbn.takml_server.feedback.api.FeedbackResponse;
import com.bbn.takml_server.metrics.MetricsService;
import com.bbn.takml_server.metrics.model.ModelMetrics;
import com.bbn.takml_server.model_management.takfs.ModelRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/model_feedback")
@Tag(name = "Model Feedback", description = "API for adding model feedback")
public class ModelFeedbackController {
    private static final Logger logger = LogManager.getLogger(ModelFeedbackController.class);

    @Autowired
    private FeedbackService feedbackService;

    /**
     * Adds a new feedback entry for a model.
     *
     * @param addFeedbackRequest validated request containing model feedback info
     * @return HTTP 200 on success, or appropriate HTTP 400/500 on validation or server error
     */
    @PostMapping(value = "/add_feedback", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Add feedback to model")
    public ResponseEntity<?> addModelFeedback(@Valid @ModelAttribute AddFeedbackRequest addFeedbackRequest) {
        logger.info("Received add model request: {}",addFeedbackRequest);

        try {
            String id = feedbackService.addFeedback(addFeedbackRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body("Saved feedback with id " + id);
        } catch (Exception e) {
            logger.error("Failed to add feedback", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add feedback");
        }
    }

    /**
     * Returns all feedback entries for a given model (based on name and optional version).
     *
     * @param modelName    the name of the model to fetch metrics for (required)
     * @param modelVersion optional version of the model; if omitted, all versions are returned
     * @return list of feedback entries
     */
    @GetMapping("/get_feedback")
    @Operation(summary = "Get all feedback for a model")
    public ResponseEntity<List<FeedbackResponse>> getFeedbackForModel(
            @RequestParam String modelName,
            @RequestParam(required = false) Double modelVersion) {
        logger.info("Received request to get all feedback for model = {}",
                modelName + (modelVersion == null ? "" : ", " + modelVersion));

        List<FeedbackResponse> feedback;

        if (modelVersion == null) {
            feedback = new ArrayList<>(feedbackService.getModelFeedback(modelName));
        } else {
            feedback = new ArrayList<>(feedbackService.getModelFeedback(modelName, modelVersion));
        }

        return ResponseEntity.ok(feedback);
    }
}
