package com.atakmap.android.takml_android;

public interface TakmlInitializationListener {
    /**
     * Callback specifying when TAK ML has finished loading all models from disk and initializing
     */
    void finishedInitializing();
}
