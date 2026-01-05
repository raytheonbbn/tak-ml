package com.bbn.takml_server.feedback.api;

import com.bbn.takml_server.feedback.OutputErrorType;
import com.bbn.takml_server.feedback.InputType;
import com.bbn.takml_server.feedback.model.ModelFeedback;

import java.time.Instant;

public class FeedbackResponse {
    private Long id;
    private String modelName;
    private Double modelVersion;
    private String callsign;
    private InputType inputType;
    private String input;
    private String output;
    private Boolean isCorrect;
    private OutputErrorType outputErrorType;
    private Integer evaluationConfidence;
    private Integer evaluationRating;
    private String comment;
    private Instant createdAt;

    public static FeedbackResponse fromEntity(ModelFeedback fb) {
        FeedbackResponse r = new FeedbackResponse();
        r.setId(fb.getId());
        r.setModelName(fb.getModelName());
        r.setModelVersion(fb.getModelVersion());
        r.setCallsign(fb.getCallsign());
        r.setInputType(fb.getInputType());
        r.setInput(fb.getInput());
        r.setOutput(fb.getOutput());
        r.setIsCorrect(fb.getIsCorrect());
        r.setOutputErrorType(fb.getOutputErrorType());
        r.setEvaluationConfidence(fb.getEvaluationConfidence());
        r.setEvaluationRating(fb.getEvaluationRating());
        r.setComment(fb.getComment());
        r.setCreatedAt(fb.getCreatedAt());
        return r;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
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

    public InputType getInputType() {
        return inputType;
    }
    public void setInputType(InputType inputType) {
        this.inputType = inputType;
    }

    public String getInput() {
        return input;
    }
    public void setInput(String input) {
        this.input = input;
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

    public Instant getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
