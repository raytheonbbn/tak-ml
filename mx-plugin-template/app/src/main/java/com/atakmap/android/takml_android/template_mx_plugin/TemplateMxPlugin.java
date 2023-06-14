package com.atakmap.android.takml_android.template_mx_plugin;

import com.atakmap.android.takml_android.MXExecuteModelCallback;
import com.atakmap.android.takml_android.MXPlugin;
import com.atakmap.android.takml_android.TakmlModel;
import com.atakmap.android.takml_android.lib.TakmlInitializationException;

public class TemplateMxPlugin implements MXPlugin {
    private static final String DESCRIPTION = "Example MX Plugin Template";
    private static final String VERSION = "1.0";

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public boolean isServerSide() {
        return false;
    }

    @Override
    public void instantiate(TakmlModel takmlModel) throws TakmlInitializationException {
        // TODO: instantiate Takml Model
    }

    @Override
    public void execute(byte[] inputDataBitmap, MXExecuteModelCallback callback) {
        // TODO: execute Takml Model with your mx plugin
    }

    @Override
    public void shutdown() {
        // TODO: any cleanup logic when shutting down
    }
}
