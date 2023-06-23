package com.atakmap.android.takml.mvc;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import com.atakmap.android.takml.receivers.GenericImageRecognitionDropDownReceiver;
import com.atakmap.coremap.log.Log;
import com.bbn.takml_sdk_android.TAKMLAppDataDirectoryQueryCallback;
import com.bbn.takml_sdk_android.TAKMLAppDataDirectoryResourcesListQueryCallback;
import com.bbn.takml_sdk_android.TAKMLFrameworkConnectionStatusCallback;
import com.bbn.takml_sdk_android.mx_framework.MXExecuteModelCallback;
import com.bbn.takml_sdk_android.mx_framework.MXInstantiateModelCallback;
import com.bbn.takml_sdk_android.mx_framework.MXListResourcesCallback;
import com.bbn.takml_sdk_android.mx_framework.request.MXExecuteReply;
import com.bbn.takml_sdk_android.mx_framework.request.MXInstantiateReply;
import com.bbn.takml_sdk_android.mx_framework.PredictionExecutor;
import com.bbn.takml_sdk_android.mx_framework.Recognition;
import com.bbn.takml_sdk_android.mx_framework.request.MXListResourcesReply;

import org.apache.commons.lang3.SerializationUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class Model implements TAKMLFrameworkConnectionStatusCallback,
        MXExecuteModelCallback, MXInstantiateModelCallback,
        MXListResourcesCallback, TAKMLAppDataDirectoryQueryCallback,
        TAKMLAppDataDirectoryResourcesListQueryCallback {

    // This class contains state that the UI will display to the user. Separating
    // the UI state from the UI implementation makes the code more modular and
    // robust to failures in the UI (when the UI fails, it can just restart and
    // display whatever state is in this Model object).

    public static final String TAG = Model.class.getSimpleName();

    public ImageRecognitionParams imageRecParams;
    public boolean connectedToFramework;
    public boolean mxPluginInstantiationStatus;
    public String currentPredictionResult;
    public String currentInstantiateToken;
    public PredictionExecutor takmlExecutor;
    public File imageFile;
    public String currentMxpInstanceID;
    public String lastMxPluginID;
    public HashMap<String, String> modelNameToMxPluginInstanceID;
    public Set<String> knownMxPlugins;
    public Set<String> knownModelFiles;
    public String takmlAppDataDirectory;
    public List<String> knownAppDataFiles;

    // reference to View / Controllers
    GenericImageRecognitionDropDownReceiver ddr;

    public Model() throws IOException {

        currentPredictionResult = "";

        modelNameToMxPluginInstanceID = new HashMap<>();

        File sdCardDirectory = Environment.getExternalStorageDirectory();
        imageRecParams = new ImageRecognitionParams();
        imageFile = new File(sdCardDirectory, "test.png");
        this.takmlExecutor = new PredictionExecutor("atak-tflite-demo-app");
        this.takmlExecutor.setFrameworkConnectionStatusCallback(this);
        this.takmlExecutor.setMxListResourcesCallback(this);
        this.takmlExecutor.requestResourcesList();
        this.takmlExecutor.setAppDataDirectoryQueryCallback(this);
        this.takmlExecutor.setAppDataDirectoryResourcesListQueryCallback(this);
        this.takmlExecutor.requestTAKMLFrameworkAppDataDirectoryResourcesList();
    }

    public void setViewControllerReference(GenericImageRecognitionDropDownReceiver ddr) {
        this.ddr = ddr;
    }

    @Override
    public void instantiateCB(MXInstantiateReply reply) {
        Log.d(TAG, "Got instantiate reply.");

        if (!currentInstantiateToken.equals(reply.getToken())) {
            currentPredictionResult = "TAK-ML responded with incorrect token for model instantiation\n" +
                            "(expected " + currentInstantiateToken + ", got " + reply.getToken() + ")";
            Log.e(TAG, currentPredictionResult);
            ddr.setHavePendingResult(false);
        } else if (!reply.isSuccess()) {
            currentPredictionResult = "Could not instantiate model: " + reply.getMsg();
            Log.e(TAG, currentPredictionResult);
            ddr.setHavePendingResult(false);
        } else {
            Log.d(TAG, "Successfully instantiated plugin " +
                    "(instance ID: " + reply.getMxpInstanceID() + ")");
            currentMxpInstanceID = reply.getMxpInstanceID();
            modelNameToMxPluginInstanceID.put(imageRecParams.modelName, currentMxpInstanceID);

            // the only reason instantiation will be called is because of a request to get
            // an image prediction, so just do an execution if instantiation is successful
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getPath());
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            takmlExecutor.executePrediction(currentMxpInstanceID, byteArray, this);
        }
    }

    @Override
    public void executeCB(MXExecuteReply reply) {

        Log.d(TAG, "Got execute reply.");
        if (reply.isSuccess()) {
            byte[] output = reply.getBytes();
            List<Recognition> result = SerializationUtils.deserialize(output);

            Log.d(TAG, "executeCB called with result: " + result);

            if (result.get(0).getConfidence() < imageRecParams.minimumConfidence) {
                currentPredictionResult =
                        "Prediction confidence was below minimum (got " +
                        result.get(0).getConfidence() + ", need at least " +
                        imageRecParams.minimumConfidence + ")";
                Log.d(TAG, currentPredictionResult);
                Log.w(TAG, currentPredictionResult);
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Predicted category: " + result.get(0).getId() + "\n");
                sb.append("Confidence: " + result.get(0).getConfidence() + "\n");
                sb.append("---\n");

                // if there is metadata associated with the model used for the prediction,
                // then append it to the result string
                if (imageRecParams.metadataLookup != null) {
                    sb.append("Category metadata: " + "\n");
                    sb.append("---\n");
                    HashMap<String, String> metadataLabels =
                            imageRecParams.metadataLookup.get(result.get(0).getId());
                    if (metadataLabels != null) {
                        for (String metadataLabel : metadataLabels.keySet()) {
                            sb.append(metadataLabel + ": " +
                                    metadataLabels.get(metadataLabel) + "\n");
                        }
                    }
                }

                currentPredictionResult = sb.toString();
            }
            Log.d(TAG, "Finished processing prediction result");
        } else {
            currentPredictionResult = "Model execution failed: " + reply.getMsg();
            Log.e(TAG, currentPredictionResult);
        }
        ddr.setHavePendingResult(false);
    }

    @Override
    public void TAKMLFrameworkConnectionChanged(boolean connectedToFramework) {
        Log.d(TAG, "Got TAKML Framework connection status update: " + connectedToFramework);

        this.connectedToFramework = connectedToFramework;
        if (!connectedToFramework) {
            currentMxpInstanceID = null;
        } else {
            takmlExecutor.requestTAKMLFrameworkAppDataDirectory();
            takmlExecutor.requestResourcesList();
            takmlExecutor.requestTAKMLFrameworkAppDataDirectoryResourcesList();
        }
        Log.d(TAG, "After update UI call.");
    }

    public void shutdown() {
        if (this.takmlExecutor != null) {
            this.takmlExecutor.destroyPlugin(this.currentMxpInstanceID);
            this.takmlExecutor.stop();
        }
    }

    @Override
    public void listResourcesCB(MXListResourcesReply reply) {
        android.util.Log.d(TAG, "list resources callback triggered: " + reply.getPlugins() + ", " + reply.getModels());
        knownMxPlugins = reply.getPlugins();
        knownModelFiles = reply.getModels();
    }

    @Override
    public void appDataDirectoryQueryResult(String takmlAppDataDirectory) {
        Log.d(TAG, "appDataDirectoryQueryResult triggered: " + takmlAppDataDirectory);
        this.takmlAppDataDirectory = takmlAppDataDirectory;
    }

    @Override
    public void appDataDirectoryResourcesListCallback(List<String> filesList) {
        Log.d(TAG, "appDataDirectoryResourcesListcallback triggered: " + filesList);
        knownAppDataFiles = filesList;
    }
}
