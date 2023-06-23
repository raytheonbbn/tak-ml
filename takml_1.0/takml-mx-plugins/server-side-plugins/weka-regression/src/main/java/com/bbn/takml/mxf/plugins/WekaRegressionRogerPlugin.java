package com.bbn.takml.mxf.plugins;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.roger.config.AttributeDescription;
import com.bbn.roger.plugin.Plugin;
import com.bbn.roger.plugin.PluginContext;
import com.bbn.roger.plugin.StartablePlugin;
import com.bbn.roger.plugin.UtilityPluginManager;
import com.bbn.roger.plugin.exception.InsufficientConfigurationException;
import com.bbn.roger.plugin.exception.RogerInstantiationException;

import com.bbn.tak.ml.mx_framework.MXFrameworkRegistrar;
import com.bbn.takml.mxf.plugins.WekaRegressionPlugin;
import com.bbn.takml.mxf.MXFrameworkPlugin;
import com.bbn.takml.mxf.MXFrameworkRogerRegistrar;

@com.bbn.roger.annotation.Plugin (
    author = "Cody Doucette"
)

public class WekaRegressionRogerPlugin implements StartablePlugin {

    private static final Logger logger = LoggerFactory.getLogger(WekaRegressionRogerPlugin.class);
    static final Set<AttributeDescription> CONFIGURATION_ATTRIBUTES = new HashSet<>();

    private PluginContext pluginContext;
    private MXFrameworkRogerRegistrar mxf;
    private boolean started;

    @Override
    public void start() {
        UtilityPluginManager utilityPluginManager = this.pluginContext.getUtilityPluginManager();
        String pluginName = "MXFrameworkPlugin";
        Plugin plugin = utilityPluginManager.getPluginByName(pluginName);
        if (plugin == null) {
            throw new RuntimeException("Could not find the MXFramework plugin");
        }

        if (MXFrameworkPlugin.class.isAssignableFrom(plugin.getClass())) {
            this.mxf = new MXFrameworkRogerRegistrar((MXFrameworkPlugin)plugin);
        } else {
            throw new RuntimeException("Could not get class of the MXFramework plugin; try configuring with \"pluginClassLoading\" set to ONE_CLASS_LOADER_PER_PLUGIN_DIRECTORY");
        }

        if (!this.mxf.register(WekaRegressionPlugin.class))
            logger.error("Could not register plugin with MX framework");

        this.started = true;
    }

    @Override
    public void stop() {
        if (!this.mxf.deregister(WekaRegressionPlugin.class))
            logger.error("Could not deregister plugin with MX framework");
        this.mxf.stop();
    }

    @Override
    public boolean isStarted() {
        return this.started;
    }

    @Override
    public void configure(Map<String, Object> map, PluginContext pluginContext)
            throws InsufficientConfigurationException {
        this.pluginContext = pluginContext;
    }

    @Override
    public void initialize() throws RogerInstantiationException {
        return;
    }

    @Override
    public Set<AttributeDescription> getConfigurationAttributes() {
        return CONFIGURATION_ATTRIBUTES;
    }
}
