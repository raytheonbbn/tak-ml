package com.bbn.takml_server.feedback.api;

import com.bbn.takml_server.feedback.OutputErrorType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import org.springframework.web.multipart.MultipartFile;

public class AddFeedbackRequest {
    @NotBlank
    private String modelName;

    @NotNull
    private Double modelVersion;

    @NotBlank
    @Size(max = 64)
    private String callsign;

    private String inputText;

    private MultipartFile inputFile;

    @NotBlank
    private String output;

    @NotNull
    private Boolean isCorrect;

    private OutputErrorType outputErrorType;

    @Min(1)
    @Max(5)
    private Integer evaluationConfidence;

    @Min(1)
    @Max(5)
    private Integer evaluationRating;

    private String comment;

    public AddFeedbackRequest() {}

    @AssertTrue(message = "Provide either inputText OR inputFile, not both")
    public boolean isValidInput() {
        boolean hasText = inputText != null;
        boolean hasFile = inputFile != null;
        return hasText ^ hasFile;
    }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public Double getModelVersion() { return modelVersion; }
    public void setModelVersion(Double modelVersion) { this.modelVersion = modelVersion; }

    public String getCallsign() {
        return callsign;
    }
    public void setCallsign(String callsign) {
        this.callsign = callsign;
    }

    public String getInputText() {
        return inputText;
    }
    public void setInputText(String inputText) {
        this.inputText = inputText;
    }

    public MultipartFile getInputFile() {
        return inputFile;
    }
    public void setInputFile(MultipartFile inputFile) {
        this.inputFile = inputFile;
    }

    public String getOutput() {
        return output;
    }
    public void setOutput(String output) {
        this.output = output;
    }

    public Boolean getIsCorrect() { return isCorrect; }
    public void setIsCorrect(Boolean isCorrect) { this.isCorrect = isCorrect; }

    public OutputErrorType getOutputErrorType() {
        return outputErrorType;
    }
    public void setOutputErrorType(OutputErrorType outputErrorType) {
        this.outputErrorType = outputErrorType;
    }

    public Integer getEvaluationConfidence() {
        return evaluationConfidence;
    }
    public void setEvaluationConfidence(Integer evaluationConfidence) {
        this.evaluationConfidence = evaluationConfidence;
    }

    public Integer getEvaluationRating() {
        return evaluationRating;
    }
    public void setEvaluationRating(Integer evaluationRating) {
        this.evaluationRating = evaluationRating;
    }

    public String getComment() {
        return comment;
    }
    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String toString() {
        String inputFileName = inputFile != null ? inputFile.getOriginalFilename() : "";

        return "AddFeedbackRequest{" +
            "modelName='" + modelName + '\'' +
            ", modelVersion=" + modelVersion +
            ", callsign='" + callsign + '\'' +
            ", inputText='" + inputText + '\'' +
            ", inputFileName='" + inputFileName + '\'' +
            ", output='" + output + '\'' +
            ", isCorrect=" + isCorrect +
            ", outputErrorType=" + outputErrorType +
            ", evaluationConfidence=" + evaluationConfidence +
            ", evaluationRating=" + evaluationRating +
            ", comment='" + comment + '\'' +
            '}';
    }
}
