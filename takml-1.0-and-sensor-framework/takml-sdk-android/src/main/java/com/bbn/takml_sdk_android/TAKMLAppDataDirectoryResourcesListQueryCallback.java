package com.bbn.takml_sdk_android;

import java.util.List;

/**
 * A callback for requesting TAKML Framework's app data directory resources list (a list of
 * the files in this directory)
 */
public interface TAKMLAppDataDirectoryResourcesListQueryCallback {
    /**
     * Received the app data directory files list from the TAKML Framework.
     * @param takmlAppDataDirectoryFiles List of files in the TAKML Framework's app data directory
     */
    void appDataDirectoryResourcesListCallback(List<String> takmlAppDataDirectoryFiles);
}
