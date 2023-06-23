package com.atakmap.android.takml.mx_framework.plugin;

import android.content.Context;
import android.util.Log;

import com.atakmap.android.takml.mx_framework.plugin.tflite_implementations.TFLiteImpl;
import com.bbn.tak.ml.mx_framework.MXFrameworkConstants;
import com.bbn.tak.ml.mx_framework.MXPlugin;
import com.bbn.tak.ml.mx_framework.MXPluginDescription;
import com.bbn.takml_sdk_android.mx_framework.Recognition;

import org.apache.commons.lang3.SerializationUtils;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.vision.classifier.Classifications;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

@MXPluginDescription(
        id= "2345678901",
        name="TFLitePlugin",
        author="Edward Lu",
        library="None",
        algorithm="None",
        version="None",
        clientSide=true,
        serverSide=false,
        description="Simple plugin to demonstrate TensorFlow Lite"
)
public class TFLitePlugin implements MXPlugin {

    public static final String TAG = TFLitePlugin.class.getSimpleName();

    private HashMap<String, Serializable> params;
    private MXPluginDescription desc;

    private TFLiteImpl impl_;
    private Context ctx_;
    private String modelDirectoryName_;
    private String modelFileName_;

    public TFLitePlugin() {
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
        Log.d(TAG, "Successfully instantiated instance of TFLitePlugin");
        modelDirectoryName_ = modelDirectory;
        modelFileName_ = modelFilename;
        this.params = params;
        impl_ = new TFLiteImpl();
        return true;
    }

    @Override
    public byte[] execute(byte[] inputData) {

        String labelsFileName = params.get(MXFrameworkConstants.MX_PLUGIN_PARAM_LABELS).toString();

        String inputProcessingConfigFileName = params.get(MXFrameworkConstants.MX_PLUGIN_PARAM_INPUT_PROCESSING_CONFIG).toString();

        Log.d(TAG, "execute() in TFLitePlugin (modelDirectoryName: " + modelDirectoryName_ + ", " +
                "modelFileName: " + modelFileName_ + ", " + "labelsFileName: " + labelsFileName);

        return impl_.execute(inputData, modelDirectoryName_, modelFileName_, labelsFileName,
                            inputProcessingConfigFileName);
    }

    @Override
    public boolean destroy() {
        Log.i(TAG, "destroy() in TFLitePlugin");
        return true;
    }
}
