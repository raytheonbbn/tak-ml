
package com.atakmap.android.takml.receivers;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.atakmap.android.atakutils.MiscUtils;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml.plugin.CameraActivity;
import com.atakmap.android.takml.plugin.LoadFileActivity;
import com.atakmap.android.takml.plugin.GenericImageRecognitionLifecycle;
import com.atakmap.android.takml.plugin.R;
import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;

import com.atakmap.android.takml.mvc.Model;
import com.atakmap.coremap.log.Log;
import com.bbn.tak.ml.TakMlConstants;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.atakmap.android.takml.receivers.SettingsDropDownReceiver.SHOW_SETTINGS;

public class GenericImageRecognitionDropDownReceiver extends DropDownReceiver implements
        OnStateListener {

    public static final String TAG = GenericImageRecognitionDropDownReceiver.class
            .getSimpleName();

    private final int MSG_UPDATE_UI = 0;
    private final int UI_UPDATE_INTERVAL_MS = 100;

    public static final String SHOW_PLUGIN = "com.atakmap.android.plugintemplate.SHOW_GENERIC_IMAGE_RECOGNITION_PLUGIN";

    private final View templateView;
    private final Context pluginContext;
    private ImageView imageView;
    private ImageButton takeNewImageButton, loadNewImageButton;
    private TextView predictionResultsDisplay;
    private TextView connectionStatusDisplay;
    private Handler mHandler;
    private Button settingsButton, sendButton;
    private ProgressBar pendingBar;
    private boolean imageChanged = true;
    private FileObserver fo;

    private static final String WAITING_TEXT = "Waiting for result";
    private Object enableUIToken;

    private boolean havePendingResult = false;

    // the UI will be automatically disabled every time the user tries to start a prediction,
    // to prevent the user from spamming buttons and breaking the application - in order to
    // avoid the user not being able to interact with the UI if the TAKML Framework fails
    // to reply to an instantiation or execution request, here is a time limit on the maximum
    // amount of time the UI will be disabled after every prediction attempt by the user.
    private static final int MAX_UI_DISABLE_TIME_MS = 10000;

    private Model model;

    /**************************** CONSTRUCTOR *****************************/

    public GenericImageRecognitionDropDownReceiver(final MapView mapView, final Context context,
                                                   Model model) {
        super(mapView);
        this.pluginContext = context;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        templateView = inflater.inflate(R.layout.main_layout, null);
        this.model = model;
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

            fo = new FileObserver(model.imageFile.getAbsolutePath(), FileObserver.MODIFY) {
                @Override
                public void onEvent(int event, @Nullable String file) {
                    Log.d(TAG, "Detected modification of image file.");
                    imageChanged = true;
                }
            };
            fo.startWatching();

            mHandler = new Handler();

            imageView = templateView.findViewById(R.id.imageView);

            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    imageChanged = true;
                }
            });

            takeNewImageButton = templateView.findViewById(R.id.takeNewImageButton);
            loadNewImageButton = templateView.findViewById(R.id.loadNewImageButton);
            sendButton = templateView.findViewById(R.id.getPredictionButton);
            predictionResultsDisplay = templateView.findViewById(R.id.predictionResultDisplay);
            connectionStatusDisplay = templateView.findViewById(R.id.frameworkConnectionStatusDisplay);
            pendingBar = templateView.findViewById(R.id.pendingBar);

            takeNewImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    model.currentPredictionResult = "";
                    startCamera();
                }
            });

            loadNewImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    model.currentPredictionResult = "";
                    startLoadActivity();
                }
            });

            sendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!model.imageRecParams.isProperlySet()) {
                        MiscUtils.toast("Please properly set parameters in settings.");

                        return;
                    }

                    Bitmap bitmap = BitmapFactory.decodeFile(model.imageFile.getPath());
                    if (bitmap != null) {
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        byte[] byteArray = stream.toByteArray();

                        if (model.connectedToFramework) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            model.currentPredictionResult = WAITING_TEXT;
                                            predictionResultsDisplay.setText(model.currentPredictionResult + "\n");
                                        }
                                    });
                                }
                            });

                            String expectedMxpInstanceID = model.modelNameToMxPluginInstanceID.get(model.imageRecParams.modelName);

                            Log.d(TAG, "Send button pressed with following state: " + "\n" +
                                    "expectedMxpInstanceID: " + expectedMxpInstanceID + "\n" +
                                    "currentMxpInstanceID: " + model.currentMxpInstanceID);

                            if (model.currentMxpInstanceID != null && expectedMxpInstanceID != null &&
                                model.currentMxpInstanceID.equals(expectedMxpInstanceID) &&
                                model.lastMxPluginID.equals(model.imageRecParams.mxPluginId)) {
                                Log.d(TAG, "Mxp Instance ID correct, doing execution.");
                                model.takmlExecutor.executePrediction(model.currentMxpInstanceID, byteArray, model);
                            } else {
                                Log.d(TAG, "Instantiation for appropriate mxp Instance ID not done yet.");
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                model.currentPredictionResult = WAITING_TEXT;
                                            }
                                        });
                                    }
                                });

                                // destroy previous instance of plugin if it did exist
                                if (model.currentMxpInstanceID != null) {
                                    model.takmlExecutor.destroyPlugin(model.currentMxpInstanceID);
                                }

                                Log.d(TAG, "Instantiating mx plugin with params: " +
                                        "mx plugin id: " + model.imageRecParams.mxPluginId + "\n" +
                                        "model name: " + model.imageRecParams.modelName + "\n" +
                                        "mx plugin params: " + model.imageRecParams.mxPluginParams);

                                model.lastMxPluginID = model.imageRecParams.mxPluginId;
                                model.currentMxpInstanceID = null;
                                model.currentInstantiateToken = model.takmlExecutor.instantiatePlugin(
                                        model.imageRecParams.mxPluginId,
                                        model.imageRecParams.modelName,
                                        model.imageRecParams.mxPluginParams,
                                        model
                                );
                            }
                            if (enableUIToken != null) {
                                mHandler.removeCallbacksAndMessages(enableUIToken);
                            }
                            havePendingResult = false;
                            disableUIInternal();
                            enableUIToken = new Object();
                            havePendingResult = true;
                            mHandler.postAtTime(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d(TAG, "Enabling UI after max ui disable time ran out.");
                                    enableUIToken = null;
                                    havePendingResult = false;
                                    model.currentPredictionResult = "Prediction request timed out. Try again.";
                                    enableUIInternal();
                                }
                            }, enableUIToken,SystemClock.uptimeMillis() + MAX_UI_DISABLE_TIME_MS);
                        } else {
                            Toast.makeText(pluginContext,
                                    "Not connected to framework.",
                                    Toast.LENGTH_LONG).show();
                            model.currentPredictionResult = "Not connected to framework. Try again.";
                        }
                    }
                }
            });

            settingsButton = (Button) templateView.findViewById(R.id.settingsButton);
            settingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    model.takmlExecutor.requestResourcesList();
                    model.takmlExecutor.requestTAKMLFrameworkAppDataDirectoryResourcesList();
                    closeDropDown();
                    Intent nextIntent = new Intent();
                    nextIntent.setAction(SHOW_SETTINGS);
                    AtakBroadcast.getInstance().sendBroadcast(nextIntent);
                }
            });

            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MSG_UPDATE_UI: {
                            updateUiInternal();
                            mHandler.sendEmptyMessageAtTime(MSG_UPDATE_UI,
                                    SystemClock.uptimeMillis() + UI_UPDATE_INTERVAL_MS);
                            break;
                        }
                    }
                }
            };
            mHandler.removeMessages(MSG_UPDATE_UI);
            mHandler.sendEmptyMessageAtTime(MSG_UPDATE_UI,
                    SystemClock.uptimeMillis() + UI_UPDATE_INTERVAL_MS);

        }
    }

    public void startCamera() {
        Intent intent = new Intent();
        intent.setClassName(pluginContext.getPackageName(),
                CameraActivity.class.getName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //GenericImageRecognitionLifecycle.activity.getApplicationContext().startActivity(intent);
        MapView.getMapView().getContext().startActivity(intent);
    }

    public void startLoadActivity() {
        /*Uri imageUri = Uri.parse("");
        try {
            Bitmap photo = BitmapFactory.decodeStream(MapView.getMapView().getContext().getContentResolver().openInputStream(imageUri));
            photo.compress(Bitmap.CompressFormat.PNG, 100, pluginContext.openFileOutput("test2.png", 8888));
            //ParcelFileDescriptor pfd = MapView.getMapView().getContext().getContentResolver().openFileDescriptor(imageUri, "r");
            //FileDescriptor fd = pfd.getFileDescriptor();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }*/

        Log.d(TAG, "Starting load activity.");
        Intent intent = new Intent();
        intent.setClassName(pluginContext.getPackageName(),
                LoadFileActivity.class.getName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        GenericImageRecognitionLifecycle.activity.getApplicationContext().startActivity(intent);

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

    public void setHavePendingResult(boolean v) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (v == false) {
                    if (enableUIToken != null) {
                        mHandler.removeCallbacksAndMessages(enableUIToken);
                    }
                }
                havePendingResult = v;

            }
        });
    }

    private void disableUIInternal() {
        sendButton.setEnabled(false);
        takeNewImageButton.setEnabled(false);
        loadNewImageButton.setEnabled(false);
    }

    private void enableUIInternal() {
        sendButton.setEnabled(true);
        takeNewImageButton.setEnabled(true);
        loadNewImageButton.setEnabled(true);
    }

    private void updateUiInternal() {

        connectionStatusDisplay.setText(Boolean.toString(model.connectedToFramework));
        connectionStatusDisplay.setTextColor(model.connectedToFramework ? Color.GREEN : Color.RED);

        if (imageChanged) {
            Log.d(TAG, "image changed.");
            imageView.setImageBitmap(BitmapFactory.decodeFile(model.imageFile.getPath()));
            imageChanged = false;
        }

        if (model.currentPredictionResult.equals(WAITING_TEXT)) {
            predictionResultsDisplay.setVisibility(GONE);
            pendingBar.setVisibility(VISIBLE);
        } else if (!model.currentPredictionResult.equals("")) {
            predictionResultsDisplay.setVisibility(VISIBLE);
            pendingBar.setVisibility(GONE);
            predictionResultsDisplay.setText(
                    "MX Plugin: " + model.imageRecParams.mxPluginName + "\n" +
                            "Model name: " + model.imageRecParams.modelName + "\n" +
                            "Confidence threshold: " + Float.toString(model.imageRecParams.minimumConfidence) + "\n" +
                            "---" + "\n" +
                            model.currentPredictionResult
            );
        } else {
            pendingBar.setVisibility(GONE);
            predictionResultsDisplay.setVisibility(VISIBLE);
            predictionResultsDisplay.setText("");
        }

        if (model.connectedToFramework && !havePendingResult) {
            enableUIInternal();
            settingsButton.setEnabled(true);
        } else {
            disableUIInternal();
            if (havePendingResult) {
                settingsButton.setEnabled(false);
            }
        }

    }
}
