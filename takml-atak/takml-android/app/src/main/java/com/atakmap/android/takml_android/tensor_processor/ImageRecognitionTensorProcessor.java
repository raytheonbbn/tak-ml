package com.atakmap.android.takml_android.tensor_processor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.atakmap.android.takml_android.processing_params.ImageRecognitionProcessingParams;
import com.atakmap.android.takml_android.takml_result.Recognition;
import com.atakmap.android.takml_android.takml_result.TakmlResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

public class ImageRecognitionTensorProcessor implements TensorProcessor {
    private static final int DEFAULT_DIM_BATCH_SIZE = 1;
    private static final int DEFAULT_DIM_PIXEL_SIZE = 3;
    private static final int DEFAULT_IMAGE_SIZE_X = 224;
    private static final int DEFAULT_IMAGE_SIZE_Y = 224;

    private final List<String> labels;
    private final ImageRecognitionProcessingParams processingParams;

    public ImageRecognitionTensorProcessor(List<String> labels, ImageRecognitionProcessingParams processingParams){
        this.labels = labels;
        this.processingParams = processingParams;
    }

    @Override
    public List<InferInput> processInputTensor(List<byte[]> images) {
        float[][][][] tensorData;
        float[] mean;
        float[] standardDeviation;

        int imageSizeX = DEFAULT_IMAGE_SIZE_X, imageSizeY = DEFAULT_IMAGE_SIZE_Y;
        long[] shape = new long[]{DEFAULT_DIM_BATCH_SIZE, DEFAULT_DIM_PIXEL_SIZE, DEFAULT_IMAGE_SIZE_X, DEFAULT_IMAGE_SIZE_Y};

        if(processingParams != null){
            imageSizeX = processingParams.getDimPixelWidth();
            imageSizeY = processingParams.getDimPixelHeight();

            shape = processingParams.getModelShape();
            tensorData = new float[(int) shape[0]]
                    [(int) shape[1]]
                    [(int) shape[2]]
                    [(int) shape[3]];
            mean = processingParams.getMean();
            standardDeviation = processingParams.getStd();
        }else{
            tensorData = new float[DEFAULT_DIM_BATCH_SIZE][DEFAULT_DIM_PIXEL_SIZE][DEFAULT_IMAGE_SIZE_X][DEFAULT_IMAGE_SIZE_Y];

            // Change from List<Float> to float[]
            mean = new float[]{0.485f, 0.456f, 0.406f};
            standardDeviation = new float[]{0.229f, 0.224f, 0.225f};
        }

        List<InferInput> ret = new ArrayList<>();
        for(byte[] imageBytes : images) {
            // get image
            Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

            // Resize image
            Bitmap scaledImage = Bitmap.createScaledBitmap(image, imageSizeX, imageSizeY, true);

            for (int y = 0; y < scaledImage.getHeight(); y++) {
                for (int x = 0; x < scaledImage.getWidth(); x++) {
                    int color = scaledImage.getPixel(x, y);
                    // Extract RGB values
                    int blue = color & 0xff;
                    int green = (color & 0xff00) >> 8;
                    int red = (color & 0xff0000) >> 16;

                    if (processingParams != null) {
                        // Possibly normalize and assign to tensor
                        tensorData[0][0][y][x] = (red / (processingParams.isNormalizePixelValues() ? 255f : 1f) - mean[0]) / standardDeviation[0];
                        tensorData[0][1][y][x] = (green / (processingParams.isNormalizePixelValues() ? 255f : 1f) - mean[1]) / standardDeviation[1];
                        tensorData[0][2][y][x] = (blue / (processingParams.isNormalizePixelValues() ? 255f : 1f) - mean[2]) / standardDeviation[2];
                    } else {
                        // Normalize and assign to tensor
                        tensorData[0][0][y][x] = (red / 255f - mean[0]) / standardDeviation[0];
                        tensorData[0][1][y][x] = (green / 255f - mean[1]) / standardDeviation[1];
                        tensorData[0][2][y][x] = (blue / 255f - mean[2]) / standardDeviation[2];
                    }
                }
            }
            InferInput inferInput = new InferInput();

            List<Integer> integerList = new ArrayList<>();
            for (long value : shape) {
                integerList.add((int) value); // Cast long to int
            }

            inferInput.setName("ImageRecognitionTensorProcessor request: " + UUID.randomUUID());
            inferInput.setDatatype(processingParams == null ? "ImageRecognition" : processingParams.getType());
            inferInput.setShape(shape);
            inferInput.setData(flatten(tensorData));
            ret.add(inferInput);
        }

        return ret;
    }

    private static Float[] flatten(float[][][][] array4D) {
        // Calculate total size first
        int totalSize = 0;
        for (float[][][] array3D : array4D) {
            for (float[][] array2D : array3D) {
                for (float[] array1D : array2D) {
                    totalSize += array1D.length;
                }
            }
        }

        // Create result array and fill it
        Float[] result = new Float[totalSize];
        int index = 0;
        for (float[][][] array3D : array4D) {
            for (float[][] array2D : array3D) {
                for (float[] array1D : array2D) {
                    System.arraycopy(array1D, 0, result, index, array1D.length);
                    index += array1D.length;
                }
            }
        }
        return result;
    }

    private static float[] softmax(float[] logits) {
        float sum = 0f;
        float[] softmaxValues = new float[logits.length];

        // Exponentiate each logit and sum them up
        for (int i = 0; i < logits.length; i++) {
            softmaxValues[i] = (float) Math.exp(logits[i]);
            sum += softmaxValues[i];
        }

        // Normalize by dividing each value by the sum
        for (int i = 0; i < softmaxValues.length; i++) {
            softmaxValues[i] /= sum;
        }

        return softmaxValues;
    }

    @Override
    public List<List<? extends TakmlResult>> processOutputTensor(List<InferOutput> outputs) {
        List<List<? extends TakmlResult>> ret = new ArrayList<>();
        for(InferOutput inferOutput : outputs) {
            float[] probabilities = inferOutput.getData();
            List<Recognition> results = new ArrayList<>();
            float[] probabilitiesOrdered = softmax(probabilities);
            int[] indices = new int[probabilitiesOrdered.length];
            for (int i = 0; i < probabilitiesOrdered.length; i++) indices[i] = i;

            SortedMap<Float, String> probabilityToLabel = new TreeMap<>(Collections.reverseOrder());

            for (int index : indices) {
                probabilityToLabel.put(probabilitiesOrdered[index], labels.get(index));
            }
            for (Map.Entry<Float, String> entry : probabilityToLabel.entrySet()) {
                results.add(new Recognition(entry.getValue(), entry.getKey()));
            }
            ret.add(results);
        }
        return ret;
    }
}
