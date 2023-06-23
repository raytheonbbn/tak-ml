package com.atakmap.android.takml.mx_framework.pytorch_plugin;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Pair;

import com.atakmap.coremap.log.Log;
import com.bbn.tak.ml.mx_framework.MXFrameworkConstants;
import com.bbn.tak.ml.mx_framework.MXPlugin;
import com.bbn.tak.ml.mx_framework.MXPluginDescription;
import com.bbn.takml_sdk_android.mx_framework.ObjectDetection;
import com.bbn.takml_sdk_android.mx_framework.Recognition;
import com.bbn.takml_sdk_android.mx_framework.parameters.ObjectDetectionParams;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

@MXPluginDescription(
        id="0987654321",
        name="PTMobilePlugin",
        author="Devon Minor, Brandon Kalashian",
        library="org.pytorch:pytorch_android",
        algorithm="None",
        version="0.0.1",
        clientSide=true,
        serverSide=false,
        description="PyTorch Mobile plugin to be used for image recognition"
)
public class PTMobilePlugin implements MXPlugin {

    public static final String TAG = PTMobilePlugin.class.getName();

    private final MXPluginDescription desc;

    private String modelFilename;
    private Classifier classifier;
    private ObjectDetector objectDetector;
    private final List<String> labels = new ArrayList<>();

    public PTMobilePlugin() {
        this.desc = getClass().getAnnotation(MXPluginDescription.class);
        if (this.desc == null) {
            throw new RuntimeException("Must complete MXPluginDescription annotation");
        }
    }

    @Override
    public String getPluginID() {
        return this.desc.id();
    }

    @Override
    public MXPluginDescription getPluginDescription() {
        return this.desc;
    }

    /**
     * Instantiate the PT Mobile Plugin with a Model
     *
     * @param modelDirectory - where the model and associated files are stored
     * @param modelFilename - model file name
     * @param params - parameters
     *
     * @return operation successful
     */
    @Override
    public boolean instantiate(String modelDirectory, String modelFilename,
                               HashMap<String, Serializable> params) {
        Log.d(TAG, "Calling instantiation");
        this.modelFilename = modelFilename;

        /** 1. Grab the model file **/
        File file = new File(modelDirectory, modelFilename);
        Log.d(TAG, "Loading model at " + file.getAbsolutePath());

        /**
         * Loads the Processing Config, which provides metadata about how to process data
         * with the Model.
         *
         * e.g.:
         * {
         *   "modelInputWidth": 640, // the expected input image width pixels
         *   "modelInputHeight": 640, // the expected input image height pixels
         *   "tensorOutputNumberRows": 25200, // the number of Tensor output rows
         *   "tensorOutputNumberColumns": 15, // the number of Tensor output columns
         *                                    // (first few bounds, last several are classes)
         *   "normMeanRGB": [0, 0, 0],
         *   "normStdRGB": [1, 1, 1],
         *   "type": "[...].ObjectDetectionParams" // Type, e.g. Object Detection Params
         *
         * }
         */
        String processingConfigFile = (String) params.get
                (MXFrameworkConstants.MX_PLUGIN_PARAM_INPUT_PROCESSING_CONFIG);
        if(processingConfigFile != null){
            ObjectDetectionParams objectDetectionParams;

            byte[] bytes;
            try {
                bytes = Files.readAllBytes(Paths.get(modelDirectory + "/" + processingConfigFile));
            } catch (IOException e) {
                Log.e(TAG, "IO Exception reading model bytes", e);
                return false;
            }
            String fileContent = new String (bytes);
            Log.d(TAG, "Loaded processing config: " + fileContent);
            /** Deserialize Json representation **/
            objectDetectionParams = new Gson().fromJson
                    (fileContent, ObjectDetectionParams.class);
            if(objectDetectionParams != null && objectDetectionParams.getModelInputHeight() > 0) {
                /** Construct an Object Detector **/
                objectDetector = new ObjectDetector(file.getAbsolutePath(), objectDetectionParams);
            }
        }

        /**
         * If not Object Detection, instantiate an Image Classifier
         */
        if(objectDetector == null){
            Log.d(TAG, "instantiate: returning classifier");
            try {
                classifier = new Classifier(file.getAbsolutePath());
            }catch (Exception e){
                Log.e(TAG, "Exception loading Classifier", e);
                return false;
            }
            Log.d(TAG, "Returning Classifier " + classifier);
        }

        /**
         * Load labels
         * e.g.:
         * pedestrian, people, bicycle, car, van, truck, tricycle, awning-tricycle,
         * bus, motor
         **/
        Object labelsObject = params.get(MXFrameworkConstants.MX_PLUGIN_PARAM_LABELS);
        if(labelsObject == null){
            Log.e(TAG, "Could not load labels, mx plugin param labels null");
            return false;
        }
        String labelsFileName = labelsObject.toString();
        Scanner scanner;
        try {
            scanner = new Scanner(new File(modelDirectory + "/" + labelsFileName));
        } catch (FileNotFoundException e) {
            Log.e(TAG, "labels file name not found", e);
            return false;
        }
        while (scanner.hasNext()){
            labels.add(scanner.next());
        }
        scanner.close();

        return true;
    }

    /**
     * Execute input data against Model
     *
     * @param inputDataBitmap byte array Bitmap input image
     *
     * @return results
     */
    @Override
    public byte[] execute(byte[] inputDataBitmap) {
        Log.d(TAG,"Calling execution");

        /** Load the input data as a Bitmap **/
        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeByteArray(inputDataBitmap, 0, inputDataBitmap.length);
        } catch (Exception e) {
            Log.e(TAG, "Error occurred while trying to read assets", e);
            return null;
        }

        /** The request type is Object Detection **/
        if(objectDetector != null){
            /** Call the underlying model execution **/
            ArrayList<ObjectDetection> result = objectDetector.analyzeImage(bitmap, labels);

            /** Return serialized results **/
            try {
                return SerializationUtils.serialize(result);
            } catch (SerializationException e) {
                Log.e(TAG, "Error occurred while trying to serialize response", e);
            }

        /** The request type is Image Classification (default) **/
        }else{
            Pair<String, Float> pred = classifier.predict(bitmap, labels);

            ArrayList<Recognition> recognitionsRet = new ArrayList<>(
                    Collections.singletonList(new Recognition(
                    pred.first,
                    this.modelFilename,
                    pred.second
            )));

            /** Return serialized results **/
            try {
               return SerializationUtils.serialize(recognitionsRet);
            } catch (SerializationException e) {
                Log.e(TAG, "Error occurred while trying to serialize response", e);
            }
        }

        return null;
    }

    @Override
    public boolean destroy() {
        Log.d(TAG, "Shutting down");
        return true;
    }
}
