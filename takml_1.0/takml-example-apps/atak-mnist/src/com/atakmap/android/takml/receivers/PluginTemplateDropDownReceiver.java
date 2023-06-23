
package com.atakmap.android.takml.receivers;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml.plugin.CameraActivity;
import com.atakmap.android.takml.network.NetworkClient;
import com.atakmap.android.takml.plugin.PluginTemplateLifecycle;
import com.atakmap.android.takml.plugin.R;
import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;

import com.atakmap.android.takml.takml.ClassificationEntity;
import com.atakmap.coremap.log.Log;
import com.bbn.takml_sdk_android.mx_framework.request.MXExecuteReply;
import com.bbn.takml_sdk_android.mx_framework.Recognition;

import org.apache.commons.lang3.SerializationUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class PluginTemplateDropDownReceiver extends DropDownReceiver implements
        OnStateListener, CameraActivity.CallBack{

    public static final String TAG = PluginTemplateDropDownReceiver.class
            .getSimpleName();

    public static final String SHOW_PLUGIN = "com.atakmap.android.plugintemplate.SHOW_PLUGIN";

    private final View templateView;
    private final Context pluginContext;
    private final MapView mapView;
    private ImageView imageView;
    private Button selectButton, sendButton;
    private NetworkClient networkClient;
    private String executeID;

    private Timer timer;

    private Handler mHandler;

    private TextView progressText;
    private ProgressBar progressBar;

    /**************************** CONSTRUCTOR *****************************/

    public PluginTemplateDropDownReceiver(final MapView mapView,
            final Context context, NetworkClient networkClient) {
        super(mapView);
        this.pluginContext = context;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        templateView = inflater.inflate(R.layout.main_layout, null);
        this.mapView = mapView;
        this.networkClient = networkClient;
    }

    /**************************** PUBLIC METHODS *****************************/

    public void disposeImpl() {
    }

    /**************************** INHERITED METHODS *****************************/

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(TAG, "showing plugin drop down");
        if (intent.getAction().equals(SHOW_PLUGIN)) {
            showDropDown(templateView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                    HALF_HEIGHT, false);

            mHandler = new Handler();

            CameraActivity.setCallBack(this);

            progressBar = templateView.findViewById(R.id.indeterminate_bar);
            progressText = templateView.findViewById(R.id.progress_text);

            progressBar.setVisibility(View.GONE);
            progressText.setVisibility(View.GONE);

            imageView = templateView.findViewById(R.id.imageView);
            selectButton = templateView.findViewById(R.id.backButton);

            selectButton.setText("take photo");

            sendButton = templateView.findViewById(R.id.confirmButton);

            // HANDLE IMAGES
            File sdCardDirectory = Environment.getExternalStorageDirectory();
            File image = new File(sdCardDirectory, "test.png");
            selectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                        startCamera();

                }
            });

            // HANDLE START EXECUTION
            sendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Bitmap bitmap = BitmapFactory.decodeFile(image.getPath());
                    if(bitmap != null) {
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        byte[] byteArray = stream.toByteArray();

                        ClassificationEntity.getInstance().setImage(byteArray);

                        executeID = networkClient.sendImage(byteArray,
                                PluginTemplateDropDownReceiver.this::predictionResponse);
                        progressBar.setVisibility(View.VISIBLE);
                        progressText.setVisibility(View.VISIBLE);
                    }
                }
            });

            timer = new Timer();
            timer.schedule(new TimerTask() {

                @Override
                public void run() {
                    Bitmap bitmap = BitmapFactory.decodeFile(image.getPath());
                    if(bitmap != null) {
                        android.util.Log.d(TAG, "onClick: !!! not null");

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
    }

    public void startCamera(){
        Intent intent = new Intent();
        intent.setClassName("com.atakmap.android.takml.plugin",
                CameraActivity.class.getName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PluginTemplateLifecycle.activity.getApplicationContext().startActivity(intent);

    }

    public void predictionResponse(MXExecuteReply reply) {
        Log.d(TAG, "-----PREDICTION RESPONSE-----");

        if (!reply.getExecuteID().equals(this.executeID)) {
            Log.e(TAG, "Error: execute ID does not match expected value");
            return;
        }

        byte[] output = reply.getBytes();
        ArrayList<Recognition> result;
        try {
            result = SerializationUtils.deserialize(output);
        } catch (Exception e) {
            Log.e(TAG, "Error deserializing response: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        ClassificationEntity.getInstance().setClassificationResults(result);
        timer.cancel();

        closeDropDown();
        Intent nextIntent = new Intent();
        nextIntent.setAction(ResultReceiver.SHOW_RESULT_DROPDOWN);
        AtakBroadcast.getInstance().sendBroadcast(nextIntent);
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

    @Override
    public void callback(Bitmap bitmap) {
        android.util.Log.d(TAG, "callback: hi !!!");
    }
}
