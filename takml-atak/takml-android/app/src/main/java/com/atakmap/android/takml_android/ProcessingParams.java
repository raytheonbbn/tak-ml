package com.atakmap.android.takml_android;

public abstract class ProcessingParams {
    public ProcessingParams() {
    }

    /**
     * The type of ProcessingParams, e.g.:
     * <pre>
     * this.type = this.getClass().getName();
     * </pre>
     *
     * @return class name
     */
    public abstract String getType();
}