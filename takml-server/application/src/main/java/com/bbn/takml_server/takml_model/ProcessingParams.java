package com.bbn.takml_server.takml_model;

import java.util.Arrays;
import java.util.Objects;

public class ProcessingParams {
    private final String type = ProcessingParams.class.getName();
    private final long[] modelShape;
    private final int dimPixelWidth, dimPixelHeight;
    private final float[] mean, std;
    private final boolean normalizePixelValues;

    private final int tensorOutputNumberRows;
    private final int tensorOutputNumberColumns;

    public ProcessingParams(long[] modelShape, int dimPixelWidth,
                                int dimPixelHeight, float[] mean, float[] std, boolean normalizePixelValues,
                            int tensorOutputNumberColumns, int tensorOutputNumberRows) {
        this.modelShape = modelShape;
        this.dimPixelWidth = dimPixelWidth;
        this.dimPixelHeight = dimPixelHeight;
        this.mean = mean;
        this.std = std;
        this.normalizePixelValues = normalizePixelValues;
        this.tensorOutputNumberRows = tensorOutputNumberRows;
        this.tensorOutputNumberColumns = tensorOutputNumberColumns;
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

    public int getDimPixelHeight() {
        return dimPixelHeight;
    }

    public float[] getMean() {
        return mean;
    }

    public float[] getStd() {
        return std;
    }

    public boolean isNormalizePixelValues() {
        return normalizePixelValues;
    }

    public int getTensorOutputNumberRows() {
        return tensorOutputNumberRows;
    }

    public int getTensorOutputNumberColumns() {
        return tensorOutputNumberColumns;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ProcessingParams that = (ProcessingParams) o;
        return dimPixelWidth == that.dimPixelWidth && dimPixelHeight == that.dimPixelHeight && normalizePixelValues == that.normalizePixelValues && tensorOutputNumberRows == that.tensorOutputNumberRows && tensorOutputNumberColumns == that.tensorOutputNumberColumns && Objects.equals(type, that.type) && Objects.deepEquals(modelShape, that.modelShape) && Objects.deepEquals(mean, that.mean) && Objects.deepEquals(std, that.std);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, Arrays.hashCode(modelShape), dimPixelWidth, dimPixelHeight, Arrays.hashCode(mean), Arrays.hashCode(std), normalizePixelValues, tensorOutputNumberRows, tensorOutputNumberColumns);
    }
}