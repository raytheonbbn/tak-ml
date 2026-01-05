package com.atakmap.android.takml_android;

import com.atakmap.android.takml_android.takml_result.TakmlResult;

import java.util.List;

public interface MXExecuteModelCallback {
    /**
     * Returns a List of TAK ML Model Results
     *
     * @param takmlResults - List of list of TAKML Results - e.g. list of tak ml results per image
     * @param success - Whether the model execution was successful
     * @param modelName - The Model that was executed
     * @param modelType - The type of model output
     */
    void modelResult(List<? extends TakmlResult> takmlResults, boolean success,
                     String modelName, String modelType);
}