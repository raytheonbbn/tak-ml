/*******************************************************************************
 * DISTRIBUTION C. Distribution authorized to U.S. Government agencies and their contractors. Other requests for this document shall be referred to the United States Air Force Research Laboratory.
 *
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
package com.bbn.takml.mxf.plugins;

import com.bbn.tak.ml.mx_framework.MXPluginDescription;
import com.bbn.tak.ml.mx_framework.MXPlugin;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession.Result;
import ai.onnxruntime.OrtSession.SessionOptions;
import ai.onnxruntime.OrtSession.SessionOptions.OptLevel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

@MXPluginDescription(
    id="8098374298",
    name="OnnxClassifierPlugin",
    author="Cody Doucette",
    library="ONNX",
    algorithm="Binary classification",
    version="1.4.0",
    clientSide=false,
    serverSide=true,
    description="ONNX plugin for TAK-ML"
)
public class OnnxClassifierPlugin implements MXPlugin {

    private static final Logger logger = LoggerFactory.getLogger(OnnxClassifierPlugin.class);

    /* A named tuple for sparse classification data. */
    public static class SparseData implements Serializable {
        public final int label;
        public final int[] indices;
        public final float[] values;
    
        public SparseData(int label, int[] indices, float[] values) {
            this.label = label;
            this.indices = indices;
            this.values = values;
        }
    }

    private MXPluginDescription desc;

    private File model;
    private HashMap<String, Serializable> params;

    private OrtEnvironment env;
    private OrtSession.SessionOptions opts;
    private OrtSession session;

    public OnnxClassifierPlugin() {
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

        this.env = OrtEnvironment.getEnvironment();
        this.opts = new SessionOptions();
        try {
            this.opts.setOptimizationLevel(OptLevel.BASIC_OPT);
            logger.info("Loading model from " + this.model);
            this.session = this.env.createSession(this.model.getPath(), opts);
        } catch (OrtException e) {
            e.printStackTrace();
            logger.error("Could not create session with model " + this.model + ": " + e);
            this.opts.close();
            try {
                this.env.close();
            } catch (OrtException e2) {
                e2.printStackTrace();
                logger.error("Could not close environment: " + e2);
            }
            return false;
        }

        return true;
    }
  
    /**
     * Zeros the array used by the scikit-learn model.
     *
     * @param data The array to zero.
     */
    public static void zeroData(float[][] data) {
        // Zero the array
        for (int i = 0; i < data.length; i++) {
            Arrays.fill(data[i], 0.0f);
        }
    }
  
    /**
     * Writes out sparse data to the last dimension of the supplied 2d array.
     *
     * @param data The 2d array to write to.
     * @param indices The indices of the sparse data.
     * @param values THe values of the sparse data.
     */
    public static void writeData(float[][] data, int[] indices, float[] values) {
        zeroData(data);
  
        for (int m = 0; m < indices.length; m++) {
            data[0][indices[m]] = values[m];
        }
    }
  
    /**
     * Find the maximum probability and return it's index.
     *
     * @param probabilities The probabilites.
     * @return The index of the max.
     */
    public static int pred(float[] probabilities) {
        float maxVal = Float.NEGATIVE_INFINITY;
        int idx = 0;
        for (int i = 0; i < probabilities.length; i++) {
            if (probabilities[i] > maxVal) {
                maxVal = probabilities[i];
               idx = i;
            }
        }
        return idx;
    }

    private Object deserialize(byte[] bytes) {
        Object o;
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInput oi = new ObjectInputStream(bis);
            o = (Object)oi.readObject();
            oi.close();
            bis.close();
        } catch (IOException e) {
            logger.error("Exception when constructing input data: " + e);
            e.printStackTrace();
            return null;
        } catch (ClassNotFoundException e) {
            logger.error("Exception when constructing input data: " + e);
            e.printStackTrace();
            return null;
        }
        return o;
    }

    @Override
    public byte[] execute(byte[] inputData) {
        SparseData data = (SparseData)deserialize(inputData);
        String inputName = this.session.getInputNames().iterator().next();
        float[][] testData = new float[1][780];
        writeData(testData, data.indices, data.values);

        OnnxTensor test;
        try {
            test = OnnxTensor.createTensor(env, testData);
        } catch (OrtException e) {
            e.printStackTrace();
            logger.error("Could not create tensor: " + e);
            return null;
        }

        Result output;
        try {
            output = session.run(Collections.singletonMap(inputName, test));
        } catch (OrtException e) {
            e.printStackTrace();
            logger.error("Could not run session: " + e);
            test.close();
            return null;
        }

        int predLabel;
        try {
            long[] labels = (long[])output.get(0).getValue();
            predLabel = (int)labels[0];
        } catch (OrtException e) {
            e.printStackTrace();
            logger.error("Could not get value of prediction");
            output.close();
            test.close();
            return null;
        }

        output.close();
        test.close();
        return ("" + predLabel).getBytes();
    }

    @Override
    public boolean destroy() {
        try {
            this.session.close();
        } catch (OrtException e) {
            e.printStackTrace();
            logger.warn("Could not close session: " + e);
        }
        this.opts.close();
        try {
            this.env.close();
        } catch (OrtException e) {
            e.printStackTrace();
            logger.warn("Could not close environment: " + e);
        }
        return true;
    }
}
