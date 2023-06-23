package com.bbn.takml.mxf;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.*;
import java.util.*;

import com.bbn.roger.plugin.exception.InsufficientConfigurationException;

import org.junit.Assert;
import org.junit.Test;

import com.bbn.takml.mxf.MXFrameworkPlugin;
import com.bbn.takml.mxf.plugins.DL4JClassifierPlugin;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import com.google.protobuf.ByteString;
import java.io.IOException;
import org.junit.Test;
import org.junit.Assert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.tak.ml.mx_framework.MXFAck;
import com.bbn.tak.ml.mx_framework.MXFInstantiateParams;
import com.bbn.tak.ml.mx_framework.MXFDestroyParams;
import com.bbn.tak.ml.mx_framework.MXFExecuteParams;
import com.bbn.tak.ml.mx_framework.MXFPrediction;
import com.bbn.tak.ml.mx_framework.MXFrameworkGrpc;

public class DL4JClassifierTest {

    private static final Logger logger =
        LoggerFactory.getLogger(DL4JClassifierTest.class);

    @Test
    public void testDL4JClassifierPlugin() {
        String modelDir = "src/test/resources/model";
        String modelFile = "vote_dl4j.zip";
        File inputFile = new File("src/test/resources/data/vote_test.csv");
        String expectedResult = "republican";

        MXFrameworkPlugin mxf = new MXFrameworkPlugin();
        DL4JClassifierPlugin plugin = new DL4JClassifierPlugin();

        // Instantiate plugin.
        boolean started = plugin.instantiate(modelDir, modelFile, null);
        Assert.assertTrue("Instantiate plugin", started);

        byte[] fileContents = null;
        try {
        	fileContents = Files.readAllBytes(inputFile.toPath());
        } catch(IOException e) {
        	Assert.fail("Exception " + e);
        }
        
        // Make prediction.
        byte[] response = plugin.execute(fileContents);
        String str = new String(response, StandardCharsets.UTF_8);
        Assert.assertEquals("Execute prediction", expectedResult, str);

        // Destroy instance.
        boolean destroyed = plugin.destroy();
        Assert.assertTrue("Destroy plugin instance", destroyed);
    }

    private String host;
    private int port;

    private MXFrameworkGrpc.MXFrameworkBlockingStub blockingStub;
    private MXFrameworkGrpc.MXFrameworkStub asyncStub;

    public DL4JClassifierTest() {
        this.host = "127.0.0.1";
        this.port = 9020;
    }

    public void createStubs(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder
            .forAddress(host, port)
            .usePlaintext()
            .build();
        this.blockingStub = MXFrameworkGrpc.newBlockingStub(channel);
        this.asyncStub = MXFrameworkGrpc.newStub(channel);
    }

    private MXFAck instantiate(String pluginID,
            String modelDirectory, String modelFilename,
            ByteString paramsMap) {
        MXFInstantiateParams params = MXFInstantiateParams.newBuilder()
            .setPluginID(pluginID)
            .setModelDirectory(modelDirectory)
            .setModelFilename(modelFilename)
            .setParams(paramsMap)
            .build();

        MXFAck response = null;
        try {
            response = blockingStub.mxfInstantiate(params);
        } catch (StatusRuntimeException e) {
            logger.warn("RPC failed: " + e.getStatus());
        }
        return response;
    }

    private MXFPrediction execute(String mxpInstanceID, ByteString data) {
        MXFExecuteParams params = MXFExecuteParams.newBuilder()
            .setInstanceID(mxpInstanceID)
            .setData(data)
            .build();

        MXFPrediction response = null;
        try {
            response = blockingStub.mxfExecute(params);
        } catch (StatusRuntimeException e) {
            logger.warn("RPC failed: " + e.getStatus());
        }
        return response;
    }

    private MXFAck destroy(String mxpInstanceID) {
        MXFDestroyParams params = MXFDestroyParams.newBuilder()
            .setInstanceID(mxpInstanceID)
            .build();

        MXFAck response = null;
        try {
            response = blockingStub.mxfDestroy(params);
        } catch (StatusRuntimeException e) {
            logger.warn("RPC failed: " + e.getStatus());
        }
        return response;
    }

    @Test
    public void testMXFExecution() {
        String modelDir = "/tak-ml/server-side/mx-plugins/dl4j-classifier/src/test/resources/model";
        String modelFile = "vote_dl4j.zip";
        File inputFile = new File("src/test/resources/data/vote_test.csv");
        String expectedResult = "republican";

        createStubs(host, port);

        Map<String, Serializable> paramsMap = new HashMap<String, Serializable>();

        byte[] paramsMapBytes = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(paramsMap);
            paramsMapBytes = bos.toByteArray();
            out.close();
            bos.close();
        } catch (IOException e) {
            logger.error("Error serializing parameters to instantiate plugin");
            Assert.fail();
        }

        MXFAck ack = instantiate(new DL4JClassifierPlugin().getPluginID(),
                modelDir, modelFile, ByteString.copyFrom(paramsMapBytes));
        Assert.assertTrue("Instantiate plugin with model", ack.getSuccess());

        byte[] fileContent = null;
        try {
            fileContent = Files.readAllBytes(inputFile.toPath());
        } catch (IOException e) {
            logger.info("Could not read input file data: " + e);
            Assert.fail();
        }
        MXFPrediction predict = execute(ack.getInstanceID(),
            ByteString.copyFrom(fileContent));
        Assert.assertTrue("Execute model over data", predict.getSuccess());

        try {
            String str = new String(predict.getData().toByteArray(), "UTF-8");
            Assert.assertEquals("Execute prediction", expectedResult, str);
        } catch (UnsupportedEncodingException e) {
            logger.error("Could not decode response from server: " + e);
            Assert.fail();
        }

        ack = destroy(ack.getInstanceID());
        Assert.assertTrue("Destroy plugin with model", ack.getSuccess());
    }

    public static void main(String[] args) {

    }
}
