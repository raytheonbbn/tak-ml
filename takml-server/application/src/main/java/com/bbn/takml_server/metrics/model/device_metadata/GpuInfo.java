package com.bbn.takml_server.metrics.model.device_metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.Objects;

@Entity
public class GpuInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true)
    private Long id;

    private String vendor;
    private String renderer;
    private String version;

    public GpuInfo() {
    }

    public GpuInfo(String vendor, String renderer, String version) {
        this.vendor = vendor;
        this.renderer = renderer;
        this.version = version;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getRenderer() {
        return renderer;
    }

    public void setRenderer(String renderer) {
        this.renderer = renderer;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GpuInfo gpuInfo = (GpuInfo) o;
        return Objects.equals(id, gpuInfo.id) && Objects.equals(vendor, gpuInfo.vendor) && Objects.equals(renderer, gpuInfo.renderer) && Objects.equals(version, gpuInfo.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, vendor, renderer, version);
    }

    @Override
    public String toString() {
        return "GpuInfo{" +
                "id=" + id +
                ", vendor='" + vendor + '\'' +
                ", renderer='" + renderer + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
