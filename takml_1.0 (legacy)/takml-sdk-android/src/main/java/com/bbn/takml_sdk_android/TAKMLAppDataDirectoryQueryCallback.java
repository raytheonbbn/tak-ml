package com.bbn.takml_sdk_android;

/**
 * A callback for requesting TAKML Framework's app data directory, which is used
 * for storing app specific files (i.e. metadata lookup files).
 */
public interface TAKMLAppDataDirectoryQueryCallback {
    /**
     * Received the app data directory from the TAKML Framework.
     * @param takmlAppDataDirectory The TAKML Framework's app data directory.
     */
    void appDataDirectoryQueryResult(String takmlAppDataDirectory);
}
