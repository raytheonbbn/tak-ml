package com.bbn.takml_server.metrics.model;

import com.bbn.takml_server.metrics.model.device_metadata.DeviceMetadata;
import com.fasterxml.jackson.annotation.JsonBackReference;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;

import java.util.Objects;

@Entity
public class InferenceMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true) // Hide this internal field from Swagger API docs / generated client
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY, // Hibernate loads DeviceMetadata only when accessed
            cascade = { CascadeType.PERSIST, CascadeType.MERGE } // Saving an InferenceMetric also persists/merges its DeviceMetadata
    )
    @JoinColumn(name = "device_metadata_id", nullable = false)
    @Schema(hidden = true)
    private DeviceMetadata deviceMetadata;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "model_name", referencedColumnName = "model_name"),
            @JoinColumn(name = "model_version", referencedColumnName = "model_version")
    })
    /*
     * @JsonBackReference — Prevents infinite recursion during JSON serialization.
     * ModelMetrics has a @JsonManagedReference collection pointing back to InferenceMetric.
     * This marks the "back" side of the relationship so Jackson skips serializing this field.
     */
    @JsonBackReference
    @Schema(hidden = true)
    private ModelMetrics modelMetrics;

    private long startMillis;
    private long durationMillis;
    private float confidence;

    public InferenceMetric() {
    }

    public InferenceMetric(long startMillis, long durationMillis, float confidence) {
        this.startMillis = startMillis;
        this.durationMillis = durationMillis;
        this.confidence = confidence;
    }

    public InferenceMetric(long startMillis,
                           long durationMillis,
                           float confidence,
                           DeviceMetadata deviceMetadata) {
        this(startMillis, durationMillis, confidence);
        this.deviceMetadata = deviceMetadata;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getStartMillis() {
        return startMillis;
    }

    public void setStartMillis(long startMillis) {
        this.startMillis = startMillis;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public void setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    public DeviceMetadata getDeviceMetadata() {
        return deviceMetadata;
    }

    public void setDeviceMetadata(DeviceMetadata deviceMetadata) {
        this.deviceMetadata = deviceMetadata;
    }

    public ModelMetrics getModelMetrics() {
        return modelMetrics;
    }

    public void setModelMetrics(ModelMetrics modelMetrics) {
        // no collection juggling here – ModelMetrics manages the list
        this.modelMetrics = modelMetrics;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InferenceMetric)) return false;
        InferenceMetric that = (InferenceMetric) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    @Override
    public String toString() {
        return "InferenceMetric{" +
                "id=" + id +
                ", startMillis=" + startMillis +
                ", durationMillis=" + durationMillis +
                ", confidence=" + confidence +
                '}';
    }
}
