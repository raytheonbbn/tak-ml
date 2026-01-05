package com.atakmap.android.takml_android.pytorch_mx_plugin;

import com.atakmap.android.takml_android.ProcessingParams;

public class PromptlessSegmentationProcParams extends ProcessingParams {
    private float threshold;
    private int minAreaPixels;
    private int inputWidth, inputHeight;
    private float expectedRangeMin;
    private float expectedRangeMax;

    private float[] mean, std;

    public PromptlessSegmentationProcParams(){
    }

    public float getExpectedRangeMin() {
	    return expectedRangeMin;
    }

    public void setExpectedRangeMin(float min) {
	    this.expectedRangeMin = min;
    }

    public float getExpectedRangeMax() {
	    return expectedRangeMax;
    }

    public void setExpectedRangeMax(float max) {
	    this.expectedRangeMax = max;
    }

    public float getThreshold() {
        return threshold;
    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }

    public int getMinAreaPixels() {
        return minAreaPixels;
    }

    public void setMinAreaPixels(int minAreaPixels) {
        this.minAreaPixels = minAreaPixels;
    }

    public int getInputWidth() {
        return inputWidth;
    }

    public void setInputWidth(int inputWidth) {
        this.inputWidth = inputWidth;
    }

    public int getInputHeight() {
        return inputHeight;
    }

    public void setInputHeight(int inputHeight) {
        this.inputHeight = inputHeight;
    }

    public float[] getMean() {
        return mean;
    }

    public void setMean(float[] mean) {
        this.mean = mean;
    }

    public float[] getStd() {
        return std;
    }

    public void setStd(float[] std) {
        this.std = std;
    }

    @Override
    public String getType() {
        return "";
    }
}
