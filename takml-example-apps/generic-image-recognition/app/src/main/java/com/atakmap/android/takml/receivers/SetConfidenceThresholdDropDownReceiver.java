package com.atakmap.android.takml.receivers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.atakmap.android.atakutils.MiscUtils;
import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml.mvc.Model;
import com.atakmap.android.takml.plugin.R;

import static com.atakmap.android.takml.receivers.SettingsDropDownReceiver.SHOW_SETTINGS;

public class SetConfidenceThresholdDropDownReceiver extends DropDownReceiver
        implements DropDown.OnStateListener {

    public static final String TAG = SetConfidenceThresholdDropDownReceiver.class.getSimpleName();
    public static final String SHOW_SET_CONFIDENCE_THRESHOLD =
            "com.atakmap.android.android.takml.image_recognition_demo_app.SHOW_SET_CONFIDENCE_THRESHOLD";

    private final View layoutView;
    private Activity parentActivity;
    private Model model;
    private Handler mHandler;

    EditText confidenceThresholdEntry;
    Button setConfidenceThresholdButton;

    public SetConfidenceThresholdDropDownReceiver(final MapView mapView, final Context context, Model model) {
        super(mapView);

        this.model = model;

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.layoutView = inflater.inflate(R.layout.minimum_confidence_threshold_setting_layout, null);
        this.parentActivity = (Activity) mapView.getContext();

        confidenceThresholdEntry = (EditText) layoutView.findViewById(R.id.confidenceThresholdEntry);
        confidenceThresholdEntry.setText(Float.toString(model.imageRecParams.minimumConfidence));
        setConfidenceThresholdButton = (Button) layoutView.findViewById(R.id.setConfidenceThresholdButton);
        setConfidenceThresholdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String userEntry = confidenceThresholdEntry.getText().toString();
                Float userEntryFloat = null;
                try {
                    userEntryFloat = Float.parseFloat(userEntry);
                } catch (NumberFormatException e) {
                    MiscUtils.toast("Invalid format for confidence threshold; try again.");
                    return;
                }
                if (userEntryFloat < 0.0f || userEntryFloat > 1.0f) {
                    MiscUtils.toast("Confidence threshold is out of range (acceptable range" +
                            " is between 0.0 and 1.0 inclusive), try again.");
                    return;
                }

                model.imageRecParams.minimumConfidence = userEntryFloat;

                MiscUtils.toast("Successfully set confidence threshold to " + userEntryFloat + "!");

                model.currentPredictionResult = "";

            }
        });

        Button done = (Button) layoutView.findViewById(R.id.doneButton);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeDropDown();
                Intent nextIntent = new Intent();
                nextIntent.setAction(SHOW_SETTINGS);
                AtakBroadcast.getInstance().sendBroadcast(nextIntent);
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
        Log.d(TAG, "Received intent: " + intent.getAction());

        if (intent.getAction().equals(SHOW_SET_CONFIDENCE_THRESHOLD)) {
            showDropDown(this.layoutView, HALF_WIDTH, FULL_HEIGHT,
                    FULL_WIDTH, HALF_HEIGHT, false);
        }

    }

}

