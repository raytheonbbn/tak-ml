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

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.records.writer.RecordWriter;
import org.datavec.api.records.writer.impl.csv.CSVRecordWriter;
import org.datavec.api.split.FileSplit;
import org.datavec.api.split.partition.NumberOfRecordsPartitioner;
import org.datavec.api.split.partition.Partitioner;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.TransformProcess.Builder;
import org.datavec.api.transform.condition.ConditionOp;
import org.datavec.api.transform.condition.column.CategoricalColumnCondition;
import org.datavec.api.transform.filter.ConditionFilter;
import org.datavec.api.transform.schema.InferredSchema;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.writable.Writable;
import org.datavec.local.transforms.LocalTransformExecutor;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;

@MXPluginDescription(
    id="2348999383",
    name="DL4JClassifierPlugin",
    author="Devon Minor",
    library="Deeplearning4j",
    algorithm="Classification",
    version="1.0.0-beta7",
    clientSide=false,
    serverSide=true,
    description="TAK-ML Deeplearning4j classification plugin"
)
public class DL4JClassifierPlugin implements MXPlugin {

    private static final Logger logger =
        LoggerFactory.getLogger(DL4JClassifierPlugin.class);

    private static final int numLinesToSkip = 1;
    private static final char delimiter = ',';

    private MXPluginDescription desc;
    private MultiLayerNetwork model;

    public DL4JClassifierPlugin() {
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
        File modelFile = new File(modelDirectory, modelFilename);
        if (!modelFile.exists()) {
            logger.error("Model at " + modelFile + " does not exist");
            return false;
        }

        try {
            this.model = MultiLayerNetwork.load(modelFile, false);
        } catch (IOException e) {
            logger.error("Error while trying to load model: " + e);
            return false;
        }

        return true;
    }

    @Override
    public byte[] execute(byte[] inputData) {
        // Store input data in temporary file.
        Path tempFile;
        try {
            tempFile = Files.createTempFile("tmp-dl4j-input", ".csv");
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

        // Grab the schema from the temporary file.
        Schema schema;
        try {
            schema = new InferredSchema(tempFile.toAbsolutePath().toString()).build();
        } catch (IOException e) {
            logger.error("ERROR while trying to infer schema from inputData: " + e);
            return null;
        }

        // Structure input data with the same format as the tmp file.
        Builder tp = new Builder(schema);
        List<String> categories = Arrays.asList("n", "y", "?");
        for (int i = 1; i < schema.numColumns() - 1; i++) {
            tp.filter(new ConditionFilter(
                new CategoricalColumnCondition(schema.getName(i), ConditionOp.NotInSet,
                    new HashSet<>(categories))));
            tp.stringToCategorical(schema.getName(i), categories);
            tp.categoricalToInteger(schema.getName(i));
        }
        TransformProcess finalTp = tp
                .removeColumns("id")
                .stringToCategorical(schema.getName(schema.numColumns() - 1), Arrays.asList("republican", "democrat"))
                .categoricalToInteger(schema.getName(schema.numColumns() - 1))
                .build();

        RecordReader rr = new CSVRecordReader(numLinesToSkip, delimiter);
        try {
            rr.initialize(new FileSplit(tempFile.toFile()));
        } catch (IOException | InterruptedException e) {
            logger.error("ERROR while trying to read original data from tmp file: " + e);
            try {
                rr.close();
            } catch (IOException e1) {
                logger.error("ERROR while trying to close RecordReader: " + e1);
                return null;
            }
            return null;
        }

        List<List<Writable>> originalData = new ArrayList<>();
        while(rr.hasNext()) {
            originalData.add(rr.next());
        }

        Path tmpProcess;
        try {
            tmpProcess = Files.createTempFile("tmp-dl4j-processed", ".csv");
        } catch (IOException e) {
            logger.error("Error while writing processed data to tmp file: " + e);
            try {
                rr.close();
            } catch (IOException e1) {
                logger.error("ERROR while trying to close RecordReader: " + e1);
                return null;
            }
            return null;
        }

        // Output processed data.
        RecordWriter rw = new CSVRecordWriter();
        try {
            Partitioner p = new NumberOfRecordsPartitioner();
            rw.initialize(new FileSplit(tmpProcess.toFile()), p);
        } catch (Exception e) {
            logger.error("ERROR while trying to write processed data to tmp file: " + e);
        }

        List<List<Writable>> processedData = LocalTransformExecutor.execute(originalData, finalTp);
        try {
            rw.writeBatch(processedData);
        } catch (IOException e) {
            logger.error("ERROR while trying to process data: " + e);
            return null;
        } finally {
            try {
                rr.close();
            } catch (IOException e) {
                logger.error("ERROR while trying to close RecordReader: " + e);
                return null;
            }
            rw.close();
        }

        // Grab processed data.
        RecordReader recordReader = new CSVRecordReader(0,delimiter);
        try {
            recordReader.initialize(new FileSplit(tmpProcess.toFile()));
        } catch (IOException | InterruptedException e) {
            logger.error("ERROR while trying to read processed data: " + e);
        }

        int labelIndex = finalTp.getFinalSchema().numColumns() - 1;
        int numClasses = 2;
        int batchSize = 1;

        DataSetIterator iterator = new RecordReaderDataSetIterator(recordReader,batchSize,labelIndex,numClasses);
        DataSet allData = iterator.next();

        // Normalize data and predict output.
        DataNormalization normalizer = new NormalizerStandardize();
        normalizer.fit(allData);

        allData.setLabelNames(Arrays.asList("republican", "democrat"));
        List<String> predict = model.predict(allData);

        /* Serialize prediction to send back to client. */
        String prediction = predict.get(0);
        return prediction.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean destroy() {
        this.model.close();
        return true;
    }
}
