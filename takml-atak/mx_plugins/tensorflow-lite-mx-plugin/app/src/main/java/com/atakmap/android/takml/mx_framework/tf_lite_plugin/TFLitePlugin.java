package com.atakmap.android.takml.mx_framework.tf_lite_plugin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;

import com.atakmap.android.takml_android.MXExecuteModelCallback;
import com.atakmap.android.takml_android.MXPlugin;
import com.atakmap.android.takml_android.ModelTypeConstants;
import com.atakmap.android.takml_android.Takml;
import com.atakmap.android.takml_android.TakmlModel;
import com.atakmap.android.takml_android.lib.TakmlInitializationException;
import com.atakmap.android.takml_android.service.MxPluginService;
import com.atakmap.android.takml_android.takml_result.Recognition;
import com.atakmap.android.takml_android.tensor_processor.InferInput;

import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions;
import org.tensorflow.lite.task.vision.classifier.Classifications;
import org.tensorflow.lite.task.vision.classifier.ImageClassifier;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.SortedMap;
import java.util.TreeMap;

public class TFLitePlugin implements MXPlugin {
    private static final String TAG = TFLitePlugin.class.getName();
    private static final String DESCRIPTION = "Tensorflow Lite Plugin used for image recognition";
    private static final String VERSION = "1.0";
    private List<String> labels;
    private static final String APPLICABLE_EXTENSION = ".tflite";

    private static final float CLASSIFICATION_THRESHOLD = 0.05f;

    private ImageClassifier imageClassifier;
    private ObjectDetector objectDetector;
    private String modelType;

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
        return false;
    }

    @Override
    public void instantiate(TakmlModel takmlModel, Context context) throws TakmlInitializationException {
        Log.d(TAG, "Calling instantiation");

        this.takmlModel = takmlModel;

        /*byte[] model = null;
        try(InputStream is = new FileInputStream(takmlModel.getModelFile())){
            model = new byte[is.available()];
            is.read(model);
        } catch (FileNotFoundException e) {
            throw new TakmlInitializationException("FileNotFoundException reading model", e);
        } catch (IOException e) {
            throw new TakmlInitializationException("IOException reading model", e);
        }*/

        if(takmlModel.getLabels() == null){
            throw new TakmlInitializationException("Model labels was null");
        }
        labels = takmlModel.getLabels();

       // ByteBuffer byteBuffer = ByteBuffer.allocateDirect(model.length);
       // byteBuffer.order(ByteOrder.nativeOrder());
       // byteBuffer.put(model);

        ByteBuffer byteBuffer = null;
        if(takmlModel.getModelUri() == null) {
            Log.e(TAG, "Unable to open URI for shareable file");
        } else {
            try (InputStream inputStream = context.getContentResolver().openInputStream(takmlModel.getModelUri())) {
                if (inputStream != null) {
                    byteBuffer = inputStreamToByteBuffer(inputStream);
                }
            } catch (IOException e) {
                throw new TakmlInitializationException("IOException reading model", e);
            }
        }

        if(takmlModel.getModelType().equals(ModelTypeConstants.IMAGE_CLASSIFICATION)) {
            ImageClassifier.ImageClassifierOptions options = ImageClassifier.ImageClassifierOptions.builder()
                           // .setScoreThreshold(CLASSIFICATION_THRESHOLD)
                            .setMaxResults(5)
                            .build();

            imageClassifier = ImageClassifier.createFromBufferAndOptions(byteBuffer, options);
        }else if(takmlModel.getModelType().equals(ModelTypeConstants.OBJECT_DETECTION)) {
            objectDetector = ObjectDetector.createFromBuffer(byteBuffer);
        }else{
            throw new TakmlInitializationException("Unsupported Model Type: " +
                    takmlModel.getModelType());
        }

        modelType = takmlModel.getModelType();
    }

    private ByteBuffer inputStreamToByteBuffer(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n;
        while ((n = inputStream.read(buffer)) != -1) {
            byteStream.write(buffer, 0, n);
        }

        byte[] bytes = byteStream.toByteArray();
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bytes.length);
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.put(bytes);
        byteBuffer.rewind(); // Reset position to 0
        return byteBuffer;
    }

    @Override
    public void execute(byte[] inputDataBitmap, MXExecuteModelCallback callback) {
        Log.d(TAG,"Calling execution");

        // convert image file into image bitmap (ARGB8888 format)
        BitmapFactory.Options op = new BitmapFactory.Options();
        op.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap imgBitmap = BitmapFactory.decodeByteArray(inputDataBitmap, 0,
                inputDataBitmap.length, op);
        if (imgBitmap == null) {
            Log.e(TAG,"Conversion failed, returning.");
            callback.modelResult(null, false, takmlModel.getName(), ModelTypeConstants.IMAGE_CLASSIFICATION);
            return;
        }

        // Inference time is the difference between the system time at the start
        // and finish of the process
        long inferenceTime = SystemClock.uptimeMillis();

        // Create preprocessor for the image.
        // See https://www.tensorflow.org/lite/inference_with_metadata/
        //            lite_support#imageprocessor_architecture
        ImageProcessor imageProcessor =
                new ImageProcessor.Builder().build();

        // Preprocess the image and convert it into a TensorImage for classification.
        TensorImage tensorImage =
                imageProcessor.process(TensorImage.fromBitmap(imgBitmap));

        if(modelType.equals(ModelTypeConstants.IMAGE_CLASSIFICATION)) {
            // Classify the input image
            List<Classifications> result = imageClassifier.classify(tensorImage);
            inferenceTime = SystemClock.uptimeMillis() - inferenceTime;
            Log.d(TAG, "Inference Time: " + inferenceTime);
            List<Recognition> recognitions = new ArrayList<>();
            if (result != null && !result.isEmpty()) {
                Log.d(TAG, "execute: " + result.size());
                SortedMap<Float, String> predictions = new TreeMap<>(Collections.reverseOrder());
                for(Classifications classifications : result) {
                    for(Category category : classifications.getCategories()) {
                        Log.d(TAG, "result val: " + category.getScore() + " " + category.getLabel());
                        predictions.put(category.getScore(), category.getLabel());
                    }
                }
                for(Map.Entry<Float, String> entry : predictions.entrySet()){
                    recognitions.add(new Recognition(entry.getValue(), entry.getKey()));
                }
            }else{
                Log.d(TAG, "execute: was null or empty");
            }
            callback.modelResult(recognitions, true, takmlModel.getName(), ModelTypeConstants.IMAGE_CLASSIFICATION);
        }else{
            // Object Detection
            List<Detection> result = objectDetector.detect(tensorImage);
            inferenceTime = SystemClock.uptimeMillis() - inferenceTime;
            Log.d(TAG, "Inference Time: " + inferenceTime);
            List<Recognition> recognitions = new ArrayList<>();
            if (result != null) {
                SortedMap<Float, Pair<String, RectF>> predictions = new TreeMap<>(Collections.reverseOrder());
                for(Detection detections : result) {
                    for(Category category : detections.getCategories()) {
                        predictions.put(category.getScore(), new Pair<>(labels.get(category.getIndex()),detections.getBoundingBox()));
                    }
                }
                for(Map.Entry<Float, Pair<String, RectF>> entry : predictions.entrySet()){
                    Pair<String, RectF> pair = entry.getValue();
                    recognitions.add(new Recognition(pair.first, entry.getKey(),
                            pair.second.bottom, pair.second.left, pair.second.right, pair.second.top));
                }
            }
            callback.modelResult(recognitions, true, takmlModel.getName(), ModelTypeConstants.OBJECT_DETECTION);
        }
    }

    @Override
    public void execute(List<InferInput> inputTensors, MXExecuteModelCallback callback) {
        // TODO: support
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
        return TFLitePluginService.class;
    }

    @Override
    public void shutdown() {
        Log.d(TAG, "Shutting down");
        if(imageClassifier != null) {
            imageClassifier.close();
        }
        if(objectDetector != null){
            objectDetector.close();
        }
    }
}
