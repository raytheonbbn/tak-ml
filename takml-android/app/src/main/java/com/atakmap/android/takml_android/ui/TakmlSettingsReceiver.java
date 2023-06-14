package com.atakmap.android.takml_android.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.gui.AlertDialogHelper;
import com.atakmap.android.importexport.ImportExportMapComponent;
import com.atakmap.android.importfiles.task.ImportFileTask;
import com.atakmap.android.importfiles.task.ImportFilesTask;
import com.atakmap.android.importfiles.ui.ImportManagerFileBrowser;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml_android.Constants;
import com.atakmap.android.takml_android.R;
import com.atakmap.android.takml_android.Takml;
import com.atakmap.android.takml_android.TakmlModel;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import gov.tak.platform.graphics.Color;

public class TakmlSettingsReceiver extends DropDownReceiver implements DropDown.OnStateListener {
    public static final String TAG = TakmlSettingsReceiver.class.getName();
    public static final String SHOW_PLUGIN = TakmlSettingsReceiver.class.getName() + "_SHOW_PLUGIN";
    private final View takmlView;
    private Intent callbackIntent;
    private final Takml TAKML;
    private final Context pluginContext;

    public TakmlSettingsReceiver(MapView mapView, Context pluginContext, Takml takml) {
        super(mapView);
        this.TAKML = takml;
        this.pluginContext = pluginContext;
        takmlView = PluginLayoutInflater.inflate(pluginContext,
                R.layout.takml_layout, null);
        takmlView.findViewById(R.id.backButton).setOnClickListener(view -> onBackButtonPressed());
    }

    @Override
    protected void disposeImpl() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action == null)
            return;

        if (action.equals(SHOW_PLUGIN + TAKML.getUuid())) {
            Log.d(TAG, "showing plugin drop down");
            showDropDown(takmlView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                    HALF_HEIGHT, false, this);

            // Models ListView
            List<String> takmlModels = new ArrayList<>();
            for(TakmlModel takmlModel : TAKML.getModels()){
                takmlModels.add(takmlModel.getName());
                android.util.Log.d(TAG, "found model: " + takmlModel.getName());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
                    android.R.layout.simple_list_item_1, android.R.id.text1,
                    takmlModels.toArray(new String[0])){

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    TextView text = (TextView) view.findViewById(android.R.id.text1);
                    text.setTextColor(Color.WHITE);
                    return view;
                }
            };
            ListView modelLv = takmlView.findViewById(R.id.model_lv);
            modelLv.setAdapter(adapter);
            setLvTextColorWhite(modelLv);
            setListViewHeightBasedOnChildren(modelLv);
            modelLv.setOnItemClickListener((adapterView, view, i, l) -> {
                String takmlFriendlyName = takmlModels.get(i);
                if(takmlFriendlyName != null) {
                    TakmlModel takmlModel = TAKML.getModel(takmlFriendlyName);
                    if(takmlModel != null) {
                        LayoutInflater inflater = (LayoutInflater) pluginContext
                                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        View modelView = inflater.inflate(R.layout.model_view, null);
                        TextView modelType = modelView.findViewById(R.id.model_type);
                        modelType.setText(takmlModel.getModelType());
                        if(takmlModel.getLabels() != null && !takmlModel.getLabels().isEmpty()) {
                            modelView.findViewById(R.id.model_labels_holder).setVisibility(View.VISIBLE);
                            TextView modelLabels = modelView.findViewById(R.id.model_labels);
                            modelLabels.setText(String.join(", ", takmlModel.getLabels()));
                        }else{
                            modelView.findViewById(R.id.model_labels_holder).setVisibility(View.GONE);
                        }
                        ((TextView) modelView.findViewById(R.id.model_extension_name)).setText(takmlModel.getModelExtension());
                        AlertDialog alertDialog = new AlertDialog.Builder(MapView.getMapView().getContext())
                                .setTitle(takmlFriendlyName)
                                .setView(modelView)
                                .setPositiveButton("OK", (dialog, position) -> dialog.dismiss())
                                .create();
                        alertDialog.show();
                    }
                }
            });

            // Mx Plugins ListView
            List<String> mxPluginNames = new ArrayList<>();
            for(String mxPluginName : TAKML.getModelExecutionPlugins()){
                String result = mxPluginName.substring(mxPluginName.lastIndexOf('.') + 1).trim();
                mxPluginNames.add(result);
                android.util.Log.d(TAG, "found mx plugin: " + result);
            }
            ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(context,
                    android.R.layout.simple_list_item_1, android.R.id.text1,
                    mxPluginNames.toArray(new String[0])) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {

                    View view = super.getView(position, convertView, parent);
                    TextView text = (TextView) view.findViewById(android.R.id.text1);
                    text.setTextColor(Color.WHITE);
                    return view;
                }
            };
            ListView mxPluginsLv = takmlView.findViewById(R.id.mx_plugins_lv);
            mxPluginsLv.setAdapter(adapter2);
            setLvTextColorWhite(mxPluginsLv);
            setListViewHeightBasedOnChildren(mxPluginsLv);

            RelativeLayout addModelBtn = takmlView.findViewById(R.id.addModelBtn);
            addModelBtn.setOnClickListener(view -> {
                addModelBtn.setBackgroundColor(Color.DKGRAY);
                new java.util.Timer().schedule(
                        new java.util.TimerTask() {
                            @Override
                            public void run() {
                                addModelBtn.setBackgroundColor(Color.parseColor("#2b2b2b"));
                            }
                        },
                        100
                );
                LayoutInflater inflater = (LayoutInflater) pluginContext
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View importModelsView = inflater != null ? inflater.inflate(R.layout.list_selection_dialog,
                        null) : null;
                Drawable icon = pluginContext.getDrawable(R.drawable.tak_ml_icon_29_32);
                List<String> options = new ArrayList<>();
                options.add("Import From Disk");
                options.add("Select From Server");
                ModelImportCustomAdapter modelImportCustomAdapter = new ModelImportCustomAdapter(pluginContext, options);
                AlertDialog alertDialog = new AlertDialog.Builder(MapView.getMapView().getContext())
                        .setTitle("Add a TAK ML Model")
                        .setIcon(icon)
                        .setView(importModelsView)
                        .setNegativeButton("Cancel", (dialog, i) -> dialog.dismiss()).
                        create();
                alertDialog.show();
                ListView list = (ListView) importModelsView.findViewById(R.id.selectionList);
                list.setAdapter(modelImportCustomAdapter);
                list.setOnItemClickListener((adapterView, view1, i, l) -> {
                    if(i == 0){
                        alertDialog.dismiss();
                        showATAKImportUtility();
                    }
                });
            });
        }
    }

    private void showATAKImportUtility(){
        final ImportManagerFileBrowser importView = ImportManagerFileBrowser
                .inflate(MapView.getMapView());
        importView.setTitle("Select TAK ML Model Zip File To Import");
        importView.setStartDirectory(
                ATAKUtilities.getStartDirectory(MapView.getMapView().getContext()));
        importView.setExtensionTypes(ImportFilesTask.getSupportedExtensions());
        AlertDialog.Builder b = new AlertDialog.Builder(MapView.getMapView().getContext());
        b.setView(importView);
        b.setNegativeButton("Cancel", null);
        b.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // User has selected items and touched OK. Import the data.
                List<File> selectedFiles = importView.getSelectedFiles();

                if (selectedFiles.size() == 0) {
                    Toast.makeText(MapView.getMapView().getContext(),
                            "No Files Were Selected to Import!",
                            Toast.LENGTH_SHORT).show();
                } else {
                    // Iterate over all of the selected files and begin an import task.
                    for (File file : selectedFiles) {
                        Log.d(TAG, "Importing file: " + file.getAbsolutePath());

                        ImportFileTask importTask = new ImportFileTask(MapView.getMapView()
                                .getContext(), null);
                        importTask
                                .addFlag(ImportFileTask.FlagImportInPlace
                                        | ImportFileTask.FlagValidateExt
                                        | ImportFileTask.FlagPromptOverwrite
                                        | ImportFileTask.FlagPromptOnMultipleMatch
                                        | ImportFileTask.FlagShowNotificationsDuringImport
                                        | ImportFileTask.FlagZoomToFile);
                        importTask.execute(file.getAbsolutePath());
                    }
                }
            }
        });
        final AlertDialog alert = b.create();

        // This also tells the importView to handle the back button presses
        // that the user provides to the alert dialog.
        importView.setAlertDialog(alert);

        // Show the dialog
        alert.show();

        AlertDialogHelper.adjustWidth(alert, 0.90d);
    }

    private void setLvTextColorWhite(ListView listView){
        listView.setCacheColorHint(Color.WHITE);
    }

    private void setListViewHeightBasedOnChildren(ListView myListView) {
        ListAdapter adapter = myListView.getAdapter();
        int totalHeight = 0;
        for (int i = 0; i < adapter.getCount(); i++) {
            View item= adapter.getView(i, null, myListView);
            item.measure(0, 0);
            totalHeight += item.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = myListView.getLayoutParams();
        params.height = totalHeight + (myListView.getDividerHeight() * (adapter.getCount() - 1));
        myListView.setLayoutParams(params);
    }

    @Override
    public void onDropDownSelectionRemoved() {

    }

    @Override
    public void onDropDownClose() {

    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {

    }

    @Override
    public void onDropDownVisible(boolean v) {

    }

    @Override
    protected boolean onBackButtonPressed() {
        closeDropDown();
        if(callbackIntent != null){
            callbackIntent.putExtra(Constants.TAK_ML_UUID, TAKML.getUuid());
            AtakBroadcast.getInstance().sendBroadcast(callbackIntent);
        }
        return false;
    }

    public void setCallbackIntent(Intent callbackIntent) {
        this.callbackIntent = callbackIntent;
    }
}
