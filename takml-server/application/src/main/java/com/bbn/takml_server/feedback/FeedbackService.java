package com.bbn.takml_server.feedback;

import com.bbn.takml_server.db.feedback.ModelFeedbackRepository;
import com.bbn.takml_server.feedback.api.AddFeedbackRequest;
import com.bbn.takml_server.feedback.api.FeedbackResponse;
import com.bbn.takml_server.feedback.model.ModelFeedback;
import com.bbn.takml_server.lib.TakmlInitializationException;
import com.bbn.takml_server.metrics.model.InferenceMetric;
import com.bbn.takml_server.metrics.model.ModelMetrics;
import com.bbn.takml_server.model_management.takfs.ModelRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class FeedbackService {
    protected static final Logger logger = LogManager.getLogger(com.bbn.takml_server.feedback.FeedbackService.class);

    @Autowired
    private ModelFeedbackRepository feedbackRepository;

    @Autowired
    private ModelRepository modelRepository;

    /**
     * Initializes FeedbackService after dependency injection is complete.
     *
     * @throws TakmlInitializationException if initialization fails.
     */
    @PostConstruct
    public void initialize() throws TakmlInitializationException {
        logger.info("Initialized Metrics Service");
    }

    /**
     * Cleanup callback invoked when the Spring context is shutting down.
     * Allows this service to log clean shutdown events.
     */
    @PreDestroy
    public void shutdown() {
        logger.info("FeedbackService shutting down cleanly.");
    }

    /**
     * Saves a new feedback entry.
     *
     * @param request validated feedback request
     * @return id of saved feedback entry
     */
    @Transactional
    public String addFeedback(AddFeedbackRequest request) {
        CompletableFuture<String> future = modelRepository.addModelFeedback(request);

        try {
            String id = future.get();
            logger.info("Saved feedback with id " + id);
            return id;
        } catch (Exception e) {
            logger.error("Failed to add feedback", e);
            throw new RuntimeException("Failed to add feedback", e);
        }
    }

    /**
     * Retrieves a list of {@link FeedbackResponse} for a given model name.
     * Read-only transactional context improves query performance.
     *
     * @param modelName the model name whose records should be fetched.
     * @return a list of {@link FeedbackResponse} or an empty list if no feedback exists.
     */
    @Transactional(readOnly = true)
    public List<FeedbackResponse> getModelFeedback(String modelName) {
        List<ModelFeedback> feedback = feedbackRepository.findByModelName(modelName);
        List<FeedbackResponse> responses = new ArrayList<>();

        for (ModelFeedback modelFeedback : feedback) {
            FeedbackResponse response = FeedbackResponse.fromEntity(modelFeedback);
            responses.add(response);
        }

        return responses;
    }

    /**
     * Retrieves a list of {@link FeedbackResponse} for a specific model/version pair.
     *
     * @param modelName    the model name to fetch.
     * @param modelVersion the associated version.
     *
     * @return a list of {@link FeedbackResponse} or an empty list if no feedback exists.
     */
    @Transactional(readOnly = true)
    public List<FeedbackResponse> getModelFeedback(String modelName, Double modelVersion) {
        List<ModelFeedback> feedback = feedbackRepository
                .findByModelNameAndModelVersion(modelName, modelVersion);
        List<FeedbackResponse> responses = new ArrayList<>();

        for (ModelFeedback modelFeedback : feedback) {
            FeedbackResponse response = FeedbackResponse.fromEntity(modelFeedback);
            responses.add(response);
        }

        return responses;
    }

    /**
     * Deletes the feedback associated with a specific model/version pair.
     *
     *
     * @param modelName    model name to delete.
     * @param modelVersion version to delete.
     */
    @Transactional
    public void removeModelFeedback(String modelName, double modelVersion) {
        feedbackRepository.deleteByModelNameAndModelVersion(modelName, modelVersion);
    }

    /**
     * Deletes all metrics for the given model name across all versions.
     *
     * @param modelName model name to delete.
     */
    @Transactional
    public void removeModelFeedback(String modelName) {
        feedbackRepository.deleteByModelName(modelName);
    }
}
