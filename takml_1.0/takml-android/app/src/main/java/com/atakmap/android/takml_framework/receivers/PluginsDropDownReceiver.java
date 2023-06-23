package com.atakmap.android.takml_framework.receivers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageButton;

import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml_framework.TakMlFrameworkPluginExpandable;
import com.atakmap.android.takml_framework.plugin.R;
import com.atakmap.coremap.log.Log;
import com.bbn.tak.ml.mx_framework.MXPluginDescription;
import com.bbn.takml.framework.ChangeType;
import com.bbn.takml.framework.TakMlFramework;
import com.bbn.takml.mx_framework.MXFramework;

public class PluginsDropDownReceiver extends DropDownReceiver implements OnStateListener {

    public static final String TAG = PluginsDropDownReceiver.class.getSimpleName();
    public static final String SHOW_PLUGINS = "com.atakmap.android.takml_framework.SHOW_PLUGINS";

    private final View pluginsView;
    private TakMlFrameworkPluginExpandable pluginsAdapter;
    private TakMlFramework framework;
    private Activity parentActivity;

    public PluginsDropDownReceiver(final MapView mapView, final Context context) {
        super(mapView);
        LayoutInflater inflater = (LayoutInflater)context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.pluginsView = inflater.inflate(R.layout.plugins_layout, null);
        this.parentActivity = (Activity)mapView.getContext();

        ExpandableListView expListView = (ExpandableListView)pluginsView.findViewById(R.id.pluginsListView);
        this.pluginsAdapter = new TakMlFrameworkPluginExpandable(context);
        expListView.setAdapter(this.pluginsAdapter);

        ImageButton refreshPlugins = (ImageButton)pluginsView.findViewById(R.id.refreshPluginsButton);
        refreshPlugins.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Refreshing mx plugins");
                MXFramework mxf = null;
                if (framework != null)
                    mxf = framework.getMXFramework();
                if (mxf != null) {
                    mxf.refreshServerPlugins();
                    mxf.refreshClientPlugins();
                    mxf.listResources();
                }
            }
        });

        Button back = (Button)pluginsView.findViewById(R.id.backButton);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeDropDown();
                Intent nextIntent = new Intent();
                nextIntent.setAction(TakMlFrameworkDropDownReceiver.SHOW_FRAMEWORK_STANDUP);
                AtakBroadcast.getInstance().sendBroadcast(nextIntent);
            }
        });
    }

    public void setFramework(TakMlFramework framework) {
        this.framework = framework;
    }

    public void updatePluginsList(MXPluginDescription desc, ChangeType changeType) {
        Log.d(TAG, "MX change occurred: " + desc.id() + " : " + changeType);
        this.parentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (changeType) {
                    case ADDED:
                    case CHANGED:
                        pluginsAdapter.add(desc);
                        break;
                    case REMOVED:
                        pluginsAdapter.remove(desc);
                        break;
                }
            }
        });
    }

    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownClose() {
    }

    @Override
    public void onDropDownSizeChanged(double v, double v1) {
    }

    @Override
    public void onDropDownVisible(boolean b) {
    }

    @Override
    protected void disposeImpl() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(SHOW_PLUGINS)) {
            showDropDown(this.pluginsView, HALF_WIDTH, FULL_HEIGHT,
                    FULL_WIDTH, HALF_HEIGHT, false);
        }
    }

}
