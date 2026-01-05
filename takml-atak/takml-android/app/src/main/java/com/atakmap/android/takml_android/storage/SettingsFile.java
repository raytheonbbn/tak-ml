package com.atakmap.android.takml_android.storage;

import android.util.Pair;

import java.util.UUID;

public class SettingsFile {
    private Pair<UUID, Long> writeToDiskLock;
    private Pair<String, Integer> takmlServerIpPort;

    public SettingsFile() {
    }

    /**
     * Gets the write to disk lock
     *
     * @return
     */
    public Pair<UUID, Long> getWriteToDiskLock() {
        return writeToDiskLock;
    }

    /**
     * Sets write to disk lock, with a UUID for the session and expiration time
     *
     * @param writeToDiskLock
     */
    public void setWriteToDiskLock(Pair<UUID, Long> writeToDiskLock) {
        this.writeToDiskLock = writeToDiskLock;
    }

    public Pair<String, Integer> getTakmlServerIpPort() {
        return takmlServerIpPort;
    }

    public void setTakmlServerIpPort(Pair<String, Integer> takmlServerIpPort) {
        this.takmlServerIpPort = takmlServerIpPort;
    }
}
