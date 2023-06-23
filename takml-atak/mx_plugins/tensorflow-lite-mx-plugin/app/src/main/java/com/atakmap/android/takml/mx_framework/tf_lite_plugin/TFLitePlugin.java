package com.atakmap.android.takml.mx_framework.tf_lite_plugin;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.os.SystemClock;

import com.atakmap.android.takml_android.MXExecuteModelCallback;
import com.atakmap.android.takml_android.MXPlugin;
import com.atakmap.android.takml_android.ModelTypeConstants;
import com.atakmap.android.takml_android.TakmlModel;
import com.atakmap.android.takml_android.lib.TakmlInitializationException;
import com.atakmap.android.takml_android.takml_result.Recognition;
import com.atakmap.coremap.log.Log;

import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions;
import org.tensorflow.lite.task.vision.classifier.Classifications;
import org.tensorflow.lite.task.vision.classifier.ImageClassifier;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class TFLitePlugin implements MXPlugin {
    private static final String TAG = TFLitePlugin.class.getName();
    private static final String DESCRIPTION = "Tensorflow Lite Plugin used for image recognition";
    private static final String VERSION = "1.0";
    private List<String> labels;
    private static final String APPLICABLE_EXTENSION = ".tflite";

    private static final float CLASSIFICATION_THRESHOLD = 0.5f;

    private ImageClassifier imageClassifier;
    private ObjectDetector objectDetector;
    private String modelType;

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

        if(takmlModel.getModelBytes() == null){
            throw new TakmlInitializationException("Model file was null");
        }

        if(takmlModel.getLabels() == null){
            throw new TakmlInitializationException("Model labels was null");
        }
        labels = takmlModel.getLabels();

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(takmlModel.getModelBytes().length);
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.put(takmlModel.getModelBytes());

        if(takmlModel.getModelType().equals(ModelTypeConstants.IMAGE_CLASSIFICATION)) {
            ImageClassifier.ImageClassifierOptions options = ImageClassifier.ImageClassifierOptions.builder()
                            .setScoreThreshold(CLASSIFICATION_THRESHOLD)
                            .setMaxResults(1)
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
            callback.modelResult(null, false, ModelTypeConstants.IMAGE_CLASSIFICATION);
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
                Classifications classification = result.iterator().next();
                Category category = getBestResult(classification.getCategories());
                recognitions.add(new Recognition(category.getLabel(), category.getScore()));
            }
            callback.modelResult(recognitions, true, ModelTypeConstants.IMAGE_CLASSIFICATION);
        }else{
            // Object Detection
            List<Detection> result = objectDetector.detect(tensorImage);
            inferenceTime = SystemClock.uptimeMillis() - inferenceTime;
            Log.d(TAG, "Inference Time: " + inferenceTime);
            List<Recognition> recognitions = new ArrayList<>();
            if (result != null) {
                for(Detection detection : result) {
                    Category category = getBestResult(detection.getCategories());
                    RectF boundingBox = detection.getBoundingBox();
                    recognitions.add(new Recognition(category.getLabel(), category.getScore(),
                            boundingBox.bottom, boundingBox.left, boundingBox.right, boundingBox.top));
                }
            }
            callback.modelResult(recognitions, true, ModelTypeConstants.OBJECT_DETECTION);
        }
    }

    private Category getBestResult(List<Category> categories) {
        float bestScore = -1000000000000000f;
        Category bestCategory = null;
        for(Category category : categories){
            if(category.getScore() > bestScore){
                bestScore = category.getScore();
                bestCategory = category;
            }
        }
        return bestCategory;
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
