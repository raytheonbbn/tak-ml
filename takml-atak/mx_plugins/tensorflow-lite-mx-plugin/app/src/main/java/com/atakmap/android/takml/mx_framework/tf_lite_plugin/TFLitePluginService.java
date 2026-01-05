package com.atakmap.android.takml.mx_framework.tf_lite_plugin;

import com.atakmap.android.takml_android.MXPlugin;
import com.atakmap.android.takml_android.service.MxPluginService;

public class TFLitePluginService extends MxPluginService {
    private static final String TAG = TFLitePluginService.class.getName();

    @Override
    public Class<? extends MXPlugin> getMxPluginClass() {
        return TFLitePlugin.class;
    }
}
