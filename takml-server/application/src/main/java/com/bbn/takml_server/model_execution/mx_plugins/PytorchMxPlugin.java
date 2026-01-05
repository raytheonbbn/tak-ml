package com.bbn.takml_server.model_execution.mx_plugins;

import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import com.bbn.takml_server.lib.TakmlInitializationException;
import com.bbn.takml_server.model_execution.TensorSerializerUtil;
import com.bbn.takml_server.model_execution.api.model.InferInput;
import com.bbn.takml_server.model_execution.MXExecuteModelCallback;
import com.bbn.takml_server.model_execution.MXPlugin;
import com.bbn.takml_server.model_execution.api.model.InferOutput;
import com.bbn.takml_server.model_execution.api.model.ModelTensor;
import com.bbn.takml_server.takml_model.ModelTypeConstants;
import com.bbn.takml_server.takml_model.TakmlModel;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pytorch.Tensor;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PytorchMxPlugin implements MXPlugin {
    private static final String DESCRIPTION = "Pytorch Mx Plugin that enables image classification and object detection";
    private static final String VERSION = "1.0";
    private static final String[] MODEL_EXTENSIONS = {".torchscript"};
    private static final String[] MODEL_OPERATIONS_SUPPORTED = {ModelTypeConstants.IMAGE_CLASSIFICATION,
            ModelTypeConstants.OBJECT_DETECTION};

    private static final Logger logger = LogManager.getLogger(PytorchMxPlugin.class);

    private Model model;
    private TakmlModel takmlModel;

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
        this.takmlModel = takmlModel;

        model = Model.newInstance(takmlModel.getName());
        try {
            model.load(Path.of(takmlModel.getModelFile().getPath()));
        } catch (IOException | MalformedModelException e){
            throw new TakmlInitializationException("Exception reading takml model: " + takmlModel.getModelFile().getPath(), e);
        }

        logger.info("Finished instantiating Takml Model: {}", takmlModel.getModelFile().getPath());

        return null; // TODO: figure out how to support grabbing the Model Tensor shapes, this doesn't seem supported by ai.djl
    }

    @Override
    public void execute(InferInput inferInput, MXExecuteModelCallback callback) {
        float[] input = TensorSerializerUtil.convertToFloatArr(inferInput.getData());
        long[] shape = TensorSerializerUtil.convertShapeToLong(inferInput.getShape());

        List<InferOutput> ret = new ArrayList<>();

        try (NDManager manager = NDManager.newBaseManager()) {
            // Perform inference using the model
            Predictor<float[], List<float[]>> predictor = model.newPredictor(new Translator<>() {

                @Override
                public NDList processInput(TranslatorContext translatorContext, float[] tensor) throws Exception {
                    NDArray ndArray = manager.create(tensor);
                    // the first element is the batch size which is the amount of tensors, thus the shape for the individual tensor
                    // is after the first index
                    // e.g. [1, 3, 224, 224] -> [3, 224, 224]
                    ndArray = ndArray.reshape(Arrays.copyOfRange(shape, 1, shape.length));
                    NDList ndList = new NDList();
                    ndList.add(ndArray);
                    return ndList;
                }


                @Override
                public List<float[]> processOutput(TranslatorContext translatorContext, NDList ndList) throws Exception {
                    List<float[]> ret = new ArrayList<>();
                    for (NDArray ndArray : ndList) {
                        ret.add(ndArray.toFloatArray());
                    }
                    return ret;
                }
            });
            List<float[]> outputs;
            try {
                // Predict the output tensor
                outputs = predictor.predict(input);
            } catch (TranslateException e){
                logger.error("TranslateException running prediction with model: {}", takmlModel.getModelFile().getPath(), e);
                callback.modelResult(null, false, takmlModel.getModelType());
                return;
            }

            for(float[] output : outputs){
                ret.add(new InferOutput(inferInput.getName(), inferInput.getShape(), inferInput.getDatatype(),
                            TensorSerializerUtil.convertToBigDecimal(output)));
            }

            callback.modelResult(ret, true, takmlModel.getModelType());
        }
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
