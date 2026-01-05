package com.bbn.takml_server.metrics.api;

import com.bbn.takml_server.metrics.model.InferenceMetric;
import com.bbn.takml_server.metrics.model.device_metadata.DeviceMetadata;

import java.util.List;
import java.util.Objects;

public class AddModelMetricsRequest {
    private String requestId;

    private String modelName;
    private Double modelVersion;

    private DeviceMetadata deviceMetadata;
    private List<InferenceMetric> inferenceMetrics;

    public AddModelMetricsRequest() {
    }

    public AddModelMetricsRequest(String requestId, String modelName, Double modelVersion, DeviceMetadata deviceMetadata,
                                  List<InferenceMetric> inferenceMetrics) {
        this.requestId = requestId;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.deviceMetadata = deviceMetadata;
        this.inferenceMetrics = inferenceMetrics;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Double getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(Double modelVersion) {
        this.modelVersion = modelVersion;
    }

    public DeviceMetadata getDeviceMetadata() {
        return deviceMetadata;
    }

    public void setDeviceMetadata(DeviceMetadata deviceMetadata) {
        this.deviceMetadata = deviceMetadata;
    }

    public List<InferenceMetric> getInferenceMetrics() {
        return inferenceMetrics;
    }

    public void setInferenceMetrics(List<InferenceMetric> inferenceMetrics) {
        this.inferenceMetrics = inferenceMetrics;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AddModelMetricsRequest that = (AddModelMetricsRequest) o;
        return Objects.equals(requestId, that.requestId) && Objects.equals(modelName, that.modelName)
                && Objects.equals(modelVersion, that.modelVersion) && Objects.equals(deviceMetadata,
                that.deviceMetadata) && Objects.equals(inferenceMetrics, that.inferenceMetrics);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId, modelName, modelVersion, deviceMetadata, inferenceMetrics);
    }

    @Override
    public String toString() {
        return "AddModelMetricsRequest{" +
                "requestId='" + requestId + '\'' +
                ", modelName='" + modelName + '\'' +
                ", modelVersion=" + modelVersion +
                ", deviceMetadata=" + deviceMetadata +
                ", inferenceMetrics=" + inferenceMetrics +
                '}';
    }
}
