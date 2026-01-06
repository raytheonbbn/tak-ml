package com.atakmap.android.takml.mx_framework.onnx_plugin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import com.atakmap.android.takml_android.MXExecuteModelCallback;
import com.atakmap.android.takml_android.MXPlugin;
import com.atakmap.android.takml_android.ModelTypeConstants;
import com.atakmap.android.takml_android.ProcessingParams;
import com.atakmap.android.takml_android.TakmlModel;
import com.atakmap.android.takml_android.lib.TakmlInitializationException;
import com.atakmap.android.takml_android.service.MxPluginService;
import com.atakmap.android.takml_android.takml_result.Recognition;
import com.atakmap.android.takml_android.tensor_processor.InferInput;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import android.util.Log;

public class OnnxPlugin implements MXPlugin {
    private static final String TAG = OnnxPlugin.class.getName();
    private static final String DESCRIPTION = "Onnx Plugin used for Image Classification";
    private static final String VERSION = "1.0";
    private static final String APPLICABLE_EXTENSION = ".onnx";
    private final OrtEnvironment ortEnvironment = OrtEnvironment.getEnvironment();
    private OrtSession ortSession;
    private String modelType;
    private List<String> labels;
    private String modelPath;
    private OnnxProcessingParams onnxProcessingParams = null;
    private String modelName;

    private static final long[] DEFAULT_MODEL_SHAPE = new long[]{1, 3, 224, 224};
    private static final int DEFAULT_IMAGE_DIM = 224;

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
        return false;
    }

    @Override
    public String[] getApplicableModelExtensions() {
        return new String[]{APPLICABLE_EXTENSION};
    }

    @Override
    public String[] getSupportedModelTypes() {
        return new String[]{ModelTypeConstants.IMAGE_CLASSIFICATION};
    }

    @Override
    public Class<? extends MxPluginService> getOptionalServiceClass() {
        return OnnxPluginService.class;
    }

    @Override
    public void instantiate(TakmlModel takmlModel, Context context) throws TakmlInitializationException {
        Log.d(TAG, "Calling instantiation");

        byte[] model = null;
        if(takmlModel.getModelUri() == null) {
            Log.e(TAG, "Unable to open URI for shareable file");
        } else {
            try (InputStream inputStream = context.getContentResolver().openInputStream(takmlModel.getModelUri())) {
                if (inputStream != null) {
                    model = new byte[inputStream.available()];
                    inputStream.read(model);
                }
            } catch (IOException e) {
                throw new TakmlInitializationException("IOException reading model", e);
            }
        }

        labels = takmlModel.getLabels();

        try {
            ortSession = ortEnvironment.createSession(model);
        } catch (OrtException e) {
            throw new TakmlInitializationException("IOException reading model: " + takmlModel.getModelUri(), e);
        }

        modelType = takmlModel.getModelType();

        ProcessingParams processingParams = takmlModel.getProcessingParams();
        if(processingParams != null){
            if(!(processingParams instanceof OnnxProcessingParams)){
                throw new TakmlInitializationException("Processing Params should be of type: "
                        + OnnxProcessingParams.class.getName());
            }
            onnxProcessingParams = (OnnxProcessingParams) processingParams;
        }

        this.modelName = takmlModel.getName();
    }

    @Override
    public void execute(byte[] inputDataBitmap, MXExecuteModelCallback callback) {
        Log.d(TAG,"Calling execution");

        if(inputDataBitmap == null){
            Log.e(TAG, "Input data was null");
            callback.modelResult(null, false, modelName, modelType);
            return;
        }

        // Load the input data as a Bitmap
        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeByteArray(inputDataBitmap, 0,
                    inputDataBitmap.length);
        } catch (Exception e) {
            Log.e(TAG, "Error occurred while trying to decode bitmap", e);
            return;
        }

        List<Recognition> results = new ArrayList<>();
        if(modelType.equals(ModelTypeConstants.IMAGE_CLASSIFICATION)) {
            int imageDimX = DEFAULT_IMAGE_DIM;
            int imageDimY = DEFAULT_IMAGE_DIM;
            if(onnxProcessingParams != null){
                imageDimX = onnxProcessingParams.getDimPixelWidth();
                imageDimY = onnxProcessingParams.getDimPixHeight();
            }
            Bitmap rawBitmap = Bitmap.createScaledBitmap(bitmap, imageDimX, imageDimY,
                    false);
            bitmap = rotate(rawBitmap, 90);

            if (bitmap != null) {
                float[][][][] imgData = ImageProcessor.preProcess(bitmap, onnxProcessingParams);
                String inputName = ortSession.getInputNames().iterator().next();

                OrtEnvironment env = OrtEnvironment.getEnvironment();
                try (OrtEnvironment ignored = env) {
                    OnnxTensor tensor;

                    try {
                        tensor = OnnxTensor.createTensor(env, imgData);
                    } catch (OrtException e) {
                        Log.e(TAG, "OrtException creating tensor for model: "
                                + modelPath, e);
                        return;
                    }
                    try (OnnxTensor ignored2 = tensor) {
                        OrtSession.Result output;
                        try {
                            output = ortSession.run(Collections.singletonMap(inputName, tensor));
                        } catch (OrtException e) {
                            Log.e(TAG, "OrtException running tensor for model: "
                                    + modelPath, e);
                            return;
                        }
                        float[] rawOutput;
                        try {
                            rawOutput = ((float[][]) output.get(0).getValue())[0];
                        } catch (OrtException e) {
                            Log.e(TAG, "OrtException reading tensor output for model: "
                                    + modelPath, e);
                            return;
                        }

                        float[] probabilities = rawOutput;
                        if(onnxProcessingParams == null){
                            // Apply softmax to logits to obtain probabilities
                            probabilities = softmax(rawOutput);
                        }

                        // Find the top-5 class probabilities
                        callback.modelResult(getProbabilities(probabilities), true,modelName, modelType);
                    }
                }
            }
        }

        callback.modelResult(results, true, modelName, modelType);
    }

    @Override
    public void execute(List<InferInput> inputTensors, MXExecuteModelCallback callback) {
        // TODO: support
    }

    private List<Recognition> getProbabilities(float[] probabilities) {


        TreeMap<Float, Recognition> ret = new TreeMap<>(Collections.reverseOrder());
        int[] indices = new int[probabilities.length];
        for (int i = 0; i < probabilities.length; i++) indices[i] = i;

        // Print top-5 probabilities
        System.out.println("Top-5 class probabilities:");
        for (int index : indices) {
            System.out.printf("Class %d: %.4f%n", index, probabilities[index]);
            ret.put(probabilities[index], new Recognition(labels.get(index), probabilities[index]));
        }

        return new ArrayList<>(ret.values());
    }

    private float[] softmax(float[] logits) {
        float maxLogit = Float.NEGATIVE_INFINITY;
        for (float logit : logits) {
            if (logit > maxLogit) {
                maxLogit = logit;
            }
        }
        float sumExp = 0f;
        float[] exps = new float[logits.length];
        for (int i = 0; i < logits.length; i++) {
            exps[i] = (float) Math.exp(logits[i] - maxLogit);
            sumExp += exps[i];
        }
        for (int i = 0; i < logits.length; i++) {
            exps[i] /= sumExp;
        }
        return exps;
    }

    // Rotate the image of the input bitmap
    public Bitmap rotate(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                matrix, true);
    }

    @Override
    public void shutdown() {
        Log.d(TAG, "Shutting down");
    }
}
