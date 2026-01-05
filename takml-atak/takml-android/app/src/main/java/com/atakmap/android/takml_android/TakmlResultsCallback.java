package com.atakmap.android.takml_android;

import com.atakmap.android.takml_android.takml_result.TakmlResult;

import java.util.List;

public interface TakmlResultsCallback {
    /**
     * Returns a List of TAK ML Model Results
     *
     * @param takmlResults - List of list of TAKML Results - e.g. list of tak ml results per image
     * @param success - Whether the model executions were successful
     * @param modelName - The Model that was executed
     * @param modelType - The type of model output
     */
    void modelResults(List<List<? extends TakmlResult>> takmlResults, boolean success,
                     String modelName, String modelType);
}
