package com.atakmap.android.takml_android;

import com.atakmap.android.takml_android.tensor_processor.TensorProcessor;

import java.io.File;
import android.net.Uri;
import java.util.List;
import java.util.UUID;

public class TakmlModel {
    private final UUID modelUUID = UUID.randomUUID();

    // required parameters
    private final String name;
    private final boolean isPseudoModel;
    private final boolean isRemoteModel;

    // optional parameters
    private double versionNumber = 1; // default is 1
    private List<String> labels;
    private String modelExtension;
    private String modelType;

    private Uri modelUri;

    private ProcessingParams processingParams;
    private TensorProcessor tensorProcessor;

    private String url;
    private String api;
    private String apiKeyName;
    private String apiKey;

    private TakmlModel(TakmlModelBuilder builder) {
        this.name = builder.name;
        this.modelUri = builder.modelUri;
        this.modelExtension = builder.modelExtension;
        this.processingParams = builder.processingParams;
        this.labels = builder.labels;
        this.modelType = builder.modelType;
        this.versionNumber = builder.versionNumber;
        isPseudoModel = false;
        isRemoteModel = false;
    }

    private TakmlModel(TakmlPsuedoModelBuilder builder) {
        this.name = builder.name;
        isPseudoModel = true;
        isRemoteModel = false;
    }

    private TakmlModel(TakmlRemoteModelBuilder builder) {
        this.name = builder.name;
        this.modelType = builder.modelType;
        this.tensorProcessor = builder.tensorProcessor;
        this.url = builder.url;
        this.api = builder.api;
        this.apiKeyName = builder.apiKeyName;
        this.apiKey = builder.apiKey;
        isPseudoModel = false;
        isRemoteModel = true;
    }

    public double getVersionNumber() {
        return versionNumber;
    }

    public UUID getModelUUID() {
        return modelUUID;
    }

    public String getName() {
        return name;
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

    public boolean isPseudoModel() {
        return isPseudoModel;
    }

    public boolean isRemoteModel() {
        return isRemoteModel;
    }

    public TensorProcessor getTensorProcessor() {
        return tensorProcessor;
    }

    public String getUrl() {
        return url;
    }

    public String getApi() {
        return api;
    }

    public String getApiKeyName() {
        return apiKeyName;
    }

    public String getApiKey() {
        return apiKey;
    }
    public Uri getModelUri() {return modelUri;}

    //Builder Class
    public static class TakmlModelBuilder{
        // required parameters
        private final String name;

        private Uri modelUri;
        private final String modelExtension;
        private final String modelType;

        // optional parameters
        private List<String> labels;
        private ProcessingParams processingParams;
        private double versionNumber = 1.0;

        public TakmlModelBuilder(String name, Uri modelUri, String modelExtension, String modelType){
            this.name = name;
            this.modelUri = modelUri;
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

        public TakmlModelBuilder setVersionNumber(double versionNumber) {
            this.versionNumber = versionNumber;
            return this;
        }

        public TakmlModel build(){
            return new TakmlModel(this);
        }
    }

    public static class TakmlPsuedoModelBuilder{
        // required parameters
        private final String name;

        public TakmlPsuedoModelBuilder(String name){
            this.name = name;
        }

        public TakmlModel build(){
            return new TakmlModel(this);
        }
    }

    public static class TakmlRemoteModelBuilder{
        // required parameters
        private final String name;
        private final String modelType;
        private final TensorProcessor tensorProcessor;
        private final String url;
        private final String api;
        private String apiKeyName;
        private String apiKey;

        public TakmlRemoteModelBuilder(String name, String modelType, TensorProcessor tensorProcessor, String url, String api) {
            this.name = name;
            this.modelType = modelType;
            this.tensorProcessor = tensorProcessor;
            this.url = url;
            this.api = api;
        }

        public void setApiKey(String apiKeyName, String apiKey) {
            this.apiKeyName = apiKeyName;
            this.apiKey = apiKey;
        }

        public TakmlModel build(){
            return new TakmlModel(this);
        }
    }
}
