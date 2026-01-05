package com.atakmap.android.takml_android.processing_params.compatibility_layer;

import com.atakmap.android.takml_android.processing_params.ImageRecognitionProcessingParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PytorchProcessingConfigParser {
    public static ImageRecognitionProcessingParams parseImageRecognitionProcessingParams(JSONObject obj) throws JSONException {
        int modelInputWidth = obj.getInt("modelInputWidth");
        int modelInputHeight = obj.getInt("modelInputHeight");
        int tensorOutputNumberRows = obj.getInt("tensorOutputNumberRows");
        int tensorOutputNumberColumns = obj.getInt("tensorOutputNumberColumns");

        float[] normMeanRGB = jsonArrayToFloatArray(obj.getJSONArray("normMeanRGB"));
        float[] normStdRGB = jsonArrayToFloatArray(obj.getJSONArray("normStdRGB"));

        // Convert the input dimensions into a model shape (e.g., NCHW = 1,3,H,W)
        long[] modelShape = new long[] {1, 3, modelInputHeight, modelInputWidth};

        // Assume pixels need to be normalized if mean/std dev values are provided
        boolean normalize = true;

        return new ImageRecognitionProcessingParams(
                modelShape,
                modelInputWidth,
                modelInputHeight,
                normMeanRGB,
                normStdRGB,
                normalize,
                tensorOutputNumberColumns,
                tensorOutputNumberRows
        );
    }

    private static float[] jsonArrayToFloatArray(JSONArray jsonArray) throws JSONException {
        float[] array = new float[jsonArray.length()];
        for (int i = 0; i < jsonArray.length(); i++) {
            array[i] = (float) jsonArray.getDouble(i);
        }
        return array;
    }
}
