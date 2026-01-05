package com.bbn.takml_server.model_execution.mx_plugins;

import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OrtUtil;
import ai.onnxruntime.TensorInfo;
import com.bbn.takml_server.lib.TakmlInitializationException;
import com.bbn.takml_server.model_execution.MXExecuteModelCallback;
import com.bbn.takml_server.model_execution.MXPlugin;
import com.bbn.takml_server.model_execution.TensorSerializerUtil;
import com.bbn.takml_server.model_execution.api.model.InferInput;
import com.bbn.takml_server.model_execution.api.model.InferOutput;
import com.bbn.takml_server.model_execution.api.model.ModelTensor;
import com.bbn.takml_server.takml_model.ModelTypeConstants;
import com.bbn.takml_server.takml_model.TakmlModel;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OnnxMxPlugin implements MXPlugin {
    private static final String DESCRIPTION = "Onnx Mx Plugin that enables image classification and object detection";
    private static final String VERSION = "1.0";
    private static final String[] MODEL_EXTENSIONS = {".onnx"};
    private static final String[] MODEL_OPERATIONS_SUPPORTED = {ModelTypeConstants.IMAGE_CLASSIFICATION,
            ModelTypeConstants.OBJECT_DETECTION};

    private static final Logger logger = LogManager.getLogger(OnnxMxPlugin.class);

    private final OrtEnvironment ortEnvironment = OrtEnvironment.getEnvironment();
    private OrtSession ortSession;
    private String modelType;
    private String modelPath;

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public boolean isServerSide() {
        return true;
    }

    @Override
    public Pair<long[], long[]> instantiate(TakmlModel takmlModel) throws TakmlInitializationException {
        if(takmlModel.getLabels() == null){
            throw new TakmlInitializationException("Model labels was null");
        }

        this.modelPath = takmlModel.getModelFile().getPath();

        byte[] model;
        try(InputStream is = new FileInputStream(takmlModel.getModelFile())){
            model = new byte[is.available()];
            is.read(model);
        } catch (FileNotFoundException e) {
            throw new TakmlInitializationException("FileNotFoundException reading model", e);
        } catch (IOException e) {
            throw new TakmlInitializationException("IOException reading model", e);
        }

        try {
            ortSession = ortEnvironment.createSession(model);
        } catch (OrtException e) {
            throw new TakmlInitializationException("OrtException reading model: "
                    + takmlModel.getModelFile().getPath());
        }

        modelType = takmlModel.getModelType();

        // Extract input tensor metadata
        Map<String, NodeInfo> inputInfo;
        try {
            inputInfo = ortSession.getInputInfo();
        } catch (OrtException e) {
            throw new TakmlInitializationException(e);
        }
        Map<String, NodeInfo> outputInfo;
        try {
            outputInfo = ortSession.getOutputInfo();
        } catch (OrtException e) {
            throw new TakmlInitializationException(e);
        }

        long[] inputShape = null, outputShape = null;
        if (!inputInfo.isEmpty()) {
            NodeInfo info = inputInfo.values().iterator().next();
            if (info.getInfo() instanceof TensorInfo tensorInfo) {
                inputShape = tensorInfo.getShape();
            }
        }
        if (!outputInfo.isEmpty()) {
            NodeInfo info = outputInfo.values().iterator().next();
            if (info.getInfo() instanceof TensorInfo tensorInfo) {
                outputShape = tensorInfo.getShape();
            }
        }

        logger.info("Finished instantiating Takml Model: {}", takmlModel.getModelFile().getPath());

        return Pair.of(inputShape, outputShape);
    }

    @Override
    public void execute(InferInput inferInput, MXExecuteModelCallback callback) {

        List<InferOutput> results = new ArrayList<>();
        if(modelType.equals(ModelTypeConstants.IMAGE_CLASSIFICATION)) {
            OrtEnvironment env = OrtEnvironment.getEnvironment();

            float[] input = TensorSerializerUtil.convertToFloatArr(inferInput.getData());
            OnnxTensor tensor;
            try {
                tensor = OnnxTensor.createTensor(env, OrtUtil.reshape(input,
                        TensorSerializerUtil.convertShapeToLong(inferInput.getShape())));
            } catch (OrtException e) {
                logger.error("OrtException creating tensor for model: " + modelPath, e);
                return;
            }

            var inputName = ortSession.getInputNames().iterator().next();

            OrtSession.Result output;
            try {
                output = ortSession.run(Collections.singletonMap(inputName, tensor));
            } catch (OrtException e) {
                logger.error("OrtException running tensor for model: " + modelPath, e);
                return;
            }
            float[][] outputTensor;
            try {
                // Extract output probabilities
                outputTensor = (float[][]) output.get(0).getValue();
            } catch (OrtException e){
                logger.error("OrtException reading output tensor for model: " + modelPath, e);
                return;
            }

            results.add(new InferOutput(inferInput.getName(), inferInput.getShape(), inferInput.getDatatype(),
                    TensorSerializerUtil.convertToBigDecimal(outputTensor[0])));
        }

        callback.modelResult(results, true, modelType);
    }

    @Override
    public String[] getApplicableModelExtensions() {
        return MODEL_EXTENSIONS;
    }

    @Override
    public String[] getSupportedModelTypes() {
        return MODEL_OPERATIONS_SUPPORTED;
    }

    @Override
    public void shutdown() {
        logger.info("Shutting down");
    }
}
