
package com.atakmap.android.takml.mx_framework;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.dropdown.DropDownMapComponent;

import com.atakmap.android.takml.mx_framework.pytorch_plugin.PTMobilePlugin;
import com.atakmap.coremap.log.Log;
import com.atakmap.android.takml.mx_framework.pytorch_plugin.R;
import com.bbn.tak.ml.mx_framework.MXFrameworkRegistrar;
import com.bbn.takml_sdk_android.mx_framework.MXFrameworkAndroidRegistrar;

import java.util.Timer;
import java.util.TimerTask;

public class PTMobilePluginMapComponent extends DropDownMapComponent {

    private static final String TAG = "PTMobilePluginMapComponent";
    private final static int REREGISTER_INTERVAL_MS = 1000;

    private MXFrameworkRegistrar mxf;

    public void onCreate(final Context context, Intent intent,
            final MapView view) {

        context.setTheme(R.style.ATAKPluginTheme);
        super.onCreate(context, intent, view);

        this.mxf = new MXFrameworkAndroidRegistrar(TAG);
        AsyncTask.execute(() -> {
            while (!mxf.register(PTMobilePlugin.class)) {
                Log.e(TAG, "Could not register plugin with MX framework, retrying...");
                try {
                    Thread.sleep(REREGISTER_INTERVAL_MS);
                } catch (InterruptedException ignored) {
                }
            }
        });

        Log.d(TAG, "Created plugin");
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        if(this.mxf != null) {
            if (!this.mxf.deregister(PTMobilePlugin.class))
                Log.e(TAG, "Could not deregister plugin with MX framework");
            this.mxf.stop();
            super.onDestroyImpl(context, view);
        }
    }

}
