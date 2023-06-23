package com.atakmap.android.takml_android.pytorch_mx_plugin;

import com.atakmap.android.takml_android.ProcessingParams;

import java.util.Arrays;
import java.util.Objects;

public class PytorchObjectDetectionParams extends ProcessingParams {
    private int modelInputWidth;
    private int modelInputHeight;
    private int tensorOutputNumberRows;
    private int tensorOutputNumberColumns;
    private float[] normMeanRGB;
    private float[] normStdRGB;

    public PytorchObjectDetectionParams(int modelInputWidth, int modelInputHeight,
                                       int tensorOutputNumberRows, int tensorOutputNumberColumns,
                                       float[] normMeanRGB, float[] normStdRGB) {
        this.type = this.getClass().getName();
        this.modelInputWidth = modelInputWidth;
        this.modelInputHeight = modelInputHeight;
        this.tensorOutputNumberRows = tensorOutputNumberRows;
        this.tensorOutputNumberColumns = tensorOutputNumberColumns;
        this.normMeanRGB = normMeanRGB;
        this.normStdRGB = normStdRGB;
    }

    public int getModelInputWidth() {
        return this.modelInputWidth;
    }

    public void setModelInputWidth(int modelInputWidth) {
        this.modelInputWidth = modelInputWidth;
    }

    public int getModelInputHeight() {
        return this.modelInputHeight;
    }

    public void setModelInputHeight(int modelInputHeight) {
        this.modelInputHeight = modelInputHeight;
    }

    public int getTensorOutputNumberRows() {
        return this.tensorOutputNumberRows;
    }

    public void setTensorOutputNumberRows(int tensorOutputNumberRows) {
        this.tensorOutputNumberRows = tensorOutputNumberRows;
    }

    public int getTensorOutputNumberColumns() {
        return this.tensorOutputNumberColumns;
    }

    public void setTensorOutputNumberColumns(int tensorOutputNumberColumns) {
        this.tensorOutputNumberColumns = tensorOutputNumberColumns;
    }

    public float[] getNormMeanRGB() {
        return this.normMeanRGB;
    }

    public void setNormMeanRGB(float[] normMeanRGB) {
        this.normMeanRGB = normMeanRGB;
    }

    public float[] getNormStdRGB() {
        return this.normStdRGB;
    }

    public void setNormStdRGB(float[] normStdRGB) {
        this.normStdRGB = normStdRGB;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            PytorchObjectDetectionParams that = (PytorchObjectDetectionParams) o;
            return this.modelInputWidth == that.modelInputWidth
                    && this.modelInputHeight == that.modelInputHeight
                    && this.tensorOutputNumberRows == that.tensorOutputNumberRows
                    && this.tensorOutputNumberColumns == that.tensorOutputNumberColumns
                    && Arrays.equals(this.normMeanRGB, that.normMeanRGB)
                    && Arrays.equals(this.normStdRGB, that.normStdRGB);
        } else {
            return false;
        }
    }

    public int hashCode() {
        int result = Objects.hash(this.modelInputWidth, this.modelInputHeight,
                this.tensorOutputNumberRows, this.tensorOutputNumberColumns);
        result = 31 * result + Arrays.hashCode(this.normMeanRGB);
        result = 31 * result + Arrays.hashCode(this.normStdRGB);
        return result;
    }
}