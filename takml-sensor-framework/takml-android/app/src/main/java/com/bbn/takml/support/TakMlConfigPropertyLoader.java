package com.bbn.takml.support;

import static com.atakmap.android.takml_framework.receivers.SettingsDropDownReceiver.TAK_ML_PREFS;

import android.content.Context;
import android.content.SharedPreferences;

public class TakMlConfigPropertyLoader {

    private final static String TAG = "TAKML_ConfigPropertyLoader";

    public static String getString(Context context, String key, String defaultValue) {
        /*String value = null;
        try {
            Properties properties = new Properties();
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open("config.properties");
            properties.load(inputStream);
            value = properties.getProperty(key);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Unable to find the config file: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "Failed to open config file.");
        }
        return value;*/
        SharedPreferences takmlPrefs = context.getSharedPreferences(TAK_ML_PREFS, Context.MODE_PRIVATE);
        return takmlPrefs.getString(key, defaultValue);
    }

    public static Integer getInt(Context context, String key, Integer defaultValue) {
        SharedPreferences takmlPrefs = context.getSharedPreferences(TAK_ML_PREFS, Context.MODE_PRIVATE);
        return takmlPrefs.getInt(key, defaultValue);
    }

    public static Boolean getBoolean(Context context, String key, Boolean defaultValue) {
        SharedPreferences takmlPrefs = context.getSharedPreferences(TAK_ML_PREFS, Context.MODE_PRIVATE);
        return takmlPrefs.getBoolean(key, defaultValue);
    }
}
