
package com.atakmap.android.takml_framework.receivers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;

import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml_framework.TakMlFrameworkMapOverlay;
import com.atakmap.android.takml_framework.plugin.R;
import com.atakmap.android.toolbar.widgets.TextContainer;
import com.atakmap.coremap.log.Log;
import com.bbn.tak.ml.TakMlConstants;
import com.bbn.takml.framework.TakMlFramework;

import org.eclipse.paho.client.mqttv3.MqttClient;


/**
 * The DropDown Receiver should define the visual experience
 * that a user might have while using this plugin.   At a
 * basic level, the dropdown can be a view of your own design
 * that is inflated.   Please be wary of the type of context
 * you use.   As noted in the Map Component, there are two
 * contexts - the plugin context and the atak context.
 * When using the plugin context - you cannot build thing or
 * post things to the ui thread.   You use the plugin context
 * to lookup resources contained specifically in the plugin.
 */
public class TakMlFrameworkDropDownReceiver extends DropDownReceiver implements OnStateListener {

    public static final String TAG = "TakMlFrameworkStandupDropDownReceiver";

    public static final String SHOW_FRAMEWORK_STANDUP = "com.atakmap.android.takml_framework.SHOW_FRAMEWORK_STANDUP";
    private final View takmlFrameworkStandupView;

    public static MqttClient client;
    public static final String TAK_ML_API_listening_address = "tcp://localhost:" + Integer.toString(TakMlConstants.TAK_ML_LISTENER_DEFAULT_PORT);

    private final MapView parentView;
    private final Context pluginContext;

    private TakMlFrameworkDropDownReceiver self;

    private TakMlFramework framework;
    private Activity parentActivity;

    /**************************** CONSTRUCTOR *****************************/

    public TakMlFrameworkDropDownReceiver(final MapView mapView, final Context context,
                                          TakMlFrameworkMapOverlay overlay,
                                          SensorsDropDownReceiver sddr,
                                          PluginsDropDownReceiver pddr,
                                          ModelsDropDownReceiver mddr,
                                          InstancesDropDownReceiver iddr,
                                          DataDropDownReceiver dddr,
                                          SettingsDropDownReceiver stddr) {
        super(mapView);

        this.parentView = mapView;
        this.pluginContext = context;
        this.self = this;
        this.parentActivity = (Activity)mapView.getContext();

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        takmlFrameworkStandupView = inflater.inflate(R.layout.takml_framework_layout, null);

        initializeFramework(sddr, pddr, mddr, iddr, dddr, stddr);

        Button sensors = (Button)takmlFrameworkStandupView.findViewById(R.id.sensors);
        sensors.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeDropDown();
                Intent nextIntent = new Intent();
                nextIntent.setAction(SensorsDropDownReceiver.SHOW_SENSORS);
                AtakBroadcast.getInstance().sendBroadcast(nextIntent);
            }
        });

        Button plugins = (Button)takmlFrameworkStandupView.findViewById(R.id.plugins);
        plugins.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeDropDown();
                Intent nextIntent = new Intent();
                nextIntent.setAction(PluginsDropDownReceiver.SHOW_PLUGINS);
                AtakBroadcast.getInstance().sendBroadcast(nextIntent);
            }
        });

        Button models = (Button)takmlFrameworkStandupView.findViewById(R.id.models);
        models.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeDropDown();
                Intent nextIntent = new Intent();
                nextIntent.setAction(ModelsDropDownReceiver.SHOW_MODELS);
                AtakBroadcast.getInstance().sendBroadcast(nextIntent);
            }
        });

        Button instances = (Button)takmlFrameworkStandupView.findViewById(R.id.instances);
        instances.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeDropDown();
                Intent nextIntent = new Intent();
                nextIntent.setAction(InstancesDropDownReceiver.SHOW_INSTANCES);
                AtakBroadcast.getInstance().sendBroadcast(nextIntent);
            }
        });

        Button dataBtn = (Button)takmlFrameworkStandupView.findViewById(R.id.data);
        dataBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Data button clicked");
                closeDropDown();
                Intent nextIntent = new Intent();
                nextIntent.setAction(DataDropDownReceiver.SHOW_DATA);
                AtakBroadcast.getInstance().sendBroadcast(nextIntent);
            }
        });

        Button settings = (Button)takmlFrameworkStandupView.findViewById(R.id.settings);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeDropDown();
                Intent nextIntent = new Intent();
                nextIntent.setAction(SettingsDropDownReceiver.SHOW_SETTINGS);
                AtakBroadcast.getInstance().sendBroadcast(nextIntent);
            }
        });

        //============================================================
        //  Switch to stand up the TAKML Framework
        //============================================================

        Switch takmlEnabledSwitch = takmlFrameworkStandupView.findViewById(R.id.takmlEnabled);
        takmlEnabledSwitch.setOnCheckedChangeListener((btn, isChecked) ->
        {
            if (isChecked) {
                initializeFramework(sddr, pddr, mddr, iddr, dddr, stddr);
            } else {
                framework.stopFramework();
                pddr.setFramework(null);
                mddr.setFramework(null);
                iddr.setFramework(null);
                stddr.setFramework(null);
                dddr.setFramework(null);
                sddr.setFramework(null);
            }
        });

    }

    private void initializeFramework(SensorsDropDownReceiver sddr, PluginsDropDownReceiver pddr,
                                     ModelsDropDownReceiver mddr, InstancesDropDownReceiver iddr,
                                     DataDropDownReceiver dddr, SettingsDropDownReceiver stddr) {
        framework = new TakMlFramework(pluginContext,
                sddr::updateSensorList,
                pddr::updatePluginsList,
                mddr::updateModelList,
                iddr::updateInstancesList);

        framework.startFramework();

        pddr.setFramework(this.framework);
        mddr.setFramework(this.framework);
        iddr.setFramework(this.framework);
        dddr.setFramework(this.framework);
        stddr.setFramework(this.framework);
        sddr.setFramework(this.framework);
    }


    /**************************** PUBLIC METHODS *****************************/

    @Override
    public void disposeImpl() {
        framework.stopFramework();
        TextContainer.getTopInstance().closePrompt();
    }

    /**************************** INHERITED METHODS *****************************/

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "showing TAK-ML Framework Standup drop down");
		// Show drop-down
        if (intent.getAction().equals(SHOW_FRAMEWORK_STANDUP)) {
            showDropDown(takmlFrameworkStandupView, HALF_WIDTH, FULL_HEIGHT,
                    FULL_WIDTH, HALF_HEIGHT, false, this);
            setAssociationKey("takmlFrameworkStandupPreference");
        }				 
    }

    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownVisible(boolean v) {
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
    }

    @Override
    public void onDropDownClose() {
    }

}
