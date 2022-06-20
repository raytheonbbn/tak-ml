package com.atakmap.android.takml.mvc;

import com.atakmap.coremap.log.Log;
import com.bbn.tak.ml.TakMlConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ImageRecognitionParams {

    public static final String TAG = ImageRecognitionParams.class.getSimpleName();
    public static final float DEFAULT_MINIMUM_CONFIDENCE = 0.7f;

    public ImageRecognitionParams() {
        mxPluginParams = new HashMap<>();
    }

    public String mxPluginName;
    public String mxPluginId;
    public String modelName;
    public float minimumConfidence = DEFAULT_MINIMUM_CONFIDENCE;
    public HashMap<String, Serializable> mxPluginParams;
    public String metadataLookupFileName;
    public HashMap<String, HashMap<String, String>> metadataLookup = null;

    public boolean isProperlySet() {
        return
                mxPluginName != null &&
                        mxPluginId != null &&
                        modelName != null;

    }

    // assumes that the metadata is formatted as JSON with the structure:
//     {
//       "category_label_1" : {
//          "metadata_label_1" : "some text",
//          "metadata_label_2" : "some other text",
//          ...
//       }
//
//      "category_label_2" : {
//          "metadata_label_1" : "some text",
//          "metadata_label_2" : "some other text",
//          ...
//       }
//
//       ...
//
//     }
    public static HashMap<String, HashMap<String, String>> loadModelMetaData(
            String takmlAppDataDirectoryPath, String metadataLookupFileName) {

        HashMap<String, HashMap<String, String>> ret = new HashMap<>();

        if (takmlAppDataDirectoryPath == null) {
            Log.e(TAG, "Failed to load model metadata, takml app data directory was null.");
            return null;
        }

        Log.d(TAG, "Attempting to load model metadata from: " +
                takmlAppDataDirectoryPath + "/" +
                metadataLookupFileName);

        File f = new File(
                takmlAppDataDirectoryPath + "/" +
                        metadataLookupFileName);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(f);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));

            String jsonString = "";
            String line;
            while ((line = reader.readLine()) != null) {
                jsonString += line + "\n";
            }

            Log.d(TAG, "Read json string: " + jsonString);

            JSONObject root = new JSONObject(jsonString);

            Iterator<String> categories = root.keys();

            while (categories.hasNext()) {
                String category = categories.next();

                JSONObject metadata = root.getJSONObject(category);

                Iterator<String> fields = metadata.keys();

                HashMap<String, String> metadataLabels = new HashMap<>();

                while (fields.hasNext()) {
                    String field = fields.next();
                    metadataLabels.put(field, metadata.getString(field));
                }

                ret.put(category, metadataLabels);
            }

        }
        catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        return ret;
    }

    public class PrettyPrintingMap<K, V> {
        private Map<K, V> map;

        public PrettyPrintingMap(Map<K, V> map) {
            this.map = map;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            Iterator<Map.Entry<K, V>> iter = map.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<K, V> entry = iter.next();
                sb.append(entry.getKey());
                sb.append('=').append('"');
                sb.append(entry.getValue());
                sb.append('"');
                if (iter.hasNext()) {
                    sb.append(',').append("\n");
                }
            }
            return sb.toString();

        }
    }

}
