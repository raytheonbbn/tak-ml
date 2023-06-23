package com.atakmap.android.takml_android;

import java.util.List;

public class TakmlModel {
    // required parameters
    private final String name;
    private final List<String> labels;
    private final String modelExtension;
    private final String modelType;

    // optional parameters
    private final byte[] modelBytes;
    private ProcessingParams processingParams;

    private TakmlModel(TakmlModelBuilder builder) {
        this.name = builder.name;
        this.modelBytes = builder.modelBytes;
        this.modelExtension = builder.modelExtension;
        this.processingParams = builder.processingParams;
        this.labels = builder.labels;
        this.modelType = builder.modelType;
    }

    public String getName() {
        return name;
    }

    public byte[] getModelBytes() {
        return modelBytes;
    }

    public String getModelExtension() {
        return modelExtension;
    }

    public ProcessingParams getProcessingParams() {
        return processingParams;
    }

    public List<String> getLabels(){
        return labels;
    }

    public String getModelType() {
        return modelType;
    }

    //Builder Class
    public static class TakmlModelBuilder{
        // required parameters
        private final String name;
        private final byte[] modelBytes;
        private final String modelExtension;
        private final String modelType;

        // optional parameters
        private List<String> labels;
        private ProcessingParams processingParams;

        public TakmlModelBuilder(String name, byte[] modelBytes, String modelExtension, String modelType){
            this.name = name;
            this.modelBytes = modelBytes;
            this.modelExtension = modelExtension;
            this.modelType = modelType;
        }

        public TakmlModelBuilder setLabels(List<String> labels){
            this.labels = labels;
            return this;
        }

        public TakmlModelBuilder setProcessingParams(ProcessingParams processingParams) {
            this.processingParams = processingParams;
            return this;
        }

        public TakmlModel build(){
            return new TakmlModel(this);
        }

    }
}
