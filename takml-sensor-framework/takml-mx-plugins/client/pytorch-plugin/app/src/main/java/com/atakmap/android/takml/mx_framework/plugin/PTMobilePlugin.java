package com.atakmap.android.takml.mx_framework.plugin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import com.atakmap.coremap.log.Log;
import com.bbn.tak.ml.mx_framework.MXPlugin;
import com.bbn.tak.ml.mx_framework.MXPluginDescription;
import com.bbn.takml_sdk_android.mx_framework.Recognition;

import org.apache.commons.lang3.SerializationUtils;
import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

@MXPluginDescription(
        id="0987654321",
        name="PTMobilePlugin",
        author="Devon Minor",
        library="org.pytorch:pytorch_android",
        algorithm="None",
        version="0.0.1",
        clientSide=true,
        serverSide=false,
        description="PyTorch Mobile plugin to be used for ATAK image classifications with PyTorch models"
)
public class PTMobilePlugin implements MXPlugin {

    public static final String TAG = PTMobilePlugin.class.getSimpleName();

    private MXPluginDescription desc;

    private String modelDirectory;
    private String modelFilename;

    private Module module;

    public PTMobilePlugin() {
        this.desc = getClass().getAnnotation(MXPluginDescription.class);
        if (this.desc == null)
            throw new RuntimeException("Must complete MXPluginDescription annotation");
    }

    @Override
    public String getPluginID() {
        return this.desc.id();
    }

    @Override
    public MXPluginDescription getPluginDescription() {
        return this.desc;
    }

    @Override
    public boolean instantiate(String modelDirectory, String modelFilename,
                               HashMap<String, Serializable> params) {
        this.modelDirectory = modelDirectory;
        this.modelFilename = modelFilename;

        File file = new File(modelDirectory, modelFilename);
        Log.d(TAG, "Loading model at " + file.getAbsolutePath());
        this.module = Module.load(file.getAbsolutePath());
        
        return true;
    }

    @Override
    public byte[] execute(byte[] inputData) {
        Log.d(TAG,"execute() in PTMobilePlugin");

        Bitmap bitmap;

        // LOAD INPUT DATA
        try {
            bitmap = BitmapFactory.decodeByteArray(inputData, 0, inputData.length);
        } catch (Exception e) {
            Log.e(TAG, "Error occurred while trying to read assets: " + e.getMessage());
            e.printStackTrace();
            return null;
        }

        // SCALE DOWN BITMAP AND CONVERT TO TENSOR
        bitmap = Bitmap.createScaledBitmap(bitmap, 28, 28, false);
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
        bitmap.recycle();

        // PASS DATA TO MODEL
        Tensor outputTensor;
        try {
            outputTensor = this.module.forward(IValue.from(inputTensor)).toTensor();
        } catch (Exception e) {
            Log.e(TAG, "Could not run input data against model: " + e.getMessage());
            e.printStackTrace();
            return null;
        }

        // GET THE SCORES FROM THE OUTPUT IN DESCENDING CONFIDENCE
        final float[] scores = outputTensor.getDataAsFloatArray();
        ArrayList<Recognition> results = new ArrayList<>();
        for (int i = 0; i < scores.length; i++) {
            results.add(new Recognition(
                    String.valueOf(i),
                    this.modelFilename,
                    scores[i]
            ));
        }
        Collections.sort(results, (o1, o2) -> o2.getConfidence().compareTo(o1.getConfidence()));

        // SERIALIZE THE RESULTS
        byte[] resultBytes;
        try {
            resultBytes = SerializationUtils.serialize(results);
        } catch (Exception e) {
            Log.e(TAG, "Error occured while trying to serialize response: " + e.getMessage());
            e.printStackTrace();
            return null;
        }

        return resultBytes;
    }

    @Override
    public boolean destroy() {
        System.err.println("stop() in PTMobilePlugin");
        return true;
    }
}
