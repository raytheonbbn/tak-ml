package com.atakmap.android.takml_android.takml_result;

import android.os.Parcel;

import androidx.annotation.NonNull;

public class Regression extends TakmlResult{
    private final double predictionResult;

    public Regression(double predictionResult) {
        this.predictionResult = predictionResult;
    }

    public Regression(Parcel in) {
        predictionResult = in.readDouble();
    }

    public double getPredictionResult() {
        return predictionResult;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int i) {
        dest.writeDouble(predictionResult);
    }

    public static final Creator<Regression> CREATOR = new Creator<Regression>() {
        @Override
        public Regression createFromParcel(Parcel in) {
            return new Regression(in);
        }

        @Override
        public Regression[] newArray(int size) {
            return new Regression[size];
        }
    };
}
