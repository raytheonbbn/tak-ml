
package com.atakmap.android.takml;

import android.content.Context;
import android.content.Intent;
import android.widget.TextView;

import com.atakmap.android.atakutils.MiscUtils;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.dropdown.DropDownMapComponent;

import com.atakmap.android.takml.network.NetworkClient;
import com.atakmap.android.takml.plugin.R;
import com.atakmap.android.takml.receivers.PluginTemplateDropDownReceiver;
import com.atakmap.android.takml.receivers.ResultReceiver;
import com.atakmap.coremap.log.Log;

import com.bbn.takml_sdk_android.mx_framework.request.MXInstantiateReply;
import com.bbn.takml_sdk_android.mx_framework.PredictionExecutor;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class PluginTemplateMapComponent extends DropDownMapComponent {

    public static final String TAG = PluginTemplateMapComponent.class.getSimpleName();

    public Context context;

    private PluginTemplateDropDownReceiver ddr;

    private NetworkClient networkClient;
    private PredictionExecutor takmlExecutor;
    private String mxpInstanceID;
    private String token;
    private MapView view;
    private Timer timer;

    public void onCreate(final Context context, Intent intent, final MapView view) {

        context.setTheme(R.style.ATAKPluginTheme);
        super.onCreate(context, intent, view);
        this.context = context;
        this.view = view;

        TimerTask setupTask = new TimerTask() {
            @Override
            public void run() {
                setupTAKMLConnection();
            }
        };

        Log.d(TAG, "Scheduling setup of connection to TAK-ML for 2000ms from now");
        timer = new Timer();
        timer.schedule(setupTask, 2000);
    }

    private void setupTAKMLConnection() {
        Log.d(TAG, "Setting up connection to TAK-ML");
        try {
            this.takmlExecutor = new PredictionExecutor("atak-mnist");

        } catch (IOException e) {
            Log.d(TAG, "Unable to instantiate prediction executor: " + e.getMessage());
            e.printStackTrace();
            return;
        }


        int counter = 0;
        while(!takmlExecutor.isConnected() && counter++ < 5) {
            Log.d(TAG, "Waiting for MQTT connection to complete");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if(takmlExecutor.isConnected()) {
            Log.d(TAG, "MQTT connection established");
        } else {
            MiscUtils.toast("Unable to connect to MQTT after 5 attempts. Aborting.");
            return;
        }

        this.token = this.takmlExecutor.instantiatePlugin(
                "0987654321",  "/sdcard/", "mnist_cnn.pt",
                new HashMap<String, Serializable>(),
                this::instantiateCB);
        if (token == null) {
            //connectionError("Error: can't instantiate plugin " + pluginID + " with model " + model);
            return;
        }

        this.networkClient = new NetworkClient(this.takmlExecutor);

        ddr = new PluginTemplateDropDownReceiver(view, context, networkClient);

        Log.d(TAG, "registering the plugin filter");
        DocumentedIntentFilter ddFilter = new DocumentedIntentFilter();
        ddFilter.addAction(PluginTemplateDropDownReceiver.SHOW_PLUGIN);
        registerDropDownReceiver(ddr, ddFilter);

        registerReceiverUsingPluginContext(context, "classification result receiver",
                new ResultReceiver(view, context, networkClient),
                ResultReceiver.SHOW_RESULT_DROPDOWN);
    }

    public void instantiateCB(MXInstantiateReply reply) {
        if (!reply.getToken().equals(this.token)) {
            Log.e(TAG, "Error: Token returned by TAK-ML for instantiating plugin does not match expected value");
            return;
        }

        if (reply.isSuccess()) {
            this.networkClient.setMxpInstanceID(reply.getMxpInstanceID());
        } else {
            Log.e(TAG, "Error: " + reply.getMsg());
        }
    }

    private void registerReceiverUsingPluginContext(Context pluginContext, String name, DropDownReceiver rec, String actionName) {
        android.util.Log.d(TAG, "Registering " + name + " receiver with intent filter");
        AtakBroadcast.DocumentedIntentFilter mainIntentFilter = new AtakBroadcast.DocumentedIntentFilter();
        mainIntentFilter.addAction(actionName);
        this.registerReceiver(pluginContext, rec, mainIntentFilter);
    }

    private void registerReceiverUsingAtakContext(String name, DropDownReceiver rec, String actionName) {
        android.util.Log.d(TAG, "Registering " + name + " receiver with intent filter");
        AtakBroadcast.DocumentedIntentFilter mainIntentFilter = new AtakBroadcast.DocumentedIntentFilter();
        mainIntentFilter.addAction(actionName);
        AtakBroadcast.getInstance().registerReceiver(rec, mainIntentFilter);
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        if (!this.takmlExecutor.destroyPlugin(this.mxpInstanceID)) {
            Log.e(TAG, "Could not destroy instance " + this.mxpInstanceID);
        }
        this.networkClient.setMxpInstanceID(null);
        this.takmlExecutor.destroyPlugin(this.mxpInstanceID);
        this.takmlExecutor.stop();
        super.onDestroyImpl(context, view);
    }

}
