package com.atakmap.android.takml.mx_framework.onnx_plugin;

import com.atakmap.android.takml_android.ProcessingParams;

import java.util.Arrays;
import java.util.Objects;

public class OnnxProcessingParams extends ProcessingParams {
    private final String type = OnnxProcessingParams.class.getName();
    private final long[] modelShape;
    private final int dimPixelWidth, dimPixHeight;
    private final float[] mean, std;

    public OnnxProcessingParams(long[] modelShape, int dimPixelWidth, int dimPixHeight, float[] mean, float[] std) {
        this.modelShape = modelShape;
        this.dimPixelWidth = dimPixelWidth;
        this.dimPixHeight = dimPixHeight;
        this.mean = mean;
        this.std = std;
    }

    public long[] getModelShape() {
        return modelShape;
    }

    public String getType() {
        return type;
    }

    public int getDimPixelWidth() {
        return dimPixelWidth;
    }

    public int getDimPixHeight() {
        return dimPixHeight;
    }

    public float[] getMean() {
        return mean;
    }

    public float[] getStd() {
        return std;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        OnnxProcessingParams that = (OnnxProcessingParams) o;
        return dimPixelWidth == that.dimPixelWidth && dimPixHeight == that.dimPixHeight && mean == that.mean && std == that.std && Objects.equals(type, that.type) && Objects.deepEquals(modelShape, that.modelShape);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, Arrays.hashCode(modelShape), dimPixelWidth, dimPixHeight, mean, std);
    }
}
