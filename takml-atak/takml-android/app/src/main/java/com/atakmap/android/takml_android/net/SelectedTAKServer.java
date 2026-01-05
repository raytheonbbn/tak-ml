package com.atakmap.android.takml_android.net;

import com.atakmap.android.takml_android.util.TakServerInfo;

public class SelectedTAKServer {
    private static SelectedTAKServer instance;
    private TakServerInfo takServerInfo;

    public synchronized static SelectedTAKServer getInstance(){
        if(instance == null){
            instance = new SelectedTAKServer();
        }
        return instance;
    }

    public void setTAkServer(TakServerInfo takServerInfo){
        this.takServerInfo = takServerInfo;
    }

    public TakServerInfo getTakServerInfo() {
        return takServerInfo;
    }
}
