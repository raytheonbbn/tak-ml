package com.bbn.takml.mxf;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import com.bbn.roger.plugin.exception.InsufficientConfigurationException;

import org.junit.Assert;
import org.junit.Test;

import com.bbn.takml.mxf.MXFrameworkPlugin;
import com.bbn.takml.mxf.plugins.OnnxClassifierPlugin;

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

public class OnnxClassifierTest {

    private static final Logger logger =
        LoggerFactory.getLogger(OnnxClassifierTest.class);

    /* Pattern for splitting libsvm format files. */
    private static final Pattern splitPattern = Pattern.compile("\\s+");

    /*
     * Converts a List of Integer into an int array.
     *
     * @param list The list to convert.
     * @return The int array.
     */
    private static int[] convertInts(List<Integer> list) {
        int[] output = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            output[i] = list.get(i);
        }
        return output;
    }

    /*
     * Converts a List of Float into a float array.
     *
     * @param list The list to convert.
     * @return The float array.
     */
    private static float[] convertFloats(List<Float> list) {
        float[] output = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            output[i] = list.get(i);
        }
        return output;
    }

    /*
     * Loads data from a libsvm format file.
     *
     * @param path The path to load the data from.
     * @return A named tuple containing the data.
     * @throws IOException If it failed to read the file.
     */
    private static OnnxClassifierPlugin.SparseData load(String path)
            throws IOException {
        int pos = 0;
        String line;
        int maxFeatureID = Integer.MIN_VALUE;
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            line = reader.readLine();
            if (line == null) {
                return null;
            }
            pos++;
            String[] fields = splitPattern.split(line);
            int lastID = -1;
            try {
                boolean valid = true;
                ArrayList<Integer> curIndices = new ArrayList<>();
                ArrayList<Float> curValues = new ArrayList<>();
                for (int i = 1; i < fields.length && valid; i++) {
                    int ind = fields[i].indexOf(':');
                    if (ind < 0) {
                        logger.warn(String.format("Weird line at %d", pos));
                        valid = false;
                    }
                    String ids = fields[i].substring(0, ind);
                    int id = Integer.parseInt(ids);
                    curIndices.add(id);
                    if (maxFeatureID < id) {
                        maxFeatureID = id;
                    }
                    float val = Float.parseFloat(fields[i].substring(ind + 1));
                    curValues.add(val);
                    if (id <= lastID) {
                        logger.warn(String.format("Repeated features at line %d", pos));
                        valid = false;
                    } else {
                        lastID = id;
                    }
                }
                if (valid) {
                    // Store the label
                    int label = Integer.parseInt(fields[0]);
                    // Store the features
                    int[] indices = convertInts(curIndices);
                    float[] values = convertFloats(curValues);
                    return new OnnxClassifierPlugin.SparseData(label, indices, values);
                } else {
                    throw new IOException("Invalid LibSVM format file at line " + pos);
                }
            } catch (NumberFormatException ex) {
                logger.warn(String.format("Weird line at %d", pos));
                throw new IOException("Invalid LibSVM format file", ex);
            }
        }
    }

    private byte[] serialize(Object o) {
        byte[] bytes;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(o);
            bytes = bos.toByteArray();
            out.close();
            bos.close();
        } catch (IOException e) {
            logger.error("Cannot serialize object");
            return null;
        }
        return bytes;
    }

    @Test
    public void testOnnxClassifierPlugin() {
        MXFrameworkPlugin mxf = new MXFrameworkPlugin();
        OnnxClassifierPlugin plugin = new OnnxClassifierPlugin();

        String modelDir = "src/test/resources/model";
        String modelFile = "lr_mnist_scikit.onnx";

        String inputFile = "src/test/resources/data/sample.t";
        String expectedResult = "7";

        HashMap<String, Serializable> params = new HashMap<String, Serializable>();

        // Instantiate plugin.
        boolean started = plugin.instantiate(modelDir, modelFile, params);
        Assert.assertTrue("Instantiate plugin", started);

        OnnxClassifierPlugin.SparseData data = null;
        try {
            data = load(inputFile);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail();
        }
        byte[] inputData = serialize(data);
        if (inputData == null)
            Assert.fail();

        // Make prediction.
        byte[] response = plugin.execute(inputData);
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

    public OnnxClassifierTest() {
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
        String inputFile = "src/test/resources/data/sample.t";
        OnnxClassifierPlugin.SparseData data = null;
        try {
            data = load(inputFile);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail();
        }
        byte[] inputData = serialize(data);
        if (inputData == null)
            Assert.fail();

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

        String modelDir = "/tak-ml/server-side/mx-plugins/onnx-classifier/src/test/resources/model";
        String modelFile = "lr_mnist_scikit.onnx";

        MXFAck ack = instantiate(new OnnxClassifierPlugin().getPluginID(),
                modelDir, modelFile, ByteString.copyFrom(paramsMapBytes));
        Assert.assertTrue("Instantiate plugin with model", ack.getSuccess());

        MXFPrediction predict = execute(ack.getInstanceID(),
            ByteString.copyFrom(inputData));
        Assert.assertTrue("Execute model over data", predict.getSuccess());

        try {
            String str = new String(predict.getData().toByteArray(), "UTF-8");
            logger.info("Reply from server: " + str);
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
