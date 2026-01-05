package com.atakmap.android.takml_android.util;

import com.atakmap.comms.TAKServer;
import com.bbn.tak.comms.ApiClient;

public class TakServerInfo {
    private final TAKServer takServer;
    private final ApiClient takserverApiClient;
    private byte[] clientCertBytes;
    private byte[] trustStoreBytes;
    private String clientCertPass;
    private String trustStorePass;

    public TakServerInfo(TAKServer takServer, ApiClient takserverApiClient){
        this.takServer = takServer;
        this.takserverApiClient = takserverApiClient;
    }

    public TakServerInfo(TAKServer takServer, ApiClient takserverApiClient, byte[] clientCertBytes,
                         byte[] trustStoreBytes, String clientCertPass, String trustStorePass) {
        this(takServer, takserverApiClient);
        this.clientCertBytes = clientCertBytes;
        this.trustStoreBytes = trustStoreBytes;
        this.clientCertPass = clientCertPass;
        this.trustStorePass = trustStorePass;
    }

    public TAKServer getTakServer() {
        return takServer;
    }

    public ApiClient getTakserverApiClient() {
        return takserverApiClient;
    }

    public byte[] getClientCertBytes() {
        return clientCertBytes;
    }

    public byte[] getTrustStoreBytes() {
        return trustStoreBytes;
    }

    public String getClientCertPass() {
        return clientCertPass;
    }

    public String getTrustStorePass() {
        return trustStorePass;
    }
}