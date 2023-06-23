/*******************************************************************************
 * DISTRIBUTION C. Distribution authorized to U.S. Government agencies and their contractors. Other requests for this document shall be referred to the United States Air Force Research Laboratory.
 *
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
package com.bbn.takml.mxf;

import com.bbn.roger.annotation.Plugin;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.junit.Test;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

import static java.util.Objects.requireNonNull;

@SuppressWarnings("PMD.CollapsibleIfStatements")
public class PluginManifestGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginManifestGenerator.class);
    private static final String PLUGIN_MANIFEST_FILENAME = "/plugin-manifest.json";
    private static final String PLUGIN_MANIFEST_FOLDER = "src/main/resources";

    @Test
    public void generateManifest() {
        printClassPath();
        printResources();
        //System.setProperty("user.dir", currentDirectory);
        LOGGER.info("User directory: " + System.getProperty("user.dir"));
        LOGGER.info("Starting Plugin Manifest Generator...");
        // ** Access the index of classes annotated with the specified annotation at runtime.
        // Uses the Class Index library.
        File manifestFolder = new File(PLUGIN_MANIFEST_FOLDER);
        if (!manifestFolder.exists()) {
            if (manifestFolder.mkdirs()) {
                LOGGER.info("Created directory structure for plugin manifest");
            }
        }
//      File manifest = new File(currentDirectory + "/" + PLUGIN_MANIFEST_FOLDER + PLUGIN_MANIFEST_FILENAME);
        File manifest = new File(PLUGIN_MANIFEST_FOLDER + PLUGIN_MANIFEST_FILENAME);
        if (!manifest.exists()) {
            LOGGER.info("Manifest does not exist, so creating a new one");
            try {
                if (!manifest.createNewFile()) {
                    LOGGER.error("Could not create manifest file at " + PLUGIN_MANIFEST_FILENAME);
                }
            } catch (IOException e) {
                LOGGER.error(e.getLocalizedMessage(), e);
            }
        } else {
            LOGGER.info("Updating manifest file...");
        }
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator generator = factory.createJsonGenerator(manifest, JsonEncoding.UTF8)) {
            generator.useDefaultPrettyPrinter(); // Enable pretty printing
            generator.writeStartObject();
            generator.writeStringField("pluginContainerName", "ROGER TAK-ML Model Execution Framework Plug-ins");
            generator.writeStringField("description", "A collection of plug-ins supporting the TAK-ML Model Execution Framework.");
            generator.writeArrayFieldStart("plugins");
            Reflections reflections = new Reflections("com.bbn.takml.mxf.plugins");
            for (Class pluginClass : reflections.getTypesAnnotatedWith(Plugin.class)) {
                LOGGER.info("Plugin class: {}", pluginClass.getName());
//            for (Class pluginClass : ClassIndex.getAnnotated(com.bbn.roger.annotation.Plugin.class, ClassLoader.getSystemClassLoader())) {
                generator.writeStartObject();
                Plugin annotation = requireNonNull(
                        (Plugin) pluginClass.getAnnotation(Plugin.class),
                        "Plugin annotation should not be null");
                generator.writeStringField("className", pluginClass.getName());
                generator.writeStringField("author", annotation.author());
                generator.writeStringField("description", annotation.description());
                generator.writeStringField("version", annotation.version());
                generator.writeArrayFieldStart("dependencies");
                for (String dependency : annotation.dependencies()) {
                    generator.writeString(dependency);
                }
                generator.writeEndArray();
                generator.writeEndObject();
            }
            generator.writeEndArray();
            generator.writeEndObject();
        } catch (IOException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
        LOGGER.info("Plugin Manifest Generator has finished.");
    }

    private void printClassPath() {
        //https://www.mkyong.com/java/how-to-print-out-the-current-project-classpath/
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        URL[] urls = ((URLClassLoader)cl).getURLs();
        for(URL url: urls){
            LOGGER.info("CLASSPATH ENTRY: {}", url.getFile());
        }
    }

    private void printResources() {
        try {
            ClassLoader cl = ClassLoader.getSystemClassLoader();
            String name = "META-INF/annotations/" + Plugin.class.getCanonicalName();
            Enumeration<URL> resources = cl.getResources(name);
            if (!resources.hasMoreElements()) {
                LOGGER.warn("Did not find resource with name {}", name);
            }
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                LOGGER.info("RESOURCE: {}", resource.getFile());
            }
        } catch (IOException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
    }
}
