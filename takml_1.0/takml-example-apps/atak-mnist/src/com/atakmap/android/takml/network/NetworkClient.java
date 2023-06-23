package com.atakmap.android.takml.network;

import android.os.Handler;
import android.util.Log;

import com.bbn.takml_sdk_android.mx_framework.MXExecuteModelCallback;
import com.bbn.takml_sdk_android.mx_framework.PredictionExecutor;

public class NetworkClient {
    private PredictionExecutor takmlExecutor;
    private String mxpInstanceID;

    public NetworkClient(PredictionExecutor takmlExecutor) {
        this.takmlExecutor = takmlExecutor;
    }

    public String sendImage(byte[] fileContent, MXExecuteModelCallback cb) {
        if (this.mxpInstanceID == null) {
            // print error;
            return null;
        }
        String executeID = this.takmlExecutor.executePrediction(this.mxpInstanceID,
                fileContent, cb);
        if (executeID == null) {
            //connectionError("Error: can't execute plugin " + reply.getPluginID() + " instance " + this.mxpInstanceID);
            //destroyMxpInstance(this.mxpInstanceID);
            //resetState();
        }
        return executeID;
    }

    public void setMxpInstanceID(String mxpInstanceID) {
        this.mxpInstanceID = mxpInstanceID;
    }

}
