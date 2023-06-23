/*******************************************************************************
 * Copyright (c) 2020 Raytheon BBN Technologies.
 *******************************************************************************/

package com.bbn.takml.mxf;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.io.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.roger.config.AttributeDescription;
import com.bbn.roger.plugin.exception.InsufficientConfigurationException;
import com.bbn.tak.ml.mx_framework.MXFAck;
import com.bbn.tak.ml.mx_framework.MXFPrediction;
import com.bbn.tak.ml.mx_framework.MXPluginDescription;
import com.bbn.tak.ml.mx_framework.MXFPluginList;
import com.bbn.tak.ml.mx_framework.MXFPluginDescription;

import java.lang.reflect.*;
import javax.json.*;
import io.leangen.geantyref.TypeFactory;
import io.leangen.geantyref.AnnotationFormatException;
import com.google.protobuf.ByteString;

import com.bbn.takml.mxf.client.api.ModelInstanceEndpointApi;
import com.bbn.takml.mxf.client.ApiException;
import com.bbn.takml.mxf.client.ApiClient;
import com.bbn.takml.mxf.client.model.MistkDataset;
import com.bbn.takml.mxf.client.model.MistkDataset.ModalityEnum;
import com.bbn.takml.mxf.client.model.ObjectInfo;
import com.bbn.takml.mxf.client.model.ModelInstanceInitParams;
import com.bbn.takml.mxf.client.model.ModelInstanceStatus;
import com.bbn.takml.mxf.client.model.ModelInstanceStatus.StateEnum;

public class MISTK {
    private static final Logger logger = LoggerFactory.getLogger(MISTK.class);

    private class MISTKState {
        private String name;
        private Process p;
        private ModelInstanceEndpointApi api;
    }

    private String mistkPluginsDirectory;
    private Map<String, MXPluginDescription> mistkPluginIDToDesc;
    private Map<String, MISTKState> instanceIDToMistkState;

    public MXPluginDescription getPlugin(String pluginID) {
        return this.mistkPluginIDToDesc.get(pluginID);
    }

    public boolean hasInstance(String mxpInstanceID) {
        return this.instanceIDToMistkState.get(mxpInstanceID) != null;
    }

    private String encodeBase64(String jsonRowData) {
        byte[] bytes = jsonRowData.getBytes();
        byte[] encodedBytes = Base64.getEncoder().encode(bytes);
        return new String(encodedBytes);
    }

    private StateEnum getStatus(ModelInstanceEndpointApi api)
            throws ApiException {
        Boolean watch = null;
        Integer resourceVersion = null;
        ModelInstanceStatus status = api.getStatus(watch, resourceVersion);
        return status.getState();
    }

    private boolean waitForState(ModelInstanceEndpointApi api,
            StateEnum targetState) {
        for (int i = 0; i < 5; i++) {
            try {
                StateEnum state = getStatus(api);
                if (state.equals(targetState)) {
                    logger.info("MISTK server reached state " + targetState);
                    return true;
                } else if (i == 4) {
                    logger.error("Timed out waiting for MISTK to reach state " + targetState);
                    return false;
                }
            } catch (ApiException e) {
                System.err.println("Exception when calling ModelInstanceEndpointApi#getStatus");
            }

            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                logger.error("Could not sleep to wait for MISTK to reach state " + targetState + "; " + e);
                return false;
            }
        }
        return false;
    }

    private MISTKState startMISTK(String mistkPluginsDirectory,
            String mistkPluginName, String mxpInstanceID,
            String modelDirectory, String modelFilename) {
        String[] commands = {
            "python3",
            "-m",
            "mistk",
            mistkPluginName,
            mistkPluginName
        };
        String[] envp = { };
        File dir = new File(mistkPluginsDirectory);

        File mistkModelFile = new File(dir, mistkPluginName + ".py");
        if (!mistkModelFile.exists()) {
            logger.error("Cannot find MISTK plugin by name: " + mistkModelFile);
            return null;
        }

        File model = new File(modelDirectory, modelFilename);
        if (!model.exists()) {
            logger.error("Model at " + model + " does not exist");
            return null;
        }

        MISTKState state = new MISTKState();
        state.name = mistkPluginName;
        state.p = null;
        try {
            state.p = Runtime.getRuntime().exec(commands, envp, dir);
        } catch (IOException e) {
            logger.error("Could not run MISTK for plugin " + mistkPluginName + ": " + e);
            return null;
        }

        ApiClient apic = new ApiClient();
        apic.setBasePath("http://127.0.0.1:8080/v1/mistk");
        state.api = new ModelInstanceEndpointApi(apic);

        if (!waitForState(state.api, StateEnum.STARTED)) {
            logger.error("Could not start MISTK server for plugin " + mistkPluginName);
            stop(state);
            return null;
        }

        try {
            ModelInstanceInitParams ip = new ModelInstanceInitParams();
            state.api.initializeModel(ip);
        } catch (ApiException e) {
            logger.error("Exception when calling initializeModel for plugin " + mistkPluginName + ": " + e);
            e.printStackTrace();
            stop(state);
            return null;
        }

        if (!waitForState(state.api, StateEnum.INITIALIZED)) {
            logger.error("MISTK server could not reach INITIALIZED state for plugin " + mistkPluginName);
            stop(state);
            return null;
        }

        try {
            state.api.buildModel(model.toString());
        } catch (ApiException e) {
            logger.error("Exception when calling buildModel for plugin " + mistkPluginName + " for model " + model.toString() + ": " + e);
            e.printStackTrace();
            stop(state);
            return null;
        }

        if (!waitForState(state.api, StateEnum.READY)) {
            logger.error("MISTK server could not reach READY state for plugin " + mistkPluginName);
            stop(state);
            return null;
        }

        return state;
    }

    public boolean configure(Map<String, Object> map) throws InsufficientConfigurationException {
        // Probably only need boolean, int, and double.
        Map<Class<?>, Class<?>> typeMap = new HashMap<Class<?>, Class<?>>();
        typeMap.put(Boolean.class, boolean.class);
        typeMap.put(Byte.class, byte.class);
        typeMap.put(Short.class, short.class);
        typeMap.put(Character.class, char.class);
        typeMap.put(Integer.class, int.class);
        typeMap.put(Long.class, long.class);
        typeMap.put(Float.class, float.class);
        typeMap.put(Double.class, double.class);

        this.mistkPluginsDirectory = (String)map.get("mistk_plugins_directory");
        ArrayList<HashMap<String, Object>> plugins =
            (ArrayList<HashMap<String, Object>>)map.get("mistk_plugins");

        if (this.mistkPluginsDirectory == null && plugins == null)
            return false;
        else if ((this.mistkPluginsDirectory == null) != (plugins == null)) {
            throw new InsufficientConfigurationException("If using MISTK, both \"mistk_plugins_directory\" and \"plugins\" must be specified as configuration options");
        }

        this.mistkPluginIDToDesc = new HashMap<String, MXPluginDescription>();

        /* Get attribute names from MXPluginDescription. */
        Method[] methods = MXPluginDescription.class.getDeclaredMethods();
        HashMap<String, Class<?>> attributes = new HashMap<String, Class<?>>();
        for (int i = 0; i < methods.length; i++)
            attributes.put(methods[i].getName(), methods[i].getReturnType());
            
        for (HashMap<String, Object> plugin : plugins) {
            Map<String, Object> descParams = new HashMap<String, Object>();
            for (Map.Entry<String, Class<?>> entry : attributes.entrySet()) {
                String attrKey = entry.getKey();
                Object attrValue = plugin.get(attrKey);
                if (attrValue == null) {
                    throw new InsufficientConfigurationException("MISTK has a configuration for a plugin that is missing the \"" + attrKey + "\" attribute");
                }
                if (!entry.getValue().isInstance(attrValue) &&
                        attrValue.getClass().isPrimitive() &&
                        !entry.getValue().isInstance(typeMap.get(attrValue.getClass()))) {
                    throw new InsufficientConfigurationException("MISTK has a configuration for the attribute \"" + attrKey + "\" but it should be of type " + entry.getValue() + ", not " + attrValue.getClass());
                }
                descParams.put(attrKey, attrValue);
            }
            MXPluginDescription desc;
            try {
                desc = TypeFactory.annotation(MXPluginDescription.class, descParams);
            } catch (AnnotationFormatException e) {
                throw new InsufficientConfigurationException("MISTK configuration could not format an annotation for a plugin");
            }
            this.mistkPluginIDToDesc.put(desc.id(), desc);
        }

        this.instanceIDToMistkState = new HashMap<String, MISTKState>();
        return true;
    }

    public MXFAck instantiate(MXPluginDescription mistkDesc, String pluginID,
            String mxpInstanceID, String modelDirectory, String modelFilename,
            HashMap<String, Serializable> params) {
        MISTKState mistkState = startMISTK(this.mistkPluginsDirectory,
            mistkDesc.name(), mxpInstanceID, modelDirectory, modelFilename);
        if (mistkState != null) {
            this.instanceIDToMistkState.put(mxpInstanceID, mistkState);
            return MXFAck.newBuilder()
                .setSuccess(true)
                .setMsg("Successfully instantiated plugin " + pluginID)
                .setInstanceID(mxpInstanceID)
                .build();
        }

        return MXFAck.newBuilder()
            .setSuccess(false)
            .setMsg("Could not instantiate MISTK plugin with ID " + pluginID + " with model " + modelFilename)
            .setInstanceID("")
            .build();
    }

    public MXFPrediction execute(String mxpInstanceID, byte[] inputData) {
        MISTKState state = this.instanceIDToMistkState.get(mxpInstanceID);
        if (state == null) {
            return MXFPrediction.newBuilder()
                .setSuccess(false)
                .setMsg("Cannot find MISTK state for instance " + mxpInstanceID)
                .setInstanceID(mxpInstanceID)
                .build();
        }
        String mistkPluginName = state.name;

        /*
         * The given data is a byte[] that was serialized from an Object[].
         * We need to unserialize it, encode any non-primitive
         * JSON types within it as a string, put all of the data
         * into a JSON array, and then base64 encode the entire string.
         */
        Object[] data;
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(inputData);
            ObjectInput oi = new ObjectInputStream(bis);
            data = (Object[])oi.readObject();
            oi.close();
            bis.close();
        } catch (IOException | ClassNotFoundException e) {
            return MXFPrediction.newBuilder()
                .setSuccess(false)
                .setMsg("Exception when constructing input data for plugin " + mistkPluginName + ": " + e)
                .setInstanceID(mxpInstanceID)
                .build();
        }

        JsonArrayBuilder builder;
        builder = Json.createArrayBuilder();
        for (int i = 0; i < data.length; i++) {
            Object o = data[i];
            if (o instanceof Integer || o instanceof Byte ||
                    o instanceof Short) {
                builder.add((int)o);
            } else if (o instanceof Long) {
                builder.add((long)o);
            } else if (o instanceof Double || o instanceof Float) {
                builder.add((double)o);
            } else if (o instanceof Boolean) {
                builder.add((boolean)o);
            } else if (o instanceof String) {
                builder.add((String)o);
            } else if (o instanceof byte[]) {
                String s = new String((byte[])o);
                builder.add((String)s);
            } else if (o == null) {
                builder.addNull();
            } else {
                logger.error("Cannot encode anything other than numbers, booleans, strings, and byte arrays into JSON. Convert any other object to a byte array first");
                return MXFPrediction.newBuilder()
                    .setSuccess(false)
                    .setMsg("Error when encoding JSON for plugin " + mistkPluginName)
                    .setInstanceID(mxpInstanceID)
                    .build();
            }
        }
        JsonArray j = builder.build();
        Map<String, Object> dataMap = new HashMap<String, Object>();
        /* For now, we only support one prediction at a time. */
        dataMap.put("0", encodeBase64(j.toString()));

        Map<String, Object> resultMap;
        try {
            resultMap = (Map<String, Object>)state.api.streamPredict(dataMap);
        } catch (ApiException e) {
            return MXFPrediction.newBuilder()
                .setSuccess(false)
                .setMsg("Exception when calling streamPredict for plugin " + mistkPluginName + ": " + e)
                .setInstanceID(mxpInstanceID)
                .build();
        }

        /* For now, we only support one prediction at a time. */
        Object prediction = resultMap.get("0");

        /* Serialize prediction to send back to client. */
        byte[] result;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(prediction);
            result = bos.toByteArray();
            out.close();
            bos.close();
        } catch (IOException e) {
            return MXFPrediction.newBuilder()
                .setSuccess(false)
                .setMsg("Exception when construction prediction byte array for plugin " + mistkPluginName + ": " + e)
                .setInstanceID(mxpInstanceID)
                .build();
        }

        return MXFPrediction.newBuilder()
            .setData(ByteString.copyFrom(result))
            .setSuccess(true)
            .setMsg("Successfully executed prediction for instance " + mxpInstanceID)
            .setInstanceID(mxpInstanceID)
            .build();
    }

    public MXFAck destroy(String mxpInstanceID) {
        MISTKState state = this.instanceIDToMistkState.get(mxpInstanceID);
        if (state == null) {
            return MXFAck.newBuilder()
                .setSuccess(false)
                .setMsg("Could not find plugin instance with ID " + mxpInstanceID)
                .setInstanceID(mxpInstanceID)
                .build();
        }

        stop(state);
        this.instanceIDToMistkState.remove(mxpInstanceID);
        return MXFAck.newBuilder()
            .setSuccess(true)
            .setMsg("Successfully destroyed plugin instance with ID " + mxpInstanceID)
            .setInstanceID(mxpInstanceID)
            .build();
    }

    public void listPlugins(MXFPluginList.Builder b) {
        for (MXPluginDescription desc : this.mistkPluginIDToDesc.values()) {
            b.addPlugins(MXFPluginDescription.newBuilder()
                .setId(desc.id())
                .setName(desc.name())
                .setAuthor(desc.author())
                .setLibrary(desc.library())
                .setAlgorithm(desc.algorithm())
                .setVersion(desc.version())
                .setClientSide(desc.clientSide())
                .setServerSide(desc.serverSide())
                .setDescription(desc.description())
                .build());
        }
    }

    private void stop(MISTKState state) {
        try {
            if (state.api != null)
                state.api.terminate();
        } catch (ApiException e) {
            logger.error("Exception when calling terminate for MISTK plugin " + state.name + ": " + e);
            e.printStackTrace();
        }

        if (state.p != null)
            state.p.destroy();
    }

    public void stop() {
        for (MISTKState state : this.instanceIDToMistkState.values())
            stop(state);
        this.instanceIDToMistkState = null;
        this.mistkPluginIDToDesc = null;
    }
}
