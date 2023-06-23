package com.atakmap.android.takml.mx_framework.pytorch_plugin;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;

import com.bbn.takml_sdk_android.mx_framework.ObjectDetection;
import com.bbn.takml_sdk_android.mx_framework.parameters.ObjectDetectionParams;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ObjectDetector {
    private final Module module;
    private final int modelInputWidth;
    private final int modelInputHeight;
    private final int tensorOutputNumberRows;
    private final int tensorOutputNumberColumns;
    private final float[] normMeanRGB;
    private final float[] normStdRGB;

    private static final int NMS_LIMIT = 15;
    private static final float MINIMUM_THRESHOLD = 0.30f;

    public ObjectDetector(String modelPath, ObjectDetectionParams objectDetectionParams){
        module = LiteModuleLoader.load(modelPath);
        modelInputWidth = objectDetectionParams.getModelInputWidth();
        modelInputHeight = objectDetectionParams.getModelInputHeight();
        tensorOutputNumberRows = objectDetectionParams.getTensorOutputNumberRows();
        tensorOutputNumberColumns = objectDetectionParams.getTensorOutputNumberColumns();
        normMeanRGB = objectDetectionParams.getNormMeanRGB();
        normStdRGB = objectDetectionParams.getNormStdRGB();
    }

    public ArrayList<ObjectDetection> analyzeImage(Bitmap bitmap, List<String> labels){
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, modelInputWidth, modelInputHeight,
                true);

        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap,
                normMeanRGB, normStdRGB);
        IValue[] outputTuple = module.forward(IValue.from(inputTensor)).toTuple();
        final Tensor outputTensor = outputTuple[0].toTensor();
        final float[] outputs = outputTensor.getDataAsFloatArray();

        float imgScaleX = (float)bitmap.getWidth() / modelInputWidth;
        float imgScaleY = (float)bitmap.getHeight() / modelInputHeight;
        float ivScaleX = (float)modelInputWidth / bitmap.getWidth();
        float ivScaleY = (float)modelInputHeight / bitmap.getHeight();

        ArrayList<ObjectDetection> results = new ArrayList<>();
        float startX = 0;
        float startY = 0;
        for (int i = 0; i< tensorOutputNumberRows; i++) {
            if (outputs[i* tensorOutputNumberColumns +4] > MINIMUM_THRESHOLD) {
                float x = outputs[i* tensorOutputNumberColumns];
                float y = outputs[i* tensorOutputNumberColumns +1];
                float w = outputs[i* tensorOutputNumberColumns +2];
                float h = outputs[i* tensorOutputNumberColumns +3];

                float left = imgScaleX * (x - w/2);
                float top = imgScaleY * (y - h/2);
                float right = imgScaleX * (x + w/2);
                float bottom = imgScaleY * (y + h/2);

                float max = outputs[i* tensorOutputNumberColumns +5];
                int clsIndex = 0;
                for (int j = 0; j < tensorOutputNumberColumns -5; j++) {
                    if (outputs[i* tensorOutputNumberColumns +5+j] > max) {
                        max = outputs[i* tensorOutputNumberColumns +5+j];
                        clsIndex = j;
                    }
                }

                Rect rect = new Rect((int)(startX+ivScaleX*left), (int)(startY+top*ivScaleY),
                        (int)(startX+ivScaleX*right), (int)(startY+ivScaleY*bottom));
                ObjectDetection result = new ObjectDetection(String.valueOf(clsIndex),
                        labels.get(clsIndex), outputs[i*tensorOutputNumberColumns+4], rect.bottom,
                        rect.left, rect.right, rect.top);
                results.add(result);
            }
        }

        return nonMaxSuppression(results);
    }

    /**
     Removes bounding boxes that overlap too much with other boxes that have
     a higher score.
     - Parameters:
     - boxes: an array of bounding boxes and their scores
     - limit: the maximum number of boxes that will be selected
     - threshold: used to decide whether boxes overlap too much
     */
    private ArrayList<ObjectDetection> nonMaxSuppression(ArrayList<ObjectDetection> boxes) {

        // Do an argsort on the confidence scores, from high to low.
        boxes.sort(Comparator.comparing(ObjectDetection::getConfidence));

        ArrayList<ObjectDetection> selected = new ArrayList<>();
        boolean[] active = new boolean[boxes.size()];
        Arrays.fill(active, true);
        int numActive = active.length;

        // The algorithm is simple: Start with the box that has the highest score.
        // Remove any remaining boxes that overlap it more than the given threshold
        // amount. If there are any boxes left (i.e. these did not overlap with any
        // previous boxes), then repeat this procedure, until no more boxes remain
        // or the limit has been reached.
        boolean done = false;
        for (int i=0; i<boxes.size() && !done; i++) {
            if (active[i]) {
                ObjectDetection boxA = boxes.get(i);
                selected.add(boxA);
                if (selected.size() >= ObjectDetector.NMS_LIMIT) break;

                for (int j=i+1; j<boxes.size(); j++) {
                    if (active[j]) {
                        ObjectDetection boxB = boxes.get(j);
                        if (iOU(new Rect(boxA.getLeft(), boxA.getTop(), boxA.getRight(),
                                boxA.getBottom()), new Rect(boxB.getLeft(), boxB.getTop(),
                                boxB.getRight(), boxB.getBottom())) >
                                ObjectDetector.MINIMUM_THRESHOLD) {
                            active[j] = false;
                            numActive -= 1;
                            if (numActive <= 0) {
                                done = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return selected;
    }

    /**
     Computes intersection-over-union overlap between two bounding boxes.
     */
    private float iOU(Rect a, Rect b) {
        float areaA = (a.right - a.left) * (a.bottom - a.top);
        if (areaA <= 0.0) return 0.0f;

        float areaB = (b.right - b.left) * (b.bottom - b.top);
        if (areaB <= 0.0) return 0.0f;

        float intersectionMinX = Math.max(a.left, b.left);
        float intersectionMinY = Math.max(a.top, b.top);
        float intersectionMaxX = Math.min(a.right, b.right);
        float intersectionMaxY = Math.min(a.bottom, b.bottom);
        float intersectionArea = Math.max(intersectionMaxY - intersectionMinY, 0) *
                Math.max(intersectionMaxX - intersectionMinX, 0);
        return intersectionArea / (areaA + areaB - intersectionArea);
    }
}
