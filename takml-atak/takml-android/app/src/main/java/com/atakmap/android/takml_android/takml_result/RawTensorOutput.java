package com.atakmap.android.takml_android.takml_result;

import android.os.Parcel;

import androidx.annotation.NonNull;

public class RawTensorOutput extends TakmlResult {
    private long[] shape;
    private float[] tensorOutput;

    public RawTensorOutput() {
    }

    public RawTensorOutput(Parcel in) {
        this.shape = in.createLongArray();
        this.tensorOutput = in.createFloatArray();
    }

    public RawTensorOutput(long[] shape, float[] tensorOutput) {
        this.shape = shape;
        this.tensorOutput = tensorOutput;
    }

    public long[] getShape() {
        return shape;
    }

    public float[] getTensorOutput() {
        return tensorOutput;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeLongArray(shape);
        parcel.writeFloatArray(tensorOutput);
    }

    public static final Creator<RawTensorOutput> CREATOR = new Creator<RawTensorOutput>() {
        @Override
        public RawTensorOutput createFromParcel(Parcel in) {
            return new RawTensorOutput(in);
        }

        @Override
        public RawTensorOutput[] newArray(int size) {
            return new RawTensorOutput[size];
        }
    };
}