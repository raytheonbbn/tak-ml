package com.atakmap.android.takml.mx_framework;

import java.util.List;

public class DetectionResults {
    private List<DetectionResult> detectionResultList;

    public DetectionResults() {
    }

    public DetectionResults(List<DetectionResult> detectionResultList) {
        this.detectionResultList = detectionResultList;
    }

    public List<DetectionResult> getDetectionResultList() {
        return detectionResultList;
    }

    public void setDetectionResultList(List<DetectionResult> detectionResultList) {
        this.detectionResultList = detectionResultList;
    }
}
