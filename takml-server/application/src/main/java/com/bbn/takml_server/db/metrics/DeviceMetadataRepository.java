package com.bbn.takml_server.db.metrics;

import com.bbn.takml_server.metrics.model.device_metadata.DeviceMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceMetadataRepository extends JpaRepository<DeviceMetadata, Long> {

    Optional<DeviceMetadata> findByModelAndBrandAndManufacturerAndDeviceAndProduct(
            String model,
            String brand,
            String manufacturer,
            String device,
            String product
    );
}