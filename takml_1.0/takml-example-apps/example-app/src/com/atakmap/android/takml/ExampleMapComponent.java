
package com.atakmap.android.takml;

import android.content.Context;
import android.content.Intent;

import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.dropdown.DropDownMapComponent;
import com.atakmap.android.takml.example.R;
import com.atakmap.android.takml.receivers.ExampleDropDownReceiver;
import com.atakmap.coremap.log.Log;

public class ExampleMapComponent extends DropDownMapComponent {

    public static final String TAG = ExampleMapComponent.class.getSimpleName();

    public Context pluginContext;
    private ExampleDropDownReceiver ddr;

    public void onCreate(final Context context, Intent intent, final MapView view) {

        context.setTheme(R.style.ATAKPluginTheme);
        super.onCreate(context, intent, view);
        pluginContext = context;

        // Create application's main drop down.
        ddr = new ExampleDropDownReceiver(view, context);

        DocumentedIntentFilter ddFilter = new DocumentedIntentFilter();
        ddFilter.addAction(ExampleDropDownReceiver.SHOW_PLUGIN);
        registerDropDownReceiver(ddr, ddFilter);
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        ddr.disposeImpl();
        super.onDestroyImpl(context, view);
    }
}
