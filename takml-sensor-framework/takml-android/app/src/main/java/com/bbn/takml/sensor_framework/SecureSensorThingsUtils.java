package com.bbn.takml.sensor_framework;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.atakmap.android.maps.MapView;
import com.atakmap.net.AtakAuthenticationCredentials;
import com.atakmap.net.AtakAuthenticationDatabase;
import com.atakmap.net.AtakCertificateDatabase;
import com.atakmap.net.AtakCertificateDatabaseIFace;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.SSLContext;

import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;

public class SecureSensorThingsUtils {
    private static final String TAG = SecureSensorThingsUtils.class.getSimpleName();

    public static void setupForMutualAuth(SensorThingsService serverService, Context context) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException, CertificateException {
        Log.d(TAG, "Setting secure client for SensorThingsService");
        KeyStore keyStore = KeyStore.getInstance("pkcs12");
        KeyStore trustStore = KeyStore.getInstance("pkcs12");

        String password = "atakatak";

        loadKeyAndTrustStores(keyStore, trustStore);
        //keyStore.load(context.getAssets().open("client1.p12"), "atakatak".toCharArray());
        //trustStore.load(context.getAssets().open("truststore-root.p12"), "atakatak".toCharArray());

        SSLContext sslcontext = SSLContexts.custom()
                .loadKeyMaterial(keyStore, password.toCharArray())
                .loadTrustMaterial(trustStore, new TrustSelfSignedStrategy())
                .useTLS()
                .build();

        SSLConnectionSocketFactory sslSF = new SSLConnectionSocketFactory(
                sslcontext,
                null,
                null,
                SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        CloseableHttpClient httpclient = HttpClients.custom()
                .setSSLSocketFactory(sslSF)
                .build();

        serverService.setClient(httpclient);


    }

    private static void loadKeyAndTrustStores(KeyStore keyStore, KeyStore trustStore) throws CertificateException, NoSuchAlgorithmException, IOException {
        /*byte[] trustStoreBytes = AtakCertificateDatabase.getAdapter().getCertificateForTypeAndServer(AtakCertificateDatabaseIFace.TYPE_TRUST_STORE_CA, serverIp);
        byte[] clientCertBytes = AtakCertificateDatabase.getAdapter().getCertificateForTypeAndServer(AtakCertificateDatabaseIFace.TYPE_CLIENT_CERTIFICATE, serverIp);*/
        byte[] trustStoreBytes = AtakCertificateDatabase.getAdapter().getCertificateForType(AtakCertificateDatabaseIFace.TYPE_TRUST_STORE_CA);
        byte[] clientCertBytes = AtakCertificateDatabase.getAdapter().getCertificateForType(AtakCertificateDatabaseIFace.TYPE_CLIENT_CERTIFICATE);

        if (trustStoreBytes == null) {
            Log.e(TAG, "Could not find trust store.");
        }
        if (clientCertBytes == null) {
            Log.e(TAG, "Could not find client certificate.");
        }

        AtakAuthenticationCredentials trustStoreCredentials = AtakAuthenticationDatabase.getAdapter()
                .getCredentialsForType(AtakAuthenticationCredentials.TYPE_caPassword);
        AtakAuthenticationCredentials clientCertCredentials = AtakAuthenticationDatabase.getAdapter()
                .getCredentialsForType(AtakAuthenticationCredentials.TYPE_clientPassword);

        /*AtakAuthenticationCredentials trustStoreCredentials = AtakAuthenticationDatabase.getAdapter()
                .getCredentialsForType(AtakAuthenticationCredentials.TYPE_caPassword, serverIp);
        AtakAuthenticationCredentials clientCertCredentials = AtakAuthenticationDatabase.getAdapter()
                .getCredentialsForType(AtakAuthenticationCredentials.TYPE_clientPassword, serverIp);*/
        if (trustStoreCredentials == null || trustStoreCredentials.password == null || trustStoreCredentials.password.length() == 0) {
            Log.e(TAG, "Trust store certificate password was empty.");
            Toast.makeText(MapView.getMapView().getContext(), "Unable to access default trust store information", Toast.LENGTH_LONG);
        }
        if (clientCertCredentials == null || clientCertCredentials.password == null || clientCertCredentials.password.length() == 0) {
            Log.e(TAG, "Client certificate password was empty.");
            Toast.makeText(MapView.getMapView().getContext(), "Unable to access default client key store information", Toast.LENGTH_LONG);
        }
        String trustStorePass = trustStoreCredentials.password;
        String clientCertPass = clientCertCredentials.password;

        // ** FOR DEBUGGING
        List<X509Certificate> certs = AtakCertificateDatabase.loadCertificate(trustStoreBytes, trustStorePass);
        List<X509Certificate> cacerts = AtakCertificateDatabase.getCACerts();
        if (!certs.equals(cacerts)) {
            Log.w(TAG, "Certs list doesn't equal ca certs list");
        }

        /*Log.d(TAG, "Check validity of the database we're using");
        AtakCertificateDatabase.CeritficateValidity validity = AtakCertificateDatabase.checkValidity(trustStoreBytes, trustStorePass);
        if (!validity.isValid()) {
            Log.w(TAG, "checkValidity returned false for the trust store.");
        }*/

        InputStream keyStoreInputStream = new ByteArrayInputStream(clientCertBytes);
        InputStream trustStoreInputStream = new ByteArrayInputStream(trustStoreBytes);

        keyStore.load(keyStoreInputStream, clientCertPass.toCharArray());
        trustStore.load(trustStoreInputStream, trustStorePass.toCharArray());
    }
}
