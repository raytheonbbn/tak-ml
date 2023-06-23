package com.bbn.takml_sdk_android.mx_framework;

import com.bbn.tak.ml.mx_framework.MXFrameworkRegistrar;
import com.bbn.tak.ml.mx_framework.MXPlugin;
import com.bbn.tak.ml.mx_framework.MXPluginDescription;
import com.bbn.takml_sdk_android.mx_framework.request.*;
import static com.bbn.tak.ml.TakMlConstants.*;

import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.atakmap.coremap.log.Log;

 /**
 * Library for MX plugin developers to register their plugins with TAK-ML.
 * <p>
 * Since MX plugins are separated from the MX framework and run in separate
 * processes, the MX plugins communicate with the MX framework using the
 * MQTT messaging protocol. The <code>MXFrameworkAndroidRegistrar</code>
 * allows client-side (Android) MX plugins to register and deregister with
 * the MX framework using MQTT.
 * <p>
 * The <code>MXFrameworkAndroidRegistrar</code> will automatically
 * reconnect to the MX framework MQTT server broker upon disconnection.
 * <p>
 * Users of this class must invoke {@link #stop()} when finished in order
 * to properly close the MQTT connection to the MX framework.
 */
public class MXFrameworkAndroidRegistrar implements MXFrameworkRegistrar {

    private static final int RECONNECT_INTERVAL_MS = 1000;

    private String TAG;
    private String clientID;
    private MqttAsyncClient client;
    private Map<String, Class<MXPlugin>> topicToPluginClass;
    private Map<String, HashMap<String, MXPlugin>> pluginIDToInstances;

    private Timer reconnectTimer;

    private class ReconnectTask extends TimerTask {
        @Override
        public void run() {
            connect();
        }
    }

    /* TODO move to shared library (also used in MXFramework). */

    private byte[] serialize(Object o, String msg) {
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

    private boolean registerPluginMQTT(MXPluginDescription desc, String topic) {
        try {
            this.client.subscribe(topic, 1, this::messageArrived);
        } catch (MqttException e) {
            Log.e(TAG, "Failed to subscribe to topic for plugin");
            e.printStackTrace();
            return false;
        }

        MXRegisterRequest req = new MXRegisterRequest(desc, topic);

        try {
            // Serialize execution request.
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(req);
            byte[] serialized = bos.toByteArray();
            out.close();
            bos.close();

            // Send MQTT message.
            MqttMessage msg = new MqttMessage();
            msg.setPayload(serialized);
            this.client.publish(MX_REGISTER, msg);
        } catch (MqttException | IOException e) {
            Log.e(TAG, "Failed to send register request to framework");
            e.printStackTrace();
            try {
                this.client.unsubscribe(topic);
            } catch (MqttException e2) {
                e2.printStackTrace();
            }
            return false;
        }

        return true;
    }

    @Override
    public boolean register(Class<?> mxpClass) {
        if (!this.client.isConnected()) {
            Log.e(TAG, "Client was not connected, register attempt failed.");
            return false;
        }

        MXPluginDescription desc = mxpClass.getAnnotation(MXPluginDescription.class);
        if (desc == null) {
            Log.e(TAG, "Classes implementing MXPlugin (" + mxpClass + ") need to use MXPluginDescription annotation");
            return false;
        }

        String topic = MX_PLUGIN_COMMAND_PREFIX + desc.id();
        boolean registered = registerPluginMQTT(desc, topic);
        if (registered) {
            this.topicToPluginClass.put(topic, (Class<MXPlugin>)mxpClass);
            HashMap<String, MXPlugin> instances = new HashMap<String, MXPlugin>();
            this.pluginIDToInstances.put(desc.id(), instances);
        }
        return registered;
    }

    private boolean deregisterPluginMQTT(String pluginID, String topic) {
        try {
            this.client.unsubscribe(topic);
        } catch (MqttException e) {
            Log.e(TAG, "Failed to unsubscribe to topic for plugin");
            e.printStackTrace();
            return false;
        }

        MXDeregisterRequest req = new MXDeregisterRequest(pluginID);

        try {
            // Serialize execution request.
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(req);
            byte[] serialized = bos.toByteArray();
            out.close();
            bos.close();

            // Send MQTT message.
            MqttMessage msg = new MqttMessage();
            msg.setPayload(serialized);
            this.client.publish(MX_DEREGISTER, msg);
        } catch (MqttException | IOException e) {
            Log.e(TAG, "Failed to send deregister request to framework");
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public boolean deregister(Class<?> mxpClass) {
        if (!this.client.isConnected())
            return false;

        MXPluginDescription desc = mxpClass.getAnnotation(MXPluginDescription.class);
        if (desc == null) {
            Log.e(TAG, "Classes implementing MXPlugin (" + mxpClass + ") need to use MXPluginDescription annotation");
            return false;
        }

        String topic = MX_PLUGIN_COMMAND_PREFIX + desc.id();
        boolean deregistered = deregisterPluginMQTT(desc.id(), topic);
        Map<String, MXPlugin> instances = this.pluginIDToInstances.get(desc.id());
        for (MXPlugin mxp : instances.values())
            mxp.destroy();
        this.pluginIDToInstances.remove(desc.id());
        this.topicToPluginClass.remove(topic);
        return deregistered;
    }

    private void refreshArrived(String topic, MqttMessage message) throws Exception {
        Class<MXPlugin> mxpClass = this.topicToPluginClass.get(topic);
        if (mxpClass == null)
            return;

        MXPluginDescription desc = mxpClass.getAnnotation(MXPluginDescription.class);
        if (desc == null) {
            Log.e(TAG, "Classes implementing MXPlugin (" + mxpClass + ") need to use MXPluginDescription annotation");
            return;
        }

        if (!registerPluginMQTT(desc, topic)) {
            Log.e(TAG, "Unable to re-register plugin with ID " + desc.id());
        }
    }

    private void messageArrived(String topic, MqttMessage message) throws Exception {
        Class<MXPlugin> mxpClass = this.topicToPluginClass.get(topic);
        if (topic == null)
            return;

        MXRequest req = (MXRequest)deserialize(message.getPayload());
        switch (req.getType()) {
            case INSTANTIATE: {
                MXInstantiateRequest ireq = (MXInstantiateRequest)req;
                MXPlugin mxp = mxpClass.newInstance();
                Map<String, MXPlugin> instances = this.pluginIDToInstances.get(ireq.getPluginID());
                String replyMsg;
                boolean success = false;

                if (instances == null) {
                    replyMsg = "Could not find plugin with ID " + ireq.getPluginID() + " to instantiate";
                } else {
                    success = mxp.instantiate(ireq.getModelDirectory(), ireq.getModelFilename(),
                            ireq.getParams());
                    if (success) {
                        instances.put(ireq.getMxpInstanceID(), mxp);
                        replyMsg = "Successfully instantiated plugin " + ireq.getPluginID();
                    } else
                        replyMsg = "Could not instantiate plugin with ID " + ireq.getPluginID() + " with model " + ireq.getModelFilename();
                }

                if (!success) {
                    Log.e(TAG, replyMsg);
                }

                /*
                 * Construct reply here instead of in framework so
                 * that we can respond to the application directly.
                 */

                MXInstantiateReply reply = new MXInstantiateReply(ireq.getToken(),
                        ireq.getMxpInstanceID(), ireq.getPluginID(), success, replyMsg);
                byte[] bytes = serialize(reply, "Error serializing successful reply for token " + ireq.getToken() + " for instance " + ireq.getMxpInstanceID() + " regarding plugin " + ireq.getPluginID());
                if (bytes == null)
                    return;

                try {
                    MqttMessage msg = new MqttMessage();
                    msg.setPayload(bytes);
                    this.client.publish(ireq.getToken(), msg);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                break;
            }
            case EXECUTE: {
                MXExecuteRequest ereq = (MXExecuteRequest) req;
                Map<String, MXPlugin> instances = this.pluginIDToInstances.get(ereq.getPluginID());
                String replyMsg;
                byte[] output = null;
                if (instances == null) {
                    replyMsg = "Could not find plugin with ID " + ereq.getPluginID() + " to instantiate";
                } else {
                    MXPlugin mxp = instances.get(ereq.getMxpInstanceID());
                    if (mxp == null) {
                        replyMsg = "Could not find instance " + ereq.getMxpInstanceID() + " for plugin with ID " + ereq.getPluginID() + " to instantiate";
                    } else {
                        output = mxp.execute(ereq.getInputData());
                        replyMsg = output == null
                                ? "Error executing prediction"
                                : "Successfully executed prediction";
                    }
                }

                if (output == null) {
                    Log.e(TAG, replyMsg);
                }

                /*
                 * Construct reply here instead of in framework so
                 * that we can respond to the application directly.
                 */

                MXExecuteReply reply = new MXExecuteReply(ereq.getExecuteID(), output,
                        output != null, replyMsg);

                byte[] bytes = serialize(reply, "Error serializing successful reply for execution " + ereq.getExecuteID() + " regarding plugin " + ereq.getPluginID());
                if (bytes == null) {
                    return;
                }

                try {
                    MqttMessage msg = new MqttMessage();
                    msg.setPayload(bytes);
                    this.client.publish(ereq.getExecuteID(), msg);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                break;
            }
            case DESTROY: {
                MXDestroyRequest dreq = (MXDestroyRequest)req;
                Map<String, MXPlugin> instances = this.pluginIDToInstances.get(dreq.getPluginID());
                if (instances == null) {
                    Log.e(TAG, "Could not find plugin with ID " + dreq.getPluginID() + " to destroy");
                } else {
                    MXPlugin mxp = instances.get(dreq.getMxpInstanceID());
                    if (mxp == null) {
                        Log.e(TAG, "Could not find instance " + dreq.getMxpInstanceID() +" for plugin with ID " + dreq.getPluginID() + " to destroy");
                    } else {
                        boolean success = mxp.destroy();
                        if (!success) {
                            Log.e(TAG, "Unable to destroy instance " + dreq.getMxpInstanceID() + " for plugin with ID " + dreq.getPluginID());
                        }
                    }
                }
                break;
            }
            default:
                Log.e(TAG, "Received invalid message type (" + req.getType() + ")");
                return;
        }

        return;
    }

    /**
     * Create a new <code>MXFrameworkAndroidRegistrar</code> object to aid in MX framework operations.
     *
     * @param clientID a String uniquely identifying the caller as an MQTT client.
     */
    public MXFrameworkAndroidRegistrar(String clientID) {
        this.TAG = clientID;
        this.clientID = clientID;
        this.topicToPluginClass = new HashMap<String, Class<MXPlugin>>();
        this.pluginIDToInstances = new HashMap<String, HashMap<String, MXPlugin>>();

        reconnectTimer = new Timer();

        if (this.clientID.length() > 23)
            this.clientID = this.clientID.substring(0, 23);

        connect();
    }

    private void connect() {
        String takmlListenerAddr = "tcp://" + TAK_ML_LISTENER_DEFAULT_HOST + ":" + TAK_ML_LISTENER_DEFAULT_PORT;
        MqttConnectOptions connectOptions = new MqttConnectOptions();
        connectOptions.setAutomaticReconnect(true);
        connectOptions.setCleanSession(true);

        try {
            this.client = new MqttAsyncClient(takmlListenerAddr, this.clientID, null);
            this.client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.e(TAG, "Lost connection to server broker");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    return;
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    return;
                }

                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    if (!serverURI.equals(takmlListenerAddr)) {
                        Log.e(TAG, "Client " + clientID + " connected to server at " + serverURI + " instead of " + takmlListenerAddr);
                        return;
                    }

                    try {
                        client.subscribe(MX_PLUGIN_COMMAND_PREFIX + MX_REFRESH, 1,
                                MXFrameworkAndroidRegistrar.this::refreshArrived);
                    } catch (MqttException e) {
                        Log.e(TAG, "Failed to subscribe for refresh commands");
                        e.printStackTrace();
                    }

                    for (Map.Entry<String, Class<MXPlugin>> entry : topicToPluginClass.entrySet()) {
                        String topic = entry.getKey();
                        Class<MXPlugin> mxpClass = entry.getValue();

                        MXPluginDescription desc = mxpClass.getAnnotation(MXPluginDescription.class);
                        if (desc == null) {
                            Log.e(TAG, "Classes implementing MXPlugin (" + mxpClass + ") need to use MXPluginDescription annotation");
                            continue;
                        }

                        registerPluginMQTT(desc, topic);
                    }
                }
            });
            this.client.connect(connectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken token) {
                    try {
                        client.subscribe(MX_PLUGIN_COMMAND_PREFIX + MX_REFRESH, 1,
                                MXFrameworkAndroidRegistrar.this::refreshArrived);
                    } catch (MqttException e) {
                        Log.e(TAG, "Failed to subscribe for refresh commands");
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Could not connect to server broker");
                    reconnectTimer.schedule(new ReconnectTask(), RECONNECT_INTERVAL_MS);
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
            Log.e(TAG, "Unable to connect MQTT client to server");
        }
    }

    @Override
    public void stop() {
        for (Class<MXPlugin> mxpClass : this.topicToPluginClass.values())
            deregister(mxpClass);

        try {
            this.client.unsubscribe(MX_PLUGIN_COMMAND_PREFIX + MX_REFRESH);
            this.client.disconnect();
            this.client.close();
        } catch (MqttException e) {
            Log.e(TAG, "Could not disconnect client");
            e.printStackTrace();
        }
    }
}
