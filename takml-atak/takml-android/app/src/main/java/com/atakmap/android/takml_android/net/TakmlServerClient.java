package com.atakmap.android.takml_android.net;


import android.util.Log;

import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.takml_android.util.TakServerInfo;
import com.atakmap.android.takml_android.util.TakServerUtils;
import com.atakmap.comms.TAKServer;
import com.bbn.takml_server.ApiCallback;
import com.bbn.takml_server.ApiClient;
import com.bbn.takml_server.ApiException;
import com.bbn.takml_server.ApiResponse;
import com.bbn.takml_server.client.ModelExecutionApi;
import com.bbn.takml_server.client.ModelFeedbackApi;
import com.bbn.takml_server.client.ModelManagementApi;
import com.bbn.takml_server.client.ModelMetricsApi;
import com.bbn.takml_server.client.models.AddModelMetricsRequest;
import com.bbn.takml_server.client.models.IndexRow;
import com.bbn.takml_server.client.models.InferOutput;
import com.bbn.takml_server.client.models.InferenceRequest;
import com.bbn.takml_server.client.models.InferenceResponse;
import com.google.gson.Gson;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

public class TakmlServerClient {
    private static final String TAG = TakmlServerClient.class.getName();
    private static final int TIME_OUT_MILLIS = 10000;
    private ApiClient client;
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);

    public interface ModelExecutionCallback{
        void handle(List<InferOutput> outputs);
    }

    public TakmlServerClient(String url, boolean tcp){
        this(url, tcp, null, null);
    }

    public TakmlServerClient(String url, boolean tcp, String apiKeyName, String apiKey){
        if(tcp){
            client = getClientTCP(url, TIME_OUT_MILLIS, TIME_OUT_MILLIS, TIME_OUT_MILLIS);
        } else {
            TakServerInfo takServerInfo = SelectedTAKServer.getInstance().getTakServerInfo();
            ///  if url is same as TAK Server, use the same certs
           if (url.contains(takServerInfo.getTakServer().getURL(false))){
               client = getClient(url, new ByteArrayInputStream(takServerInfo.getClientCertBytes()),
                       new ByteArrayInputStream(takServerInfo.getTrustStoreBytes()),
                       takServerInfo.getClientCertPass(), takServerInfo.getTrustStorePass(),
                       TIME_OUT_MILLIS, TIME_OUT_MILLIS, TIME_OUT_MILLIS);
           } else {
               /// use TCP
               client = getClientTCP(url, TIME_OUT_MILLIS, TIME_OUT_MILLIS, TIME_OUT_MILLIS);
           }
        }
        if (apiKeyName != null && apiKey != null) {
            client = client.addDefaultHeader(apiKeyName, apiKey);
        }
    }


    public TakmlServerClient(String url, String optionalApiKeyName, String optionalApiKey, byte[] clientStoreBytes,
                              byte[] trustStoreBytes, String clientStorePass, String trustStorePass) {
        client = getClient(url, new ByteArrayInputStream(clientStoreBytes),
                new ByteArrayInputStream(trustStoreBytes),
                clientStorePass, trustStorePass,
                TIME_OUT_MILLIS, TIME_OUT_MILLIS, TIME_OUT_MILLIS);
        if(optionalApiKey != null) {
            client = client.addDefaultHeader(optionalApiKeyName, optionalApiKey);
        }
    }

    public ModelManagementApi getModelManagementApi(){
        return new ModelManagementApi(client);
    }

    public ModelFeedbackApi getModelFeedbackApi(){
        return new ModelFeedbackApi(client);
    }

    public void executeModelAsync(InferenceRequest input, String modelName, String modelVersion,
                                  ModelExecutionCallback modelExecutionCallback){
        ModelExecutionApi modelExecutionApi = new ModelExecutionApi(client);
        executorService.execute(() -> {
            try {
                Log.d(TAG, "Sending inference request with input: {}" + new Gson().toJson(input) + ", modelName: " + modelName + " and version: " + modelVersion);
                ApiResponse<InferenceResponse> response = modelExecutionApi.postModelVersionInferWithHttpInfo(input, modelName, modelVersion);
                Log.d(TAG, "executeModelAsync: " + response.getStatusCode());
                modelExecutionCallback.handle(response.getData().getOutputs());
            } catch (ApiException e) {
                Log.e(TAG, "ApiException querying executing mx model", e);
                modelExecutionCallback.handle(null);
            }
        });
    }

    public void submitMetricsAsync(AddModelMetricsRequest addModelMetricsRequest,
                                   MetricsCallback metricsCallback) {
        ModelMetricsApi modelMetricsApi = new ModelMetricsApi(client);
        executorService.execute(() -> {
            try {
                modelMetricsApi.addModelMetrics(addModelMetricsRequest);
                metricsCallback.metricsSubmitted(true, null);
            } catch (ApiException e) {
                // If server actually returned 2xx but client choked on content-type, treat as success
                if (e.getCode() == 200) {
                    metricsCallback.metricsSubmitted(true, null);
                    return;
                }

                Log.e(TAG, "Failure submitting metrics request with code", e);
                metricsCallback.metricsSubmitted(false, e.getResponseBody());
            }
        });
    }


    private ApiClient getClient(String url,
                                      InputStream clientKeyStoreInputStream,
                                      InputStream trustStoreInputStream,
                                      String keyStorePassword, String trustStorePassword,
                                      long readTimeout, long connectTimeout, long writeTimeout) {
        KeyStore ks = null;
        try {
            ks = KeyStore.getInstance("PKCS12");
        } catch (KeyStoreException e) {
            Log.e(TAG, "Error loading keystore file", e);
            return null;
        }

        try {
            ks.load(clientKeyStoreInputStream, keyStorePassword.toCharArray());
        } catch (CertificateException | NoSuchAlgorithmException | IOException e) {
            Log.e(TAG, "Error loading keystore file", e);
            return null;
        }

        KeyManagerFactory kmf = null;
        try {
            kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Error loading key manager factory", e);
            return null;
        }

        try {
            kmf.init(ks, keyStorePassword.toCharArray());
        } catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException e) {
            Log.e(TAG, "Error initializing keystore", e);
            return null;
        }

        KeyManager[] kms = kmf.getKeyManagers();
        KeyStore ts = null;
        try {
            ts = KeyStore.getInstance("PKCS12");
        } catch (KeyStoreException e) {
            Log.e(TAG, "Error loading trust store", e);
            return null;
        }

        try {
            ts.load(trustStoreInputStream, trustStorePassword.toCharArray());
        } catch (NoSuchAlgorithmException | CertificateException | IOException e) {
            Log.e(TAG, "Error loading trust store file", e);
            return null;
        }

        TrustManagerFactory tmf = null;
        try {
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Error loading trust manager factory", e);
            return null;
        }

        try {
            tmf.init(ts);
        } catch (KeyStoreException e) {
            Log.e(TAG, "Error initializing trust manager factory", e);
            return null;
        }

        TrustManager[] tms = tmf.getTrustManagers();
        SSLContext ctx = null;
        try {
            ctx = SSLContext.getInstance("SSL");
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Error finding ssl context", e);
            return null;
        }

        try {
            ctx.init(kms, tms, new SecureRandom());
        } catch (KeyManagementException e) {
            Log.e(TAG, "Error initializing ssl context", e);
            return null;
        }

        SSLSocketFactory sslSocketFactory = ctx.getSocketFactory();

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .writeTimeout(writeTimeout, TimeUnit.SECONDS)
                .sslSocketFactory(sslSocketFactory, (X509TrustManager)tms[0])
                .hostnameVerifier((s, sslSession) -> true)
                .build();

        ApiClient apiClient = new ApiClient();
        apiClient.setHttpClient(okHttpClient);
        apiClient.setBasePath(url);
        return apiClient;
    }

    public static ApiClient getClientTCP(String url, long readTimeout, long connectTimeout, long writeTimeout) {

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .writeTimeout(writeTimeout, TimeUnit.SECONDS)
                .hostnameVerifier((s, sslSession) -> true)
                .build();

        ApiClient apiClient = new ApiClient();
        apiClient.setHttpClient(okHttpClient);
        apiClient.setBasePath(url);
        return apiClient;
    }
}
