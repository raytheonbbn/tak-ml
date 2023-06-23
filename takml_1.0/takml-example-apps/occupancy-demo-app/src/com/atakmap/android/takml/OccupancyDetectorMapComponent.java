
package com.atakmap.android.takml;

import android.content.Context;
import android.content.Intent;

import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.dropdown.DropDownMapComponent;
import com.atakmap.android.takml.occupancy.R;
import com.atakmap.android.takml.receivers.OccupancyDetectorDropDownReceiver;
import com.atakmap.coremap.log.Log;

public class OccupancyDetectorMapComponent extends DropDownMapComponent {

    public static final String TAG = OccupancyDetectorMapComponent.class.getSimpleName();

    public Context pluginContext;
    private OccupancyDetectorDropDownReceiver ddr;

    public void onCreate(final Context context, Intent intent, final MapView view) {

        context.setTheme(R.style.ATAKPluginTheme);
        super.onCreate(context, intent, view);
        pluginContext = context;

        ddr = new OccupancyDetectorDropDownReceiver(view, context);

        Log.d(TAG, "registering the plugin filter");
        DocumentedIntentFilter ddFilter = new DocumentedIntentFilter();
        ddFilter.addAction(OccupancyDetectorDropDownReceiver.SHOW_PLUGIN);
        registerDropDownReceiver(ddr, ddFilter);
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        ddr.disposeImpl();
        super.onDestroyImpl(context, view);
    }
}
