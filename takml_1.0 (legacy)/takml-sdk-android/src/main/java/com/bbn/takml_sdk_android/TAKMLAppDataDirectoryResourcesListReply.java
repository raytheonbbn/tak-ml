package com.bbn.takml_sdk_android;

import java.io.Serializable;
import java.util.List;

/**
 * Reply to query for resources in TAKML Framework's app data directory.
 */
public class TAKMLAppDataDirectoryResourcesListReply implements Serializable {
    private static final long serialVersionUID = 1L;
    List<String> filesList;

    /**
     * Create a TAKMLAppDataDirectoryResourcesListReply to hold a list of file names.
     */
    public TAKMLAppDataDirectoryResourcesListReply(List<String> filesList) {
        this.filesList = filesList;
    }

    /**
     * Get files list from object.
     */
    public List<String> getFilesList() {
        return filesList;
    }
}
