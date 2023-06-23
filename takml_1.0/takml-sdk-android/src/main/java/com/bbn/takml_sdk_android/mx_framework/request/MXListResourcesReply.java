package com.bbn.takml_sdk_android.mx_framework.request;

import java.io.Serializable;
import java.util.Set;

/**
 * Reply to list the available resources in the MX framework.
 * <p>
 * If requested, the MX framework can provide asynchronous updates about
 * changes to the MX plugins and models that are made available or unavailable
 * to the application.
 * <p>
 * Note that the request to list MX framework resources does not have
 * its own class. Instead, an {@link MXRequest} message is sent with
 * its {@link MXRequest#type} set to {@link MXRequest.Type#LIST_RESOURCES}.
 */
public class MXListResourcesReply implements Serializable {
    private static final long serialVersionUID = 1L;
    private Set<String> pluginLabels;
    private Set<String> modelLabels;

    /**
     * Create a new reply to list the available resources in the MX framework.
     *
     * @param pluginLabels the list of plugins available in the MX framework, stylized as labels.
     * @param modelLabels the list of models available in the MX framework, stylized as labels.
     */
    public MXListResourcesReply(Set<String> pluginLabels, Set<String> modelLabels) {
        this.pluginLabels = pluginLabels;
        this.modelLabels = modelLabels;
    }

    /**
     * Get the list of plugins available in the MX framework, stylized as labels.
     * <p>
     * To be more informational, the list of plugins is given as a list of strings,
     * where each string is a label of the form "Plugin Name (Plugin ID)". Callers
     * can use {@link com.bbn.takml_sdk_android.mx_framework.PredictionExecutor#pluginLabelToID(String)}
     * to extract the plugin ID from the label when needed to use that ID in a
     * request to the MX framework.
     *
     * @return the list of plugins available in the MX framework, stylized as labels.
     */
    public Set<String> getPlugins() {
        return this.pluginLabels;
    }

    /**
     * Get the list of models available in the MX framework, stylized as labels.
     * <p>
     * To be more informational, the list of models is given as a list of strings,
     * where each string is a label of the form "Model Name (Location)". Callers
     * can use {@link com.bbn.takml_sdk_android.mx_framework.PredictionExecutor#modelLabelToName(String)}
     * to extract the model filename from the label when needed to use that filename
     * in a request to the MX framework.
     *
     * @return the list of models available in the MX framework, stylized as labels.
     */
    public Set<String> getModels() {
        return this.modelLabels;
    }
}
