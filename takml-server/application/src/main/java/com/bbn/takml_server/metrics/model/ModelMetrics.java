package com.bbn.takml_server.metrics.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;

@Entity
public class ModelMetrics {
    @EmbeddedId
    private ModelMetricsId id;

    // orphanRemoval: If an InferenceMetric is removed from the list and it no longer has a parent
    // (modelMetrics = null), then delete that metric row from the database.
    @OneToMany(mappedBy = "modelMetrics", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<InferenceMetric> inferenceMetricList = new ArrayList<>();

    public ModelMetrics() {
    }

    public ModelMetrics(String modelName, Double modelVersion, List<InferenceMetric> inferenceMetricList) {
        this.id = new ModelMetricsId(modelName, modelVersion);
        setInferenceMetricList(inferenceMetricList);
    }

    public ModelMetricsId getId() {
        return id;
    }

    public void setId(ModelMetricsId id) {
        this.id = id;
    }

    public String getModelName() {
        return id != null ? id.getModelName() : null;
    }

    public Double getModelVersion() {
        return id != null ? id.getModelVersion() : null;
    }

    public List<InferenceMetric> getInferenceMetricList() {
        return inferenceMetricList;
    }

    public void setModelName(String modelName) {
        if (id == null) id = new ModelMetricsId();
        id.setModelName(modelName);
    }

    public void setModelVersion(double modelVersion) {
        if (id == null) id = new ModelMetricsId();
        id.setModelVersion(modelVersion);
    }

    public void setInferenceMetricList(List<InferenceMetric> metrics) {
        // clear existing links
        if (this.inferenceMetricList != null) {
            for (InferenceMetric m : this.inferenceMetricList) {
                m.setModelMetrics(null);
            }
        }

        this.inferenceMetricList = new ArrayList<>();
        if (metrics != null) {
            for (InferenceMetric m : metrics) {
                addInferenceMetric(m);
            }
        }
    }

    public void addInferenceMetric(InferenceMetric metric) {
        if (metric == null) {
            return;
        }
        inferenceMetricList.add(metric);
        metric.setModelMetrics(this);
    }

    public void removeInferenceMetric(InferenceMetric metric) {
        if (metric == null) return;
        this.inferenceMetricList.remove(metric);
        if (metric.getModelMetrics() == this) {
            metric.setModelMetrics(null);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ModelMetrics that = (ModelMetrics) o;
        return Objects.equals(id, that.id) && Objects.equals(inferenceMetricList, that.inferenceMetricList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, inferenceMetricList);
    }

    @Override
    public String toString() {
        return "ModelMetrics{" +
                "id=" + id +
                ", inferenceMetricList=" + inferenceMetricList +
                '}';
    }
}
