package com.atakmap.android.takml_android;

import static org.mockito.Mockito.mock;

import android.util.Log;

import com.atakmap.android.takml_android.lib.TakmlInitializationException;

import java.util.Collections;
import java.util.Set;

public class ExampleMXPlugin implements MXPlugin {
    public static ExampleMXPlugin FAKE_EXAMPLE_MX_PLUGIN = mock(ExampleMXPlugin.class);

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
        Log.d("TAG", "called");
        FAKE_EXAMPLE_MX_PLUGIN.instantiate(takmlModel);
    }

    @Override
    public void execute(byte[] inputDataBitmap, MXExecuteModelCallback callback) {
        Log.d("TAG", "executed");
        FAKE_EXAMPLE_MX_PLUGIN.execute(inputDataBitmap, callback);
    }

    @Override
    public String[] getApplicableModelExtensions() {
        return new String[]{".test"};
    }

    @Override
    public String[] getSupportedModelTypes() {
        return new String[]{ModelTypeConstants.GENERIC_RECOGNITION};
    }

    @Override
    public void shutdown() {
        FAKE_EXAMPLE_MX_PLUGIN.shutdown();
    }
}
