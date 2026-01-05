package com.atakmap.android.takml_android.executorch_mx_plugin;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;

import com.atakmap.android.takml_android.lib.TakmlInitializationException;
import com.atakmap.android.takml_android.takml_result.RawTensorOutput;
import com.atakmap.android.takml_android.takml_result.Segmentation;
import com.atakmap.android.takml_android.tensor_processor.InferInput;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.pytorch.executorch.EValue;
import org.pytorch.executorch.Module;
import org.pytorch.executorch.Tensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Segmenter {
    private final List<String> labels;
    public Module module;
    private int modelInputWidth;
    private int modelInputHeight;
    private static final int CLASSNUM = 21;
    private static final String TAG = Segmenter.class.getSimpleName();

    public Segmenter(Context context, Uri modelUri, ExecutorchProcessingParams optionalObjectDetectionParams, List<String> labels) throws TakmlInitializationException {
        this.labels = labels;
        File tempFile = copyModelUriToTempFile(context, modelUri);
        module = Module.load(tempFile.getPath());
        if (optionalObjectDetectionParams != null) {
            modelInputWidth = optionalObjectDetectionParams.getModelInputWidth();
            modelInputHeight = optionalObjectDetectionParams.getModelInputHeight();
        }
    }

    private static File copyModelUriToTempFile(Context context, Uri modelUri) throws TakmlInitializationException {
        File tempFile;

        try {
            tempFile = File.createTempFile("model" + UUID.randomUUID(), ".pte", null);

            try (
                    InputStream inputStream = context.getContentResolver().openInputStream(modelUri);
                    FileOutputStream outputStream = new FileOutputStream(tempFile)
            ) {
                if (inputStream == null) {
                    throw new TakmlInitializationException("Input stream from URI is null: " + modelUri);
                }

                byte[] buffer = new byte[8192]; // 8KB buffer — tune as needed
                int bytesRead;
                long totalBytes = 0;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }

                Log.d(TAG, "Streamed " + totalBytes + " bytes to: " + tempFile.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new TakmlInitializationException("Failed to create or write to temp model file", e);
        }
        return tempFile;
    }

    public Segmentation segment(Bitmap bitmap) {
        Segmentation segmentation = new Segmentation();

        Bitmap mBitmap = Bitmap.createScaledBitmap(bitmap, modelInputWidth, modelInputHeight, true);

        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(mBitmap,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
        final float[] inputs = inputTensor.getDataAsFloatArray();

        final long startTime = SystemClock.elapsedRealtime();
        EValue[] outputTensors = module.forward(EValue.from(inputTensor));
        final long inferenceTime = SystemClock.elapsedRealtime() - startTime;
        Log.d("ImageSegmentation",  "inference time (ms): " + inferenceTime);

        final Tensor outputTensor = outputTensors[0].toTensor();
        final float[] scores = outputTensor.getDataAsFloatArray();
        int width = mBitmap.getWidth();
        int height = mBitmap.getHeight();
        int[] intValues = new int[width * height];
        for (int j = 0; j < height; j++) {
            for (int k = 0; k < width; k++) {
                int maxi = 0, maxj = 0, maxk = 0;
                double maxnum = -Double.MAX_VALUE;
                for (int i = 0; i < CLASSNUM; i++) {
                    float score = scores[i * (width * height) + j * width + k];
                    if (score > maxnum) {
                        maxnum = score;
                        maxi = i; maxj = j; maxk = k;
                    }
                }
                if (maxi == 0)
                    intValues[maxj * width + maxk] = 0xFFFF0000;
            }
        }

        Bitmap bmpSegmentation = Bitmap.createScaledBitmap(mBitmap, width, height, true);
        Bitmap outputBitmap = bmpSegmentation.copy(bmpSegmentation.getConfig(), true);
        outputBitmap.setPixels(intValues, 0, outputBitmap.getWidth(), 0, 0, outputBitmap.getWidth(), outputBitmap.getHeight());
        final Bitmap transferredBitmap = Bitmap.createScaledBitmap(outputBitmap, mBitmap.getWidth(), mBitmap.getHeight(), true);



        segmentation.setBitmap(Bitmap.createScaledBitmap(transferredBitmap, transferredBitmap.getWidth(), transferredBitmap.getHeight(), true));

        return segmentation;
    }

    public Segmentation segment2(Bitmap bitmap, float x, float y) {
        Segmentation segmentation = new Segmentation();
        Log.d(TAG, "Input bitmap dimensions: " + bitmap.getWidth() + "x" + bitmap.getHeight());

        // 1. Prepare Image Input
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, modelInputWidth, modelInputHeight, true);

        FloatBuffer buffer = ByteBuffer.allocateDirect(3 * modelInputWidth * modelInputHeight * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        TensorImageUtils.bitmapToFloatBuffer(scaledBitmap, 0, 0, modelInputWidth, modelInputHeight,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB, buffer, 0);

        Tensor inputTensor1 = Tensor.fromBlob(buffer, new long[]{3, modelInputWidth, modelInputHeight});

        // 2. Prepare Coordinate Input
        float xNew = x / bitmap.getWidth() * modelInputWidth;
        float yNew = y / bitmap.getHeight() * modelInputHeight;
        Log.d(TAG, "Transformed coordinates: (" + xNew + ", " + yNew + ")");

        // Flattened data for coordinates
        float[] xy = new float[]{xNew, yNew};

        Tensor inputTensor2 = Tensor.fromBlob(xy, new long[]{1, 1, 2});

        // 3. Run Inference
        long startTime = SystemClock.elapsedRealtime();
        EValue[] outputTensors;
        try {
            outputTensors = module.forward(EValue.from(inputTensor1), EValue.from(inputTensor2));
        } catch (Exception e) {
            Log.e(TAG, "Inference failed", e);
            return segmentation;
        }

        long inferenceTime = SystemClock.elapsedRealtime() - startTime;
        Log.d(TAG, "Inference time (ms): " + inferenceTime);

        // 4. Process Outputs
        // Ensure we actually have outputs
        if (outputTensors == null || outputTensors.length < 2) {
            Log.e(TAG, "Model did not return enough outputs. Got: " + (outputTensors == null ? "null" : outputTensors.length));
            return segmentation;
        }

        Tensor masksTensor = outputTensors[0].toTensor();
        Tensor confidenceTensor = outputTensors[1].toTensor();

        long[] maskShape = masksTensor.shape();
        Log.d(TAG, "Raw Mask Shape: " + Arrays.toString(maskShape));

        int maskChannels, maskHeight, maskWidth;

        // Handle different output ranks dynamically
        if (maskShape.length == 4) {
            // Shape: [Batch, Channels, Height, Width] -> [1, 3, 254, 254]
            maskChannels = (int) maskShape[1];
            maskHeight = (int) maskShape[2];
            maskWidth = (int) maskShape[3];
        } else if (maskShape.length == 3) {
            // Shape: [Channels, Height, Width] -> [3, 254, 254]
            maskChannels = (int) maskShape[0];
            maskHeight = (int) maskShape[1];
            maskWidth = (int) maskShape[2];
        } else {
            Log.e(TAG, "Unexpected mask shape rank: " + maskShape.length);
            return segmentation;
        }

        Log.d(TAG, "Parsed Output: W=" + maskWidth + " H=" + maskHeight + " C=" + maskChannels);

        // 5. Find Best Mask
        float[] confidenceScores = confidenceTensor.getDataAsFloatArray();
        float highestConfidence = -1;
        int bestMaskIndex = 0;

        // Use .length to avoid "index out of bounds" crash here too
        for (int i = 0; i < confidenceScores.length; i++) {
            if (confidenceScores[i] > highestConfidence) {
                highestConfidence = confidenceScores[i];
                bestMaskIndex = i;
            }
            Log.d(TAG, "Confidence [" + i + "]: " + confidenceScores[i]);
        }
        Log.d(TAG, "Using mask index: " + bestMaskIndex);

        // 6. Generate Output Bitmap
        float[] masks = masksTensor.getDataAsFloatArray();

        // Create a canvas bitmap scaled to the mask output size
        Bitmap outputScaledBitmap = Bitmap.createScaledBitmap(bitmap, maskWidth, maskHeight, true);
        int[] pixels = new int[maskWidth * maskHeight];
        outputScaledBitmap.getPixels(pixels, 0, maskWidth, 0, 0, maskWidth, maskHeight);

        int minChipX = maskWidth, minChipY = maskHeight, maxChipX = 0, maxChipY = 0;
        boolean maskFound = false;

        // Calculate offset to jump to the start of the 'bestMaskIndex' data
        int pixelCount = maskWidth * maskHeight;
        int startOffset = bestMaskIndex * pixelCount;

        for (int i = 0; i < pixelCount; i++) {
            // Check probability in the specific mask channel
            if (masks[startOffset + i] > 0.0f) { // Threshold can be adjusted (e.g. > 0.5f)
                // Apply Green Tint
                pixels[i] = addColorTint(pixels[i], 0x00FF00, 0.40f);

                // Update bounds for the chip
                int w = i % maskWidth;
                int h = i / maskWidth;

                minChipX = Math.min(w, minChipX);
                minChipY = Math.min(h, minChipY);
                maxChipX = Math.max(w, maxChipX);
                maxChipY = Math.max(h, maxChipY);
                maskFound = true;
            }
        }

        // Update the Segmentation object results
        segmentation.setCoordW(maskWidth);
        segmentation.setCoordH(maskHeight);

        // Create the full segmented image
        Bitmap segmentedBitmap = Bitmap.createBitmap(pixels, maskWidth, maskHeight, Bitmap.Config.ARGB_8888);
        // Scale back up to original size
        segmentation.setBitmap(Bitmap.createScaledBitmap(segmentedBitmap, bitmap.getWidth(), bitmap.getHeight(), true));

        // 7. Create Chip (Crop)
        if (maskFound && maxChipX > minChipX && maxChipY > minChipY) {
            int chipW = maxChipX - minChipX + 1;
            int chipH = maxChipY - minChipY + 1;

            // Extract chip from the tinted bitmap
            Bitmap chipBitmap = Bitmap.createBitmap(segmentedBitmap, minChipX, minChipY, chipW, chipH);
            segmentation.addChip(chipBitmap);

            // Add coordinates
            List<float[]> coords = new ArrayList<>();
            coords.add(new float[]{(float) minChipX, (float) minChipY, (float) maxChipX, (float) maxChipY});
            segmentation.setCoordinates(coords);

            Log.d(TAG, "Created chip: " + chipW + "x" + chipH);
        } else {
            Log.w(TAG, "No valid mask found.");
        }

        return segmentation;
    }

    private static int addColorTint(int baseColor, int tintRgb, float alphaFrac) {
        // alphaFrac in [0..1]
        int a = (baseColor >>> 24) & 0xFF;
        int br = (baseColor >>> 16) & 0xFF, bg = (baseColor >>> 8) & 0xFF, bb = baseColor & 0xFF;
        int tr = (tintRgb >>> 16) & 0xFF, tg = (tintRgb >>> 8) & 0xFF, tb = tintRgb & 0xFF;

        int nr = (int)(br * (1f - alphaFrac) + tr * alphaFrac);
        int ng = (int)(bg * (1f - alphaFrac) + tg * alphaFrac);
        int nb = (int)(bb * (1f - alphaFrac) + tb * alphaFrac);

        return (a << 24) | (nr << 16) | (ng << 8) | nb;
    }

    public List<RawTensorOutput> rawInference(List<InferInput> inferInputs) {
        List<EValue> inputs = new ArrayList<>();
        for (InferInput inferInput : inferInputs) {
            long[] shape = inferInput.getShape();
            Object rawData = inferInput.getData();

            Tensor inputTensor;
            if (rawData instanceof Float[]) {
                inputTensor = Tensor.fromBlob(ArrayUtils.toPrimitive((Float[]) rawData), shape);
            } else if (rawData instanceof Integer[]) {
                inputTensor = Tensor.fromBlob(ArrayUtils.toPrimitive((Integer[]) rawData), shape);
            } else if (rawData instanceof Long[]) {
                inputTensor = Tensor.fromBlob(ArrayUtils.toPrimitive((Long[]) rawData), shape);
            } else if (rawData instanceof Byte[]) {
                inputTensor = Tensor.fromBlob(ArrayUtils.toPrimitive((Byte[]) rawData), shape);
            } else if (rawData instanceof Double[]) {
                inputTensor = Tensor.fromBlob(ArrayUtils.toPrimitive((Double[]) rawData), shape);
            } else {
                Log.e(TAG, "Unsupported data type: " + rawData.getClass());
                return null;
            }

            inputs.add(EValue.from(inputTensor));
        }
        Log.d(TAG, "Gathered inputs and running model");
        EValue[] outputTensors = module.forward(inputs.toArray(new EValue[]{}));
        Log.d(TAG, "Ran model");
        List<RawTensorOutput> ret = new ArrayList<>();
        for (EValue eValue : outputTensors) {
            float[] output = eValue.toTensor().getDataAsFloatArray();
            ret.add(new RawTensorOutput(eValue.toTensor().shape(), output));
        }
        return ret;
    }

    public static int addTint(int color, float tintFactor) {
        int alpha = (color >> 24) & 0xFF;
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;

        int newRed = (int) (red + (255 - red) * tintFactor);
        int newGreen = (int) (green + (255 - green) * tintFactor);
        int newBlue = (int) (blue + (255 - blue) * tintFactor);

        return (alpha << 24) | (newRed << 16) | (newGreen << 8) | newBlue;
    }
}
