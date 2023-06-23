package com.atakmap.android.takml.mx_framework.plugin;

import android.content.Context;
import android.util.Log;

import com.bbn.tak.ml.mx_framework.MXPlugin;
import com.bbn.takml_sdk_android.mx_framework.Recognition;
import com.atakmap.android.takml.mx_framework.plugin.weka_implementations.WekaImpl;
import com.bbn.tak.ml.mx_framework.MXPluginDescription;

import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@MXPluginDescription(
        id= "1111111111",
        name="WekaPlugin",
        author="Edward Lu",
        library="None",
        algorithm="None",
        version="None",
        clientSide=true,
        serverSide=false,
        description="Simple plugin to demonstrate Weka"
)
public class WekaPlugin implements MXPlugin {

    public static final String TAG = WekaPlugin.class.getSimpleName();

    private MXPluginDescription desc;

    private WekaImpl impl_;
    private String modelDirectoryName_;
    private String modelFileName_;

    public WekaPlugin() {
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
        impl_ = new WekaImpl();
        modelDirectoryName_ = modelDirectory;
        modelFileName_ = modelFilename;
        return true;
    }

    @Override
    public byte[] execute(byte[] inputData) {

        Log.d(TAG, "execute() (modelDirectoryName: " + modelDirectoryName_ + ", " +
                "modelFileName: " + modelFileName_);

        List<Recognition> results =
                impl_.execute(inputData, modelDirectoryName_, modelFileName_);

        if (results == null) {
            Log.e(TAG, "Weka execution failed.");
            return null;
        }

        Log.d(TAG, "execute() result: " + results);

        ArrayList<Recognition> resultsArrayList = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            resultsArrayList.add(results.get(i));
        }

        byte[] resultBytes = null;
        try {
            resultBytes = SerializationUtils.serialize(resultsArrayList);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error serializing result: " + e.getMessage());
            return null;
        }

        Log.d(TAG, "result bytes: " + Arrays.toString(resultBytes));

        return resultBytes;
    }

    @Override
    public boolean destroy() {
        Log.i(TAG, "destroy()");
        return true;
    }

}
