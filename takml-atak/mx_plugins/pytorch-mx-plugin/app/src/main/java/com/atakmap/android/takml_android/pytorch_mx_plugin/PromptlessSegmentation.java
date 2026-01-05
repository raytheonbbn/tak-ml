package com.atakmap.android.takml_android.pytorch_mx_plugin;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.atakmap.android.takml_android.takml_result.TakmlResult;

import java.util.ArrayList;
import java.util.List;

public class PromptlessSegmentation extends TakmlResult {
    private List<PromptlessSegmentationDetection> detections;
    private float[] rawMasks;

    public PromptlessSegmentation() {
        this.detections = new ArrayList<>();
        this.rawMasks = new float[0];
    }

    public PromptlessSegmentation(Parcel parcel) {
        // Read list with explicit classloader (works for Parcelable or Serializable items)
        this.detections = new ArrayList<>();
        parcel.readList(this.detections, PromptlessSegmentationDetection.class.getClassLoader());

        // Read float array
        this.rawMasks = parcel.createFloatArray();
        if (this.rawMasks == null) this.rawMasks = new float[0];
    }

    public List<PromptlessSegmentationDetection> getDetections() {
        return detections;
    }

    public void setDetections(List<PromptlessSegmentationDetection> detections) {
        this.detections = (detections != null) ? detections : new ArrayList<>();
    }

    public float[] getRawMasks() {
        return rawMasks;
    }

    public void setRawMasks(float[] rawMasks) {
        this.rawMasks = (rawMasks != null) ? rawMasks : new float[0];
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        // Write list without requiring typed creators
        parcel.writeList(detections != null ? detections : new ArrayList<>());

        // Write masks
        parcel.writeFloatArray(rawMasks != null ? rawMasks : new float[0]);
    }

    public static final Creator<PromptlessSegmentation> CREATOR = new Creator<PromptlessSegmentation>() {
        @Override
        public PromptlessSegmentation createFromParcel(Parcel in) {
            return new PromptlessSegmentation(in);
        }

        @Override
        public PromptlessSegmentation[] newArray(int size) {
            return new PromptlessSegmentation[size];
        }
    };
}
