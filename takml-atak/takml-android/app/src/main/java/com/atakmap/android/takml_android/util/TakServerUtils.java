package com.atakmap.android.takml_android.util;

import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.util.ServerListDialog;
import com.atakmap.comms.NetConnectString;
import com.atakmap.comms.TAKServer;
import com.atakmap.net.AtakAuthenticationCredentials;
import com.atakmap.net.AtakAuthenticationDatabase;
import com.atakmap.net.AtakCertificateDatabase;
import com.atakmap.net.AtakCertificateDatabaseIFace;
import com.bbn.tak.comms.ApiClient;
import com.bbn.tak.comms.helper.ClientUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TakServerUtils {
    private static final String TAG = TakServerUtils.class.getName();

    public interface NetworkSelectedListener {
        void networkSelected(boolean success, TakServerInfo takServerInfo);
    }

    private static Map<String, TakServerInfo> serverBaseUrlWithPortToTAKServerInfos() {
        Map<String, TakServerInfo> ret = new HashMap<>();
        TAKServer[] servers = CotMapComponent.getInstance().getServers();
        if(servers == null) {
            // ** No servers defined yet
            return new HashMap<>();
        }
        for (TAKServer server : servers) {
            if(!server.isConnected()){
                continue;
            }
            NetConnectString connectString =  NetConnectString.fromString(server.getConnectString());
            Log.d(TAG, "newWebSocketsClient: " + connectString.getHost() + " " + connectString.getProto());

            byte[] clientCertBytes = CertUtils.getClientCertBytes(connectString);
            byte[] trustStoreBytes = CertUtils.getTrustStoreBytes(connectString);

            if(connectString.getProto().equals("ssl")){
                AtakAuthenticationCredentials credentials =
                        AtakAuthenticationDatabase.getAdapter().getCredentialsForType("clientPassword", connectString.getHost());
                AtakAuthenticationCredentials credentials2 =
                        AtakAuthenticationDatabase.getAdapter().getCredentialsForType("caPassword", connectString.getHost());
                if(credentials != null && credentials2 != null && clientCertBytes != null && trustStoreBytes != null) {
                    Log.d(TAG, "getApiClientsAndCotPortsForConnectedServers: using SCTP connection");
                    String keystorePassword = CertUtils.getClientCertPassword(connectString);

                    ApiClient apiClient = ClientUtil.getClient(connectString.getHost(), 8443,
                            clientCertBytes, trustStoreBytes, ClientUtil.CertType.P12, keystorePassword,
                            keystorePassword, 200, 200, 200);

                    TakServerInfo takServerInfo = new TakServerInfo(server, apiClient,
                            clientCertBytes, trustStoreBytes, keystorePassword, keystorePassword);

                    ret.put(server.getURL(true), takServerInfo);
                }else{
                    trustStoreBytes = AtakCertificateDatabase.getAdapter()
                            .getCertificateForType(AtakCertificateDatabaseIFace.TYPE_TRUST_STORE_CA);
                    clientCertBytes = AtakCertificateDatabase.getAdapter()
                            .getCertificateForType(AtakCertificateDatabaseIFace.TYPE_CLIENT_CERTIFICATE);
                    if(clientCertBytes != null && trustStoreBytes != null) { // load default client store if available
                        AtakAuthenticationCredentials trustStoreCredentials = AtakAuthenticationDatabase.getAdapter()
                                .getCredentialsForType(AtakAuthenticationCredentials.TYPE_caPassword);
                        AtakAuthenticationCredentials clientCertCredentials = AtakAuthenticationDatabase.getAdapter()
                                .getCredentialsForType(AtakAuthenticationCredentials.TYPE_clientPassword);
                        String trustStorePass = trustStoreCredentials.password;
                        String clientCertPass = clientCertCredentials.password;

                        ApiClient apiClient = ClientUtil.getClient(connectString.getHost(), 8443,
                                clientCertBytes, trustStoreBytes, ClientUtil.CertType.P12, clientCertPass, trustStorePass, 200, 200, 200);

                        TakServerInfo takServerInfo = new TakServerInfo(server, apiClient,
                                clientCertBytes, trustStoreBytes, clientCertPass, trustStorePass);

                        Log.d(TAG, "getApiClientsAndCotPortsForConnectedServers2: " + connectString.getHost());
                        ret.put(server.getURL(true), takServerInfo);
                    }else {
                        Log.d(TAG, "getApiClientsAndCotPortsForConnectedServers: using TCP connection");
                        Toast.makeText(MapView.getMapView().getContext(), "TAK Server connection missing certificates and/or credentials",
                                Toast.LENGTH_LONG).show();
                    }
                }
            }else {
                ret.put(server.getURL(true), new TakServerInfo(server, ClientUtil.getClientTCP(connectString.getHost(), 8080, 200, 200, 200)));
            }
        }
        return ret;
    }

    public static TakServerInfo getTakServerInfo(){
        Map<String, TakServerInfo> apiClients = serverBaseUrlWithPortToTAKServerInfos();
        if(apiClients.isEmpty()){
            return null;
        }
        return apiClients.values().iterator().next();
    }

    /**
     * Sets up ApiClient with connected TAK Server. If more than one active TAK Server exists,
     * prompts user to select TAK Server to use.
     *
     * @param listener required
     */
    public static void getOrSelectNetwork(TakServerUtils.NetworkSelectedListener listener){
        if (CotMapComponent.getInstance() == null) {
            Log.w("DeviceProfileClient", "getProfile: No server list available");
            listener.networkSelected(false, null);
        } else {
            Map<String, TakServerInfo> apiClients =
                    serverBaseUrlWithPortToTAKServerInfos();

            if(apiClients.size() == 1){
                TakServerInfo takServerInfo = apiClients.values().iterator().next();
                listener.networkSelected(true, takServerInfo);
            }else if(apiClients.size() > 1){
                ServerListDialog.selectServer(
                        MapView.getMapView().getContext(),
                        "CKI TAK: Select a server to use for this session",
                        CotMapComponent.getInstance().getServers(),
                        server -> {
                            if (server == null) {
                                com.atakmap.coremap.log.Log.d(TAG, "No configured server selected");
                                listener.networkSelected(false, null);
                                return;
                            }
                            String serverBaseUrl = server.getURL(true);

                            TakServerInfo takServerInfo = apiClients.get(serverBaseUrl);
                            if(takServerInfo == null){
                                Log.e(TAG, "TakServerInfo null for server with url: " + serverBaseUrl);
                                listener.networkSelected(false, null);
                                return;
                            }

                            listener.networkSelected(true, takServerInfo);
                        });
            }else{
                listener.networkSelected(false, null);
            }
        }
    }
}