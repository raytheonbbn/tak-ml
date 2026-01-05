package com.bbn.takml_server.metrics.model.device_metadata;

import com.bbn.takml_server.metrics.model.InferenceMetric;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/*
 * @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}):
 *   Applied to this class AND automatically to Hibernate proxy subclasses.
 *   It prevents Jackson from trying to serialize internal Hibernate proxy fields:
 *     - hibernateLazyInitializer (ByteBuddy interceptor)
 *     - handler
 *   Without this, GET endpoints throw:
 *       InvalidDefinitionException: No serializer found for ByteBuddyInterceptor
 *   because Jackson attempts to serialize Hibernate’s lazy-loading internals.
 */
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(
        uniqueConstraints = @UniqueConstraint(
                name = "uk_device_metadata_business_key",
                columnNames = { "model", "brand", "manufacturer", "device", "product" }
        )
)
public class DeviceMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true)
    private Long id;

    private String model;
    private String brand;
    private String manufacturer;
    private String device;
    private String product;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private GpuInfo gpuInfo;

    @OneToMany(mappedBy = "deviceMetadata",
            cascade = CascadeType.ALL
    )
    @JsonIgnore
    @Schema(hidden = true)
    private List<InferenceMetric> inferenceMetrics = new ArrayList<>();

    public DeviceMetadata() {}

    public DeviceMetadata(String model, String brand, String manufacturer,
                          String device, String product, GpuInfo gpuInfo) {
        this.model = model;
        this.brand = brand;
        this.manufacturer = manufacturer;
        this.device = device;
        this.product = product;
        this.gpuInfo = gpuInfo;
    }

    // helpers
    public void addInferenceMetric(InferenceMetric metric) {
        if (metric == null) return;
        if (!inferenceMetrics.contains(metric)) {
            inferenceMetrics.add(metric);
        }
        metric.setDeviceMetadata(this);
    }

    public void removeInferenceMetric(InferenceMetric metric) {
        if (metric == null) return;
        inferenceMetrics.remove(metric);
        if (metric.getDeviceMetadata() == this) {
            metric.setDeviceMetadata(null);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public GpuInfo getGpuInfo() {
        return gpuInfo;
    }

    public void setGpuInfo(GpuInfo gpuInfo) {
        this.gpuInfo = gpuInfo;
    }

    public List<InferenceMetric> getInferenceMetrics() {
        return inferenceMetrics;
    }

    public void setInferenceMetrics(List<InferenceMetric> inferenceMetrics) {
        this.inferenceMetrics = inferenceMetrics;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeviceMetadata that)) return false;
        return Objects.equals(model, that.model) &&
                Objects.equals(brand, that.brand) &&
                Objects.equals(manufacturer, that.manufacturer) &&
                Objects.equals(device, that.device) &&
                Objects.equals(product, that.product);
    }

    @Override
    public int hashCode() {
        return Objects.hash(model, brand, manufacturer, device, product);
    }
}
