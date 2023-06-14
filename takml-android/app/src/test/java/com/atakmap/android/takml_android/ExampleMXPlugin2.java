package com.atakmap.android.takml_android;

import static org.mockito.Mockito.mock;

import android.util.Log;

import com.atakmap.android.takml_android.lib.TakmlInitializationException;

public class ExampleMXPlugin2 implements MXPlugin {
    public static ExampleMXPlugin2 FAKE_EXAMPLE_MX_PLUGIN_2 = mock(ExampleMXPlugin2.class);

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public boolean isServerSide() {
        return false;
    }

    @Override
    public void instantiate(TakmlModel takmlModel)
            throws TakmlInitializationException {
        FAKE_EXAMPLE_MX_PLUGIN_2.instantiate(takmlModel);
    }

    @Override
    public void execute(byte[] inputDataBitmap, MXExecuteModelCallback callback) {
        FAKE_EXAMPLE_MX_PLUGIN_2.execute(inputDataBitmap, callback);
    }

    @Override
    public String[] getApplicableModelExtensions() {
        return new String[]{".test2"};
    }

    @Override
    public String[] getSupportedModelTypes() {
        return new String[]{ModelTypeConstants.GENERIC_RECOGNITION};
    }

    @Override
    public void shutdown() {
        FAKE_EXAMPLE_MX_PLUGIN_2.shutdown();
    }
}
