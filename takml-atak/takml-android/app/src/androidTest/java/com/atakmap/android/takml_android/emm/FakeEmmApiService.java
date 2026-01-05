package com.atakmap.android.takml_android.emm;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import us.watchtower.dpc.aidl.ml.IWatchtowerMlApiService;
import us.watchtower.dpc.aidl.ml.InferenceStartResponse;
import us.watchtower.dpc.aidl.ml.PolicyResponse;
import us.watchtower.dpc.aidl.ml.SensorDataSendResponse;
import us.watchtower.dpc.aidl.ml.SensorSubscribeResponse;

public class FakeEmmApiService extends Service {
    private static final String TAG = FakeEmmApiService.class.getName();

    private final IBinder binder = new WatchtowerMlApiServiceImpl();

    public static boolean PERMIT_REGISTER = true;
    public static Set<String> MODELS_TO_DENY = new HashSet<>();
    public static boolean isPseudoModel = false;

    public static String modelToReturn = "test";

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private static class WatchtowerMlApiServiceImpl extends IWatchtowerMlApiService.Stub {
        // Store active sessions and subscriptions
        private final ConcurrentHashMap<String, String> activeInferences = new ConcurrentHashMap<>();

        @Override
        public PolicyResponse register(String atakPluginId) throws RemoteException {
            // Implement registration logic
            Log.d("WatchtowerML", "Registering plugin: " + atakPluginId);
            PolicyResponse policyResponse = new PolicyResponse();
            policyResponse.success = PERMIT_REGISTER;
            return policyResponse;
        }

        @Override
        public InferenceStartResponse inferenceStart(String atakPluginId, String modelRequested) throws RemoteException {
            // Create response
            InferenceStartResponse response = new InferenceStartResponse();
            response.requestId = UUID.randomUUID().toString();
            response.modelSelected = isPseudoModel ? modelToReturn : modelRequested;
            response.allow = !MODELS_TO_DENY.contains(modelRequested);
            response.expirationTime = System.currentTimeMillis() + 3600000; // 1 hour from now
            response.success = true;

            Log.d(TAG, "inferenceStart: returning " + response.modelSelected);

            // Track active inference
            activeInferences.put(response.requestId, atakPluginId);
            return response;
        }

        @Override
        public PolicyResponse inferenceEnd(String atakPluginId, String requestId, boolean success) throws RemoteException {
            // Clean up inference session
            activeInferences.remove(requestId);
            
            Log.d("WatchtowerML", "Ending inference for request: " + requestId + 
                  ", success: " + success);
            
            return new PolicyResponse();
        }

        @Override
        public void inferenceEndAsync(String atakPluginId, String requestId, boolean success) throws RemoteException {
            // Asynchronous version
            new Thread(() -> {
                try {
                    inferenceEnd(atakPluginId, requestId, success);
                } catch (RemoteException e) {
                    Log.e("WatchtowerML", "Async inference end failed", e);
                }
            }).start();
        }

        @Override
        public SensorSubscribeResponse sensorSubscribe(String atakPluginId, String sensorId) throws RemoteException {
            return null;
        }

        @Override
        public PolicyResponse sensorUnsubscribe(String atakPluginId, String requestId) throws RemoteException {
            return null;
        }

        @Override
        public void sensorUnsubscribeAsync(String atakPluginId, String requestId) throws RemoteException {
        }

        @Override
        public SensorDataSendResponse sensorDataSend(String atakPluginId, String sensorId) throws RemoteException {
            return null;
        }
    }
}