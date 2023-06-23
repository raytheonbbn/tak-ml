
package com.atakmap.android.takml.mx_framework;

import android.content.Context;
import android.content.Intent;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.dropdown.DropDownMapComponent;

import com.atakmap.android.takml.mx_framework.plugin.PTMobilePlugin;
import com.atakmap.coremap.log.Log;
import com.atakmap.android.takml.mx_framework.plugin.R;
import com.bbn.tak.ml.mx_framework.MXFrameworkRegistrar;
import com.bbn.takml_sdk_android.mx_framework.MXFrameworkAndroidRegistrar;

import java.util.Timer;
import java.util.TimerTask;

public class PTMobilePluginMapComponent extends DropDownMapComponent {

    private static final String TAG = PTMobilePluginMapComponent.class.getSimpleName();
    private final static int REREGISTER_INTERVAL_MS = 1000;

    private Context pluginContext;
    private MXFrameworkRegistrar mxf;
    private Timer reregisterTimer;

    private class ReregisterTask extends TimerTask {
        @Override
        public void run() {
            register();
        }
    }

    public void onCreate(final Context context, Intent intent,
            final MapView view) {

        context.setTheme(R.style.ATAKPluginTheme);
        super.onCreate(context, intent, view);
        pluginContext = context;

        this.mxf = new MXFrameworkAndroidRegistrar(TAG);
        this.reregisterTimer = new Timer();
        register();

        Log.d(TAG, "Created plugin");
    }

    private void register() {
        if (!this.mxf.register(PTMobilePlugin.class)) {
            Log.e(TAG, "Could not register plugin with MX framework, retrying...");
            reregisterTimer.schedule(new ReregisterTask(), REREGISTER_INTERVAL_MS);
        }
    }


    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        if (!this.mxf.deregister(PTMobilePlugin.class))
            Log.e(TAG, "Could not deregister plugin with MX framework");
	this.mxf.stop();
        super.onDestroyImpl(context, view);
    }

}
