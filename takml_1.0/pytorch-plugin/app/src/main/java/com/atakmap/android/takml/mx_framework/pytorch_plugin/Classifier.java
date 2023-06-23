package com.atakmap.android.takml.mx_framework.pytorch_plugin;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.Pair;

import org.pytorch.LiteModuleLoader;
import org.pytorch.MemoryFormat;
import org.pytorch.Tensor;
import org.pytorch.Module;
import org.pytorch.IValue;
import org.pytorch.torchvision.TensorImageUtils;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;


public class Classifier {
    private static final String TAG = Classifier.class.getName();
    private static final int INPUT_TENSOR_WIDTH = 224;
    private static final int INPUT_TENSOR_HEIGHT = 224;
    private final Module module;

    public Classifier(String modelPath){
        module = LiteModuleLoader.load(modelPath);
    }

    public int argMax(float[] inputs){
        int maxIndex = -1;
        float maxvalue = 0.0f;
        for (int i = 0; i < inputs.length; i++){
            if(inputs[i] > maxvalue) {
                maxIndex = i;
                maxvalue = inputs[i];
            }
        }
        return maxIndex;
    }

    /**
     * Find the maximum element in an array
     *
     * @param array the array
     * @return the maximum element
     */
    public static float maximum(float[] array) {
        if (array.length <= 0)
            throw new IllegalArgumentException("The array is empty");
        float max = array[0];
        for (int i = 1; i < array.length; i++)
            if (array[i] > max)
                max = array[i];
        return max;
    }

    private double softmax(double input, double[] neuronValues) {
        double total = Arrays.stream(neuronValues).map(Math::exp).sum();
        return Math.exp(input) / total;
    }

    public static double[] convertFloatsToDoubles(float[] input)
    {
        if (input == null)
        {
            return null;
        }
        double[] output = new double[input.length];
        for (int i = 0; i < input.length; i++)
        {
            output[i] = input[i];
        }
        return output;
    }

    public Pair<String, Float> predict(Bitmap bitmap, List<String> labels){
        bitmap = Bitmap.createScaledBitmap(bitmap,INPUT_TENSOR_WIDTH,INPUT_TENSOR_HEIGHT,false);

        // preparing input tensor
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB,
                MemoryFormat.CHANNELS_LAST);

        // running the model
        final Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();

        // getting tensor content as java array of floats
        final float[] scores = outputTensor.getDataAsFloatArray();

        int classIndex = argMax(scores);
        float confidence = (float) softmax(maximum(scores), convertFloatsToDoubles(scores));
        Log.d(TAG, "predict result: " + labels.get(classIndex) + " " + confidence);

        return new Pair<>(labels.get(classIndex), confidence);
    }

}