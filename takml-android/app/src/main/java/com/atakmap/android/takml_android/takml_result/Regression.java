package com.atakmap.android.takml_android.takml_result;

public class Regression extends TakmlResult{
    private final double predictionResult;

    public Regression(double predictionResult) {
        this.predictionResult = predictionResult;
    }

    public double getPredictionResult() {
        return predictionResult;
    }
}
