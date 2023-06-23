package com.atakmap.android.takml_framework.receivers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml_framework.plugin.R;
import com.atakmap.coremap.log.Log;
import com.bbn.takml.framework.ChangeType;
import com.bbn.takml.framework.TakMlFramework;
import com.bbn.takml.mx_framework.MXFramework;
import com.bbn.takml_sdk_android.mx_framework.request.MXDestroyRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InstancesDropDownReceiver extends DropDownReceiver implements OnStateListener {

    public static final String TAG = InstancesDropDownReceiver.class.getSimpleName();
    public static final String SHOW_INSTANCES = "com.atakmap.android.takml_framework.SHOW_INSTANCES";

    private final View instancesView;
    private final Context pluginContext;
    private ArrayAdapter<String> instancesAdapter;
    private TakMlFramework framework;
    private Activity parentActivity;

    private Set<Instance> instancesSet;

    private class Instance {
        String pluginID;
        String mxpInstanceID;

        public Instance(String pluginID, String mxpInstanceID) {
            this.pluginID = pluginID;
            this.mxpInstanceID = mxpInstanceID;
        }

        public boolean equals(Instance i) {
            if (i == null)
                return false;
            return this.pluginID.equals(i.pluginID) && this.mxpInstanceID.equals(i.mxpInstanceID);
        }
    }

    private byte[] serialize(Object o, String msg) {
        byte[] bytes;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(o);
            bytes = bos.toByteArray();
            out.close();
            bos.close();
        } catch (IOException e) {
            Log.e(TAG, msg);
            return null;
        }
        return bytes;
    }

    public InstancesDropDownReceiver(final MapView mapView, final Context context) {
        super(mapView);
        LayoutInflater inflater = (LayoutInflater)context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.instancesView = inflater.inflate(R.layout.instances_layout, null);
        this.parentActivity = (Activity)mapView.getContext();
        this.pluginContext = context;

        List<String> instances = new ArrayList<>();
        this.instancesAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, instances);
        ListView instancesListView = (ListView)instancesView.findViewById(R.id.instancesListView);
        instancesListView.setAdapter(this.instancesAdapter);

        this.instancesSet = new HashSet<Instance>();

        Button destroyAllInstances = (Button)instancesView.findViewById(R.id.destroyAllInstancesButton);
        destroyAllInstances.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MXFramework mxf = null;
                if (framework != null)
                    mxf = framework.getMXFramework();
                if (mxf != null) {
                    for (Instance i : instancesSet) {
                        MXDestroyRequest request = new MXDestroyRequest(i.pluginID, i.mxpInstanceID);
                        byte[] bytes = serialize(request, "Error serializing request to destroy instance " + i.mxpInstanceID);
                        if (bytes == null)
                            continue;
                        mxf.destroy(bytes);
                    }
                }
            }
        });

        Button back = (Button)instancesView.findViewById(R.id.backButton);
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

    public void updateInstancesList(String pluginID, String mxpInstanceID, ChangeType changeType) {
        Log.d(TAG, "Instance change occurred: " + mxpInstanceID + " : " + changeType);
        this.parentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (changeType) {
                    case ADDED:
                        instancesAdapter.add("Instance ID: " + mxpInstanceID + "(plugin " + pluginID + ")");
                        instancesSet.add(new Instance(pluginID, mxpInstanceID));
                        break;
                    case CHANGED:
                        break;
                    case REMOVED:
                        instancesAdapter.remove("Instance ID: " + mxpInstanceID + "(plugin " + pluginID + ")");
                        instancesSet.remove(new Instance(pluginID, mxpInstanceID));
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
        if (intent.getAction().equals(SHOW_INSTANCES)) {
            showDropDown(this.instancesView, HALF_WIDTH, FULL_HEIGHT,
                    FULL_WIDTH, HALF_HEIGHT, false);
        }
    }

}
