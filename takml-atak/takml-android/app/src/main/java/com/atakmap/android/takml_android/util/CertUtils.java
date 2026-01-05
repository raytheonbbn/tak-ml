package com.atakmap.android.takml_android.util;

import android.util.Log;
import androidx.annotation.Nullable;
import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.comms.NetConnectString;
import com.atakmap.comms.TAKServer;
import com.atakmap.comms.app.CotPortListActivity;
import com.atakmap.net.AtakAuthenticationCredentials;
import com.atakmap.net.AtakAuthenticationDatabase;
import com.atakmap.net.AtakCertificateDatabase;
import com.bbn.tak.comms.ApiClient;
import com.bbn.tak.comms.helper.ClientUtil;
import com.bbn.tak.comms.helper.ClientUtil.CertType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CertUtils {
    private static final String TAG = CertUtils.class.getName();

    public CertUtils() {
    }

    public static byte[] getClientCertBytes(NetConnectString connectString) {
        return getCertBytes(connectString, "CLIENT_CERTIFICATE");
    }

    public static byte[] getTrustStoreBytes(NetConnectString connectString) {
        return getCertBytes(connectString, "TRUST_STORE_CA");
    }
    private static byte[] getCertBytes(NetConnectString connectString, String certType) {
        Log.d("CertUtils", "newSecureWebSocketsClient: " + connectString.getHost());
        byte[] certBytes = AtakCertificateDatabase.getCertificateForServer(certType, connectString.getHost());
        return certBytes;
    }

    public static String getClientCertPassword(NetConnectString connectString) {
        return getPassword(connectString, "clientPassword");
    }

    public static String getTrustStorePassword(NetConnectString connectString) {
        return getPassword(connectString, "caPassword");
    }

    @Nullable
    private static String getPassword(NetConnectString connectString, String passwordType) {
        AtakAuthenticationCredentials credentials = AtakAuthenticationDatabase.getAdapter().getCredentialsForType(passwordType, connectString.getHost());
        if (credentials.password != null && credentials.password.length() != 0) {
            return credentials.password;
        } else {
            Log.e("CertUtils", "Password was empty for " + passwordType);
            return null;
        }
    }

    public static TAKServer[] getServers() {
        return CotMapComponent.getInstance().getServers();
    }

    public static NetConnectString getConnectStringForServer(TAKServer server) {
        return NetConnectString.fromString(server.getConnectString());
    }

    public static List<NetConnectString> getConnectStringsForConnectedServers() {
        List<NetConnectString> ret = new ArrayList<>();
        for (TAKServer server : getServers()) {
            if (server.isConnected()) {
                ret.add(getConnectStringForServer(server));
            }
        }
        return ret;
    }
}
