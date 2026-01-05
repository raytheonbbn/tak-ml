package com.bbn.takml_server.model_execution;

public interface TakmlInitializationListener {
    /**
     * Callback specifying when TAK ML has finished loading all models from disk and initializing
     */
    void finishedInitializing();
}
