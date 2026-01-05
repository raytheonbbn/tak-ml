package com.bbn.takml_server.metrics.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ModelMetricsId implements Serializable {

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "model_version")
    private Double modelVersion;

    public ModelMetricsId() {}

    public ModelMetricsId(String modelName, Double modelVersion) {
        this.modelName = modelName;
        this.modelVersion = modelVersion;
    }

    public String getModelName() {
        return modelName;
    }

    public Double getModelVersion() {
        return modelVersion;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public void setModelVersion(Double modelVersion) {
        this.modelVersion = modelVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModelMetricsId that)) return false;
        return Double.compare(that.modelVersion, modelVersion) == 0 &&
                Objects.equals(modelName, that.modelName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelName, modelVersion);
    }

    @Override
    public String toString() {
        return "ModelMetricsId{" +
                "modelName='" + modelName + '\'' +
                ", modelVersion=" + modelVersion +
                '}';
    }
}
