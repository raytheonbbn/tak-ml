
package com.atakmap.android.weka_example_plugin;

import static java.lang.Double.NaN;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.gui.EditText;
import com.atakmap.android.gui.PluginSpinner;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml_android.MXExecuteModelCallback;
import com.atakmap.android.takml_android.TakmlInitializationListener;
import com.atakmap.android.takml_android.takml_result.Regression;
import com.atakmap.android.weka_example_plugin.plugin.R;
import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;

import com.atakmap.android.takml_android.ModelTypeConstants;
import com.atakmap.android.takml_android.Takml;
import com.atakmap.android.takml_android.TakmlExecutor;
import com.atakmap.android.takml_android.TakmlModel;
import com.atakmap.android.takml_android.lib.TakmlInitializationException;
import com.atakmap.android.takml_android.takml_result.Recognition;
import com.atakmap.android.takml_android.takml_result.TakmlResult;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instances;

public class WekaExamplePluginDropDownReceiver extends DropDownReceiver implements
        OnStateListener {

    public static final String TAG = WekaExamplePluginDropDownReceiver.class
            .getName();

    public static final String SHOW_PLUGIN = "com.atakmap.android.weka_example_plugin.SHOW_PLUGIN";
    private final View templateView;
    private final Context pluginContext;
    private final Takml TAKML;

    private byte[] importModelFromAssets(Context context, String name){
        InputStream modelInputStream = null;
        byte[] modelBytes = null;
        try {
            modelInputStream = context.getAssets().open(name);
            modelBytes = new byte[modelInputStream.available()];
            modelInputStream.read(modelBytes);
        } catch (IOException e) {
            Log.e(TAG, "Could not read iris model from Assets", e);
        }
        return modelBytes;
    }

    /**************************** CONSTRUCTOR *****************************/

    public WekaExamplePluginDropDownReceiver(final MapView mapView,
                                             final Context context) {
        super(mapView);
        this.pluginContext = context;
        // Remember to use the PluginLayoutInflator if you are actually inflating a custom view
        // In this case, using it is not necessary - but I am putting it here to remind
        // developers to look at this Inflator
        templateView = PluginLayoutInflater.inflate(context,
                R.layout.main_layout, null);

        TAKML = new Takml(pluginContext);

        /// Import Iris Model from Assets Folder
        byte[] modelBytes = importModelFromAssets(context, "iris_model_logistic_allfeatures.model");
        // Create a TAK ML Model Wrapper
        TakmlModel takmlModel = new TakmlModel.TakmlModelBuilder("Iris Flowers Weka",
                modelBytes, ".model", ModelTypeConstants.GENERIC_RECOGNITION)
                .build();
        // Import the model to TAK ML
        TAKML.addTakmlModel(takmlModel, true);
    }

    /**************************** PUBLIC METHODS *****************************/

    public void disposeImpl() {
    }

    /**************************** INHERITED METHODS *****************************/

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: ");
        final String action = intent.getAction();
        if (action == null)
            return;

        if (action.equals(SHOW_PLUGIN)) {
            showDropDown(templateView, HALF_WIDTH, FULL_HEIGHT,
                    FULL_WIDTH, HALF_HEIGHT, false);

            instantiateIrisClassification();
            instantiateLinearRegression();

            templateView.findViewById(R.id.takmlSettingsBtn).setOnClickListener(view -> {
                Intent intent2 = new Intent();
                intent2.setAction(SHOW_PLUGIN);
                TAKML.showConfigUI(intent2);
            });
        }
    }

    private void instantiateIrisClassification(){
        TakmlExecutor takmlExecutor = null;
        TakmlModel takmlModel = TAKML.getModel("Iris Flowers Weka");
        try {
            takmlExecutor = TAKML.createExecutor(takmlModel);
        } catch (TakmlInitializationException e) {
            String error = "Could not initialize takmlExecutor: " + e.getMessage();
            Log.e(TAG, "Could not initialize takmlExecutor", e);
            Toast.makeText(pluginContext, error,
                    Toast.LENGTH_SHORT).show();
        }

        EditText pedalLengthEditText = templateView.findViewById(R.id.pedalLengthEditText);
        EditText pedalWidthEditText = templateView.findViewById(R.id.pedalWidthEditText);
        EditText sepalLengthEditText = templateView.findViewById(R.id.sepalLengthEditText);
        EditText sepalWidthEditText = templateView.findViewById(R.id.sepalWidthEditText);

        TakmlExecutor finalTakmlExecutor = takmlExecutor;
        templateView.findViewById(R.id.btn).setOnClickListener(view -> {
            ArrayList<Attribute> attributes = new ArrayList<>();
            attributes.add(new Attribute("sepallength"));
            attributes.add(new Attribute("sepalwidth"));
            attributes.add(new Attribute("petallength"));
            attributes.add(new Attribute("petalwidth"));

            List<String> attributeValues = new ArrayList<>();
            attributeValues.add("Iris-setosa");
            attributeValues.add("Iris-versicolor");
            attributeValues.add("Iris-virginica");
            attributes.add(new Attribute("@@class@@", attributeValues));

            // 2. create Instances object
            Instances test = new Instances("TestInstances", attributes, 0);

            //double[] raw={5, 3.5, 2, 0.4, NaN};
            double[] raw={Double.parseDouble(sepalLengthEditText.getText().toString()),
                    Double.parseDouble(sepalWidthEditText.getText().toString()),
                    Double.parseDouble(pedalLengthEditText.getText().toString()),
                    Double.parseDouble(pedalWidthEditText.getText().toString()), NaN};
            test.add(new DenseInstance(1.0, raw));

            byte[] serializedInput = test.toString().getBytes(StandardCharsets.UTF_8);
            finalTakmlExecutor.executePrediction(serializedInput, (takmlResults, success, modelType) -> {
                StringBuilder stringBuilder = new StringBuilder();
                for (TakmlResult takmlResult : takmlResults) {
                    Recognition recognition = (Recognition) takmlResult;
                    String result = recognition.getLabel() + " " + recognition.getConfidence();
                    stringBuilder.append(result);
                }
                Log.d(TAG, stringBuilder.toString());
                Toast.makeText(pluginContext, stringBuilder.toString(), Toast.LENGTH_LONG).show();
            });
        });
    }

    private void instantiateLinearRegression(){
        TakmlExecutor takmlExecutor = null;
        TakmlModel takmlModel = TAKML.getModel("House Price Weka");
        try {
            takmlExecutor = TAKML.createExecutor(takmlModel);
        } catch (TakmlInitializationException e) {
            String error = "Could not initialize takmlExecutor: " + e.getMessage();
            Log.e(TAG, "Could not initialize takmlExecutor", e);
            Toast.makeText(pluginContext, error,
                    Toast.LENGTH_SHORT).show();
        }

        EditText houseSizeEditText = templateView.findViewById(R.id.houseSizeEditText);
        EditText lotSizeEditText = templateView.findViewById(R.id.lotSizeEditText);
        EditText bedroomsEditText = templateView.findViewById(R.id.bedroomsEditText);
        EditText graniteEditText = templateView.findViewById(R.id.graniteEditText);
        EditText bathroomEditText = templateView.findViewById(R.id.bathroomsEditText);

        TakmlExecutor finalTakmlExecutor = takmlExecutor;
        templateView.findViewById(R.id.predictBtn).setOnClickListener(view -> {
            ArrayList<Attribute> attributes = new ArrayList<>();
            attributes.add(new Attribute("houseSize"));
            attributes.add(new Attribute("lotSize"));
            attributes.add(new Attribute("bedrooms"));
            attributes.add(new Attribute("granite"));
            attributes.add(new Attribute("bathroom"));
            attributes.add(new Attribute("sellingPrice"));

            // 2. create Instances object
            Instances test = new Instances("house", attributes, 0);

            //double[] raw={5, 3.5, 2, 0.4, NaN};
            double[] raw={Double.parseDouble(houseSizeEditText.getText().toString()),
                    Double.parseDouble(lotSizeEditText.getText().toString()),
                    Double.parseDouble(bedroomsEditText.getText().toString()),
                    Double.parseDouble(graniteEditText.getText().toString()),
                    Double.parseDouble(bathroomEditText.getText().toString()),
                    NaN};
            test.add(new DenseInstance(1.0, raw));

            byte[] serializedInput = test.toString().getBytes(StandardCharsets.UTF_8);
            finalTakmlExecutor.executePrediction(serializedInput, (takmlResults, success, modelType) -> {
                StringBuilder stringBuilder = new StringBuilder();
                for (TakmlResult takmlResult : takmlResults) {
                    Regression recognition = (Regression) takmlResult;
                    DecimalFormat formatter = new DecimalFormat("#,###.00");
                    String result = "$" + formatter.format(recognition.getPredictionResult());
                    stringBuilder.append(result);
                }
                Log.d(TAG, stringBuilder.toString());
                Toast.makeText(pluginContext, stringBuilder.toString(), Toast.LENGTH_LONG).show();
            });
        });

    }

    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownVisible(boolean v) {
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
    }

    @Override
    public void onDropDownClose() {
    }

}
