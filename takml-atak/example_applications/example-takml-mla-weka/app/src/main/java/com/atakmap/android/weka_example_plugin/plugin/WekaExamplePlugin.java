
package com.atakmap.android.weka_example_plugin.plugin;

import android.content.Context;

import com.atak.plugins.impl.PluginContextProvider;
import com.atakmap.android.maps.MapComponent;
import com.atakmap.android.weka_example_plugin.WekaExamplePluginMapComponent;

import gov.tak.api.plugin.IPlugin;
import gov.tak.api.plugin.IServiceController;
import transapps.maps.plugin.tool.ToolDescriptor;

public class WekaExamplePlugin implements IPlugin {

    IServiceController serviceController;
    WekaExamplePluginTool tool;
    WekaExamplePluginMapComponent pluginMapComponent;
    Context pluginContext;

    public WekaExamplePlugin(IServiceController serviceController) {
        this.serviceController = serviceController;
        final PluginContextProvider ctxProvider = serviceController
                .getService(PluginContextProvider.class);
        if (ctxProvider != null)
            pluginContext = ctxProvider.getPluginContext();
    }

    @Override
    public void onStart() {

        pluginMapComponent = new WekaExamplePluginMapComponent();
        tool = new WekaExamplePluginTool(pluginContext);

        serviceController.registerComponent(ToolDescriptor.class, tool);
        serviceController.registerComponent(MapComponent.class,
                pluginMapComponent);
    }

    @Override
    public void onStop() {
        serviceController.unregisterComponent(ToolDescriptor.class, tool);
        serviceController.unregisterComponent(MapComponent.class,
                pluginMapComponent);

    }
}
