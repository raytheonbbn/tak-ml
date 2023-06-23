
package com.atakmap.android.takml_example;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.gui.PluginSpinner;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml_android.ProcessingParams;
import com.atakmap.android.takml_android.pytorch_mx_plugin.PytorchObjectDetectionParams;
import com.atakmap.android.takml_example.plugin.R;
import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;

import com.atakmap.android.takml_android.MXExecuteModelCallback;
import com.atakmap.android.takml_android.ModelTypeConstants;
import com.atakmap.android.takml_android.Takml;
import com.atakmap.android.takml_android.TakmlExecutor;
import com.atakmap.android.takml_android.TakmlInitializationListener;
import com.atakmap.android.takml_android.TakmlModel;
import com.atakmap.android.takml_android.lib.TakmlInitializationException;
import com.atakmap.android.takml_android.takml_result.Recognition;
import com.atakmap.android.takml_android.takml_result.TakmlResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TakmlExampleDropDownReceiver extends DropDownReceiver implements
        OnStateListener {

    public static final String TAG = TakmlExampleDropDownReceiver.class
            .getName();

    public static final String SHOW_PLUGIN = TakmlExampleDropDownReceiver.class.getName() + "_SHOW_PLUGIN";
    private final View templateView;
    private final Context pluginContext;
    private Bitmap carsImage;
    private final Bitmap catImage;
    private final Bitmap dogImage;
    private Bitmap saladImage;
    private static byte[] carsImageBytes, catImageBytes, dogImageBytes, saladImageBytes, selectedImageBytes;

    private PluginSpinner pluginSpinner;
    private Takml takml;
    private TakmlExecutor takmlExecutor;

    private int selectedIndex = 0;

    private final static int OBJECT_DETECTION_TEXT_X = 15;
    private final static int OBJECT_DETECTION_TEXT_Y = 20;
    private final static int OBJECT_DETECTION_TEXT_WIDTH = 70;
    private final static int OBJECT_DETECTION_TEXT_HEIGHT = 25;
    private ImageView imageView;

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

    private Bitmap loadImageBitmap(String assetName){
        InputStream inputStream = null;
        try {
            inputStream = pluginContext.getAssets().open(assetName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return BitmapFactory.decodeStream(inputStream);
    }

    /**************************** CONSTRUCTOR *****************************/

    public TakmlExampleDropDownReceiver(final MapView mapView,
                                        final Context context) {
        super(mapView);

        this.pluginContext = context;
        // Remember to use the PluginLayoutInflator if you are actually inflating a custom view
        // In this case, using it is not necessary - but I am putting it here to remind
        // developers to look at this Inflator
        templateView = PluginLayoutInflater.inflate(context,
                R.layout.main_layout, null);

        imageView = templateView.findViewById(R.id.image);
        imageView.buildDrawingCache();

        /// load the cars image
        carsImage = loadImageBitmap("vehicles.png");
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        carsImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
        carsImage = Bitmap.createScaledBitmap(carsImage, 640, 640,
                true);
        carsImageBytes = stream.toByteArray();
        /// load the cat image
        catImage = loadImageBitmap("cat.jpeg");
        ByteArrayOutputStream stream2 = new ByteArrayOutputStream();
        catImage.compress(Bitmap.CompressFormat.PNG, 100, stream2);
        catImageBytes = stream2.toByteArray();
        /// load the dogs image

        dogImage = loadImageBitmap("dog.png");
        ByteArrayOutputStream stream3 = new ByteArrayOutputStream();
        dogImage.compress(Bitmap.CompressFormat.PNG, 100, stream3);
        dogImageBytes = stream3.toByteArray();
        /// load the salad image
        saladImage = loadImageBitmap("salad.png");
        ByteArrayOutputStream stream4 = new ByteArrayOutputStream();
        saladImage = Bitmap.createScaledBitmap(saladImage, 420, 420,
                true);
        saladImage.compress(Bitmap.CompressFormat.PNG, 100, stream4);
        saladImageBytes = stream4.toByteArray();

        takml = new Takml(pluginContext);

        /// Import Iris Model from Assets Folder
        byte[] modelBytes = importModelFromAssets(context, "dogs_cats_model.torchscript");
        // Create a TAK ML Model Wrapper
        TakmlModel takmlModel = new TakmlModel.TakmlModelBuilder("Dogs and Cats Pytorch",
                modelBytes, ".torchscript", ModelTypeConstants.IMAGE_CLASSIFICATION)
                .setLabels(Arrays.asList("cat", "dog"))
                .build();
        // Import the model to TAK ML
        takml.addTakmlModel(takmlModel, true);

        takml.addInitializationListener(new TakmlInitializationListener() {
            @Override
            public void finishedInitializing() {
                TakmlModel takmlModel = takml.getModel("Dogs and Cats Pytorch");
                try {
                    takmlExecutor = takml.createExecutor(takmlModel);
                } catch (TakmlInitializationException e) {
                    Log.e(TAG, "Could not initialize takmlExecutor", e);
                }
            }
        });
    }

    /**************************** PUBLIC METHODS *****************************/

    public void disposeImpl() {
    }

    /**************************** INHERITED METHODS *****************************/

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action == null)
            return;

        if (action.equals(SHOW_PLUGIN)) {
            showDropDown(templateView, HALF_WIDTH, FULL_HEIGHT,
                    FULL_WIDTH, HALF_HEIGHT, false);

            imageView = templateView.findViewById(R.id.image);
            pluginSpinner = templateView.findViewById(R.id.spinnerImage);
            List<String> displayNames = new ArrayList<>(Arrays.asList("Vehicles", "Cat", "Dog", "Salad"));
            Collections.sort(displayNames);
            ArrayAdapter<String> configArrayAdapter = new ArrayAdapter<>(
                    context, android.R.layout.simple_spinner_item, displayNames);
            configArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            pluginSpinner.setAdapter(configArrayAdapter);
            pluginSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    if (view instanceof TextView) {
                        ((TextView) view).setTextColor(Color.WHITE);
                    }
                    selectedIndex = i;
                    if(pluginSpinner.getSelectedItem().toString().equals("Vehicles")){
                        imageView.setImageBitmap(carsImage);
                        selectedImageBytes = carsImageBytes.clone();
                    }else if(pluginSpinner.getSelectedItem().toString().equals("Cat")){
                        imageView.setImageBitmap(catImage);
                        selectedImageBytes = catImageBytes.clone();
                    }else if(pluginSpinner.getSelectedItem().toString().equals("Dog")){
                        imageView.setImageBitmap(dogImage);
                        selectedImageBytes = dogImageBytes.clone();
                    }else{
                        imageView.setImageBitmap(saladImage);
                        selectedImageBytes = saladImageBytes.clone();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });
            pluginSpinner.setSelection(selectedIndex);

            /**
             * View available TAK ML Models
             */
            Log.d(TAG, "Models Available: " + takml.getModels());

            /**
             * View Selected TAK ML Model
             */
            //Log.d(TAG, "selected model: " + takmlExecutor.getSelectedModel().getName());

            /**
             * View Selected Model Execution Plugin
             */
            //Log.d(TAG, "selected plugin: " + takmlExecutor.getSelectedModelExecutionPlugin());

            /**
             * View Model Execution Plugins
             */
            for(String plugin : takml.getModelExecutionPlugins()){
                Log.d(TAG, "MX Plugin Available: " + plugin.getClass());
            }

            TakmlExecutor finalTakmlExecutor = takmlExecutor;
            templateView.findViewById(R.id.btn).setOnClickListener(view -> {
                Log.d(TAG, "onReceive: " + selectedImageBytes.length);
                finalTakmlExecutor.executePrediction(selectedImageBytes, new MXExecuteModelCallback() {
                    @Override
                    public void modelResult(List<? extends TakmlResult> takmlResults, boolean success, String modelType) {
                        Log.d(TAG, "Got execute reply.");
                        if (!success) {
                            Log.e(TAG, "Could not execute request");
                            return;
                        }

                        ProcessingParams processingParams = takmlExecutor.getSelectedModel().getProcessingParams();
                        int outputWidth = 420;
                        int outputHeight = 420;
                        if(processingParams != null){
                            if(processingParams instanceof PytorchObjectDetectionParams){
                                PytorchObjectDetectionParams pytorchObjectDetectionParams =
                                        (PytorchObjectDetectionParams) processingParams;
                                outputWidth = pytorchObjectDetectionParams.getModelInputWidth();
                                outputHeight = pytorchObjectDetectionParams.getModelInputHeight();
                            }
                        }

                        StringBuilder stringBuilder = new StringBuilder();
                        if(modelType.equals(ModelTypeConstants.IMAGE_CLASSIFICATION)){
                            Recognition recognition = (Recognition) takmlResults.get(0);
                            stringBuilder.append(recognition.getLabel())
                                    .append(", confidence = ")
                                    .append(recognition.getConfidence());
                        }else{
                            imageView.buildDrawingCache();
                            Bitmap bmap = imageView.getDrawingCache();
                            Bitmap bmp2 = bmap.copy(bmap.getConfig(), true);
                            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bmp2, outputWidth, outputHeight,
                                        true);
                            Canvas canvas = new Canvas(resizedBitmap);
                            for(TakmlResult takmlResult : takmlResults) {
                                Recognition objectDetection = (Recognition) takmlResult;
                                if(objectDetection.getConfidence() < 0.3){
                                    continue;
                                }
                                Recognition recognition = (Recognition) takmlResult;
                                stringBuilder.append(recognition.getLabel())
                                        .append(", confidence = ")
                                        .append(recognition.getConfidence())
                                        .append(" ");

                                Rect rect = new Rect((int) objectDetection.getLeft(),
                                        (int) objectDetection.getTop(),
                                        (int) objectDetection.getRight(),
                                        (int) objectDetection.getBottom());

                                Paint mPaintRectangle = new Paint();
                                mPaintRectangle.setColor(Color.YELLOW);
                                mPaintRectangle.setStrokeWidth(5);
                                mPaintRectangle.setStyle(Paint.Style.STROKE);

                                Paint mPaintText = new Paint();
                                canvas.drawRect(rect, mPaintRectangle);

                                Path mPath = new Path();
                                RectF mRectF = new RectF(rect.left, rect.top, rect.left + OBJECT_DETECTION_TEXT_WIDTH, rect.top + OBJECT_DETECTION_TEXT_HEIGHT);
                                mPath.addRect(mRectF, Path.Direction.CW);
                                mPaintText.setColor(Color.MAGENTA);
                                canvas.drawPath(mPath, mPaintText);

                                mPaintText.setColor(Color.WHITE);
                                mPaintText.setStrokeWidth(0);
                                mPaintText.setStyle(Paint.Style.FILL);
                                mPaintText.setTextSize(32);
                                canvas.drawText(String.format("%s", objectDetection.getLabel()), rect.left + OBJECT_DETECTION_TEXT_X, rect.top + OBJECT_DETECTION_TEXT_Y, mPaintText);
                            }
                            imageView.setImageBitmap(resizedBitmap);
                        }

                        new Handler(Looper.getMainLooper()).post(() ->
                                Toast.makeText(pluginContext, stringBuilder.toString(),
                                        Toast.LENGTH_SHORT).show());
                        Log.d(TAG, "Finished processing prediction result");
                    }
                });
            });

            templateView.findViewById(R.id.takmlSettingsBtn).setOnClickListener(view -> {
                Intent settingsIntent = new Intent();
                settingsIntent.setAction(SHOW_PLUGIN);
                finalTakmlExecutor.showConfigUI(settingsIntent);
            });
        }
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
