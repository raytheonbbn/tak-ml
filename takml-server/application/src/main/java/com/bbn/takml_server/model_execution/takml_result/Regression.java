package com.bbn.takml_server.model_execution.takml_result;

public class Regression extends TakmlResult {
    private final double predictionResult;

    public Regression(double predictionResult) {
        this.predictionResult = predictionResult;
    }

    public double getPredictionResult() {
        return predictionResult;
    }
}
