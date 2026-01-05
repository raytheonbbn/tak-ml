package com.atakmap.android.weka_example_plugin.plugin;

import com.atak.plugins.impl.AbstractPlugin;
import com.atak.plugins.impl.PluginContextProvider;
import com.atakmap.android.weka_example_plugin.WekaExamplePluginMapComponent;

import gov.tak.api.plugin.IServiceController;

public class TakmlMlaLifecycle extends AbstractPlugin {

    public TakmlMlaLifecycle(IServiceController serviceController) {
        super(serviceController, new WekaExamplePluginTool(serviceController.getService
                (PluginContextProvider.class).getPluginContext()), new WekaExamplePluginMapComponent());
    }
}