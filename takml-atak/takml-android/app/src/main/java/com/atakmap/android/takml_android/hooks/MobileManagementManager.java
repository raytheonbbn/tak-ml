package com.atakmap.android.takml_android.hooks;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.atakmap.android.maps.MapView;

import java.util.UUID;

import us.watchtower.dpc.WatchtowerMlApiServiceUtility;
import us.watchtower.dpc.aidl.ml.IWatchtowerMlApiService;
import us.watchtower.dpc.aidl.ml.InferenceStartResponse;
import us.watchtower.dpc.aidl.ml.PolicyResponse;

public class MobileManagementManager {
    public static final String TAG = MobileManagementManager.class.getName();

    protected static final String PLUGIN_CONTEXT_CLASS = "com.atak.plugins.impl.PluginContext";
    protected HookEndpointState hookEndpointState;

    protected String deviceId = UUID.randomUUID().toString();
    private IWatchtowerMlApiService iRemoteService;
    private Context context;
    protected final ServiceConnection mConnection = new ServiceConnection() {
        // Called when the connection with the service is established.
        public void onServiceConnected(ComponentName className, IBinder service) {
            // Following the preceding example for an AIDL interface,
            // this gets an instance of the IRemoteInterface, which we can use to call on the service.
            iRemoteService = IWatchtowerMlApiService.Stub.asInterface(service);

            try {
                PolicyResponse policyResponse = iRemoteService.register(deviceId);
                if(policyResponse.success) {
                    hookEndpointState = HookEndpointState.REGISTERED;
                }
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }

        // Called when the connection with the service disconnects unexpectedly.
        public void onServiceDisconnected(ComponentName className) {
            Log.e(TAG, "Service has unexpectedly disconnected");
            iRemoteService = null;
        }
    };

    public MobileManagementManager(){
    }

    public void start(Context context) {
        this.context = context;
        boolean isUsingPluginContext = context.getClass().getName().equals(PLUGIN_CONTEXT_CLASS);
        if (isUsingPluginContext) {
            deviceId = MapView.getDeviceUid();
        }

        WatchtowerMlApiServiceUtility.Companion.startService(context, mConnection);
    }

    /*
    // TODO: remove this, this is solely for demoing purposes
    public void start(Context context, Intent emmServiceIntent) {
        this.context = context;
        boolean isUsingPluginContext = context.getClass().getName().equals(PLUGIN_CONTEXT_CLASS);
        if (isUsingPluginContext) {
            deviceId = MapView.getDeviceUid();
        }

        context.bindService(emmServiceIntent, mConnection, Context.BIND_AUTO_CREATE);
    }*/


    /**
     * Check if a TAKML operation (INFERENCE or SENSOR) is allowed to run. This will return true unless
     * a connected hook (e.g. an Enterprise Mobile Manager) states false.
     *
     * @param takmlOperation - TAK ML Operation in question, required
     * @return PermissionReply - stating whether to allow operation and also providing a unique request id
     */
    public PermissionReply checkIfAllowedToRun(TakmlOperation takmlOperation){
        return checkIfAllowedToRun(takmlOperation, null);
    }

    /**
     * Check if a TAKML operation (INFERENCE or SENSOR) is allowed to run. This will return true unless
     * a connected hook (e.g. an Enterprise Mobile Manager) states false.
     *
     * @param takmlOperation - TAK ML Operation in question, required
     * @param optionalModelName - Model Name
     * @return PermissionReply - stating whether to allow operation, a unique request id, and the model selected by the hook to run (as suggested by the first hook request, see TODO warning below).
     */
    public PermissionReply checkIfAllowedToRun(TakmlOperation takmlOperation, String optionalModelName){
        String requestId = UUID.randomUUID().toString();

        // Check execution with registered Hooks service, if exists
        if(hookEndpointState == HookEndpointState.REGISTERED) {
            Log.d(TAG, "checkIfAllowedToRun: 1");
            if (takmlOperation == TakmlOperation.INFERENCE) {
                Log.d(TAG, "checkIfAllowedToRun: 2");
                try {
                    InferenceStartResponse response = iRemoteService.inferenceStart(deviceId, optionalModelName);
                    Log.d(TAG, "checkIfAllowedToRun: 3");
                    return new PermissionReply(response.allow, response.requestId, response.modelSelected);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException trying to call inference start", e);
                    Log.d(TAG, "checkIfAllowedToRun: 4");
                    return new PermissionReply(false, requestId, null);
                }
            }
        }
        Log.d(TAG, "checkIfAllowedToRun: 5");

        return new PermissionReply(true, requestId);
    }

    public HookEndpointState getHooksInfo() {
        return hookEndpointState;
    }

    public void maybeInvokeInferenceEndRequests(String requestId, boolean success) {
        // Check execution with registered Hooks service, if exists
        if(hookEndpointState == HookEndpointState.REGISTERED) {
            try {
                Log.d(TAG, "maybeInvokeInferenceEndRequests: request id = " + requestId);
                iRemoteService.inferenceEndAsync(deviceId, requestId, success);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException trying to call inference end", e);
            }
        }
    }
}
