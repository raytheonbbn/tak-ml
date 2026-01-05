package com.bbn.takml_server.metrics;

import com.bbn.takml_server.db.metrics.DeviceMetadataRepository;
import com.bbn.takml_server.metrics.model.InferenceMetric;
import com.bbn.takml_server.metrics.model.ModelMetrics;
import com.bbn.takml_server.db.metrics.ModelMetricsRepository;
import com.bbn.takml_server.lib.TakmlInitializationException;
import com.bbn.takml_server.metrics.model.device_metadata.DeviceMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

@Service
public class MetricsService {
    protected static final Logger logger = LogManager.getLogger(MetricsService.class);

    @Autowired
    private ModelMetricsRepository modelMetricsRepository;

    @Autowired
    private DeviceMetadataRepository deviceMetadataRepository;

    /**
     * Initializes the MetricsService after dependency injection is complete.
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
        logger.info("MetricsService shutting down cleanly.");
    }

    /**
     * Consumes a batch of inference metrics for a given model name/version pair.
     *
     * <p>This method performs several key responsibilities:
     * <ul>
     *   <li>Fetches or creates the {@link ModelMetrics} parent entity.</li>
     *   <li>Deduplicates {@link DeviceMetadata} by querying business-key fields
     *       (model, brand, manufacturer, device, product).</li>
     *   <li>Associates all provided {@link InferenceMetric} instances with the
     *       resolved device metadata and the model/version pair.</li>
     *   <li>Persists all metrics in a single atomic transactional operation.</li>
     * </ul>
     *
     * <p>The device metadata lookup follows:
     * <pre>
     *     if (existing device metadata exists) → reuse the existing entity
     *     else → persist a new DeviceMetadata instance
     * </pre>
     *
     * @param modelName        model name key identifying the model.
     * @param modelVersion     version number for the model.
     * @param deviceMetadata   metadata describing the device which generated these metrics.
     *                         May be {@code null}, in which case metrics will not be linked
     *                         to a device.
     * @param inferenceMetrics collection of inference metrics to persist. Must not be {@code null}.
     *
     * @return {@code true} always, indicating that metrics were successfully handled. T
     */
    @Transactional
    public boolean consumeMetrics(String modelName,
                                  Double modelVersion,
                                  DeviceMetadata deviceMetadata,
                                  List<InferenceMetric> inferenceMetrics) {

        if (modelName == null) {
            logger.error("Model name must not be null");
            return false;
        }
        if (modelVersion == null) {
            logger.error("Model version must not be null");
            return false;
        }
        if (inferenceMetrics == null) {
            logger.error("Inference metrics must not be null");
            return false;
        }
        // device metadata may be null, since it is optionally included

        ModelMetrics modelMetrics =
                modelMetricsRepository.findByIdModelNameAndIdModelVersion(modelName, modelVersion);

        if (modelMetrics == null) {
            modelMetrics = new ModelMetrics(modelName, modelVersion, new ArrayList<>());
        }

        // Find or create DeviceMetadata
        DeviceMetadata attachedDevice = null;
        if (deviceMetadata != null) {
            attachedDevice = deviceMetadataRepository
                    .findByModelAndBrandAndManufacturerAndDeviceAndProduct(
                            deviceMetadata.getModel(),
                            deviceMetadata.getBrand(),
                            deviceMetadata.getManufacturer(),
                            deviceMetadata.getDevice(),
                            deviceMetadata.getProduct()
                    )
                    .orElse(deviceMetadata); // reuse existing or use new instance
        }

        // Wire inferences to parent and device metadata
        for (InferenceMetric metric : inferenceMetrics) {
            metric.setDeviceMetadata(attachedDevice);
            modelMetrics.addInferenceMetric(metric);
        }

        modelMetricsRepository.save(modelMetrics);

        return true;
    }

    /**
     * Retrieves all {@link ModelMetrics} entries for a given model name.
     * Read-only transactional context improves query performance.
     *
     * @param modelName the model name whose records should be fetched.
     * @return a list of matching {@link ModelMetrics} or an empty list if none exist.
     */
    @Transactional(readOnly = true)
    public List<ModelMetrics> getModelMetrics(String modelName) {
        List<ModelMetrics> modelMetrics = modelMetricsRepository.findByIdModelName(modelName);
        if (modelMetrics == null || modelMetrics.isEmpty()) {
            logger.warn("Model Metrics do not exist for model '{}'", modelName);
        }
        return modelMetrics;
    }

    /**
     * Retrieves the {@link ModelMetrics} record for a specific model/version pair.
     *
     * @param modelName    the model name to fetch.
     * @param modelVersion the associated version.
     *
     * @return a matching {@link ModelMetrics} instance or {@code null} if none exist.
     */
    @Transactional(readOnly = true)
    public ModelMetrics getModelMetrics(String modelName, double modelVersion) {
        ModelMetrics modelMetrics =
                modelMetricsRepository.findByIdModelNameAndIdModelVersion(modelName, modelVersion);

        if (modelMetrics == null) {
            logger.warn("Model Metrics do not exist for model '{}' with version '{}'",
                    modelName, modelVersion);
        }
        return modelMetrics;
    }

    /**
     * Fetches all model metrics records from the database.
     *
     * <p>Useful for administrative screens or tests where the full set
     * of stored model metrics needs to be inspected.
     *
     * @return a list containing all {@link ModelMetrics} rows in the database.
     */
    @Transactional(readOnly = true)
    public List<ModelMetrics> getAllModelMetrics() {
        List<ModelMetrics> allMetrics = new ArrayList<>();
        modelMetricsRepository.findAll().forEach(allMetrics::add);

        if (allMetrics.isEmpty()) {
            logger.warn("No ModelMetrics records found in database.");
        } else {
            logger.info("Fetched {} total ModelMetrics records.", allMetrics.size());
        }

        return allMetrics;
    }

    /**
     * Deletes the metrics associated with a specific model/version pair.
     *
     * <p>The deletion cascades because {@link ModelMetrics} is configured with
     * <code>cascade = CascadeType.ALL</code> and <code>orphanRemoval = true</code>,
     * ensuring all dependent {@link InferenceMetric} entries are removed.
     *
     * @param modelName    model name to delete.
     * @param modelVersion version to delete.
     */
    @Transactional
    public void removeModelMetrics(String modelName, double modelVersion) {
        modelMetricsRepository.deleteByIdModelNameAndIdModelVersion(modelName, modelVersion);
    }

    /**
     * Deletes all metrics for the given model name across all versions.
     *
     * <p>Useful when a model is deprecated and its metrics should be fully removed.
     *
     * @param modelName model name to delete.
     */
    @Transactional
    public void removeModelMetrics(String modelName) {
        modelMetricsRepository.deleteByIdModelName(modelName);
    }

    /**
     * Returns the number of inferences recorded for a given model and model version
     *
     * @param modelName - required
     * @param modelVersion - required
     *
     * @return raw count
     */
    public int getInferenceCounts(String modelName, double modelVersion){
        ModelMetrics modelMetrics = getModelMetrics(modelName, modelVersion);
        if(modelMetrics == null){
            return 0;
        }
        return modelMetrics.getInferenceMetricList().size();
    }

    /**
     * Returns the number of inferences recorded for a given model
     *
     * @param modelName - required
     *
     * @return raw count
     */
    public int getInferenceCounts(String modelName){
        List<ModelMetrics> modelMetricsList = getModelMetrics(modelName);
        if(modelMetricsList == null){
            return 0;
        }
        int count = 0;
        for(ModelMetrics modelMetrics : modelMetricsList){
            count += modelMetrics.getInferenceMetricList().size();
        }
        return count;
    }
}