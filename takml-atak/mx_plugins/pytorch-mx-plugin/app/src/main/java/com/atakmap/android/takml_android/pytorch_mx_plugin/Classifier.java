package com.atakmap.android.takml_android.pytorch_mx_plugin;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.Pair;

import com.atakmap.android.takml_android.lib.TakmlInitializationException;
import com.atakmap.android.takml_android.takml_result.Recognition;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.MemoryFormat;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class Classifier {
    private static final String TAG = Classifier.class.getName();
    private static final int INPUT_TENSOR_WIDTH = 224;
    private static final int INPUT_TENSOR_HEIGHT = 224;
    private final Module module;

    public Classifier(byte[] model) throws TakmlInitializationException{
        String path = null;
        File tempFile;
        try {
            tempFile = File.createTempFile("model", ".pt", null);
        } catch (IOException e) {
            throw new TakmlInitializationException("Could not create temporary file for model", e);
        }
        try(FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(model);
            path = tempFile.getAbsolutePath();
        } catch (IOException e) {
            throw new TakmlInitializationException("Could not create temporary file for model", e);
        }
        module = LiteModuleLoader.load(path);
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

    public List<Recognition> getOrderedRecognitions(float[] inputs, List<String> labels){
        double[] doubleInputs = convertFloatsToDoubles(inputs);

        TreeMap<Float, Integer> confToIndex = new TreeMap<>(Collections.reverseOrder());
        for (int i = 0; i < inputs.length; i++){
            confToIndex.put(inputs[i], i);
        }
        List<Recognition> ret = new ArrayList<>();
        for(Map.Entry<Float, Integer> i : confToIndex.entrySet()){
            ret.add(new Recognition(labels.get(i.getValue()), (float) softmax(i.getKey(), doubleInputs)));
        }
        return ret;
    }

    public List<Recognition>  predict(Bitmap bitmap, List<String> labels){
        bitmap = Bitmap.createScaledBitmap(bitmap,INPUT_TENSOR_WIDTH,INPUT_TENSOR_HEIGHT,false);

        // preparing input tensor
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB,
                MemoryFormat.CHANNELS_LAST);

        // running the model
        final Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();

        // getting tensor content as java array of floats
        final float[] scores = outputTensor.getDataAsFloatArray();

        List<Recognition> recognitions = getOrderedRecognitions(scores, labels);
        Log.d(TAG, "predict result: " + recognitions.get(0).getLabel() + " " +
                recognitions.get(0).getConfidence());

        return recognitions;
    }

}