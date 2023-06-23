package com.atakmap.android.takml.mx_framework.plugin.weka.binaryclassifier;

import com.atakmap.coremap.log.Log;
import com.bbn.tak.ml.mx_framework.MXPlugin;
import com.bbn.tak.ml.mx_framework.MXPluginDescription;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@MXPluginDescription(
        id="4570384440",
        name="WekaBinaryClassifierPlugin",
        author="Cody Doucette",
        library="Weka",
        algorithm="Binary classification",
        version="3.8.4",
        clientSide=true,
        serverSide=false,
        description="Weka binary classification plugin"
)
public class WekaBinaryClassifierPlugin implements MXPlugin {

    public static final String TAG = WekaBinaryClassifierPlugin.class.getSimpleName();

    private MXPluginDescription desc;

    private File model;
    private HashMap<String, Serializable> params;

    public WekaBinaryClassifierPlugin() {
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
        this.model = new File(modelDirectory, modelFilename);
        if (!this.model.exists()) {
            Log.e(TAG, "Model at " + model + " does not exist");
            return false;
        }

        this.params = params;
        return true;
    }

    @Override
    public byte[] execute(byte[] inputData) {
        System.err.println("execute() in WekaBinaryClassifierPlugin");
        Random rand = new Random(inputData.length + this.model.length());
        String result = "1," + (.5 + rand.nextDouble() % 0.5);
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) { }
        return result.getBytes();
    }

    @Override
    public boolean destroy() {
        Log.i(TAG, "destroy() in WekaBinaryClassifierPlugin");
        return true;
    }


}
