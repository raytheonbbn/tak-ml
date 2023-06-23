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
import java.nio.file.Files;
import java.util.*;

@MXPluginDescription(
    id="1111111111",
    name="TensorFlowVGG16Plugin",
    author="Cody Doucette",
    library="TensorFlow",
    algorithm="VGG16 CNN",
    version="2.1.0",
    clientSide=false,
    serverSide=true,
    description="Plugin for executing classification requests using a VGG16 model in TensorFlow"
)
public class TensorFlowVGG16Plugin implements MXPlugin {

    private static final Logger logger =
        LoggerFactory.getLogger(TensorFlowVGG16Plugin.class);

    private String pythonPath;
    private String scriptDirectory;
    private String taxonFile;
    private String taxonLookupFile;

    private MXPluginDescription desc;
    private File model;
    private HashMap<String, Serializable> params;
 
    public TensorFlowVGG16Plugin() {
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

    private Serializable getRequiredParam(String key) throws IllegalArgumentException {
        Serializable value = this.params.get(key);
        if (value == null)
            logger.error("Client must specify parameter " + key);
        return value;
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

        this.pythonPath = (String)getRequiredParam("python_path");
        this.scriptDirectory = (String)getRequiredParam("script_directory");
        this.taxonFile = (String)getRequiredParam("taxon_file");
        this.taxonLookupFile = (String)getRequiredParam("taxon_lookup_file");

        if (this.pythonPath == null || this.scriptDirectory == null ||
                this.taxonFile == null || this.taxonLookupFile == null)
            return false;

        return true;
    }

    @Override
    public byte[] execute(byte[] inputData) {
        File inputFile;
        File outputFile;

        try {
            inputFile = File.createTempFile(this.getClass().getName(), null);
            logger.info("Input file name: " + inputFile.getName());
        } catch (IOException e) {
            logger.error("Could not construct input file: " + e);
            return null;
        }

        try {
            outputFile = File.createTempFile(this.getClass().getName(), null);
            logger.info("Output file name: " + outputFile.getName());
        } catch (IOException e) {
            logger.error("Could not construct output file: " + e);
            return null;
        }

        logger.info("taxon file: " + taxonFile);
        logger.info("taxon lookup file: " + taxonLookupFile);
        String[] commands = {
            "python3",
            "model4_execute.py",
            this.model.getAbsolutePath(),
            inputFile.getAbsolutePath(),
            outputFile.getAbsolutePath(),
            this.taxonFile,
            this.taxonLookupFile
        };
        String[] envp = { "PYTHONPATH=" + this.pythonPath };
        File dir = new File(this.scriptDirectory);

        /* Write data to temporary input file. */
        OutputStream stream;
        try {
            stream = new FileOutputStream(inputFile);
            stream.write(inputData);
            stream.close();
        } catch (FileNotFoundException e) {
            logger.error("Could not find input file: " + e);
            return null;
        } catch (IOException e) {
            logger.error("Could not write data to input file: " + e);
            return null;
        }

        try {
            Process p = Runtime.getRuntime().exec(commands, envp, dir);
            p.waitFor();
        } catch (IOException e) {
            logger.error("Could not run Python script: " + e);
            return null;
        } catch (InterruptedException e) {
            logger.error("Could not wait for Python script to finish: " + e);
            return null;
        }

        /* Get data from temporary output file. */
        byte[] fileContent;
        try {
            fileContent = Files.readAllBytes(outputFile.toPath());
        } catch (IOException e) {
            logger.error("Could not read data from output file: {0}", e);
            return null;
        }

        inputFile.delete();
        outputFile.delete();

        return fileContent;
    }

    // TODO: keep track of running Python processes and kill them here.
    @Override
    public boolean destroy() {
        return true;
    }
}
