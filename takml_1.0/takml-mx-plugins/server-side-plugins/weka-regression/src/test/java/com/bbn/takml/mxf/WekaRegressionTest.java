package com.bbn.takml.mxf;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.ByteBuffer;
import java.io.*;
import java.util.*;

import com.bbn.roger.plugin.exception.InsufficientConfigurationException;

import org.junit.Assert;
import org.junit.Test;

import com.bbn.takml.mxf.MXFrameworkPlugin;
import com.bbn.takml.mxf.plugins.WekaRegressionPlugin;

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

public class WekaRegressionTest {

    private static final Logger logger =
        LoggerFactory.getLogger(WekaRegressionTest.class);

    @Test
    public void testWekaRegressionPlugin() {
        String modelDir = "src/test/resources/model";
        String modelFile = "solar-flare.model";
        Double expectedResult = 1.10;

        MXFrameworkPlugin mxf = new MXFrameworkPlugin();
        WekaRegressionPlugin plugin = new WekaRegressionPlugin();

        String[] attrs = {
            "class_A", "class_B", "class_C", "class_D", "class_E", "class_F", "class_H",
            "spot_X", "spot_R", "spot_S", "spot_A", "spot_H", "spot_K",
            "dist_X", "dist_O", "dist_I", "dist_C",
            "activity", "evolution_decay", "evolution_no", "evolution_growth",
            "prev_1","prev_2","prev_3", "complex", "pass", "area", "largest_area",
            "c_class"
        };
        
        HashMap<String, Serializable> paramsMap = new HashMap<String, Serializable>();
        paramsMap.put("attrNames", attrs);
        paramsMap.put("classIndex", attrs.length - 1); // c_class

        // Instantiate plugin.
        boolean started = plugin.instantiate(modelDir, modelFile, paramsMap);
        Assert.assertTrue("Instantiate plugin", started);

        String values = "0,0,0,0,1,0,0,0,0,1,0,0,0,0,1,0,0,1,0,0,1,1,0,0,1,1,0,0";
        byte[] inputData = values.getBytes();

        // Make prediction.
        byte[] response = plugin.execute(inputData);
        Double val = ByteBuffer.wrap(response).getDouble();
        if (Math.abs(val - expectedResult) > 0.1)
            Assert.fail("Did not return expected result");

        // Destroy instance.
        boolean destroyed = plugin.destroy();
        Assert.assertTrue("Destroy plugin instance", destroyed);
    }

    private String host;
    private int port;

    private MXFrameworkGrpc.MXFrameworkBlockingStub blockingStub;
    private MXFrameworkGrpc.MXFrameworkStub asyncStub;

    public WekaRegressionTest() {
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
        String modelDir = "/tak-ml/server-side/mx-plugins/weka-regression/src/test/resources/model";
        String modelFile = "solar-flare.model";
        Double expectedResult = 1.10;

        createStubs(host, port);

        String[] attrs = {
            "class_A", "class_B", "class_C", "class_D", "class_E", "class_F", "class_H",
            "spot_X", "spot_R", "spot_S", "spot_A", "spot_H", "spot_K",
            "dist_X", "dist_O", "dist_I", "dist_C",
            "activity", "evolution_decay", "evolution_no", "evolution_growth",
            "prev_1","prev_2","prev_3", "complex", "pass", "area", "largest_area",
            "c_class"
        };
        
        Map<String, Serializable> paramsMap = new HashMap<String, Serializable>();
        paramsMap.put("attrNames", attrs);
        paramsMap.put("classIndex", attrs.length - 1); // c_class

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

        MXFAck ack = instantiate(new WekaRegressionPlugin().getPluginID(),
                modelDir, modelFile, ByteString.copyFrom(paramsMapBytes));
        Assert.assertTrue("Instantiate plugin with model", ack.getSuccess());

        String values = "0,0,0,0,1,0,0,0,0,1,0,0,0,0,1,0,0,1,0,0,1,1,0,0,1,1,0,0";
        byte[] inputData = values.getBytes();

        MXFPrediction predict = execute(ack.getInstanceID(),
            ByteString.copyFrom(inputData));
        Assert.assertTrue("Execute model over data", predict.getSuccess());

        Double val = ByteBuffer.wrap(predict.getData().toByteArray()).getDouble();
        if (Math.abs(val - expectedResult) > 0.1)
            Assert.fail("Did not return expected result");

        ack = destroy(ack.getInstanceID());
        Assert.assertTrue("Destroy plugin with model", ack.getSuccess());
    }

    public static void main(String[] args) {

    }
}
