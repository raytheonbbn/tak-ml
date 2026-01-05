package com.atakmap.android.takml_android.executorch_mx_plugin;

import com.atakmap.android.takml_android.MXPlugin;
import com.atakmap.android.takml_android.service.MxPluginService;

public class ExecutorchPluginService extends MxPluginService {
    private static final String TAG = ExecutorchPluginService.class.getName();

    @Override
    public Class<? extends MXPlugin> getMxPluginClass() {
        return ExecutorchPlugin.class;
    }
}
