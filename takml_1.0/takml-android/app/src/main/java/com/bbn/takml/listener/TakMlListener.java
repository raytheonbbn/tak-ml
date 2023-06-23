package com.bbn.takml.listener;

import static com.bbn.tak.ml.TakMlConstants.*;
import static com.bbn.tak.ml.TakMlConstants.MX_DEREGISTER;
import static com.bbn.tak.ml.TakMlConstants.MX_DESTROY;
import static com.bbn.tak.ml.TakMlConstants.MX_EXECUTE;
import static com.bbn.tak.ml.TakMlConstants.MX_INSTANTIATE;
import static com.bbn.tak.ml.TakMlConstants.MX_LIST_RESOURCES_REPLY;
import static com.bbn.tak.ml.TakMlConstants.MX_LIST_RESOURCES_REQ;
import static com.bbn.tak.ml.TakMlConstants.MX_REFRESH;
import static com.bbn.tak.ml.TakMlConstants.MX_REGISTER;
import static com.bbn.tak.ml.TakMlConstants.SENSOR_DATA_REPORT_PREFIX;
import static com.bbn.tak.ml.TakMlConstants.SENSOR_DB_QUERY;
import static com.bbn.tak.ml.TakMlConstants.SENSOR_DEREGISTER;
import static com.bbn.tak.ml.TakMlConstants.SENSOR_QUERY_LIST;
import static com.bbn.tak.ml.TakMlConstants.SENSOR_READ_START_REQUEST;
import static com.bbn.tak.ml.TakMlConstants.SENSOR_READ_STOP_REQUEST;
import static com.bbn.tak.ml.TakMlConstants.SENSOR_REGISTER;
import static com.bbn.tak.ml.TakMlConstants.TAKML_APP_DATA_DIRECTORY_QUERY;
import static com.bbn.tak.ml.TakMlConstants.TAKML_APP_DATA_DIRECTORY_RESPONSE;
import static com.bbn.tak.ml.TakMlConstants.TAKML_APP_DATA_RESOURCES_LIST_QUERY;
import static com.bbn.tak.ml.TakMlConstants.TAKML_APP_DATA_RESOURCES_LIST_RESPONSE;
import static com.bbn.tak.ml.TakMlConstants.TAK_ML_LISTENER_DEFAULT_PORT;

import android.util.Log;

import com.bbn.takml.mx_framework.MXFramework;
import com.bbn.takml.sensor_framework.SensorFramework;
import com.bbn.takml.support.TakMlClasspathResourceLoader;
import com.bbn.takml_sdk_android.TAKMLAppDataDirectoryResourcesListReply;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.moquette.broker.Server;
import io.moquette.broker.config.IConfig;
import io.moquette.broker.config.IResourceLoader;
import io.moquette.broker.config.ResourceLoaderConfig;
import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.InterceptAcknowledgedMessage;
import io.moquette.interception.messages.InterceptConnectMessage;
import io.moquette.interception.messages.InterceptConnectionLostMessage;
import io.moquette.interception.messages.InterceptDisconnectMessage;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.moquette.interception.messages.InterceptSubscribeMessage;
import io.moquette.interception.messages.InterceptUnsubscribeMessage;
import io.netty.buffer.ByteBuf;

//======================================================================
//  There is no API documentation for Moquette; Github
//  repository (next best thing) is located at:
//  https://github.com/moquette-io/moquette
//======================================================================

public class TakMlListener implements Runnable {

    private boolean cancelled = false;

    public static final String TAK_ML_Listener_ListeningIP = "0.0.0.0";
    public static final boolean TAK_ML_Listener_AllowAnonymous = true;

    private Server listenerServer;
    private InterceptHandler interceptor;
    private ServerPublisher publisher;
    private SensorFramework sensorFramework;
    private MXFramework mxFramework;

    private File takmlDirectory;

    private boolean isServerStarted = false;

    /***************CONSTRUCTOR**********/
    public TakMlListener(SensorFramework inputSensorFramework, MXFramework inputMXFramework,
                         File takmlDirectory) {
        this.sensorFramework = inputSensorFramework;
        this.sensorFramework.takMlListener = this;
        this.mxFramework = inputMXFramework;
        this.listenerServer = new Server();
        this.publisher = new ServerPublisher(this.listenerServer);
        this.takmlDirectory = takmlDirectory;
    }

    public SensorFramework getSensorFramework() {
        return sensorFramework;
    }

    public ServerPublisher getPublisher() {
        return this.publisher;
    }

    public void cancel() {
        cancelled = true;
        this.listenerServer.removeInterceptHandler(this.interceptor);
        this.listenerServer.stopServer();
    }

    @Override
    public void run() {

        //===================================================
        //  Stand up the TAKML_Listener - Moquette Server
        //===================================================
        try {
            IResourceLoader classpathLoader = new TakMlClasspathResourceLoader(TAK_ML_LISTENER_DEFAULT_PORT, TAK_ML_Listener_ListeningIP, TAK_ML_Listener_AllowAnonymous);
            final IConfig classPathConfig = new ResourceLoaderConfig(classpathLoader);
            this.interceptor = new PublishListener();
            final List<? extends InterceptHandler> userHandlers = Collections.singletonList(this.interceptor);
            this.listenerServer.startServer(classPathConfig, userHandlers);
            isServerStarted = true;
        } catch (Exception e) {
            System.out.println("Caught: " + e);
            e.printStackTrace();
        }


        /*while(!cancelled) {
            try {
                Thread.sleep(10);
                //Collection<ClientDescriptor> clients = TAK_ML_Listener.listConnectedClients();
            }
            catch(InterruptedException ie) {
                cancelled = true;
            }
        }
        */

        // ** TODO: put this somewhere
        //--------------------------------
        // tear down TAK-ML Framework
        //--------------------------------
        //this.sensorFramework.teardownDB();
    }

    private static byte[] byteBufToBytes(ByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        int readerIndex = buf.readerIndex();
        buf.getBytes(readerIndex, bytes);
        return bytes;
    }

    /***************************************************************
     *  Support classes
     ***************************************************************/

    private class PublishListener extends AbstractInterceptHandler {

        private final String TAG = PublishListener.class.getSimpleName();

        @Override
        public String getID() {
            return "EmbeddedLauncherPublishListener";
        }

        @Override
        public void onPublish(InterceptPublishMessage msg) {
            Log.d(TAG, "Received new message");
            //------------------------------------------
            // Process a Sensor Registration message
            //------------------------------------------

            String strMsg = null;
            if(msg != null && msg.getPayload() != null) {
                strMsg = msg.getPayload().toString(Charset.defaultCharset());
                Log.d(TAG, "Message payload: " + strMsg);
            }
            String topic = msg.getTopicName().toLowerCase();
            String clientID = msg.getClientID();

            try {
                if (topic.contains(SENSOR_REGISTER)) {
                    Log.d(TAG, "Received sensor registration");
                    sensorFramework.registerSensor(strMsg);
                } else if (topic.contains(SENSOR_DATA_REPORT_PREFIX)) {
                    Log.d(TAG, "Received sensor observation");
                    sensorFramework.recordSensorData(strMsg);
                } else if (topic.contains(SENSOR_DB_QUERY)) {
                    Log.d(TAG, "Received sensor database query.");
                    sensorFramework.executeDBquery(strMsg, topic, clientID);
                } else if (topic.contains(SENSOR_DEREGISTER)) {
                    Log.d(TAG, "Received sensor deregister");
                    sensorFramework.deRegisterSensor(strMsg, true);
                } else if (topic.contains(SENSOR_READ_START_REQUEST)) {
                    Log.d(TAG, "Received sensor read start request.");
                    sensorFramework.processSensorReadStartRequest(byteBufToBytes(msg.getPayload()));
                } else if (topic.contains(SENSOR_READ_STOP_REQUEST)) {
                    Log.d(TAG, "Received sensor read stop request.");
                    sensorFramework.processSensorReadStopRequest(byteBufToBytes(msg.getPayload()));
                } else if (topic.contains(SENSOR_QUERY_LIST)) {
                    Log.d(TAG, "Received sensor query list request.");
                    sensorFramework.processSensorQueryList(byteBufToBytes(msg.getPayload()));
                } else if (topic.contains(MX_INSTANTIATE)) {
                    Log.d(TAG, "Received MX create");
                    mxFramework.instantiate(byteBufToBytes(msg.getPayload()));
                } else if (topic.contains(MX_EXECUTE)) {
                    Log.d(TAG, "Received MX execute");
                    mxFramework.execute(byteBufToBytes(msg.getPayload()));
                } else if (topic.contains(MX_DESTROY)) {
                    Log.d(TAG, "Received MX destroy");
                    mxFramework.destroy(byteBufToBytes(msg.getPayload()));
                } else if (topic.contains(MX_REGISTER)) {
                    Log.d(TAG, "Received MX register");
                    mxFramework.register(byteBufToBytes(msg.getPayload()));
                } else if (topic.contains(MX_DEREGISTER)) {
                    Log.d(TAG, "Received MX deregister");
                    mxFramework.deregister(byteBufToBytes(msg.getPayload()));
                } else if (topic.contains(MX_LIST_RESOURCES_REQ)) {
                    Log.d(TAG, "Received MX list resources request");
                    mxFramework.listResources();
                } else if (topic.contains(TAKML_APP_DATA_DIRECTORY_QUERY)) {
                    publisher.publish(TAKML_APP_DATA_DIRECTORY_RESPONSE, (takmlDirectory.getAbsolutePath() + "/" + "app_data").getBytes(), "");
                } else if (topic.contains(TAKML_APP_DATA_RESOURCES_LIST_QUERY)) {
                    Log.d(TAG, "Got app data resources list query, listing files in " + takmlDirectory.getAbsolutePath() + "/" + "app_data/");
                    String[] appdataFileNames = new File(takmlDirectory.getAbsolutePath() + "/" + "app_data/").list();
                    List<String> fileNamesList = new ArrayList<>();
                    if (appdataFileNames != null) {
                        for (String n : appdataFileNames) {
                            fileNamesList.add(n);
                        }
                    }
                    Log.d(TAG, "Found app data files: " + fileNamesList);
                    TAKMLAppDataDirectoryResourcesListReply r = new TAKMLAppDataDirectoryResourcesListReply(fileNamesList);
                    byte[] output = MXFramework.serialize(r, "Error serializing successful reply for app data directory resources list.");
                    publisher.publish(TAKML_APP_DATA_RESOURCES_LIST_RESPONSE, output, "");
                } else if (topic.contains(MX_REFRESH) || topic.contains(MX_LIST_RESOURCES_REPLY) ||
                        topic.contains("/takml_mxf_")) {
                } else {
                    Log.e(TAG, "Received message of unhandled type: " + msg.getTopicName());
                }
            } catch (IOException ioe) {
                Log.d(TAG, "Exception occurred while handling published event: " + ioe.getMessage());
                ioe.printStackTrace();
            }
        }

        @Override
        public Class<?>[] getInterceptedMessageTypes() {
            return InterceptHandler.ALL_MESSAGE_TYPES;
        }

        @Override
        public void onConnect(InterceptConnectMessage msg) {
        }

        @Override
        public void onDisconnect(InterceptDisconnectMessage msg) {
        }

        @Override
        public void onConnectionLost(InterceptConnectionLostMessage msg) {
        }

        @Override
        public void onSubscribe(InterceptSubscribeMessage msg) {
        }

        @Override
        public void onUnsubscribe(InterceptUnsubscribeMessage msg) {
        }

        @Override
        public void onMessageAcknowledged(InterceptAcknowledgedMessage msg) {
        }

    }

    public boolean isServerStarted() {
        return isServerStarted;
    }
}
