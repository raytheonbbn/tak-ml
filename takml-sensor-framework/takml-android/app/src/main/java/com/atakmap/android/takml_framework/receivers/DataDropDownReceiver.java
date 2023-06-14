package com.atakmap.android.takml_framework.receivers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml_framework.plugin.R;
import com.atakmap.coremap.log.Log;
import com.bbn.takml.framework.TakMlFramework;
import com.bbn.takml.sensor_framework.DataStreamerTask;

import java.util.Timer;
import java.util.TimerTask;

public class DataDropDownReceiver extends DropDownReceiver implements OnStateListener {

    public static final String TAG = DataDropDownReceiver.class.getSimpleName();
    public static final String SHOW_DATA = "com.atakmap.android.takml_framework.SHOW_DATA";

    private final View dataView;
    private final Context pluginContext;
    private TakMlFramework framework;
    private Activity parentActivity;

    private ArrayAdapter<String> pullModelAdapter;
    private Spinner pullModelDropdown;
    private Timer dbSizeTimer;
    private Timer dataStreamTimer;
    private TextView dataToServerCountView;
    private static long dataToServerCount = -1;
    private Switch deleteSwitch1;
    private Switch deleteSwitch2;

    public DataDropDownReceiver(final MapView mapView, final Context context) {
        super(mapView);
        LayoutInflater inflater = (LayoutInflater)context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.dataView = inflater.inflate(R.layout.data_layout, null);
        this.parentActivity = (Activity)mapView.getContext();
        this.pluginContext = context;

        Button back = (Button) dataView.findViewById(R.id.backButton);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeDropDown();
                Intent nextIntent = new Intent();
                nextIntent.setAction(TakMlFrameworkDropDownReceiver.SHOW_FRAMEWORK_STANDUP);
                AtakBroadcast.getInstance().sendBroadcast(nextIntent);
            }
        });

        deleteSwitch1 = (Switch)dataView.findViewById(R.id.deleteSwitch1);
        deleteSwitch2 = (Switch)dataView.findViewById(R.id.deleteSwitch2);

        deleteSwitch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(!isChecked) {
                    return;
                }

                if(deleteSwitch2.isChecked()) {
                    framework.getSensorFramework().deleteAllData();
                    Toast.makeText(mapView.getContext(), "DB cleared", Toast.LENGTH_LONG).show();
                    deleteSwitch1.setChecked(false);
                    deleteSwitch2.setChecked(false);
                }
            }
        });

        deleteSwitch2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(!isChecked) {
                    return;
                }

                if(deleteSwitch1.isChecked()) {
                    framework.getSensorFramework().deleteAllData();
                    Toast.makeText(mapView.getContext(), "DB cleared", Toast.LENGTH_LONG).show();
                    deleteSwitch1.setChecked(false);
                    deleteSwitch2.setChecked(false);
                }
            }
        });

        dataToServerCountView = (TextView)dataView.findViewById(R.id.dataToServerCount);

        Switch streamToServerSwitch = (Switch)dataView.findViewById(R.id.streamToServerSwitch);
        streamToServerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    Log.d(TAG, "Starting data streaming");
                    dataStreamTimer = new Timer();
                    dataStreamTimer.schedule(new DataStreamerTask(framework), 0, 100);
                } else {
                    Log.d(TAG, "Stopping data streaming");
                    dataStreamTimer.cancel();
                }
            }
        });



        TimerTask dbCountCheck = new TimerTask() {
            @Override
            public void run() {
                if(DataDropDownReceiver.this.framework == null) {
                    return;
                }
                final Long curObservationCount = DataDropDownReceiver.this.framework.getSensorFramework().getObservationCount();
                DataDropDownReceiver.this.parentActivity.runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                TextView dataItemCount = (TextView)dataView.findViewById(R.id.dataCount);
                                dataItemCount.setText(String.valueOf(curObservationCount));

                                if(dataToServerCount == -1) {
                                    Log.d(TAG, "First execution of dbCountCheck task, loading data from DB");
                                    dataToServerCount = framework.getSensorFramework().getDataTXFromDB();
                                    Log.d(TAG, "Loaded DATA_TX count: " + dataToServerCount);
                                }
                                dataToServerCountView.setText(String.valueOf(dataToServerCount));

                                Log.d(TAG, "Updating counts: " + dataToServerCount);
                            }
                        }
                );
            }
        };

        dbSizeTimer = new Timer();
        dbSizeTimer.scheduleAtFixedRate(dbCountCheck, 2000, 2000);
    }

    public void setFramework(TakMlFramework framework) {
        this.framework = framework;
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

    public static void updateDataToServerCount(int sendCount) {
        dataToServerCount += sendCount;
    }
    public static void clearDataToServerCount() {
        dataToServerCount = 0;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "On receive called with " + intent.getAction());
        if (intent.getAction().equals(SHOW_DATA)) {
            showDropDown(this.dataView, HALF_WIDTH, FULL_HEIGHT,
                    FULL_WIDTH, HALF_HEIGHT, false);
        }
    }

}
