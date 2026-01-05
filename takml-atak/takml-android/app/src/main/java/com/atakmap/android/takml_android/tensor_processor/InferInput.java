package com.atakmap.android.takml_android.tensor_processor;

import java.util.Arrays;

public class InferInput {
    private long[] shape;
    private Object data;
    private String name;
    private String datatype;

    public InferInput(){

    }

    public InferInput(long[] shape, Object data, String name, String datatype) {
        this.shape = shape;
        this.data = data;
        this.name = name;
        this.datatype = datatype;
    }

    public InferInput(long[] shape, float[] data, String name, String datatype) {
        this.shape = shape;

        Float[] dataArray = new Float[data.length];
        for (int i = 0; i < data.length; i++) {
            dataArray[i] = data[i];
        }
        this.data = dataArray;

        this.name = name;
        this.datatype = datatype;
    }

    public long[] getShape() {
        return shape;
    }

    public void setShape(long[] shape) {
        this.shape = shape;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object[] data) {
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }
}
