package com.bbn.takml_server.db.metrics;

import com.bbn.takml_server.metrics.model.ModelMetrics;
import com.bbn.takml_server.metrics.model.ModelMetricsId;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ModelMetricsRepository extends CrudRepository<ModelMetrics, ModelMetricsId> {
    List<ModelMetrics> findByIdModelName(String modelName);
    ModelMetrics findByIdModelNameAndIdModelVersion(String modelName, double modelVersion);
    void deleteByIdModelNameAndIdModelVersion(String modelName, double modelVersion);
    void deleteByIdModelName(String modelName);
}
