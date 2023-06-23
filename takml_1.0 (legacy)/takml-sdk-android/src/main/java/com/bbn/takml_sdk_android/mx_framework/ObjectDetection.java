package com.bbn.takml_sdk_android.mx_framework;

import java.io.Serializable;
import java.util.Objects;

/** An immutable result returned by an Object Detector describing what was recognized. */
public class ObjectDetection implements Serializable {
    /**
     * A unique identifier for what has been recognized. Specific to the class, not the instance of
     * the object.
     */
    private final String id;

    /** Display name for the recognition. */
    private final String title;

    /**
     * A sortable score for how good the recognition is relative to others. Higher should be better.
     */
    private final Float confidence;

    /**
     * The bounding area containing the object
     */
    private final int bottom;
    private final int left;
    private final int right;
    private final int top;

    public ObjectDetection(String id, String title, Float confidence, int bottom, int left,
                           int right, int top) {
        this.id = id;
        this.title = title;
        this.confidence = confidence;
        this.bottom = bottom;
        this.left = left;
        this.right = right;
        this.top = top;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Float getConfidence() {
        return confidence;
    }

    public int getBottom() {
        return bottom;
    }

    public int getLeft() {
        return left;
    }

    public int getRight() {
        return right;
    }

    public int getTop() {
        return top;
    }

    @Override
    public String toString() {
        return "ObjectDetection{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", confidence=" + confidence +
                ", bottom=" + bottom +
                ", left=" + left +
                ", right=" + right +
                ", top=" + top +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObjectDetection that = (ObjectDetection) o;
        return bottom == that.bottom && left == that.left && right == that.right && top == that.top
                && Objects.equals(id, that.id) && Objects.equals(title, that.title)
                && Objects.equals(confidence, that.confidence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, confidence, bottom, left, right, top);
    }
}
