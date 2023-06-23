package com.atakmap.android.takml.mx_framework.plugin.tflite_implementations;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.SystemClock;
import android.util.Log;

import com.atakmap.android.takml.mx_framework.DetectionResult;
import com.atakmap.android.takml.mx_framework.DetectionResults;
import com.bbn.takml_sdk_android.mx_framework.Recognition;
import com.google.gson.Gson;

import org.apache.commons.lang3.SerializationUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.Rot90Op;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.vision.classifier.Classifications;
import org.tensorflow.lite.task.vision.classifier.ImageClassifier;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class TFLiteImpl {

    private static final String TAG = TFLiteImpl.class.getSimpleName();

    public static final int MAX_RESULTS = 1;

    public static final String CLASSIFICATION_OR_OBJECT_DETECTION_TYPE = "classification_or_object_detection";
    public static final String CLASSIFICATION_DETECTION_TYPE = "classification";
    public static final String OBJECT_DETECTION_TYPE = "object_detection";

    /** Float MobileNet requires additional normalization of the used input. */
    private static final float IMAGE_MEAN = 127.5f;
    private static final float IMAGE_STD = 127.5f;

    /**
     * Float model does not need dequantization in the post-processing. Setting mean and std as 0.0f
     * and 1.0f, repectively, to bypass the normalization.
     */
    private static final float PROBABILITY_MEAN = 0.0f;
    private static final float PROBABILITY_STD = 1.0f;
    private static final int DELEGATE_CPU = 0;
    private static final int DELEGATE_GPU = 1;
    private static final int DELEGATE_NNAPI = 2;
    private static final int MODEL_MOBILENETV1 = 0;
    private static final int MODEL_EFFICIENTNETV0 = 1;
    private static final int MODEL_EFFICIENTNETV1 = 2;
    private static final int MODEL_EFFICIENTNETV2 = 3;

    private Float threshold = 0.5f;
    private int numThreads = 2;
    private int maxResults = 3;


    public byte[] execute(byte[] inputData, String modelDirectoryName,
                                     String modelFileName, String labelsFileName,
                                     String inputProcessingConfigFileName) {

        Log.d(TAG, "execute called with parameters: " + "\n" +
                "model directory: " + modelDirectoryName + "\n" +
                "model file name: " + modelFileName + "\n" +
                "labels file name: " + labelsFileName);

        // convert image file into image bitmap (ARGB8888 format)
        BitmapFactory.Options op = new BitmapFactory.Options();
        op.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap imgBitmap = BitmapFactory.decodeByteArray(inputData, 0, inputData.length, op);
        if (imgBitmap == null) {
            Log.e(TAG,"Conversion failed, returning.");
            return null;
        }

        Log.d(TAG, "Converted image file to bitmap.");

        HashMap<String, HashMap<String, String>> inputProcessingConfig;
        try {
            inputProcessingConfig = loadInputProcessingConfig(modelDirectoryName, inputProcessingConfigFileName);
        } catch (Exception e) {
            Log.e(TAG, "Failed to read input processing config json.");
            return null;
        }

        Log.d(TAG, "Loaded input processing config: " + new PrettyPrintingMap(inputProcessingConfig));

        /* ACTUAL USE OF TENSORFLOW LITE API STARTS HERE */
        try {

        Map<String, String> params = inputProcessingConfig.get(CLASSIFICATION_OR_OBJECT_DETECTION_TYPE);
        assert params != null;
        if(params.get(CLASSIFICATION_DETECTION_TYPE) == null) {
            return processClassificationOrObjectDetection(imgBitmap, modelDirectoryName,
                    modelFileName, labelsFileName, true);
        }else{
            return processClassificationOrObjectDetection(imgBitmap, modelDirectoryName,
                    modelFileName, labelsFileName, false);
        }

        }catch (Exception e){
            Log.e(TAG, "execute: ", e);
        }

        return null;
    }

    private byte[] processClassificationOrObjectDetection(Bitmap imgBitmap, String modelDirectoryName,
                                                          String modelFileName,
                                                          String labelsFileName, boolean classify){
        /* ACTUAL USE OF TENSORFLOW LITE API STARTS HERE */

        ImageClassifier.ImageClassifierOptions.Builder optionsBuilder =
                ImageClassifier.ImageClassifierOptions.builder()
                        .setScoreThreshold(threshold)
                        .setMaxResults(maxResults);

        File file = new File(modelDirectoryName + "/" + modelFileName);
        FileInputStream is = null;
        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        File labelsFile = new File(modelDirectoryName + "/" + labelsFileName);
        Scanner input = null;
        try {
            input = new Scanner(labelsFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        List<String> labels = new ArrayList<>();

        while (input.hasNextLine()) {
            labels.add(input.nextLine());
        }

        // Create preprocessor for the image.
        // See https://www.tensorflow.org/lite/inference_with_metadata/
        //            lite_support#imageprocessor_architecture
        ImageProcessor imageProcessor =
                new ImageProcessor.Builder().add(new Rot90Op(0)).build();

        // Preprocess the image and convert it into a TensorImage for classification.
        TensorImage tensorImage =
                imageProcessor.process(TensorImage.fromBitmap(imgBitmap));

        byte[] resultBytes = null;
        if(classify) {
            ImageClassifier imageClassifier = null;
            try {
                imageClassifier =
                        ImageClassifier.createFromBufferAndOptions(
                                is.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length()),
                                optionsBuilder.build());
            } catch (IOException e) {
                Log.e(TAG, "Could not classify image", e);
            }

            // Classify the input image
            List<Classifications> results = imageClassifier.classify(tensorImage);
            Classifications result = results.get(0);
            Category category = getHighestResult(result.getCategories());
            String label = labels.get(Integer.parseInt(category.getLabel()));

            try {
                resultBytes = SerializationUtils.serialize(Collections.singletonList(
                        new Recognition(label, label, category.getScore())).iterator().next());
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Error serializing result: " + e.getMessage());
                return null;
            }
        }else{
            ObjectDetector.ObjectDetectorOptions.Builder builder =
                    ObjectDetector.ObjectDetectorOptions.builder()
                            .setScoreThreshold(threshold)
                            .setMaxResults(maxResults);

            ObjectDetector objectDetector = null;
            try {
                objectDetector =
                        ObjectDetector.createFromBufferAndOptions(
                                is.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length()),
                                builder.build());
            } catch (IOException e) {
                Log.e(TAG, "Could not detect image", e);
                return null;
            }

            // Classify the input image
            ArrayList<Detection> results = new ArrayList<>(objectDetector.detect(tensorImage));
            ArrayList<DetectionResult> detectionResults = new ArrayList<>();
            for(Detection detection : results){
                Log.d(TAG, "processClassificationOrObjectDetection: " + detection);
                Category category = getHighestResult(detection.getCategories());
                detectionResults.add(new DetectionResult(category.getLabel(),
                        category.getScore(),
                        detection.getBoundingBox().bottom,
                        detection.getBoundingBox().left,
                        detection.getBoundingBox().right,
                        detection.getBoundingBox().top));
            }
            try {
                resultBytes = new Gson().toJson(new DetectionResults(detectionResults)).getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Error serializing result: " + e.getMessage());
                return null;
            }
        }

        return resultBytes;
    }

    private Category getHighestResult(List<Category> categories) {
        List<Category> sortedCategories = new ArrayList<>(categories);
        Collections.sort(sortedCategories, new Comparator<Category>() {
            @Override
            public int compare(Category category1, Category category2) {
                return category1.getIndex() - category2.getIndex();
            }
        });
        return sortedCategories.get(0);
    }

    public static HashMap<String, HashMap<String, String>> loadInputProcessingConfig(
            String modelDirectoryString, String inputProcessingConfigFileName) throws IOException, JSONException {

        HashMap<String, HashMap<String, String>> ret = new HashMap<>();

        File f = new File(
                modelDirectoryString + "/" +
                        inputProcessingConfigFileName);
        FileInputStream fis = null;

        fis = new FileInputStream(f);
        BufferedReader reader = new BufferedReader(new InputStreamReader(fis));

        String jsonString = "";
        String line;
        while ((line = reader.readLine()) != null) {
            jsonString += line + "\n";
        }

        com.atakmap.coremap.log.Log.d(TAG, "Read json string: " + jsonString);

        JSONObject root = new JSONObject(jsonString);

        Iterator<String> categories = root.keys();

        while (categories.hasNext()) {
            String category = categories.next();

            JSONObject metadata = root.getJSONObject(category);

            Iterator<String> fields = metadata.keys();

            HashMap<String, String> metadataLabels = new HashMap<>();

            while (fields.hasNext()) {
                String field = fields.next();
                metadataLabels.put(field, metadata.getString(field));
            }

            ret.put(category, metadataLabels);
        }

        if (fis != null) {
            fis.close();
        }

        return ret;
    }

    public class PrettyPrintingMap<K, V> {
        private Map<K, V> map;

        public PrettyPrintingMap(Map<K, V> map) {
            this.map = map;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            Iterator<Map.Entry<K, V>> iter = map.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<K, V> entry = iter.next();
                sb.append(entry.getKey());
                sb.append('=').append('"');
                sb.append(entry.getValue());
                sb.append('"');
                if (iter.hasNext()) {
                    sb.append(',').append("\n");
                }
            }
            return sb.toString();

        }
    }
}
