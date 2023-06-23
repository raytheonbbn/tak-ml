package com.atakmap.android.takml_android.pytorch_mx_plugin;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Pair;

import com.atakmap.android.takml_android.MXExecuteModelCallback;
import com.atakmap.android.takml_android.MXPlugin;
import com.atakmap.android.takml_android.ModelTypeConstants;
import com.atakmap.android.takml_android.ProcessingParams;
import com.atakmap.android.takml_android.TakmlModel;
import com.atakmap.android.takml_android.lib.TakmlInitializationException;
import com.atakmap.android.takml_android.takml_result.Recognition;
import com.atakmap.coremap.log.Log;

import java.util.Collections;
import java.util.List;

public class PtMobilePlugin implements MXPlugin {
    private static final String TAG = PtMobilePlugin.class.getName();
    private static final String DESCRIPTION = "PyTorch Mobile plugin used for image recognition";
    private static final String VERSION = "1.0";
    private Classifier classifier;
    private ObjectDetector objectDetector;
    private List<String> labels;
    private static final String APPLICABLE_EXTENSION = ".torchscript";

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
    public void instantiate(TakmlModel takmlModel) throws TakmlInitializationException {
        Log.d(TAG, "Calling instantiation");

        ProcessingParams processingParams = takmlModel.getProcessingParams();
        byte[] model = takmlModel.getModelBytes();
        labels = takmlModel.getLabels();

        classifier = null;
        objectDetector = null;

        /**
         * Loads the Processing Config, which provides metadata about how to process data
         * with the Model.
         *
         * e.g.:
         * {
         *   "modelInputWidth": 640, // the expected input image width pixels
         *   "modelInputHeight": 640, // the expected input image height pixels
         *   "tensorOutputNumberRows": 25200, // the number of Tensor output rows
         *   "tensorOutputNumberColumns": 15, // the number of Tensor output columns
         *                                    // (first few bounds, last several are classes)
         *   "normMeanRGB": [0, 0, 0],
         *   "normStdRGB": [1, 1, 1],
         *   "type": "[...].ObjectDetectionParams" // Type, e.g. Object Detection Params
         *
         * }
         */
        Log.d(TAG, "instantiating : " + takmlModel.getModelType().toString());
        if(takmlModel.getModelType().equals(ModelTypeConstants.OBJECT_DETECTION)) {
            PytorchObjectDetectionParams objectDetectionParams = (PytorchObjectDetectionParams) processingParams;
            if (objectDetectionParams.getModelInputHeight() > 0) {
                /** Construct an Object Detector **/
                objectDetector = new ObjectDetector(model, objectDetectionParams);
            }
        }else if(takmlModel.getModelType().equals(ModelTypeConstants.IMAGE_CLASSIFICATION)) {
            /**
             * If not Object Detection, instantiate an Image Classifier
             */
            classifier = new Classifier(model);
        }else{
            throw new TakmlInitializationException("Unknown model type: " + takmlModel.getModelType());
        }
    }

    @Override
    public void execute(byte[] inputDataBitmap, MXExecuteModelCallback callback) {
        Log.d(TAG,"Calling execution");

        /** Load the input data as a Bitmap **/
        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeByteArray(inputDataBitmap, 0, inputDataBitmap.length);
        } catch (Exception e) {
            Log.e(TAG, "Error occurred while trying to read assets", e);
            return;
        }

        /** The request type is Object Detection **/
        if(objectDetector != null){
            Log.d(TAG, "executing object detection");
            /** Call the underlying model execution **/
            List<Recognition> result = objectDetector.analyzeImage(bitmap, labels);

            /** Return results **/
            callback.modelResult(result, true, ModelTypeConstants.OBJECT_DETECTION);

            /** The request type is Image Classification (default) **/
        }else{
            Log.d(TAG, "executing image classification");
            Pair<String, Float> pred = classifier.predict(bitmap, labels);

            List<Recognition> recognitionsRet = Collections.singletonList(new Recognition(
                            pred.first,
                            pred.second
                    ));

            /** Return results **/
            callback.modelResult(recognitionsRet, true,
                    ModelTypeConstants.IMAGE_CLASSIFICATION);
        }
    }

    @Override
    public String[] getApplicableModelExtensions() {
        return new String[]{APPLICABLE_EXTENSION};
    }

    @Override
    public String[] getSupportedModelTypes() {
        return new String[]{ModelTypeConstants.IMAGE_CLASSIFICATION,
                ModelTypeConstants.OBJECT_DETECTION};
    }

    @Override
    public void shutdown() {
        Log.d(TAG, "Shutting down");
        objectDetector = null;
        classifier = null;
    }
}
