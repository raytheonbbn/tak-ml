/*******************************************************************************
 * DISTRIBUTION C. Distribution authorized to U.S. Government agencies and their contractors. Other requests for this document shall be referred to the United States Air Force Research Laboratory.
 *
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
package com.bbn.takml.mxf.plugins;

import com.bbn.tak.ml.mx_framework.MXPluginDescription;
import com.bbn.tak.ml.mx_framework.MXPlugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

@MXPluginDescription(
    id="0000000000",
    name="NoOpPlugin",
    author="Cody Doucette",
    library="None",
    algorithm="None",
    version="1.0",
    clientSide=false,
    serverSide=true,
    description="No-operation plugin as an example ROGER plugin for TAK-ML"
)
public class NoOpPlugin implements MXPlugin {

    private static final Logger logger = LoggerFactory.getLogger(NoOpPlugin.class);

    private MXPluginDescription desc;

    private File model;
    private HashMap<String, Serializable> params;

    public NoOpPlugin() {
        this.desc = getClass().getAnnotation(MXPluginDescription.class);
        if (this.desc == null)
            throw new RuntimeException("Must complete MXPluginDescription annotation");
    }

    @Override
    public String getPluginID() {
        return this.desc.id();
    }

    @Override
    public MXPluginDescription getPluginDescription() {
        return this.desc;
    }

    @Override
    public boolean instantiate(String modelDirectory, String modelFilename,
                               HashMap<String, Serializable> params) {
        logger.info("Called instantiate() in NoOpPlugin");
        this.model = new File(modelDirectory, modelFilename);
        if (!this.model.exists()) {
            logger.error("Model at " + model + " does not exist");
            return false;
        }
        this.params = params;

        return true;
    }

    @Override
    public byte[] execute(byte[] inputData) {
        logger.info("Called execute() in NoOpPlugin");
        return new String("").getBytes();
    }

    @Override
    public boolean destroy() {
        logger.info("Called stop() in NoOpPlugin");
        return true;
    }
}
