package com.bbn.takml_server.feedback.model;

import com.bbn.takml_server.feedback.OutputErrorType;
import com.bbn.takml_server.feedback.InputType;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "model_feedback")
public class ModelFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "model_name", nullable = false)
    private String modelName;

    @Column(name = "model_version", nullable = false)
    private Double modelVersion;

    @Column(name = "callsign", nullable = false)
    private String callsign;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type", nullable = false)
    private InputType inputType;

    @Column(name = "input", nullable = false)
    private String input;

    @Column(name = "output", nullable = false)
    private String output;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    @Enumerated(EnumType.STRING)
    @Column(name = "output_error_type")
    private OutputErrorType outputErrorType;

    @Column(name = "evaluation_confidence")
    private Integer evaluationConfidence;

    @Column(name = "evaluation_rating")
    private Integer evaluationRating;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public ModelFeedback() {}

    public Long getId() {
        return id;
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
