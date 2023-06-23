package com.bbn.takml_sdk_android.mx_framework;

import static com.bbn.tak.ml.TakMlConstants.*;

import com.bbn.takml_sdk_android.TAKMLAppDataDirectoryQueryCallback;
import com.bbn.takml_sdk_android.TAKMLAppDataDirectoryResourcesListQueryCallback;
import com.bbn.takml_sdk_android.TAKMLAppDataDirectoryResourcesListReply;
import com.bbn.takml_sdk_android.TAKMLFrameworkConnectionStatusCallback;
import com.bbn.takml_sdk_android.mx_framework.MXInstantiateModelCallback;
import com.bbn.takml_sdk_android.mx_framework.MXExecuteModelCallback;
import com.bbn.takml_sdk_android.mx_framework.MXListResourcesCallback;
import com.bbn.tak.ml.sensor.SensorListQueryCallback;
import com.bbn.takml_sdk_android.mx_framework.request.*;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import com.atakmap.coremap.log.Log;
import com.bbn.tak.ml.sensor.info.SensorList;
import com.bbn.tak.ml.sensor.request.SFQuerySensorList;
import com.bbn.tak.ml.sensor.request.SFReadStartRequest;
import com.bbn.tak.ml.sensor.request.SFReadStopRequest;

import de.fraunhofer.iosb.ilt.frostserver.json.deserialize.EntityParser;
import de.fraunhofer.iosb.ilt.frostserver.model.Sensor;
import de.fraunhofer.iosb.ilt.frostserver.model.core.IdString;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Library for application developers to make machine learning predictions using TAK-ML.
 */
public class PredictionExecutor {

    private final int DEFAULT_QOS = 1;

    private final int RECONNECT_INTERVAL_MS = 1000;

    private MqttAsyncClient client;
    private String takmlListenerAddr;
    private String clientID;
    private String TAG;
    private HashMap<String, String> instanceIDToPluginID;
    private Map<String, MXInstantiateModelCallback> instantiateCallbacks;
    private Map<String, MXExecuteModelCallback> executeCallbacks;
    private ReentrantLock lock;

    private ReentrantLock resourcesListLock;
    private MXListResourcesCallback listCB;
    private boolean requestResources;

    private TAKMLFrameworkConnectionStatusCallback frameworkConnectionStatusCallback;

    private SensorListQueryCallback sensorListQueryCallback;
    private EntityParser parser;

    private TAKMLAppDataDirectoryQueryCallback appDataDirectoryQueryCallback;
    private TAKMLAppDataDirectoryResourcesListQueryCallback appDataDirectoryResourcesListQueryCallback;

    private Timer reconnectTimer;

    private class ReconnectTask extends TimerTask {
        @Override
        public void run() {
            connect();
        }
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

    /**
     * Create a new <code>PredictionExecutor</code>.
     *
     * @param takmlListenerAddr the URL of the TAK-ML listener, in the format "tcp://host:port"
     * @param clientID a String uniquely identifying the caller as an MQTT client.
     * @throws IOException
     */
    public PredictionExecutor(String takmlListenerAddr, String clientID)
            throws IOException {
        this.TAG = clientID + "-prediction-executor";
        this.clientID = clientID;
        if (this.clientID.length() > 23)
            this.clientID = this.clientID.substring(0, 23);
        this.takmlListenerAddr = takmlListenerAddr;
        this.instantiateCallbacks = new HashMap<String, MXInstantiateModelCallback>();
        this.executeCallbacks = new HashMap<String, MXExecuteModelCallback>();
        this.instanceIDToPluginID = new HashMap<String, String>();
        this.lock = new ReentrantLock();
        this.resourcesListLock = new ReentrantLock();
        this.requestResources = false;
        this.parser = new EntityParser(IdString.class);
        this.reconnectTimer = new Timer();
        connect();
    }

    /**
     * Create a new <code>PredictionExecutor</code>.
     *
     * @param clientID a String uniquely identifying the caller as an MQTT client.
     * @throws IOException
     */
    public PredictionExecutor(String clientID) throws IOException {
        this("tcp://" + TAK_ML_LISTENER_DEFAULT_HOST + ":" + TAK_ML_LISTENER_DEFAULT_PORT, clientID);
    }

    private void instantiateResponseArrived(String topic, MqttMessage message) throws Exception {
        String token = topic;
        this.lock.lock();
        if (!this.instantiateCallbacks.containsKey(token)) {
            this.lock.unlock();
            Log.e(TAG, "Received callback to instantiate model for invalid token " + token);
            return;
        }
        MXInstantiateModelCallback callback = this.instantiateCallbacks.get(token);
        this.instantiateCallbacks.remove(token);
        this.lock.unlock();
        this.client.unsubscribe(token);
        MXInstantiateReply reply = (MXInstantiateReply)deserialize(message.getPayload());
        if (reply.isSuccess()) {
            this.instanceIDToPluginID.put(reply.getMxpInstanceID(), reply.getPluginID());
        }
        callback.instantiateCB(reply);
    }

    private void executeResponseArrived(String topic, MqttMessage message) throws Exception {
        String executeID = topic;
        this.lock.lock();
        if (!this.executeCallbacks.containsKey(executeID)) {
            this.lock.unlock();
            Log.e(TAG, "Received callback to execute model for invalid execution ID " + executeID);
            return;
        }
        MXExecuteModelCallback callback = this.executeCallbacks.get(executeID);
        this.executeCallbacks.remove(executeID);
        this.lock.unlock();
        this.client.unsubscribe(executeID);
        MXExecuteReply reply = (MXExecuteReply)deserialize(message.getPayload());
        callback.executeCB(reply);
    }

    /**
     * Stop the <code>PredictionExecutor</code>.
     * <p>
     * Disconnects and closes the MQTT client.
     */
    public void stop() {
        try {
            this.client.disconnect();
            this.client.close();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * Whether the MQTT client is currently connected to the TAK-ML listener.
     * @return whether the MQTT client is currently connected to the TAK-ML listener.
     */
    public boolean isConnected() {
        return this.client.isConnected();
    }

    private void connect() {
        Log.d(TAG, "Calling connect...");
        MqttConnectOptions connectOptions = new MqttConnectOptions();
        connectOptions.setAutomaticReconnect(true);
        connectOptions.setCleanSession(true);
        connectOptions.setMaxInflight(55000);

        try {
            this.client = new MqttAsyncClient(this.takmlListenerAddr, this.clientID, null);
            this.client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.e(TAG, "Lost connection to server broker");
                    if (frameworkConnectionStatusCallback != null) {
                        frameworkConnectionStatusCallback.TAKMLFrameworkConnectionChanged(false);
                    }
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
                        Log.e(TAG, "Client for " + clientID + " connected to server at " + serverURI + " instead of " + takmlListenerAddr);
                        return;
                    }

                    lock.lock();

                    if (frameworkConnectionStatusCallback != null) {
                        frameworkConnectionStatusCallback.TAKMLFrameworkConnectionChanged(true);
                    }

                    for (String token : instantiateCallbacks.keySet()) {
                        try {
                            client.subscribe(token, DEFAULT_QOS, PredictionExecutor.this::instantiateResponseArrived);
                        } catch (MqttException e) {
                            e.printStackTrace();
                            Log.e(TAG, "Could not re-subscribe to token " + token);
                        }
                    }

                    for (String executeID : executeCallbacks.keySet()) {
                        try {
                            client.subscribe(executeID, DEFAULT_QOS, PredictionExecutor.this::executeResponseArrived);
                        } catch (MqttException e) {
                            e.printStackTrace();
                            Log.e(TAG, "Could not re-subscribe to execute ID " + executeID);
                        }
                    }
                    lock.unlock();

                    if (listCB != null && requestResources) {
                        requestResourcesList();
                    }

                    String sensorListTopic = SENSOR_LIST_QUERY_REPLY_PREFIX + clientID;

                    try {
                        client.subscribe(sensorListTopic, DEFAULT_QOS, PredictionExecutor.this::sensorListResponseArrived);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }

                    Log.d(TAG, "Subscribed to sensor list topic: " + sensorListTopic);
                }
            });
            this.client.connect(connectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Client successfully connected.");
                    if (listCB != null && requestResources) {
                        requestResourcesList();
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Failed to connect, retrying...");
                    reconnectTimer.schedule(new ReconnectTask(), RECONNECT_INTERVAL_MS);
                }
            });
            Log.d(TAG,"Finished connect call.");
        } catch (MqttException e) {
            e.printStackTrace();
            Log.e(TAG, "Could not connect client to server broker.");
        }
    }

    /**
     * Sets the callback for the connection status to the TAK-ML listener.
     *
     * @param cb the callback for the connection status to the TAK-ML listener.
     */
    public void setFrameworkConnectionStatusCallback(TAKMLFrameworkConnectionStatusCallback cb) {
        this.frameworkConnectionStatusCallback = cb;
    }

    /**
     * Sets the callback for replies to the sensor list query.
     *
     * @param sensorListQueryCallback the callback for replies to the sensor list query.
     */
    public void setSensorListQueryCallback(SensorListQueryCallback sensorListQueryCallback) {
        this.sensorListQueryCallback = sensorListQueryCallback;
    }

    /**
     * Sets the callback for listing MX framework resources.
     *
     * @param cb the callback for listing MX framework resources.
     */
    public void setMxListResourcesCallback(MXListResourcesCallback cb) {
        this.listCB = cb;
    }

    /**
     * Sets the callback for the TAKML Framework app data directory query.
     *
     * @param cb the callback for the TAKML Framework app data directory query.
     */
    public void setAppDataDirectoryQueryCallback(TAKMLAppDataDirectoryQueryCallback cb) {
        this.appDataDirectoryQueryCallback = cb;
    }

    /**
     * Sets the callback for the TAKML Framework app data directory resources list query.
     *
     * @param cb the callback for the TAKML Framework app data directory resources list query.
     */
    public void setAppDataDirectoryResourcesListQueryCallback(TAKMLAppDataDirectoryResourcesListQueryCallback cb) {
        this.appDataDirectoryResourcesListQueryCallback = cb;
    }

    /**
     * Instantiate an MX plugin.
     *
     * @param pluginID the unique identifier for a {@link com.bbn.tak.ml.mx_framework.MXPlugin}.
     * @param modelDirectory the directory for the model file.
     * @param modelFilename the filename for the model file.
     * @param params the dictionary of parameters used by the plugin.
     * @param cb the callback that will receive the reqply to this instantiation request.
     * @return
     */
    public String instantiatePlugin(String pluginID, String modelDirectory, String modelFilename,
                                    HashMap<String, Serializable> params,
                                    MXInstantiateModelCallback cb) {
        String token = "/takml_mxf_token_" + UUID.randomUUID().toString();
        MXInstantiateRequest req = new MXInstantiateRequest(pluginID, modelDirectory,
                modelFilename, token, params);

        try {
            this.client.subscribe(token, DEFAULT_QOS, this::instantiateResponseArrived);
        } catch (MqttException e) {
            e.printStackTrace();
            Log.e(TAG, "Could not subscribe to token " + token + " when instantiating plugin " + pluginID);
            return null;
        }
        this.lock.lock();
        this.instantiateCallbacks.put(token, cb);
        this.lock.unlock();

        try {
            // Serialize creation request.
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(req);
            byte[] serialized = bos.toByteArray();
            out.close();
            bos.close();

            // Send MQTT message to MX framework.
            MqttMessage msg = new MqttMessage();
            msg.setPayload(serialized);
            this.client.publish(MX_INSTANTIATE, msg);
        } catch (MqttException | IOException e) {
            e.printStackTrace();
            this.lock.lock();
            this.instantiateCallbacks.remove(token);
            this.lock.unlock();
            try {
                this.client.unsubscribe(token);
            } catch (MqttException e2) {
                e.printStackTrace();
            }
            token = null;
        }

        return token;
    }

    /**
     * Instantiate an MX plugin.
     *
     * @param pluginID the unique identifier for a {@link com.bbn.tak.ml.mx_framework.MXPlugin}.
     * @param modelFilename the filename for the model file.
     * @param params the dictionary of parameters used by the plugin.
     * @param cb the callback that will receive the reply to this instantiation request.
     * @return
     */
    public String instantiatePlugin(String pluginID, String modelFilename,
                                    HashMap<String, Serializable> params,
                                    MXInstantiateModelCallback cb) {
        return instantiatePlugin(pluginID, null, modelFilename, params, cb);
    }

    /**
     * Execute a prediction using an {@link com.bbn.tak.ml.mx_framework.MXPlugin}.
     *
     * @param mxpInstanceID the ID of the instance that will make a prediction.
     * @param inputData the input data for the prediction.
     * @param cb the callback that will receive the reply to this execute request.
     * @return
     */
    public String executePrediction(String mxpInstanceID, byte[] inputData,
                                    MXExecuteModelCallback cb) {
        if (inputData.length > TAK_ML_MAX_PAYLOAD_SIZE) {
            Log.e(TAG, "Data passed to executePrediction() (" + inputData.length + " bytes) greater than the maximum payload size (" + TAK_ML_MAX_PAYLOAD_SIZE + ")");
            return null;
        }

        String pluginID = this.instanceIDToPluginID.get(mxpInstanceID);
        String executeID = mxpInstanceID + "/execute_" + UUID.randomUUID().toString();
        MXExecuteRequest req = new MXExecuteRequest(pluginID, mxpInstanceID, executeID, inputData);

        try {
            this.client.subscribe(executeID, DEFAULT_QOS, this::executeResponseArrived);
        } catch (MqttException e) {
            e.printStackTrace();
            Log.e(TAG, "Could not subscribe to execution ID " + executeID + " when executing plugin " + pluginID + " instance " + mxpInstanceID);
            return null;
        }
        this.lock.lock();
        this.executeCallbacks.put(executeID, cb);
        this.lock.unlock();

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
            this.client.publish(MX_EXECUTE, msg);
        } catch (MqttException | IOException e) {
            e.printStackTrace();
            this.lock.lock();
            this.executeCallbacks.remove(executeID);
            this.lock.unlock();
            try {
                this.client.unsubscribe(executeID);
            } catch (MqttException e2) {
                e.printStackTrace();
            }
            executeID = null;
        }

        return executeID;
    }

    /**
     * Destroy a plugin instance.
     *
     * @param mxpInstanceID the ID of the plugin instance to destroy.
     * @return whether the request to destroy the plugin instance was successful.
     */
    public boolean destroyPlugin(String mxpInstanceID) {
        String pluginID = this.instanceIDToPluginID.get(mxpInstanceID);
        MXDestroyRequest req = new MXDestroyRequest(pluginID, mxpInstanceID);

        try {
            // Serialize destroy request.
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(req);
            byte[] serialized = bos.toByteArray();
            out.close();
            bos.close();

            // Send MQTT message.
            MqttMessage msg = new MqttMessage();
            msg.setPayload(serialized);
            this.client.publish(MX_DESTROY, msg);
        } catch (MqttException | IOException e) {
            e.printStackTrace();
            return false;
        }

        this.instanceIDToPluginID.remove(mxpInstanceID);
        return true;
    }

    /**
     * Request that the MX framework list the available MX plugins and models and
     * then issue callbacks to the application when either of those resources change.
     *
     * {@link setMxListResourcesCallback(MXListResourcesCallback)} must be called before
     * requesting the resources list.
     */
    public void requestResourcesList() {
        this.requestResources = true;

        if (!this.isConnected()) {
            return;
        }

        if (this.listCB != null) {
            try {
                this.client.subscribe(MX_LIST_RESOURCES_REPLY, DEFAULT_QOS, this::listResourcesReplyArrived);
            } catch (MqttException e) {
                e.printStackTrace();
                Log.e(TAG, "Could not subscribe to receive resources list updates");
            }
            try {
                // Send MQTT message.
                MqttMessage msg = new MqttMessage();
                msg.setPayload(new byte[0]);
                this.client.publish(MX_LIST_RESOURCES_REQ, msg);
            } catch (MqttException e) {
                e.printStackTrace();
                Log.e(TAG, "Could not publish request to list resources");
            }
        }
    }

    private void listResourcesReplyArrived(String topic, MqttMessage message) throws Exception {
        if (this.listCB == null) {
            Log.e(TAG, "Not configured to use a callback for listing TAK-ML resources");
            return;
        }

        MXListResourcesReply reply = (MXListResourcesReply)deserialize(message.getPayload());
        this.resourcesListLock.lock();
        this.listCB.listResourcesCB(reply);
        this.resourcesListLock.unlock();
    }

    /**
     * Extract a plugin ID from a plugin label.
     * <p>
     * The list of plugins delivered by the MX framework are stylized as labels
     * of the format "Plugin Name (Plugin ID)." This method extracts the plugin
     * ID for an application, which can be used in requests to the MX framework.
     *
     * @param pluginLabel the plugin label from which to extract an ID.
     * @return a plugin ID.
     */
    public static String pluginLabelToID(String pluginLabel) {
        return pluginLabel.substring(pluginLabel.indexOf("(") + 1, pluginLabel.length() - 1);
    }

    /**
     * Extract a model name from a model label.
     * <p>
     * The list of models delivered by the MX framework are stylized as labels
     * of the format "Model Name (Location)." This method extracts the model
     * name for an application, which can be used in requests to the MX framework.
     *
     * @param modelLabel the model label from which to extract a model name.
     * @return a model name.
     */
    public static String modelLabelToName(String modelLabel) {
        return modelLabel.substring(0, modelLabel.indexOf("(") - 1);
    }

    /**
     * Request to start sensor reading.
     *
     * @param sensorPluginID the ID of the sensor plugin.
     * @param cb the callback function for the sensor data reading.
     * @return whether the request to start sensor reading was successful.
     */
    public boolean requestSensorReadStart(String sensorPluginID, IMqttMessageListener cb) {

        String topic = SENSOR_DATA_REPORT_PREFIX + sensorPluginID;

        Log.d(TAG, "Subscribing to sensor data on: " + topic);

        try {
            this.client.subscribe(topic, DEFAULT_QOS, cb);
        } catch (MqttException e) {
            e.printStackTrace();
        }

        SFReadStartRequest req = new SFReadStartRequest(clientID, sensorPluginID);

        try {
            // Serialize creation request.
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(req);
            byte[] serialized = bos.toByteArray();
            out.close();
            bos.close();

            // Send MQTT message to TAKML Framework App.
            MqttMessage msg = new MqttMessage();
            msg.setPayload(serialized);
            client.publish(SENSOR_READ_START_REQUEST, msg);
        } catch (MqttException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Request to stop sensor reading.
     *
     * @param sensorPluginID the ID of the sensor plugin.
     * @return whether the request to stop sensor reading was successful.
     */
    public boolean requestSensorReadStop(String sensorPluginID) {

        String topic = SENSOR_DATA_REPORT_PREFIX + sensorPluginID;

        Log.d(TAG, "Unsubscribing from sensor data on: " + topic);

        try {
            this.client.unsubscribe(topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }

        SFReadStopRequest req = new SFReadStopRequest(clientID, sensorPluginID);

        try {
            // Serialize creation request.
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(req);
            byte[] serialized = bos.toByteArray();
            out.close();
            bos.close();

            // Send MQTT message to TAKML Framework App.
            MqttMessage msg = new MqttMessage();
            msg.setPayload(serialized);
            client.publish(SENSOR_READ_STOP_REQUEST, msg);
        } catch (MqttException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Request that the application be updated on available sensors.
     * <p>
     * {@link #setSensorListQueryCallback(SensorListQueryCallback)} should be called
     * first to set the callback that receives the updates.
     */
    public boolean requestRegisteredSensors() {

        SFQuerySensorList req = new SFQuerySensorList(clientID);

        try {
            // Serialize creation request.
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(req);
            byte[] serialized = bos.toByteArray();
            out.close();
            bos.close();

            // Send MQTT message to TAKML Framework App.
            MqttMessage msg = new MqttMessage();
            msg.setPayload(serialized);
            client.publish(SENSOR_QUERY_LIST, msg);
        } catch (MqttException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private void sensorListResponseArrived(String topic, MqttMessage message) throws Exception {
        Log.d(TAG, "message arrived (topic: " + topic + ")");

        if (topic.contains(SENSOR_LIST_QUERY_REPLY_PREFIX)) {
            SensorList sensorList = (SensorList) deserialize(message.getPayload());
            List<String> serializedSensorList = sensorList.getSerializedSensors();
            List<Sensor> sensorsList = new ArrayList<>();
            for (String serializedSensor : serializedSensorList) {
                Sensor sensor = parser.parseSensor(serializedSensor);
                sensorsList.add(sensor);
            }
            if (sensorListQueryCallback != null) {
                sensorListQueryCallback.receivedSensorList(sensorsList);
            } else {
                Log.e(TAG, "sensorListQueryCallback was null.");
            }
        }
    }

    /**
     * Request the TAKML Framework's app data directory, so that app specific data can be loaded.
     *
     * {@link setAppDataDirectoryQueryCallback(TAKMLAppDataDirectoryQueryCallback)} must be called before
     * requesting the TAKML Framework's app data directory.
     */
    public void requestTAKMLFrameworkAppDataDirectory() {

        if (!this.isConnected()) {
            Log.e(TAG, "Not connected, takml framework app data query failed.");
            return;
        }

        if (this.appDataDirectoryQueryCallback != null) {
            try {
                this.client.subscribe(TAKML_APP_DATA_DIRECTORY_RESPONSE, DEFAULT_QOS, this::appDataDirectoryQueryReplyArrived);
            } catch (MqttException e) {
                e.printStackTrace();
                Log.e(TAG, "Could not subscribe to receive resources list updates");
            }
            try {
                // Send MQTT message.
                MqttMessage msg = new MqttMessage();
                msg.setPayload(new byte[0]);
                this.client.publish(TAKML_APP_DATA_DIRECTORY_QUERY, msg);
            } catch (MqttException e) {
                e.printStackTrace();
                Log.e(TAG, "Could not publish request for takml app data directory");
            }
        }
    }

    private void appDataDirectoryQueryReplyArrived(String topic, MqttMessage message) {
        Log.d(TAG, "message arrived (topic: " + topic + ")");

        if (topic.contains(TAKML_APP_DATA_DIRECTORY_RESPONSE)) {
            String takmlAppDataDirectory = new String(message.getPayload());
            if (appDataDirectoryQueryCallback != null) {
                appDataDirectoryQueryCallback.appDataDirectoryQueryResult(takmlAppDataDirectory);
            } else {
                Log.e(TAG, "takmlAppDataDirectoryQueryCallback was null.");
            }
        }
    }

    /**
     * Request the TAKML Framework's app data directory, so that app specific data can be loaded.
     *
     * {@link setAppDataDirectoryResourcesListQueryCallback(TAKMLAppDataDirectoryResourcesListQueryCallback)} must be called before
     * requesting the TAKML Framework's app data directory.
     */
    public void requestTAKMLFrameworkAppDataDirectoryResourcesList() {

        if (!this.isConnected()) {
            Log.e(TAG, "Not connected, takml framework app data resources list query failed.");
            return;
        }

        if (this.appDataDirectoryQueryCallback != null) {
            try {
                this.client.subscribe(TAKML_APP_DATA_RESOURCES_LIST_RESPONSE, DEFAULT_QOS,
                        this::appDataDirectoryResourcesListQueryReplyArrived);
            } catch (MqttException e) {
                e.printStackTrace();
                Log.e(TAG, "Could not subscribe to receive resources list updates");
            }
            try {
                // Send MQTT message.
                MqttMessage msg = new MqttMessage();
                msg.setPayload(new byte[0]);
                this.client.publish(TAKML_APP_DATA_RESOURCES_LIST_QUERY, msg);
            } catch (MqttException e) {
                e.printStackTrace();
                Log.e(TAG, "Could not publish request for takml app data directory resources list");
            }
        }
    }

    private void appDataDirectoryResourcesListQueryReplyArrived(String topic, MqttMessage message) {
        Log.d(TAG, "message arrived (topic: " + topic + ")");

        if (topic.contains(TAKML_APP_DATA_RESOURCES_LIST_RESPONSE)) {
            TAKMLAppDataDirectoryResourcesListReply reply =
                    (TAKMLAppDataDirectoryResourcesListReply) deserialize(message.getPayload());
            if (appDataDirectoryResourcesListQueryCallback != null) {
                appDataDirectoryResourcesListQueryCallback.appDataDirectoryResourcesListCallback(reply.getFilesList());
            } else {
                Log.e(TAG, "takmlAppDataDirectoryResourcesListQueryCallback was null.");
            }
        }
    }
}
