package com.atakmap.android.takml_android.ui;

import static com.atakmap.android.takml_android.Constants.VERSION_PARAM;
import static com.atakmap.android.takml_android.MetadataConstants.RUN_ON_SERVER;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.gui.AlertDialogHelper;
import com.atakmap.android.importfiles.task.ImportFilesTask;
import com.atakmap.android.importfiles.ui.ImportManagerFileBrowser;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml_android.R;;
import com.atakmap.android.takml_android.Constants;
import com.atakmap.android.takml_android.Takml;
import com.atakmap.android.takml_android.hooks.HookEndpointState;
import com.atakmap.android.takml_android.TakmlModel;
import com.atakmap.android.takml_android.TakmlReceiver;
import com.atakmap.android.takml_android.net.SelectedTAKServer;
import com.atakmap.android.takml_android.net.TakFsManager;
import com.atakmap.android.takml_android.util.IOUtils;
import com.atakmap.android.takml_android.util.TakServerInfo;
import com.atakmap.android.takml_android.util.TakServerUtils;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.coremap.log.Log;
import com.bbn.tak_sync_file_manager.model.IndexRow;

import org.apache.commons.io.FilenameUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import gov.tak.platform.graphics.Color;

public class TakmlSettingsReceiver extends DropDownReceiver implements DropDown.OnStateListener {
    public static final String TAG = TakmlSettingsReceiver.class.getName();
    public static final String SHOW_PLUGIN = TakmlSettingsReceiver.class.getName() + "_SHOW_PLUGIN";
    public static final String IMPORTED_TAKML_MODEL = TakmlSettingsReceiver.class.getName() + "_IMPORTED_TAKML_MODEL";
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
            loadUI(context);
        }else if (action.equals(IMPORTED_TAKML_MODEL + TAKML.getUuid())) {
            if(isVisible()) {
                loadUI(context);
            }
        }
    }

    private void loadUI(Context context){
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
                    TextView modelVersion = modelView.findViewById(R.id.model_version);
                    modelVersion.setText(String.valueOf(takmlModel.getVersionNumber()));
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
                }else if(i == 1){
                    alertDialog.dismiss();
                    showSelectTAKServerMaybeAndDownloadView();
                }
            });
        });

        HookEndpointState info = TAKML.gethooksInfo();
        updateMediatorStatus(info);
    }

    private void updateMediatorStatus(HookEndpointState info) {
        TextView statusText = takmlView.findViewById(R.id.status_text);
        View statusIndicator = takmlView.findViewById(R.id.status_indicator);

        if (info == HookEndpointState.REGISTERED) {
            statusText.setText("Connected to Mediator");
            statusIndicator.setBackgroundResource(R.drawable.status_circle_connected);
        } else {
            statusText.setText("Disconnected from Mediator");
            statusIndicator.setBackgroundResource(R.drawable.status_circle_disconnected);
        }
    }

    private void showATAKImportUtility(){
        final ImportManagerFileBrowser importView = ImportManagerFileBrowser
                .inflate(MapView.getMapView());
        importView.setTitle("Select Settings File");
        importView.setStartDirectory(
                ATAKUtilities.getStartDirectory(MapView.getMapView().getContext()));
        importView.setExtensionTypes(ImportFilesTask.getSupportedExtensions());
        AlertDialog.Builder b = new AlertDialog.Builder(MapView.getMapView().getContext());
        b.setView(importView);
        b.setNegativeButton("Cancel", null);
        b.setPositiveButton("Ok", (dialog, which) -> AsyncTask.execute(() -> {
            List<File> selectedFiles = importView.getSelectedFiles();
            // TODO: use your selected file
        }));
        final AlertDialog alert = b.create();
        importView.setAlertDialog(alert);
        alert.show();
        AlertDialogHelper.adjustWidth(alert, 0.90d);
    }

    private void showSelectTAKServerMaybeAndDownloadView(){
        TakServerInfo takServerInfo = SelectedTAKServer.getInstance().getTakServerInfo();
        if(takServerInfo == null){
            TakServerUtils.getOrSelectNetwork((success, takServerInf) -> {
                if (!success) {
                    Log.e(TAG, "Tak Server information is null");
                }else{
                    SelectedTAKServer.getInstance().setTAkServer(takServerInf);
                    showDownloadView(takServerInf);
                }
            });
        }else {
            showDownloadView(takServerInfo);
        }
    }

    private void showDownloadView(TakServerInfo takServerInfo){
        Log.d(TAG, "networkSelected: " + takServerInfo.getTakServer().getConnectString());
        TakFsManager.getInstance().initialize(takServerInfo.getTakserverApiClient());

        AsyncTask.execute(() -> {
            Set<IndexRow> indexRows = TakFsManager.getInstance().getModels();

            ((Activity) MapView.getMapView().getContext()).runOnUiThread(() -> {
                LayoutInflater inflater = (LayoutInflater) pluginContext
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                ScrollView scrollView = (ScrollView) inflater.inflate(R.layout.download_models_table,
                        null);
                TableLayout tableLayout = scrollView.findViewById(R.id.tableDownloadModels);
                AlertDialog.Builder b = new AlertDialog.Builder(MapView.getMapView().getContext());
                b.setView(scrollView);
                b.setTitle("Select TAK ML Model to download");
                b.setNegativeButton("Cancel",null);
                Dialog dialog = b.create();
                for(IndexRow indexRow : indexRows){
                    Map<String, String> additionalMetadata = indexRow.getAdditionalMetadata();
                    if(additionalMetadata == null){
                        continue;
                    }

                    double version = 1;
                    try {
                        String versionStr = additionalMetadata.get(VERSION_PARAM);
                        if(versionStr != null) {
                            version = Double.parseDouble(versionStr);
                        }
                    } catch (NumberFormatException e){
                        Log.e(TAG, "NumberFormatException parsing version for takml model: " + indexRow.getName(), e);
                    }
                    boolean newVersionAvailable = false;
                    for(TakmlModel takmlModel : TAKML.getModels()){
                        if(indexRow.getName().equals(takmlModel.getName())){
                            if(version > takmlModel.getVersionNumber()) {
                                newVersionAvailable = true;
                            }
                            break;
                        }
                    }
                    String runOnServerString = additionalMetadata.get(RUN_ON_SERVER);
                    if (Boolean.parseBoolean(runOnServerString)) {
                        continue;
                    }
                    TableRow tableRow = (TableRow) inflater.inflate(R.layout.download_models_row, null);
                    String modelName = indexRow.getName();
                    if(modelName.length() > 14){
                        modelName = modelName.substring(0, 11) + "…";
                    }
                    if(newVersionAvailable){
                        ((TextView) tableRow.findViewById(R.id.modelName)).setText(modelName + " (New Version!)");
                    }else {
                        ((TextView) tableRow.findViewById(R.id.modelName)).setText(modelName);
                    }

                    ((TextView) tableRow.findViewById(R.id.version)).setText(String.valueOf(version));

                    String supportedDevices = indexRow.getAdditionalMetadata().get("SUPPORTED_DEVICES");
                    if(supportedDevices != null){
                        ((TextView)tableRow.findViewById(R.id.modelType)).setText(supportedDevices);
                    }

                    ((TextView)tableRow.findViewById(R.id.modelType)).setText(indexRow.getAdditionalMetadata().get("MODEL_TYPE"));
                    tableRow.findViewById(R.id.modelDownloadBtn).setOnClickListener(view -> {
                        dialog.dismiss();
                        //
                        ((Activity) MapView.getMapView().getContext()).runOnUiThread(() -> {
                            Toast.makeText(pluginContext, "Downloading model...", Toast.LENGTH_SHORT).show();
                        });
                        TakFsManager.getInstance().downloadModel(indexRow, file -> {
                            AsyncTask.execute(() -> importTakmlModel(file));
                        });
                    });
                    tableLayout.addView(tableRow);
                }
                dialog.show();
                dialog.getWindow().setLayout(
                        (int)(MapView.getMapView().getContext().getResources().getDisplayMetrics().widthPixels * 0.9),
                        (int)(MapView.getMapView().getContext().getResources().getDisplayMetrics().heightPixels * 0.8)
                );
            });
        });
    }

    private void importTakmlModel(File takmlModelZip){
        // Remove existing takml model
        String path = Environment.getExternalStorageDirectory()
                + File.separator + "atak" + File.separator + "tools" + File.separator
                + "datapackage" + File.separator + takmlModelZip.getName();
        File missionPackageFile = new File(path);
        if(missionPackageFile.exists()){
            Log.d(TAG, "trying to remove: " + path);
            if(missionPackageFile.delete()){
                Log.d(TAG, "deleted: " + path);
            }
        }
        String path2 = Constants.TAKML_MP_STORAGE_DIR + File.separator +
                FilenameUtils.removeExtension(takmlModelZip.getName());
        File takmlModel = new File(path2);
        if(takmlModel.exists()){
            Log.d(TAG, "trying to remove: " + path2);
            IOUtils.deleteDir(takmlModel);
        }

        if(missionPackageFile.exists()){
            Log.e(TAG, "could not remove mission package: " + path);
        }
        if(takmlModel.exists()){
            Log.e(TAG, "could not remove mission package: " + path2);
        }

        // [friendly name of TAK ML model]/[name of takml zip folder]/[model files]
        //            ^ this
        File targetFolder = new File(((Activity) MapView.getMapView().getContext()).getCacheDir(), "tmp" + UUID.randomUUID());
        if(targetFolder.mkdir()){
            Log.d(TAG, "Created storage location for TAK ML model: " + takmlModelZip.getName());
        }
        try {
            unzip(takmlModelZip, targetFolder);
        } catch (IOException e) {
            Log.e(TAG, "IOException unzipping takml model: " + takmlModelZip.getName(), e);
            return;
        }

        File[] unzippedParent = targetFolder.listFiles();
        if(unzippedParent == null){
            Log.e(TAG, "TAKML ZIP file model does not contain any files: "
                    + takmlModelZip.getName());
            return;
        }

        // [friendly name of TAK ML model]/[name of takml zip folder]/[model files]
        //                                            ^ this
        File unzipedFile = null;
        for(File file : unzippedParent){
            unzipedFile = file;
        }
        if(unzipedFile == null){
            Log.e(TAG, "TAKML ZIP file model does not contain takml zip folder: "
                    + takmlModelZip.getName());
            return;
        }

        Intent intent = new Intent();
        intent.setAction(TakmlReceiver.IMPORT_TAKML_MODEL);
        intent.putExtra(Constants.TAK_ML_UUID, TAKML.getUuid().toString());
        intent.putExtra(Constants.TAKML_MODEL_PATH,
                unzipedFile.getPath() + File.separator + Constants.TAKML_CONFIG_FILE);
        AtakBroadcast.getInstance().sendBroadcast(intent);
    }

    public static void unzip(File zipFile, File targetDirectory) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(Files.newInputStream(zipFile.toPath())))) {
            ZipEntry ze;
            int count;
            byte[] buffer = new byte[8192];
            while ((ze = zis.getNextEntry()) != null) {
                File file = new File(targetDirectory, ze.getName());
                File dir = ze.isDirectory() ? file : file.getParentFile();
                if(dir == null){
                    Log.e(TAG, "unzip takml model, could not create directory");
                    return;
                }
                if (!dir.isDirectory() && !dir.mkdirs())
                    throw new FileNotFoundException("Failed to ensure directory: " +
                            dir.getAbsolutePath());
                if (ze.isDirectory())
                    continue;
                try (FileOutputStream fout = new FileOutputStream(file)) {
                    while ((count = zis.read(buffer)) != -1)
                        fout.write(buffer, 0, count);
                }
            }
        }
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
