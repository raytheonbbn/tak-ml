package com.bbn.takml.mx_framework;

import static com.bbn.tak.ml.TakMlConstants.*;
import static com.bbn.tak.ml.TakMlConstants.MX_LIST_RESOURCES_REPLY;
import static com.bbn.tak.ml.TakMlConstants.MX_PLUGIN_COMMAND_PREFIX;
import static com.bbn.tak.ml.TakMlConstants.MX_REFRESH;

import android.content.Context;
import android.widget.Toast;

import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;
import com.bbn.tak.ml.mx_framework.MXFAck;
import com.bbn.tak.ml.mx_framework.MXFDestroyParams;
import com.bbn.tak.ml.mx_framework.MXFExecuteParams;
import com.bbn.tak.ml.mx_framework.MXFInstantiateParams;
import com.bbn.tak.ml.mx_framework.MXFPluginDescription;
import com.bbn.tak.ml.mx_framework.MXFPluginList;
import com.bbn.tak.ml.mx_framework.MXFPrediction;
import com.bbn.tak.ml.mx_framework.MXFrameworkGrpc;
import com.bbn.tak.ml.mx_framework.MXPluginDescription;
import com.bbn.tak.ml.mx_framework.Model;
import com.bbn.tak.ml.mx_framework.ModelFile;
import com.bbn.tak.ml.mx_framework.ModelList;
import com.bbn.tak.ml.mx_framework.ModelName;
import com.bbn.takml.framework.ChangeType;
import com.bbn.takml.listener.ServerPublisher;
import com.bbn.takml_sdk_android.mx_framework.request.*;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.leangen.geantyref.AnnotationFormatException;
import io.leangen.geantyref.TypeFactory;

public class MXFramework {

    private final static String TAG = "TAKML_MX_Framework";
    private ServerPublisher publisher;
    private Map<String, String> instanceIDToPluginID;
    private Map<String, String> pluginIDToPluginTopic;
    private Map<String, MXPluginDescription> pluginIDToPluginDesc;
    private Map<String, Model> modelLabelToModel;
    private Map<String, Model> serverModels;
    private MXPluginChangedCallback mxPluginChangedCallback;
    private ModelChangedCallback modelChangedCallback;
    private InstanceChangedCallback instanceChangedCallback;

    /* Channel to MX framework on server. */
    private ManagedChannel channel;
    private MXFrameworkGrpc.MXFrameworkStub asyncStub;
    private String remoteServIP;
    private int remoteServPort;
    private Context context;

    private File defaultModelsDirectory;

    public MXFramework(Context context, MXPluginChangedCallback mxPluginChangedCallback,
                       ModelChangedCallback modelChangedCallback,
                       InstanceChangedCallback instanceChangedCallback,
                       File takmlDirectory, String remoteServIP, int remoteServPort) {
        Log.w(TAG, "Creating MX Framework");
        this.context = context;
        this.instanceIDToPluginID = new HashMap<String, String>();

        // Server plugins are in @pluginIDToPluginDesc but not @pluginIDToPluginTopic.
        this.pluginIDToPluginTopic = new HashMap<String, String>();
        this.pluginIDToPluginDesc = new HashMap<String, MXPluginDescription>();

        // Store server models to be able to update TAK-ML Framework.
        this.serverModels = new HashMap<String, Model>();

        this.mxPluginChangedCallback = mxPluginChangedCallback;
        this.modelChangedCallback = modelChangedCallback;
        this.instanceChangedCallback = instanceChangedCallback;

        this.remoteServIP = remoteServIP;
        this.remoteServPort = remoteServPort;

        this.defaultModelsDirectory = new File(takmlDirectory, "models");
        this.defaultModelsDirectory.mkdir();
        File defaultMetadataDirectory = new File(takmlDirectory, "metadata");
        defaultMetadataDirectory.mkdir();
        this.modelLabelToModel = new HashMap<String, Model>();
        refreshClientModels();
        Log.i(TAG, "Default models directory: " + this.defaultModelsDirectory.getPath());

        grpcChannelConnect();
        refreshServerPlugins();
        refreshServerModels();
    }

    public void stop() {
        grpcChannelDisconnect();
        removeAllPlugins();
        removeAllModels();
        listResources();
    }

    private void grpcChannelDisconnect() {
        try {
            this.channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void grpcChannelConnect() {

        if(this.remoteServIP == null || this.remoteServIP.isEmpty()) {
            Toast.makeText(MapView.getMapView().getContext(), "No server information has been set for TAK-ML. Not connecting. Server info can be updated in TAK-ML settings.", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "Connecting to " + this.remoteServIP + ":" + this.remoteServPort);
        try {
            this.channel = ManagedChannelBuilder
                    .forAddress(this.remoteServIP, this.remoteServPort)
                    .usePlaintext()
                    .build();
            this.asyncStub = MXFrameworkGrpc.newStub(channel);
        } catch (Exception e) {
            Log.e(TAG, "Unable to connect to server " + this.remoteServIP + ":" + this.remoteServPort, e);
            Toast.makeText(MapView.getMapView().getContext(), "Unable to connect to TAK-ML server at " + this.remoteServIP + ":" + this.remoteServPort + ". Server info can be updated in TAK-ML settings.", Toast.LENGTH_SHORT).show();
        }
    }

    public void grpcChannelReconnect(String remoteServIP, int remoteServPort) {
        grpcChannelDisconnect();
        this.remoteServIP = remoteServIP;
        this.remoteServPort = remoteServPort;
        grpcChannelConnect();
    }

    private void publisherPublish(String topic, byte[] bytes) {
        if(publisher == null) {
            Log.w(TAG, "TAK-ML publisher not yet initialized, but received publication");
            return;
        }
        try {
            this.publisher.publish(topic, bytes, "takml_mxf");
        } catch (IllegalStateException e) {
            e.printStackTrace();
            Log.i(TAG, "Could not publish message to topic " + topic + "; server broker not yet started");
        }
    }

    public void setPublisher(ServerPublisher publisher) {
        this.publisher = publisher;

        /*
         * Can only be done once the publisher is set,
         * but are likely to fail anyway here because
         * the server is probably still setting up.
         */
        refreshClientPlugins();
        listResources();
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
            Log.e(TAG, "Exception when constructing input data: " + e);
            e.printStackTrace();
            return null;
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Exception when constructing input data: " + e);
            e.printStackTrace();
            return null;
        }
        return o;
    }

    public static byte[] serialize(Object o, String msg) {
        byte[] bytes;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(o);
            bytes = bos.toByteArray();
            out.close();
            bos.close();
        } catch (IOException e) {
            Log.e(TAG, msg);
            return null;
        }
        return bytes;
    }

    private void instantiateError(MXInstantiateRequest req, String msg) {
        Log.e(TAG, msg);
        MXInstantiateReply reply = new MXInstantiateReply(req.getToken(), null,
                req.getPluginID(), false, msg);
        byte[] bytes = serialize(reply, "Error serializing error in request for token " + req.getToken() + " regarding plugin " + req.getPluginID());
        if (bytes == null)
            return;
        publisherPublish(req.getToken(), bytes);
    }

    public void instantiate(byte[] bytes) {
        Log.w(TAG, "Called instantiate()");
        MXInstantiateRequest req = (MXInstantiateRequest)deserialize(bytes);

        if (!this.pluginIDToPluginDesc.containsKey(req.getPluginID())) {
            instantiateError(req, "Plugin with ID " + req.getPluginID() + " not found among available plugins");
            return;
        }

        MXPluginDescription desc = this.pluginIDToPluginDesc.get(req.getPluginID());
        if (desc == null) {
            instantiateError(req, "Plugin with ID " + req.getPluginID() + " was found but has no descriptive attributes");
            return;
        }

        if (desc.clientSide()) {
            String pluginTopic = this.pluginIDToPluginTopic.get(req.getPluginID());
            if (pluginTopic == null) {
                instantiateError(req, "Local plugin with ID " + req.getPluginID() + " has no registered callback topic");
                return;
            }

            String mxpInstanceID;
            int i = 0;
            do {
                mxpInstanceID = "/takml_mxf_instance_" + UUID.randomUUID().toString();
                if (i++ > 100) {
                    instantiateError(req, "Unable to find a random string to use as an instance ID; there is likely a bug");
                    return;
                }
            } while (this.instanceIDToPluginID.containsKey(mxpInstanceID));
            this.instanceIDToPluginID.put(mxpInstanceID, req.getPluginID());
            this.instanceChangedCallback.instanceChangeOccurred(req.getPluginID(),
                    mxpInstanceID, ChangeType.ADDED);

            // Set assigned MXP instance ID.
            req.setMxpInstanceID(mxpInstanceID);
            // Insert default directory if none was specified.
            if (req.getModelDirectory() == null)
                req.setModelDirectory(this.defaultModelsDirectory.getPath());

            byte[] newBytes = serialize(req, "Error serializing request for token " + req.getToken() + " regarding plugin " + req.getPluginID());
            if (newBytes == null) {
                instantiateError(req, "Error serializing request for token " + req.getToken() + " regarding plugin " + req.getPluginID());
                return;
            }

            publisherPublish(pluginTopic, newBytes);
        } else if (desc.serverSide()) {
            String modelDirectory = req.getModelDirectory() == null ? "" : req.getModelDirectory();
            HashMap<String, Serializable> paramsMap = req.getParams() == null
                    ? new HashMap<String, Serializable>()
                    : req.getParams();
            byte[] paramsMapBytes = serialize(paramsMap, "Error serializing parameters to send to server to instantiate plugin " + req.getPluginID());
            if (paramsMapBytes == null) {
                instantiateError(req, "Error serializing parameters to send to server to instantiate plugin " + req.getPluginID());
                return;
            }

            MXFInstantiateParams params = MXFInstantiateParams.newBuilder()
                    .setPluginID(req.getPluginID())
                    .setModelDirectory(modelDirectory)
                    .setModelFilename(req.getModelFilename())
                    .setToken(req.getToken())
                    .setParams(ByteString.copyFrom(paramsMapBytes))
                    .build();

            StreamObserver<MXFAck> cb = new StreamObserver<MXFAck>() {
                @Override
                public void onNext(MXFAck ack) {
                    if (ack.getSuccess()) {
                        instanceIDToPluginID.put(ack.getInstanceID(), req.getPluginID());
                        instanceChangedCallback.instanceChangeOccurred(req.getPluginID(),
                                ack.getInstanceID(), ChangeType.ADDED);
                        MXInstantiateReply reply = new MXInstantiateReply(req.getToken(),
                                ack.getInstanceID(), req.getPluginID(),
                                true,  "Successfully instantiated plugin");
                        byte[] bytes = serialize(reply, "Error serializing successful reply for token " + req.getToken() + " regarding plugin " + req.getPluginID());
                        if (bytes == null)
                            return;
                        publisherPublish(req.getToken(), bytes);
                    } else {
                        instantiateError(req, "gRPC call to " + remoteServIP + ":" + remoteServPort + " to instantiate plugin " + req.getPluginID() + " was unsuccessful: " + ack.getMsg());
                    }
                }

                @Override
                public void onError(Throwable t) {
                    instantiateError(req, "gRPC call to " + remoteServIP + ":" + remoteServPort + " to instantiate plugin " + req.getPluginID() + " was unsuccessful: " + t);
                }

                @Override
                public void onCompleted() {
                    return;
                }
            };

            /* Send to MXP on server. */
            asyncStub.mxfInstantiate(params, cb);
        }
    }

    private void executeError(MXExecuteRequest req, String msg) {
        Log.e(TAG, msg);
        MXExecuteReply reply = new MXExecuteReply(req.getExecuteID(),
                null, false, msg);
        byte[] bytes = serialize(reply, "Error serializing error reply for execution " + req.getExecuteID() + " regarding plugin " + req.getPluginID());
        if (bytes == null)
            return;
        publisherPublish(req.getExecuteID(), bytes);
    }

    public void execute(byte[] bytes) {
        Log.w(TAG, "Called execute()");
        MXExecuteRequest req = (MXExecuteRequest)deserialize(bytes);

        String mxpInstanceID = req.getMxpInstanceID();
        if (mxpInstanceID == null) {
            executeError(req, "Execution request for plugin sent with null instance ID");
            return;
        }

        String pluginID = this.instanceIDToPluginID.get(mxpInstanceID);
        if (pluginID == null) {
            executeError(req, "Plugin for instance " + req.getMxpInstanceID() + " not found");
            return;
        }

        MXPluginDescription desc = this.pluginIDToPluginDesc.get(pluginID);
        if (desc == null) {
            executeError(req, "Plugin description for plugin " + pluginID + " for instance " + req.getMxpInstanceID() + " not found");
            return;
        }

        if (desc.clientSide()) {
            /* Send to MXP. */
            String mxpTopic = this.pluginIDToPluginTopic.get(pluginID);
            if (mxpTopic == null) {
                executeError(req, "Local plugin with ID " + pluginID + " has no registered callback topic");
                return;
            }
            publisherPublish(mxpTopic, bytes);
        } else if (desc.serverSide()) {
            MXFExecuteParams mxfe = MXFExecuteParams.newBuilder()
                    .setInstanceID(req.getMxpInstanceID())
                    .setData(ByteString.copyFrom(req.getInputData()))
                    .build();

            StreamObserver<MXFPrediction> cb = new StreamObserver<MXFPrediction>() {
                @Override
                public void onNext(MXFPrediction prediction) {
                    if (!prediction.getInstanceID().equals(req.getMxpInstanceID())) {
                        executeError(req, "gRPC call to " + remoteServIP + ":" + remoteServPort + " to instantiate plugin " + req.getMxpInstanceID() + " returned a prediction for the wrong instance ID: " + prediction.getInstanceID());
                        return;
                    }
                    if (prediction.getSuccess()) {
                        MXExecuteReply reply = new MXExecuteReply(req.getExecuteID(), prediction.getData().toByteArray(),
                                true, "Successfully executed prediction");
                        byte[] bytes = serialize(reply, "Error serializing successful reply for execution " + req.getExecuteID() + " regarding plugin " + req.getPluginID());
                        if (bytes == null)
                            return;
                        publisherPublish(req.getExecuteID(), bytes);
                    } else {
                        executeError(req, "gRPC call to " + remoteServIP + ":" + remoteServPort + " to execute prediction for plugin " + pluginID + " was unsuccessful: " + prediction.getMsg());
                    }
                }

                @Override
                public void onError(Throwable t) {
                    executeError(req, "gRPC call to " + remoteServIP + ":" + remoteServPort + " to execute prediction for plugin " + pluginID + " was unsuccessful: " + t);
                }

                @Override
                public void onCompleted() {
                    return;
                }
            };

            /* Send to MXP on server. */
            asyncStub.mxfExecute(mxfe, cb);
        }
    }

    public void destroy(byte[] bytes) {
        Log.w(TAG, "Called destroy()");
        MXDestroyRequest req = (MXDestroyRequest)deserialize(bytes);

        String pluginID = this.instanceIDToPluginID.get(req.getMxpInstanceID());
        if (pluginID == null) {
            Log.e(TAG, "Plugin for instance " + req.getMxpInstanceID() + " not found");
            return;
        }

        MXPluginDescription desc = this.pluginIDToPluginDesc.get(pluginID);
        if (desc == null) {
            Log.e(TAG, "Plugin with ID " + pluginID + " for instance " + req.getMxpInstanceID() + " not found");
            return;
        }

        if (desc.clientSide()) {
            String pluginTopic = this.pluginIDToPluginTopic.get(pluginID);
            if (pluginTopic == null) {
                Log.e(TAG, "Local plugin with ID " + pluginID + " has no registered callback topic");
                return;
            }

            this.instanceIDToPluginID.remove(req.getMxpInstanceID());

            /* Send to MXP. */
            publisherPublish(pluginTopic, bytes);
            this.instanceChangedCallback.instanceChangeOccurred(req.getPluginID(),
                    req.getMxpInstanceID(), ChangeType.REMOVED);
        } else if (desc.serverSide()) {
            MXFDestroyParams params = MXFDestroyParams.newBuilder()
                    .setInstanceID(req.getMxpInstanceID())
                    .build();

            StreamObserver<MXFAck> cb = new StreamObserver<MXFAck>() {
                @Override
                public void onNext(MXFAck ack) {
                    if (!ack.getInstanceID().equals(req.getMxpInstanceID())) {
                        Log.e(TAG, "gRPC call to " + remoteServIP + ":" + remoteServPort + " to destroy instance " + req.getMxpInstanceID() + " returned an ack for the wrong ID: " + ack.getInstanceID());
                        return;
                    }
                    if (ack.getSuccess()) {
                        instanceIDToPluginID.remove(req.getMxpInstanceID());
                        instanceChangedCallback.instanceChangeOccurred(req.getPluginID(),
                                req.getMxpInstanceID(), ChangeType.REMOVED);
                    } else
                        Log.e(TAG, "gRPC call to " + remoteServIP + ":" + remoteServPort + " to destroy instance " + ack.getInstanceID() + " was unsuccessful: " + ack.getMsg());
                }

                @Override
                public void onError(Throwable t) {
                    Log.e(TAG, "Error issuing gRPC call to " + remoteServIP + ":" + remoteServPort + " to destroy instance " + req.getMxpInstanceID() + " of plugin " + pluginID + ": " + t);
                }

                @Override
                public void onCompleted() {
                    return;
                }
            };

            /* Send to MXP on server. */
            asyncStub.mxfDestroy(params, cb);
        }
    }

    public void register(byte[] bytes) {
        Log.w(TAG, "Called register()");
        MXRegisterRequest req = (MXRegisterRequest)deserialize(bytes);
        if (this.pluginIDToPluginTopic.put(req.getPluginID(), req.getTopic()) != null)
            mxPluginChangedCallback.mxPluginChangeOccurred(req.getPluginDescription(), ChangeType.CHANGED);
        else
            mxPluginChangedCallback.mxPluginChangeOccurred(req.getPluginDescription(), ChangeType.ADDED);
        this.pluginIDToPluginDesc.put(req.getPluginID(), req.getPluginDescription());
        listResources();
    }

    public void deregister(byte[] bytes) {
        Log.w(TAG, "Called deregister()");
        MXDeregisterRequest req = (MXDeregisterRequest)deserialize(bytes);
        MXPluginDescription desc = this.pluginIDToPluginDesc.get(req.getPluginID());
        this.pluginIDToPluginTopic.remove(req.getPluginID());
        if (desc != null)
            this.mxPluginChangedCallback.mxPluginChangeOccurred(desc, ChangeType.REMOVED);
        this.pluginIDToPluginDesc.remove(req.getPluginID());
        listResources();
    }

    private MXPluginDescription buildMXPluginDescription(MXFPluginDescription d) {
        Map<String, Object> descParams = new HashMap<String, Object>();
        descParams.put("id", d.getId());
        descParams.put("name", d.getName());
        descParams.put("author", d.getAuthor());
        descParams.put("library", d.getLibrary());
        descParams.put("algorithm", d.getAlgorithm());
        descParams.put("version", d.getVersion());
        descParams.put("clientSide", d.getClientSide());
        descParams.put("serverSide", d.getServerSide());
        descParams.put("description", d.getDescription());

        MXPluginDescription desc = null;
        try {
            desc = TypeFactory.annotation(MXPluginDescription.class, descParams);
        } catch (AnnotationFormatException e) {
            Log.w(TAG, "Could not build MXPluginDescription for plugin listed from server");
        }
        return desc;
    }

    private void removeAllPlugins() {
        Set<String> pluginIDs = new HashSet<String>();
        for (MXPluginDescription desc : this.pluginIDToPluginDesc.values()) {
            pluginIDs.add(desc.id());
        }

        for (String pluginID : pluginIDs) {
            /* Not every pluginID will have a topic (server plugins won't). */
            this.pluginIDToPluginTopic.remove(pluginID);
            this.mxPluginChangedCallback.mxPluginChangeOccurred(
                    this.pluginIDToPluginDesc.get(pluginID),
                    ChangeType.REMOVED);
            this.pluginIDToPluginDesc.remove(pluginID);
        }
    }

    public void refreshClientPlugins() {
        publisherPublish(MX_PLUGIN_COMMAND_PREFIX + MX_REFRESH, new byte[0]);
    }

    public void refreshServerPlugins() {
        // Collect list of server plugins that we previously had.
        Set<String> serverPluginIDs = new HashSet<String>();
        for (MXPluginDescription desc : this.pluginIDToPluginDesc.values()) {
            if (desc.serverSide())
                serverPluginIDs.add(desc.id());
        }

        StreamObserver<MXFPluginList> cb = new StreamObserver<MXFPluginList>() {
            @Override
            public void onNext(MXFPluginList list) {
                if (list.getSuccess()) {
                    for (MXFPluginDescription d : list.getPluginsList()) {
                        MXPluginDescription desc = buildMXPluginDescription(d);
                        if (d == null)
                            continue;

                        if (pluginIDToPluginDesc.put(desc.id(), desc) != null)
                            mxPluginChangedCallback.mxPluginChangeOccurred(desc, ChangeType.CHANGED);
                        else
                            mxPluginChangedCallback.mxPluginChangeOccurred(desc, ChangeType.ADDED);

                        // Remove this plugin ID from the list we're checking.
                        serverPluginIDs.remove(desc.id());
                    }
                } else {
                    Log.e(TAG, "gRPC call to " + remoteServIP + ":" + remoteServPort + " to list plugins was unsuccessful, removing all plugins: " + list.getMsg());
                }

                // Remove these plugin IDs, since they weren't in the update.
                for (String pluginID : serverPluginIDs) {
                    mxPluginChangedCallback.mxPluginChangeOccurred(pluginIDToPluginDesc.get(pluginID),
                            ChangeType.REMOVED);
                    pluginIDToPluginDesc.remove(pluginID);
                }
            }

            @Override
            public void onError(Throwable t) {
                Log.e(TAG, "Error issuing gRPC call to " + remoteServIP + ":" + remoteServPort + " to list plugins, removing all plugins: " + t);
                // Remove these plugin IDs, since there was an error.
                for (String pluginID : serverPluginIDs) {
                    mxPluginChangedCallback.mxPluginChangeOccurred(pluginIDToPluginDesc.get(pluginID),
                            ChangeType.REMOVED);
                    pluginIDToPluginDesc.remove(pluginID);
                }
                listResources();
            }

            @Override
            public void onCompleted() {
                listResources();
            }
        };

        /* Send to MXF on server. */
        asyncStub.mxfListPlugins(Empty.newBuilder().build(), cb);
    }

    private void removeAllModels() {
        Set<String> modelLabels = new HashSet<String>();
        for (String modelLabel : this.modelLabelToModel.keySet()) {
            modelLabels.add(modelLabel);
        }

        for (String label : modelLabels) {
            this.modelChangedCallback.modelChangeOccurred(label,
                    this.modelLabelToModel.get(label), ChangeType.REMOVED);
            this.modelLabelToModel.remove(label);
        }
    }

    public void refreshClientModels() {
        // Collect list of server models that we previously had.
        Set<String> clientModelLabels = new HashSet<String>();
        for (String modelLabel : this.modelLabelToModel.keySet()) {
            Model m = this.modelLabelToModel.get(modelLabel);
            if (m.getSource().equals("Client"))
                clientModelLabels.add(modelLabel);
        }

        File[] files = this.defaultModelsDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return !name.endsWith(".json");
            }
        });

        for (File f : files) {
            String modelName = f.getName();
            File metadataDir = new File(f.getParentFile().getParentFile(), "metadata");
            int extIndex = modelName.lastIndexOf(".");
            File metadataFile = new File(metadataDir, modelName.substring(0, extIndex + 1) + "json");

            String modelLabel = f.getName() + " (Client)";
            clientModelLabels.remove(modelLabel);

            String metadata = "";
            if (!metadataFile.exists()) {
                Log.i(TAG, "No metadata file (in JSON) for model " + f.getName());
            } else {
                byte[] fileContents = null;
                try {
                    fileContents = Files.readAllBytes(metadataFile.toPath());
                    metadata = new String(fileContents, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    Log.i(TAG, "Error reading metadata file for model " + f.getName() + ": " + e);
                }
            }

            Model m = Model.newBuilder()
                    .setName(f.getName())
                    .setSource("Client")
                    .setMetadata(metadata)
                    .build();
            if (this.modelLabelToModel.put(modelLabel, m) != null) {
                this.modelChangedCallback.modelChangeOccurred(modelLabel, m, ChangeType.CHANGED);
            } else {
                this.modelChangedCallback.modelChangeOccurred(modelLabel, m, ChangeType.ADDED);
            }
        }

        // Remove these models, since we did not find them.
        for (String modelLabel : clientModelLabels) {
            this.modelChangedCallback.modelChangeOccurred(modelLabel,
                    this.modelLabelToModel.get(modelLabel), ChangeType.REMOVED);
            this.modelLabelToModel.remove(modelLabel);
        }
    }

    public void refreshServerModels() {
        // Collect list of server models that we previously had.
        Set<String> serverModelLabels = new HashSet<String>();
        for (String modelLabel : this.modelLabelToModel.keySet()) {
            Model m = this.modelLabelToModel.get(modelLabel);
            if (m.getSource().equals("Server"))
                serverModelLabels.add(modelLabel);
        }

        StreamObserver<ModelList> cb = new StreamObserver<ModelList>() {
            @Override
            public void onNext(ModelList list) {
                if (list.getSuccess()) {
                    for (Model m : list.getModelsList()) {
                        String modelLabel = m.getName() + " (" + m.getSource() + ")";
                        if (modelLabelToModel.put(modelLabel, m) != null)
                            modelChangedCallback.modelChangeOccurred(modelLabel, m, ChangeType.CHANGED);
                        else
                            modelChangedCallback.modelChangeOccurred(modelLabel, m, ChangeType.ADDED);

                        // Remove this model from the list we're checking.
                        serverModelLabels.remove(modelLabel);
                    }
                } else {
                    Log.e(TAG, "gRPC call to " + remoteServIP + ":" + remoteServPort + " to list models was unsuccessful, removing all models: " + list.getMsg());
                }

                // Remove these models, since they weren't in the update.
                for (String modelLabel : serverModelLabels) {
                    modelChangedCallback.modelChangeOccurred(modelLabel,
                            modelLabelToModel.get(modelLabel), ChangeType.REMOVED);
                    modelLabelToModel.remove(modelLabel);
                }
            }

            @Override
            public void onError(Throwable t) {
                Log.e(TAG, "Error issuing gRPC call to " + remoteServIP + ":" + remoteServPort + " to list models, removing all models: " + t);
                // Remove these models, since there was an error.
                for (String modelLabel : serverModelLabels) {
                    modelChangedCallback.modelChangeOccurred(modelLabel,
                            modelLabelToModel.get(modelLabel), ChangeType.REMOVED);
                    modelLabelToModel.remove(modelLabel);
                }
                listResources();
            }

            @Override
            public void onCompleted() {
                listResources();
            }
        };

        /* Send to MXF on server. */
        asyncStub.mxfListModels(Empty.newBuilder().build(), cb);
    }

    public void listResources() {
        Set<String> pluginLabels = new HashSet<String>();
        for (MXPluginDescription desc : this.pluginIDToPluginDesc.values()) {
            String pluginLabel = desc.name() + " (" + desc.id() + ")";
            pluginLabels.add(pluginLabel);
        }

        Set<String> modelLabels = new HashSet<String>();
        for (String modelLabel : this.modelLabelToModel.keySet()) {
            modelLabels.add(modelLabel);
        }

        MXListResourcesReply reply = new MXListResourcesReply(pluginLabels, modelLabels);
        byte[] output = serialize(reply, "Error serializing successful reply for listing resources");
        if (output == null)
            return;
        publisherPublish(MX_LIST_RESOURCES_REPLY, output);
    }

    public void pullModel(String modelName) {
        StreamObserver<ModelFile> cb = new StreamObserver<ModelFile>() {
            @Override
            public void onNext(ModelFile mf) {
                if (!mf.getSuccess()) {
                    Log.e(TAG, "gRPC call to " + remoteServIP + ":" + remoteServPort + " to pull model was unsuccessful" + mf.getMsg());
                    return;
                }

                Model m = mf.getModel();
                if (m.getName().equals("")) {
                    Log.e(TAG, "gRPC call to " + remoteServIP + ":" + remoteServPort + " to pull model came back without a name");
                    return;
                }

                File modelFile = new File(defaultModelsDirectory, m.getName());
                FileOutputStream out;
                try {
                    out = new FileOutputStream(modelFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Cannot pull server model to location " + modelFile);
                    return;
                }
                try {
                    out.write(mf.getContents().toByteArray());
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Cannot write pulled server model to location " + modelFile);
                    return;
                }

                int extIndex = m.getName().lastIndexOf(".");
                String modelMetadataFilename = m.getName().substring(0, extIndex + 1) + "json";
                File modelMetadataFile = new File(defaultModelsDirectory, modelMetadataFilename);
                try {
                    out = new FileOutputStream(modelMetadataFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Cannot pull server model metadata to location " + modelMetadataFile);
                    return;
                }
                try {
                    out.write(m.getMetadata().getBytes());
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Cannot write pulled server model metadata to location " + modelMetadataFile);
                    return;
                }

                String modelLabel = m.getName() + " (Client)";
                if (modelLabelToModel.put(modelLabel, m) != null)
                    modelChangedCallback.modelChangeOccurred(modelLabel, m, ChangeType.CHANGED);
                else
                    modelChangedCallback.modelChangeOccurred(modelLabel, m, ChangeType.ADDED);
            }

            @Override
            public void onError(Throwable t) {
                Log.e(TAG, "Error issuing gRPC call to " + remoteServIP + ":" + remoteServPort + " to pull model: " + t);
            }

            @Override
            public void onCompleted() {
                listResources();
            }
        };

        /* Send to MXF on server. */
        asyncStub.mxfPullModel(ModelName.newBuilder().setName(modelName).build(), cb);
    }
}
