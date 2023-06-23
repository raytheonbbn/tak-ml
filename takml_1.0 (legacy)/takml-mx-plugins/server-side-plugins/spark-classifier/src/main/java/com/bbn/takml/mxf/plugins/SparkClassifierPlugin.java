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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import org.apache.spark.ml.classification.LogisticRegressionModel;
import org.apache.spark.ml.feature.IndexToString;
import org.apache.spark.ml.feature.StringIndexer;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

@MXPluginDescription(
    id="9872343212",
    name="SparkClassifierPlugin",
    author="Devon Minor",
    library="Apache Spark",
    algorithm="Classification",
    version="2.4.6",
    clientSide=false,
    serverSide=true,
    description="TAK-ML Apache Spark classification plugin"
)
public class SparkClassifierPlugin implements MXPlugin {

    private static final Logger logger =
        LoggerFactory.getLogger(SparkClassifierPlugin.class);

    private SparkSession spark;
    private LogisticRegressionModel lrModel;
    private MXPluginDescription desc;

    public SparkClassifierPlugin() {
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
        File model = new File(modelDirectory, modelFilename);
        if (!model.exists()) {
            logger.error("Model at " + model + " does not exist");
            return false;
        }

        this.spark = SparkSession
            .builder()
            .appName("SparkClassifierPlugin")
            .master("local[1]")
            .config("spark.some.config.option", "2.4.6")
            .config("system.driver.memory", 471859200)
            .config("system.testing.memory", 471859200)
            .getOrCreate();
        this.spark.sparkContext().setLogLevel("WARN");

        this.lrModel = LogisticRegressionModel.load(model.getPath());
        if (this.lrModel == null)
            return false;

        return true;
    }

    @Override
    public byte[] execute(byte[] inputData) {
        // Store input data in temporary file.
        Path tempFile;
        try {
            tempFile = Files.createTempFile("tmp-spark-logistic-regression", ".csv");
        } catch (IOException e) {
            logger.error("Error generating temp file: " + e);
            return null;
        }
        
        try {
            Files.write(tempFile, inputData);
         } catch(IOException e) {
             logger.error("Error writing input data to temp file: " + e);
             return null;
        }

        // Read in the data from temporary file.
        Dataset<Row> testData = this.spark.read()
            .format("csv")
            .option("header", true)
            .option("inferSchema", true)
            .option("sep", ",")
            .load(tempFile.toAbsolutePath().toString());

        // Open test data.
        String[] cols = Arrays.copyOfRange(testData.columns(),
            1, testData.columns().length);
        String[] newCols = new String[cols.length - 1];
        int i = 0;
        for (String s : cols) {
            testData = new StringIndexer()
                .setInputCol(s)
                .setOutputCol(String.format("%sIndex", s))
                .fit(testData)
                .transform(testData);

            if (!s.equals("Class")) {
                newCols[i] = String.format("%sIndex", s);
                i++;
            }
        }

        VectorAssembler assembler = new VectorAssembler()
                .setInputCols(newCols)
                .setOutputCol("features");
        Dataset<Row> finalTD = assembler.transform(testData);

        // Predict class for test data.
        Dataset<Row> output = this.lrModel.transform(finalTD);

        // Transform numerical value to string.
        IndexToString converter = new IndexToString()
            .setInputCol("ClassIndex")
            .setOutputCol("origClass");
        Dataset<Row> converted = converter.transform(output);

        String predictedValue = converted.select("origClass").collectAsList().get(0).get(0).toString();
        return predictedValue.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean destroy() {
        this.spark.close();
        return true;
    }
}
