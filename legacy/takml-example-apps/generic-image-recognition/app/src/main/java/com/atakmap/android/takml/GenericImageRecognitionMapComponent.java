
package com.atakmap.android.takml;

import android.content.Context;
import android.content.Intent;

import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.dropdown.DropDownMapComponent;

import com.atakmap.android.takml.plugin.R;
import com.atakmap.android.takml.mvc.Model;
import com.atakmap.android.takml.receivers.GenericImageRecognitionDropDownReceiver;
import com.atakmap.android.takml.receivers.SetConfidenceThresholdDropDownReceiver;
import com.atakmap.android.takml.receivers.SetMXPluginDropDownReceiver;
import com.atakmap.android.takml.receivers.SetMxPluginParametersDropDownReceiver;
import com.atakmap.android.takml.receivers.SettingsDropDownReceiver;
import com.atakmap.coremap.log.Log;

import java.io.IOException;

public class GenericImageRecognitionMapComponent
        extends DropDownMapComponent {

    public static final String TAG = GenericImageRecognitionMapComponent.class.getSimpleName();

    public Context pluginContext;
    private GenericImageRecognitionDropDownReceiver ddr;

    private Model model;


    public void onCreate(final Context context, Intent intent,
            final MapView view) {

        context.setTheme(R.style.ATAKPluginTheme);
        super.onCreate(context, intent, view);
        pluginContext = context;

        try {
            model = new Model();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to initialize model: " + e.getMessage());
            return;
        }

        ddr = new GenericImageRecognitionDropDownReceiver(view, context, model);

        model.setViewControllerReference(ddr);

        SetMXPluginDropDownReceiver smpddr = new SetMXPluginDropDownReceiver(view, pluginContext, model);
        registerReceiverUsingPluginContext(pluginContext,
                smpddr, SetMXPluginDropDownReceiver.SHOW_SET_MX_PLUGIN);

        SettingsDropDownReceiver sddr = new SettingsDropDownReceiver(view, pluginContext, model);
        registerReceiverUsingPluginContext(pluginContext,
                sddr, SettingsDropDownReceiver.SHOW_SETTINGS);

        SetConfidenceThresholdDropDownReceiver sctddr = new SetConfidenceThresholdDropDownReceiver(view, pluginContext, model);
        registerReceiverUsingPluginContext(pluginContext,
                sctddr, SetConfidenceThresholdDropDownReceiver.SHOW_SET_CONFIDENCE_THRESHOLD);

        SetMxPluginParametersDropDownReceiver smppddr = new SetMxPluginParametersDropDownReceiver(view, pluginContext, model);
        registerReceiverUsingPluginContext(pluginContext,
                smppddr, SetMxPluginParametersDropDownReceiver.SHOW_SET_MX_PLUGIN_PARAMETERS);

        Log.d(TAG, "registering the plugin filter");
        DocumentedIntentFilter ddFilter = new DocumentedIntentFilter();
        ddFilter.addAction(GenericImageRecognitionDropDownReceiver.SHOW_PLUGIN);
        registerDropDownReceiver(ddr, ddFilter);

    }

    private void registerReceiverUsingPluginContext(Context pluginContext, DropDownReceiver rec, String actionName) {
        DocumentedIntentFilter mainIntentFilter = new DocumentedIntentFilter();
        mainIntentFilter.addAction(actionName);
        this.registerReceiver(pluginContext, rec, mainIntentFilter);
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        if(model != null) {
            model.shutdown();
        }
        super.onDestroyImpl(context, view);
    }

}
