package com.atakmap.android.takml_android.executorch_mx_plugin;

import com.atakmap.android.takml_android.ModelTypeConstants;
import com.atakmap.android.takml_android.ProcessingParams;

import java.util.Arrays;
import java.util.Objects;

public class ExecutorchProcessingParams extends ProcessingParams {
    private int modelInputWidth;
    private int modelInputHeight;

    private boolean includeCoordinates = false;


    public ExecutorchProcessingParams(int modelInputWidth, int modelInputHeight,
                                      boolean includeCoordinates) {
        this.modelInputWidth = modelInputWidth;
        this.modelInputHeight = modelInputHeight;
        this.includeCoordinates = includeCoordinates;
    }

    public boolean getIncludeCoordinates() {
        return includeCoordinates;
    }

    public void setIncludeCoordinates(boolean includeCoordinates) {
        this.includeCoordinates = includeCoordinates;
    }

    public int getModelInputWidth() {
        return this.modelInputWidth;
    }

    public void setModelInputHeight(int modelInputHeight) {
        this.modelInputHeight = modelInputHeight;
    }

    public void setModelInputWidth(int modelInputWidth) {
        this.modelInputWidth = modelInputWidth;
    }

    public int getModelInputHeight() {
        return this.modelInputHeight;
    }


    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            ExecutorchProcessingParams that = (ExecutorchProcessingParams) o;
            return this.modelInputWidth == that.modelInputWidth
                    && this.modelInputHeight == that.modelInputHeight
                    && this.includeCoordinates == that.includeCoordinates;
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(this.modelInputWidth, this.modelInputHeight, this.includeCoordinates);
    }

    @Override
    public String getType() {
        return ExecutorchProcessingParams.class.getName();
    }
}