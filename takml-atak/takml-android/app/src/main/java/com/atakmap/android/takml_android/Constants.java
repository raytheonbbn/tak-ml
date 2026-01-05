package com.atakmap.android.takml_android;

import android.os.Environment;

import java.io.File;

public class Constants {
    public static final String TAKML_MP_STORAGE_DIR = Environment
            .getExternalStorageDirectory() + File.separator + "atak" + File.separator
            + "takml" + File.separator + "models";
    public static final String TAKML_SETTINGS_FILE = Environment
            .getExternalStorageDirectory() + File.separator + "atak" + File.separator
            + "takml_settings.json";
    public static final String TAK_ML_UUID = "uuid";
    public static final String KNOWN_MX_PLUGINS = "known_mx_plugins";
    public static final String TAKML_MODEL_PATH = "takml_model_path";
    public static final String TAKML_CONFIG_FILE = "takml_config.yaml";
    public static final String TAKML_CONFIG_EXTENSION = ".yaml";
    public static final String TAKML_SYSTEM_PREFS = "TAKML_PREFS";
    public static final String TAKML_RESULT_LIST = "takml_results";
    public static final String TAKML_MX_SERVICE_REQUEST_ID = "mx_plugin_service_request_id";
    public static final String TAKML_MX_SERVICE_REQUEST_SUCCESS = "mx_plugin_service_request_success";
    public static final String TAKML_MX_SERVICE_REQUEST_MODEL_NAME = "mx_plugin_service_request_model_name";
    public static final String TAKML_MX_SERVICE_REQUEST_MODEL_TYPE = "mx_plugin_service_request_model_type";

    public static final String VERSION_PARAM = "VERSION";
}
