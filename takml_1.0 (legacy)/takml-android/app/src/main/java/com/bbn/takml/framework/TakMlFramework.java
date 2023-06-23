package com.bbn.takml.framework;

import static com.atakmap.android.takml_framework.receivers.SettingsDropDownReceiver.DEFAULT_ML_PORT_VALUE;
import static com.atakmap.android.takml_framework.receivers.SettingsDropDownReceiver.HOST_ADDR;
import static com.atakmap.android.takml_framework.receivers.SettingsDropDownReceiver.ML_PORT;

import android.content.Context;
import android.os.Environment;

import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;
import com.bbn.tak.ml.mx_framework.MXPluginDescription;
import com.bbn.tak.ml.mx_framework.Model;
import com.bbn.tak.ml.sensor.SensorDataStream;
import com.bbn.takml.listener.TakMlListener;
import com.bbn.takml.mx_framework.InstanceChangedCallback;
import com.bbn.takml.mx_framework.MXFramework;
import com.bbn.takml.mx_framework.MXPluginChangedCallback;
import com.bbn.takml.mx_framework.ModelChangedCallback;
import com.bbn.takml.sensor_framework.SensorFramework;
import com.bbn.takml.support.TakMlConfigPropertyLoader;

import java.io.File;

public class TakMlFramework {//} extends Service {

    private final static String TAG = "TAKML_Framework";

    private Context context;
    private boolean cancelled;
    private TakMlListener listener;
    private Thread TAKML_ListenerThread;
    private SensorListChangedCallback sensorListChangedCallback;
    private MXPluginChangedCallback mxPluginChangedCallback;
    private ModelChangedCallback modelChangedCallback;
    private InstanceChangedCallback instanceChangedCallback;

    private File takmlDirectory;
    private String remoteServIP;
    private int remoteServPort;

    private MXFramework mxFramework;

    //=================================
    //  Constructors
    //=================================
    public TakMlFramework(Context context, SensorListChangedCallback sensorListChangedCallback,
                          MXPluginChangedCallback mxPluginChangedCallback,
                          ModelChangedCallback modelChangedCallback,
                          InstanceChangedCallback instanceChangedCallback) {
        this.context = context;
        this.cancelled = false;

        if (sensorListChangedCallback == null) {
            sensorListChangedCallback = this::updateSensorList;
        }

        this.sensorListChangedCallback = sensorListChangedCallback;

        if (mxPluginChangedCallback == null) {
            mxPluginChangedCallback = this::mxChangeOccurred;
        }
        this.mxPluginChangedCallback = mxPluginChangedCallback;

        if (modelChangedCallback == null) {
            modelChangedCallback = this::modelChangeOccurred;
        }
        this.modelChangedCallback = modelChangedCallback;

        if (instanceChangedCallback == null) {
            instanceChangedCallback = this::instanceChangeOccurred;
        }
        this.instanceChangedCallback = instanceChangedCallback;

        String takmlDirName = TakMlConfigPropertyLoader.getString(MapView.getMapView().getContext(), "takml_data_directory", "com.atakmap.android.takml_framework.plugin/files");
        this.takmlDirectory = new File(Environment.getExternalStorageDirectory(), takmlDirName);
        this.takmlDirectory.mkdirs();

        this.remoteServIP = TakMlConfigPropertyLoader.getString(MapView.getMapView().getContext(), HOST_ADDR, "");
        this.remoteServPort = TakMlConfigPropertyLoader.getInt(MapView.getMapView().getContext(), ML_PORT, DEFAULT_ML_PORT_VALUE);
    }

    public TakMlFramework(Context context) {
        this(context, null, null, null, null);
    }

    public String getRemoteServIP() {
        return this.remoteServIP;
    }

    public int getRemoteServPort() {
        return this.remoteServPort;
    }

    public void reconnectRemoteServer(String remoteServIP, int remoteServPort) {
        this.remoteServIP = remoteServIP;
        this.remoteServPort = remoteServPort;

        try {
            // ** TODO: resolve the fact that these two shouldn't use the same port
            this.mxFramework.grpcChannelReconnect(this.remoteServIP, this.remoteServPort);
            this.getSensorFramework().connectToServer(false, remoteServIP, remoteServPort);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateSensorList(SensorDataStream sensorDataStream, ChangeType changeType) {
        Log.d(TAG, "Sensor change occurred: " + sensorDataStream.getSensor().toString() + " : " + changeType);
    }

    public void mxChangeOccurred(MXPluginDescription desc, ChangeType changeType) {
        Log.d(TAG, changeType.toString() + " " + desc.id());
    }

    public void modelChangeOccurred(String modelLabel, Model m, ChangeType changeType) {
        Log.d(TAG, changeType.toString() + " " + modelLabel);
    }

    public void instanceChangeOccurred(String pluginID, String mxpInstanceID, ChangeType changeType) {
        Log.d(TAG, changeType.toString() + " " + mxpInstanceID);
    }

    /*
    @Override
    public IBinder onBind(Intent intent) {
        TextContainer.getTopInstance()
                .displayPrompt("onBind() for TAKML Framework");
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        TextContainer.getTopInstance()
                .displayPrompt("onCreate() for TAKML Framework");
    }
     */

    //@Override
    //public int onStartCommand(Intent intent, int flags, int startId) {
    public boolean startFramework() {

        //===============================================
        //  create TAKML_Sensor_Framework
        //===============================================
        SensorFramework sensorFramework = new SensorFramework(this.context, sensorListChangedCallback);

        //===============================================
        //  create TAKML_MX_Framework
        //===============================================
        this.mxFramework = new MXFramework(this.context,
                mxPluginChangedCallback, modelChangedCallback, instanceChangedCallback,
                this.takmlDirectory,  this.remoteServIP, this.remoteServPort);

        //===============================================
        //  stand up TAKML_Listener
        //===============================================
        this.listener = new TakMlListener(sensorFramework, this.mxFramework, this.takmlDirectory);
        TAKML_ListenerThread = new Thread(listener);
        TAKML_ListenerThread.start();

        while(!listener.isServerStarted()) {
            Log.d(TAG, "Waiting for TAK ML listener to start");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        /* Allow MX framework access to publishing functionality of broker. */
        this.mxFramework.setPublisher(listener.getPublisher());

        /* Allow Sensor framework access to publishing functionality of broker. */
        sensorFramework.setPublisher(listener.getPublisher());

        return true;
    }

    public MXFramework getMXFramework() {
        return this.mxFramework;
    }

    public SensorFramework getSensorFramework() {
        // ** TODO: why is the sensor framework a field on the listener and not the TAK ML Framework - should probably move
        return this.listener.getSensorFramework();
    }

    public boolean stopFramework() {

        Log.d(TAG, "Stopping MX framework");
        this.mxFramework.stop();
        this.mxFramework = null;

        TAKML_ListenerThread.interrupt();
        this.listener.cancel();

        return true;
    }

}
