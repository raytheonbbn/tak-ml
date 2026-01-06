package com.atakmap.android.takml.mx_framework.onnx_plugin;

import com.atakmap.android.takml_android.MXPlugin;
import com.atakmap.android.takml_android.service.MxPluginService;

public class OnnxPluginService extends MxPluginService {
    @Override
    public Class<? extends MXPlugin> getMxPluginClass() {
        return OnnxPlugin.class;
    }
}
