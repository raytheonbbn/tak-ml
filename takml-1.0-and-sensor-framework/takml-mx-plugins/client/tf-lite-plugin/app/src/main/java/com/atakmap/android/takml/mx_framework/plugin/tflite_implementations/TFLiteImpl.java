package com.atakmap.android.takml.mx_framework.plugin.tflite_implementations;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.atakmap.android.takml.mx_framework.TFLitePluginConstants;
import com.bbn.takml_sdk_android.mx_framework.Recognition;

import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class TFLiteImpl {

    private static final String TAG = TFLiteImpl.class.getSimpleName();

    public static final int MAX_RESULTS = 1;

    /** Float MobileNet requires additional normalization of the used input. */
    private static final float IMAGE_MEAN = 127.5f;
    private static final float IMAGE_STD = 127.5f;

    /**
     * Float model does not need dequantization in the post-processing. Setting mean and std as 0.0f
     * and 1.0f, repectively, to bypass the normalization.
     */
    private static final float PROBABILITY_MEAN = 0.0f;
    private static final float PROBABILITY_STD = 1.0f;

    public List<Recognition> execute(byte[] inputData, String modelDirectoryName,
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

        /* ACTUAL USE OF TENSORFLOW LITE API STARTS HERE */

        // create tflite interpreter, which will take image input and output predictions
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(1);
        Interpreter interpreter = null;
        try {
            interpreter = new Interpreter(
                    loadMappedFile(modelDirectoryName + "/" + modelFileName),
                    options
            );
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to load model into interpreter: " + e.getMessage());
            return null;
        }
        if (interpreter == null) {
            Log.e(TAG, "Failed to create interpreter.");
            return null;
        }

        Log.d(TAG, "Created TFLite interpreter (model name " + modelFileName + ").");

        HashMap<String, HashMap<String, String>> inputProcessingConfig;
        try {
            inputProcessingConfig = loadInputProcessingConfig(modelDirectoryName, inputProcessingConfigFileName);
        } catch (Exception e) {
            Log.e(TAG, "Failed to read input processing config json.");
            return null;
        }

        Log.d(TAG, "Loaded input processing config: " + new PrettyPrintingMap(inputProcessingConfig));

        // do image pre processing
        int imageTensorIndex = 0;
        int[] imageShape = interpreter.getInputTensor(imageTensorIndex).shape(); // {1, height, width, 3}

        int imageSizeY = imageShape[1];
        int imageSizeX = imageShape[2];

        Log.d(TAG, "Image size y: " + imageSizeY);
        Log.d(TAG, "Image size x: " + imageSizeX);

        int cropSize = Math.min(imgBitmap.getWidth(), imgBitmap.getHeight());
        DataType imageDataType = interpreter.getInputTensor(imageTensorIndex).dataType();

        ImageProcessor.Builder imageProcessorBuilder = new ImageProcessor.Builder();

        // always do cropping and resizing
        imageProcessorBuilder = imageProcessorBuilder
                .add(new ResizeWithCropOrPadOp(cropSize, cropSize))
                .add(new ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR));

        if (inputProcessingConfig.containsKey(TFLitePluginConstants.INPUT_PROCESSING_CONFIG_NORMALIZATION_OP)) {
            HashMap<String, String> normalizationOpParams =
                    inputProcessingConfig.get(TFLitePluginConstants.INPUT_PROCESSING_CONFIG_NORMALIZATION_OP);
            float image_mean = Float.parseFloat(normalizationOpParams.get(TFLitePluginConstants.NORMALIZATION_OP_IMAGE_MEAN));
            float image_std = Float.parseFloat(normalizationOpParams.get(TFLitePluginConstants.NORMALIZATION_OP_IMAGE_STD));
            imageProcessorBuilder = imageProcessorBuilder.add(new NormalizeOp(image_mean, image_std));
        }

        ImageProcessor imageProcessor = imageProcessorBuilder.build();
        TensorImage rawInput = new TensorImage(imageDataType);
        rawInput.load(imgBitmap);
        TensorImage input = imageProcessor.process(rawInput);

        Log.d(TAG, "Finished image pre processing.");

        // create TensorBuffer object for predictions to be output to
        int probabilityTensorIndex = 0;
        int[] probabilityShape = interpreter.getOutputTensor(probabilityTensorIndex).shape();  // {1, NUM_CLASSES}
        DataType probabilityDataType = interpreter.getOutputTensor(probabilityTensorIndex).dataType();
        TensorBuffer outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);

        Log.d(TAG, "Created output buffer.");

        // execute model
        interpreter.run(input.getBuffer(), outputProbabilityBuffer.getBuffer().rewind());

        Log.d(TAG, "Finished execution.");

        // process output
        List<String> labels = null;
        try {
            labels = loadLabels(modelDirectoryName + "/" + labelsFileName);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to load labels.");
            return null;
        }
        if (labels == null) {
            Log.e(TAG, "Failed to load labels.");
            return null;
        }

        Log.d(TAG, "Loaded labels (labels name " + labelsFileName + "), labels: " + labels);

        TensorBuffer processedOutputProbabilityBuffer;

        try {
            TensorProcessor probabilityProcessor =
                    new TensorProcessor.Builder().add(new NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD)).build();
            processedOutputProbabilityBuffer = probabilityProcessor.process(outputProbabilityBuffer);
        } catch (Exception e) {
            Log.e(TAG, "Failed to process output: " + e.getMessage());
            return null;
        }

        if (processedOutputProbabilityBuffer.getFloatArray().length == 1) {
            // assume that if the output probability buffer has length 1, that the classifier
            // was a binary classifier, and that the labels file has two id's, where the first
            // id from the top corresponds to 0, and the second id corresponds to 1

           float confidence = processedOutputProbabilityBuffer.getFloatArray()[0];
           List<Recognition> ret = new ArrayList<>();
           Recognition rec = null;

           if (confidence < 0.5) {
               rec = new Recognition(labels.get(0), "", 1 - confidence);
           } else {
               rec = new Recognition(labels.get(1), "", confidence);
           }

           ret.add(rec);

           return ret;

        } else {
            Map<String, Float> labeledProbability;

            try {
                labeledProbability =
                        new TensorLabel(labels, processedOutputProbabilityBuffer)
                                .getMapWithFloatValue();
            } catch (Exception e) {
                Log.e(TAG, "Failed to do results post processing: " + e.getMessage());
                return null;
            }

            Log.d(TAG, "Did results post processing.");

            return getTopKProbability(labeledProbability);
        }

    }

    /** Gets the top-k results */
    private static List<Recognition> getTopKProbability(Map<String, Float> labelProb) {
        // Find the best classifications.
        PriorityQueue<Recognition> pq =
                new PriorityQueue<>(
                        MAX_RESULTS,
                        new Comparator<Recognition>() {
                            @Override
                            public int compare(Recognition lhs, Recognition rhs) {
                                // Intentionally reversed to put high confidence at the head of the queue.
                                return Float.compare(rhs.getConfidence(), lhs.getConfidence());
                            }
                        });

        for (Map.Entry<String, Float> entry : labelProb.entrySet()) {
            pq.add(new Recognition("" + entry.getKey(), entry.getKey(), entry.getValue()));
        }

        final ArrayList<Recognition> recognitions = new ArrayList<>();
        int recognitionsSize = Math.min(pq.size(), MAX_RESULTS);
        for (int i = 0; i < recognitionsSize; ++i) {
            recognitions.add(pq.poll());
        }
        return recognitions;
    }

    private static MappedByteBuffer loadMappedFile(String filePath) throws IOException {
        FileInputStream fis = new FileInputStream(filePath);
        FileChannel channel = fis.getChannel();
        return channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
    }

    public static List<String> loadLabels(String filePath) throws IOException {
        FileInputStream inputStream = new FileInputStream(filePath);

        List var4;
        try {
            var4 = FileUtil.loadLabels(inputStream, Charset.defaultCharset());
        } catch (Throwable var7) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Throwable var6) {
                    var7.addSuppressed(var6);
                }
            }

            throw var7;
        }

        if (inputStream != null) {
            inputStream.close();
        }

        return var4;
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
