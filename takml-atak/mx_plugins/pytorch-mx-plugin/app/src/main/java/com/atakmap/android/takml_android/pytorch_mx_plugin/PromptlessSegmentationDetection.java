package com.atakmap.android.takml_android.pytorch_mx_plugin;

public class PromptlessSegmentationDetection {
    private float[] coordinates;
    private String label;
    private float confidence;

    public PromptlessSegmentationDetection(){
    }

    public float[] getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(float[] coordinates) {
        this.coordinates = coordinates;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

}
