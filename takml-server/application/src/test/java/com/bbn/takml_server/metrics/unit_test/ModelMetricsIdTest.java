package com.bbn.takml_server.metrics.unit_test;

import com.bbn.takml_server.metrics.model.ModelMetrics;
import com.bbn.takml_server.metrics.model.ModelMetricsId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link ModelMetricsId} embeddable key class.
 *
 * <p>This suite ensures that the composite identifier used for the
 * {@link ModelMetrics} entity behaves correctly with respect to
 * construction, accessors, and equality semantics.</p>
 */
public class ModelMetricsIdTest {

    @Test
    void testConstructorAndGetters() {
        ModelMetricsId id = new ModelMetricsId("A", 1.0);

        Assertions.assertEquals("A", id.getModelName());
        Assertions.assertEquals(1.0, id.getModelVersion());
    }

    @Test
    void testSetters() {
        ModelMetricsId id = new ModelMetricsId();
        id.setModelName("B");
        id.setModelVersion(2.0);

        Assertions.assertEquals("B", id.getModelName());
        Assertions.assertEquals(2.0, id.getModelVersion());
    }

    @Test
    void testEqualsAndHashCode() {
        ModelMetricsId id1 = new ModelMetricsId("X", 1.0);
        ModelMetricsId id2 = new ModelMetricsId("X", 1.0);

        Assertions.assertEquals(id1, id2);
        Assertions.assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void testToString() {
        ModelMetricsId id = new ModelMetricsId("Z", 3.0);
        Assertions.assertTrue(id.toString().contains("Z"));
    }
}
