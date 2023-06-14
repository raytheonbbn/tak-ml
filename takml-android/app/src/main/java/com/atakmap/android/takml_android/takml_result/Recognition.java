package com.atakmap.android.takml_android.takml_result;

public class Recognition extends TakmlResult {
    private final String label;
    private final float confidence;

    private float bottom;
    private float left;
    private float right;
    private float top;

    /**
     * This constructor is applicable for Classification
     *
     * @param label - the classified label
     * @param confidence - confidence of classification
     */
    public Recognition(String label, Float confidence) {
        this.label = label;
        this.confidence = confidence;
    }

    /**
     * This constructor is applicable for Object Detection where the resulting recognition has a
     * specific position relative to the image.
     *
     * @param label - the object detected label
     * @param confidence - confidence of detection
     * @param bottom - bottom position of object relative to image
     * @param left - left position of object relative to image
     * @param right - right position of object relative to image
     * @param top - top position of object relative to image
     */
    public Recognition(String label, Float confidence, float bottom, float left, float right, float top) {
        this(label, confidence);
        this.bottom = bottom;
        this.left = left;
        this.right = right;
        this.top = top;
    }

    public String getLabel() {
        return label;
    }

    public float getConfidence() {
        return confidence;
    }

    public float getBottom() {
        return this.bottom;
    }

    public float getLeft() {
        return this.left;
    }

    public float getRight() {
        return this.right;
    }

    public float getTop() {
        return this.top;
    }
}