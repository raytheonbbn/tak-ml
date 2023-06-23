package com.bbn.takml.mxf;

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

import java.io.*;
import java.nio.file.Files;
import java.util.Map;
import java.util.HashMap;
import java.util.Base64;

import com.bbn.tak.ml.mx_framework.MXFAck;
import com.bbn.tak.ml.mx_framework.MXFInstantiateParams;
import com.bbn.tak.ml.mx_framework.MXFDestroyParams;
import com.bbn.tak.ml.mx_framework.MXFExecuteParams;
import com.bbn.tak.ml.mx_framework.MXFPrediction;
import com.bbn.tak.ml.mx_framework.MXFrameworkGrpc;

import com.bbn.takml.mxf.client.api.ModelInstanceEndpointApi;
import com.bbn.takml.mxf.client.ApiException;
import com.bbn.takml.mxf.client.ApiClient;

@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
public class MISTKLogisticRegressionClient {
    private static final Logger logger =
        LoggerFactory.getLogger(MISTKLogisticRegressionClient.class);

    private String host;
    private int port;

    private MXFrameworkGrpc.MXFrameworkBlockingStub blockingStub;
    private MXFrameworkGrpc.MXFrameworkStub asyncStub;

    public MISTKLogisticRegressionClient() {
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

    private MXFAck instantiate(String pluginID, String modelFilename) {
        MXFInstantiateParams params = MXFInstantiateParams.newBuilder()
            .setPluginID(pluginID)
            .setModelFilename(modelFilename)
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
        Object[] data = {1, 1};

        byte[] serializedData;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(data);
            serializedData = bos.toByteArray();
            out.close();
            bos.close();
        } catch (IOException e) {
            logger.error("Exception when constructing input data for MISTK client: " + e);
            e.printStackTrace();
            Assert.fail();
            return;
        }

        createStubs(host, port);

        MXFAck ack = instantiate("234092384", "scikit-logistic-regression-model.bin");
        Assert.assertNotNull("Instantiate plugin with model", ack);
        if (!ack.getSuccess()) {
            logger.error(ack.getMsg());
            System.out.println("************** " + ack.getMsg());
        }
        Assert.assertTrue("Instantiate plugin with model", ack.getSuccess());

        MXFPrediction predict = execute(ack.getInstanceID(),
            ByteString.copyFrom(serializedData));
        Assert.assertNotNull("Execute model over data", predict);
        Assert.assertTrue("Execute model over data", predict.getSuccess());

        byte[] predictionBytes = predict.getData().toByteArray();
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(predictionBytes);
            ObjectInput oi = new ObjectInputStream(bis);
            Object prediction = (Object)oi.readObject();
            oi.close();
            bis.close();
            logger.info("Reply from server: " + prediction);
        } catch (IOException e) { 
            logger.error("Exception when constructing input data for MISTK client: " + e);
            e.printStackTrace();
            Assert.fail();
        } catch (ClassNotFoundException e) {
            logger.error("Exception when constructing input data for MISTK client: " + e);
            e.printStackTrace();
            Assert.fail();
        }

        ack = destroy(ack.getInstanceID());
        Assert.assertNotNull("Destroy plugin with model", ack);
        Assert.assertTrue("Destroy plugin with model", ack.getSuccess());
    }

    public static void main(String[] args) {

    }
}
