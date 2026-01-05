package com.bbn.takml_server.metrics.unit_test;

import com.bbn.takml_server.metrics.model.InferenceMetric;
import com.bbn.takml_server.metrics.model.ModelMetrics;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for the {@link ModelMetrics} entity.
 *
 * This test suite provides direct, isolated verification of the core data model
 * behavior for the ModelMetrics class. These tests focus on functionality that is
 * not exercised through higher-level controller or service integration tests, and
 * help ensure strong coverage of the underlying domain object.</p>
 */
public class ModelMetricsTest {

    @Test
    void testConstructorAndGetters() {
        List<InferenceMetric> metrics = List.of(
                new InferenceMetric(1L, 2L, 0.9f)
        );

        ModelMetrics mm = new ModelMetrics("modelA", 1.0, metrics);

        Assertions.assertEquals("modelA", mm.getModelName());
        Assertions.assertEquals(1.0, mm.getModelVersion());
        Assertions.assertEquals(1, mm.getInferenceMetricList().size());

        // Ensure back-reference is populated
        Assertions.assertNotNull(mm.getInferenceMetricList().getFirst().getModelMetrics());
    }

    @Test
    void testAddInferenceMetric() {
        ModelMetrics mm = new ModelMetrics("m", 1.0, new ArrayList<>());

        InferenceMetric im = new InferenceMetric(10L, 20L, 0.5f);
        mm.addInferenceMetric(im);

        Assertions.assertEquals(1, mm.getInferenceMetricList().size());
        Assertions.assertEquals(mm, im.getModelMetrics());
    }

    @Test
    void testEqualsAndHashCode() {
        ModelMetrics mm1 = new ModelMetrics("A", 1.0, new ArrayList<>());
        ModelMetrics mm2 = new ModelMetrics("A", 1.0, new ArrayList<>());

        Assertions.assertEquals(mm1, mm2);
        Assertions.assertEquals(mm1.hashCode(), mm2.hashCode());
    }

    @Test
    void testSetters() {
        ModelMetrics mm = new ModelMetrics();
        mm.setModelName("X");
        mm.setModelVersion(2.0);

        Assertions.assertEquals("X", mm.getModelName());
        Assertions.assertEquals(2.0, mm.getModelVersion());
    }
}
