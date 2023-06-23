
package com.atakmap.android.takml_framework;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.atakmap.android.cot.UIDHandler;
import com.atakmap.android.dropdown.DropDownMapComponent;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapEventDispatcher.MapEventDispatchListener;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.takml_framework.plugin.R;
import com.atakmap.android.takml_framework.receivers.DataDropDownReceiver;
import com.atakmap.android.takml_framework.receivers.InstancesDropDownReceiver;
import com.atakmap.android.takml_framework.receivers.ModelsDropDownReceiver;
import com.atakmap.android.takml_framework.receivers.PluginsDropDownReceiver;
import com.atakmap.android.takml_framework.receivers.SensorsDropDownReceiver;
import com.atakmap.android.takml_framework.receivers.SettingsDropDownReceiver;
import com.atakmap.android.takml_framework.receivers.TakMlFrameworkDropDownReceiver;
import com.atakmap.android.user.geocode.GeocodeManager;
import com.atakmap.app.preferences.ToolsPreferenceFragment;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.net.DeviceProfileClient;


import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import de.mindpipe.android.logging.log4j.LogConfigurator;

/**
 * This is an example of a MapComponent within the ATAK 
 * ecosphere.   A map component is the building block for all
 * activities within the system.   This defines a concrete 
 * thought or idea. 
 */
public class TakMlFrameworkMapComponent extends DropDownMapComponent {

    public static final String TAG = "TakMlFrameworkStandupMapComponent";

    private Context pluginContext;
    private TakMlFrameworkDropDownReceiver dropDown;
    private TakMlFrameworkMapOverlay mapOverlay;

    public void configLog() {
        try {
            final LogConfigurator logConfigurator = new LogConfigurator();

            logConfigurator.setFileName(Environment.getExternalStorageDirectory() + File.separator
                    + "log4j.log");

            logConfigurator.configure();
        } catch (Exception e) {
            Log.i(TAG, "configLog: " + e);
        }
    }


    @Override
    public void onStart(final Context context, final MapView view) {
        Log.d(TAG, "onStart");
    }

    @Override
    public void onPause(final Context context, final MapView view) {
        Log.d(TAG, "onPause");
    }

    @Override
    public void onResume(final Context context,
            final MapView view) {
        Log.d(TAG, "onResume");
    }

    @Override
    public void onStop(final Context context,
            final MapView view) {
        Log.d(TAG, "onStop");
    }

    public void onCreate(final Context context, Intent intent,
            final MapView view) {
        //BasicConfigurator.configure();
        configLog();

        // Set the theme.  Otherwise, the plugin will look vastly different
        // than the main ATAK experience.   The theme needs to be set 
        // programatically because the AndroidManifest.xml is not used.
        context.setTheme(R.style.ATAKPluginTheme);

        super.onCreate(context, intent, view);
        pluginContext = context;

        this.mapOverlay = new TakMlFrameworkMapOverlay(view, pluginContext);
        view.getMapOverlayManager().addOverlay(this.mapOverlay);

        SensorsDropDownReceiver sddr = new SensorsDropDownReceiver(view, pluginContext);
        registerReceiverUsingPluginContext(pluginContext, "sensors",
                sddr, SensorsDropDownReceiver.SHOW_SENSORS);

        PluginsDropDownReceiver pddr = new PluginsDropDownReceiver(view, pluginContext);
        registerReceiverUsingPluginContext(pluginContext, "plugins",
                pddr, PluginsDropDownReceiver.SHOW_PLUGINS);

        ModelsDropDownReceiver mddr = new ModelsDropDownReceiver(view, pluginContext);
        registerReceiverUsingPluginContext(pluginContext, "models",
                mddr, ModelsDropDownReceiver.SHOW_MODELS);

        InstancesDropDownReceiver iddr = new InstancesDropDownReceiver(view, pluginContext);
        registerReceiverUsingPluginContext(pluginContext, "instances",
                iddr, InstancesDropDownReceiver.SHOW_INSTANCES);

        DataDropDownReceiver dddr = new DataDropDownReceiver(view, pluginContext);
        registerReceiverUsingPluginContext(pluginContext, "data",
                dddr, DataDropDownReceiver.SHOW_DATA);

        SettingsDropDownReceiver stddr = new SettingsDropDownReceiver(view, pluginContext);
        registerReceiverUsingPluginContext(pluginContext, "settings",
                stddr, SettingsDropDownReceiver.SHOW_SETTINGS);

        // In this example, a drop down receiver is the 
        // visual component within the ATAK system.  The 
        // trigger for this visual component is an intent.   
        // see the plugin.TakMlFrameworkStandupTool where that intent
        // is triggered.
        this.dropDown = new TakMlFrameworkDropDownReceiver(view, context, this.mapOverlay,
                sddr, pddr, mddr, iddr, dddr, stddr);

        // We use documented intent filters within the system
        // in order to automatically document all of the 
        // intents and their associated purposes.
        Log.d(TAG, "registering the show framework standup filter");
        DocumentedIntentFilter ddFilter = new DocumentedIntentFilter();
        ddFilter.addAction(TakMlFrameworkDropDownReceiver.SHOW_FRAMEWORK_STANDUP,
                "Show the TAK-ML Framework Standup drop-down");
        this.registerDropDownReceiver(this.dropDown, ddFilter);
        Log.d(TAG, "registered the show framework standup filter");

        // in this case we also show how one can register
        // additional information to the uid detail handle when 
        // generating cursor on target.   Specifically the 
        // NETT-T service specification indicates the the 
        // details->uid should be filled in with an appropriate
        // attribute.   

        // add in the nett-t required uid entry.
        UIDHandler.getInstance().addAttributeInjector(
                new UIDHandler.AttributeInjector() {
                    public void injectIntoDetail(Marker marker,
                            CotDetail detail) {
                        if (marker.getType().startsWith("a-f"))
                            return;
                        detail.setAttribute("nett", "XX");
                    }

                    public void injectIntoMarker(CotDetail detail,
                            Marker marker) {
                        if (marker.getType().startsWith("a-f"))
                            return;
                        String callsign = detail.getAttribute("nett");
                        if (callsign != null)
                            marker.setMetaString("nett", callsign);
                    }

                });

        // In order to use shared preferences with a plugin you will need
        // to use the context from ATAK since it has the permission to read
        // and write preferences.
        // Additionally - in the XML file you cannot use PreferenceCategory
        // to enclose your Prefences - otherwise the preference will not
        // be persisted.   You can fake a PreferenceCategory by adding an
        // empty preference category at the top of each group of preferences.
        // See how this is done in the current example.

        //see if any TAK-ML Framework Standup profiles/data are available on the TAK Server. Requires the server to be
        //properly configured, and "Apply TAK Server profile updates" setting enabled in ATAK prefs
        Log.d(TAG, "Checking for TAK-ML Framework Standup profile on TAK Server");
        DeviceProfileClient.getInstance().getProfile(view.getContext(),
                "takml_framework");

        //register profile request to run upon connection to TAK Server, in case we're not yet
        //connected, or the the request above fails
        //CotMapComponent.getInstance().addToolProfileRequest("takml_framework");

        registerSpisVisibilityListener(view);

        view.addOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent event) {
                Log.d(TAG, "dispatchKeyEvent: " + event.toString());
                return false;
            }
        });

        GeocodeManager.getInstance(context).registerGeocoder(fakeGeoCoder);

        TextView tv = new TextView(context);
        LayoutParams lp_tv = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        lp_tv.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        tv.setText("Test Center Layout");
        tv.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "Test Test Test");
            }
        });
        com.atakmap.android.video.VideoDropDownReceiver.registerVideoViewLayer(
                new com.atakmap.android.video.VideoViewLayer("test-layer", tv,
                        lp_tv));

    }

    private void registerReceiverUsingPluginContext(Context pluginContext, String name, DropDownReceiver rec, String actionName) {
        DocumentedIntentFilter mainIntentFilter = new DocumentedIntentFilter();
        mainIntentFilter.addAction(actionName);
        this.registerReceiver(pluginContext, rec, mainIntentFilter);
    }

    public GeocodeManager.Geocoder fakeGeoCoder = new GeocodeManager.Geocoder() {
        @Override
        public String getUniqueIdentifier() {
            return "fake-geocoder";
        }

        @Override
        public String getTitle() {
            return "Gonna get you Lost";
        }

        @Override
        public String getDescription() {
            return "Sample Geocoder implementation registered with TAK";
        }

        @Override
        public boolean testServiceAvailable() {
            return true;
        }

        @Override
        public List<Address> getLocation(GeoPoint geoPoint) {
            Address a = new Address(Locale.getDefault());
            a.setAddressLine(0, "100 WrongWay Street");
            a.setAddressLine(1, "Boondocks, Nowhere");
            a.setCountryCode("UNK");
            a.setPostalCode("999999");
            a.setLatitude(geoPoint.getLatitude());
            a.setLongitude(geoPoint.getLongitude());
            return new ArrayList<>(Collections.singleton(a));
        }

        @Override
        public List<Address> getLocation(String s, GeoBounds geoBounds) {
            Address a = new Address(Locale.getDefault());
            a.setAddressLine(0, "100 WrongWay Street");
            a.setAddressLine(1, "Boondocks, Nowhere");
            a.setCountryCode("UNK");
            a.setPostalCode("999999");
            a.setLatitude(0);
            a.setLongitude(0);
            return new ArrayList<>(Collections.singleton(a));
        }
    };

    private void registerSpisVisibilityListener(MapView view) {
        spiListener = new SpiListener(view);
        for (int i = 0; i < 4; ++i) {
            MapItem mi = view
                    .getMapItem(view.getSelfMarker().getUID() + ".SPI" + i);
            if (mi != null) {
                mi.addOnVisibleChangedListener(spiListener);
            }
        }

        final MapEventDispatcher dispatcher = view.getMapEventDispatcher();
        dispatcher.addMapEventListener(MapEvent.ITEM_REMOVED, spiListener);
        dispatcher.addMapEventListener(MapEvent.ITEM_ADDED, spiListener);

    }

    SpiListener spiListener;

    private class SpiListener implements MapEventDispatchListener,
            MapItem.OnVisibleChangedListener {
        private final MapView view;

        public SpiListener(MapView view) {
            this.view = view;
        }

        @Override
        public void onMapEvent(MapEvent event) {
            MapItem item = event.getItem();
            if (item == null)
                return;
            if (event.getType().equals(MapEvent.ITEM_ADDED)) {
                if (item.getUID()
                        .startsWith(view.getSelfMarker().getUID() + ".SPI")) {
                    item.addOnVisibleChangedListener(this);
                    Log.d(TAG, "visibility changed for: " + item.getUID() + " "
                            + item.getVisible());
                }
            } else if (event.getType().equals(MapEvent.ITEM_REMOVED)) {
                if (item.getUID()
                        .startsWith(view.getSelfMarker().getUID() + ".SPI"))
                    item.removeOnVisibleChangedListener(this);
            }
        }

        @Override
        public void onVisibleChanged(MapItem item) {
            Log.d(TAG, "visibility changed for: " + item.getUID() + " "
                    + item.getVisible());
        }
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        Log.d(TAG, "calling on destroy");
        //GLMapItemFactory.unregisterSpi(GLSpecialMarker.SPI);
        //this.dropDown.dispose();
        ToolsPreferenceFragment.unregister("takmlFrameworkStandupPreference");
        //RadioMapComponent.getInstance().unregisterControl(genericRadio);
        view.getMapOverlayManager().removeOverlay(mapOverlay);
        super.onDestroyImpl(context, view);
    }

}
