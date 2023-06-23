/*******************************************************************************
 * Copyright (c) 2020 Raytheon BBN Technologies.
 *******************************************************************************/

package com.bbn.takml.mxf;

import com.bbn.tak.ml.mx_framework.MXPlugin;
import com.bbn.tak.ml.mx_framework.MXPluginDescription;

import java.util.*;
import java.io.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.bbn.roger.config.AttributeDescription;
import com.bbn.roger.plugin.PluginContext;
import com.bbn.roger.plugin.Plugin;
import com.bbn.roger.plugin.StartablePlugin;
import com.bbn.roger.plugin.UtilityPluginManager;
import com.bbn.roger.plugin.exception.InsufficientConfigurationException;
import com.bbn.roger.plugin.exception.RogerInstantiationException;
import com.bbn.roger.plugin.io.attribute.BasicServerConfigAttributes;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;

import com.bbn.tak.ml.mx_framework.MXFAck;
import com.bbn.tak.ml.mx_framework.MXFInstantiateParams;
import com.bbn.tak.ml.mx_framework.MXFDestroyParams;
import com.bbn.tak.ml.mx_framework.MXFExecuteParams;
import com.bbn.tak.ml.mx_framework.MXFPrediction;
import com.bbn.tak.ml.mx_framework.MXFPluginList;
import com.bbn.tak.ml.mx_framework.ModelList;
import com.bbn.tak.ml.mx_framework.Model;
import com.bbn.tak.ml.mx_framework.ModelName;
import com.bbn.tak.ml.mx_framework.ModelFile;
import com.bbn.tak.ml.mx_framework.MXFPluginDescription;
import com.bbn.tak.ml.mx_framework.MXFrameworkGrpc;

@com.bbn.roger.annotation.Plugin(
    author = "Cody Doucette"
)

public class MXFrameworkPlugin implements StartablePlugin {
    private static final Logger logger = LoggerFactory.getLogger(MXFrameworkPlugin.class);

    private int port;
    private Server server;
    private String defaultModelDirectory;
    private PluginContext pluginContext;
    private String pluginName;
    private MISTK mistk;
    private boolean started = false;

    // Plugin ID to MXPlugin class, for purposes of instantiating plugin.
    // Non-native plugins may map to native plugins. For example, MISTK plugins
    // written in other languages map to the MISTKPlugin object.
    private Map<String, Class<?>> pluginIDToPluginClass;
    // Plugin ID to plugin description, for purposes of reporting plugin info.
    private Map<String, MXPluginDescription> pluginIDToDesc;
    // Instance ID to MXPlugin object, for the purpose of executing predictions.
    private Map<String, MXPlugin> instanceIDToPlugin;
    // Instance ID to plugin ID, for purposes of finding which logical plugin
    // (which may not be native) corresponds to an instance. For example, an
    // instance may map to the plugin ID for a Python TensorFlow plugin, which
    // is handled by the MISTKPlugin.
    private Map<String, String> instanceIDToPluginID;

    static final Set<AttributeDescription> CONFIGURATION_ATTRIBUTES = new HashSet<>();

    private static final AttributeDescription[] MXFRAMEWORK_ATTRIBUTES = {
        new AttributeDescription(
            "default_model_directory",
            "The default directory where binary models are stored",
            false,
            String.class
        ),
        new AttributeDescription(
            "mistk_plugins_directory",
            "The directory where MISTK models (plugins) are stored",
            false,
            String.class
        ),
        new AttributeDescription(
            "mistk_plugins",
            "MXPluginDescription objects of all plugins for MISTK",
            false,
            ArrayList.class
        )
    };

    static {
        CONFIGURATION_ATTRIBUTES.addAll(BasicServerConfigAttributes.getAllAttributes());
        CONFIGURATION_ATTRIBUTES.addAll(Arrays.asList(MXFRAMEWORK_ATTRIBUTES));
    }

    public MXFrameworkPlugin() {
        this.pluginIDToPluginClass = new HashMap<String, Class<?>>();
        this.pluginIDToDesc = new HashMap<String, MXPluginDescription>();
        this.instanceIDToPlugin = new HashMap<String, MXPlugin>();
        this.instanceIDToPluginID = new HashMap<String, String>();
    }

    @Override
    public void configure(Map<String, Object> map, PluginContext pluginContext)
            throws InsufficientConfigurationException {
        this.port = (int)map.get(
            BasicServerConfigAttributes.Attribute.SERVER_PORT_ATTR.toString());

        this.defaultModelDirectory = (String)map.get("default_model_directory");
        if (this.defaultModelDirectory == null)
            this.defaultModelDirectory = "/opt/takml/models";

        this.mistk = new MISTK();
        if (!this.mistk.configure(map)) {
            this.mistk = null;
        }

        this.pluginContext = pluginContext;
    }

    @Override
    public void initialize() throws RogerInstantiationException {
        this.server = ServerBuilder.forPort(port)
            .addService(new MXFramework()).build();
       /*
        * Register the MXFrameworkPlugin here, so that individual MXPs
        * can find it. Note that this,.pluginName is not set until after
        * this method is called, and putting this code in start() will
        * make it too late, since MXPs may start before the MX framework.
        */
        UtilityPluginManager utilityPluginManager =
            pluginContext.getUtilityPluginManager();
        utilityPluginManager.registerPlugin("MXFrameworkPlugin", this);
    }

    @Override
    public void start() {
        try {
            server.start();
            logger.info("Server started, listening on port " + this.port);
        } catch (IOException e) {
            System.err.println("Could not start server");
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                this.stop();
            }
        });
        setStarted(true);
    }

    public void stop() {
        if (this.mistk != null)
            this.mistk.stop();
        if (server != null) {
            server.shutdown();
        }
        setStarted(false);
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

    private MXFAck instantiateNative(Class<?> mxpClass, String pluginID,
            String mxpInstanceID, String modelDirectory, String modelFilename,
            HashMap<String, Serializable> params) {

        MXPlugin mxp;
        try {
            mxp = (MXPlugin)mxpClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            return MXFAck.newBuilder()
                    .setSuccess(false)
                    .setMsg("Could not instantiate plugin with ID " + pluginID + " with model " + modelFilename + ": " + e)
                    .setInstanceID("")
                    .build();
        }

        boolean success = mxp.instantiate(modelDirectory,
            modelFilename, params);
        if (success) {
            this.instanceIDToPlugin.put(mxpInstanceID, mxp);
            this.instanceIDToPluginID.put(mxpInstanceID, pluginID);
            return MXFAck.newBuilder()
                .setSuccess(true)
                .setMsg("Successfully instantiated plugin " + pluginID)
                .setInstanceID(mxpInstanceID)
                .build();
        }

        return MXFAck.newBuilder()
            .setSuccess(false)
            .setMsg("Could not instantiate plugin with ID " + pluginID + " with model " + modelFilename)
            .setInstanceID("")
            .build();
    }

    private String generateMxpInstanceID() {
        int i = 0;
        String mxpInstanceID;
        do {
            mxpInstanceID = "/takml_mxf_instance_" + UUID.randomUUID().toString();
            if (i++ > 100) {
                throw new RuntimeException("Unable to find a random string to use as an instance ID; there is likely a bug");
            }
        } while (this.instanceIDToPlugin.containsKey(mxpInstanceID));
        return mxpInstanceID;
    }

    private MXFAck instantiate(MXFInstantiateParams request) {
        String pluginID = request.getPluginID();
        Class<?> mxpClass = this.pluginIDToPluginClass.get(pluginID);
        MXPluginDescription mistkDesc = null;
        if (mxpClass == null && this.mistk != null) {
            mistkDesc = this.mistk.getPlugin(pluginID);
        }

        if (mxpClass == null && mistkDesc == null) {
            return MXFAck.newBuilder()
                .setSuccess(false)
                .setMsg("Could not find registered plugin with ID " + pluginID)
                .setInstanceID("")
                .build();
        }

        String mxpInstanceID = generateMxpInstanceID();

        HashMap<String, Serializable> params;
        if (request.getParams().equals(ByteString.EMPTY))
            params = new HashMap<String, Serializable>();
        else
            params = (HashMap<String, Serializable>)deserialize(request.getParams().toByteArray());

        String modelDirectory = request.getModelDirectory().equals("")
            ? this.defaultModelDirectory
            : request.getModelDirectory();

        MXFAck ack;
        if (mxpClass != null) {
            ack = instantiateNative(mxpClass, pluginID, mxpInstanceID,
                modelDirectory, request.getModelFilename(), params);
        } else if (mistkDesc != null) {
            ack = this.mistk.instantiate(mistkDesc, pluginID, mxpInstanceID,
                modelDirectory, request.getModelFilename(), params);
        } else {
            throw new RuntimeException("Plugin was found, but is neither a native nor MISTK plugin\n");
        }
        return ack;
    }

    private MXFPrediction executeNative(MXPlugin mxp,
            String mxpInstanceID, byte[] inputData) {
        byte[] output = mxp.execute(inputData);
        if (output == null) {
            return MXFPrediction.newBuilder()
                .setSuccess(false)
                .setMsg("Error executing prediction for plugin instance with ID " + mxpInstanceID)
                .setInstanceID(mxpInstanceID)
                .build();
        }

        return MXFPrediction.newBuilder()
            .setData(ByteString.copyFrom(output))
            .setSuccess(true)
            .setMsg("Successfully executed prediction for instance " + mxpInstanceID)
            .setInstanceID(mxpInstanceID)
            .build();
    }

    private MXFPrediction execute(MXFExecuteParams request) {
        String mxpInstanceID = request.getInstanceID();
        MXPlugin mxp = this.instanceIDToPlugin.get(mxpInstanceID);
        boolean mistkHasInstance = false;
        if (mxp == null && this.mistk != null) {
            mistkHasInstance = this.mistk.hasInstance(mxpInstanceID);
        }

        if (mxp == null && !mistkHasInstance) {
            return MXFPrediction.newBuilder()
                .setSuccess(false)
                .setMsg("Could not find plugin instance with ID " + mxpInstanceID)
                .setInstanceID(mxpInstanceID)
                .build();
        }

        MXFPrediction predict;
        if (mxp != null) {
            predict = executeNative(mxp, mxpInstanceID,
                request.getData().toByteArray());
        } else if (mistkHasInstance) {
            predict = this.mistk.execute(mxpInstanceID,
                request.getData().toByteArray());
        } else {
           return MXFPrediction.newBuilder()
                .setSuccess(false)
                .setMsg("Instance was found, but is neither a native nor MISTK plugin")
                .setInstanceID(mxpInstanceID)
                .build();
        }
        return predict;
    }

    private MXFAck destroyNative(MXPlugin mxp, String mxpInstanceID) {
        boolean success = mxp.destroy();
        if (!success) {
            return MXFAck.newBuilder()
                .setSuccess(false)
                .setMsg("Could not destroy plugin instance with ID " + mxpInstanceID)
                .setInstanceID(mxpInstanceID)
                .build();
        }

        this.instanceIDToPlugin.remove(mxpInstanceID);
        this.instanceIDToPluginID.remove(mxpInstanceID);
        return MXFAck.newBuilder()
            .setSuccess(true)
            .setMsg("Successfully destroyed plugin instance with ID " + mxpInstanceID)
            .setInstanceID(mxpInstanceID)
            .build();
    }

    private MXFAck destroy(MXFDestroyParams request) {
        String mxpInstanceID = request.getInstanceID();
        MXPlugin mxp = this.instanceIDToPlugin.get(mxpInstanceID);
        boolean mistkHasInstance = false;
        if (mxp == null && this.mistk != null) {
            mistkHasInstance = this.mistk.hasInstance(mxpInstanceID);
        }

        if (mxp == null && !mistkHasInstance) {
            return MXFAck.newBuilder()
                .setSuccess(false)
                .setMsg("Could not find plugin instance with ID " + mxpInstanceID)
                .setInstanceID(mxpInstanceID)
                .build();
        }

        MXFAck ack;
        if (mxp != null) {
            ack = destroyNative(mxp, mxpInstanceID);
        } else if (mistkHasInstance) {
            ack = this.mistk.destroy(mxpInstanceID);
        } else {
           return MXFAck.newBuilder()
                .setSuccess(false)
                .setMsg("Instance was found, but is neither a native nor MISTK plugin")
                .setInstanceID(mxpInstanceID)
                .build();
        }
        return ack;
    }

    private MXFPluginList listPlugins() {
        MXFPluginList.Builder b = MXFPluginList.newBuilder()
            .setSuccess(true)
            .setMsg("Successfully listed server plugins");
        for (MXPluginDescription desc : this.pluginIDToDesc.values()) {
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
        this.mistk.listPlugins(b);
        return b.build();
    }

    private ModelList listModels() {
        File dir = new File(this.defaultModelDirectory);
        File [] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return !name.endsWith(".json");
            }
        });

        ModelList.Builder b = ModelList.newBuilder()
            .setSuccess(true)
            .setMsg("Successfully listed server models")
            .setDirectory(this.defaultModelDirectory);
        for (File f : files) {
            String modelName = f.getName();
            File metadataDir = new File(f.getParentFile().getParentFile(), "metadata");
            int extIndex = modelName.lastIndexOf(".");
            File metadataFile = new File(metadataDir, modelName.substring(0, extIndex + 1) + "json");

            if (!metadataFile.exists()) {
                logger.info("No metadata file (in JSON) for model " + f.getName());
                continue;
            }

            byte[] fileContents = null;
            String metadata;
            try {
                fileContents = Files.readAllBytes(metadataFile.toPath());
                metadata = new String(fileContents, StandardCharsets.UTF_8);
            } catch (IOException e) {
                logger.error("Error reading metadata file for model " + f.getName() + ": " + e);
                metadata = "";
            }

            b.addModels(Model.newBuilder()
                .setName(f.getName())
                .setSource("Server")
                .setMetadata(metadata)
                .build());
        }
        return b.build();
    }

    private ModelFile pullModel(ModelName mName) {
        File modelFile = new File(this.defaultModelDirectory, mName.getName());
        if (!modelFile.exists()) {
            logger.error("No model file for request for " + mName.getName());
            return ModelFile.newBuilder()
                .setSuccess(false)
                .setMsg("Model " + mName.getName() + " does not exist")
                .build();
    
        }

        byte[] modelContents = null;
        try {
            modelContents = Files.readAllBytes(modelFile.toPath());
        } catch (IOException e) {
            logger.error("Error reading model file for model " + mName.getName() + ": " + e);
            return ModelFile.newBuilder()
                .setSuccess(false)
                .setMsg("Model " + mName.getName() + " could not be read")
                .build();
        }

        String metadata;
        String modelName = modelFile.getName();
        File metadataDir = new File(modelFile.getParentFile().getParentFile(), "metadata");
        int extIndex = modelName.lastIndexOf(".");
        File metadataFile = new File(metadataDir, modelName.substring(0, extIndex + 1) + "json");
        if (!metadataFile.exists()) {
            logger.info("No metadata file (in JSON) for model " + mName.getName());
            metadata = "";
        } else {
            try {
                byte[] metadataContents = Files.readAllBytes(metadataFile.toPath());
                metadata = new String(metadataContents, StandardCharsets.UTF_8);
            } catch (IOException e) {
                logger.error("Error reading metadata file for model " + mName.getName() + ": " + e);
                metadata = "";
            }
        }

        return ModelFile.newBuilder()
            .setSuccess(true)
            .setMsg("Successfully pulled server model")
            .setModel(Model.newBuilder()
                .setName(mName.getName())
                .setSource("Server")
                .setMetadata(metadata)
                .build())
            .setContents(ByteString.copyFrom(modelContents))
            .build();
    }

    public boolean register(Class<?> mxpClass) {
        MXPluginDescription desc = mxpClass.getAnnotation(MXPluginDescription.class);
        if (desc == null) {
            logger.error("Classes implementing MXPlugin (" + mxpClass + ") need to use MXPluginDescpription annotation");
            return false;
        }

        this.pluginIDToPluginClass.put(desc.id(), mxpClass);
        this.pluginIDToDesc.put(desc.id(), desc);
        return true;
    }

    public boolean deregister(Class<?> mxpClass) {
        MXPluginDescription desc = mxpClass.getAnnotation(MXPluginDescription.class);
        if (desc == null) {
            logger.error("Classes implementing MXPlugin (" + mxpClass + ") need to use MXPluginDescpription annotation");
            return false;
        }

        this.pluginIDToPluginClass.remove(desc.id());
        this.pluginIDToDesc.remove(desc);
        return true;
    }

    private class MXFramework extends MXFrameworkGrpc.MXFrameworkImplBase {
        @Override
        public void mxfInstantiate(MXFInstantiateParams request,
                StreamObserver<MXFAck> responseObserver) {
            responseObserver.onNext(instantiate(request));
            responseObserver.onCompleted();
        }

        @Override
        public void mxfExecute(MXFExecuteParams request,
                StreamObserver<MXFPrediction> responseObserver) {
            responseObserver.onNext(execute(request));
            responseObserver.onCompleted();
        }

        @Override
        public void mxfDestroy(MXFDestroyParams request,
                StreamObserver<MXFAck> responseObserver) {
            responseObserver.onNext(destroy(request));
            responseObserver.onCompleted();
        }

        @Override
        public void mxfListPlugins(Empty request,
                StreamObserver<MXFPluginList> responseObserver) {
            responseObserver.onNext(listPlugins());
            responseObserver.onCompleted();
        }

        @Override
        public void mxfListModels(Empty request,
                StreamObserver<ModelList> responseObserver) {
            responseObserver.onNext(listModels());
            responseObserver.onCompleted();
        }

        @Override
        public void mxfPullModel(ModelName name,
                StreamObserver<ModelFile> responseObserver) {
            responseObserver.onNext(pullModel(name));
            responseObserver.onCompleted();
        }
    }

    @Override
    public boolean isStarted() {
        synchronized (this) {
            return this.started;
        }
    }

    private void setStarted(boolean started) {
        synchronized (this) {
            this.started = started;
            notifyAll();
        }
    }

    @Override
    public Set<AttributeDescription> getConfigurationAttributes() {
        return CONFIGURATION_ATTRIBUTES;
    }

}
