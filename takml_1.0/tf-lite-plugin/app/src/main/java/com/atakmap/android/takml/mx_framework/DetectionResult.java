package com.atakmap.android.takml.mx_framework;

import android.graphics.RectF;

import org.tensorflow.lite.support.label.Category;

import java.io.Serializable;

public class DetectionResult implements Serializable {
    private String category;
    private float score;
    private float bottom;
    private float left;
    private float right;
    private float top;

    public DetectionResult() {
    }

    public DetectionResult(String category, float score, float bottom, float left, float right, float top) {
        this.category = category;
        this.score = score;
        this.bottom = bottom;
        this.left = left;
        this.right = right;
        this.top = top;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public float getBottom() {
        return bottom;
    }

    public void setBottom(float bottom) {
        this.bottom = bottom;
    }

    public float getLeft() {
        return left;
    }

    public void setLeft(float left) {
        this.left = left;
    }

    public float getRight() {
        return right;
    }

    public void setRight(float right) {
        this.right = right;
    }

    public float getTop() {
        return top;
    }

    public void setTop(float top) {
        this.top = top;
    }
}
