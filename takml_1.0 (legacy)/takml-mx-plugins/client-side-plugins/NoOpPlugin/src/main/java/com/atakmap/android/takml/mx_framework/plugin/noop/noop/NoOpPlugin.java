package com.atakmap.android.takml.mx_framework.plugin.noop.noop;

import com.atakmap.coremap.log.Log;
import com.bbn.tak.ml.mx_framework.MXPlugin;
import com.bbn.tak.ml.mx_framework.MXPluginDescription;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;

@MXPluginDescription(
        id="1234567890",
        name="NoOpPlugin",
        author="Cody Doucette",
        library="None",
        algorithm="None",
        version="None",
        clientSide=true,
        serverSide=false,
        description="No-operation plugin to be used as a template for client-side MX plugins"
)
public class NoOpPlugin implements MXPlugin {

    public static final String TAG = NoOpPlugin.class.getSimpleName();

    private MXPluginDescription desc;

    private File model;
    private HashMap<String, Serializable> params;

    public NoOpPlugin() {
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
        Log.i(TAG, "execute() in NoOpPlugin");
        return new byte[0];
    }

    @Override
    public boolean destroy() {
        Log.i(TAG, "destroy() in NoOpPlugin");
        return true;
    }

}
