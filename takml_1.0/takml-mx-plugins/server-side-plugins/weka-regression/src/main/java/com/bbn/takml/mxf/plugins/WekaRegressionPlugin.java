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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

import weka.classifiers.functions.LinearRegression;
import weka.core.Instances;
import weka.core.Instance;
import weka.core.DenseInstance;
import weka.core.Attribute;
import weka.core.SerializationHelper;

@MXPluginDescription(
    id="4239482340",
    name="WekaRegressionPlugin",
    author="Cody Doucette",
    library="Weka",
    algorithm="Linear Regression",
    version="3.8.4",
    clientSide=false,
    serverSide=true,
    description="TAK-ML Weka linear regression plugin"
)
public class WekaRegressionPlugin implements MXPlugin {

    private static final Logger logger =
        LoggerFactory.getLogger(WekaRegressionPlugin.class);

    private MXPluginDescription desc;
    private File model;
    private HashMap<String, Serializable> params;

    private String[] attrNames;
    private Integer classIndex;
    private Instances inst;
    private LinearRegression cls;

    public WekaRegressionPlugin() {
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

        this.attrNames = (String[])params.get("attrNames");
        if (this.attrNames == null) {
            logger.error("WekaRegression plugin requires parameter attrNames");
            return false;
        }

        ArrayList<Attribute> attributeList = new ArrayList<Attribute>();
        for (int i = 0; i < attrNames.length; i++) {
            Attribute a = new Attribute(attrNames[i]);
            attributeList.add(a);
        }

        this.inst = new Instances("data", attributeList, 1);
        this.classIndex = (Integer)params.get("classIndex");
        if (this.classIndex == null) {
            logger.error("WekaRegression plugin requires parameter classIndex");
            return false;
        }
        this.inst.setClassIndex(this.classIndex);

        try {
            this.cls = (LinearRegression)SerializationHelper.read(this.model.getPath());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("WekaRegression plugin error loading model");
            return false;
        }

        return true;
    }

    @Override
    public byte[] execute(byte[] inputData) {
        // Expecting a CSV string of values.
        String values = new String(inputData);
        String[] valueList = values.split(",");
        Double[] valueListInt = new Double[valueList.length];
        Instance row = new DenseInstance(valueListInt.length);
        for (int i = 0; i < valueList.length; i++) {
            valueListInt[i] = Double.valueOf(valueList[i]);
            row.setValue(i, valueListInt[i]);
        }
        row.setDataset(this.inst);

        double c_class;
        try {
            c_class = this.cls.classifyInstance(row);
            byte[] bytes = new byte[8];
            ByteBuffer.wrap(bytes).putDouble(c_class);
            return bytes;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Could not make classify instance");
            return null;
        }
    }

    @Override
    public boolean destroy() {
        return true;
    }
}
