package com.bbn.takml_server.mx;

import com.bbn.takml_server.model_execution.takml_result.Recognition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class PostProcessorUtil {
    private static float[] softmax(float[] logits) {
        float sum = 0f;
        float[] softmaxValues = new float[logits.length];

        // Exponentiate each logit and sum them up
        for (int i = 0; i < logits.length; i++) {
            softmaxValues[i] = (float) Math.exp(logits[i]);
            sum += softmaxValues[i];
        }

        // Normalize by dividing each value by the sum
        for (int i = 0; i < softmaxValues.length; i++) {
            softmaxValues[i] /= sum;
        }

        return softmaxValues;
    }

    public static List<Recognition> getProbabilities(float[] probabilities, boolean softmax, List<String> labels) {
        List<Recognition> results = new ArrayList<>();
        float[] probabilitiesOrdered = probabilities;
        if(softmax){
            probabilitiesOrdered = softmax(probabilities);
        }
        int[] indices = new int[probabilitiesOrdered.length];
        for (int i = 0; i < probabilitiesOrdered.length; i++) indices[i] = i;

        SortedMap<Float, String> probabilityToLabel = new TreeMap<>(Collections.reverseOrder());

        for (int index : indices) {
            probabilityToLabel.put(probabilitiesOrdered[index], labels.get(index));
        }
        for(Map.Entry<Float, String> entry: probabilityToLabel.entrySet()){
            results.add(new Recognition(entry.getValue(), entry.getKey()));
        }
        return results;
    }
}
