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
import java.nio.charset.StandardCharsets;
import java.util.*;

import weka.classifiers.Classifier;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.JSONLoader;

@MXPluginDescription(
    id="2348892383",
    name="WekaClassifierPlugin",
    author="Devon Minor",
    library="Weka",
    algorithm="Classification",
    version="3.8.4",
    clientSide=false,
    serverSide=true,
    description="TAK-ML Weka classification plugin"
)
public class WekaClassifierPlugin implements MXPlugin {

    private static final Logger logger =
        LoggerFactory.getLogger(WekaClassifierPlugin.class);

    private MXPluginDescription desc;
    private File model;
    private Classifier cls;
    private HashMap<String, Serializable> params;

    public WekaClassifierPlugin() {
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
        this.model = new File(modelDirectory, modelFilename);
        if (!this.model.exists()) {
            logger.error("Model at " + model + " does not exist");
            return false;
        }
        this.params = params;

        try {
            this.cls = (Classifier)SerializationHelper.read(this.model.getPath());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public byte[] execute(byte[] inputData) {
        Instances testData;
        try {
            InputStream is = new ByteArrayInputStream(inputData);
            JSONLoader jsonLoader = new JSONLoader();
            jsonLoader.setSource(is);
            testData = jsonLoader.getDataSet();
            testData.setClassIndex(testData.numAttributes() - 1);
        } catch (Exception e) {
            logger.error("Exception when reading test data: " + e);
            e.printStackTrace();
            return null;
        }

        String classification;
        try {
            double val = cls.classifyInstance(testData.firstInstance());
            classification = testData.firstInstance().classAttribute().value((int) val);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return classification.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean destroy() {
        return true;
    }
}
