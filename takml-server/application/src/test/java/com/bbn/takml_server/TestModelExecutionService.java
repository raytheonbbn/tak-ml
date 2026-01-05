package com.bbn.takml_server;

import com.bbn.takml_server.model_execution.ModelExecutionService;
import com.bbn.takml_server.takml_model.TakmlModel;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@Primary
public class TestModelExecutionService extends ModelExecutionService {
    // overrides for testing
    public Pair<long[], long[]> importTakmlModel(TakmlModel takmlModel, File modelDir){
        synchronized (takmlModels) {
            takmlModels.add(takmlModel);
        }

        logger.info("Imported takml model: " + takmlModel.getName());

        takmlModelNameToWrapperFolderName.put(takmlModel.getName(), modelDir.getPath());

        return null;
    }
}
