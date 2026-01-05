package com.atakmap.android.takml_android;

import com.atakmap.android.takml_android.MXPlugin;
import com.atakmap.android.takml_android.service.MxPluginService;

public class ExampleMxPluginService2 extends MxPluginService {
    @Override
    public Class<? extends MXPlugin> getMxPluginClass() {
        return ExampleMXPlugin2.class;
    }
}
