package com.atakmap.android.takml_android.executorch_mx_plugin;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import com.atakmap.android.takml_android.MXExecuteModelCallback;
import com.atakmap.android.takml_android.MXPlugin;
import com.atakmap.android.takml_android.ModelTypeConstants;
import com.atakmap.android.takml_android.TakmlModel;
import com.atakmap.android.takml_android.lib.TakmlInitializationException;
import com.atakmap.android.takml_android.service.MxPluginService;
import com.atakmap.android.takml_android.takml_result.RawTensorOutput;
import com.atakmap.android.takml_android.takml_result.Recognition;
import com.atakmap.android.takml_android.takml_result.Segmentation;
import com.atakmap.android.takml_android.tensor_processor.InferInput;

import org.apache.commons.io.IOUtils;
import org.pytorch.executorch.EValue;
import org.pytorch.executorch.Module;
import org.pytorch.executorch.Tensor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExecutorchPlugin implements MXPlugin {
    private static final String TAG = ExecutorchPlugin.class.getName();
    private static final String DESCRIPTION = "Executorch plugin used for image recognition";
    private static final String VERSION = "1.0";
    private Segmenter segmenter;
    private List<String> labels;
    private static final String APPLICABLE_EXTENSION = ".pte";

    private Context context;
    private String modelName;

    private ExecutorchProcessingParams processingParams;

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
    public void instantiate(TakmlModel takmlModel, Context context) throws TakmlInitializationException {
        try {
            Os.setenv("ADSP_LIBRARY_PATH", context.getApplicationInfo().nativeLibraryDir, true);
            Log.d(TAG, "Set ADSP_LIBRARY_PATH, added: " + context.getApplicationInfo().nativeLibraryDir);
        } catch (ErrnoException e) {
            Log.e(TAG, "Cannot set ADSP_LIBRARY_PATH", e);
        }

        Log.d(TAG, "Calling instantiation");
        this.context = context;

        Log.d(TAG, "Successfully loaded model: " + takmlModel.getModelUri().toString());

        labels = takmlModel.getLabels();
        modelName = takmlModel.getName();

        segmenter = null;

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
        if(takmlModel.getProcessingParams() != null) {
            processingParams = (ExecutorchProcessingParams) takmlModel.getProcessingParams();
        }
//        if(takmlModel.getModelType().equals(ModelTypeConstants.OBJECT_DETECTION)) {
//            if (processingParams.getModelInputHeight() > 0) {
//                /** Construct an Object Detector **/
//                objectDetector = new ObjectDetector(model, processingParams);
//            }
//        }else if(takmlModel.getModelType().equals(ModelTypeConstants.IMAGE_CLASSIFICATION)) {
//            /**
//             * If not Object Detection, instantiate an Image Classifier
//             */
//            classifier = new Classifier(model);
//        }else

        if(takmlModel.getModelType().equals(ModelTypeConstants.SEGMENTATION)) {
            segmenter = new Segmenter(context, takmlModel.getModelUri(), processingParams, labels);
            Log.d(TAG, "Instantiated segmenter");
        }else{
            throw new TakmlInitializationException("Unknown model type: " + takmlModel.getModelType());
        }
    }

    @Override
    public void execute(byte[] inputDataBitmap, MXExecuteModelCallback callback) {
        Log.d(TAG,"Calling execution");

        int offset = 0;
        if(processingParams.getIncludeCoordinates()) {
            offset = 8;
        }

        /** Load the input data as a Bitmap **/
        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeByteArray(inputDataBitmap, offset, inputDataBitmap.length - offset);
        } catch (Exception e) {
            Log.e(TAG, "Error occurred while trying to read assets", e);
            return;
        }

        /** The request type is Object Detection **/

        if (segmenter != null) {
            Log.d(TAG, "executing image segmentation");

            List<Segmentation> segmentations = new ArrayList<>();
            try {
                if(processingParams.getIncludeCoordinates()) {
                    Log.d(TAG, "execute segmentation with embedded coordinates");
                    float x = ByteBuffer.wrap(Arrays.copyOfRange(inputDataBitmap, 0, 4)).getFloat();
                    float y = ByteBuffer.wrap(Arrays.copyOfRange(inputDataBitmap, 4, 8)).getFloat();
                    segmentations.add(segmenter.segment2(bitmap, x, y));
                } else {
                    Log.d(TAG, "execute segmentation without embedded coordinates");
                    segmentations.add(segmenter.segment(bitmap)); // ** e.g., for DL3 model
                }

                Log.d(TAG, "segmentation complete. Produced " + segmentations.get(0).getChips().size() + " chips");

                callback.modelResult(segmentations, true, modelName, ModelTypeConstants.SEGMENTATION);
            } catch (Exception e) {
                Log.e(TAG, "Exception during segmentation processing: " + e.getMessage());
                callback.modelResult(segmentations, false, modelName,
                        ModelTypeConstants.SEGMENTATION);
            }
        }
    }

    @Override
    public void execute(List<InferInput> inferInputs, MXExecuteModelCallback callback) {
        try {
            List<RawTensorOutput> outputs = segmenter.rawInference(inferInputs);
            callback.modelResult(outputs, true, modelName, ModelTypeConstants.SEGMENTATION);
            Log.d(TAG, "segmentation complete. Produced " + outputs.size() + " output tensors");
        } catch (Exception e) {
            Log.e(TAG, "Exception during segmentation processing", e);
            callback.modelResult(null, false, modelName, ModelTypeConstants.SEGMENTATION);
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
    public Class<? extends MxPluginService> getOptionalServiceClass() {
        return ExecutorchPluginService.class;
    }

    @Override
    public void shutdown() {
        Log.d(TAG, "Shutting down");
    }
}
