package com.bbn.takml_server.metrics.unit_test;

import com.bbn.takml_server.metrics.model.InferenceMetric;
import com.bbn.takml_server.metrics.model.device_metadata.DeviceMetadata;
import com.bbn.takml_server.metrics.model.device_metadata.GpuInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Unit tests for the {@link DeviceMetadata} entity.
 *
 * <p>These tests validate that device metadata, GPU info, and associated
 * inference metric relationships behave as expected entirely independently
 * of persistence or controller logic.
 */
public class DeviceMetadataTest {

    @Test
    void testConstructorAndGetters() {
        GpuInfo gpu = new GpuInfo("Qualcomm", "Adreno", "v1.2");
        DeviceMetadata dm = new DeviceMetadata(
                "Pixel 8", "Google", "Google",
                "husky", "pixel_8", gpu
        );

        Assertions.assertEquals("Pixel 8", dm.getModel());
        Assertions.assertEquals("Google", dm.getBrand());
        Assertions.assertEquals("Google", dm.getManufacturer());
        Assertions.assertEquals("husky", dm.getDevice());
        Assertions.assertEquals("pixel_8", dm.getProduct());
        Assertions.assertEquals(gpu, dm.getGpuInfo());
    }

    @Test
    void testGpuInfoSetter() {
        DeviceMetadata dm = new DeviceMetadata();
        GpuInfo gpu = new GpuInfo("ARM", "Mali", "v2.0");

        dm.setGpuInfo(gpu);

        Assertions.assertEquals(gpu, dm.getGpuInfo());
    }

    @Test
    void testAddInferenceMetric() {
        DeviceMetadata dm = new DeviceMetadata();
        InferenceMetric im = new InferenceMetric(10L, 20L, 0.7f);

        dm.addInferenceMetric(im);

        Assertions.assertEquals(1, dm.getInferenceMetrics().size());
        Assertions.assertTrue(dm.getInferenceMetrics().contains(im));
        Assertions.assertEquals(dm, im.getDeviceMetadata());
    }

    @Test
    void testRemoveInferenceMetric() {
        DeviceMetadata dm = new DeviceMetadata();
        InferenceMetric im = new InferenceMetric(5L, 15L, 0.4f);

        dm.addInferenceMetric(im);
        dm.removeInferenceMetric(im);

        Assertions.assertTrue(dm.getInferenceMetrics().isEmpty());
        Assertions.assertNull(im.getDeviceMetadata());
    }

    @Test
    void testSetInferenceMetricsList() {
        DeviceMetadata dm = new DeviceMetadata();

        InferenceMetric im1 = new InferenceMetric(1, 2, 0.1f);
        im1.setDeviceMetadata(dm);
        InferenceMetric im2 = new InferenceMetric(3, 4, 0.9f);
        im2.setDeviceMetadata(dm);

        dm.setInferenceMetrics(List.of(im1, im2));


        Assertions.assertEquals(2, dm.getInferenceMetrics().size());
        Assertions.assertEquals(dm, im1.getDeviceMetadata());
        Assertions.assertEquals(dm, im2.getDeviceMetadata());
    }

    @Test
    void testEqualsAndHashCode() {
        GpuInfo gpu = new GpuInfo("Vendor", "Renderer", "1.0");

        DeviceMetadata a = new DeviceMetadata("M", "B", "Man", "Dev", "Prod", gpu);
        DeviceMetadata b = new DeviceMetadata("M", "B", "Man", "Dev", "Prod", gpu);

        Assertions.assertEquals(a, b);
        Assertions.assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void testToString() {
        DeviceMetadata dm = new DeviceMetadata(
                "Pixel", "Google", "Google",
                "husky", "pixel_8",
                new GpuInfo("Vendor", "Renderer", "1.0")
        );

        Assertions.assertTrue(dm.toString().contains("DeviceMetadata"));
    }
}
