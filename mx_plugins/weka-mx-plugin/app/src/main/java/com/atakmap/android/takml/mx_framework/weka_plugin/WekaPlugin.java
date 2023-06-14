package com.atakmap.android.takml.mx_framework.weka_plugin;

import com.atakmap.android.takml_android.MXExecuteModelCallback;
import com.atakmap.android.takml_android.MXPlugin;
import com.atakmap.android.takml_android.ModelTypeConstants;
import com.atakmap.android.takml_android.TakmlModel;
import com.atakmap.android.takml_android.lib.TakmlInitializationException;
import com.atakmap.android.takml_android.takml_result.Recognition;
import com.atakmap.android.takml_android.takml_result.Regression;
import com.atakmap.android.takml_android.takml_result.TakmlResult;
import com.atakmap.coremap.log.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ArffLoader;
import weka.classifiers.functions.LinearRegression;

public class WekaPlugin implements MXPlugin {

    private static final String TAG = WekaPlugin.class.getName();
    private static final String DESCRIPTION = "Weka Plugin used for generic recognition and " +
            "linear regression";
    private static final String VERSION = "1.0";
    private static final String APPLICABLE_EXTENSION = ".model";
    private Classifier classifier;
    private LinearRegression linearRegression;
    private String modelType = null;

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public boolean isServerSide() {
        return false;
    }


    @Override
    public String[] getApplicableModelExtensions() {
        return new String[]{APPLICABLE_EXTENSION};
    }

    @Override
    public String[] getSupportedModelTypes() {
        return new String[]{ModelTypeConstants.GENERIC_RECOGNITION, ModelTypeConstants.LINEAR_REGRESSION};
    }

    @Override
    public void instantiate(TakmlModel takmlModel) throws TakmlInitializationException {
        Log.d(TAG, "Calling instantiation");

        if(takmlModel.getModelBytes() == null){
            throw new TakmlInitializationException("Model bytes is null");
        }

        if(takmlModel.getModelType().equals(ModelTypeConstants.GENERIC_RECOGNITION)) {
            try(InputStream modelBytesStream = new ByteArrayInputStream(takmlModel.getModelBytes())){
                classifier = (Classifier) SerializationHelper.read(modelBytesStream);
            } catch (IOException e) {
                throw new TakmlInitializationException("IO Error loading Weka Classifier", e);
            } catch (Exception e) {
                throw new TakmlInitializationException("Error loading Weka Classifier", e);
            }
        }else if (takmlModel.getModelType().equals(ModelTypeConstants.LINEAR_REGRESSION)) {
            try(InputStream modelBytesStream = new ByteArrayInputStream(takmlModel.getModelBytes())){
                linearRegression = (LinearRegression) SerializationHelper.read(modelBytesStream);
            } catch (IOException e) {
                throw new TakmlInitializationException("IO Error loading Weka Linear Regression", e);
            } catch (Exception e) {
                throw new TakmlInitializationException("Error loading Weka Linear Regression", e);
            }
        }else{
            throw new TakmlInitializationException("Unsupported Model Type: " + takmlModel.getModelType());
        }
        modelType = takmlModel.getModelType();
    }

    @Override
    public void execute(byte[] inputData, MXExecuteModelCallback callback) {
        Log.d(TAG,"Calling execution");

        if(inputData == null){
            Log.e(TAG, "Input data was null");
            callback.modelResult(null, false, modelType);
            return;
        }

        List<TakmlResult> recognitions = new ArrayList<>();
        String inputDataStr = new String(inputData, StandardCharsets.UTF_8);
        Instances data;
        try (BufferedReader reader = new BufferedReader(new StringReader(inputDataStr))) {
            ArffLoader.ArffReader arff = new ArffLoader.ArffReader(reader);
            data = arff.getData();
            data.setClassIndex(data.numAttributes() - 1);
        } catch (IOException e) {
            Log.e(TAG, "Could not read inputData", e);
            callback.modelResult(null, false,
                    modelType);
            return;
        }
        if(modelType.equals(ModelTypeConstants.LINEAR_REGRESSION)) {
            Log.d(TAG, "execute: " + linearRegression);
            for (int i = 0; i < data.numInstances(); i++) {
                Instance instance = data.get(i);

                // classify with confidence and result
                double output;
                try {
                    output = linearRegression.classifyInstance(instance);
                } catch (Exception e) {
                    Log.e(TAG, "Could not get distribution for instance", e);
                    continue;
                }
                recognitions.add(new Regression(output));
            }
        }else if(modelType.equals(ModelTypeConstants.GENERIC_RECOGNITION)) {
            for (int i = 0; i < data.numInstances(); i++) {
                Instance instance = data.get(i);

                // classify with confidence and result
                double[] confidences;
                try {
                    confidences = classifier.distributionForInstance(instance);
                } catch (Exception e) {
                    android.util.Log.e(TAG, "Could not get distribution for instance", e);
                    continue;
                }
                String highestConfidenceLabel = "";
                double highestConfidence = 0.0;
                for (int j = 0; j < confidences.length; j++) {
                    if (confidences[j] > highestConfidence) {
                        highestConfidence = confidences[j];
                        highestConfidenceLabel = data.classAttribute().value(j);
                    }
                }

                recognitions.add(new Recognition(highestConfidenceLabel, (float) highestConfidence));
            }
        }

        callback.modelResult(recognitions, true, modelType);
    }

    @Override
    public void shutdown() {
        Log.d(TAG, "Shutting down");
    }
}
