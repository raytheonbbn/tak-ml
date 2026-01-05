package com.atakmap.android.takml_android.service;

import static com.atakmap.android.takml_android.Takml.SERVICE_MODEL_RESULT;
import static com.atakmap.android.takml_android.processing_params.compatibility_layer.PytorchProcessingConfigParser.parseImageRecognitionProcessingParams;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import com.atakmap.android.takml_android.Constants;
import com.atakmap.android.takml_android.IMxPluginService;
import com.atakmap.android.takml_android.MXPlugin;
import com.atakmap.android.takml_android.ProcessingParams;
import com.atakmap.android.takml_android.R;
import com.atakmap.android.takml_android.TakmlModel;
import com.atakmap.android.takml_android.lib.TakmlInitializationException;
import com.atakmap.android.takml_android.processing_params.ImageRecognitionProcessingParams;
import com.atakmap.android.takml_android.tensor_processor.InferInput;
import com.atakmap.android.takml_android.util.MxPluginsUtil;
import com.atakmap.android.takml_android.util.SecureFileTransfer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class MxPluginService extends Service {
    private static final String TAG = MxPluginService.class.getName();

    private static final int MAX_CHUNK_SIZE = 1024 * 1024; // 1MB per chunk

    private final String ANDROID_CHANNEL_ID = "com.bbn.takml.Location.Channel_" + this.getClass().getSimpleName();
    private static final int NOTIFICATION_ID = 555;

    // TODO: is 3 a good default, make configurable?
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);
    private final ConcurrentHashMap<String, MXPlugin> modelUUIDToPlugin = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();

    private final IMxPluginService.Stub binder = new IMxPluginService.Stub() {
        @Override
        public void registerModel(String modelUUID, String name, String modelExtension, String modelType,
                                  String modelFilePath, String serializedProcessingParams, List<String> labels) throws RemoteException {
            Log.d(TAG, "called register MxPlugin Model with config: " + serializedProcessingParams);

            ProcessingParams processingParams = null;
            if(serializedProcessingParams != null) {
                JSONObject obj;
                try {
                    obj = new JSONObject(serializedProcessingParams);
                } catch (JSONException e) {
                    Log.e(TAG, "JSON exception parsing processing config", e);
                    return;
                }
                String type;
                try {
                    type = obj.getString("type");
                } catch (JSONException e) {
                    type = ImageRecognitionProcessingParams.class.getName();
                    Log.e(TAG, "JSON exception parsing processing config 'type', using default: " + type, e);
                }
                // the below is deprecated, replace with generic version
                boolean pytorchCompatibility = false;
                if(type.equals("com.atakmap.android.takml_android.pytorch_mx_plugin.PytorchObjectDetectionParams")){
                    type = ImageRecognitionProcessingParams.class.getName();
                    pytorchCompatibility = true;
                }

                Class<?> test;
                try {
                    test = Class.forName(type);

                    Class<? extends ProcessingParams> processingParamsClass = test.asSubclass(ProcessingParams.class);

                    if(pytorchCompatibility){
                        processingParams = parseImageRecognitionProcessingParams(obj);
                    }else {
                        processingParams = gson.fromJson(serializedProcessingParams,
                                processingParamsClass);
                    }
                } catch (ClassNotFoundException e) {
                    // This can happen if one TAK ML instance from on ATAK plugin tries importing
                    // a class defined extending ProcessingParams.java that is not defined in that plugin.
                    // For example Plugin A has TFLite Processing Config, Plugin B does not. Plugin B
                    // would import this TAK ML model without the processing config. This is a limitation
                    // with the current implementation. Given that Plugin B is configured to operate without
                    // TF Lite in this example, this scenario is acceptable. As such Plugin B would simply
                    // have some knowledge of the TAK ML Model.
                    Log.w(TAG, "Class not found exception reading type of processing config, " +
                            "importing without processing config", e);
                    return;
                } catch (JSONException e) {
                    Log.e(TAG, "JSONException parsing processing config: " + serializedProcessingParams, e);
                }
            }

            Log.d(TAG, "Creating model metadata representation for " + modelFilePath);
            TakmlModel takmlModel = new TakmlModel.TakmlModelBuilder(name,
                    Uri.parse(modelFilePath), modelExtension, modelType).setLabels(labels).setProcessingParams(processingParams).build();

            // Each model is associated with an MxPlugin instance (e.g. could be several TflitePlugin instances)
            modelUUIDToPlugin.computeIfAbsent(modelUUID, k-> {
                String mxPluginName = getMxPluginClass().getName();
                MXPlugin mxPlugin;
                try {
                    Log.d(TAG, "Constructing mxPlugin: " + mxPluginName);
                    mxPlugin = MxPluginsUtil.constructMxPlugin(mxPluginName);
                } catch (Exception e) {
                    Log.e(TAG, "Exception constructing Mx Plugin: " + mxPluginName, e);
                    return null;
                }
                try {
                    Log.d(TAG, "Instantiating mxPlugin: " + mxPluginName);
                    mxPlugin.instantiate(takmlModel, MxPluginService.this);
                } catch (TakmlInitializationException e) {
                    Log.e(TAG, "Exception instantiating MxPlugin " + mxPluginName + " with TAK ML Model: " + modelUUID, e);
                    return null;
                }
                return mxPlugin;
            });
        }

        @Override
        public void execute(String requestUUID, String modelUUID, byte[] inputData) throws RemoteException {
            MXPlugin mxPlugin = modelUUIDToPlugin.get(modelUUID);
            if(mxPlugin != null){
                executorService.execute(() ->
                        mxPlugin.execute(inputData, (takmlResults, success, modelName, modelType) -> {

                    // serialize for broadcast
                    Intent intent = new Intent(SERVICE_MODEL_RESULT);
                    intent.putExtra(Constants.TAKML_MX_SERVICE_REQUEST_ID, requestUUID);
                    intent.putExtra(Constants.TAKML_MX_SERVICE_REQUEST_SUCCESS, success);
                    intent.putExtra(Constants.TAKML_MX_SERVICE_REQUEST_MODEL_NAME, modelName);
                    intent.putExtra(Constants.TAKML_MX_SERVICE_REQUEST_MODEL_TYPE, modelType);
                    intent.putParcelableArrayListExtra(Constants.TAKML_RESULT_LIST, new ArrayList<>(takmlResults));
                    sendBroadcast(intent);
                }));
            }
        }

        @Override
        public void executeTensor(String requestUUID, String modelUUID, ParcelFileDescriptor pfd) throws RemoteException {
            MXPlugin mxPlugin = modelUUIDToPlugin.get(modelUUID);
            if (mxPlugin == null || pfd == null) {
                if (pfd != null) {
                    safeClosePfd(pfd);
                }
                return;
            }

            SecureFileTransfer transfer = new SecureFileTransfer(MxPluginService.this);
            String json = transfer.readJsonFromPfd(pfd);
            if (json == null) {
                Log.e(TAG, "Failed to read JSON from ParcelFileDescriptor");
                return;
            }
            Type listType = new TypeToken<ArrayList<InferInput>>() {}.getType();
            List<InferInput> inferInputs = new Gson().fromJson(json, listType);
            executorService.submit(() -> mxPlugin.execute(inferInputs, (takmlResults, success, modelName, modelType) -> {
                Log.d(TAG, "TAK-ML Service results received. Sending back via intent.");
                // serialize for broadcast
                Intent intent = new Intent(SERVICE_MODEL_RESULT);
                intent.putExtra(Constants.TAKML_MX_SERVICE_REQUEST_ID, requestUUID);
                intent.putExtra(Constants.TAKML_MX_SERVICE_REQUEST_SUCCESS, success);
                intent.putExtra(Constants.TAKML_MX_SERVICE_REQUEST_MODEL_TYPE, modelType);
                intent.putParcelableArrayListExtra(Constants.TAKML_RESULT_LIST, new ArrayList<>(takmlResults));
                sendBroadcast(intent);
            }));
        }
    };

    private void safeClosePfd(ParcelFileDescriptor pfd) {
        try {
            if (pfd != null) {
                pfd.close();
            }
        } catch (IOException e) {
            Log.w(TAG, "Error closing ParcelFileDescriptor", e);
        }
    }

    @Override
    public void onCreate() {
        NotificationChannel serviceChannel = new NotificationChannel(ANDROID_CHANNEL_ID,
                "TAKML Mx Plugin " + this.getClass().getSimpleName()
                        + " Service",NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(serviceChannel);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification.Builder builder = new Notification.Builder(this, ANDROID_CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("TAK ML " + this.getClass().getSimpleName() + " Plugin Service Running")
                .setSmallIcon(R.drawable.tak_ml_icon_29_32)
                .setAutoCancel(true);
        Notification notification = builder.build();

        startForeground(NOTIFICATION_ID, notification);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    public abstract Class<? extends MXPlugin> getMxPluginClass();

    @Override
    public void onDestroy() {
        try {
            // Wait a while for existing tasks to terminate
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                // Cancel currently executing tasks forcefully
                executorService.shutdownNow();
                // Wait a while for tasks to respond to being cancelled
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    Log.e(TAG, "Pool did not terminate");
                }
            }
        } catch (InterruptedException ex) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
