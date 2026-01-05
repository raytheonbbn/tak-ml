package com.atakmap.android.takml.mx_framework.weka_plugin;

import com.atakmap.android.takml_android.MXPlugin;
import com.atakmap.android.takml_android.service.MxPluginService;

public class WekaPluginService extends MxPluginService {
    @Override
    public Class<? extends MXPlugin> getMxPluginClass() {
        return WekaPlugin.class;
    }
}
