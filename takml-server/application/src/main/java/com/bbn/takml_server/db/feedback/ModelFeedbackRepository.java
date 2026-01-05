package com.bbn.takml_server.db.feedback;

import com.bbn.takml_server.feedback.model.ModelFeedback;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ModelFeedbackRepository extends CrudRepository<ModelFeedback, String> {

    List<ModelFeedback> findByModelName(String modelName);
    List<ModelFeedback> findByModelNameAndModelVersion(String modelName, Double modelVersion);

    void deleteByModelName(String modelName);
    void deleteByModelNameAndModelVersion(String modelName, Double modelVersion);
}

