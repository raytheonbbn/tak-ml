
package com.atakmap.android.takml_framework.plugin;

import static com.atakmap.android.takml_framework.receivers.SettingsDropDownReceiver.DEFAULT_ML_PORT_VALUE;
import static com.atakmap.android.takml_framework.receivers.SettingsDropDownReceiver.DEFAULT_SENSOR_PORT_VALUE;
import static com.atakmap.android.takml_framework.receivers.SettingsDropDownReceiver.HOST_ADDR;
import static com.atakmap.android.takml_framework.receivers.SettingsDropDownReceiver.ML_PORT;
import static com.atakmap.android.takml_framework.receivers.SettingsDropDownReceiver.SENSOR_PORT;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import com.atakmap.android.maps.MapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml_framework.TakMlFrameworkMapComponent;
import com.atakmap.android.takml_framework.TakMlFrameworkWidget;
import com.atakmap.android.takml_framework.receivers.SettingsDropDownReceiver;
import com.atakmap.coremap.log.Log;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import transapps.maps.plugin.lifecycle.Lifecycle;

/**
 * In a plugin, the lifecycle and tool components are the only 
 * parts of the NettWarrior plugin architecture that ATAK uses.
 * An ATAK can have zero or more of each of these and they are 
 * defined in the assets folder under the plugins.xml file.
 *
 * A lifecycle roughy maps to the ATAK concept of a MapComponent
 * and is able to add a concrete concept to the ATAK environment.
 * In this case, this lifecycle is responsbile for two 
 * MapComponents.
 */
public class TakMlFrameworkLifecycle implements Lifecycle {

    private final Context pluginContext;
    private final Collection<MapComponent> overlays;
    private MapView mapView;

    private final static String TAG = TakMlFrameworkLifecycle.class.getSimpleName();

    public TakMlFrameworkLifecycle(Context ctx) {
        this.pluginContext = ctx;
        this.overlays = new LinkedList<MapComponent>();
        this.mapView = null;
    }

    @Override
    public void onConfigurationChanged(Configuration arg0) {
        for (MapComponent c : this.overlays)
            c.onConfigurationChanged(arg0);
    }

    @Override
    public void onCreate(final Activity arg0,
            final transapps.mapi.MapView arg1) {
        if (arg1 == null || !(arg1.getView() instanceof MapView)) {
            Log.w(TAG, "This plugin is only compatible with ATAK MapView");
            return;
        }
        this.mapView = (MapView) arg1.getView();
        TakMlFrameworkLifecycle.this.overlays.add(new TakMlFrameworkMapComponent());
        TakMlFrameworkLifecycle.this.overlays.add(new TakMlFrameworkWidget());

        // create components
        Iterator<MapComponent> iter = TakMlFrameworkLifecycle.this.overlays
                .iterator();
        MapComponent c;
        while (iter.hasNext()) {
            c = iter.next();
            try {
                c.onCreate(TakMlFrameworkLifecycle.this.pluginContext,
                        arg0.getIntent(),
                        TakMlFrameworkLifecycle.this.mapView);
            } catch (Exception e) {
                Log.w(TAG,
                        "Unhandled exception trying to create overlays MapComponent",
                        e);
                iter.remove();
            }
        }

        SharedPreferences takmlPrefs = mapView.getContext().getSharedPreferences(SettingsDropDownReceiver.TAK_ML_PREFS, Context.MODE_PRIVATE);
        if(!takmlPrefs.contains(ML_PORT)) {
            SharedPreferences.Editor editor = takmlPrefs.edit();
            editor.putInt(ML_PORT, DEFAULT_ML_PORT_VALUE).commit();
        }
        if(!takmlPrefs.contains(SENSOR_PORT)) {
            SharedPreferences.Editor editor = takmlPrefs.edit();
            editor.putInt(SENSOR_PORT, DEFAULT_SENSOR_PORT_VALUE).commit();
        }
        if(!takmlPrefs.contains(HOST_ADDR)) {
            SharedPreferences.Editor editor = takmlPrefs.edit();
            editor.putString(HOST_ADDR, "127.0.0.1").commit();
        }

        Log.d(TAG, "TAK-ML lifecycle loaded");
    }

    @Override
    public void onDestroy() {
        for (MapComponent c : this.overlays)
            c.onDestroy(this.pluginContext, this.mapView);
    }

    @Override
    public void onFinish() {
        // XXX - no corresponding MapComponent method
    }

    @Override
    public void onPause() {
        for (MapComponent c : this.overlays)
            c.onPause(this.pluginContext, this.mapView);
    }

    @Override
    public void onResume() {
        for (MapComponent c : this.overlays)
            c.onResume(this.pluginContext, this.mapView);
    }

    @Override
    public void onStart() {
        for (MapComponent c : this.overlays)
            c.onStart(this.pluginContext, this.mapView);
    }

    @Override
    public void onStop() {
        for (MapComponent c : this.overlays)
            c.onStop(this.pluginContext, this.mapView);
    }

}
