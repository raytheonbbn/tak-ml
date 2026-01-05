package com.atakmap.android.takml_android.tensor_processor;

public class InferOutput {
    private long[] shape;
    private float[] data;

    public InferOutput() {
    }

    public InferOutput(long[] shape, float[] data) {
        this.shape = shape;
        this.data = data;
    }

    public long[] getShape() {
        return shape;
    }

    public void setShape(long[] shape) {
        this.shape = shape;
    }

    public float[] getData() {
        return data;
    }

    public void setData(float[] data) {
        this.data = data;
    }
}
