package com.bbn.takml_server.metrics.unit_test;

import com.bbn.takml_server.metrics.model.InferenceMetric;
import com.bbn.takml_server.metrics.model.ModelMetrics;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link InferenceMetric} entity.
 *
 * <p>These tests validate the correctness of the data model responsible for
 * representing individual inference timing and confidence measurements. They
 * ensure predictable behavior of this entity independent of any surrounding
 * persistence layers or controller logic.</p>
 */
public class InferenceMetricTest {

    @Test
    void testConstructorAndGetters() {
        InferenceMetric im = new InferenceMetric(1L, 2L, 0.8f);

        Assertions.assertEquals(1L, im.getStartMillis());
        Assertions.assertEquals(2L, im.getDurationMillis());
        Assertions.assertEquals(0.8f, im.getConfidence());
    }

    @Test
    void testModelMetricsSetter() {
        InferenceMetric im = new InferenceMetric(3, 4, 0.1f);
        ModelMetrics mm = new ModelMetrics("M", 1.0, null);

        im.setModelMetrics(mm);

        Assertions.assertEquals(mm, im.getModelMetrics());
    }

    @Test
    void testEqualsAndHashCode() {
        InferenceMetric a = new InferenceMetric(1, 2, 0.5f);
        InferenceMetric b = new InferenceMetric(1, 2, 0.5f);

        Assertions.assertEquals(a, b);
        Assertions.assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void testToString() {
        InferenceMetric im = new InferenceMetric(1, 2, 0.9f);
        Assertions.assertTrue(im.toString().contains("InferenceMetric"));
    }
}
