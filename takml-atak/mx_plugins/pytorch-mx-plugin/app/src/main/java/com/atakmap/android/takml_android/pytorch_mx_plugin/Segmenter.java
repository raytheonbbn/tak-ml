package com.atakmap.android.takml_android.pytorch_mx_plugin;

import android.graphics.Bitmap;
import android.util.Log;

import com.atakmap.android.takml_android.lib.TakmlInitializationException;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Segmenter {
    private final Module module;
    private final List<String> labels;

    private static final float DEFAULT_THRESHOLD = 0.4867f;
    private static final int DEFAULT_MIN_AREA_PIXELS = 10;

    private static final int DEFAULT_HEIGHT = 224, DEFAULT_WIDTH = 224;

    private final float threshold;
    private final int minAreaPixels;
    private final int inputHeight;
    private final int inputWidth;

    final float[] DEFAULT_MEAN = new float[]{0f, 0f, 0f};
    final float[] DEFAULT_STD  = new float[]{1f, 1f, 1f};

    private float[] mean, std;


    public Segmenter(byte[] model, List<String> labels, PromptlessSegmentationProcParams procParams)
            throws TakmlInitializationException {
        this.labels = labels;

        File tmp;
        try {
            tmp = File.createTempFile("model", ".pt", null);
            try (FileOutputStream fos = new FileOutputStream(tmp)) {
                fos.write(model);
            }
        } catch (IOException e) {
            throw new TakmlInitializationException("Model tmp write failed", e);
        }

        // Use LiteModuleLoader only if you saved with _save_for_lite_interpreter.
        module = LiteModuleLoader.load(tmp.getAbsolutePath());

        if(procParams != null){
            threshold = procParams.getThreshold();
            minAreaPixels = procParams.getMinAreaPixels();
            inputWidth = procParams.getInputWidth();
            inputHeight = procParams.getInputHeight();
            mean = procParams.getMean();
            std = procParams.getStd();
        }else{
            threshold = DEFAULT_THRESHOLD;
            minAreaPixels = DEFAULT_MIN_AREA_PIXELS;
            inputWidth = DEFAULT_WIDTH;
            inputHeight = DEFAULT_HEIGHT;
            mean = DEFAULT_MEAN;
            std = DEFAULT_STD;
        }
    }

    public List<PromptlessSegmentation> runSegmentation(Bitmap bitmap) {
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true);

        Tensor chw = TensorImageUtils.bitmapToFloat32Tensor(scaled, mean, std); // [3,H,W] in [0,1]

        // 3) Add batch dim -> [1,3,H,W]
        float[] chwData = chw.getDataAsFloatArray();
        Tensor input = Tensor.fromBlob(chwData, new long[]{1, 3, inputHeight, inputWidth});

        // 4) Forward (handle dict/tuple/tensor)
        IValue outVal = module.forward(IValue.from(input));
        Tensor outTensor;
        if (outVal.isDictStringKey()) {
            Map<String, IValue> d = outVal.toDictStringKey();
            outTensor = d.containsKey("out") ? d.get("out").toTensor()
                    : d.values().iterator().next().toTensor();
        } else if (outVal.isTuple()) {
            IValue[] t = outVal.toTuple();
            outTensor = t[0].toTensor();
        } else {
            outTensor = outVal.toTensor();
        }

        long[] shape = outTensor.shape(); // expect [1,C,H,W] or [C,H,W]
        final int dims = shape.length;
        final int C = (int) (dims == 4 ? shape[1] : shape[0]);
        final int H = (int) (dims == 4 ? shape[2] : shape[1]);
        final int W = (int) (dims == 4 ? shape[3] : shape[2]);
        float[] out = outTensor.getDataAsFloatArray();

        // 5) Build label map and confidence map
        final int[]   labelMap = new int[H * W];
        final float[] confMap  = new float[H * W];

        if (C == 1) {
            int positives = 0;
            double meanProb = 0.0;
            for (int y = 0; y < H; y++) {
                for (int x = 0; x < W; x++) {
                    int idx = y * W + x;
                    float logit = getCHW(out, 0, x, y, C, H, W);
                    float prob  = sigmoid(logit);
                    confMap[idx] = prob;                         // per-pixel confidence
                    boolean on = prob > threshold;
                    labelMap[idx] = on ? 0 : -1;
                    if (on) positives++;
                    meanProb += prob;
                }
            }
            meanProb /= (H * W);
            android.util.Log.i("Segmenter",
                    String.format(Locale.US, "meanProb=%.4f posFrac=%.4f",
                            meanProb, positives / (float) (H * W)));
        } else {
            for (int y = 0; y < H; y++) {
                for (int x = 0; x < W; x++) {
                    int idx = y * W + x;

                    // First pass: find winning class and its logit
                    int bestC = 0;
                    float maxLogit = -Float.MAX_VALUE;
                    for (int c = 0; c < C; c++) {
                        float v = getCHW(out, c, x, y, C, H, W);
                        if (v > maxLogit) {
                            maxLogit = v;
                            bestC = c;
                        }
                    }

                    // Second pass: stable softmax denominator
                    double sumExp = 0.0;
                    for (int c = 0; c < C; c++) {
                        double v = getCHW(out, c, x, y, C, H, W) - maxLogit;
                        sumExp += Math.exp(v);
                    }

                    float bestProb = (float) (1.0 / sumExp);    // softmax prob of argmax class
                    labelMap[idx] = bestC;
                    confMap[idx]  = bestProb;                   // per-pixel confidence
                }
            }
        }

        // 6) Connected components with average confidence
        List<PromptlessSegmentation> results = new ArrayList<>();
        final boolean[] visited = new boolean[H * W];
        final int[] dx = {1, -1, 0, 0};
        final int[] dy = {0, 0, 1, -1};

        PromptlessSegmentation promptlessSegmentation = new PromptlessSegmentation();
        promptlessSegmentation.setRawMasks(out);
        List<PromptlessSegmentationDetection> detections = new ArrayList<>();

        for (int y0 = 0; y0 < H; y0++) {
            for (int x0 = 0; x0 < W; x0++) {
                int idx0 = y0 * W + x0;
                if (visited[idx0]) continue;

                int seed = labelMap[idx0];
                if (seed < 0) { // skip background for binary case
                    visited[idx0] = true;
                    continue;
                }

                int area = 0;
                long sumX = 0, sumY = 0;
                double sumConf = 0.0;                           // accumulate pixel confidences
                Deque<int[]> q = new ArrayDeque<>();
                q.add(new int[]{x0, y0});
                visited[idx0] = true;

                while (!q.isEmpty()) {
                    int[] p = q.removeFirst();
                    int x = p[0], y = p[1], idx = y * W + x;
                    area++;
                    sumX += x;
                    sumY += y;
                    sumConf += confMap[idx];

                    for (int k = 0; k < 4; k++) {
                        int nx = x + dx[k], ny = y + dy[k];
                        if (nx < 0 || nx >= W || ny < 0 || ny >= H) continue;
                        int nidx = ny * W + nx;
                        if (visited[nidx]) continue;
                        if (labelMap[nidx] != seed) continue;
                        visited[nidx] = true;
                        q.addLast(new int[]{nx, ny});
                    }
                }

                if (area >= minAreaPixels) {
                    float cx = (float) (sumX / (double) area + 0.5);
                    float cy = (float) (sumY / (double) area + 0.5);
                    String label = seed < labels.size()
                            ? labels.get(seed) : String.format(Locale.US, "class_%d", seed);

                    float avgConf = (float) (sumConf / Math.max(1, area)); // 0..1

                    PromptlessSegmentationDetection ps = new PromptlessSegmentationDetection();
                    ps.setCoordinates(new float[]{cx, cy}); // NOTE: model-space coords
                    ps.setLabel(label);
                    ps.setConfidence(avgConf);
                    detections.add(ps);
                }
            }
        }
        promptlessSegmentation.setDetections(detections);
        results.add(promptlessSegmentation);

        Log.i("Segmenter", "components=" + results.size());
        return results;
    }

    private float getCHW(float[] a, int c, int x, int y, int C, int H, int W) {
        return a[c * H * W + y * W + x]; // N==1 baked in
    }

    private float sigmoid(float x) {
        return (float) (1.0 / (1.0 + Math.exp(-x)));
    }
}
