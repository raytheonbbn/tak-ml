package com.atakmap.android.takml_android;

import static org.mockito.Mockito.mock;

import android.content.Context;
import com.atakmap.android.takml_android.lib.TakmlInitializationException;
import com.atakmap.android.takml_android.service.MxPluginService;
import com.atakmap.android.takml_android.takml_result.Recognition;
import com.atakmap.android.takml_android.tensor_processor.InferInput;

import java.util.Collections;
import java.util.List;


public class ExampleMXPlugin2 implements MXPlugin {
    public static ExampleMXPlugin2 FAKE_EXAMPLE_MX_PLUGIN_2 = mock(ExampleMXPlugin2.class);
    public static final String APPLICABLE_EXTENSION = ".test2";
    private String modelName;

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
    public void instantiate(TakmlModel takmlModel, Context context) throws TakmlInitializationException {

    }

    @Override
    public void execute(byte[] inputData, MXExecuteModelCallback callback) {
        callback.modelResult(Collections.singletonList(new Recognition("test", 1.0f)),
                true, modelName,ModelTypeConstants.IMAGE_CLASSIFICATION);
        FAKE_EXAMPLE_MX_PLUGIN_2.execute(inputData, null);
    }

    @Override
    public void execute(List<InferInput> inputTensors, MXExecuteModelCallback callback) {

    }

    @Override
    public String[] getApplicableModelExtensions() {
        return new String[]{APPLICABLE_EXTENSION};
    }

    @Override
    public String[] getSupportedModelTypes() {
        return new String[]{ModelTypeConstants.GENERIC_RECOGNITION};
    }

    @Override
    public Class<? extends MxPluginService> getOptionalServiceClass() {
        return ExampleMxPluginService2.class;
    }

    @Override
    public void shutdown() {
        FAKE_EXAMPLE_MX_PLUGIN_2.shutdown();
    }
}
