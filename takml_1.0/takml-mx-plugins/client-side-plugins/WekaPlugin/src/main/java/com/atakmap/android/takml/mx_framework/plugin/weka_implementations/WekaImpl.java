package com.atakmap.android.takml.mx_framework.plugin.weka_implementations;

import android.os.Environment;
import android.util.Log;

import com.bbn.takml_sdk_android.mx_framework.Recognition;
import com.github.fge.jackson.JsonLoader;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import de.fraunhofer.iosb.ilt.frostserver.json.deserialize.EntityParser;
import de.fraunhofer.iosb.ilt.frostserver.model.Observation;
import de.fraunhofer.iosb.ilt.frostserver.model.core.IdString;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ArffLoader;


public class WekaImpl {

    EntityParser parser;

    private static final String TAG = WekaImpl.class.getSimpleName();

    public WekaImpl() {
        parser = new EntityParser(IdString.class);
    }

    public List<Recognition> execute(byte[] inputData, String modelDirectoryName,
                                     String modelFileName) {

        Classifier cls;
        try {
            cls = (Classifier) SerializationHelper.read(modelDirectoryName + "/" + modelFileName);
        } catch (Exception e) {
            Log.e(TAG, "Error loading Weka Classifier: " + e.toString());
            return returnError();
        }

        Log.d(TAG, "Successfully loaded classifier (" + modelFileName + ").");

        Log.d(TAG, "Starting execution.");

        String serializedObservation = new String(inputData, StandardCharsets.UTF_8);
        Observation observation = null;
        try {
            observation = parser.parseObservation(serializedObservation);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to deserialize observation: " + e.getMessage());
            return returnError();
        }

        Log.d(TAG, "Successfully loaded Observation from MLA Plugin.");

        Instances data = null;
        try {
            BufferedReader reader =
                    new BufferedReader(new StringReader(observation.getResult().toString()));
            ArffLoader.ArffReader arff = new ArffLoader.ArffReader(reader);
            data = arff.getData();
            data.setClassIndex(data.numAttributes() - 1);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to load arff data: " + e.getMessage());
            return returnError();
        }

//        // code to load test .arff file
//        try {
//            BufferedReader reader =
//                    new BufferedReader(new FileReader(modelDirectoryName + "/" + "User03-and-other-TEST03.arff"));
//            ArffLoader.ArffReader arff = new ArffLoader.ArffReader(reader);
//            data = arff.getData();
//            data.setClassIndex(data.numAttributes() - 1);
//        } catch (IOException e) {
//            e.printStackTrace();
//            Log.e(TAG, "Failed to load arff data: " + e.getMessage());
//            return returnError();
//        }

        Log.d(TAG, "Successfully loaded .arff into Instances object.");

        ArrayList<Recognition> ret = new ArrayList<>();
        try {
            for (int i = 0; i < data.numInstances(); i++) {
                Instance instance = data.get(i);

//                // classify without the confidence, just the result
//                double val = cls.classifyInstance(data.firstInstance());
//                String result = data.firstInstance().classAttribute().value((int) val);

                // classify with confidence and result
                double[] confidences = cls.distributionForInstance(instance);
                String highestConfidenceLabel = "";
                double highestConfidence = 0.0;
                for (int j = 0; j < confidences.length; j++) {
                    if (confidences[j] > highestConfidence) {
                        highestConfidence = confidences[j];
                        highestConfidenceLabel = data.classAttribute().value(j);
                    }
                }

                ret.add(new Recognition(highestConfidenceLabel, "weka-classification", (float) highestConfidence));

            }
        } catch (Exception e) {
            Log.e(TAG, "Error occurred while trying to classify instance: " + e.toString());
            e.printStackTrace();
            return returnError();
        }

        Log.d(TAG, "Successfully did execution.");

        return ret;
    }

    private List<Recognition> returnError() {
        ArrayList<Recognition> ret = new ArrayList<>();
        ret.add(new Recognition("prediction_failed", "", 0.0f));
        return ret;
    }


    private String getResultsRawString(List<Recognition> results) {
        String ret = "";
        for (Recognition r : results) {
            ret += r.toString() + "\n";
        }
        return ret;
    }

    private String getResultsSummaryString(List<Recognition> results) {
        HashMap<String, Integer> idTallies = new HashMap<>();

        for (Recognition r : results) {
            if (!idTallies.containsKey(r.getId())) {
                idTallies.put(r.getId(), 1);
            } else {
                idTallies.put(r.getId(), idTallies.get(r.getId()) + 1);
            }
        }
        com.atakmap.coremap.log.Log.d(TAG, "Tally results: " + idTallies);

        String highestTallyId = "";
        int highestTally = 0;
        for (String key : idTallies.keySet()) {
            if (idTallies.get(key) > highestTally) {
                highestTallyId = key;
                highestTally = idTallies.get(key);
            }
        }

        return "most predicted id: " + highestTallyId + ", ratio of predictions (" + highestTallyId + "/total): " + highestTally + "/" +
                results.size();
    }

}
