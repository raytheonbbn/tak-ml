package com.bbn.takml_server.controller;

import com.bbn.tak_sync_file_manager.model.IndexRow;
import com.bbn.takml_server.model_execution.ModelExecutionService;
import com.bbn.takml_server.model_execution.api.ApiUtil;
import com.bbn.takml_server.model_execution.api.V2Api;
import com.bbn.takml_server.model_execution.api.model.InferInput;
import com.bbn.takml_server.model_execution.api.model.InferOutput;
import com.bbn.takml_server.model_execution.api.model.InferenceRequest;
import com.bbn.takml_server.model_execution.api.model.InferenceResponse;
import com.bbn.takml_server.model_execution.api.model.ModelMetadataResponse;
import com.bbn.takml_server.model_execution.api.model.ServerMetadataResponse;
import com.bbn.takml_server.model_execution.takml_result.TakmlResult;
import com.bbn.takml_server.model_management.takfs.ModelRepository;
import com.bbn.takml_server.takml_model.TakmlModel;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.bbn.takml_server.takml_model.MetadataConstants.*;

@Controller
@RequestMapping("/")
@Tag(name = "Model Execution", description = "API for model inference execution with TAK ML models on server")
public class ModelExecutionController implements V2Api {
    private static final Logger logger = LogManager.getLogger(ModelExecutionController.class);

    @Autowired
    ModelExecutionService modelExecutionService;

    @Autowired
    ModelRepository modelRepository;

    @Override
    public ResponseEntity<ModelMetadataResponse> getModelVersionMetadata(String modelName, String modelVersion) throws Exception {
        Set<IndexRow> models = modelRepository.getModelsMetadata();
        for(IndexRow indexRow : models){
            String version = indexRow.getAdditionalMetadata().get(VERSION_META);
            if(indexRow.getName().equalsIgnoreCase(modelName) && version.equalsIgnoreCase(modelVersion)){
                ModelMetadataResponse response = new ModelMetadataResponse();
                response.setName(indexRow.getName());
                response.setVersions(Collections.singletonList(version));

                String platforms = indexRow.getAdditionalMetadata().get(SUPPORTED_DEVICES_META);
                String runOnServer = indexRow.getAdditionalMetadata().get(RUN_ON_SERVER_META);
                if(platforms != null) {
                    response.setPlatform(platforms);
                }else if(runOnServer != null && runOnServer.equalsIgnoreCase("true")){
                    response.setPlatform("Server");
                }

                return ResponseEntity.ok(response);
            }
        }
        return ResponseEntity.notFound().build();
    }

    @Override
    public ResponseEntity<Void> getModelVersionReady(String modelName, String modelVersion) throws Exception {
        return V2Api.super.getModelVersionReady(modelName, modelVersion);
    }

    @Override
    public ResponseEntity<ServerMetadataResponse> getServerMetadata() throws Exception {
        return V2Api.super.getServerMetadata();
    }

    @Override
    public ResponseEntity<Void> getV2HealthLive() throws Exception {
        return V2Api.super.getV2HealthLive();
    }

    /**
     * POST /v2/models/{MODEL_NAME}/versions/{MODEL_VERSION}/infer : Inference
     * Performs inference using a specific version of a model.
     *
     * @param modelName Name of the model (required)
     * @param modelVersion Version of the model (required)
     * @param inferenceRequest Inference request payload (required)
     * @return Inference response (status code 200)
     *         or Bad Request (status code 400)
     *         or Model or version not found (status code 404)
     */
    @Override
    public ResponseEntity<InferenceResponse> postModelVersionInfer(String modelName, String modelVersion, InferenceRequest inferenceRequest) throws Exception {
        logger.info("Received postModelVersionInfer request: {}", inferenceRequest.getId());
        // assuming one inference input request at a time
        InferInput inferInput = inferenceRequest.getInputs().getFirst();

        Pair<List<InferOutput>, HttpStatus> output = modelExecutionService.runInference(modelName, inferInput);
        if(output.getRight() != HttpStatus.OK) {
            return new ResponseEntity<>(null, output.getRight());
        }
        return new ResponseEntity<>(new InferenceResponse(modelName, output.getLeft()), HttpStatus.OK);
    }
}
