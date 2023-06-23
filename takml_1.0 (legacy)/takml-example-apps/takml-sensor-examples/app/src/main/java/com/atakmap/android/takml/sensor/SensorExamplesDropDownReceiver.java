
package com.atakmap.android.takml.sensor;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml.sensor.plugin.R;
import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;

import com.atakmap.coremap.log.Log;
import com.bbn.tak.ml.MQTTHelper;
import com.bbn.tak.ml.TakMlConstants;
import com.bbn.tak.ml.sensor.SensorDataCallback;
import com.bbn.tak.ml.sensor.SensorFrameworkClient;
import com.bbn.tak.ml.sensor_framework.SensorDBQuery_Observation;
import com.bbn.tak.ml.sensor_framework.SensorDataUtils;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.model.Observation;

public class SensorExamplesDropDownReceiver extends DropDownReceiver implements
        OnStateListener {

    public static final String TAG = SensorExamplesDropDownReceiver.class
            .getSimpleName();

    public static final String SHOW_PLUGIN = "com.atakmap.android.takml.sensor.SHOW_PLUGIN";
    private final View templateView;
    private final Context pluginContext;
    private MqttClient client;
    private EditText responseEditText;

    /**************************** CONSTRUCTOR *****************************/

    public SensorExamplesDropDownReceiver(final MapView mapView,
            final Context context) {
        super(mapView);
        this.pluginContext = context;

        // Remember to use the PluginLayoutInflator if you are actually inflating a custom view
        // In this case, using it is not necessary - but I am putting it here to remind
        // developers to look at this Inflator
        templateView = PluginLayoutInflater.inflate(context, R.layout.main_layout, null);
        responseEditText = templateView.findViewById(R.id.editTextResponse);

        Button clearButton = templateView.findViewById(R.id.clearResultsBtn);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                responseEditText.setText("");
            }
        });


        // ** called when subscription data arrives, simply updates the results text box
        SensorDataCallback sensorDataCallback = new SensorDataCallback() {
            @Override
            public void dataAvailable(Observation result) {
                Log.d(TAG, "Received sensor data subscribtion result: " + result.getResult() + " recorded at " + result.getResultTime());
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (responseEditText.getText().toString().startsWith("Loading")) {
                            responseEditText.setText("");
                        }
                        responseEditText.setText(responseEditText.getText() + "Received sensor data query result: " + result.getResult() + " recorded at " + result.getResultTime() + "\n");
                    }
                });
            }
        };

        // ** the SensorFrameworkClient allows for interacting with the TAK-ML sensor framework
        final SensorFrameworkClient sfc = new SensorFrameworkClient((msg, ex) -> Log.e(TAG, msg, ex), null, "SensorExamples");

        // ** on-click unsubscribe from the (currently hard coded) sensor feed
        // ** the sensor name acts as a prefix, so here Mag will match MagnetometerPlugin
        Button unsubscribeButton = templateView.findViewById(R.id.unsubscribeBtn);
        unsubscribeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread() {
                    @Override
                    public void run() {
                        sfc.unsubscribe("Mag", sensorDataCallback);
                    }
                }.start();
            }
        });


        // ** on-click subscribe to the (currently hard coded) sensor feed
        // ** the sensor name acts as a prefix, so here Mag with match MagnetometerPlugin
        Button subscribeButton = templateView.findViewById(R.id.subscribeBtn);
        subscribeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread() {
                    @Override
                    public void run() {
                        sfc.subscribe("Mag", sensorDataCallback);
                    }
                }.start();
            }
        });

        // ** on-click query the DB
        // ** two filters are applied on the query object:
        // ** 1. the sensor name (acts as a prefix for the actual sensor name)
        // ** 2. the resultime must be within the previous 30 hours
        // ** The same type of SensorDataCallback is used here with queries, as was used with subscriptions above
        Button queryButton = templateView.findViewById(R.id.queryBtn);
        queryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread() {
                    @Override
                    public void run() {
                        SensorDBQuery_Observation query = new SensorDBQuery_Observation();
                        query.setSensorName("Mag");
                        query.setResultTimePrevXhrs(30);
                        sfc.querySensorDB(query, new SensorDataCallback() {
                            @Override
                            public void dataAvailable(Observation result) {
                                Log.d(TAG, "Received sensor data query result: " + result.getResult() + " recorded at " + result.getResultTime());
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(responseEditText.getText().toString().startsWith("Loading")) {
                                            responseEditText.setText("");
                                        }
                                        responseEditText.setText(responseEditText.getText() + "Received sensor data query result: " + result.getResult() + " recorded at " + result.getResultTime() + "\n");
                                    }
                                });
                            }
                        });
                    }
                }.start();
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

            Log.d(TAG, "showing plugin drop down");
            showDropDown(templateView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                    HALF_HEIGHT, false);


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
