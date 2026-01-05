package com.atakmap.android.takml_android.pytorch_mx_plugin;

import com.atakmap.android.takml_android.MXPlugin;
import com.atakmap.android.takml_android.service.MxPluginService;

public class PytorchPluginService extends MxPluginService {
    private static final String TAG = PytorchPluginService.class.getName();

    @Override
    public Class<? extends MXPlugin> getMxPluginClass() {
        return PtMobilePlugin.class;
    }
}
