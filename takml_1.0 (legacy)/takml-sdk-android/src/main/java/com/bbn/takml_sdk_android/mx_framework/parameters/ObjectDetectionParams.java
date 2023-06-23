package com.bbn.takml_sdk_android.mx_framework.parameters;

import com.bbn.takml_sdk_android.mx_framework.request.ReplyType;

import java.util.Arrays;
import java.util.Objects;

public class ObjectDetectionParams extends ProcessingParams {
    private int modelInputWidth;
    private int modelInputHeight;
    private int tensorOutputNumberRows;
    private int tensorOutputNumberColumns;

    private float[] normMeanRGB;
    private float[] normStdRGB;

    /**
     * Object Detection Parameters input
     *
     * @param modelInputWidth - the width of the images the ML model expects
     * @param modelInputHeight - the height of the images the ML model expects
     * @param tensorOutputNumberRows - the number of rows the ML model outputs as tensor
     * @param tensorOutputNumberColumns - the number of columns the ML model outputs as tensor
     * @param normMeanRGB - normalized mean RGB
     * @param normStdRGB - normalized Standard Deviation RGB
     */
    public ObjectDetectionParams(int modelInputWidth, int modelInputHeight,
                                 int tensorOutputNumberRows, int tensorOutputNumberColumns,
                                 float[] normMeanRGB, float[] normStdRGB) {
        this.type = getClass().getName();
        this.modelInputWidth = modelInputWidth;
        this.modelInputHeight = modelInputHeight;
        this.tensorOutputNumberRows = tensorOutputNumberRows;
        this.tensorOutputNumberColumns = tensorOutputNumberColumns;
        this.normMeanRGB = normMeanRGB;
        this.normStdRGB = normStdRGB;
    }

    public int getModelInputWidth() {
        return modelInputWidth;
    }

    public void setModelInputWidth(int modelInputWidth) {
        this.modelInputWidth = modelInputWidth;
    }

    public int getModelInputHeight() {
        return modelInputHeight;
    }

    public void setModelInputHeight(int modelInputHeight) {
        this.modelInputHeight = modelInputHeight;
    }

    public int getTensorOutputNumberRows() {
        return tensorOutputNumberRows;
    }

    public void setTensorOutputNumberRows(int tensorOutputNumberRows) {
        this.tensorOutputNumberRows = tensorOutputNumberRows;
    }

    public int getTensorOutputNumberColumns() {
        return tensorOutputNumberColumns;
    }

    public void setTensorOutputNumberColumns(int tensorOutputNumberColumns) {
        this.tensorOutputNumberColumns = tensorOutputNumberColumns;
    }

    public float[] getNormMeanRGB() {
        return normMeanRGB;
    }

    public void setNormMeanRGB(float[] normMeanRGB) {
        this.normMeanRGB = normMeanRGB;
    }

    public float[] getNormStdRGB() {
        return normStdRGB;
    }

    public void setNormStdRGB(float[] normStdRGB) {
        this.normStdRGB = normStdRGB;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObjectDetectionParams that = (ObjectDetectionParams) o;
        return modelInputWidth == that.modelInputWidth && modelInputHeight == that.modelInputHeight
                && tensorOutputNumberRows == that.tensorOutputNumberRows
                && tensorOutputNumberColumns == that.tensorOutputNumberColumns
                && Arrays.equals(normMeanRGB, that.normMeanRGB)
                && Arrays.equals(normStdRGB, that.normStdRGB);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(modelInputWidth, modelInputHeight, tensorOutputNumberRows,
                tensorOutputNumberColumns);
        result = 31 * result + Arrays.hashCode(normMeanRGB);
        result = 31 * result + Arrays.hashCode(normStdRGB);
        return result;
    }
}
