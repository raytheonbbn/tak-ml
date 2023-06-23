
package com.atakmap.android.takml.receivers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml.solarflare.CameraActivity;
import com.atakmap.android.takml.solarflare.SolarFlarePredictorLifecycle;
import com.atakmap.android.takml.solarflare.R;
import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;

import com.atakmap.coremap.log.Log;
import com.bbn.takml_sdk_android.mx_framework.request.MXExecuteReply;
import com.bbn.takml_sdk_android.mx_framework.request.MXInstantiateReply;
import com.bbn.takml_sdk_android.mx_framework.PredictionExecutor;
import com.bbn.takml_sdk_android.mx_framework.request.MXListResourcesReply;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class SolarFlarePredictorDropDownReceiver extends DropDownReceiver implements
        OnStateListener, CameraActivity.CallBack {

    public static final String[] CLASSES = {"A", "B", "C", "D", "E", "F", "H"};
    public static final String[] SIZES = {"No penumbra", "Rudimentary penumbra", "Small, symmetric", "Small asymmetric", "Large, symmetric", "Large, asymmetric"};
    public static final String[] DIST = {"Undefined", "Open", "Intermediate", "Compact"};

    public static final String TAG = SolarFlarePredictorDropDownReceiver.class.getSimpleName();

    public static final String SHOW_PLUGIN = "com.atakmap.android.takml.solarflare.SHOW_PLUGIN";

    private final View templateView;
    private final Context pluginContext;
    private final MapView mapView;
    private Activity parentActivity;
    private ImageView imageView;
    private Button selectButton, processButton, sendButton;
    String spotClassPredict, spotSizePredict, spotDistPredict;
    private RadioGroup radioGroup;

    private ArrayAdapter<String> pluginIDAdapter;
    private Spinner pluginIDDropdown;
    private ArrayAdapter<String> modelAdapter;
    private Spinner modelDropdown;

    private PredictionExecutor takmlExecutor;
    private String token;
    private String mxpInstanceID;

    private Timer timer;
    private Handler mHandler;

    public SolarFlarePredictorDropDownReceiver(final MapView mapView, final Context context) {
        super(mapView);
        this.pluginContext = context;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        templateView = inflater.inflate(R.layout.main_layout, null);
        this.mapView = mapView;
        this.parentActivity = (Activity)mapView.getContext();

        this.mHandler = new Handler();

        try {
            this.takmlExecutor = new PredictionExecutor("solar-flare-demo-app");
            this.takmlExecutor.setMxListResourcesCallback(this::listResourcesCB);
            this.takmlExecutor.requestResourcesList();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Not connected to TAK-ML");
        }

        CameraActivity.setCallBack(this);

        imageView = templateView.findViewById(R.id.imageView);
        selectButton = templateView.findViewById(R.id.backButton);

        selectButton.setText("Take Photo");

        File sdCardDirectory = Environment.getExternalStorageDirectory();
        File image = new File(sdCardDirectory, "test.png");
        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCamera();
            }
        });

        processButton = templateView.findViewById(R.id.confirmButton);
        processButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView spotClass = (TextView)templateView.findViewById(R.id.spotClassResult);
                spotClass.setText(CLASSES[new Random().nextInt(CLASSES.length)]);
                spotClassPredict = spotClass.getText().toString();

                TextView spotSize = (TextView)templateView.findViewById(R.id.spotSizeResult);
                spotSize.setText(SIZES[new Random().nextInt(SIZES.length)]);
                spotSizePredict = spotSize.getText().toString();
            }
        });

        radioGroup = (RadioGroup)templateView.findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton radioButton = (RadioButton)group.findViewById(checkedId);
                spotDistPredict = radioButton.getText().toString();
            }
        });

        this.pluginIDDropdown = (Spinner)templateView.findViewById(R.id.pluginIdValue);
        this.modelDropdown = (Spinner)templateView.findViewById(R.id.modelValue);

        sendButton = templateView.findViewById(R.id.predict);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (takmlExecutor == null) {
                    try {
                        takmlExecutor = new PredictionExecutor("solar-flare-demo-app");
                        takmlExecutor.setMxListResourcesCallback(SolarFlarePredictorDropDownReceiver.this::listResourcesCB);
                        takmlExecutor.requestResourcesList();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Cannot connect to TAK-ML");
                        connectionError("Error: cannot connect to TAK-ML");
                        return;
                    }
                }

                if (!takmlExecutor.isConnected()) {
                    connectionError("Error: cannot connect to TAK-ML");
                    return;
                }

                String pluginLabel = pluginIDDropdown.getSelectedItem().toString();
                String pluginID = PredictionExecutor.pluginLabelToID(pluginLabel);

                String modelLabel = modelDropdown.getSelectedItem().toString();
                String model = PredictionExecutor.modelLabelToName(modelLabel);

                HashMap<String, Serializable> params = new HashMap<String, Serializable>();
                String[] attrs = {"class_A", "class_B", "class_C", "class_D", "class_E", "class_F", "class_H",
                        "spot_X", "spot_R", "spot_S", "spot_A", "spot_H", "spot_K",
                        "dist_X", "dist_O", "dist_I", "dist_C",
                        "activity", "evolution_decay", "evolution_no", "evolution_growth",
                        "prev_1", "prev_2", "prev_3", "complex", "pass", "area", "largest_area",
                        "c_class"
                };
                params.put("attrNames", attrs);
                params.put("classIndex", attrs.length - 1);

                token = takmlExecutor.instantiatePlugin(
                        pluginID, model, params,
                        SolarFlarePredictorDropDownReceiver.this::instantiateCB);
                if (token == null) {
                    connectionError("Error: can't instantiate plugin " + pluginID + " with model " + model);
                    return;
                }
            }
        });

        this.timer = new Timer();
        this.timer.schedule(new TimerTask() {

            @Override
            public void run() {
                Bitmap bitmap = BitmapFactory.decodeFile(image.getPath());
                if (bitmap != null) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            imageView.setImageBitmap(bitmap);
                        }
                    });
                }
            }

        },0,1000);//Update text every second
    }

    public void disposeImpl() {
        if (this.takmlExecutor != null)
            this.takmlExecutor.stop();
    }

    private static int indexOfString(String[] arr, String s, int defaultVal) {
        for (int i = 0; i < arr.length; i++) {
            if (s.equals(arr[i]))
                return i;
        }
        return defaultVal;
    }

    public void connectionError(String msg) {
        TextView solarFlare = (TextView)templateView.findViewById(R.id.solarFlarePredictionResult);
        solarFlare.setText("Error: cannot connect to TAK-ML");
    }

    public void instantiateCB(MXInstantiateReply reply) {
        String token = this.token;
        if (!token.equals(reply.getToken())) {
            TextView solarFlare = (TextView)templateView.findViewById(R.id.solarFlarePredictionResult);
            solarFlare.setText("Error: Token returned by TAK-ML for instantiating plugin does not match expected value");
            return;
        }

        /* Clear token for next attempt. */
        this.token = null;

        if (!reply.isSuccess()) {
            TextView solarFlare = (TextView)templateView.findViewById(R.id.solarFlarePredictionResult);
            solarFlare.setText("Error: " + reply.getMsg());
            return;
        }
        this.mxpInstanceID = reply.getMxpInstanceID();

        String request = indexOfString(CLASSES, spotClassPredict, 0) + "," +
                indexOfString(SIZES, spotSizePredict, 0) + "," +
                indexOfString(DIST, spotDistPredict, 0) + "," +
                "0,1,0,0,0,0,1,0,0,0,0,1,0,0,1,0,0,1,1,0,0,1,1,0,0";

        String executeID = takmlExecutor.executePrediction(mxpInstanceID,
                request.getBytes(), SolarFlarePredictorDropDownReceiver.this::executeResponse);
        if (executeID == null) {
            connectionError("Error: can't execute plugin " + reply.getPluginID() + " instance " + mxpInstanceID);
            destroyMxpInstance();
        }
    }

    private void destroyMxpInstance() {
        if (!this.takmlExecutor.destroyPlugin(this.mxpInstanceID)) {
            Log.e(TAG, "Could not destroy instance " + this.mxpInstanceID);
        }
        this.mxpInstanceID = null;
    }

    public void executeResponse(MXExecuteReply reply) {
        this.timer.cancel();

        if (!reply.isSuccess()) {
            TextView solarFlare = (TextView)templateView.findViewById(R.id.solarFlarePredictionResult);
            solarFlare.setText("Error: " + reply.getMsg());
            destroyMxpInstance();
            return;
        }

        Double result = ByteBuffer.wrap(reply.getBytes()).getDouble();
        TextView solarFlare = (TextView)templateView.findViewById(R.id.solarFlarePredictionResult);
        solarFlare.setText("Estimated number of C-class solar flares: " + result);

        destroyMxpInstance();
    }

    public void listResourcesCB(MXListResourcesReply reply) {
        parentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Set<String> pluginLabels = reply.getPlugins();
                String[] pluginStrings = new String[pluginLabels.size()];
                pluginLabels.toArray(pluginStrings);
                pluginIDAdapter = new ArrayAdapter<String>(pluginContext,
                        android.R.layout.simple_spinner_item, pluginStrings);
                pluginIDAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                pluginIDDropdown.setAdapter(pluginIDAdapter);
                pluginIDAdapter.notifyDataSetChanged();

                Set<String> modelLabels = reply.getModels();
                String[] modelStrings = new String[modelLabels.size()];
                modelLabels.toArray(modelStrings);
                modelAdapter = new ArrayAdapter<String>(pluginContext,
                        android.R.layout.simple_spinner_item, modelStrings);
                modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                modelDropdown.setAdapter(modelAdapter);
                modelAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(TAG, "showing plugin drop down");
        if (intent.getAction().equals(SHOW_PLUGIN)) {
            showDropDown(templateView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                    HALF_HEIGHT, false);
        }
    }

    public void startCamera(){
        Intent intent = new Intent();
        intent.setClassName("com.atakmap.android.takml.solarflare",
                CameraActivity.class.getName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        SolarFlarePredictorLifecycle.activity.getApplicationContext().startActivity(intent);
    }

    @Override
    public void onDropDownSelectionRemoved() {
        return;
    }

    @Override
    public void onDropDownVisible(boolean v) {
        return;
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
        return;
    }

    @Override
    public void onDropDownClose() {
        return;
    }

    @Override
    public void callback(Bitmap bitmap) {
        return;
    }
}
