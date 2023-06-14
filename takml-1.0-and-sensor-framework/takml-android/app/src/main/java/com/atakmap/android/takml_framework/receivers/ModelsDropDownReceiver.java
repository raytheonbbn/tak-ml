package com.atakmap.android.takml_framework.receivers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;

import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml_framework.plugin.R;
import com.atakmap.coremap.log.Log;
import com.bbn.tak.ml.mx_framework.Model;
import com.bbn.takml.framework.ChangeType;
import com.bbn.takml.framework.TakMlFramework;
import com.bbn.takml.mx_framework.MXFramework;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModelsDropDownReceiver extends DropDownReceiver implements OnStateListener {

    public static final String TAG = ModelsDropDownReceiver.class.getSimpleName();
    public static final String SHOW_MODELS = "com.atakmap.android.takml_framework.SHOW_MODELS";

    private final View modelsView;
    private final Context pluginContext;
    private ArrayAdapter<String> modelsAdapter;
    private TakMlFramework framework;
    private Activity parentActivity;

    private Set<String> serverModels;
    private ArrayAdapter<String> pullModelAdapter;
    private Spinner pullModelDropdown;

    private void updateServerModels() {
        String[] modelStrings = new String[serverModels.size()];
        serverModels.toArray(modelStrings);
        pullModelAdapter = new ArrayAdapter<String>(this.pluginContext,
                android.R.layout.simple_spinner_item, modelStrings);
        pullModelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pullModelDropdown.setAdapter(pullModelAdapter);
        pullModelAdapter.notifyDataSetChanged();
    }

    public ModelsDropDownReceiver(final MapView mapView, final Context context) {
        super(mapView);
        LayoutInflater inflater = (LayoutInflater)context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.modelsView = inflater.inflate(R.layout.models_layout, null);
        this.parentActivity = (Activity)mapView.getContext();
        this.pluginContext = context;

        List<String> models = new ArrayList<>();
        this.modelsAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, models);
        ListView modelsListView = (ListView)modelsView.findViewById(R.id.modelsListView);
        modelsListView.setAdapter(this.modelsAdapter);

        this.serverModels = new HashSet<String>();
        this.pullModelDropdown = (Spinner)modelsView.findViewById(R.id.modelValue);
        updateServerModels();

        Button refreshModels = (Button)modelsView.findViewById(R.id.refreshModelsButton);
        refreshModels.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MXFramework mxf = null;
                if (framework != null)
                    mxf = framework.getMXFramework();
                if (mxf != null) {
                    mxf.refreshServerModels();
                    mxf.refreshClientModels();
                }
            }
        });

        Button pullModel = (Button)modelsView.findViewById(R.id.pullModelButton);
        pullModel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String modelName = pullModelDropdown.getSelectedItem().toString();
                MXFramework mxf = null;
                if (framework != null)
                    mxf = framework.getMXFramework();
                if (mxf != null) {
                    mxf.pullModel(modelName);
                }
            }
        });

        Button back = (Button)modelsView.findViewById(R.id.backButton);
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

    public void updateModelList(String modelLabel, Model m, ChangeType changeType) {
        Log.d(TAG, "Model change occurred: " + modelLabel + " : " + changeType);
        this.parentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Don't include metadata, for now.
                switch (changeType) {
                    case ADDED:
                        modelsAdapter.add(modelLabel);
                        if (m.getSource().equals("Server")) {
                            serverModels.add(m.getName());
                        }
                        break;
                    case CHANGED:
                        // No effect (except to change position) right now, but the
                        // metadata could change, and we will show the metadata in the future.
                        modelsAdapter.remove(modelLabel);
                        modelsAdapter.add(modelLabel);
                        if (m.getSource().equals("Server")) {
                            serverModels.remove(m.getName());
                            serverModels.add(m.getName());
                        }
                        break;
                    case REMOVED:
                        modelsAdapter.remove(modelLabel);
                        if (m.getSource().equals("Server")) {
                            serverModels.remove(m.getName());
                        }
                        break;
                }

                updateServerModels();
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
        if (intent.getAction().equals(SHOW_MODELS)) {
            showDropDown(this.modelsView, HALF_WIDTH, FULL_HEIGHT,
                    FULL_WIDTH, HALF_HEIGHT, false);
        }
    }

}
