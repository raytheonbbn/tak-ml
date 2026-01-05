package com.atakmap.android.takml_android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.atakmap.android.takml_android.storage.TakmlModelStorage;

import java.util.List;

public class TakmlReceiver extends BroadcastReceiver {
    private static final String TAG = TakmlReceiver.class.getName();
    public static final String RECEIVE = TakmlReceiver.class.getName() + ".RECEIVE";
    public static final String IMPORT_TAKML_MODEL = TakmlReceiver.class.getName() + ".IMPORT_TAKML_MODEL";
    private final Takml takml;
    private final TakmlModelStorage takmlModelStorage;

    public TakmlReceiver(Takml takml, TakmlModelStorage takmlModelStorage){
        this.takml = takml;
        this.takmlModelStorage = takmlModelStorage;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action == null)
            return;

        if (action.equals(RECEIVE)) {
            if(intent.getStringExtra(Constants.TAK_ML_UUID).equals(takml.getUuid().toString())){
                return;
            }
            List<String> knownMxPlugins = intent.getStringArrayListExtra(Constants.KNOWN_MX_PLUGINS);
            if(knownMxPlugins != null){
                takml.pluginNamesFoundOnClasspath.addAll(knownMxPlugins);
            }
        }else if(action.equals(IMPORT_TAKML_MODEL)){
            String takmlModelPath = intent.getStringExtra(Constants.TAKML_MODEL_PATH);
            if(takmlModelPath != null){
                takmlModelStorage.importModel(takmlModelPath);
            }
        }
    }
}
