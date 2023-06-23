package com.atakmap.android.takml.receivers;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml.network.NetworkClient;
import com.atakmap.android.takml.plugin.R;
import com.atakmap.android.takml.takml.ClassificationEntity;
import com.atakmap.coremap.log.Log;
import com.bbn.takml_sdk_android.mx_framework.request.MXExecuteReply;
import com.bbn.takml_sdk_android.mx_framework.Recognition;

import org.apache.commons.lang3.SerializationUtils;

import java.util.ArrayList;

public class ResultReceiver extends ViewTableReceiver  implements
        DropDown.OnStateListener {
    public static final String TAG = PluginTemplateDropDownReceiver.class
            .getSimpleName();

    public static final String SHOW_RESULT_DROPDOWN = "com.atakmap.android.plugintemplate.SHOW_RESULT_DROPDOWN";

    private final View resultView;
    private final Context pluginContext;
    private final MapView mapView;
    private Button backButton, refreshButton;

    private Intent intent;
    private byte[] imageBytes;
    private ArrayList<Recognition> classificationResult;
    private GridLayout table, edibleTable;

    private NetworkClient networkClient;
    private String executeID;

    private ProgressBar progressBar;
    private TextView progressText;

    public ResultReceiver(final MapView mapView,
                             final Context context, NetworkClient networkClient) {
        super(mapView, context);
        this.pluginContext = context;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        resultView = inflater.inflate(R.layout.result_layout, null);
        this.mapView = mapView;
        this.networkClient = networkClient;
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
        Log.d(TAG, "showing result receiver");
        if (intent.getAction().equals(SHOW_RESULT_DROPDOWN)) {

            this.intent = intent;

            progressBar = resultView.findViewById(R.id.indeterminate_bar);
            progressText = resultView.findViewById(R.id.progress_text);

            showDropDown(resultView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                    HALF_HEIGHT, false);

            // RETURN TO SELECT IMAGE
            backButton = resultView.findViewById(R.id.backButton);
            backButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    closeDropDown();
                    Intent intent1 = new Intent();
                    intent1.setAction(PluginTemplateDropDownReceiver.SHOW_PLUGIN);
                    AtakBroadcast.getInstance().sendBroadcast(intent1);
                }
            });

            ClassificationEntity classificationEntity = ClassificationEntity.getInstance();

            if(null != classificationEntity.getClassificationResults() && null != classificationEntity.getImage()){
                classificationResult = classificationEntity.getClassificationResults();
                imageBytes = classificationEntity.getImage();

                displayResults(classificationResult);
            }
        }
    }

    public void displayResults(ArrayList<Recognition> classificationResult){
        GridLayout.LayoutParams nameParams = newLayoutParams();
        GridLayout.LayoutParams typeParams = newLayoutParams();

        table = (GridLayout) resultView.findViewById(R.id.results_table);
        table.setColumnCount(2);

        // REMOVE RESIDUALS FROM LAST TIME WE DISPLAYED
        table.removeAllViews();


        // DISPLAY THE TOP THREE RESULTS
        for (int i = 0; i < 3; i++) {
            String id_ = "Prediction: " + classificationResult.get(i).getId();
            String class_ = "Score: " + classificationResult.get(i).getConfidence();

            TextView className = createTableEntry("", MED_DARK_GRAY);
            className.setText(id_);
            TextView classification = createTableEntry("", MED_DARK_GRAY);
            classification.setText(class_);

            table.addView(className, nameParams);
            table.addView(classification, typeParams);

            nameParams = newLayoutParams();
            typeParams = newLayoutParams();
        }
    }

    public void predictionResponse(MXExecuteReply reply) {
        if (!reply.getExecuteID().equals(this.executeID)) {
            Log.e(TAG, "Error: execute ID does not match expected value");
            return;
        }

        byte[] output = reply.getBytes();
        try {
            classificationResult = SerializationUtils.deserialize(output);
        } catch (Exception e) {
            Log.e(TAG, "Error deserializing response: " + e.getMessage());
            e.printStackTrace();
        }

        ClassificationEntity.getInstance().setClassificationResults(classificationResult);

        progressBar.setVisibility(View.GONE);
        progressText.setVisibility(View.GONE);

        displayResults(classificationResult);
    }
}
