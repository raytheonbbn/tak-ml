package com.atakmap.android.takml_android.takml_result;

import android.os.Parcel;

import androidx.annotation.NonNull;

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

    public Recognition(Parcel in) {
        this.label = in.readString();
        this.confidence = in.readFloat();
        this.bottom = in.readFloat();
        this.left = in.readFloat();
        this.right = in.readFloat();
        this.top = in.readFloat();
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int i) {
        dest.writeString(label);
        dest.writeFloat(confidence);
        dest.writeFloat(bottom);
        dest.writeFloat(left);
        dest.writeFloat(right);
        dest.writeFloat(top);
    }

    public static final Creator<Recognition> CREATOR = new Creator<Recognition>() {
        @Override
        public Recognition createFromParcel(Parcel in) {
            return new Recognition(in);
        }

        @Override
        public Recognition[] newArray(int size) {
            return new Recognition[size];
        }
    };
}