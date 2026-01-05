package com.bbn.takml_server.model_management.takfs;

import com.bbn.tak_sync_file_manager.FileClient;
import com.bbn.tak_sync_file_manager.TakFileManager;
import com.bbn.tak_sync_file_manager.TakFileManagerServer;
import org.springframework.beans.factory.annotation.Autowired;

public class TakmlFileManager {
    TakFileManagerServer takFileManagerServer;

    public TakFileManagerServer getTakFileManagerServer() {
        return takFileManagerServer;
    }
}
