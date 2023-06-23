package com.bbn.tak.ml.mx_framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for MX plugins.
 * <p>
 * Each {@link MXPlugin} must include this annotation.
 * This annotation defines the plugin for use in the MX framework.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MXPluginDescription {

    /**
     * The unique ID for this plugin, often a 10-digit String.
     * @return a String ID.
     */
    String id();

    /**
     * The name of the plugin. Not necessarily unique.
     * @return a String name.
     */
    String name();

    /**
     * The author of the plugin.
     * @return a String author.
     */
    String author();

    /**
     * The main machine learning library (or libraries) that the plugin uses.
     * @return a String library.
     */
    String library();

    /**
     * The type of machine learning algorithm that the plugin implements.
     * @return a String algorithm.
     */
    String algorithm();

    /**
     * The version of the machine learning {@link library()} that the plugin uses.
     * @return a String version.
     */
    String version();

    /**
     * Whether the plugin is a client-side (mobile) plugin.
     * @return a boolean.
     */
    boolean clientSide();

    /**
     * Whether the plugin is a server-side plugin.
     * @return a boolean.
     */
    boolean serverSide();

    /**
     * A free-form description of the plugin.
     * @return a String description.
     */
    String description();
}
