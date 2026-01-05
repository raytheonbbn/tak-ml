package com.bbn.takml_server.model_management.takfs;

import com.bbn.tak_sync_file_manager.model.IndexRow;
import com.bbn.takml_server.feedback.api.AddFeedbackRequest;
import com.bbn.takml_server.feedback.api.FeedbackResponse;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface ModelRepository {
    /**
     * Uploads TAK ML Model Wrapper
     *
     * @param takmlModelWrapper - takml model wrapper zip
     * @param requesterCallsign - callsign of uploader
     * @param runOnServer       - model should run on server
     * @param optionalSupportedDevices - what devices the model supports (optional)
     *
     * @return hash for takml model in Enterprise Sync
     */
    CompletableFuture<String> saveModelWrapper(byte[] takmlModelWrapper, String requesterCallsign, boolean runOnServer, List<String> optionalSupportedDevices);

    /**
     * Update model
     *
     * @param modelHash - hash for existing model
     * @param takmlModelWrapper - takml model wrapper zip
     * @param requesterCallsign - callsign of uploader
     * @param runOnServer - model should run on server
     * @param optionalSupportedDevices - what devices the model supports (optional)
     *
     * @return hash for takml model in Enterprise Sync
     */
    CompletableFuture<String> editModelWrapper(String modelHash, byte[] takmlModelWrapper, String requesterCallsign,
                                               boolean runOnServer, List<String> optionalSupportedDevices);

    /**
     * Update model
     * @param modelHash - hash of takml model
     *
     * @return modelHash deleted
     */
    CompletableFuture<String> removeModel(String modelHash);

    /**
     * Download model
     * @param modelHash - hash of takml model
     *
     * @return model wrapper
     */
    CompletableFuture<byte[]> downloadModel(String modelHash);

    /**
     * Get Model Metadata
     *
     * @param modelHash - hash for the model
     * @return IndexRow of model data info
     */
    IndexRow getModelMetadata(String modelHash);

    /**
     * Get all Models Metadata
     *
     * @return Set of IndexRow of model data infos
     */
    Set<IndexRow> getModelsMetadata();

    /**
     * Add feedback for a model
     *
     * @param request feedback request
     * @return ID of created feedback row (as String)
     */
    CompletableFuture<String> addModelFeedback(AddFeedbackRequest request);
}
