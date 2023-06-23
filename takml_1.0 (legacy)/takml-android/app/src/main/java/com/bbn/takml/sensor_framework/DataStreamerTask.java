package com.bbn.takml.sensor_framework;

import static com.atakmap.android.takml_framework.receivers.SettingsDropDownReceiver.DEFAULT_STREAM_BATCH_SIZE;
import static com.atakmap.android.takml_framework.receivers.SettingsDropDownReceiver.STREAM_BATCH_SIZE;
import static com.atakmap.android.takml_framework.receivers.SettingsDropDownReceiver.TAK_ML_PREFS;

import android.content.Context;
import android.content.SharedPreferences;

import com.atakmap.android.maps.MapView;
import com.bbn.takml.framework.TakMlFramework;

import java.util.TimerTask;

public class DataStreamerTask extends TimerTask {
    private TakMlFramework framework;
    private SharedPreferences takmlPrefs = MapView.getMapView().getContext().getSharedPreferences(TAK_ML_PREFS, Context.MODE_PRIVATE);

    public DataStreamerTask(TakMlFramework framework) {
        this.framework = framework;
    }

    @Override
    public void run() {
        framework.getSensorFramework().streamData(takmlPrefs.getInt(STREAM_BATCH_SIZE, DEFAULT_STREAM_BATCH_SIZE));
    }
}
